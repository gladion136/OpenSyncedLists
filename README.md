# OpenSyncedLists

OpenSyncedLists is the fast, private way to keep every checklist, to-do and
small project under control – synced across your devices and encrypted
end-to-end.

Whether it's the weekly shopping, a bucket list or the tasks for your next
project: create a list in seconds, sort it your way and tick items off as you
go. When you're done, reset the whole list with a single tap and start over.

## Why you'll love it

- **Effortless sorting** – move any item by drag-and-drop, or jump it straight to the top or bottom.
- **Clear overview** – tags, progress counters and a compact mode keep busy lists easy to scan.
- **Stays in order** – checked items move aside but keep their place, ready to be reused.
- **One-tap reset** – clear all checkmarks at once and run the same list again.
- **Perfect for small projects** – group tasks with your own tags and track what's left.

## Made to fit you

- Left-handed mode and adjustable controls (with or without buttons).
- Adjustable font size for list elements.
- Share lists via URL.
- Export as Markdown, clipboard, message, JSON and more.
- Free and open source.

## Privacy by design

We don't collect data. Your lists are only synced with your own server
instance and are stored **end-to-end encrypted** there – the server operator
can never read them. The private key to decrypt the lists is only stored
offline on your local device. You can even use a different server per list.

No accounts, no tracking, no data collection.

## Install Android-Client

[<img src="https://f-droid.org/badge/get-it-on.png"
      alt="Get it on F-Droid"
      height="80">](https://f-droid.org/packages/eu.schmidt.systems.opensyncedlists/)
[<img src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png"
      alt="Get it on Google Play"
      height="80">](https://play.google.com/store/apps/details?id=eu.schmidt.systems.opensyncedlists)

Or you can download and install the apk from this repository [Android_Client/app/release/OpenSyncedLists.apk](Android_Client/app/release/OpenSyncedLists.apk).

## Install Express-Server

The express server is easy to setup inside containers. Clone the [server](https://gitlab.com/gladion136/opensyncedlists-server) and run:

```sh
sudo docker-compose up -d
```
