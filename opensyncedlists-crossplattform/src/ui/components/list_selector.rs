use ribir::prelude::*;
use std::sync::{Arc, Mutex};

use crate::ui::app::AppState;

#[derive(Clone)]
pub struct ListSelector {
    pub state: Arc<Mutex<AppState>>,
}

impl ListSelector {
    pub fn new(state: Arc<Mutex<AppState>>) -> Self {
        Self { state }
    }
}

impl Widget for ListSelector {
    fn build(&self) -> Widget {
        let state = self.state.clone();
        let current_index = {
            let s = state.lock().unwrap();
            s.current_list_index
        };

        Column {
            children: {
                let mut children = Vec::new();

                // Header
                children.push(
                    Container {
                        padding: EdgeInsets::all(8.0),
                        child: Text {
                            text: "Lists".to_string(),
                            font_size: 18.0,
                            font_weight: FontWeight::Bold,
                        }
                    }.into()
                );

                // List items
                let s = state.lock().unwrap();
                for (index, list) in s.lists.iter().enumerate() {
                    let is_selected = current_index == Some(index);
                    let state_clone = state.clone();
                    let list_name = list.header.name.clone();
                    let list_size = list.header.list_size.clone();

                    children.push(
                        Container {
                            padding: EdgeInsets::all(8.0),
                            margin: EdgeInsets::horizontal(4.0),
                            background: if is_selected {
                                Color::from_rgb(0.8, 0.9, 1.0)
                            } else {
                                Color::TRANSPARENT
                            },
                            border: if is_selected {
                                Border::all(2.0, Color::from_rgb(0.2, 0.4, 0.8))
                            } else {
                                Border::none()
                            },
                            cursor: Cursor::Pointer,
                            child: Column {
                                children: [
                                    Text {
                                        text: list_name,
                                        font_size: 14.0,
                                        font_weight: if is_selected {
                                            FontWeight::Bold
                                        } else {
                                            FontWeight::Normal
                                        },
                                    },
                                    Text {
                                        text: list_size,
                                        font_size: 12.0,
                                        color: Color::from_rgb(0.6, 0.6, 0.6),
                                    }
                                ]
                            },
                            on_click: move || {
                                let mut s = state_clone.lock().unwrap();
                                s.current_list_index = Some(index);
                            }
                        }.into()
                    );
                }

                children
            }
        }.into()
    }
}
