# MilaDB - Android MySQL/MariaDB Client

Modern, native Android application for managing MySQL/MariaDB databases directly from your mobile device.

## Features

- **Direct MySQL/MariaDB Connection**: Connect via JDBC without intermediary servers
- **SSH Tunnel Support**: Secure connections through SSH tunneling
- **SSL/TLS Encryption**: Encrypted database connections
- **Database Management**: Browse databases, tables, and data
- **CRUD Operations**: Create, Read, Update, and Delete records
- **SQL Editor**: Execute custom SQL queries with history
- **Data Export**: Export tables as SQL files
- **Connection Management**: Save and manage multiple connections
- **Dark/Light Theme**: Automatic theme switching

## Technical Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose with Material 3
- **Architecture**: MVVM (Model-View-ViewModel)
- **Database**: JDBC (MySQL Connector/J 8.0.33)
- **SSH**: JSch 0.1.55
- **Async**: Kotlin Coroutines + Flow
- **Navigation**: Compose Navigation
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 34 (Android 14)

## Installation

### From Source
```bash
git clone https://github.com/smtbcn/miladb.mysql.git
cd miladb.mysql
./gradlew assembleDebug
```

The APK will be generated in `app/build/outputs/apk/debug/`

## Usage

1. **Connect**: Enter MySQL/MariaDB server credentials
2. **Browse**: View databases and tables
3. **Manage**: Edit, add, or delete records
4. **Query**: Execute custom SQL statements
5. **Export**: Save table data as SQL files

## Security

- All data stays on your device
- No cloud storage or intermediary servers
- Direct database connections
- Local credential storage

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the MIT License.

## Author

**Samet Biçen**

---

Made with ❤️ for the Android community
