# Urovo DT50 RFID Reader

Flutter application for reading and writing UHF RFID tags on Urovo DT50 devices.

## Features

- **RFID Tag Scan** – Discover and list tags via hardware trigger button or software-initiated scan
- **EPC Write** – Write text-based EPC to a selected tag
- **Memory Read/Write** – Read and write arbitrary memory banks (Reserved, EPC, TID, User)
- **Auto-tuning** – Automatically cycles through power, antenna, frequency, and trigger configurations when no tags are found
- **Hardware Trigger** – Handles the physical scan trigger (KeyCode 523) for press-to-scan / release-to-stop

## Platform Channel

Communication between Dart and Android native code uses the `com.urovo.dt50/rfid` MethodChannel.

### Methods (Dart → Native)

| Method | Description |
|--------|-------------|
| `connect` | Connect to the RFID module |
| `disconnect` | Disconnect from the RFID module |
| `startInventory` | Start tag inventory |
| `stopInventory` | Stop tag inventory |
| `setOutputPower` | Set RF output power (0–33 dBm) |
| `readMemory` | Read a memory bank region |
| `writeMemory` | Write to a memory bank region |
| `writeEpc` | Overwrite a tag's EPC |

### Callbacks (Native → Dart)

| Callback | Description |
|----------|-------------|
| `onTagRead` | Tag discovered (epc, rssi, tid) |
| `onConnectionChanged` | Connection state changed |
| `onScanningStateChanged` | Scanning state changed |
| `onError` | Error occurred |

## Project Structure

```
lib/
├── main.dart                 # App entry point
├── models/
│   └── rfid_tag.dart         # RfidTag model (EPC, RSSI, decoding)
├── services/
│   └── rfid_service.dart     # RFID service (ChangeNotifier + MethodChannel)
├── screens/
│   ├── home_screen.dart      # Home screen (Scan + Write tabs)
│   ├── settings_screen.dart  # Settings (port, baud rate, power)
│   └── write_screen.dart     # Tag write screen
├── theme/
│   └── app_theme.dart        # Theme definitions (dark / light)
└── widgets/
    ├── connection_card.dart   # Connection status card
    ├── scan_button.dart       # Scan button widget
    ├── stats_card.dart        # Statistics card
    └── tag_list.dart          # Tag list widget

android/app/src/main/
├── kotlin/com/urovo/dt50/
│   ├── MainActivity.kt       # Activity + hardware trigger handling
│   └── RfidPlugin.kt         # Flutter MethodChannel handler
├── java/com/urovo/rfid/
│   ├── RfidServiceManager.java    # Urovo RFID service binding
│   ├── RfidManagerWrapper.java    # AIDL wrapper
│   ├── DirectRfidReader.java      # Direct serial-port reader
│   ├── UrovoPowerManager.java     # RFID module power control
│   └── aidl/
│       ├── IRfidCallback.java     # AIDL callback interface
│       ├── IRfidManager.java      # AIDL manager interface
│       └── RfidDate.java          # AIDL data class
├── java/com/rfiddevice/serialport/
│   └── SerialPort.java            # JNI serial port
└── jniLibs/
    ├── arm64-v8a/                 # 64-bit native libraries
    └── armeabi-v7a/               # 32-bit native libraries
```

## Build

```bash
flutter pub get
flutter build apk
```

## Tests

```bash
flutter test
```

## Requirements

- Flutter SDK ^3.9.0
- Android minSdk 26
- Urovo DT50 device with RFID module
