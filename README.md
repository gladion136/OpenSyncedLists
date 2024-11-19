# OpenSyncedLists

Create easy to sort lists and synchronize it with your own server.

## Features

- Easily manage, create, edit and share lists
- Easy sorting of list items (through 5 movement options)
- Collaboration between multiple devices through fast and automatic synchronization
- Share lists via URL
- Export lists as Markdown, Clipboard, Message, JSON and more
- Different servers for synchronization per list is possible
- Server can/must be self-hosted (we don't collect data!)

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
