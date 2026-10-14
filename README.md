# Liquid Glass for Android / XOS

An iOS-style liquid glass launcher, lock screen, control center and notification center for Android.
Plain Android framework code, no third-party libraries, `minSdk 21` (Android 5.0), so it installs on any XOS version.

## What is inside

| Piece | File |
|---|---|
| Home screen, glass dock, search, widgets | `LauncherActivity`, `Widgets`, `IconTile` |
| Glass panel with blur, tint, sheen and rim light | `GlassView`, `Blur`, `Wallpaper`, `Backdrop` |
| Lock screen with glass clock (movable, restylable) | `LockScreenActivity`, `LockClockView`, `LockEditorActivity` |
| Control center and notification center | `ControlCenter`, `NotificationCenter`, `GlassAccessibilityService` |
| Settings, wallpapers, themes | `SettingsActivity` |
| Download website | `website/index.html` |

## Build the APK for free (no Android Studio needed)

1. Create a free GitHub account and a new repository.
2. Upload everything in this folder to it (drag and drop in the browser works).
3. In the repository, open **Settings > Pages** and set **Source** to **GitHub Actions**.
4. Open the **Actions** tab and wait for "Build APK and publish website" to turn green (about 5 minutes).
5. Your download site is at `https://<your-username>.github.io/<repository-name>/`, and the APK is
   `.../LiquidGlass.apk` on that site. The APK is also attached to the workflow run as an artifact.

Or build locally with Android Studio: open this folder and run **Build > Build APK(s)**.

## How the replacement works

Android does not let an ordinary app delete the stock lock screen or control center, so this app covers them:

* **Home screen**: set Liquid Glass as the default home app.
* **Lock screen**: a foreground service shows `LockScreenActivity` above the keyguard when the screen turns off.
  Swiping up asks Android to dismiss the real keyguard, so your PIN or fingerprint still applies.
* **Control center / notification center**: an accessibility service adds invisible strips along the top edge.
  Swipe down on the right for the control center, on the left for notifications. If the stock shade opens anyway,
  it is closed and the glass one is shown.
* **Blur**: every glass panel draws a blurred slice of the wallpaper. Overlay panels draw a blurred screenshot of
  the app underneath (Android 11+), or the blurred wallpaper on older versions.

## Known limits

* Some phones (including some XOS builds) restrict overlays on the lock screen or limit background apps.
  Allow auto-start and turn off battery optimisation for Liquid Glass if the lock screen does not appear.
* Toggling Wi-Fi, Bluetooth, mobile data and airplane mode directly is blocked for normal apps on modern Android,
  so those buttons open the system panel for that setting. Brightness, volume, flashlight and rotation work in place.
* Not compiled or run on a device yet. Please test and report any crashes.
