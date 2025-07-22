## 🇵🇱 Polski

This project also has a [Polish version of the README](README_PL.md).

# 📱 BLE Scanner

Android mobile application written in Kotlin for scanning nearby Bluetooth Low Energy (BLE) devices and creating BLE Mesh networks for specialized devices.

## 🔍 Project Description

BLE Scanner is a tool for discovering BLE devices around you. The app uses Android's Bluetooth system to scan available devices and display their names, MAC addresses, and signal strength (RSSI).

Additionally, it supports creating BLE Mesh networks for specific types of devices, allowing management of those devices (reading and writing data). This app can be useful for testing beacons, IoT devices, fitness bands, and other BLE-enabled gadgets.

## 🎯 Features

- ✅ Scanning BLE devices
- ✅ Displaying device name, MAC address, and signal strength (RSSI)
- ✅ Handling location and Bluetooth permissions
- ✅ Creating BLE Mesh networks
- ✅ Managing devices within the mesh network
- ✅ Reading data from devices
- ✅ Writing data to devices

## 📸 Screenshots

![BLE Scanner](screenshots/BLE_Scanner.jpg)

## 🛠️ Technologies Used

The project is built with:

- Kotlin
- Android SDK (API 31+)
- Android BLE API
- View Binding
- Android Permissions API

## 🚀 How to Run Locally

1. Open the project in **Android Studio**.
2. Build the project (`Build > Make Project`).
3. Run the app on a physical device (emulators generally do not support BLE).
4. Grant the required permissions: Bluetooth and Location.

> ℹ️ **Note:** Android emulators usually do not support Bluetooth features — using a physical device is recommended.

## 👤 Author

- **PollemAnt**
- GitHub: [github.com/PollemAnt](https://github.com/PollemAnt)


