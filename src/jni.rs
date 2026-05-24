use jni::EnvUnowned;
use jni::objects::{JClass, JString};
use jni::sys::jstring;
use std::sync::mpsc;
use std::sync::{Arc, Mutex};

use crate::extractor::{ExtractRequest, ProgressCallback, extract_parallel};
use crate::parser::{PartitionMeta, PayloadReader};
use crate::zip_utils;

static PROGRESS_RX: Mutex<Option<mpsc::Receiver<String>>> = Mutex::new(None);

struct ChannelCallback {
    tx: mpsc::Sender<String>,
}

impl ProgressCallback for ChannelCallback {
    fn on_progress(&self, partition: &str, ops_done: usize, ops_total: usize, bytes_written: u64) {
        let _ = self.tx.send(format!(
            "P|{}|{}|{}|{}",
            partition, ops_done, ops_total, bytes_written
        ));
    }
    fn on_partition_done(&self, partition: &str, success: bool) {
        let _ = self.tx.send(format!(
            "D|{}|{}",
            partition,
            if success { "1" } else { "0" }
        ));
    }
    fn on_error(&self, partition: &str, message: &str) {
        let _ = self.tx.send(format!("E|{}|{}", partition, message));
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_nukeru_backend_NukeruJni_getHelloWorld<'local>(
    mut unowned_env: EnvUnowned<'local>,
    _class: JClass<'local>,
) -> JString<'local> {
    unowned_env
        .with_env(|env| -> jni::errors::Result<JString> {
            JString::from_str(env, "Hello from Rust!")
        })
        .resolve::<jni::errors::ThrowRuntimeExAndDefault>()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_nukeru_backend_NukeruJni_getPartitions<'local>(
    mut unowned_env: EnvUnowned<'local>,
    _class: JClass<'local>,
    zip_path: JString<'local>,
) -> JString<'local> {
    unowned_env
        .with_env(|env| -> jni::errors::Result<JString> {
            let zip_path_rust: String = env.get_string(&zip_path)?.into();

            let result = (|| -> anyhow::Result<String> {
                let zip_info = zip_utils::inspect_ota_zip(&zip_path_rust)?;
                let mut reader = PayloadReader::open(&zip_path_rust, zip_info.payload_offset)?;
                let (manifest, data_offset) = reader.parse()?;

                let partitions = crate::parser::list_partitions(&manifest, data_offset);

                let mut parts = Vec::new();
                for p in partitions {
                    parts.push(format!("{}|{}|{}", p.name, p.size_bytes, p.op_count));
                }
                Ok(parts.join(";"))
            })();

            let response = match result {
                Ok(s) => format!("OK:{}", s),
                Err(e) => format!("ERR:{}", e),
            };

            JString::from_str(env, &response)
        })
        .resolve::<jni::errors::ThrowRuntimeExAndDefault>()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_nukeru_backend_NukeruJni_startExtraction<'local>(
    mut unowned_env: EnvUnowned<'local>,
    _class: JClass<'local>,
    zip_path: JString<'local>,
    output_dir: JString<'local>,
    partitions_str: JString<'local>,
) -> JString<'local> {
    unowned_env
        .with_env(|env| -> jni::errors::Result<JString> {
            let zip_path_rust: String = env.get_string(&zip_path)?.into();
            let output_dir_rust: String = env.get_string(&output_dir)?.into();
            let parts_raw: String = env.get_string(&partitions_str)?.into();

            crate::IS_CANCELLED.store(false, std::sync::atomic::Ordering::Relaxed);

            let target_parts: Vec<String> = parts_raw.split(',').map(|s| s.to_string()).collect();

            let (tx, rx) = mpsc::channel();
            *PROGRESS_RX.lock().unwrap() = Some(rx);

            std::thread::spawn(move || {
                let res = (|| -> anyhow::Result<()> {
                    let zip_info = zip_utils::inspect_ota_zip(&zip_path_rust)?;
                    let mut reader = PayloadReader::open(&zip_path_rust, zip_info.payload_offset)?;
                    let (manifest, data_offset) = reader.parse()?;
                    let all_partitions = crate::parser::list_partitions(&manifest, data_offset);

                    let filtered: Vec<PartitionMeta> = all_partitions
                        .into_iter()
                        .filter(|p| target_parts.contains(&p.name))
                        .collect();

                    let req = ExtractRequest {
                        zip_path: Arc::new(zip_path_rust),
                        zip_offset: zip_info.payload_offset,
                        output_dir: Arc::new(output_dir_rust),
                        partitions: filtered,
                        data_offset,
                    };

                    let cb = Arc::new(ChannelCallback { tx: tx.clone() });
                    extract_parallel(&req, &manifest, cb);
                    Ok(())
                })();

                if let Err(e) = res {
                    let _ = tx.send(format!("FATAL|{}", e));
                }
                let _ = tx.send("FINISHED".to_string());
            });

            JString::from_str(env, "OK")
        })
        .resolve::<jni::errors::ThrowRuntimeExAndDefault>()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_nukeru_backend_NukeruJni_pollProgress<'local>(
    mut unowned_env: EnvUnowned<'local>,
    _class: JClass<'local>,
) -> JString<'local> {
    unowned_env
        .with_env(|env| -> jni::errors::Result<JString> {
            let mut guard = PROGRESS_RX.lock().unwrap();
            let response = if let Some(rx) = guard.as_mut() {
                match rx.try_recv() {
                    Ok(msg) => msg,
                    Err(mpsc::TryRecvError::Empty) => "WAIT".to_string(),
                    Err(mpsc::TryRecvError::Disconnected) => "DISCONNECTED".to_string(),
                }
            } else {
                "NONE".to_string()
            };
            JString::from_str(env, &response)
        })
        .resolve::<jni::errors::ThrowRuntimeExAndDefault>()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_nukeru_backend_NukeruJni_cancelExtraction<'local>(
    _unowned_env: EnvUnowned<'local>,
    _class: JClass<'local>,
) {
    crate::IS_CANCELLED.store(true, std::sync::atomic::Ordering::Relaxed);
}
