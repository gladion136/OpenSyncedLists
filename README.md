# OpenSyncedLists - Cross-Platform Edition

Eine plattformübergreifende Desktop- und Web-Anwendung für synchronisierte Listen, kompatibel mit der Android OpenSyncedLists App. Entwickelt mit Rust und dem Ribir UI Framework.

## Features

- ✅ **Cross-Platform**: Läuft auf Linux, Windows und im Web Browser
- ✅ **Kompatibilität**: Verwendet das gleiche Datenformat wie die Android-App
- ✅ **Synchronisation**: Server-basierte Synchronisation zwischen Geräten
- ✅ **Listen-Management**: Erstellen, bearbeiten und organisieren von Listen
- ✅ **Checkbox-Listen**: Unterstützung für abhakbare Aufgaben-Listen
- ✅ **Tags**: Kategorisierung von Listen mit Tags
- ✅ **Import/Export**: JSON-basierter Datenaustausch
- ✅ **Offline-Betrieb**: Lokale Speicherung und Bearbeitung

## Architektur

### Datenstrukturen
- `SyncedList`: Hauptdatenstruktur für Listen
- `SyncedListElement`: Einzelne Listenelemente mit Name, Beschreibung und Checkbox-Status
- `SyncedListStep`: Änderungsschritte für Synchronisation (ADD, REMOVE, UPDATE, MOVE, SWAP, CLEAR)
- `SyncedListHeader`: Metadaten und Einstellungen für Listen

### Module
- `data/`: Kerndatenstrukturen und -logik
- `storage/`: Lokale Dateispeicherung und Verschlüsselung
- `sync/`: Server-Synchronisation über REST API
- `ui/`: Benutzeroberfläche mit Ribir Framework

## Installation und Build

### Voraussetzungen
```bash
# Rust installieren
curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh

# Für Web-Unterstützung
rustup target add wasm32-unknown-unknown
```

### Desktop-Anwendung bauen
```bash
cargo build --release
```

### Web-Anwendung bauen
```bash
# WASM-Pack installieren
cargo install wasm-pack

# Web-Version bauen
wasm-pack build --target web --out-dir pkg
```

### Ausführen
```bash
# Desktop
cargo run

# Development Server für Web
python3 -m http.server 8000
```

## Kompatibilität mit Android-App

Diese Anwendung ist vollständig kompatibel mit der Android OpenSyncedLists App:

- **Gleiches Datenformat**: JSON-basierte Speicherung mit identischer Struktur
- **Synchronisation**: Gleiche REST API für Server-Synchronisation  
- **Verschlüsselung**: Kompatible Verschlüsselungsmethoden
- **Import/Export**: Kann Android-App Exports direkt importieren

## Datenformat

Die Anwendung verwendet das gleiche JSON-Format wie die Android-App:

```json
{
  "header": {
    "id": "uuid",
    "name": "Einkaufsliste",
    "checkOption": true,
    "checkedList": true,
    "hostname": "https://server.example.com",
    "tags": [{"name": "Shopping"}]
  },
  "steps": [
    {
      "id": "step-uuid",
      "timestamp": 1640995200000,
      "change_action": "ADD",
      "change_id": "element-uuid",
      "change_value_element": {
        "id": "element-uuid",
        "name": "Milch",
        "description": "1 Liter",
        "checked": false
      }
    }
  ]
}
```

## Server-Synchronisation

Die Anwendung nutzt die gleichen REST-Endpunkte wie die Android-App:

- `GET /test` - Verbindungstest
- `GET /list/get?id=...&secret=...` - Liste abrufen
- `POST /list/set` - Liste aktualisieren
- `POST /list/add` - Neue Liste hinzufügen
- `GET /list/remove?id=...&secret=...` - Liste löschen

## Entwicklung

### Projekt-Struktur
```
src/
├── main.rs              # Anwendungseingang
├── data/                # Datenstrukturen
│   ├── mod.rs
│   ├── list.rs         # SyncedList
│   ├── element.rs      # SyncedListElement
│   ├── step.rs         # SyncedListStep
│   ├── header.rs       # SyncedListHeader
│   ├── action.rs       # Action enum
│   └── tag.rs          # ListTag
├── storage/             # Speicher-Backend
│   ├── mod.rs
│   ├── file_storage.rs # Lokale Dateispeicherung
│   └── secure_storage.rs # Verschlüsselung
├── sync/                # Server-Synchronisation
│   ├── mod.rs
│   └── server.rs       # REST API Client
└── ui/                  # Benutzeroberfläche
    ├── mod.rs
    └── app.rs          # Haupt-UI
```

### Testing
```bash
cargo test
```

## Lizenz

GPL-3.0-or-later - Gleiche Lizenz wie die Android-App für maximale Kompatibilität.

## Beitragen

Dieses Projekt erweitert die OpenSyncedLists Familie um Desktop- und Web-Unterstützung. Beiträge sind willkommen!