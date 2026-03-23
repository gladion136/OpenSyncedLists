use serde::{Deserialize, Serialize};

/// A tag for categorizing lists
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub struct ListTag {
    pub name: String,
}

impl ListTag {
    pub fn new(name: String) -> Self {
        Self { name }
    }
}
