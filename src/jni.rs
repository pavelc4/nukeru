use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::jstring;

#[no_mangle]
pub extern "C" fn Java_com_nukeru_backend_NukeruJni_getHelloWorld<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
) -> jstring {
    let output = env.new_string("Hello from Rust!").expect("Couldn't create java string!");
    output.into_raw()
}
