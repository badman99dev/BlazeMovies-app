# BlazeMovies

A full-featured Android streaming app built with Jetpack Compose. Browse movies, TV shows, live TV, and Zee5 content — watch in-app with a custom ExoPlayer-based player, complete with IMDb enrichment, downloads, and more.

<p align="center">
  <strong>Current Release: v2.0.1</strong><br>
  <a href="https://github.com/badman99dev/BlazeMovies-app/releases">Download APK</a>
</p>

---

## Features

### Content & Playback
- **Movies & TV Shows** — Browse, search, and stream directly in-app
- **In-App Video Player** — Custom Media3/ExoPlayer with fullscreen, subtitles, audio track selection, playback speed, and quality controls
- **Live TV** — Stream live channels via broadcast proxy
- **Zee5 Integration** — Browse and watch Zee5 content
- **YouTube Trailers** — Watch trailers with IMDb details (cast, ratings, synopsis, crew) below
- **Downloads** — Ketch-based download manager with progress notifications

### Enrichment & Details
- **IMDb Integration** — Parallel API calls for title details, cast, credits, age certificates, and episode metadata
- **Cast & Crew** — Collapsible cast section with photos, character names, and fallback from backend data
- **Storyline Fallback** — Backend description shown when IMDb plot is unavailable
- **MetaChips** — Quality, audio, language, and country badges on detail pages
- **Similar Content** — Recommendations on detail pages

### UX & Polish
- **Coil Crossfade** — 300ms global image fade-in across the entire app
- **Connection Banner** — Auto-dismissing "Connection Lost" (3s) and "Back Online" (1.5s) banners
- **Expandable Descriptions** — Tap to expand/collapse storylines
- **Portrait Player** — Scrollable info below video with cast, description, and metadata
- **ACRA Crash Reporting** — Toast + dialog crash reports

### Infrastructure
- **User Auth** — Firebase Authentication + Firestore for bookmarks, likes, and comments
- **In-App Updates** — Parallel GitHub + DB update check with versioned releases
- **Search** — MeiliSearch-powered full-text search with suggestions
- **Push Notifications** — Telegram bot integration for admin broadcasts
- **Content Moderation** — Blur filters for sensitive content

---

## Tech Stack

| Category | Technology |
|----------|-----------|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Clean Architecture |
| DI | Hilt |
| Networking | Retrofit + OkHttp |
| Image Loading | Coil (global crossfade) |
| Media | Media3 / ExoPlayer |
| Downloads | Ketch (WorkManager-based) |
| Auth & DB | Firebase Auth + Firestore |
| Crash Reports | ACRA |
| Search | MeiliSearch |
| APIs | BlazeMovies Backend (PHP/Vercel) + IMDb API (`api.tiffara.com`) |
| Build | GitHub Actions (3-mode: test / minor / major) |

---

## Screens

| Screen | Description |
|--------|-------------|
| **Splash** | Animated intro with logo |
| **Home** | Slider, featured content, categories |
| **Movies** | Browse movies with filters |
| **TV Shows** | Browse series with season/episode detail |
| **Movie/TV Detail** | Poster, metadata, MetaChips, cast, watch buttons, comments |
| **Movie Watch** | Portrait player + scrollable info (cast, storyline, similar) |
| **Series Watch** | Season/episode selector + streaming + episode plots |
| **Video Player** | Fullscreen ExoPlayer with subtitles, audio tracks, speed |
| **YouTube Trailer** | Embedded trailer + IMDb details (cast, ratings, crew) |
| **Zee5** | Browse and watch Zee5 content |
| **Live TV** | Live channel streaming |
| **Search** | Full-text search with suggestions |
| **Trending** | Popular content |
| **Latest Uploads** | Recently added content |
| **Library** | Bookmarks, watch history |
| **Downloads** | Download manager with progress |
| **Profile** | User account, settings |
| **Settings** | Player preferences, update check |
| **Auth** | Login / register |
| **Notifications** | Admin broadcast notifications |

