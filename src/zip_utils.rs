use std::fs::File;
use std::io::{BufReader, Read};
use std::os::fd::FromRawFd;

use anyhow::{Context, Result, bail};
use zip::ZipArchive;

pub struct OtaZipInfo {
    pub payload_offset: u64,
    pub payload_size: u64,
    pub properties: String,
}

pub fn open_zip(path: &str) -> Result<File> {
    if let Some(fd_str) = path.strip_prefix("FD:") {
        let fd: i32 = fd_str.parse().context("Invalid FD string")?;
        let orig = unsafe { File::from_raw_fd(fd) };
        let cloned = orig.try_clone().context("Failed to dup FD")?;
        std::mem::forget(orig); // prevent closing the FD owned by Kotlin
        Ok(cloned)
    } else {
        File::open(path).with_context(|| format!("Failed to open: {}", path))
    }
}

pub fn inspect_ota_zip(zip_path: &str) -> Result<OtaZipInfo> {
    let file = open_zip(zip_path)?;

    let mut archive =
        ZipArchive::new(BufReader::new(file)).context("Failed to open ZIP archive")?;

    let properties = {
        let mut entry = archive
            .by_name("payload_properties.txt")
            .context("payload_properties.txt not found")?;

        let mut s = String::new();
        entry.read_to_string(&mut s)?;
        s
    };

    let (payload_offset, payload_size) = {
        let entry = archive
            .by_name("payload.bin")
            .context("payload.bin not found")?;

        if entry.compression() != zip::CompressionMethod::Stored {
            bail!(
                "payload.bin Not STORED (got {:?}). Not an A/B OTA package.",
                entry.compression()
            );
        }

        (
            entry
                .data_start()
                .context("failed to fetch payload.bin offset")?,
            entry.size(),
        )
    };

    Ok(OtaZipInfo {
        payload_offset,
        payload_size,
        properties,
    })
}
