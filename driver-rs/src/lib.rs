extern crate jni;
use jni::JNIEnv;
use jni::objects::{JString, JObject, JClass, JByteArray};
use jni::sys::jclass;

#[no_mangle]
pub extern "system" fn Java_dev_zenrho_driver_classloader_ProtectedClassLoader_loadClassNative<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    java_class_name: JString<'local>,
    java_class_bytes: JByteArray<'local>,
    loader: JObject<'local>
) -> jclass {
    // Get the class name as a String
    let class_name: String = match env.get_string(&java_class_name) {
        Ok(s) => s.into(),
        Err(err) => {
            // Handle error appropriately
            env.exception_describe().unwrap();
            panic!("> Error reading class! {:?}", err);
        }
    };

    // Convert the byte array
    let class_bytes: Vec<u8> = match env.convert_byte_array(&java_class_bytes) {
        Ok(bytes) => bytes,
        Err(err) => {
            env.exception_describe().unwrap();
            panic!("> Error loading class! {:?}", err);
        }
    };

    // Define the class
    match env.define_class(&class_name, &loader, &class_bytes) {
        Ok(class) => class.into_raw(),
        Err(err) => {
            // Handle error appropriately
            env.exception_describe().unwrap();
            panic!("> Error defining class! {:?}", err);
        }
    }
}
