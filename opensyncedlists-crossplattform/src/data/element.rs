use serde::{Deserialize, Serialize};

/// An Element of a SyncedList
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub struct SyncedListElement {
    pub id: String,
    pub checked: bool,
    pub name: String,
    pub description: String,
}

impl SyncedListElement {
    pub fn new(id: String, name: String, description: String) -> Self {
        Self {
            id,
            name,
            description,
            checked: false,
        }
    }

    /// Get SyncedListElement as Markdown
    pub fn as_markdown(&self, is_checked_list: bool) -> String {
        let mut result = String::new();

        if is_checked_list {
            result.push_str(if self.checked { "- [x]" } else { "- [ ]" });
        } else {
            result.push('-');
        }

        result.push(' ');
        result.push_str(&self.name);

        if !self.description.is_empty() {
            result.push_str(" - ");
            result.push_str(&self.description);
        }

        result
    }
}
