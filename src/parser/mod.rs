pub mod manifest;

use std::fs::File;
use std::io::{BufReader, Seek, SeekFrom};

use anyhow::{Context, Result};

use crate::update_metadata::DeltaArchiveManifest;

pub use manifest::list_partitions;

pub const PAYLOAD_MAGIC: &[u8; 4] = b"CrAU";
pub const BRILLO_MAJOR_VERSION: u64 = 2;

pub struct PayloadHeader {
    pub version: u64,
    pub manifest_size: u64,
    pub metadata_sig_size: u32,
    pub header_size: u64,
}

pub struct PartitionMeta {
    pub name: String,
    pub size_bytes: u64,
    pub op_count: usize,
}

pub struct PayloadReader {
    reader: BufReader<File>,
    payload_offset: u64,
}

impl PayloadReader {
    pub fn open(zip_path: &str, payload_offset: u64) -> Result<Self> {
        let file =
            File::open(zip_path).with_context(|| format!("Tidak bisa buka: {}", zip_path))?;

        let mut reader = BufReader::new(file);
        reader
            .seek(SeekFrom::Start(payload_offset))
            .context("failed to seek to payload.bin offset")?;

        Ok(Self {
            reader,
            payload_offset,
        })
    }

    pub fn parse(&mut self) -> Result<(DeltaArchiveManifest, u64)> {
        let header = manifest::parse_header(&mut self.reader)?;
        let (mf, data_offset_in_payload) = manifest::parse_manifest(&mut self.reader, &header)?;

        let abs_data_offset = self.payload_offset + data_offset_in_payload;
        Ok((mf, abs_data_offset))
    }
}
