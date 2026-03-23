use serde::{Deserialize, Serialize};

use super::ListTag;

/// Header for a SyncedList, contains all information about a list
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SyncedListHeader {
    pub id: String,
    pub name: String,
    pub check_option: bool,
    pub checked_list: bool,
    pub overview_active: bool,
    pub auto_sync: bool,
    pub invert_element: bool,
    pub jump_buttons: bool,
    pub hostname: String,
    pub secret: String,
    pub local_secret: String,
    pub list_size: String,
    pub tags: Vec<ListTag>,
}

impl SyncedListHeader {
    pub fn new(id: String, name: String, hostname: String, secret: String, local_secret: String) -> Self {
        Self {
            id,
            name,
            hostname,
            secret,
            local_secret,
            check_option: true,
            checked_list: true,
            overview_active: false,
            invert_element: false,
            auto_sync: true,
            jump_buttons: true,
            list_size: "0 / 0".to_string(),
            tags: Vec::new(),
        }
    }

    pub fn is_checked_list(&self) -> bool {
        self.check_option && self.checked_list
    }

    pub fn set_tag_list(&mut self, tags: Vec<ListTag>) {
        // Remove duplicates
        let mut unique_tags = Vec::new();
        for tag in tags {
            if !unique_tags.contains(&tag) {
                unique_tags.push(tag);
            }
        }
        self.tags = unique_tags;
    }
}
