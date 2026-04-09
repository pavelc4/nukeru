use nukeru::parser::PayloadReader;
use nukeru::zip_utils::inspect_ota_zip;

fn main() {
    let args: Vec<String> = std::env::args().collect();
    if args.len() < 2 {
        eprintln!("Usage: nukeru-test <path/to/ota.zip>");
        std::process::exit(1);
    }

    let zip_path = &args[1];

    println!("[1] Inspecting zip...");
    let info = inspect_ota_zip(zip_path).unwrap();
    println!("    payload offset : {}", info.payload_offset);
    println!("    payload size   : {} bytes", info.payload_size);
    println!("    properties     :\n{}", info.properties);

    println!("[2] Parsing manifest...");
    let mut reader = PayloadReader::open(zip_path, info.payload_offset).unwrap();
    let (manifest, data_offset) = reader.parse().unwrap();
    println!("    data offset    : {}", data_offset);
    println!("    partitions     : {}", manifest.partitions.len());

    println!("[3] Partition list:");
    for p in &manifest.partitions {
        let name = p.partition_name.as_deref().unwrap_or("?");
        let size = p
            .new_partition_info
            .as_ref()
            .and_then(|i| i.size)
            .unwrap_or(0);
        let ops = p.operations.len();
        println!("    {:20} {:>10} bytes  {} ops", name, size, ops);
    }
    println!("[4] Extracting vbmeta...");

    use nukeru::extractor::{ExtractRequest, ProgressCallback, extract_parallel};
    use std::sync::Arc;

    struct StdoutCb;
    impl ProgressCallback for StdoutCb {
        fn on_progress(&self, p: &str, done: usize, total: usize, bytes: u64) {
            println!("    [{}] {}/{} ops, {} bytes", p, done, total, bytes);
        }
        fn on_partition_done(&self, p: &str, success: bool) {
            println!("    [{}] done — success: {}", p, success);
        }
        fn on_error(&self, p: &str, msg: &str) {
            eprintln!("    [{}] ERROR: {}", p, msg);
        }
    }

    use nukeru::parser::PartitionMeta;

    let req = ExtractRequest {
        zip_path: Arc::new(zip_path.clone()),
        zip_offset: info.payload_offset,
        output_dir: Arc::new("/tmp/nukeru-out".to_string()),
        partitions: vec![PartitionMeta {
            name: "vbmeta".to_string(),
            size_bytes: 8192,
            op_count: 1,
        }],
        data_offset,
    };

    extract_parallel(&req, &manifest, Arc::new(StdoutCb));
    println!("    output: /tmp/nukeru-out/vbmeta.img");
    println!(
        "    size  : {} bytes",
        std::fs::metadata("/tmp/nukeru-out/vbmeta.img")
            .map(|m| m.len())
            .unwrap_or(0)
    );
}
