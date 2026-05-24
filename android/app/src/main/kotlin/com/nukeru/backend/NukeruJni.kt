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

    /**
     * Parses the OTA zip and returns partitions as a string:
     * "name|size_bytes|op_count;name2|size_bytes2|op_count2"
     * Returns "OK:..." on success, "ERR:..." on failure.
     */
    external fun getPartitions(zipPath: String): String

    /**
     * Starts extraction on a background Rust thread.
     * partitions is a comma-separated list of partition names (e.g. "boot,vendor_boot").
     */
    external fun startExtraction(zipPath: String, outputDir: String, partitions: String): String

    /**
     * Polls progress from the background extraction thread.
     * Returns:
     * "WAIT" if no new events
     * "DISCONNECTED" if the thread died
     * "NONE" if no extraction is running
     * "FINISHED" if extraction completed successfully
     * "P|partition|ops_done|ops_total|bytes_written" for progress
     * "D|partition|1_or_0" for partition done
     * "E|partition|error_message" for partition error
     * "FATAL|error_message" for fatal error
     */
    external fun pollProgress(): String

    /**
     * Cancels the ongoing extraction process.
     */
    external fun cancelExtraction()
}
