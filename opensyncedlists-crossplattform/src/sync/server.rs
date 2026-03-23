use anyhow::Result;
use log::debug;
use reqwest::Client;
use serde_json::Value;
use std::collections::HashMap;

use crate::data::SyncedList;
use crate::storage::SecureStorage;

#[derive(Clone)]
pub struct ServerWrapper {
    client: Client,
}

impl ServerWrapper {
    pub fn new() -> Self {
        Self {
            client: Client::new(),
        }
    }

    /// Check connection to a server
    pub async fn check_connection(&self, hostname: &str) -> Result<bool> {
        debug!("Checking connection to {}", hostname);

        let url = format!("{}/test", hostname);
        let response = self.client.get(&url).send().await;

        match response {
            Ok(resp) => Ok(resp.status().is_success()),
            Err(_) => Ok(false),
        }
    }

    /// Get a synced list from a server
    pub async fn get_list(&self, hostname: &str, id: &str, secret: &str) -> Result<SyncedList> {
        debug!("Getting list {} from {}", id, hostname);

        let url = format!("{}/list/get", hostname);
        let response = self
            .client
            .get(&url)
            .query(&[("id", id), ("secret", secret)])
            .send()
            .await?;

        if response.status().is_success() {
            let json_text = response.text().await?;
            let json_value: Value = serde_json::from_str(&json_text)?;

            if let Some(data) = json_value.get("data").and_then(|d| d.as_str()) {
                let decrypted = SecureStorage::decrypt_data(data, secret)?;
                SyncedList::from_json_with_header(&decrypted)
            } else {
                Err(anyhow::anyhow!("Invalid response format"))
            }
        } else {
            Err(anyhow::anyhow!("Failed to get list: {}", response.status()))
        }
    }

    /// Upload/set a list on a server
    pub async fn set_list(&self, list: &SyncedList, based_on_hash: &str) -> Result<()> {
        debug!(
            "Setting list {} on {}",
            list.header.id, list.header.hostname
        );

        let url = format!("{}/list/set", list.header.hostname);
        let list_json = list.to_json_with_header()?;
        let encrypted_data = SecureStorage::encrypt_data(&list_json, &list.header.local_secret)?;
        let data_hash = SecureStorage::hash_data(&encrypted_data);

        let mut form = HashMap::new();
        form.insert("data", encrypted_data);
        form.insert("hash", data_hash);
        form.insert("basedOnHash", based_on_hash.to_string());

        let response = self
            .client
            .post(&url)
            .query(&[("id", &list.header.id), ("secret", &list.header.secret)])
            .json(&form)
            .send()
            .await?;

        if response.status().is_success() {
            Ok(())
        } else {
            Err(anyhow::anyhow!("Failed to set list: {}", response.status()))
        }
    }

    /// Add a new list to a server
    pub async fn add_list(&self, list: &SyncedList) -> Result<()> {
        debug!("Adding list {} to {}", list.header.id, list.header.hostname);

        let url = format!("{}/list/add", list.header.hostname);
        let list_json = list.to_json_with_header()?;
        let encrypted_data = SecureStorage::encrypt_data(&list_json, &list.header.local_secret)?;
        let data_hash = SecureStorage::hash_data(&encrypted_data);

        let mut form = HashMap::new();
        form.insert("data", encrypted_data);
        form.insert("hash", data_hash);

        let response = self
            .client
            .post(&url)
            .query(&[("id", &list.header.id), ("secret", &list.header.secret)])
            .json(&form)
            .send()
            .await?;

        if response.status().is_success() {
            Ok(())
        } else {
            Err(anyhow::anyhow!("Failed to add list: {}", response.status()))
        }
    }

    /// Remove a list from a server
    pub async fn remove_list(&self, hostname: &str, id: &str, secret: &str) -> Result<()> {
        debug!("Removing list {} from {}", id, hostname);

        let url = format!("{}/list/remove", hostname);
        let response = self
            .client
            .get(&url)
            .query(&[("id", id), ("secret", secret)])
            .send()
            .await?;

        if response.status().is_success() {
            Ok(())
        } else {
            Err(anyhow::anyhow!(
                "Failed to remove list: {}",
                response.status()
            ))
        }
    }
}
