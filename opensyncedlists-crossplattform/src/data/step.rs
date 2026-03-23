use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};
use uuid::Uuid;

use super::{Action, SyncedListElement};

/// A step/change in a SyncedList
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub struct SyncedListStep {
    pub id: String,
    pub timestamp: i64,
    pub change_action: Action,
    pub change_id: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub change_value_element: Option<SyncedListElement>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub change_value_string: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub change_value_int: Option<i32>,
}

impl SyncedListStep {
    pub fn new_add(element: SyncedListElement) -> Self {
        Self {
            id: Uuid::new_v4().to_string(),
            timestamp: Utc::now().timestamp_millis(),
            change_action: Action::Add,
            change_id: element.id.clone(),
            change_value_element: Some(element),
            change_value_string: None,
            change_value_int: None,
        }
    }

    pub fn new_remove(element_id: String) -> Self {
        Self {
            id: Uuid::new_v4().to_string(),
            timestamp: Utc::now().timestamp_millis(),
            change_action: Action::Remove,
            change_id: element_id,
            change_value_element: None,
            change_value_string: None,
            change_value_int: None,
        }
    }

    pub fn new_update(element: SyncedListElement) -> Self {
        Self {
            id: Uuid::new_v4().to_string(),
            timestamp: Utc::now().timestamp_millis(),
            change_action: Action::Update,
            change_id: element.id.clone(),
            change_value_element: Some(element),
            change_value_string: None,
            change_value_int: None,
        }
    }

    pub fn new_move(element_id: String, new_index: i32) -> Self {
        Self {
            id: Uuid::new_v4().to_string(),
            timestamp: Utc::now().timestamp_millis(),
            change_action: Action::Move,
            change_id: element_id,
            change_value_element: None,
            change_value_string: None,
            change_value_int: Some(new_index),
        }
    }

    pub fn new_swap(element_id1: String, element_id2: String) -> Self {
        Self {
            id: Uuid::new_v4().to_string(),
            timestamp: Utc::now().timestamp_millis(),
            change_action: Action::Swap,
            change_id: element_id1,
            change_value_element: None,
            change_value_string: Some(element_id2),
            change_value_int: None,
        }
    }

    pub fn new_clear() -> Self {
        Self {
            id: Uuid::new_v4().to_string(),
            timestamp: Utc::now().timestamp_millis(),
            change_action: Action::Clear,
            change_id: String::new(),
            change_value_element: None,
            change_value_string: None,
            change_value_int: None,
        }
    }
}
