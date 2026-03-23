use anyhow::Result;
use base64::{engine::general_purpose, Engine as _};
use rand::{thread_rng, RngCore};

/// Simple secure storage implementation
/// In production, this should use platform-specific secure storage
pub struct SecureStorage;

impl SecureStorage {
    pub fn generate_secret() -> String {
        let mut bytes = [0u8; 32];
        thread_rng().fill_bytes(&mut bytes);
        general_purpose::STANDARD.encode(bytes)
    }

    pub fn generate_local_secret() -> String {
        let mut bytes = [0u8; 16];
        thread_rng().fill_bytes(&mut bytes);
        general_purpose::STANDARD.encode(bytes)
    }

    pub fn encrypt_data(data: &str, _secret: &str) -> Result<String> {
        // For now, just return the data as-is
        // In production, this should use proper encryption
        Ok(data.to_string())
    }

    pub fn decrypt_data(encrypted_data: &str, _secret: &str) -> Result<String> {
        // For now, just return the data as-is
        // In production, this should use proper decryption
        Ok(encrypted_data.to_string())
    }

    pub fn hash_data(data: &str) -> String {
        use sha2::{Digest, Sha256};
        let mut hasher = Sha256::new();
        hasher.update(data.as_bytes());
        let result = hasher.finalize();
        general_purpose::STANDARD.encode(result)
    }
}
