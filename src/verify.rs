use anyhow::{Result, bail};
use sha2::{Digest, Sha256};

pub fn verify_sha256(data: &[u8], expected: &[u8]) -> Result<()> {
    if expected.is_empty() {
        return Ok(());
    }

    let got = Sha256::digest(data);

    if got.as_slice() != expected {
        bail!(
            "SHA256 mismatch\n  expected : {}\n  got      : {}",
            hex::encode(expected),
            hex::encode(got)
        );
    }
    Ok(())
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn verify_ok() {
        let data = b"hello nukeru";
        let hash = Sha256::digest(data);
        verify_sha256(data, &hash).unwrap();
    }

    #[test]
    fn verify_fail() {
        assert!(verify_sha256(b"hello", &[0u8; 32]).is_err());
    }

    #[test]
    fn verify_empty_skip() {
        verify_sha256(b"apapun", &[]).unwrap();
    }
}
