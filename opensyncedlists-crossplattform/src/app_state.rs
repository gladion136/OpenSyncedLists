use log::{error, info};
use uuid::Uuid;

use crate::data::{SyncedList, SyncedListElement, SyncedListHeader, SyncedListStep};
use crate::storage::{FileStorage, SecureStorage};
use crate::sync::ServerWrapper;

#[derive(Clone)]
pub struct AppState {
    pub lists: Vec<SyncedList>,
    pub current_list_index: Option<usize>,
    pub storage: FileStorage,
    pub server: ServerWrapper,

    // UI State
    pub show_new_list_dialog: bool,
    pub show_add_element_dialog: bool,
    pub show_import_dialog: bool,
    pub show_list_settings: bool,
    pub show_element_editor: bool,

    // New List Dialog
    pub new_list_name: String,
    pub new_list_hostname: String,
    pub new_list_is_task_list: bool,

    // New Element
    pub new_element_name: String,
    pub new_element_description: String,

    // Edit Element
    pub editing_element_id: Option<String>,
    pub edit_element_name: String,
    pub edit_element_description: String,

    // Status
    pub status_message: String,
    pub is_syncing: bool,

    // Drag & Drop
    pub dragging_element_id: Option<String>,
}

impl AppState {
    pub fn new() -> Self {
        let storage = FileStorage::new().expect("Failed to initialize storage");
        let mut app = Self {
            lists: Vec::new(),
            current_list_index: None,
            storage,
            server: ServerWrapper::new(),

            show_new_list_dialog: false,
            show_add_element_dialog: false,
            show_import_dialog: false,
            show_list_settings: false,
            show_element_editor: false,

            new_list_name: String::new(),
            new_list_hostname: String::new(),
            new_list_is_task_list: true,

            new_element_name: String::new(),
            new_element_description: String::new(),

            editing_element_id: None,
            edit_element_name: String::new(),
            edit_element_description: String::new(),

            status_message: String::new(),
            is_syncing: false,

            dragging_element_id: None,
        };

        app.load_lists();
        app
    }

    pub fn load_lists(&mut self) {
        info!("Loading lists from storage");
        if let Ok(list_ids) = self.storage.list_all_lists() {
            let mut loaded_lists = Vec::new();
            for id in list_ids {
                if let Ok(mut list) = self.storage.load_list(&id) {
                    list.recalculate_buffers();
                    loaded_lists.push(list);
                    info!("Loaded list: {}", id);
                }
            }
            self.lists = loaded_lists;
            self.set_status(format!("Loaded {} lists", self.lists.len()));
        } else {
            self.set_status("No lists found - create your first list!".to_string());
        }
    }

    pub fn current_list(&self) -> Option<&SyncedList> {
        self.current_list_index.and_then(|i| self.lists.get(i))
    }

    pub fn current_list_mut(&mut self) -> Option<&mut SyncedList> {
        self.current_list_index.and_then(|i| self.lists.get_mut(i))
    }

    pub fn select_list(&mut self, index: usize) {
        self.current_list_index = Some(index);
        if let Some(list) = self.current_list() {
            self.set_status(format!("Selected list: {}", list.header.name));
        }
    }

    pub fn create_new_list(&mut self) {
        if self.new_list_name.is_empty() {
            self.set_status("Please enter a list name".to_string());
            return;
        }

        let id = Uuid::new_v4().to_string();
        let secret = SecureStorage::generate_secret();
        let local_secret = SecureStorage::generate_local_secret();

        let mut header = SyncedListHeader::new(
            id,
            self.new_list_name.clone(),
            self.new_list_hostname.clone(),
            secret,
            local_secret,
        );

        // Set list type based on checkbox
        header.check_option = true;
        header.checked_list = self.new_list_is_task_list;

        let mut list = SyncedList::new(header);
        list.recalculate_buffers();

        // Save to storage
        if let Err(e) = self.storage.save_list(&list) {
            error!("Failed to save list: {}", e);
            self.set_status("Failed to save list".to_string());
            return;
        }

        let list_name = list.header.name.clone();
        self.lists.push(list);
        self.current_list_index = Some(self.lists.len() - 1);

        info!("Created new list: {}", list_name);
        self.set_status(format!("Created list: {}", list_name));

        // Reset dialog
        self.cancel_new_list();
    }

