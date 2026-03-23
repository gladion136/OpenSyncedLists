use anyhow::Result;
use log::{debug, error};
use serde::{Deserialize, Serialize};
use uuid::Uuid;

use super::{Action, SyncedListElement, SyncedListHeader, SyncedListStep};

/// A synced list. Stores Steps/Changes and a list Header.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SyncedList {
    pub header: SyncedListHeader,
    pub steps: Vec<SyncedListStep>,
    #[serde(skip)]
    pub elements_buffer: Vec<SyncedListElement>,
    #[serde(skip)]
    pub checked_elements_buffer: Vec<SyncedListElement>,
    #[serde(skip)]
    pub unchecked_elements_buffer: Vec<SyncedListElement>,
}

impl SyncedList {
    pub fn new(header: SyncedListHeader) -> Self {
        let mut list = Self {
            header,
            steps: Vec::new(),
            elements_buffer: Vec::new(),
            checked_elements_buffer: Vec::new(),
            unchecked_elements_buffer: Vec::new(),
        };
        list.recalculate_buffers();
        list
    }

    pub fn from_json_with_header(json: &str) -> Result<Self> {
        let mut list: SyncedList = serde_json::from_str(json)?;
        list.recalculate_buffers();
        Ok(list)
    }

    /// Calculate the elements of a list from the steps
    pub fn reformat_elements(&self) -> Vec<SyncedListElement> {
        debug!("Build list with {} steps", self.steps.len());
        let mut result = Vec::new();

        for step in &self.steps {
            match step.change_action {
                Action::Add => {
                    if let Some(element) = &step.change_value_element {
                        result.push(element.clone());
                    }
                }
                Action::Update => {
                    if let Some(change_element) = &step.change_value_element {
                        let mut success = false;
                        for elem in result.iter_mut() {
                            if elem.id == step.change_id {
                                *elem = change_element.clone();
                                success = true;
                                break;
                            }
                        }
                        if !success {
                            result.push(change_element.clone());
                        }
                    }
                }
                Action::Swap => {
                    if let Some(swap_with_id) = &step.change_value_string {
                        let mut first_idx = None;
                        let mut second_idx = None;

                        for (i, elem) in result.iter().enumerate() {
                            if elem.id == step.change_id {
                                first_idx = Some(i);
                            }
                            if elem.id == *swap_with_id {
                                second_idx = Some(i);
                            }
                        }

                        if let (Some(i), Some(j)) = (first_idx, second_idx) {
                            result.swap(i, j);
                        }
                    }
                }
                Action::Move => {
                    if let Some(dst_index) = step.change_value_int {
                        let dst_index = dst_index as usize;
                        if dst_index < result.len() {
                            let mut src_index = None;
                            for (i, elem) in result.iter().enumerate() {
                                if elem.id == step.change_id {
                                    src_index = Some(i);
                                    break;
                                }
                            }

                            if let Some(src) = src_index {
                                if src != dst_index {
                                    Self::move_item(src, dst_index, &mut result);
                                }
                            }
                        } else {
                            error!("Invalid destination index: {}", dst_index);
                        }
                    }
                }
                Action::Remove => {
                    result.retain(|elem| elem.id != step.change_id);
                }
                Action::Clear => {
                    result.clear();
                }
            }
        }

        result
    }

    /// Recalculate all buffers
    pub fn recalculate_buffers(&mut self) {
        self.elements_buffer = self.reformat_elements();

        if self.header.is_checked_list() {
            self.checked_elements_buffer = self
                .elements_buffer
                .iter()
                .filter(|elem| elem.checked)
                .cloned()
                .collect();
            self.unchecked_elements_buffer = self
                .elements_buffer
                .iter()
                .filter(|elem| !elem.checked)
                .cloned()
                .collect();
            self.header.list_size = format!(
                "{} / {}",
                self.checked_elements_buffer.len(),
                self.elements_buffer.len()
            );
        } else {
            self.header.list_size = self.elements_buffer.len().to_string();
        }
    }

    /// Add one elementStep and recalculate the buffers
    pub fn add_element_step(&mut self, step: SyncedListStep) {
        if step.change_action == Action::Clear {
            self.steps.clear();
        }
        self.steps.push(step);
        self.optimize();
        self.recalculate_buffers();
    }

