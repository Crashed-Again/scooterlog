# ScooterLog

Android app for scooter riders: profile, trick tracker with badges, Beginner → Pro progress bar,
skatepark visit tracker, and JSON export.

## Build with GitHub Actions
1. Create a GitHub repo and push all of these files to it (keep the `.github` folder).
2. Open the **Actions** tab → **Build APK** (runs on every push, or press *Run workflow*).
3. When it finishes, download the **ScooterLog-apk** artifact and install `app-debug.apk`.

The workflow installs Gradle itself, so no gradle wrapper files are needed.
Trick "% of riders" numbers are estimates stored in `Tricks.kt` — edit them freely.
