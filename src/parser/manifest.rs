use std::io::Read;

use anyhow::{Context, Result, bail};
use byteorder::{BigEndian, ReadBytesExt};
use protobuf::Message;

use crate::parser::{BRILLO_MAJOR_VERSION, PAYLOAD_MAGIC, PartitionMeta, PayloadHeader};
use crate::update_metadata::DeltaArchiveManifest;

pub fn parse_header<R: Read>(r: &mut R) -> Result<PayloadHeader> {
    let mut magic = [0u8; 4];
    r.read_exact(&mut magic)
        .context("Failed to read magic bytes")?;

    if &magic != PAYLOAD_MAGIC {
        bail!("Magic mismatch: expected CrAU, got {:?}", magic);
    }

    let version = r
        .read_u64::<BigEndian>()
        .context("Failed to read version")?;
    let manifest_size = r
        .read_u64::<BigEndian>()
        .context("Failed to read manifest_size")?;

    if manifest_size == 0 {
        bail!("manifest_size is 0 — payload may be corrupt");
    }

    let metadata_sig_size = if version >= BRILLO_MAJOR_VERSION {
        r.read_u32::<BigEndian>()
            .context("Failed to read metadata_sig_size")?
    } else {
        0
    };

    let header_size = 4
        + 8
        + 8
        + if version >= BRILLO_MAJOR_VERSION {
            4u64
        } else {
            0
        };

    Ok(PayloadHeader {
        version,
        manifest_size,
        metadata_sig_size,
        header_size,
    })
}

pub fn parse_manifest<R: Read>(
    r: &mut R,
    header: &PayloadHeader,
) -> Result<(DeltaArchiveManifest, u64)> {
    let mut buf = vec![0u8; header.manifest_size as usize];
    r.read_exact(&mut buf)
        .context("Failed to read manifest bytes")?;

    if header.metadata_sig_size > 0 {
        let mut sig = vec![0u8; header.metadata_sig_size as usize];
        r.read_exact(&mut sig)
            .context("Failed to skip metadata signature")?;
    }

    let manifest = DeltaArchiveManifest::parse_from_bytes(&buf)
        .context("Failed to deserialize DeltaArchiveManifest")?;

    if manifest.partitions.is_empty() {
        bail!("Manifest has no partitions — not an A/B OTA?");
    }

    let data_offset = header.header_size + header.manifest_size + header.metadata_sig_size as u64;

    Ok((manifest, data_offset))
}

pub fn list_partitions(manifest: &DeltaArchiveManifest, _data_offset: u64) -> Vec<PartitionMeta> {
    manifest
        .partitions
        .iter()
        .map(|p| PartitionMeta {
            name: p.partition_name.clone().expect("REASON"),
            size_bytes: p
                .new_partition_info
                .as_ref()
                .and_then(|i| i.size)
                .unwrap_or(0),
            op_count: p.operations.len(),
        })
        .collect()
}
