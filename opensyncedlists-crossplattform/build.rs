fn main() {
    // Build script for cross-platform compilation
    println!("cargo:rerun-if-changed=src/");

    // Set platform-specific configurations
    #[cfg(target_os = "windows")]
    {
        // Windows-specific build settings
        println!("cargo:rustc-link-arg=/SUBSYSTEM:WINDOWS");
    }

    #[cfg(target_arch = "wasm32")]
    {
        // WASM-specific build settings
        println!("cargo:rustc-cfg=web_sys_unstable_apis");
    }
}