    pub fn cancel_new_list(&mut self) {
        self.show_new_list_dialog = false;
        self.new_list_name.clear();
        self.new_list_hostname.clear();
        self.new_list_is_task_list = true;
    }

    pub fn delete_list(&mut self, index: usize) {
        if let Some(list) = self.lists.get(index) {
            let list_name = list.header.name.clone();
            let list_id = list.header.id.clone();

            // Delete from storage
            if let Err(e) = self.storage.delete_list(&list_id) {
                error!("Failed to delete list from storage: {}", e);
                self.set_status("Failed to delete list".to_string());
                return;
            }

            // Remove from memory
            self.lists.remove(index);

            // Adjust current selection
            if let Some(current_index) = self.current_list_index {
                if current_index == index {
                    self.current_list_index = None;
                } else if current_index > index {
                    self.current_list_index = Some(current_index - 1);
                }
            }

            info!("Deleted list: {}", list_name);
            self.set_status(format!("Deleted list: {}", list_name));
        }
    }

    pub fn edit_list(&mut self, index: usize) {
        if let Some(list) = self.lists.get(index) {
            self.new_list_name = list.header.name.clone();
            self.new_list_hostname = list.header.hostname.clone();
            self.new_list_is_task_list = list.header.is_checked_list();
            self.show_new_list_dialog = true;

            // TODO: Implement edit mode vs create mode
            self.set_status("Edit mode not fully implemented yet".to_string());
        }
    }

    pub fn add_element(&mut self) {
        if self.new_element_name.is_empty() {
            self.set_status("Please enter an element name".to_string());
            return;
        }

        let element_name = self.new_element_name.clone();
        let element_description = self.new_element_description.clone();

        if let Some(list) = self.current_list_mut() {
            let id = list.generate_unique_element_id();
            let element = SyncedListElement::new(id, element_name.clone(), element_description);

            let step = SyncedListStep::new_add(element);
            list.add_element_step(step);

            // Save to storage - clone the list to avoid borrow issues
            let list_clone = list.clone();
            if let Err(e) = self.storage.save_list(&list_clone) {
                error!("Failed to save list: {}", e);
                self.set_status("Failed to save changes".to_string());
                return;
            }

            info!("Added element: {}", element_name);
            self.set_status(format!("Added: {}", element_name));

            // Clear inputs
            self.new_element_name.clear();
            self.new_element_description.clear();
        } else {
            self.set_status("No list selected".to_string());
        }
    }

    pub fn toggle_element_checked(&mut self, element_id: &str) {
        if let Some(list) = self.current_list_mut() {
            if let Some(mut element) = list
                .elements_buffer
                .iter()
                .find(|e| e.id == element_id)
                .cloned()
            {
                element.checked = !element.checked;
                let element_name = element.name.clone();
                let is_checked = element.checked;
                let step = SyncedListStep::new_update(element);
                list.add_element_step(step);

                // Save to storage - clone the list to avoid borrow issues
                let list_clone = list.clone();
                if let Err(e) = self.storage.save_list(&list_clone) {
                    error!("Failed to save list: {}", e);
                    self.set_status("Failed to save changes".to_string());
                    return;
                }

                let status = if is_checked {
                    "completed"
                } else {
                    "uncompleted"
                };
                self.set_status(format!("Marked '{}' as {}", element_name, status));
            }
        }
    }

    pub fn remove_element(&mut self, element_id: &str) {
        let element_name = if let Some(list) = self.current_list() {
            list.elements_buffer
                .iter()
                .find(|e| e.id == element_id)
                .map(|e| e.name.clone())
                .unwrap_or_else(|| "Unknown".to_string())
        } else {
            return;
        };

        if let Some(list) = self.current_list_mut() {
            let step = SyncedListStep::new_remove(element_id.to_string());
            list.add_element_step(step);

            // Save to storage - clone the list to avoid borrow issues
            let list_clone = list.clone();
            if let Err(e) = self.storage.save_list(&list_clone) {
                error!("Failed to save list: {}", e);
                self.set_status("Failed to save changes".to_string());
                return;
            }

            info!("Removed element: {}", element_name);
            self.set_status(format!("Removed: {}", element_name));
        }
    }

