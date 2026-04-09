use std::fs::{self, File};
use std::io::{BufWriter, Read, Seek, SeekFrom, Write};
use std::path::Path;
use std::sync::Arc;

use anyhow::{Context, Result, bail};
use bzip2::read::BzDecoder;
use rayon::prelude::*;
use xz2::read::XzDecoder;

use crate::update_metadata::install_operation::Type as OpType;
use crate::update_metadata::{DeltaArchiveManifest, InstallOperation};
use crate::verify::verify_sha256;

use super::{ExtractRequest, ProgressCallback};

const BLOCK_SIZE: u64 = 4096;

pub fn run(req: &ExtractRequest, manifest: &DeltaArchiveManifest, cb: Arc<dyn ProgressCallback>) {
    let selected: std::collections::HashSet<&str> =
        req.partitions.iter().map(|p| p.name.as_str()).collect();

    manifest
        .partitions
        .par_iter()
        .filter(|p| {
            let name = p.partition_name.as_deref().unwrap_or("");
            selected.is_empty() || selected.contains(name)
        })
        .for_each(|partition| {
            let name = partition.partition_name.as_deref().unwrap_or("");
            let out_path = Path::new(req.output_dir.as_str()).join(format!("{}.img", name));

            match extract_partition(req, partition, &out_path, cb.as_ref()) {
                Ok(_) => cb.on_partition_done(name, true),
                Err(e) => {
                    cb.on_error(name, &e.to_string());
                    cb.on_partition_done(name, false);
                    let _ = fs::remove_file(&out_path);
                }
            }
        });
}

fn extract_partition(
    req: &ExtractRequest,
    partition: &crate::update_metadata::PartitionUpdate,
    out_path: &Path,
    cb: &dyn ProgressCallback,
) -> Result<()> {
    let mut src = File::open(req.zip_path.as_str())
        .with_context(|| format!("Failed to open zip: {}", req.zip_path))?;

    let dst = File::create(out_path)
        .with_context(|| format!("Failed to create output: {:?}", out_path))?;
    let mut writer = BufWriter::with_capacity(1 << 20, dst);

    let name = partition.partition_name.as_deref().unwrap_or("");
    let ops_total = partition.operations.len();
    let mut bytes_written: u64 = 0;

    for (i, op) in partition.operations.iter().enumerate() {
        apply_op(&mut src, req.data_offset, op, &mut writer)
            .with_context(|| format!("Op #{} on partition '{}'", i, name))?;

        let op_bytes: u64 = op
            .dst_extents
            .iter()
            .map(|e| e.num_blocks.unwrap_or(0) as u64 * BLOCK_SIZE)
            .sum();
        bytes_written += op_bytes;

        cb.on_progress(name, i + 1, ops_total, bytes_written);
    }

    writer.flush().context("Flush failed")?;
    Ok(())
}

fn apply_op<W: Write>(
    src: &mut File,
    data_offset: u64,
    op: &InstallOperation,
    writer: &mut W,
) -> Result<()> {
    match op.type_() {
        OpType::ZERO | OpType::DISCARD => return write_zeros(writer, op),
        OpType::REPLACE | OpType::REPLACE_BZ | OpType::REPLACE_XZ => {}
        other => bail!("Unsupported op type: {:?}", other),
    }

    let data_len = op.data_length.context("data_length missing")?;
    let op_offset = op.data_offset.context("data_offset missing")?;

    src.seek(SeekFrom::Start(data_offset + op_offset))
        .context("Seek failed")?;

    let mut compressed = vec![0u8; data_len as usize];
    src.read_exact(&mut compressed)
        .context("Read op data failed")?;

    if let Some(hash) = op.data_sha256_hash.as_deref() {
        verify_sha256(&compressed, hash).context("SHA256 mismatch")?;
    }

    let raw = decompress(op.type_(), &compressed)?;
    writer.write_all(&raw).context("Write failed")?;
    Ok(())
}

fn decompress(op_type: OpType, data: &[u8]) -> Result<Vec<u8>> {
    match op_type {
        OpType::REPLACE => Ok(data.to_vec()),
        OpType::REPLACE_BZ => {
            let mut out = Vec::new();
            BzDecoder::new(data)
                .read_to_end(&mut out)
                .context("bzip2 failed")?;
            Ok(out)
        }
        OpType::REPLACE_XZ => {
            let mut out = Vec::new();
            XzDecoder::new(data)
                .read_to_end(&mut out)
                .context("xz failed")?;
            Ok(out)
        }
        _ => unreachable!(),
    }
}

fn write_zeros<W: Write>(writer: &mut W, op: &InstallOperation) -> Result<()> {
    let total: u64 = op
        .dst_extents
        .iter()
        .map(|e| e.num_blocks.unwrap_or(0) as u64)
        .sum();
    let zeros = vec![0u8; BLOCK_SIZE as usize];
    for _ in 0..total {
        writer.write_all(&zeros).context("Write zeros failed")?;
    }
    Ok(())
}
