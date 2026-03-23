use ribir::prelude::*;
use uuid::Uuid;

use crate::data::{SyncedList, SyncedListElement, SyncedListHeader, SyncedListStep};
use crate::storage::{FileStorage, SecureStorage};
use crate::sync::ServerWrapper;

pub struct OpenSyncedListsApp {
    lists: Vec<SyncedList>,
    current_list_index: Option<usize>,
    storage: FileStorage,
    server: ServerWrapper,
    show_new_list_dialog: bool,
    new_list_name: String,
    new_list_hostname: String,
    new_element_name: String,
    new_element_description: String,
    filter_text: String,
    show_settings: bool,
    is_syncing: bool,
    sync_status: String,
}

impl Default for OpenSyncedListsApp {
    fn default() -> Self {
        let storage = FileStorage::new().expect("Failed to initialize storage");
        let mut app = Self {
            lists: Vec::new(),
            current_list_index: None,
            storage,
            server: ServerWrapper::new(),
            show_new_list_dialog: false,
            new_list_name: String::new(),
            new_list_hostname: String::new(),
            new_element_name: String::new(),
            new_element_description: String::new(),
            filter_text: String::new(),
            show_settings: false,
            is_syncing: false,
            sync_status: String::new(),
        };

        app.load_lists();
        app
    }
}

impl OpenSyncedListsApp {
    fn load_lists(&mut self) {
        if let Ok(list_ids) = self.storage.list_all_lists() {
            let mut loaded_lists = Vec::new();
            for id in list_ids {
                if let Ok(list) = self.storage.load_list(&id) {
                    loaded_lists.push(list);
                }
            }
            self.lists = loaded_lists;
        }
    }

    fn current_list(&self) -> Option<&SyncedList> {
        self.current_list_index.and_then(|i| self.lists.get(i))
    }

    fn current_list_mut(&mut self) -> Option<&mut SyncedList> {
        self.current_list_index.and_then(|i| self.lists.get_mut(i))
    }

    fn save_current_list(&self) {
        if let Some(list) = self.current_list() {
            let _ = self.storage.save_list(list);
        }
    }

    fn add_new_list(&mut self) {
        if self.new_list_name.is_empty() {
            return;
        }

        let id = Uuid::new_v4().to_string();
        let secret = SecureStorage::generate_secret();
        let local_secret = SecureStorage::generate_local_secret();

        let header = SyncedListHeader::new(
            id,
            self.new_list_name.clone(),
            self.new_list_hostname.clone(),
            secret,
            local_secret,
        );
        let list = SyncedList::new(header);

        self.lists.push(list);
        self.current_list_index = Some(self.lists.len() - 1);
        self.show_new_list_dialog = false;
        self.new_list_name.clear();
        self.new_list_hostname.clear();

        self.save_current_list();
    }

    fn add_element(&mut self) {
        if self.new_element_name.is_empty() {
            return;
        }

        if let Some(list) = self.current_list_mut() {
            let id = list.generate_unique_element_id();
            let element = SyncedListElement::new(
                id,
                self.new_element_name.clone(),
                self.new_element_description.clone(),
            );

            let step = SyncedListStep::new_add(element);
            list.add_element_step(step);

            self.new_element_name.clear();
            self.new_element_description.clear();
            self.save_current_list();
        }
    }

    fn toggle_element_checked(&mut self, element_id: &str) {
        if let Some(list) = self.current_list_mut() {
            if let Some(mut element) = list
                .elements_buffer
                .iter()
                .find(|e| e.id == element_id)
                .cloned()
            {
                element.checked = !element.checked;
                let step = SyncedListStep::new_update(element);
                list.add_element_step(step);
                self.save_current_list();
            }
        }
    }

    fn remove_element(&mut self, element_id: &str) {
        if let Some(list) = self.current_list_mut() {
            let step = SyncedListStep::new_remove(element_id.to_string());
            list.add_element_step(step);
            self.save_current_list();
        }
    }

