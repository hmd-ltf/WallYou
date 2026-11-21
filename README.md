<!-- ---------- Header ---------- -->
<div align="center">
  <img width="200" height="200"src="fastlane/metadata/android/en-US/images/icon.png">
  <h1>Wall You</h1>
<p>Privacy focused wallpaper app built with MD3.</p>

<!-- ---------- Badges ---------- -->
  <div align="center">
    <img alt="License" src="https://img.shields.io/github/license/hmd-ltf/WallYou?color=c3e7ff&style=flat-square">
    <img alt="Downloads" src="https://img.shields.io/github/downloads/hmd-ltf/WallYou/total.svg?color=c3e7ff&style=flat-square">
    <img alt="Last commit" src="https://img.shields.io/github/last-commit/hmd-ltf/WallYou?color=c3e7ff&style=flat-square">
    <img alt="Repo size" src="https://img.shields.io/github/repo-size/hmd-ltf/WallYou?color=c3e7ff&style=flat-square">
    <img alt="Stars" src="https://img.shields.io/github/stars/hmd-ltf/WallYou?color=c3e7ff&style=flat-square">
    <br>
</div>
</div>

<!-- ---------- Description ---------- -->
## Features

- [x] Wallpaper Browsing and Downloading
- [x] Automatic Wallpaper Changer
- [x] Favorites List
- [x] Wallpaper History
- [x] Image Filter Effects
- [x] Wallhaven multi-select filters (categories, purity, ratios, resolutions, colors) and tag suggestions
- [x] Wallhaven login with an API key to unlock NSFW wallpapers
- [x] Full-screen home and lock screen preview before applying a wallpaper
- [x] Material You theme preview generated from any wallpaper

<!-- ---------- Download ---------- -->
## Download
  <div align="center">

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png" alt="Get it on F-Droid" height="80">](https://f-droid.org/packages/com.bnyro.wallpaper/)
[<img src="https://gitlab.com/IzzyOnDroid/repo/-/raw/master/assets/IzzyOnDroid.png" alt="Get it on IzzyOnDroid" height="80">](https://apt.izzysoft.de/fdroid/index/apk/com.bnyro.wallpaper)
[<img src="https://raw.githubusercontent.com/vadret/android/master/assets/get-github.png" alt="Get it on GitHub" height="80">](https://github.com/hmd-ltf/WallYou/releases)

</div>

F-Droid and IzzyOnDroid ship the upstream [you-apps/WallYou](https://github.com/you-apps/WallYou) builds. The features added in this fork are only in APKs built from this repository, either from the [Releases page](https://github.com/hmd-ltf/WallYou/releases) or built yourself.

<!-- ---------- Releases ---------- -->
## Releases

### What's new in v18.0

- Wallhaven: select several categories, purities, aspect ratios and resolutions at once
- Wallhaven: new minimum resolution, color, toplist range and "Hot" sorting filters
- Tags: popular tag suggestions, excluded tags and adding several tags at once
- Log in to Wallhaven with your API key in the settings to unlock NSFW wallpapers
- Preview how a wallpaper looks on your home and lock screen before applying it
- See the Material You theme Android would generate from a wallpaper

### Build it yourself

You need JDK 17 and the Android SDK (e.g. installed with Android Studio).

```bash
git clone https://github.com/hmd-ltf/WallYou.git
cd WallYou
./gradlew assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`. Install it with `adb install app/build/outputs/apk/debug/app-debug.apk` or copy it to your phone and open it. Debug builds install next to the regular app as "Wall You (debug)".

### Publishing a release

Nothing is built on GitHub; releases are built and uploaded by hand.

1. Bump `versionCode` and `versionName` in [`app/build.gradle.kts`](app/build.gradle.kts) and add `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`.
2. Build the APK: `./gradlew assembleRelease`, then sign `app/build/outputs/apk/release/app-release-unsigned.apk` with your own key using `apksigner` from the Android SDK build tools. Keep the key safe and outside this repository, and use the same one for every release so installed apps can update.
3. Upload it: `gh release create v18.0 WallYou-v18.0.apk --title "Wall You v18.0" --generate-notes`

<!-- ---------- Screenshots ---------- -->
## Screenshots

<div style="display: flex;">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/1-home.png" width=30%>
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/2-filter.png" width=30%>
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/3-option.png" width=30%>
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/4-about.png" width=30%>

</div>

<!-- ---------- Contribution ---------- -->
## Feedback and contributions
***All contributions are very welcome!***

* Feel free to join the [Matrix room](https://matrix.to/#/#you-apps:matrix.org) for discussions about the app.
* Bug reports and feature requests can be submitted [here](https://github.com/hmd-ltf/WallYou/issues) (please make sure to fill out all the requested information properly!).
* If you are a developer and wish to contribute to the app, please **fork** the project and submit a [**pull request**](https://help.github.com/articles/about-pull-requests/).

## Translation
<a href="https://hosted.weblate.org/projects/you-apps/wall-you/">
<img src="https://hosted.weblate.org/widgets/you-apps/-/wall-you/287x66-grey.png" alt="Translation status" />
</a>

## Credits
* Icon design by [M00NJ](https://github.com/M00NJ)

## License

Wall You is licensed under the [**GNU General Public License**](https://www.gnu.org/licenses/gpl.html): You can use, study and share it as you want.