---

## Project Structure

```
app/src/main/java/com/movie/app/best/
├── data/
│   ├── model/          # Data models (App, IMDb, Zee5, Auth, Watch, etc.)
│   ├── remote/         # Retrofit API services (MovieApi, ImdbApi, Zee5Api, AuthApi, BypassApi)
│   ├── repository/     # Data repositories
│   ├── settings/       # App settings
│   └── debug/          # Debug utilities
├── di/                 # Hilt dependency injection modules
├── ui/
│   ├── screens/
│   │   ├── home/          # Home with slider + categories
│   │   ├── movies/        # Movie browsing
│   │   ├── tvshows/       # TV show browsing
│   │   ├── moviedetail/   # Movie detail + MetaChips
│   │   ├── tvshowdetail/  # TV show detail + seasons
│   │   ├── moviewatch/    # Movie portrait player
│   │   ├── serieswatch/   # Series portrait player + episodes
│   │   ├── player/        # Fullscreen ExoPlayer + YouTube trailer
│   │   ├── zee5/          # Zee5 browse + watch
│   │   ├── search/        # MeiliSearch
│   │   ├── trending/      # Trending content
│   │   ├── latestupload/  # Latest uploads
│   │   ├── categories/    # Category browsing
│   │   ├── library/       # Bookmarks + history
│   │   ├── downloads/     # Download manager
│   │   ├── profile/       # User profile
│   │   ├── settings/      # App settings
│   │   ├── auth/          # Login / register
│   │   ├── notification/  # Push notifications
│   │   ├── myfeed/        # Personalized feed
│   │   ├── main/          # Main scaffold + connection banners
│   │   └── splash/        # Splash screen
│   ├── navigation/      # Compose Navigation routes
│   └── theme/           # Material 3 theme
├── util/               # Utilities + extensions
└── MovieApplication.kt # App entry — ImageLoaderFactory, ACRA, Ketch init
```

---

## Build System

The app uses a 3-mode GitHub Actions build workflow (`.github/workflows/build-apk.yml`):

| Mode | Behavior |
|------|----------|
| **test** (default) | Patch bump, debug APK, upload to tempserv — no release |
| **minor** | Patch bump, release APK, GitHub Release + `release.json` |
| **major** | Major bump, release APK, GitHub Release + `release.json` |

Trigger manually via GitHub Actions UI with `workflow_dispatch`, or auto-trigger on push to `main` (test mode).

---

## Configuration

### Backend API
The app connects to the BlazeMovies backend (PHP hosted on Vercel):

```kotlin
// app/build.gradle.kts
buildConfigField("String", "BASE_URL", "\"https://blazemovies.vercel.app/v1/\"")
```

### IMDb API
Used for enrichment (cast, ratings, plots, episodes):
```
https://api.tiffara.com
```

### Ecosystem

| Component | Repo | Tech |
|-----------|------|------|
| **Backend** | [badman99dev/BlazeMovies](https://github.com/badman99dev/BlazeMovies) | PHP / Vercel |
| **Android App** | [badman99dev/BlazeMovies-app](https://github.com/badman99dev/BlazeMovies-app) | Kotlin / Compose |
| **Telegram Bot** | [badman99dev/Wasmer-hub-messenger](https://github.com/badman99dev/Wasmer-hub-messenger) | Python / Render |

---

## Requirements

- Android Studio (latest stable)
- Minimum SDK: 24 (Android 7.0)
- Target SDK: 35
- Compile SDK: 35
- Java 17

## Installation

1. Clone the repository:
```bash
git clone https://github.com/badman99dev/BlazeMovies-app.git
```
2. Open in Android Studio
3. Sync Gradle
4. Run on emulator or device

Or download the latest APK from [Releases](https://github.com/badman99dev/BlazeMovies-app/releases).

---

## License

All rights reserved. This project is not licensed for redistribution or commercial use.