    /// Move item from source index to target index
    fn move_item<T>(source_index: usize, target_index: usize, list: &mut Vec<T>) {
        if source_index >= list.len() || target_index >= list.len() {
            return;
        }

        let item = list.remove(source_index);
        let insert_index = if source_index < target_index {
            target_index
        } else {
            target_index
        };
        list.insert(insert_index, item);
    }

    /// Generate a unique element ID in the list
    pub fn generate_unique_element_id(&self) -> String {
        loop {
            let new_id = Uuid::new_v4().to_string();
            if !self.elements_buffer.iter().any(|elem| elem.id == new_id) {
                return new_id;
            }
        }
    }

    /// Get list elements as Markdown
    pub fn as_markdown(&self) -> String {
        let mut result = self.header.name.clone();

        if self.header.is_checked_list() {
            for element in &self.unchecked_elements_buffer {
                result.push('\n');
                result.push_str(&element.as_markdown(self.header.is_checked_list()));
            }
            for element in &self.checked_elements_buffer {
                result.push('\n');
                result.push_str(&element.as_markdown(self.header.is_checked_list()));
            }
        } else {
            for element in &self.elements_buffer {
                result.push('\n');
                result.push_str(&element.as_markdown(self.header.is_checked_list()));
            }
        }

        result
    }

    /// Convert to JSON with header
    pub fn to_json_with_header(&self) -> Result<String> {
        Ok(serde_json::to_string(self)?)
    }

    /// Convert to JSON (steps only)
    pub fn to_json(&self) -> Result<String> {
        #[derive(Serialize)]
        struct StepsOnly {
            steps: Vec<SyncedListStep>,
        }

        let steps_only = StepsOnly {
            steps: self.steps.clone(),
        };

        Ok(serde_json::to_string(&steps_only)?)
    }

    /// Sync this list with another list
    pub fn sync(&mut self, other: &SyncedList) -> bool {
        let old_json = self.to_json().unwrap_or_default();
        let new_list = Self::merge_lists(self, other);
        self.header = new_list.header.clone();
        self.steps = new_list.steps.clone();
        self.recalculate_buffers();

        let new_json = self.to_json().unwrap_or_default();
        old_json != new_json
    }

    /// Merge two lists and return the synchronized result
    fn merge_lists(list1: &SyncedList, list2: &SyncedList) -> SyncedList {
        let mut result_steps = list1.steps.clone();

        for step in &list2.steps {
            // Check if step already exists
            if !result_steps.iter().any(|s| s == step) {
                // Insert step at correct position based on timestamp
                let mut inserted = false;
                for (i, existing_step) in result_steps.iter().enumerate().rev() {
                    if existing_step.timestamp < step.timestamp {
                        result_steps.insert(i + 1, step.clone());
                        inserted = true;
                        break;
                    }
                }
                if !inserted {
                    result_steps.insert(0, step.clone());
                }
            }
        }

        let mut result = SyncedList {
            header: list1.header.clone(),
            steps: result_steps,
            elements_buffer: Vec::new(),
            checked_elements_buffer: Vec::new(),
            unchecked_elements_buffer: Vec::new(),
        };

        result.optimize();
        result.recalculate_buffers();
        result
    }

    /// Optimize the list steps
    pub fn optimize(&mut self) {
        // Remove steps after CLEAR action
        for i in (0..self.steps.len()).rev() {
            if self.steps[i].change_action == Action::Clear {
                self.steps.drain(0..i);
                return;
            }
        }

        if self.header.hostname.is_empty() {
            self.full_optimization();
        } else {
            self.sync_supported_optimization();
        }
    }

    /// Full optimization - most aggressive, removes all MOVE and SWAP steps
    fn full_optimization(&mut self) {
        let result = self.reformat_elements();
        self.steps.clear();

        for element in result {
            let step = SyncedListStep::new_add(element);
            self.steps.push(step);
        }
    }

    /// Sync supported optimization - less aggressive, keeps sync compatibility
    fn sync_supported_optimization(&mut self) {
        let result = self.reformat_elements();

        // Keep all steps that are not MOVE or SWAP
        let mut optimized_steps = Vec::new();
        for step in &self.steps {
            if step.change_action != Action::Move && step.change_action != Action::Swap {
                optimized_steps.push(step.clone());
            }
        }

        // Generate new MOVE steps based on current positions
        for (i, element) in result.iter().enumerate() {
            let move_step = SyncedListStep::new_move(element.id.clone(), i as i32);
            optimized_steps.push(move_step);
        }

        self.steps = optimized_steps;
    }
}
