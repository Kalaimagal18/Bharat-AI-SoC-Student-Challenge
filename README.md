Bharat AI-SoC Student Challenge
1. Project Description

Android application developed using Android Studio

Built as part of the Bharat AI-SoC Student Challenge

Demonstrates Android development and Gradle-based build system

May include AI/ML integration depending on project features

2. Requirements

Android Studio (latest stable version recommended)

JDK 11 or higher

Android SDK installed

Internet connection (if project uses APIs or model downloads)

3. How to Run (Android Studio Method)

Open Android Studio

Click Open Project

Select the project root folder

Wait for Gradle sync to finish

Connect Android device or start emulator

Click Run

4. How to Build Using Command Line
Windows:
gradlew.bat assembleDebug
Mac/Linux:
./gradlew assembleDebug
5. APK Location After Build

app/build/outputs/apk/debug/app-debug.apk

6. Project Structure

app/ → Main source code (Java/Kotlin, layouts, manifest)

gradle/ → Gradle wrapper configuration

gradlew → Linux/Mac build script

gradlew.bat → Windows build script

settings.gradle → Module configuration

build.gradle → Project-level configuration

gradle.properties → Gradle settings

.gitignore → Prevents unnecessary files from being uploaded

7. Important Notes

Do not upload build/ folder

Do not upload .gradle/

Do not upload local.properties

Only source code and configuration files are included