    fn move_element_up(&mut self, element_id: &str) {
        if let Some(list) = self.current_list_mut() {
            if let Some(pos) = list.elements_buffer.iter().position(|e| e.id == element_id) {
                if pos > 0 {
                    let step = SyncedListStep::new_move(element_id.to_string(), (pos - 1) as i32);
                    list.add_element_step(step);
                    self.save_current_list();
                }
            }
        }
    }

    fn move_element_down(&mut self, element_id: &str) {
        if let Some(list) = self.current_list_mut() {
            if let Some(pos) = list.elements_buffer.iter().position(|e| e.id == element_id) {
                if pos < list.elements_buffer.len() - 1 {
                    let step = SyncedListStep::new_move(element_id.to_string(), (pos + 1) as i32);
                    list.add_element_step(step);
                    self.save_current_list();
                }
            }
        }
    }
}

impl Compose for OpenSyncedListsApp {
    fn compose(this: State<Self>) -> Widget {
        fn_widget! {
            let app_data = Stateful::new(OpenSyncedListsApp::default());

            @Row {
                // Sidebar with lists
                @Container {
                    size: Size::new(250., f32::INFINITY),
                    background: Color::from_hex("#F5F5F5"),
                    border: Border::all(BorderSide::new(1., Color::from_hex("#E0E0E0"))),
                    @Column {
                        // Header
                        @Container {
                            padding: EdgeInsets::all(16.),
                            background: Color::from_hex("#1976D2"),
                            @Row {
                                @Text {
                                    text: "Lists",
                                    color: Color::WHITE,
                                    text_style: TypographyTheme::of(BuildCtx::get()).headline_small.clone().into()
                                }
                                @Spacer {}
                                @button! {
                                    cursor: CursorIcon::Pointer,
                                    background: Color::from_hex("#1565C0"),
                                    foreground: Color::WHITE,
                                    padding: EdgeInsets::all(8.),
                                    border_radius: Radius::all(4.),
                                    on_tap: move |_| {
                                        $app_data.write().show_new_list_dialog = true;
                                    },
                                    @Text { text: "+" }
                                }
                            }
                        }

                        // Lists
                        @Expanded {
                            @ScrollView {
                                @Column {
                                    children: pipe! {
                                        let app = $app_data.read();
                                        app.lists.iter().enumerate().map(|(index, list)| {
                                            let is_selected = app.current_list_index == Some(index);
                                            let list_name = list.header.name.clone();
                                            let list_size = list.header.list_size.clone();

                                            widget! {
                                                @Container {
                                                    padding: EdgeInsets::all(8.),
                                                    margin: EdgeInsets::all(4.),
                                                    background: if is_selected {
                                                        Color::from_hex("#E3F2FD")
                                                    } else {
                                                        Color::TRANSPARENT
                                                    },
                                                    border: if is_selected {
                                                        Border::all(BorderSide::new(2., Color::from_hex("#1976D2")))
                                                    } else {
                                                        Border::all(BorderSide::new(1., Color::TRANSPARENT))
                                                    },
                                                    cursor: CursorIcon::Pointer,
                                                    border_radius: Radius::all(4.),
                                                    on_tap: move |_| {
                                                        $app_data.write().current_list_index = Some(index);
                                                    },
                                                    @Column {
                                                        cross_axis: CrossAxis::Start,
                                                        @Text {
                                                            text: list_name,
                                                            text_style: if is_selected {
                                                                TypographyTheme::of(BuildCtx::get()).body_large.clone().bold().into()
                                                            } else {
                                                                TypographyTheme::of(BuildCtx::get()).body_large.clone().into()
                                                            }
                                                        }
                                                        @Text {
                                                            text: list_size,
                                                            color: Color::from_hex("#757575"),
                                                            text_style: TypographyTheme::of(BuildCtx::get()).body_small.clone().into()
                                                        }
                                                    }
                                                }
                                            }.into()
                                        }).collect::<Vec<Widget>>()
                                    }
                                }
                            }
                        }
                    }
                }

                // Main content area
                @Expanded {
                    @Container {
                        padding: EdgeInsets::all(16.),
                        background: Color::WHITE,
                        @pipe! {
                            if let Some(list) = $app_data.read().current_list() {
                                let list = list.clone();
                                widget! {
                                    @Column {
                                        // Header
                                        @Row {
                                            @Text {
                                                text: list.header.name.clone(),
                                                text_style: TypographyTheme::of(BuildCtx::get()).headline_medium.clone().into()
                                            }
                                            @Spacer {}
                                            @button! {
                                                cursor: CursorIcon::Pointer,
                                                padding: EdgeInsets::all(8.),
                                                border_radius: Radius::all(4.),
                                                background: Color::from_hex("#F5F5F5"),
                                                on_tap: move |_| {
                                                    $app_data.write().show_settings = true;
                                                },
                                                @Text { text: "⚙" }
                                            }
                                            @if !list.header.hostname.is_empty() {
                                                @button! {
                                                    cursor: CursorIcon::Pointer,
                                                    padding: EdgeInsets::all(8.),
                                                    border_radius: Radius::all(4.),
                                                    background: Color::from_hex("#4CAF50"),
                                                    foreground: Color::WHITE,
                                                    on_tap: move |_| {
                                                        $app_data.write().sync_status = "Sync not implemented yet".to_string();
                                                    },
                                                    @Text { text: "🔄" }
                                                }
                                            }
                                        }

                                        // Add element section
                                        @Container {
                                            margin: EdgeInsets::vertical(16.),
                                            padding: EdgeInsets::all(16.),
                                            background: Color::from_hex("#F8F9FA"),
                                            border_radius: Radius::all(8.),
                                            @Column {
                                                @Row {
                                                    @Expanded {
                                                        @Input {
                                                            text: pipe!($app_data.new_element_name.clone()),
                                                            placeholder: "Add new item...",
                                                            on_changed: move |text| {
                                                                $app_data.write().new_element_name = text;
                                                            }
                                                        }
                                                    }
                                                    @button! {
                                                        cursor: CursorIcon::Pointer,
                                                        margin: EdgeInsets::only_left(8.),
                                                        padding: EdgeInsets::symmetric(horizontal: 16., vertical: 8.),
                                                        background: Color::from_hex("#1976D2"),
                                                        foreground: Color::WHITE,
                                                        border_radius: Radius::all(4.),
                                                        on_tap: move |_| {
                                                            $app_data.write().add_element();
                                                        },
                                                        @Text { text: "Add" }
                                                    }
                                                }
                                                @if !$app_data.read().new_element_name.is_empty() {
                                                    @Container {
                                                        margin: EdgeInsets::only_top(8.),
                                                        @Input {
                                                            text: pipe!($app_data.new_element_description.clone()),
                                                            placeholder: "Description (optional)...",
                                                            on_changed: move |text| {
                                                                $app_data.write().new_element_description = text;
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        // Elements list
                                        @Expanded {
                                            @ScrollView {
                                                @Column {
                                                    children: pipe! {
                                                        let app = $app_data.read();
                                                        if let Some(current_list) = app.current_list() {
                                                            let elements = if current_list.header.is_checked_list() {
                                                                let mut elements = current_list.unchecked_elements_buffer.clone();
                                                                elements.extend(current_list.checked_elements_buffer.clone());
                                                                elements
                                                            } else {
                                                                current_list.elements_buffer.clone()
                                                            };

                                                            elements.iter().map(|element| {
                                                                let element_id = element.id.clone();
                                                                let element_name = element.name.clone();
                                                                let element_description = element.description.clone();
                                                                let element_checked = element.checked;
                                                                let is_checked_list = current_list.header.is_checked_list();

                                                                widget! {
                                                                    @Container {
                                                                        padding: EdgeInsets::all(12.),
                                                                        margin: EdgeInsets::vertical(4.),
                                                                        background: Color::WHITE,
                                                                        border: Border::all(BorderSide::new(1., Color::from_hex("#E0E0E0"))),
                                                                        border_radius: Radius::all(8.),
                                                                        @Row {
                                                                            @if is_checked_list {
                                                                                @Checkbox {
                                                                                    checked: element_checked,
                                                                                    on_changed: move |_| {
                                                                                        $app_data.write().toggle_element_checked(&element_id);
                                                                                    }
                                                                                }
                                                                            }
                                                                            @Expanded {
                                                                                @Column {
                                                                                    cross_axis: CrossAxis::Start,
                                                                                    @Text {
                                                                                        text: element_name,
                                                                                        text_decoration: if element_checked {
                                                                                            TextDecoration::LINE_THROUGH
                                                                                        } else {
                                                                                            TextDecoration::NONE
                                                                                        },
                                                                                        color: if element_checked {
                                                                                            Color::from_hex("#757575")
                                                                                        } else {
                                                                                            Color::BLACK
                                                                                        }
                                                                                    }
                                                                                    @if !element_description.is_empty() {
                                                                                        @Text {
                                                                                            text: element_description,
                                                                                            color: Color::from_hex("#757575"),
                                                                                            text_style: TypographyTheme::of(BuildCtx::get()).body_small.clone().into()
                                                                                        }
                                                                                    }
                                                                                }
                                                                            }
                                                                            @Row {
                                                                                @button! {
                                                                                    cursor: CursorIcon::Pointer,
                                                                                    padding: EdgeInsets::all(4.),
                                                                                    margin: EdgeInsets::all(2.),
                                                                                    background: Color::from_hex("#F5F5F5"),
                                                                                    border_radius: Radius::all(4.),
                                                                                    on_tap: move |_| {
                                                                                        $app_data.write().move_element_up(&element_id);
                                                                                    },
                                                                                    @Text { text: "⬆" }
                                                                                }
                                                                                @button! {
                                                                                    cursor: CursorIcon::Pointer,
                                                                                    padding: EdgeInsets::all(4.),
                                                                                    margin: EdgeInsets::all(2.),
                                                                                    background: Color::from_hex("#F5F5F5"),
                                                                                    border_radius: Radius::all(4.),
                                                                                    on_tap: move |_| {
                                                                                        $app_data.write().move_element_down(&element_id);
                                                                                    },
                                                                                    @Text { text: "⬇" }
                                                                                }
                                                                                @button! {
                                                                                    cursor: CursorIcon::Pointer,
                                                                                    padding: EdgeInsets::all(4.),
                                                                                    margin: EdgeInsets::all(2.),
                                                                                    background: Color::from_hex("#F44336"),
                                                                                    foreground: Color::WHITE,
                                                                                    border_radius: Radius::all(4.),
                                                                                    on_tap: move |_| {
                                                                                        $app_data.write().remove_element(&element_id);
                                                                                    },
                                                                                    @Text { text: "🗑" }
                                                                                }
                                                                            }
                                                                        }
                                                                    }
                                                                }.into()
                                                            }).collect::<Vec<Widget>>()
                                                        } else {
                                                            Vec::new()
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        @if !$app_data.read().sync_status.is_empty() {
                                            @Container {
                                                margin: EdgeInsets::only_top(16.),
                                                padding: EdgeInsets::all(12.),
                                                background: Color::from_hex("#E3F2FD"),
                                                border_radius: Radius::all(4.),
                                                @Text {
                                                    text: pipe!($app_data.sync_status.clone()),
                                                    color: Color::from_hex("#1976D2")
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                widget! {
                                    @Container {
                                        @Column {
                                            main_axis: MainAxis::Center,
                                            cross_axis: CrossAxis::Center,
                                            @Text {
                                                text: "Welcome to OpenSyncedLists",
                                                text_style: TypographyTheme::of(BuildCtx::get()).headline_large.clone().into()
                                            }
                                            @Container {
                                                margin: EdgeInsets::only_top(16.),
                                                @Text {
                                                    text: "Select a list from the sidebar or create a new one to get started.",
                                                    text_style: TypographyTheme::of(BuildCtx::get()).body_large.clone().into(),
                                                    color: Color::from_hex("#757575")
                                                }
                                            }
                                            @Container {
                                                margin: EdgeInsets::only_top(24.),
                                                @button! {
                                                    cursor: CursorIcon::Pointer,
                                                    padding: EdgeInsets::symmetric(horizontal: 24., vertical: 12.),
                                                    background: Color::from_hex("#1976D2"),
                                                    foreground: Color::WHITE,
                                                    border_radius: Radius::all(8.),
                                                    on_tap: move |_| {
                                                        $app_data.write().show_new_list_dialog = true;
                                                    },
                                                    @Text { text: "Create Your First List" }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // New List Dialog
            @if $app_data.read().show_new_list_dialog {
                @Container {
                    size: Size::new(f32::INFINITY, f32::INFINITY),
                    background: Color::from_rgba(0., 0., 0., 0.5),
                    @Container {
                        size: Size::new(400., 300.),
                        background: Color::WHITE,
                        border_radius: Radius::all(12.),
                        padding: EdgeInsets::all(24.),
                        @Column {
                            @Text {
                                text: "Create New List",
                                text_style: TypographyTheme::of(BuildCtx::get()).headline_medium.clone().into()
                            }
                            @Container {
                                margin: EdgeInsets::vertical(16.),
                                @Column {
                                    @Text {
                                        text: "List Name:",
                                        text_style: TypographyTheme::of(BuildCtx::get()).body_medium.clone().into()
                                    }
                                    @Container {
                                        margin: EdgeInsets::only_top(8.),
                                        @Input {
                                            text: pipe!($app_data.new_list_name.clone()),
                                            placeholder: "Enter list name...",
                                            on_changed: move |text| {
                                                $app_data.write().new_list_name = text;
                                            }
                                        }
                                    }
                                    @Container {
                                        margin: EdgeInsets::only_top(16.),
                                        @Text {
                                            text: "Server URL (optional):",
                                            text_style: TypographyTheme::of(BuildCtx::get()).body_medium.clone().into()
                                        }
                                    }
                                    @Container {
                                        margin: EdgeInsets::only_top(8.),
                                        @Input {
                                            text: pipe!($app_data.new_list_hostname.clone()),
                                            placeholder: "https://your-server.com",
                                            on_changed: move |text| {
                                                $app_data.write().new_list_hostname = text;
                                            }
                                        }
                                    }
                                }
                            }
                            @Spacer {}
                            @Row {
                                @Spacer {}
                                @button! {
                                    cursor: CursorIcon::Pointer,
                                    padding: EdgeInsets::symmetric(horizontal: 16., vertical: 8.),
                                    margin: EdgeInsets::only_right(8.),
                                    background: Color::from_hex("#F5F5F5"),
                                    border_radius: Radius::all(4.),
                                    on_tap: move |_| {
                                        let mut app = $app_data.write();
                                        app.show_new_list_dialog = false;
                                        app.new_list_name.clear();
                                        app.new_list_hostname.clear();
                                    },
                                    @Text { text: "Cancel" }
                                }
                                @button! {
                                    cursor: CursorIcon::Pointer,
                                    padding: EdgeInsets::symmetric(horizontal: 16., vertical: 8.),
                                    background: Color::from_hex("#1976D2"),
                                    foreground: Color::WHITE,
                                    border_radius: Radius::all(4.),
                                    on_tap: move |_| {
                                        $app_data.write().add_new_list();
                                    },
                                    @Text { text: "Create" }
                                }
                            }
                        }
                    }
                }
            }
        }
        .into()
    }
}
