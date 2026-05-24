include!(concat!(env!("OUT_DIR"), "/protos/mod.rs"));

use std::sync::atomic::AtomicBool;
pub static IS_CANCELLED: AtomicBool = AtomicBool::new(false);

pub mod extractor;
pub mod parser;
pub mod verify;
pub mod zip_utils;

#[cfg(target_os = "android")]
pub mod jni;