    pub fn move_element_up(&mut self, element_id: &str) {
        if let Some(list) = self.current_list_mut() {
            if let Some(pos) = list.elements_buffer.iter().position(|e| e.id == element_id) {
                if pos > 0 {
                    let step = SyncedListStep::new_move(element_id.to_string(), (pos - 1) as i32);
                    list.add_element_step(step);

                    // Save to storage - clone the list to avoid borrow issues
                    let list_clone = list.clone();
                    if let Err(e) = self.storage.save_list(&list_clone) {
                        error!("Failed to save list: {}", e);
                        self.set_status("Failed to save changes".to_string());
                        return;
                    }

                    self.set_status("Moved element up".to_string());
                } else {
                    self.set_status("Element is already at the top".to_string());
                }
            }
        }
    }

    pub fn move_element_down(&mut self, element_id: &str) {
        if let Some(list) = self.current_list_mut() {
            if let Some(pos) = list.elements_buffer.iter().position(|e| e.id == element_id) {
                if pos < list.elements_buffer.len() - 1 {
                    let step = SyncedListStep::new_move(element_id.to_string(), (pos + 1) as i32);
                    list.add_element_step(step);

                    // Save to storage - clone the list to avoid borrow issues
                    let list_clone = list.clone();
                    if let Err(e) = self.storage.save_list(&list_clone) {
                        error!("Failed to save list: {}", e);
                        self.set_status("Failed to save changes".to_string());
                        return;
                    }

                    self.set_status("Moved element down".to_string());
                } else {
                    self.set_status("Element is already at the bottom".to_string());
                }
            }
        }
    }

    pub fn move_element_to_top(&mut self, element_id: &str) {
        if let Some(list) = self.current_list_mut() {
            if let Some(pos) = list.elements_buffer.iter().position(|e| e.id == element_id) {
                if pos > 0 {
                    let element = list.elements_buffer.remove(pos);
                    list.elements_buffer.insert(0, element);

                    let step = SyncedListStep::new_move(element_id.to_string(), 0);
                    list.add_element_step(step);

                    // Save to storage
                    let list_clone = list.clone();
                    if let Err(e) = self.storage.save_list(&list_clone) {
                        error!("Failed to save list: {}", e);
                        self.set_status("Failed to save changes".to_string());
                        return;
                    }

                    self.set_status("Moved element to top".to_string());
                } else {
                    self.set_status("Element is already at the top".to_string());
                }
            }
        }
    }

    pub fn move_element_to_bottom(&mut self, element_id: &str) {
        if let Some(list) = self.current_list_mut() {
            let len = list.elements_buffer.len();
            if let Some(pos) = list.elements_buffer.iter().position(|e| e.id == element_id) {
                if pos < len - 1 {
                    let element = list.elements_buffer.remove(pos);
                    list.elements_buffer.push(element);

                    let step = SyncedListStep::new_move(element_id.to_string(), (len - 1) as i32);
                    list.add_element_step(step);

                    // Save to storage
                    let list_clone = list.clone();
                    if let Err(e) = self.storage.save_list(&list_clone) {
                        error!("Failed to save list: {}", e);
                        self.set_status("Failed to save changes".to_string());
                        return;
                    }

                    self.set_status("Moved element to bottom".to_string());
                } else {
                    self.set_status("Element is already at the bottom".to_string());
                }
            }
        }
    }

    pub fn start_edit_element(&mut self, element_id: String) {
        // Clone the data first to avoid borrow checker issues
        let element_data = if let Some(list) = self.current_list() {
            list.elements_buffer
                .iter()
                .find(|e| e.id == element_id)
                .map(|e| (e.name.clone(), e.description.clone()))
        } else {
            None
        };

        if let Some((name, description)) = element_data {
            self.editing_element_id = Some(element_id);
            self.edit_element_name = name;
            self.edit_element_description = description;
            self.show_element_editor = true;
        }
    }

