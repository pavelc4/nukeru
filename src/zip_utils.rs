use std::fs::File;
use std::io::{BufReader, Read};

use anyhow::{Context, Result, bail};
use zip::ZipArchive;

pub struct OtaZipInfo {
    pub payload_offset: u64,
    pub payload_size: u64,
    pub properties: String,
}

pub fn inspect_ota_zip(zip_path: &str) -> Result<OtaZipInfo> {
    let file = File::open(zip_path).with_context(|| format!("Tidak bisa buka: {}", zip_path))?;

    let mut archive = ZipArchive::new(BufReader::new(file)).context("Bukan ZIP valid")?;

    let properties = {
        let mut entry = archive
            .by_name("payload_properties.txt")
            .context("payload_properties.txt tidak ditemukan")?;

        let mut s = String::new();
        entry.read_to_string(&mut s)?;
        s
    };

    let (payload_offset, payload_size) = {
        let entry = archive
            .by_name("payload.bin")
            .context("payload.bin tidak ditemukan")?;

        if entry.compression() != zip::CompressionMethod::Stored {
            bail!(
                "payload.bin bukan STORED (got {:?}). Bukan A/B OTA package.",
                entry.compression()
            );
        }

        (entry.data_start(), entry.size())
    };

    Ok(OtaZipInfo {
        payload_offset,
        payload_size,
        properties,
    })
}
