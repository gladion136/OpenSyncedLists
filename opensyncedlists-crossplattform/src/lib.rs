#![allow(stable_features)]

use dioxus::prelude::*;
use dioxus_signals::{Readable, Signal, Writable};
use log::info;

#[cfg(target_arch = "wasm32")]
use wasm_bindgen::prelude::*;

mod app_state;
mod data;
mod storage;
mod sync;

use app_state::AppState;

#[cfg(target_arch = "wasm32")]
#[wasm_bindgen(start)]
pub fn main() {
    console_error_panic_hook::set_once();
    wasm_logger::init(wasm_logger::Config::default());

    info!("Starting OpenSyncedLists - Cross-platform Rust/Dioxus version");
    launch(app);
}

#[cfg(not(target_arch = "wasm32"))]
pub fn main() {
    info!("Starting OpenSyncedLists - Cross-platform Rust/Dioxus version");
    launch(app);
}

fn app() -> Element {
    let mut app_state = use_signal(|| AppState::new());

    rsx! {
        div {
            style: "width: 100vw; height: 100vh; background-color: #f8f9fa; display: flex; flex-direction: column; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', sans-serif;",

            // Header
            header {
                style: "height: 60px; background-color: #495057; display: flex; align-items: center; justify-content: space-between; padding: 0 20px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);",
                div {
                    style: "display: flex; align-items: center; gap: 15px;",
                    span {
                        style: "color: white; font-size: 20px; font-weight: bold;",
                        "📝 OpenSyncedLists"
                    }
                    span {
                        style: "color: #adb5bd; font-size: 14px;",
                        "{app_state.read().lists.len()} Listen"
                    }
                }
                button {
                    style: "background-color: #28a745; color: white; border: none; padding: 8px 16px; border-radius: 6px; cursor: pointer; font-size: 14px; font-weight: 500;",
                    onclick: move |_| app_state.write().show_new_list_dialog = true,
                    "➕ Neue Liste"
                }
            }

            // Main Content
            div {
                style: "flex: 1; display: flex; overflow: hidden;",

                // Sidebar - Lists
                ListSidebar { app_state: app_state }

                // Main Content Area
                MainContent { app_state: app_state }
            }

            // Status Bar
            if !app_state.read().status_message.is_empty() {
                div {
                    style: "height: 40px; background-color: #e3f2fd; border-top: 1px solid #2196f3; display: flex; align-items: center; padding: 0 20px; font-size: 14px; color: #1976d2;",
                    "ℹ️ {app_state.read().status_message}"
                }
            }

            // Dialogs
            if app_state.read().show_new_list_dialog {
                NewListDialog { app_state: app_state }
            }

            if app_state.read().show_add_element_dialog {
                AddElementDialog { app_state: app_state }
            }

            if app_state.read().show_element_editor {
                EditElementDialog { app_state: app_state }
            }
        },
    }
}

#[component]
fn ListSidebar(app_state: Signal<AppState>) -> Element {
    rsx! {
        div {
            style: "width: 300px; background-color: white; border-right: 1px solid #dee2e6; display: flex; flex-direction: column;",

            // Lists Header
            div {
                style: "padding: 20px; border-bottom: 1px solid #dee2e6;",
                h3 {
                    style: "margin: 0; color: #495057; font-size: 16px;",
                    "Meine Listen"
                }
            }

            // Lists Container
            div {
                style: "flex: 1; overflow-y: auto;",
                if app_state.read().lists.is_empty() {
                    div {
                        style: "padding: 40px 20px; text-align: center; color: #6c757d;",
                        div {
                            style: "font-size: 48px; margin-bottom: 16px;",
                            "📋"
                        }
                        div {
                            style: "font-size: 16px; margin-bottom: 8px;",
                            "Keine Listen vorhanden"
                        }
                        div {
                            style: "font-size: 14px;",
                            "Erstellen Sie Ihre erste Liste!"
                        }
                    }
                } else {
                    {
                        app_state.read().lists.iter().enumerate().map(|(index, list)| {
                            let current_index = app_state.read().current_list_index;
                            let is_selected = current_index == Some(index);

                            rsx! {
                                div {
                                    key: "{list.header.id}",
                                    style: format!(
                                        "padding: 12px 20px; border-bottom: 1px solid #f8f9fa; cursor: pointer; transition: background-color 0.2s; {}",
                                        if is_selected {
                                            "background-color: #e3f2fd; border-left: 3px solid #2196f3;"
                                        } else {
                                            "hover: background-color: #f8f9fa;"
                                        }
                                    ),
                                    onclick: move |_| app_state.write().select_list(index),

                                    div {
                                        style: "display: flex; justify-content: space-between; align-items: center;",
                                        div {
                                            div {
                                                style: "font-weight: 500; color: #495057; margin-bottom: 4px;",
                                                "{list.header.name}"
                                            }
                                            div {
                                                style: "font-size: 12px; color: #6c757d;",
                                                if list.header.is_checked_list() { "📋 To-Do Liste" } else { "📝 Einfache Liste" }
                                                " • {list.elements_buffer.len()} Einträge"
                                            }
                                        }
                                        div {
                                            style: "display: flex; gap: 8px;",
                                            button {
                                                style: "background: none; border: none; color: #6c757d; cursor: pointer; padding: 4px; border-radius: 4px; font-size: 14px;",
                                                onclick: move |e| {
                                                    e.stop_propagation();
                                                    app_state.write().edit_list(index);
                                                },
                                                "✏️"
                                            }
                                            button {
                                                style: "background: none; border: none; color: #dc3545; cursor: pointer; padding: 4px; border-radius: 4px; font-size: 14px;",
                                                onclick: move |e| {
                                                    e.stop_propagation();
                                                    app_state.write().delete_list(index);
                                                },
                                                "🗑️"
                                            }
                                        }
                                    }
                                }
                            }
                        })
                    }
                }
            }
        }
    }
}

