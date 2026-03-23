use anyhow::Result;

#[cfg(not(target_arch = "wasm32"))]
use std::fs;
use std::path::{Path, PathBuf};
#[cfg(target_arch = "wasm32")]
use wasm_bindgen::prelude::*;
#[cfg(target_arch = "wasm32")]
use web_sys::{window, Storage};

use crate::data::SyncedList;

#[derive(Clone)]
pub struct FileStorage {
    data_dir: PathBuf,
}

impl FileStorage {
    pub fn new() -> Result<Self> {
        let data_dir = PathBuf::from(".");

        // Skip file system operations for WASM/browser environment
        #[cfg(not(target_arch = "wasm32"))]
        {
            if !data_dir.exists() {
                fs::create_dir_all(&data_dir)?;
            }
        }

        Ok(Self { data_dir })
    }

    #[cfg(target_arch = "wasm32")]
    fn get_local_storage() -> Result<Storage> {
        let window = window().ok_or_else(|| anyhow::anyhow!("No window object"))?;
        let storage = window
            .local_storage()
            .map_err(|_| anyhow::anyhow!("Failed to access localStorage"))?
            .ok_or_else(|| anyhow::anyhow!("localStorage not available"))?;
        Ok(storage)
    }

    pub fn save_list(&self, list: &SyncedList) -> Result<()> {
        #[cfg(target_arch = "wasm32")]
        {
            let storage = Self::get_local_storage()?;
            let json = list.to_json_with_header()?;
            let key = format!("opensyncedlists_list_{}", list.header.id);
            storage
                .set_item(&key, &json)
                .map_err(|_| anyhow::anyhow!("Failed to save to localStorage"))?;
            return Ok(());
        }

        #[cfg(not(target_arch = "wasm32"))]
        {
            let file_path = self.data_dir.join(format!("{}.json", list.header.id));
            let json = list.to_json_with_header()?;
            fs::write(file_path, json)?;
        }

        Ok(())
    }

    pub fn load_list(&self, id: &str) -> Result<SyncedList> {
        #[cfg(target_arch = "wasm32")]
        {
            let storage = Self::get_local_storage()?;
            let key = format!("opensyncedlists_list_{}", id);
            let json = storage
                .get_item(&key)
                .map_err(|_| anyhow::anyhow!("Failed to read from localStorage"))?
                .ok_or_else(|| anyhow::anyhow!("List not found in localStorage"))?;
            return SyncedList::from_json_with_header(&json);
        }

        #[cfg(not(target_arch = "wasm32"))]
        {
            let file_path = self.data_dir.join(format!("{}.json", id));
            let json = fs::read_to_string(file_path)?;
            return SyncedList::from_json_with_header(&json);
        }
    }

    pub fn list_all_lists(&self) -> Result<Vec<String>> {
        #[cfg(target_arch = "wasm32")]
        {
            let storage = Self::get_local_storage()?;
            let mut list_ids = Vec::new();

            let length = storage
                .length()
                .map_err(|_| anyhow::anyhow!("Failed to get localStorage length"))?;
            for i in 0..length {
                if let Ok(Some(key)) = storage.key(i) {
                    if key.starts_with("opensyncedlists_list_") {
                        let id = key
                            .strip_prefix("opensyncedlists_list_")
                            .unwrap()
                            .to_string();
                        list_ids.push(id);
                    }
                }
            }
            return Ok(list_ids);
        }

        #[cfg(not(target_arch = "wasm32"))]
        {
            let mut list_ids = Vec::new();
            for entry in fs::read_dir(&self.data_dir)? {
                let entry = entry?;
                let path = entry.path();
                if path.is_file() && path.extension().and_then(|s| s.to_str()) == Some("json") {
                    if let Some(stem) = path.file_stem().and_then(|s| s.to_str()) {
                        list_ids.push(stem.to_string());
                    }
                }
            }
            return Ok(list_ids);
        }
    }

    pub fn delete_list(&self, id: &str) -> Result<()> {
        #[cfg(target_arch = "wasm32")]
        {
            let storage = Self::get_local_storage()?;
            let key = format!("opensyncedlists_list_{}", id);
            storage
                .remove_item(&key)
                .map_err(|_| anyhow::anyhow!("Failed to delete from localStorage"))?;
            return Ok(());
        }

        #[cfg(not(target_arch = "wasm32"))]
        {
            let file_path = self.data_dir.join(format!("{}.json", id));
            if file_path.exists() {
                fs::remove_file(file_path)?;
            }
        }

        Ok(())
    }

    pub fn export_list(&self, list: &SyncedList, export_path: &Path) -> Result<()> {
        #[cfg(not(target_arch = "wasm32"))]
        {
            let json = list.to_json_with_header()?;
            fs::write(export_path, json)?;
        }
        Ok(())
    }

    pub fn export_all_lists(&self, lists: &[SyncedList], export_path: &Path) -> Result<()> {
        #[cfg(not(target_arch = "wasm32"))]
        {
            let json = serde_json::to_string_pretty(lists)?;
            fs::write(export_path, json)?;
        }
        Ok(())
    }

    pub fn import_list(&self, import_path: &Path) -> Result<SyncedList> {
        #[cfg(not(target_arch = "wasm32"))]
        {
            let json = fs::read_to_string(import_path)?;
            return SyncedList::from_json_with_header(&json);
        }
        #[cfg(target_arch = "wasm32")]
        {
            return Err(anyhow::anyhow!("Import not supported in WASM"));
        }
    }

    pub fn import_lists(&self, import_path: &Path) -> Result<Vec<SyncedList>> {
        #[cfg(not(target_arch = "wasm32"))]
        {
            let json = fs::read_to_string(import_path)?;
            return Ok(serde_json::from_str(&json)?);
        }
        #[cfg(target_arch = "wasm32")]
        {
            return Err(anyhow::anyhow!("Import not supported in WASM"));
        }
    }
}
