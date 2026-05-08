use jni::EnvUnowned;
use jni::objects::JClass;
use jni::objects::JString;

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