#[component]
fn MainContent(app_state: Signal<AppState>) -> Element {
    let current_index = app_state.read().current_list_index;

    rsx! {
        div {
            style: "flex: 1; display: flex; flex-direction: column; background-color: #ffffff;",

            if let Some(index) = current_index {
                {
                    let current_list = app_state.read().lists.get(index).cloned();
                    match current_list {
                        Some(list) => rsx! {
                            // List Header
                            div {
                                style: "padding: 20px; border-bottom: 1px solid #dee2e6; background-color: #f8f9fa;",
                                div {
                                    style: "display: flex; justify-content: space-between; align-items: center;",
                                    div {
                                        h2 {
                                            style: "margin: 0; color: #495057; font-size: 24px;",
                                            "{list.header.name}"
                                        }
                                        div {
                                            style: "margin-top: 4px; font-size: 14px; color: #6c757d;",
                                            if list.header.is_checked_list() { "📋 To-Do Liste" } else { "📝 Einfache Liste" }
                                            " • {list.elements_buffer.len()} Einträge"
                                        }
                                    }
                                    div {
                                        style: "display: flex; gap: 12px;",
                                        button {
                                            style: "background-color: #28a745; color: white; border: none; padding: 8px 16px; border-radius: 6px; cursor: pointer; font-size: 14px;",
                                            onclick: move |_| app_state.write().show_add_element_dialog = true,
                                            "➕ Neuer Eintrag"
                                        }
                                        button {
                                            style: "background-color: #17a2b8; color: white; border: none; padding: 8px 16px; border-radius: 6px; cursor: pointer; font-size: 14px;",
                                            onclick: move |_| app_state.write().sync_current_list(),
                                            "🔄 Synchronisieren"
                                        }
                                    }
                                }
                            }

                            // List Content
                            div {
                                style: "flex: 1; padding: 20px; overflow-y: auto;",

                                if list.elements_buffer.is_empty() {
                                    div {
                                        style: "text-align: center; padding: 60px 20px; color: #6c757d;",
                                        div {
                                            style: "font-size: 64px; margin-bottom: 20px;",
                                            if list.header.is_checked_list() { "✅" } else { "📝" }
                                        }
                                        div {
                                            style: "font-size: 18px; margin-bottom: 12px;",
                                            "Diese Liste ist noch leer"
                                        }
                                        div {
                                            style: "font-size: 14px;",
                                            "Fügen Sie Ihren ersten Eintrag hinzu!"
                                        }
                                    }
                                } else {
                                    div {
                                        style: "display: flex; flex-direction: column; gap: 8px;",
                                        {
                                            list.elements_buffer.iter().enumerate().map(|(element_index, element)| {
                                                let element_id = element.id.clone();
                                                let element_id_drag_start = element_id.clone();
                                                let element_id_drop = element_id.clone();
                                                let element_id_style = element_id.clone();
                                                let element_id_toggle = element_id.clone();
                                                let element_id_up = element_id.clone();
                                                let element_id_down = element_id.clone();
                                                let element_id_edit = element_id.clone();
                                                let element_id_remove = element_id.clone();

                                                rsx! {
                                                    div {
                                                        key: "{element.id}",
                                                        onmouseenter: move |_| {
                                                            if app_state.read().dragging_element_id.is_some() {
                                                                // Visual feedback beim Hovern während Drag
                                                            }
                                                        },
                                                        style: format!(
                                                            "background-color: {}; border: 1px solid #dee2e6; border-radius: 8px; padding: 16px; display: flex; align-items: center; gap: 12px; transition: all 0.2s; {}",
                                                            if app_state.read().dragging_element_id.as_ref() == Some(&element_id_style) {
                                                                "#e3f2fd"
                                                            } else {
                                                                "white"
                                                            },
                                                            "hover: box-shadow: 0 2px 8px rgba(0,0,0,0.1);"
                                                        ),

                                                        if list.header.is_checked_list() {
                                                            input {
                                                                r#type: "checkbox",
                                                                checked: element.checked,
                                                                style: "width: 18px; height: 18px; cursor: pointer;",
                                                                onchange: move |_| app_state.write().toggle_element_checked(&element_id_toggle),
                                                            }
                                                        }

                                                        div {
                                                            style: "flex: 1;",
                                                            div {
                                                                style: format!(
                                                                    "font-size: 16px; color: #495057; margin-bottom: 4px; {}",
                                                                    if element.checked { "text-decoration: line-through; opacity: 0.6;" } else { "" }
                                                                ),
                                                                "{element.name}"
                                                            }
                                                            if !element.description.is_empty() {
                                                                div {
                                                                    style: format!(
                                                                        "font-size: 14px; color: #6c757d; {}",
                                                                        if element.checked { "opacity: 0.5;" } else { "" }
                                                                    ),
                                                                    "{element.description}"
                                                                }
                                                            }
                                                        }

                                                        div {
                                                            style: "display: flex; gap: 8px;",
                                                            {
                                                                let is_being_dragged = app_state.read().dragging_element_id.as_ref() == Some(&element_id_style);
                                                                let is_dragging_mode = app_state.read().dragging_element_id.is_some();

                                                                if is_dragging_mode && !is_being_dragged {
                                                                    rsx! {
                                                                        button {
                                                                            style: "background: #28a745; border: none; color: white; cursor: pointer; padding: 6px 12px; border-radius: 4px; font-size: 14px; font-weight: bold;",
                                                                            onclick: move |_| app_state.write().handle_drop(element_id_drop.clone()),
                                                                            "📍 Hier ablegen"
                                                                        }
                                                                    }
                                                                } else if is_being_dragged {
                                                                    rsx! {
                                                                        button {
                                                                            style: "background: #6c757d; border: none; color: white; cursor: pointer; padding: 6px 12px; border-radius: 4px; font-size: 14px;",
                                                                            onclick: move |_| app_state.write().cancel_drag(),
                                                                            "✖ Abbrechen"
                                                                        }
                                                                    }
                                                                } else {
                                                                    rsx! {
                                                                        button {
                                                                            style: "background: none; border: none; color: #17a2b8; cursor: grab; padding: 6px; border-radius: 4px; font-size: 16px;",
                                                                            onclick: move |_| app_state.write().start_drag(element_id_drag_start.clone()),
                                                                            title: "Element verschieben",
                                                                            "✋"
                                                                        }
                                                                        if element_index > 0 {
                                                                            button {
                                                                                style: "background: none; border: none; color: #6c757d; cursor: pointer; padding: 6px; border-radius: 4px; font-size: 16px;",
                                                                                onclick: move |_| app_state.write().move_element_up(&element_id_up),
                                                                                "⬆️"
                                                                            }
                                                                        }
                                                                        if element_index < list.elements_buffer.len() - 1 {
                                                                            button {
                                                                                style: "background: none; border: none; color: #6c757d; cursor: pointer; padding: 6px; border-radius: 4px; font-size: 16px;",
                                                                                onclick: move |_| app_state.write().move_element_down(&element_id_down),
                                                                                "⬇️"
                                                                            }
                                                                        }
                                                                        button {
                                                                            style: "background: none; border: none; color: #6c757d; cursor: pointer; padding: 6px; border-radius: 4px; font-size: 16px;",
                                                                            onclick: move |_| app_state.write().start_edit_element(element_id_edit.clone()),
                                                                            "✏️"
                                                                        }
                                                                        button {
                                                                            style: "background: none; border: none; color: #dc3545; cursor: pointer; padding: 6px; border-radius: 4px; font-size: 16px;",
                                                                            onclick: move |_| app_state.write().remove_element(&element_id_remove),
                                                                            "🗑️"
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            })
                                        }
                                    }
                                }
                            }
                        },
                        None => rsx! {
                            div {
                                style: "flex: 1; display: flex; align-items: center; justify-content: center; color: #6c757d;",
                                "Liste nicht gefunden"
                            }
                        },
                    }
                }
            } else {
                // No list selected
                div {
                    style: "flex: 1; display: flex; align-items: center; justify-content: center; flex-direction: column; color: #6c757d; text-align: center;",
                    div {
                        style: "font-size: 72px; margin-bottom: 24px;",
                        "📋"
                    }
                    div {
                        style: "font-size: 24px; margin-bottom: 12px;",
                        "Willkommen bei OpenSyncedLists"
                    }
                    div {
                        style: "font-size: 16px; margin-bottom: 24px; max-width: 400px;",
                        "Wählen Sie eine Liste aus der Seitenleiste aus oder erstellen Sie eine neue Liste, um zu beginnen."
                    }
                    button {
                        style: "background-color: #28a745; color: white; border: none; padding: 12px 24px; border-radius: 8px; cursor: pointer; font-size: 16px; font-weight: 500;",
                        onclick: move |_| app_state.write().show_new_list_dialog = true,
                        "🚀 Erste Liste erstellen"
                    }
                }
            }
        }
    }
}

// Dialog Components
#[component]
fn NewListDialog(app_state: Signal<AppState>) -> Element {
    rsx! {
        div {
            style: "position: fixed; top: 0; left: 0; width: 100%; height: 100%; background-color: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 1000;",
            onclick: move |_| app_state.write().cancel_new_list(),

            div {
                style: "background-color: white; padding: 24px; border-radius: 12px; box-shadow: 0 4px 20px rgba(0,0,0,0.15); max-width: 500px; width: 90%;",
                onclick: move |e| e.stop_propagation(),

                h3 {
                    style: "margin: 0 0 20px 0; color: #495057; font-size: 20px;",
                    "Neue Liste erstellen"
                }

                div {
                    style: "margin-bottom: 16px;",
                    label {
                        style: "display: block; margin-bottom: 8px; font-weight: 500; color: #495057;",
                        "Listen-Name:"
                    }
                    input {
                        r#type: "text",
                        placeholder: "z.B. Einkaufsliste, To-Do's...",
                        value: "{app_state.read().new_list_name}",
                        style: "width: 100%; padding: 12px; border: 1px solid #dee2e6; border-radius: 6px; font-size: 16px; box-sizing: border-box;",
                        oninput: move |e| app_state.write().new_list_name = e.value(),
                    }
                }

                div {
                    style: "margin-bottom: 16px;",
                    label {
                        style: "display: block; margin-bottom: 8px; font-weight: 500; color: #495057;",
                        "Server-Hostname (optional):"
                    }
                    input {
                        r#type: "text",
                        placeholder: "z.B. sync.example.com",
                        value: "{app_state.read().new_list_hostname}",
                        style: "width: 100%; padding: 12px; border: 1px solid #dee2e6; border-radius: 6px; font-size: 16px; box-sizing: border-box;",
                        oninput: move |e| app_state.write().new_list_hostname = e.value(),
                    }
                }

                div {
                    style: "margin-bottom: 24px;",
                    label {
                        style: "display: flex; align-items: center; gap: 8px; cursor: pointer; font-weight: 500; color: #495057;",
                        input {
                            r#type: "checkbox",
                            checked: app_state.read().new_list_is_task_list,
                            style: "width: 18px; height: 18px;",
                            onchange: move |e| app_state.write().new_list_is_task_list = e.checked(),
                        }
                        "Als To-Do Liste (mit Checkboxen)"
                    }
                }

                div {
                    style: "display: flex; gap: 12px; justify-content: flex-end;",
                    button {
                        style: "background-color: #6c757d; color: white; border: none; padding: 10px 20px; border-radius: 6px; cursor: pointer; font-size: 14px;",
                        onclick: move |_| app_state.write().cancel_new_list(),
                        "Abbrechen"
                    }
                    button {
                        style: "background-color: #28a745; color: white; border: none; padding: 10px 20px; border-radius: 6px; cursor: pointer; font-size: 14px; font-weight: 500;",
                        onclick: move |_| app_state.write().create_new_list(),
                        "Erstellen"
                    }
                }
            }
        }
    }
}

#[component]
fn AddElementDialog(app_state: Signal<AppState>) -> Element {
    rsx! {
        div {
            style: "position: fixed; top: 0; left: 0; width: 100%; height: 100%; background-color: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 1000;",
            onclick: move |_| app_state.write().cancel_add_element(),

            div {
                style: "background-color: white; padding: 24px; border-radius: 12px; box-shadow: 0 4px 20px rgba(0,0,0,0.15); max-width: 500px; width: 90%;",
                onclick: move |e| e.stop_propagation(),

                h3 {
                    style: "margin: 0 0 20px 0; color: #495057; font-size: 20px;",
                    "Neuen Eintrag hinzufügen"
                }

                div {
                    style: "margin-bottom: 16px;",
                    label {
                        style: "display: block; margin-bottom: 8px; font-weight: 500; color: #495057;",
                        "Titel:"
                    }
                    input {
                        r#type: "text",
                        placeholder: "Eintrag-Titel...",
                        value: "{app_state.read().new_element_name}",
                        style: "width: 100%; padding: 12px; border: 1px solid #dee2e6; border-radius: 6px; font-size: 16px; box-sizing: border-box;",
                        oninput: move |e| app_state.write().new_element_name = e.value(),
                    }
                }

                div {
                    style: "margin-bottom: 24px;",
                    label {
                        style: "display: block; margin-bottom: 8px; font-weight: 500; color: #495057;",
                        "Beschreibung (optional):"
                    }
                    textarea {
                        placeholder: "Weitere Details...",
                        value: "{app_state.read().new_element_description}",
                        style: "width: 100%; padding: 12px; border: 1px solid #dee2e6; border-radius: 6px; font-size: 16px; min-height: 80px; resize: vertical; box-sizing: border-box;",
                        oninput: move |e| app_state.write().new_element_description = e.value(),
                    }
                }

                div {
                    style: "display: flex; gap: 12px; justify-content: flex-end;",
                    button {
                        style: "background-color: #6c757d; color: white; border: none; padding: 10px 20px; border-radius: 6px; cursor: pointer; font-size: 14px;",
                        onclick: move |_| app_state.write().cancel_add_element(),
                        "Abbrechen"
                    }
                    button {
                        style: "background-color: #28a745; color: white; border: none; padding: 10px 20px; border-radius: 6px; cursor: pointer; font-size: 14px; font-weight: 500;",
                        onclick: move |_| app_state.write().add_element(),
                        "Hinzufügen"
                    }
                }
            }
        }
    }
}

#[component]
fn EditElementDialog(app_state: Signal<AppState>) -> Element {
    rsx! {
        div {
            style: "position: fixed; top: 0; left: 0; width: 100%; height: 100%; background-color: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 1000;",
            onclick: move |_| app_state.write().show_element_editor = false,

            div {
                style: "background-color: white; padding: 24px; border-radius: 12px; box-shadow: 0 4px 20px rgba(0,0,0,0.15); max-width: 500px; width: 90%;",
                onclick: move |e| e.stop_propagation(),

                h3 {
                    style: "margin: 0 0 20px 0; color: #495057; font-size: 20px;",
                    "Eintrag bearbeiten"
                }

                div {
                    style: "margin-bottom: 16px;",
                    label {
                        style: "display: block; margin-bottom: 8px; font-weight: 500; color: #495057;",
                        "Titel:"
                    }
                    input {
                        r#type: "text",
                        value: "{app_state.read().edit_element_name}",
                        style: "width: 100%; padding: 12px; border: 1px solid #dee2e6; border-radius: 6px; font-size: 16px; box-sizing: border-box;",
                        oninput: move |e| app_state.write().edit_element_name = e.value(),
                    }
                }

                div {
                    style: "margin-bottom: 24px;",
                    label {
                        style: "display: block; margin-bottom: 8px; font-weight: 500; color: #495057;",
                        "Beschreibung:"
                    }
                    textarea {
                        value: "{app_state.read().edit_element_description}",
                        style: "width: 100%; padding: 12px; border: 1px solid #dee2e6; border-radius: 6px; font-size: 16px; min-height: 80px; resize: vertical; box-sizing: border-box;",
                        oninput: move |e| app_state.write().edit_element_description = e.value(),
                    }
                }

                div {
                    style: "display: flex; gap: 12px; justify-content: flex-end;",
                    button {
                        style: "background-color: #6c757d; color: white; border: none; padding: 10px 20px; border-radius: 6px; cursor: pointer; font-size: 14px;",
                        onclick: move |_| app_state.write().show_element_editor = false,
                        "Abbrechen"
                    }
                    button {
                        style: "background-color: #28a745; color: white; border: none; padding: 10px 20px; border-radius: 6px; cursor: pointer; font-size: 14px; font-weight: 500;",
                        onclick: move |_| app_state.write().save_element_changes(),
                        "Speichern"
                    }
                }
            }
        }
    }
}
