fn main() {
    protobuf_codegen::Codegen::new()
        .pure()
        .include("proto")
        .input("proto/update_metadata.proto")
        .cargo_out_dir("protos")
        .run_from_script();
}
