package com.nukeru.backend

object NukeruJni {
    init {
        try {
            System.loadLibrary("nukeru")
        } catch (e: UnsatisfiedLinkError) {
            e.printStackTrace()
        }
    }

    /**
     * Test JNI connection.
     * Returns "Hello from Rust!" if successfully connected.
     */
    external fun getHelloWorld(): String
}
