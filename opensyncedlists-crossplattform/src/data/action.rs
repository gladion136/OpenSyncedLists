use serde::{Deserialize, Serialize};

/// Possible Actions of a SyncedListStep
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "UPPERCASE")]
pub enum Action {
    Add,
    Remove,
    Update,
    Move,
    Swap,
    Clear,
}
