<!-- ---------- Header ---------- -->
<div align="center">
  <img width="200" height="200"src="fastlane/metadata/android/en-US/images/icon.png">
  <h1>Wall You</h1>
<p>Privacy focused wallpaper app built with MD3.</p>

<!-- ---------- Badges ---------- -->
  <div align="center">
    <a href="https://github.com/hmd-ltf/WallYou/releases/latest"><img alt="Latest release" src="https://img.shields.io/github/v/release/hmd-ltf/WallYou?color=c3e7ff&style=flat-square"></a>
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

[<img src="https://raw.githubusercontent.com/vadret/android/master/assets/get-github.png" alt="Get it on GitHub" height="80">](https://github.com/hmd-ltf/WallYou/releases/latest)
[<img src="https://raw.githubusercontent.com/ImranR98/Obtainium/main/assets/graphics/badge_obtainium.png" alt="Get it on Obtainium" height="80">](https://apps.obtainium.imranr.dev/redirect.html?r=obtainium://add/https://github.com/hmd-ltf/WallYou)

</div>

Download the signed APK from the latest GitHub release, or add this repository to [Obtainium](https://obtainium.imranr.dev/) to get new versions automatically. This fork is not on F-Droid or IzzyOnDroid, whose builds come from upstream [you-apps/WallYou](https://github.com/you-apps/WallYou) and lack this fork's features.

<!-- ---------- Releases ---------- -->
## Releases

[![Latest release](https://img.shields.io/github/v/release/hmd-ltf/WallYou?label=latest&color=c3e7ff&style=flat-square)](https://github.com/hmd-ltf/WallYou/releases/latest)
[![Release date](https://img.shields.io/github/release-date/hmd-ltf/WallYou?color=c3e7ff&style=flat-square)](https://github.com/hmd-ltf/WallYou/releases/latest)

Every version is published on the [Releases page](https://github.com/hmd-ltf/WallYou/releases) with a signed APK (`WallYou-vX.Y.apk`) and the list of changes since the previous release. Download the APK on your phone and open it to install or update.

### What's new in v18.0

- Wallhaven: select several categories, purities, aspect ratios and resolutions at once
- Wallhaven: new minimum resolution, color, toplist range and "Hot" sorting filters
- Tags: popular tag suggestions, excluded tags and adding several tags at once
- Log in to Wallhaven with your API key in the settings to unlock NSFW wallpapers
- Preview how a wallpaper looks on your home and lock screen before applying it
- See the Material You theme Android would generate from a wallpaper

### Publishing a release

Releases are built, signed and published by the [Build and release app](.github/workflows/build-release.yml) workflow.

1. Bump `versionCode` and `versionName` in [`app/build.gradle.kts`](app/build.gradle.kts) and add `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`.
2. Start the release in one of two ways:
   - push a tag such as `v18.0`, or
   - open **Actions → Build and release app → Run workflow** and enter the version tag.
3. The workflow signs the APK and publishes the release with notes generated from the merged pull requests.

The workflow needs these repository secrets (**Settings → Secrets and variables → Actions**). Keep using the same keystore, otherwise installed apps can't update to the new release.

| Secret | Value |
| --- | --- |
| `KEYSTORE` | The release keystore file, base64 encoded (`base64 -w 0 release.jks`) |
| `SIGNING_KEY_ALIAS` | Alias of the key in the keystore |
| `SIGNING_STORE_PASSWORD` | Keystore password |
| `SIGNING_KEY_PASSWORD` | Key password |

To create a keystore: `keytool -genkeypair -v -keystore release.jks -alias wallyou -keyalg RSA -keysize 4096 -validity 10000`

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