    pub fn cancel_add_element(&mut self) {
        self.show_add_element_dialog = false;
        self.new_element_name.clear();
        self.new_element_description.clear();
    }

    pub fn save_element_changes(&mut self) {
        // Extract the values first to avoid borrowing issues
        let editing_id = self.editing_element_id.clone();
        let new_name = self.edit_element_name.clone();
        let new_description = self.edit_element_description.clone();

        if let Some(id) = editing_id {
            if let Some(list) = self.current_list_mut() {
                if let Some(mut element) = list.elements_buffer.iter().find(|e| e.id == id).cloned()
                {
                    element.name = new_name;
                    element.description = new_description;

                    let step = SyncedListStep::new_update(element);
                    list.add_element_step(step);

                    // Save to storage - clone the list to avoid borrow issues
                    let list_clone = list.clone();
                    if let Err(e) = self.storage.save_list(&list_clone) {
                        error!("Failed to save list: {}", e);
                        self.set_status("Failed to save changes".to_string());
                        return;
                    }

                    self.set_status("Eintrag aktualisiert".to_string());
                }
            }
        }
        self.show_element_editor = false;
        self.editing_element_id = None;
        self.edit_element_name.clear();
        self.edit_element_description.clear();
    }

    pub fn sync_current_list(&mut self) {
        let (list_name, has_server) = if let Some(list) = self.current_list() {
            (list.header.name.clone(), !list.header.hostname.is_empty())
        } else {
            self.set_status("No list selected".to_string());
            return;
        };

        if !has_server {
            self.set_status("No server configured for this list".to_string());
            return;
        }

        self.is_syncing = true;
        self.set_status("Sync functionality not fully implemented yet".to_string());

        // TODO: Implement actual sync with tokio runtime
        info!("Sync requested for list: {}", list_name);
    }

    pub fn export_current_list(&mut self) {
        let list_name = if let Some(list) = self.current_list() {
            list.header.name.clone()
        } else {
            self.set_status("No list selected".to_string());
            return;
        };

        // For now, just show status. In a real implementation, this would
        // open a file dialog and export the list
        self.set_status(format!("Export '{}' - Feature coming soon!", list_name));
        info!("Export requested for list: {}", list_name);
    }

    fn set_status(&mut self, message: String) {
        self.status_message = message;
        // TODO: Clear status after timeout
    }

    pub fn save_current_list(&self) {
        if let Some(list) = self.current_list() {
            if let Err(e) = self.storage.save_list(list) {
                error!("Failed to save list: {}", e);
            }
        }
    }

    pub fn start_drag(&mut self, element_id: String) {
        info!("Start drag: {}", element_id);
        self.dragging_element_id = Some(element_id);
    }

    pub fn handle_drop(&mut self, target_element_id: String) {
        let dragging_id = self.dragging_element_id.clone();

        info!(
            "Handle drop - dragging: {:?}, target: {}",
            dragging_id, target_element_id
        );

        if let Some(dragging_id) = dragging_id {
            if dragging_id != target_element_id {
                if let Some(list) = self.current_list_mut() {
                    let dragging_pos = list
                        .elements_buffer
                        .iter()
                        .position(|e| e.id == dragging_id);
                    let target_pos = list
                        .elements_buffer
                        .iter()
                        .position(|e| e.id == target_element_id);

                    info!(
                        "Positions - dragging: {:?}, target: {:?}",
                        dragging_pos, target_pos
                    );

                    if let (Some(_), Some(target_idx)) = (dragging_pos, target_pos) {
                        let step = SyncedListStep::new_move(dragging_id, target_idx as i32);
                        list.add_element_step(step);

                        let list_clone = list.clone();
                        if let Err(e) = self.storage.save_list(&list_clone) {
                            error!("Failed to save list: {}", e);
                            self.set_status("Failed to save changes".to_string());
                        } else {
                            info!("Element successfully moved");
                            self.set_status("Element verschoben".to_string());
                        }
                    }
                }
            }
        }
        self.dragging_element_id = None;
    }

    pub fn cancel_drag(&mut self) {
        self.dragging_element_id = None;
    }
}
