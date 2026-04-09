pub mod operation;

use std::sync::Arc;

use crate::parser::PartitionMeta;
use crate::update_metadata::DeltaArchiveManifest;

pub trait ProgressCallback: Send + Sync {
    fn on_progress(&self, partition: &str, ops_done: usize, ops_total: usize, bytes_written: u64);
    fn on_partition_done(&self, partition: &str, success: bool);
    fn on_error(&self, partition: &str, message: &str);
}

pub struct ExtractRequest {
    pub zip_path: Arc<String>,
    pub zip_offset: u64,
    pub output_dir: Arc<String>,
    pub partitions: Vec<PartitionMeta>,
    pub data_offset: u64,
}

pub fn extract_parallel(
    req: &ExtractRequest,
    manifest: &DeltaArchiveManifest,
    cb: Arc<dyn ProgressCallback>,
) {
    operation::run(req, manifest, cb);
}
