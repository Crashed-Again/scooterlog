# ScooterLog

Android app for scooter riders (Material You / Pixel style).

- **Profile**: name, age, Street or Park rider, Beginner -> Pro progress bar, badges, JSON export
- **Tricks**: 74 tricks with search and filters. Tap one for a description, how-to steps and a looping animation
- **Parks**: skateparks near you appear automatically (OpenStreetMap data), with Google Maps directions,
  check-ins and a visit counter for each park

## Build with GitHub Actions
1. Push all of these files to a GitHub repo (keep the `.github` folder).
2. Open the **Actions** tab -> **Build APK** (runs on every push, or press *Run workflow*).
3. Download the **ScooterLog-apk** artifact and install `app-debug.apk`.

## Editing
- `Tricks.kt`: add or change tricks, descriptions, steps and the "% of riders" estimates
- `TrickAnimation.kt`: the animation engine (animations are generated from each trick's `Anim` settings)
