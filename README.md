# Amigo Secreto - Wishlist Android App

A simple Android application for managing wishlists, perfect for Secret Santa events and gift exchanges.

## Overview

This Android app allows users to create and manage a personal wishlist with details about desired products, including price ranges and where to find them. Users can easily share their wishlist with others, making it ideal for Secret Santa exchanges and gift-giving occasions.

## Features

- ✨ **Wishlist Management**: Add, edit, and delete desired items
- 💰 **Price Range Tracking**: Set minimum and maximum price expectations
- 🏪 **Store Suggestions**: Note where items can be found
- 📱 **Easy Sharing**: Share your complete wishlist via any messaging app
- 🎯 **Category Organization**: Organize items by categories
- 📋 **Detailed View**: View complete details for each wishlist item

## Screenshots & UI

The app features a clean, intuitive interface with:
- Splash screen with app branding
- Main wishlist view showing all items
- Add/Edit forms for wishlist management
- Detail view for individual items
- Share functionality integrated into the action bar

## Technical Details

### Architecture
- **Platform**: Android (API 17+)
- **Language**: Java
- **Database**: SQLite for local data storage
- **UI**: Native Android Views with Material Design elements

### Key Components

#### Activities
- `SplashActivity.java:10-42` - App launch screen with 5-second timer
- `ListarDesejos.java:26-171` - Main activity displaying wishlist items
- `InserirDesejoActivity.java` - Form for adding new wishlist items
- `AlterarDesejoActivity.java` - Form for editing existing items
- `DetalheDesejoActivity.java` - Detailed view of individual items

#### Data Model
- `Desejo.java:9-176` - Core wishlist item model with fields:
  - Product name
  - Category
  - Store suggestions
  - Price range (minimum/maximum)
  - Parcelable implementation for data passing

#### Database
- `DesejoDAO.java` - Data Access Object for SQLite operations
- `MySQLiteOpenHelper.java` - Database helper for table management

### Build Configuration
- **Gradle Version**: 2.3.2
- **Compile SDK**: API 22
- **Min SDK**: API 17
- **Target SDK**: API 22
- **Version**: 1.7 (Build 7)

## Installation

### Prerequisites
- Android Studio
- Android SDK (API 17 or higher)
- Java Development Kit (JDK)

### Setup Instructions

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd amigosecreto
   ```

2. **Open in Android Studio**
   - Launch Android Studio
   - Select "Open an existing Android Studio project"
   - Navigate to the project directory

3. **Build the project**
   ```bash
   ./gradlew build
   ```

4. **Run on device/emulator**
   - Connect an Android device or start an emulator
   - Click the "Run" button in Android Studio

## Usage

### Adding Items to Wishlist
1. Open the app and wait for the splash screen
2. Tap the "+" button to add a new item
3. Fill in the product details:
   - Product name
   - Category
   - Price range
   - Store suggestions
4. Save the item

### Sharing Your Wishlist
1. View your complete wishlist
2. Tap the share icon in the action bar
3. Choose your preferred sharing method
4. Your formatted wishlist will be shared with price ranges and store information

### Managing Items
- Tap any item to view detailed information
- Use the edit option to modify existing items
- Long-press for additional management options

## Development Notes

- The app uses SQLite for persistent local storage
- All data remains on the device (no cloud sync)
- Ad integration implemented via AdBuddiz SDK
- Supports Android's native sharing functionality
- Material Design principles for consistent UI/UX

## Version History

- **v1.7** - Current version with full wishlist management
- Enhanced sharing capabilities
- Improved UI/UX with Material Design elements

## Contributing

This is a personal project showcasing Android development skills. The codebase demonstrates:
- Clean architecture with separation of concerns
- Proper data persistence with SQLite
- Material Design implementation
- Android best practices for activities and fragments

## License

This project is for educational and portfolio purposes.

---

*Built with ❤️ for Android*