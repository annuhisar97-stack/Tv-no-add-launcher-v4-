# NoAds TV Launcher V4

## V4 priorities
1. No ads: no advertising SDK or ad permission is included.
2. Lightweight native Android launcher.
3. Customization: dark/light background, card size, columns, clock/date visibility.
4. Large TV-friendly app cards and D-pad focus navigation.
5. Favorites via long-press.
6. Wallpaper picker.
7. All / Favorites / Media / Games / Tools categories.
8. App search.
9. Clock and date.
10. Clean dark, Apple TV-inspired layout (inspired, not affiliated).
11. Remote-control / D-pad navigation.
12. Boot receiver requests startup after boot, package update, and locked boot.

## Important Android startup note
Install the app, then choose **NoAds TV Launcher** under Android TV Settings → Apps → Default apps → Home app (menu names vary by device). Android and some TV firmware versions restrict launching an Activity from a boot receiver, so the receiver is best-effort; making the launcher the default Home app is the reliable setup. Some devices also require enabling autostart in the manufacturer's settings.

## Build APK with GitHub Actions
This is an Android Studio/Gradle project. Upload the contents of this ZIP to a GitHub repository, then use Android Studio or add a Gradle workflow to build `app-debug.apk`. No APK has been built in this ZIP.
