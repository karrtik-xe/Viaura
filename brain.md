# PROJECT BRAIN — Viaura (read this first)

Single source of truth for any AI tool or human working on this repo. Everything below describes the CURRENT layout only.

## 1. What this project is

- **Viaura**: native Android video + audio player. applicationId `god.kartik.viaura`; Gradle namespace / R root `com.example` (never change).
- UI: 100% Jetpack Compose, own liquid-glass design (see rule R7). Video engine: libmpv (`player/mpv/`). Subtitles: custom pipeline (`subtitles/`).
- Online music/video: NewPipeExtractor (no API key). Lyrics: many providers (`lyrics/`). Network > Media plugins: Vega (JS) + CloudStream (.cs3) providers (`providers/`, module `cloudstream-lib`).
- Downloads: own downloader (`download/`), torrents download-only (`torrent/`), yt-dlp runtime (`ytdlp/`).
- Build: GitHub Actions only (`.github/workflows/build-apk.yml`). No local APK build is expected. Origin: Google AI Studio Android export.
- Gradle modules: `:app` and `:cloudstream-lib` (sources live in `app/src/cloudstream-lib.zip`, unpacked before Gradle starts).

## 2. Task → file finder

Paths are relative to `app/src/main/java/com/example/` unless they start with `app/`, `.github/` or `tools/`.

| I want to change… | Open |
|---|---|
| App start, permissions at launch, PiP, intent handling, HOME↔PLAYER navigation | `app/AppEntryActivity.kt` |
| Splash screen | `app/StartupSplashScreen.kt` |
| Crash report shown on next start | `app/CrashReasonRecorder.kt`, `app/CrashReportSheet.kt` |
| In-app updater (GitHub releases) | `app/update/GithubReleaseUpdater.kt`, `app/update/UpdateSettingsCardAndSheet.kt` |
| Home screen / folder list / Recent button logic | `library/home/HomeLibraryScreen.kt` |
| Scan the phone for videos, VideoItem / VideoFolder models | `library/scan/VideoFolderScanEngine.kt`, `library/scan/VideoLibraryModels.kt` |
| Video list card / grid card (Remove Card UI: `library/cards/CardStyleSettings.kt`, progress bar on thumbnail: `library/cards/ThumbnailPlayedProgress.kt`) | `library/cards/VideoListItemCard.kt`, `library/cards/VideoGridItemCard.kt` |
| Video thumbnails (cache, load, prefetch) | `library/thumbnails/` |
| Sort & View popup | `library/sort/` |
| Playlists tab, favourites, saved, add-to-playlist | `library/playlists/` |
| Recently played tab and resume history | `library/history/` |
| Private vault | `library/vault/` |
| Bulk rename / inline rename | `library/rename/` |
| MKV tracks, chapters, attachments (EBML read/write) | `library/mkv/MkvEbmlEngine.kt` |
| Media Information screen | `library/info/` |
| Archive (zip/rar) playlists | `library/archive/` |
| M3U / IPTV playlists | `library/m3u/` |
| Video player screen (state, gestures, back animation) | `player/screen/VideoPlayerScreenHost.kt` |
| Video player on-screen controls / gestures / top bar | `player/overlay/VideoPlayerControlsOverlay.kt` |
| Video player 3-dot menu (video settings, filters, speed, …) | `player/panels/PlayerMoreMenuPanel.kt` |
| Glass bottom sheet frame of player panels | `player/panels/PlayerGlassModalSheet.kt`, colours: `player/panels/PlayerPanelGlassTokens.kt` |
| mpv engine wrapper (load, seek, tracks, filters, hwdec) | `player/mpv/MpvEngineController.kt` |
| mpv Android surface view | `player/mpv/MpvSurfaceHostView.kt` |
| Mini video player (popup / background) | `player/mini/` |
| Sleep timer logic / UI | `player/SleepTimerController.kt`, `player/panels/SleepTimerPanel.kt` |
| Chapters list, skip intro/outro button | `player/chapters/`, `player/panels/ChapterSelectionPanel.kt` |
| Control layout editor (customise buttons) | `player/layout/` |
| Smart auto playback (codec / HDR advice) | `player/smart/SmartAutoPlaybackAdvisor.kt` |
| Subtitle track picker panel | `subtitles/panels/SubtitleSelectionPanel.kt` |
| Subtitle delay panel | `subtitles/panels/SubtitleDelayPanel.kt` |
| Subtitle style panel (typography, colours, misc) | `subtitles/style/` |
| ASS/SSA styles editor and override engine | `subtitles/ass/` |
| Subtitle format conversion (SRT/VTT/… → ASS) | `subtitles/engine/` |
| Subtitle fonts (embedded, user, Google, Go-Noto fallback) | `subtitles/fonts/` |
| Remember chosen subtitle per video | `subtitles/store/` |
| Subtitle file picker / raw subtitle text editor | `subtitles/ui/` |
| Audio player full screen | `audio/ui/AudioPlayerFullScreen.kt` |
| Audio playback engine, queue, effects | `audio/AudioQueuePlaybackCore.kt` |
| Audio notification / foreground service | `audio/notification/` |
| Audio EQ, channel, surround models; language matching | `audio/settings/` |
| Audio tab (folders, tracks, cards) | `audio/library/` |
| Audio audio-track picker / delay panel (video player) | `audio/panels/` |
| Album art loading and colours | `audio/artwork/` |
| Lyrics lookup (repository, cache, providers) | `lyrics/LyricsLookupRepository.kt`, `lyrics/providers/` |
| Karaoke lyrics view | `lyrics/ui/KaraokeLyricsPanel.kt` |
| Online music (YouTube Music search/stream) | `online/music/` |
| Online video (YouTube streams, comments, quality) | `online/video/` |
| Online recommendations / interest profile | `online/recommend/` |
| Online home panels, swipe toolbar, channel page | `online/ui/` |
| Network tab (paste link & play, recent streams) | `network/ui/`, `network/stream/` |
| DNS-over-HTTPS, shared HTTP client, connectivity | `network/` |
| Local bypass proxy / provider network settings | `network/bypass/` |
| Media plugins: install, list, run (Vega / CloudStream) | `providers/store/PluginInstallStore.kt`, `providers/ProviderApiRepository.kt` |
| Plugin JS runtime host (Vega) and native crypto, cookies | `providers/runtime/`, asset `app/src/main/assets/media/provider_js_runtime.js` |
| In-app browser (Cloudflare / login for providers) | `providers/browser/` |
| Provider title page (seasons, quality, audio) | `providers/detail/` |
| Provider home page, hero slider, post cards | `providers/ui/` |
| Provider stream resolve, failover, server race | `providers/playback/` |
| Provider download sheet (server / quality probing) | `providers/download/` |
| Manga reader | `providers/manga/` |
| Download queue, store, foreground service | `download/` |
| Download sheets (online + link) and Downloads tab | `download/ui/` |
| HTTP chunked / HLS / DASH downloader | `download/http/` |
| MKV muxing of downloads, tag writing | `download/mux/`, `download/tagging/` |
| Torrent download / file picker | `torrent/` |
| yt-dlp runtime (install, update, worker) | `ytdlp/` |
| AI translation (provider, API key, subtitle translator) | `ai/` |
| Settings dashboard (sheet, tabs) | `settings/dashboard/SettingsDashboardSheet.kt` |
| Settings > Subtitles tab | `settings/subtitles/SubtitleSettingsTab.kt` |
| Settings > Folders / About / AI / Advanced / Appearance | `settings/folders/`, `settings/about/`, `settings/ai/`, `settings/advanced/`, `settings/appearance/` |
| App settings state, preferences, ViewModel, UI strings | `settings/state/` |
| Player settings data class (every player option) | `settings/state/PlayerSettingsModel.kt` |
| UI languages (English / Hinglish / Hindi) | `settings/i18n/`, JSON in `app/src/main/assets/lang/` |
| Settings export/import XML | `settings/SettingsXmlBackupManager.kt` |
| Colours, theme, fonts, wallpaper "System" tone | `theme/` |
| Liquid glass modifiers, sliders, switches, pull-to-refresh | `glass/` |
| Small reusable widgets (bounce click, header bar, icons) | `widgets/` (in zip) |
| Icons (vector XML) | zip `app/src/viaura-icons.zip`, see section 6 |
| CloudStream engine / extractors | zip `app/src/cloudstream-lib.zip` (see rule R4) |
| CI / build | `.github/workflows/build-apk.yml` (section 8) |

## 3. Rules (must follow)

- **R1 – Packages are NOT folders.** Folders are feature folders. Every Kotlin file keeps its original `package` (e.g. `com.example.ui.components`, `com.example.util`). Never change a package: manifest, proguard, imports and R8 rely on them. To find a symbol use section 10, not the package.
- **R2 – File names are unique** (case-insensitive) across the project. Top-level functions compile to a class named `<FileName>Kt`; two files with the same name in one package would collide in the JVM.
- **R3 – Split files share package-level symbols.** Helpers that used to be file-private and are needed by two files are `internal`. Before adding a new top-level helper, search the whole package for the same name.
- **R4 – `cloudstream-lib` file names and packages must never change.** `.cs3` extensions call facade classes such as `MainAPIKt`/`UtilsKt` by file name. Edit inside `app/src/cloudstream-lib.zip` only (unzip → edit → re-zip with the same layout).
- **R5 – No new big open-source files in git.** Fonts, python stdlib, yt-dlp, QuickJS, CA bundle, libpython are downloaded by CI (section 8). Do not commit them.
- **R6 – Small-file folders stay zipped** (section 7). Edit the unpacked copy locally, then re-zip with `cd app/src && zip -X -r <zip> <folder>`.
- **R7 – UI**: reuse `glass/` modifiers, `theme/` tokens, `viaura_*` icons. Use `ViauraSlider` / `ViauraSwitch`, never raw Material `Slider`/`Switch`. Settings files import `com.example.ui.i18n.Text` so texts are translated.
- **R8 – Languages**: user-visible text is the English string in code; translations are keys of the three JSON files (`app/src/main/assets/lang/`). Add a new text to all three.
- **R9 – No APK build / no emulator in AI sessions**; static checks only. Never claim "verified" for something that was not run.
- **R10 – Torrents are download-only in the UI**; `torrent/TorrentLoopbackStreamServer.kt` is only used by provider links (`providers/playback/PluginStreamResolver.kt`, `providers/ProviderApiRepository.kt`).
- **R11 – Do not rename or move files again without** updating `.github/legacy-paths.txt` (old paths removed by CI) and this file.

# PROJECT BRAIN — Viaura (read this first)

Single source of truth for any AI tool or human working on this repo. Everything below describes the CURRENT layout only.

## 1. What this project is

- **Viaura**: native Android video + audio player. applicationId `god.kartik.viaura`; Gradle namespace / R root `com.example` (never change).
- UI: 100% Jetpack Compose, own liquid-glass design (see rule R7). Video engine: libmpv (`player/mpv/`). Subtitles: custom pipeline (`subtitles/`).
- Online music/video: NewPipeExtractor (no API key). Lyrics: many providers (`lyrics/`). Network > Media plugins: Vega (JS) + CloudStream (.cs3) providers (`providers/`, module `cloudstream-lib`).
- Downloads: own downloader (`download/`), torrents download-only (`torrent/`), yt-dlp runtime (`ytdlp/`).
- Build: GitHub Actions only (`.github/workflows/build-apk.yml`). No local APK build is expected. Origin: Google AI Studio Android export.
- Gradle modules: `:app` and `:cloudstream-lib` (sources live in `app/src/cloudstream-lib.zip`, unpacked before Gradle starts).

## 2. Task → file finder

Paths are relative to `app/src/main/java/com/example/` unless they start with `app/`, `.github/` or `tools/`.

| I want to change… | Open |
|---|---|
| App start, permissions at launch, PiP, intent handling, HOME↔PLAYER navigation | `app/AppEntryActivity.kt` |
| Splash screen | `app/StartupSplashScreen.kt` |
| Crash report shown on next start | `app/CrashReasonRecorder.kt`, `app/CrashReportSheet.kt` |
| In-app updater (GitHub releases) | `app/update/GithubReleaseUpdater.kt`, `app/update/UpdateSettingsCardAndSheet.kt` |
| Home screen / folder list / Recent button logic | `library/home/HomeLibraryScreen.kt` |
| Scan the phone for videos, VideoItem / VideoFolder models | `library/scan/VideoFolderScanEngine.kt`, `library/scan/VideoLibraryModels.kt` |
| Video list card / grid card (Remove Card UI: `library/cards/CardStyleSettings.kt`, progress bar on thumbnail: `library/cards/ThumbnailPlayedProgress.kt`) | `library/cards/VideoListItemCard.kt`, `library/cards/VideoGridItemCard.kt` |
| Video thumbnails (cache, load, prefetch) | `library/thumbnails/` |
| Sort & View popup | `library/sort/` |
| Playlists tab, favourites, saved, add-to-playlist | `library/playlists/` |
| Recently played tab and resume history | `library/history/` |
| Private vault | `library/vault/` |
| Bulk rename / inline rename | `library/rename/` |
| MKV tracks, chapters, attachments (EBML read/write) | `library/mkv/MkvEbmlEngine.kt` |
| Media Information screen | `library/info/` |
| Archive (zip/rar) playlists | `library/archive/` |
| M3U / IPTV playlists | `library/m3u/` |
| Video player screen (state, gestures, back animation) | `player/screen/VideoPlayerScreenHost.kt` |
| Video player on-screen controls / gestures / top bar | `player/overlay/VideoPlayerControlsOverlay.kt` |
| Video player 3-dot menu (video settings, filters, speed, …) | `player/panels/PlayerMoreMenuPanel.kt` |
| Glass bottom sheet frame of player panels | `player/panels/PlayerGlassModalSheet.kt`, colours: `player/panels/PlayerPanelGlassTokens.kt` |
| mpv engine wrapper (load, seek, tracks, filters, hwdec) | `player/mpv/MpvEngineController.kt` |
| mpv Android surface view | `player/mpv/MpvSurfaceHostView.kt` |
| Mini video player (popup / background) | `player/mini/` |
| Sleep timer logic / UI | `player/SleepTimerController.kt`, `player/panels/SleepTimerPanel.kt` |
| Chapters list, skip intro/outro button | `player/chapters/`, `player/panels/ChapterSelectionPanel.kt` |
| Control layout editor (customise buttons) | `player/layout/` |
| Smart auto playback (codec / HDR advice) | `player/smart/SmartAutoPlaybackAdvisor.kt` |
| Subtitle track picker panel | `subtitles/panels/SubtitleSelectionPanel.kt` |
| Subtitle delay panel | `subtitles/panels/SubtitleDelayPanel.kt` |
| Subtitle style panel (typography, colours, misc) | `subtitles/style/` |
| ASS/SSA styles editor and override engine | `subtitles/ass/` |
| Subtitle format conversion (SRT/VTT/… → ASS) | `subtitles/engine/` |
| Subtitle fonts (embedded, user, Google, Go-Noto fallback) | `subtitles/fonts/` |
| Remember chosen subtitle per video | `subtitles/store/` |
| Subtitle file picker / raw subtitle text editor | `subtitles/ui/` |
| Audio player full screen | `audio/ui/AudioPlayerFullScreen.kt` |
| Audio playback engine, queue, effects | `audio/AudioQueuePlaybackCore.kt` |
| Audio notification / foreground service | `audio/notification/` |
| Audio EQ, channel, surround models; language matching | `audio/settings/` |
| Audio tab (folders, tracks, cards) | `audio/library/` |
| Audio audio-track picker / delay panel (video player) | `audio/panels/` |
| Album art loading and colours | `audio/artwork/` |
| Lyrics lookup (repository, cache, providers) | `lyrics/LyricsLookupRepository.kt`, `lyrics/providers/` |
| Karaoke lyrics view | `lyrics/ui/KaraokeLyricsPanel.kt` |
| Online music (YouTube Music search/stream) | `online/music/` |
| Online video (YouTube streams, comments, quality) | `online/video/` |
| Online recommendations / interest profile | `online/recommend/` |
| Online home panels, swipe toolbar, channel page | `online/ui/` |
| Network tab (paste link & play, recent streams) | `network/ui/`, `network/stream/` |
| DNS-over-HTTPS, shared HTTP client, connectivity | `network/` |
| Local bypass proxy / provider network settings | `network/bypass/` |
| Media plugins: install, list, run (Vega / CloudStream) | `providers/store/PluginInstallStore.kt`, `providers/ProviderApiRepository.kt` |
| Plugin JS runtime host (Vega) and native crypto, cookies | `providers/runtime/`, asset `app/src/main/assets/media/provider_js_runtime.js` |
| In-app browser (Cloudflare / login for providers) | `providers/browser/` |
| Provider title page (seasons, quality, audio) | `providers/detail/` |
| Provider home page, hero slider, post cards | `providers/ui/` |
| Provider stream resolve, failover, server race | `providers/playback/` |
| Provider download sheet (server / quality probing) | `providers/download/` |
| Manga reader | `providers/manga/` |
| Download queue, store, foreground service | `download/` |
| Download sheets (online + link) and Downloads tab | `download/ui/` |
| HTTP chunked / HLS / DASH downloader | `download/http/` |
| MKV muxing of downloads, tag writing | `download/mux/`, `download/tagging/` |
| Torrent download / file picker | `torrent/` |
| yt-dlp runtime (install, update, worker) | `ytdlp/` |
| AI translation (provider, API key, subtitle translator) | `ai/` |
| Settings dashboard (sheet, tabs) | `settings/dashboard/SettingsDashboardSheet.kt` |
| Settings > Subtitles tab | `settings/subtitles/SubtitleSettingsTab.kt` |
| Settings > Folders / About / AI / Advanced / Appearance | `settings/folders/`, `settings/about/`, `settings/ai/`, `settings/advanced/`, `settings/appearance/` |
| App settings state, preferences, ViewModel, UI strings | `settings/state/` |
| Player settings data class (every player option) | `settings/state/PlayerSettingsModel.kt` |
| UI languages (English / Hinglish / Hindi) | `settings/i18n/`, JSON in `app/src/main/assets/lang/` |
| Settings export/import XML | `settings/SettingsXmlBackupManager.kt` |
| Colours, theme, fonts, wallpaper "System" tone | `theme/` |
| Liquid glass modifiers, sliders, switches, pull-to-refresh | `glass/` |
| Small reusable widgets (bounce click, header bar, icons) | `widgets/` (in zip) |
| Icons (vector XML) | zip `app/src/viaura-icons.zip`, see section 6 |
| CloudStream engine / extractors | zip `app/src/cloudstream-lib.zip` (see rule R4) |
| CI / build | `.github/workflows/build-apk.yml` (section 8) |

## 3. Rules (must follow)

- **R1 – Packages are NOT folders.** Folders are feature folders. Every Kotlin file keeps its original `package` (e.g. `com.example.ui.components`, `com.example.util`). Never change a package: manifest, proguard, imports and R8 rely on them. To find a symbol use section 10, not the package.
- **R2 – File names are unique** (case-insensitive) across the project. Top-level functions compile to a class named `<FileName>Kt`; two files with the same name in one package would collide in the JVM.
- **R3 – Split files share package-level symbols.** Helpers that used to be file-private and are needed by two files are `internal`. Before adding a new top-level helper, search the whole package for the same name.
- **R4 – `cloudstream-lib` file names and packages must never change.** `.cs3` extensions call facade classes such as `MainAPIKt`/`UtilsKt` by file name. Edit inside `app/src/cloudstream-lib.zip` only (unzip → edit → re-zip with the same layout).
- **R5 – No new big open-source files in git.** Fonts, python stdlib, yt-dlp, QuickJS, CA bundle, libpython are downloaded by CI (section 8). Do not commit them.
- **R6 – Small-file folders stay zipped** (section 7). Edit the unpacked copy locally, then re-zip with `cd app/src && zip -X -r <zip> <folder>`.
- **R7 – UI**: reuse `glass/` modifiers, `theme/` tokens, `viaura_*` icons. Use `ViauraSlider` / `ViauraSwitch`, never raw Material `Slider`/`Switch`. Settings files import `com.example.ui.i18n.Text` so texts are translated.
- **R8 – Languages**: user-visible text is the English string in code; translations are keys of the three JSON files (`app/src/main/assets/lang/`). Add a new text to all three.
- **R9 – No APK build / no emulator in AI sessions**; static checks only. Never claim "verified" for something that was not run.
- **R10 – Torrents are download-only in the UI**; `torrent/TorrentLoopbackStreamServer.kt` is only used by provider links (`providers/playback/PluginStreamResolver.kt`, `providers/ProviderApiRepository.kt`).
- **R11 – Do not rename or move files again without** updating `.github/legacy-paths.txt` (old paths removed by CI) and this file.


# PROJECT BRAIN — Viaura (read this first)

Single source of truth for any AI tool or human working on this repo. Everything below describes the CURRENT layout only.

## 1. What this project is

- **Viaura**: native Android video + audio player. applicationId `god.kartik.viaura`; Gradle namespace / R root `com.example` (never change).
- UI: 100% Jetpack Compose, own liquid-glass design (see rule R7). Video engine: libmpv (`player/mpv/`). Subtitles: custom pipeline (`subtitles/`).
- Online music/video: NewPipeExtractor (no API key). Lyrics: many providers (`lyrics/`). Network > Media plugins: Vega (JS) + CloudStream (.cs3) providers (`providers/`, module `cloudstream-lib`).
- Downloads: own downloader (`download/`), torrents download-only (`torrent/`), yt-dlp runtime (`ytdlp/`).
- Build: GitHub Actions only (`.github/workflows/build-apk.yml`). No local APK build is expected. Origin: Google AI Studio Android export.
- Gradle modules: `:app` and `:cloudstream-lib` (sources live in `app/src/cloudstream-lib.zip`, unpacked before Gradle starts).

## 2. Task → file finder

Paths are relative to `app/src/main/java/com/example/` unless they start with `app/`, `.github/` or `tools/`.

| I want to change… | Open |
|---|---|
| App start, permissions at launch, PiP, intent handling, HOME↔PLAYER navigation | `app/AppEntryActivity.kt` |
| Splash screen | `app/StartupSplashScreen.kt` |
| Crash report shown on next start | `app/CrashReasonRecorder.kt`, `app/CrashReportSheet.kt` |
| In-app updater (GitHub releases) | `app/update/GithubReleaseUpdater.kt`, `app/update/UpdateSettingsCardAndSheet.kt` |
| Home screen / folder list / Recent button logic | `library/home/HomeLibraryScreen.kt` |
| Scan the phone for videos, VideoItem / VideoFolder models | `library/scan/VideoFolderScanEngine.kt`, `library/scan/VideoLibraryModels.kt` |
| Video list card / grid card (Remove Card UI: `library/cards/CardStyleSettings.kt`, progress bar on thumbnail: `library/cards/ThumbnailPlayedProgress.kt`) | `library/cards/VideoListItemCard.kt`, `library/cards/VideoGridItemCard.kt` |
| Video thumbnails (cache, load, prefetch) | `library/thumbnails/` |
| Sort & View popup | `library/sort/` |
| Playlists tab, favourites, saved, add-to-playlist | `library/playlists/` |
| Recently played tab and resume history | `library/history/` |
| Private vault | `library/vault/` |
| Bulk rename / inline rename | `library/rename/` |
| MKV tracks, chapters, attachments (EBML read/write) | `library/mkv/MkvEbmlEngine.kt` |
| Media Information screen | `library/info/` |
| Archive (zip/rar) playlists | `library/archive/` |
| M3U / IPTV playlists | `library/m3u/` |
| Video player screen (state, gestures, back animation) | `player/screen/VideoPlayerScreenHost.kt` |
| Video player on-screen controls / gestures / top bar | `player/overlay/VideoPlayerControlsOverlay.kt` |
| Video player 3-dot menu (video settings, filters, speed, …) | `player/panels/PlayerMoreMenuPanel.kt` |
| Glass bottom sheet frame of player panels | `player/panels/PlayerGlassModalSheet.kt`, colours: `player/panels/PlayerPanelGlassTokens.kt` |
| mpv engine wrapper (load, seek, tracks, filters, hwdec) | `player/mpv/MpvEngineController.kt` |
| mpv Android surface view | `player/mpv/MpvSurfaceHostView.kt` |
| Mini video player (popup / background) | `player/mini/` |
| Sleep timer logic / UI | `player/SleepTimerController.kt`, `player/panels/SleepTimerPanel.kt` |
| Chapters list, skip intro/outro button | `player/chapters/`, `player/panels/ChapterSelectionPanel.kt` |
| Control layout editor (customise buttons) | `player/layout/` |
| Smart auto playback (codec / HDR advice) | `player/smart/SmartAutoPlaybackAdvisor.kt` |
| Subtitle track picker panel | `subtitles/panels/SubtitleSelectionPanel.kt` |
| Subtitle delay panel | `subtitles/panels/SubtitleDelayPanel.kt` |
| Subtitle style panel (typography, colours, misc) | `subtitles/style/` |
| ASS/SSA styles editor and override engine | `subtitles/ass/` |
| Subtitle format conversion (SRT/VTT/… → ASS) | `subtitles/engine/` |
| Subtitle fonts (embedded, user, Google, Go-Noto fallback) | `subtitles/fonts/` |
| Remember chosen subtitle per video | `subtitles/store/` |
| Subtitle file picker / raw subtitle text editor | `subtitles/ui/` |
| Audio player full screen | `audio/ui/AudioPlayerFullScreen.kt` |
| Audio playback engine, queue, effects | `audio/AudioQueuePlaybackCore.kt` |
| Audio notification / foreground service | `audio/notification/` |
| Audio EQ, channel, surround models; language matching | `audio/settings/` |
| Audio tab (folders, tracks, cards) | `audio/library/` |
| Audio audio-track picker / delay panel (video player) | `audio/panels/` |
| Album art loading and colours | `audio/artwork/` |
| Lyrics lookup (repository, cache, providers) | `lyrics/LyricsLookupRepository.kt`, `lyrics/providers/` |
| Karaoke lyrics view | `lyrics/ui/KaraokeLyricsPanel.kt` |
| Online music (YouTube Music search/stream) | `online/music/` |
| Online video (YouTube streams, comments, quality) | `online/video/` |
| Online recommendations / interest profile | `online/recommend/` |
| Online home panels, swipe toolbar, channel page | `online/ui/` |
| Network tab (paste link & play, recent streams) | `network/ui/`, `network/stream/` |
| DNS-over-HTTPS, shared HTTP client, connectivity | `network/` |
| Local bypass proxy / provider network settings | `network/bypass/` |
| Media plugins: install, list, run (Vega / CloudStream) | `providers/store/PluginInstallStore.kt`, `providers/ProviderApiRepository.kt` |
| Plugin JS runtime host (Vega) and native crypto, cookies | `providers/runtime/`, asset `app/src/main/assets/media/provider_js_runtime.js` |
| In-app browser (Cloudflare / login for providers) | `providers/browser/` |
| Provider title page (seasons, quality, audio) | `providers/detail/` |
| Provider home page, hero slider, post cards | `providers/ui/` |
| Provider stream resolve, failover, server race | `providers/playback/` |
| Provider download sheet (server / quality probing) | `providers/download/` |
| Manga reader | `providers/manga/` |
| Download queue, store, foreground service | `download/` |
| Download sheets (online + link) and Downloads tab | `download/ui/` |
| HTTP chunked / HLS / DASH downloader | `download/http/` |
| MKV muxing of downloads, tag writing | `download/mux/`, `download/tagging/` |
| Torrent download / file picker | `torrent/` |
| yt-dlp runtime (install, update, worker) | `ytdlp/` |
| AI translation (provider, API key, subtitle translator) | `ai/` |
| Settings dashboard (sheet, tabs) | `settings/dashboard/SettingsDashboardSheet.kt` |
| Settings > Subtitles tab | `settings/subtitles/SubtitleSettingsTab.kt` |
| Settings > Folders / About / AI / Advanced / Appearance | `settings/folders/`, `settings/about/`, `settings/ai/`, `settings/advanced/`, `settings/appearance/` |
| App settings state, preferences, ViewModel, UI strings | `settings/state/` |
| Player settings data class (every player option) | `settings/state/PlayerSettingsModel.kt` |
| UI languages (English / Hinglish / Hindi) | `settings/i18n/`, JSON in `app/src/main/assets/lang/` |
| Settings export/import XML | `settings/SettingsXmlBackupManager.kt` |
| Colours, theme, fonts, wallpaper "System" tone | `theme/` |
| Liquid glass modifiers, sliders, switches, pull-to-refresh | `glass/` |
| Small reusable widgets (bounce click, header bar, icons) | `widgets/` (in zip) |
| Icons (vector XML) | zip `app/src/viaura-icons.zip`, see section 6 |
| CloudStream engine / extractors | zip `app/src/cloudstream-lib.zip` (see rule R4) |
| CI / build | `.github/workflows/build-apk.yml` (section 8) |

## 3. Rules (must follow)

- **R1 – Packages are NOT folders.** Folders are feature folders. Every Kotlin file keeps its original `package` (e.g. `com.example.ui.components`, `com.example.util`). Never change a package: manifest, proguard, imports and R8 rely on them. To find a symbol use section 10, not the package.
- **R2 – File names are unique** (case-insensitive) across the project. Top-level functions compile to a class named `<FileName>Kt`; two files with the same name in one package would collide in the JVM.
- **R3 – Split files share package-level symbols.** Helpers that used to be file-private and are needed by two files are `internal`. Before adding a new top-level helper, search the whole package for the same name.
- **R4 – `cloudstream-lib` file names and packages must never change.** `.cs3` extensions call facade classes such as `MainAPIKt`/`UtilsKt` by file name. Edit inside `app/src/cloudstream-lib.zip` only (unzip → edit → re-zip with the same layout).
- **R5 – No new big open-source files in git.** Fonts, python stdlib, yt-dlp, QuickJS, CA bundle, libpython are downloaded by CI (section 8). Do not commit them.
- **R6 – Small-file folders stay zipped** (section 7). Edit the unpacked copy locally, then re-zip with `cd app/src && zip -X -r <zip> <folder>`.
- **R7 – UI**: reuse `glass/` modifiers, `theme/` tokens, `viaura_*` icons. Use `ViauraSlider` / `ViauraSwitch`, never raw Material `Slider`/`Switch`. Settings files import `com.example.ui.i18n.Text` so texts are translated.
- **R8 – Languages**: user-visible text is the English string in code; translations are keys of the three JSON files (`app/src/main/assets/lang/`). Add a new text to all three.
- **R9 – No APK build / no emulator in AI sessions**; static checks only. Never claim "verified" for something that was not run.
- **R10 – Torrents are download-only in the UI**; `torrent/TorrentLoopbackStreamServer.kt` is only used by provider links (`providers/playback/PluginStreamResolver.kt`, `providers/ProviderApiRepository.kt`).
- **R11 – Do not rename or move files again without** updating `.github/legacy-paths.txt` (old paths removed by CI) and this file.


## 4. Folder-by-folder file list

All folders are under `app/src/main/java/com/example/`. The Kotlin `package` of a file is NOT the folder (see rule R1); the package is shown in brackets once per folder. Files inside a zip are marked **[in zip]** (see section 7).


### `ai/`  — packages: `com.example.ai`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `AiProviderCatalog.kt` | Cloud AI providers selectable from Settings > Advanced > AI Features > Provider. | AiProvider, AiModelInfo, BestModelSelector, ApiKeyVerificationState | 69 |
| `AiProviderHttpGateway.kt` | Talks to whichever [AiProvider] the user configured. Every provider except Anthropic exposes | AiProviderService | 355 |
| `AiSettingsPrefsStore.kt` | All persisted state for Settings > Advanced > AI Features. | AiFeaturesSettings, AiFeaturesSettingsStore | 108 |
| `AiSubtitleTranslator.kt` | Format-preserving subtitle translation pipeline. | SubtitleTranslationService | 375 |
| `ApiKeyKeystoreVault.kt` | Encrypts the user's AI provider API key at rest using an Android Keystore AES key, | ApiKeyCrypto | 68 |
| `TranslationLanguageCatalog.kt` | Per-track state shown in the subtitle track sheet while/after AI translation. | TrackTranslationStatus, TranslateLanguages | 90 |

### `app/`  — packages: `com.example.util`, `com.example.ui.components`, `com.example`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `AppEntryActivity.kt` | Contains Screen, MainActivity | Screen, MainActivity | 1109 |
| `AppStorageFolders.kt` | AppStorageManager | AppStorageManager | 176 |
| `CrashReasonRecorder.kt` | Keeps the reason of the last crash so it can be read on the phone itself (no computer / logcat needed). | CrashReporter | 101 |
| `CrashReportSheet.kt` | Shows the reason of the previous run's crash (see CrashReporter). Same bottom sheet as the Download sheet (it follows the | CrashReportDialog | 102 |
| `HapticFeedbackController.kt` | Centralized, global haptic feedback controller for Viaura. | AppHaptics | 145 |
| `PictureInPictureState.kt` | Lightweight global holder so any Composable can react to Picture-in-Picture mode | PipModeState | 24 |
| `StartupSplashScreen.kt` | Launch animation shown right after the app is opened from the home screen. | AppLaunchSplash | 236 |

### `app/permissions/`  — packages: `com.example.ui.components`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `StoragePermissionHelper.kt` | Contains getRequiredMediaPermissions, hasAllFilesAccess, hasVideoPermission, openAppSettings, openAllFilesAccessSettings … | StoragePermissionDialog | 493 |

### `app/update/`  — packages: `com.example.update`, `com.example.ui.components`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `AppUpdateModels.kt` | Which GitHub releases the in-app updater looks at. | UpdateChannel, AppUpdateInfo | 61 |
| `GithubReleaseUpdater.kt` | In-app updater (GitHub Releases, no Play Store). | AppUpdater | 474 |
| `ReleaseNotesMarkdownView.kt` | Small Markdown renderer for the GitHub release notes (headings, bullets, numbered lists, bold, code, links, quotes) | mdComment, mdImage, mdHtmlTag, mdHeading | 207 |
| `UpdateSettingsCardAndSheet.kt` | Settings > About > "Updates" card (auto check switch, channel, "Check for Updates Now") | UpdatesSettingsSection, AppUpdateHost | 535 |

### `audio/`  — packages: `com.example.player`, `com.example.util`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `AudioQueuePlaybackCore.kt` | Global Singleton Audio Playback Engine for the entire application. | AudioRepeatMode, AudioPlaybackManager | 2127 |
| `AudioResumeHistoryStore.kt` | Lightweight persistent resume history for Viaura's audio player. | AudioPlaybackHistoryManager, AudioPlaybackResume | 246 |
| `AudioTagMetadataReader.kt` | Advanced Audio Metadata & Embedded Lyrics Extractor. | AudioMetadataExtractor | 661 |
| `DisabledOnlineAudioCacheStub.kt` | Legacy online-track disk cache disabled by design. User-triggered media downloads and automatic full-track | OnlineAudioCache | 39 |
| `PerVideoAudioChoiceStore.kt` | Remembers, PER VIDEO, which audio track (language) the user picked, so that leaving the | SavedAudioRef, VideoAudioSelectionMemory | 196 |

### `audio/artwork/`  — packages: `com.example.ui.components`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `ArtworkColorExtractor.kt` | Two colours taken from a thumbnail: [top] for the upper part of the screen, [bottom] for the lower part. | ArtworkColors | 79 |
| `AudioArtworkDiskCaches.kt` | Asynchronous, cached loader for audio album artwork & track thumbnails. | RemoteAudioThumbnailDiskCache, LocalAudioThumbnailDiskCache | 142 |
| `AudioArtworkLoader.kt` | Asynchronous, cached loader for remote audio thumbnails / artwork URLs with multi-tier high-res fallbacks. | AudioTrackThumbnail | 409 |

### `audio/library/`  — packages: `com.example.ui.screens`, `com.example.util`, `com.example.ui.components`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `AudioEmptyStateViews.kt` | Contains EmptyAudioView, EmptyAudioFoldersInlineView | EmptyAudioView, EmptyAudioFoldersInlineView | 260 |
| `AudioEqualizerBars.kt` | Animated 4-bar equalizer for active playing track | AnimatedAudioEqualizer | 232 |
| `AudioFileOperations.kt` | Contains AudioFileManager | AudioFileManager | 517 |
| `AudioFilePickerDialog.kt` | Contains STRICT_AUDIO_EXTENSIONS, RESTRICTED_AUDIO_FOLDERS, isRestrictedAudioPath, AudioFileItem, AudioFileTreeDialog | AudioFileItem, AudioFileTreeDialog | 700 |
| `AudioFolderCards.kt` | Audio Folder Card - List Layout | OnlineFolderCard, AudioFolderListCard, AudioFolderGridCard | 540 |
| `AudioFolderVideoPickerSheet.kt` | Bottom Sheet Modal for selecting target folders and individual videos for preferred audio rules. | AudioFolderVideoSelectorSheet | 976 |
| `AudioLibraryDiskCache.kt` | Contains AudioLibraryCache | AudioLibraryCache | 466 |
| `AudioLibraryModels.kt` | Sentinel path identifying the virtual "Online" folder (search/stream any song online). | AudioTrackItem, AudioFolderItem | 256 |
| `AudioMusicTab.kt` | Music & Audio Tab Content. | AudioMusicTabContent | 1007 |
| `AudioTrackCards.kt` | Audio Track Card - List Layout | AudioTrackListCard, AudioTrackGridCard | 793 |

### `audio/notification/`  — packages: `com.example.player`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `AudioNotificationActionReceiver.kt` | BroadcastReceiver for handling media notification actions (Play/Pause, Next, Previous, Stop). | AudioNotificationReceiver | 39 |
| `AudioNotificationBuilder.kt` | Handles music playback system notification, MediaSessionCompat, | AudioNotificationManager | 504 |
| `AudioPlaybackForegroundService.kt` | Foreground Service for persistent audio playback and media notification controls. | AudioPlaybackService | 178 |

### `audio/panels/`  — packages: `com.example.ui.components`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `AudioDelayPanel.kt` | 2B. AUDIO DELAY & SYNC PANEL | AudioDelayPanel | 559 |
| `AudioTrackSelectionPanel.kt` | 2. AUDIO TRACK SELECTION PANEL | AudioSelectionPanel | 688 |

### `audio/settings/`  — packages: `com.example.player`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `AudioChannelAndEqModels.kt` | Audio channel configuration modes for MPV player and audio output. | AudioChannelMode, AudioSurroundMode, TrackAudioConfig, EqualizerPreset | 123 |
| `AudioLanguageMatching.kt` | Intelligent Language normalization and matching engine. | AudioLanguageMatcher | 308 |

### `audio/ui/`  — packages: `com.example.ui.components`, `com.example.ui.screens`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `AudioPlayerFullScreen.kt` | Dedicated Full-Screen Audio Player UI. | AudioPlayerScreen | 3255 |
| `AudioSeekScrubber.kt` | Contains InteractiveAudioScrubber, formatAudioScrubberTime | InteractiveAudioScrubber | 309 |
| `MiniAudioPlayerBar.kt` | Minimized Floating Audio Player. | MinimizedAudioPlayer | 380 |

### `download/`  — packages: `com.example.util`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `DownloadForegroundNotificationService.kt` | Contains DownloadForegroundService | DownloadForegroundService | 227 |
| `DownloadPublicFolderSaver.kt` | Moves a finished download from the app's private download folder into the public Movies/Viaura (or Music/Viaura) | DownloadFileSaver | 126 |
| `DownloadQueueManager.kt` | One coordinator for every download of the app: Online Videos, Online Audio, links pasted in Local Network, Media | DownloadManager | 557 |
| `DownloadRecordStore.kt` | Download list. It is kept on disk; unfinished downloads are started again by [DownloadManager.resumeInterrupted]. | DownloadStore | 105 |

### `download/http/`  — packages: `com.example.download`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `ChunkedHttpFetcher.kt` | Contains HttpFetch | HttpFetch | 369 |
| `FetchExceptionsAndTokens.kt` | The link has expired (403 / 404 / 410): the page has to be asked for fresh links. | FetchCancelled, HttpStatusException, LinkExpiredException, RangeIgnored, CancelToken, HeadInfo | 63 |
| `HlsDashSegmentFetcher.kt` | HLS (.m3u8) and DASH-fragment streams: every segment is fetched in parallel, finished segments are kept, then joined into one file. | SegmentFetch | 236 |

### `download/model/`  — packages: `com.example.util`, `com.example.download`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `DownloadItemModels.kt` | One download. It is saved to disk, so after an app restart it continues from the point where it stopped. | DownloadStatus, DownloadSourceType, DownloadItem, DownloadFormat | 84 |
| `DownloadPlanModels.kt` | One file that has to be fetched (a video stream, an audio stream or a complete muxed file). | PlanPart, PlanSubtitle, PlanChapter, DownloadPlan | 125 |

### `download/mux/`  — packages: `com.example.download`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `MkvStreamMuxer.kt` | Joins separately downloaded streams (video, one or many audio tracks, subtitles) into ONE Matroska file without | MkvMuxer | 630 |
| `MuxInputModels.kt` | Contains UnsupportedMuxException, MuxSource, MuxSubtitle | UnsupportedMuxException, MuxSource, MuxSubtitle | 18 |

### `download/probe/`  — packages: `com.example.download`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `MediaContainerDetector.kt` | Finds the REAL container of a video from its first bytes (never from a guess): "mkv", "webm", "mp4", "mov", "m4v", "3gp", | ContainerSniffer | 55 |
| `OnlineMediaProber.kt` | Contains MediaProbe | MediaProbe | 491 |
| `ProbeResultModels.kt` | A stream as yt-dlp lists it (nothing is guessed: every field comes from the website's own answer). | RawFormat, VideoChoice, AudioChoice, LinkInfo | 94 |

### `download/tagging/`  — packages: `com.example.download`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `AudioTagWriter.kt` | Writes title / artist / lyrics / cover into M4A, MP3 and native FLAC metadata without re-encoding audio. | AudioTagger | 308 |

### `download/ui/`  — packages: `com.example.ui.components`, `com.example.ui.screens`, `com.example`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `DownloadGlassSheetFrame.kt` | Translucent bottom sheet used by every download panel (same glass colour as the other app sheets). | DownloadGlassSheet | 274 |
| `DownloadSheetWidgets.kt` | Building blocks shared by every download panel (online video / audio, local network link, media provider title), so that all of | SheetTrailing, SheetChip, DownloadSheetHeader, DownloadModeTabs, SelectorRow, SheetCheckMark … | 424 |
| `DownloadsListEntries.kt` | True when a downloaded song has lyrics: a sibling .lrc file, or lyrics embedded in its tags. | DownloadsContentEntry | 235 |
| `DownloadsTab.kt` | Downloads Screen: Completely redesigned to match folder UI 1:1. | DownloadsTabContent | 1258 |
| `OnlineDownloadOptionsSheet.kt` | Download panel of every source (Online Video / Online Audio / Local Network link / Media provider link). The qualities, | DownloadOptionsSheet | 559 |
| `ShareLinkQuickDownloadActivity.kt` | "Quick download" share target: a link shared from another app (Instagram, YouTube ...) opens Viaura's download panel | QuickDownloadActivity | 78 |

### `glass/`  — packages: `com.example.ui.components`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `EdgeToEdgeDialogWindow.kt` | Ensures any Compose Dialog window expands strictly edge-to-edge across 100% of the display, | EdgeToEdgeSheetWindowEffect | 183 |
| `GlassPanelTokens.kt` | The app-size setting (1..100), provided once by the app theme so every panel / sheet can follow it. | LiquidGlassParams, ViauraSurfaceStyle, PanelScaled | 126 |
| `GlassShadowModifier.kt` | Drop-in replacement for `Modifier.shadow(...)` used across the app (files import this one instead of the Compose one). | shadow | 33 |
| `LiquidGlassModifiers.kt` | LIQUID GLASS (Settings > Interface Appearance > "Liquid Glass") | LiquidBackdropState, LiquidDialogBlur | 411 |
| `LiquidGlassPreviewCanvas.kt` | Live preview for the Liquid Glass sliders (Settings > Interface Appearance). A coloured checker board stands in for | LiquidGlassPreview | 169 |
| `LiquidGlassSurfaces.kt` | Contains LiquidGlassDefaults, LiquidGlassCard, LiquidGlassDialog | LiquidGlassDefaults, LiquidGlassCard, LiquidGlassDialog | 166 |
| `LiquidPullRefreshIndicator.kt` | Android 16 / Material 3 Expressive Content-Push Pull-To-Refresh. | LiquidPullToRefresh, ViauraExpressiveLoadingIndicator | 667 |
| `VideoFrameGlassBackdrop.kt` | LIQUID GLASS FOR THE VIDEO PLAYER CONTROLS | VideoGlassSource, VideoGlassSampler | 353 |

### `glass/controls/`  — packages: `com.example.ui.components`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `LiquidSeekbarDrawing.kt` | Colours / geometry helpers | glassGradientColors, sampleGradient, seekbarStyleHasThumb, seekbarFillAlpha | 417 |
| `ViauraSlidersAndSwitches.kt` | LIQUID GLASS CONTROLS (Settings > Interface Appearance > Liquid Glass > "Liquid Glass Sliders & Switches") | LiquidGlassSlider, ViauraSlider, LiquidGlassSwitch, ViauraSwitch, LiquidGlassVerticalBar | 491 |

### `library/`  — packages: `com.example.util`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `EpisodeNumberParser.kt` | Detects season / episode numbers (and the show title) from a video file name written in almost any | ParsedEpisode, EpisodeNameParser | 153 |
| `NaturalSortComparator.kt` | Natural order comparator for strings that naturally orders numbers numerically | NaturalOrderComparator | 119 |
| `StorageVolumeRoots.kt` | Returns the shared-storage volume roots that Viaura can enumerate. | accessibleStorageRoots | 56 |
| `StubPlaceholderVideoDetector.kt` | Detects "stub" MP4 files: a tiny, valid MP4 (usually a ~10 s placeholder clip saying the format is | PackagedStubVideoDetector | 74 |
| `VideoFileOperations.kt` | Contains VideoFileManager | VideoFileManager | 606 |
| `VideoLibraryDiskCache.kt` | Contains CachedVideoMeta, VideoLibraryCache | CachedVideoMeta, VideoLibraryCache | 543 |

### `library/archive/`  — packages: `com.example.util.archive`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `ArchiveByteSources.kt` | An archive the user picked with the system file picker (content:// or file://). Read in place, never copied. | UriArchiveSource, HttpArchiveSource, LimitedInputStream | 178 |
| `ArchiveEntryRandomAccess.kt` | One video INSIDE an archive as a random-access data source for Android's MediaMetadataRetriever / MediaExtractor and for | ArchiveEntryDataSource, ArchiveSeekableReader | 190 |
| `ArchiveLoopbackStreamServer.kt` | Plays a video that sits INSIDE an archive without extracting or copying it: mpv opens http://127.0.0.1:<port>/arc/<id>/<index>/<name> | ArchiveStreamServer | 222 |
| `ArchiveModels.kt` | Random-access reading of ZIP / TAR archives WITHOUT copying or extracting them. The archive stays where it is (a file on the | ArchiveEntry, ArchiveKind, ArchiveScan, ArchiveException, ArchiveSource | 63 |
| `ArchiveRecordStore.kt` | Everything a playlist needs to play the videos of one archive again later (where it is + its table of contents). | ArchiveRecord, ArchiveStore | 228 |
| `ArchiveTocEngine.kt` | Contains ArchiveEngine | ArchiveEngine | 335 |
| `ArchiveVideoProber.kt` | Finds out duration, resolution, frame rate and subtitle tracks of the videos inside an archive, and gives the thumbnail | ArchiveProbe | 83 |

### `library/cards/`  — packages: `com.example.ui.components`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `VideoGridItemCard.kt` | Grid View card for Video Items | VideoItemGridCard | 334 |
| `VideoListItemCard.kt` | Contains formatDuration, resolveSubtitleBadgeText, VideoItemCard, ReorderTwoLineHandle | VideoItemCard | 465 |

### `library/files/`  — packages: `com.example.ui.components`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `DirectoryPickerSheet.kt` | Contains DirectoryPickerAction, RESTRICTED_DIR_NAMES, isRestrictedDirectory, DirEntry, FloatingDirectoryPickerSheet | DirectoryPickerAction, DirEntry, FloatingDirectoryPickerSheet | 786 |
| `MoveCopyProgressBar.kt` | Contains BottomMoveCopyProgressBar | BottomMoveCopyProgressBar | 215 |

### `library/folders/`  — packages: `com.example.util`, `com.example.ui.components`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `FolderBlockList.kt` | Contains FolderBlockListManager | FolderBlockListManager | 67 |
| `FolderCreationTime.kt` | Contains FolderDateUtils | FolderDateUtils | 70 |
| `FolderGlyphIcon.kt` | Which ready-made folder artwork [ModernFolderIcon] shows. AUTO picks it from the glyph type. | FolderIconKind, FolderGlyphType, ModernFolderIcon | 175 |

### `library/history/`  — packages: `com.example.util`, `com.example.ui.screens`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `RecentlyPlayedTab.kt` | Contains RecentsFolder, RecentlyPlayedTabContent | RecentsFolder, RecentlyPlayedTabContent | 1560 |
| `SeenUnplayedTracker.kt` | Unified, persistent Seen/Unplayed media manager for both Video and Audio across Viaura. | MediaSeenManager | 180 |
| `VideoAudioSideMemory.kt` | Remembers which side ("VIDEO" or "AUDIO") the user was on inside the Recents and Playlists sections, so | MediaSideMemory | 33 |
| `VideoPlaybackHistoryStore.kt` | Global persistent playback history and resume manager. | PlaybackHistoryManager | 477 |
| `VideoPlaybackRecordModel.kt` | Data model for persisted video playback records. | VideoPlaybackRecord | 46 |

### `library/home/`  — packages: `com.example.ui.screens`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `HomeFolderTreeHelpers.kt` | Recursively removes just-deleted videos/folders from an already-scanned [VideoFolder] tree, | pruneDeletedFromFolder, findFolderInTree | 309 |
| `HomeLibraryScreen.kt` | Contains HomeScreen | HomeScreen | 6655 |

### `library/info/`  — packages: `com.example.ui.screens`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `MediaInfoModels.kt` | Contains MediaInfoSection, MediaInfoData | MediaInfoSection, MediaInfoData | 144 |
| `MediaInfoSectionsContent.kt` | Contains MediaInfoContent | MediaInfoContent | 423 |
| `MediaInformationPage.kt` | Contains MediaInformationScreen | MediaInformationScreen | 619 |
| `MediaOverviewCard.kt` | Contains OverviewCard | OverviewCard | 1030 |

### `library/m3u/`  — packages: `com.example.util`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `M3uPlaylistModels.kt` | One playable entry of an M3U / M3U8 playlist (IPTV channel or plain media URL). | M3uEntry, M3uParseResult | 25 |
| `M3uPlaylistTextParser.kt` | Pure Kotlin/JVM parser (no Android classes) so it can be unit-tested on the JVM. | M3uPlaylistParser | 195 |

### `library/metadata/`  — packages: `com.example.util`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `ExtractedMediaMetadataModel.kt` | Unified metadata extraction result for a video file. | ExtractedMediaMetadata | 39 |
| `MediaFileMetadataExtractor.kt` | Centralized, authoritative media metadata extractor for video dimensions, exact FPS, | MediaMetadataExtractor | 1189 |

### `library/mkv/`  — packages: `com.example.util`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `MkvContainerModels.kt` | MARK: - Data Models for MKV Track Manager | MkvTrackType, MkvTrackInfo, MkvChapterInfo, MkvAttachmentInfo, MkvContainerMetadata | 299 |
| `MkvEbmlEngine.kt` | MARK: - Core MKV Engine (EBML Parser & Demuxer / Remuxer) | MkvManagerEngine | 2709 |
| `SeekableByteReaders.kt` | Minimal random-access byte reader used by the MKV / MP4 container parsers. The member names are the same as the ones of | SeekableReader, FileSeekableReader | 80 |

### `library/playlists/`  — packages: `com.example.util`, `com.example.ui.components`, `com.example.ui.screens`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `AddToPlaylistPickerDialog.kt` | Contains PENDING_PLAYLIST_ID, AddToPlaylistDialog | AddToPlaylistDialog | 738 |
| `CustomPlaylistStore.kt` | True for every path that points to an online item: online audio ("online://"), online video ("online-video://") | CustomPlaylist | 405 |
| `CustomPlaylistsTab.kt` | Contains CustomPlaylistsTabContent | CustomPlaylistsTabContent | 2391 |
| `FavouritesFolderStore.kt` | The "Favourites" folder of the playlist section: one for the video side, one for the audio side. It is never stored; | FavouritesFolder | 77 |
| `PlaylistFavouriteMarkStore.kt` | Remembers which items of a playlist the user marked as Favourite (heart) or Saved (bookmark). | PlaylistMarkStore | 50 |
| `PlaylistFileImporter.kt` | Real playlist import: downloads / reads the actual content, parses it and stores the result in the | PlaylistImporter | 600 |
| `PlaylistImportResultModel.kt` | Outcome of an import. The UI shows [Failure.message] as-is, so it is written for the user. | PlaylistImportResult | 40 |
| `PlaylistImportSheets.kt` | 3. Add Playlist from URL / local M3U file | AddPlaylistFromUrlSheet, AddArchivePlaylistSheet | 335 |
| `PlaylistOptionsAndCreateSheets.kt` | Same translucent "liquid glass" container colour the app's other bottom sheets use. | PlaylistOptionsSheet, CreateEmptyPlaylistSheet | 355 |
| `PlaylistQuickTargets.kt` | An online item of an online folder that the swipe toolbar acts on (audio track or video). | PlaylistQuickTarget | 189 |
| `PlaylistRevealSwipeRow.kt` | Left swipe on a playlist card: the card slides left, stops exactly over the width of the revealed icons and the | PlaylistRevealSwipe, PlaylistFilterTabs | 301 |
| `SharedPlaylistHeaderCard.kt` | Contains PlaylistHeaderSection | PlaylistHeaderSection | 710 |
| `SharedPlaylistPage.kt` | Section Header: ♫ AUDIOS (count) matching the offline player UI | SharedPlaylistScreen | 751 |

### `library/rename/`  — packages: `com.example.ui.components`, `com.example.util`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `BulkRenameComputation.kt` | Contains BulkRenameEngine | BulkRenameEngine | 156 |
| `BulkRenameEditorBody.kt` | Contains BulkRenameBody | BulkRenameBody | 753 |
| `BulkRenameModels.kt` | One file taking part in a bulk rename. [key] is its full path (unique). | BulkRenameMode, BulkCaseFormat, BulkRenameItem, BulkRenameConfig, BulkRenameStatus, BulkRenamePreview | 52 |
| `BulkRenameSheetHost.kt` | Bulk Rename bottom sheet (opens from the Rename button of the selection bar when 2+ videos are selected). | BulkRenameSheet | 296 |
| `InlineRenameTextField.kt` | Contains InlineRenameField | InlineRenameField | 156 |

### `library/scan/`  — packages: `com.example.ui.screens`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `MediaSizeAndDurationFormat.kt` | Contains formatDuration, formatFileSize | formatDuration, formatFileSize | 50 |
| `VideoFolderScanEngine.kt` | Contains VideoFolderScanner | VideoFolderScanner | 529 |
| `VideoLibraryModels.kt` | Contains VideoItem, VideoFolder | VideoItem, VideoFolder | 161 |

### `library/sort/`  — packages: `com.example.ui.components`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `SortAndViewModels.kt` | Contains SortField, SortDirection, ViewMode, LayoutMode, VisibleFields | SortField, SortDirection, ViewMode, LayoutMode, VisibleFields | 123 |
| `SortAndViewOptionsPopup.kt` | Contains SortAndViewOptionsPopup | SortAndViewOptionsPopup | 1071 |
| `SortAndViewPillControls.kt` | Top Card for Sort Field (Title, Date, Size) | SortTypeCard, SortDirectionPillToggle, ViewModePillToggle, LayoutPillToggle | 506 |

### `library/thumbnails/`  — packages: `com.example.ui.components`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `VideoThumbnailCaches.kt` | Bounded in-memory cache for list thumbnails. | ThumbnailCache, VideoThumbnailDiskCache | 181 |
| `VideoThumbnailLoading.kt` | Dedicated bounded dispatcher for thumbnail generation so fast scrolling | thumbnailDispatcher, prefetchThumbnailDispatcher, peekCachedVideoThumbnail, loadVideoThumbnail | 363 |
| `VideoThumbnailPrefetch.kt` | Process-wide thumbnail warming. Unlike a LaunchedEffect inside one screen, these jobs keep running | ThumbnailPrefetcher | 180 |
| `VideoThumbnailViews.kt` | Contains VideoThumbnailCard, VideoThumbnailLoader | VideoThumbnailCard, VideoThumbnailLoader | 294 |

### `library/vault/`  — packages: `com.example.util`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `PrivateVaultModels.kt` | Contains PrivateMediaType, PrivateFolder | PrivateMediaType, PrivateFolder | 49 |
| `PrivateVaultStore.kt` | Contains PrivateVaultManager | PrivateVaultManager | 510 |

### `lyrics/`  — packages: `com.example.util`, `com.example.repository.lyrics`, `com.example.utils.media`, `com.example.preferences`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `LocalLyricsSidecarReader.kt` | Local lyric extraction for the repository; the player also refreshes metadata through its existing extractor. | EmbeddedLyricsExtractor | 89 |
| `LrcTextReader.kt` | Shared LRC reader used by the lyrics UI and the online lyrics fetcher. | LrcParser | 94 |
| `LrcTimingHelpers.kt` | LRC/enhanced-LRC parser and playback-position helpers shared by lyrics UI and providers. | LyricsUtils | 260 |
| `LyricsLookupRepository.kt` | Where the player gets its lyrics. | LyricsRepository | 777 |
| `LyricsLookupResults.kt` | Lyrics for one track, plus which of its sources they came from. | LyricsResult, OnlineLyricsResult | 82 |
| `LyricsPreferenceStore.kt` | Small lyrics-only preference surface matching the reference repository's contract. | AudioPreferences | 32 |
| `LyricsSourceResultCache.kt` | Contains LyricsProviderCache | LyricsProviderCache | 99 |
| `LyricsToLrcConverter.kt` | Converts provider models to enhanced LRC so the existing audio library/cache remains compatible. | LyricsTextCodec | 35 |
| `LyricsWebFetcher.kt` | Online lyrics lookup for ANY track (local files and online music) - a port of the way MpvRx finds | LyricsFetcher | 723 |
| `OnlineLyricsGateway.kt` | Application-facing bridge to the full MpvRx provider repository. | OnlineLyricsService | 103 |

### `lyrics/model/`  — packages: `com.example.domain.lyrics`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `LyricsDataModels.kt` | Contains LyricsSourceType, Lyrics, SyncedLine, SyncedWord | LyricsSourceType, Lyrics, SyncedLine, SyncedWord | 48 |
| `LyricsSourceCatalog.kt` | Online lyrics databases the repository can ask, in default priority order. | LyricsProvider | 123 |

### `lyrics/online/`  — packages: `com.example.data.lyrics`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `LrcLibHttpApi.kt` | Contains LrcLibResponse, LrcLibApiService | LrcLibResponse, LrcLibApiService | 117 |

### `lyrics/providers/`  **[in zip `app/src/lyrics-providers-src.zip`]**  — packages: `com.example.data.lyrics.providers`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `BetterLyricsSource.kt` | Word-timed lyrics from BetterLyrics — the backend behind the YouTube Music | BetterLyricsProvider | 48 |
| `BiniLyricsSource.kt` | Apple Music TTML from a third host, and the only provider here that will | BiniLyricsProvider | 99 |
| `GeniusLyricsScraper.kt` | Web scrape of Genius.com, used strictly as the last resort: it has no | GeniusProvider | 227 |
| `KuGouLyricsSource.kt` | Line-synced lyrics from KuGou's public mobile endpoints — a Chinese | KuGouProvider | 190 |
| `LrcLibLyricsSource.kt` | LRCLIB — a free, key-less, community database. | LrcLibProvider | 124 |
| `LrcRedLyricsSource.kt` | lrc.red: Apple Music TTML filed by ISRC. | LrcRedProvider | 144 |
| `LyricsHttpClients.kt` | Shared plumbing for the lyrics providers. | lyricsJson, LYRICS_AGENT, TIMEOUT_SECONDS, PATIENT_TIMEOUT_SECONDS | 175 |
| `LyricsPlusSource.kt` | Syllable-timed lyrics from LyricsPlus, the open backend behind the YouLy+ | LyricsPlusProvider | 195 |
| `LyricsSourceContract.kt` | One title/artist pair a lookup was given, as the playing file described it. | TrackRef, LyricsFetchQuery, LyricsProviderClient | 75 |
| `LyricsSourceRegistry.kt` | What one provider said about one track. | ProviderOutcome, LyricsProviderRegistry | 110 |
| `LyricsTextParsers.kt` | A break worth interrupting the line for. Anything shorter just reads as pacing. | TimedLine, KaraokeLrc | 354 |
| `LyricsTrackMatching.kt` | Reject fuzzy catalogue matches belonging to a different song, edit or artist. | LyricsTrackMatcher | 71 |
| `MegalobizLyricsScraper.kt` | Line-synced community LRC scraped from Megalobiz. | MegalobizProvider | 43 |
| `MusixmatchLyricsSource.kt` | Word timing with a line-synced fallback, from Musixmatch's web API. | MusixmatchProvider | 428 |
| `PaxSenixLyricsApi.kt` | PaxSenix: Apple Music lyrics through a public proxy, plus two authenticated | PaxSenixApi | 363 |
| `PaxSenixLyricsSource.kt` | The single PaxSenix route a provider instance stands for. | PaxSenixRoute, PaxSenixProvider | 46 |
| `SimpMusicLyricsSource.kt` | Lyrics from SimpMusic's community database, keyed on the YouTube video id. | SimpMusicProvider | 73 |
| `TtmlLyricsParser.kt` | A document is one `<p>` per sung line, each holding a `<span>` per syllable | TtmlLyrics | 311 |
| `UnisonLyricsSource.kt` | A community-submitted database and the only provider here whose contents are | UnisonProvider | 82 |
| `YouTubeCaptionLyricsSource.kt` | Lyrics tied to the exact video being played: its captions, and the plain | YouTubeTranscriptProvider | 98 |
| `YouTubeMusicLyricsSource.kt` | Depth-first walk over every object in a document, whatever it is nested under. | YouTubeMusicLyricsProvider | 112 |

### `lyrics/translation/`  — packages: `com.example.data.lyrics`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `LyricsLineTranslator.kt` | API-key-free lyrics translation. It uses the public Google Translate web endpoints and falls | LyricsTranslationService | 199 |

### `lyrics/ui/`  — packages: `com.example.ui.components`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `KaraokeLyricsPanel.kt` | Shared synced/plain renderer for compact player lyrics and the full-screen, karaoke-style view. | AudioLyricsContent | 463 |
| `LyricsLoadingSkeleton.kt` | Lightweight animated lyric-line placeholders shown while providers are racing. | LyricsPlaceholderLines | 58 |
| `LyricsSourcePickerDialog.kt` | Contains LyricsProviderPickerDialog, PickerRow | LyricsProviderPickerDialog | 105 |

### `network/`  — packages: `com.example.util.network`, `com.example.util`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `ConnectivityMonitor.kt` | Observes network availability and transitions between Wi-Fi and Cellular networks. | NetworkMonitor | 110 |
| `DohDnsResolver.kt` | Contains ViauraDns | ViauraDns | 227 |
| `MpvTlsCaBundleInstaller.kt` | libmpv / FFmpeg on Android has no system CA folder it can read (no /etc/ssl/certs), so with "tls-verify=yes" and no | TlsCaBundle | 82 |
| `SharedOkHttpClient.kt` | Contains ViauraHttpClient | ViauraHttpClient | 100 |

### `network/bypass/`  — packages: `com.example.util`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `LoopbackBypassProxyServer.kt` | Minimal HTTP proxy for the app's own traffic (127.0.0.1 only): CONNECT tunnels (https) and absolute-URI requests (http). | LocalBypassProxy | 380 |
| `ProviderNetworkBypassSettings.kt` | Contains NetworkBypass | NetworkBypass | 110 |
| `WebStreamLoopbackProxy.kt` | Plays a website's stream THROUGH THE APP: mpv opens http://127.0.0.1:<port>/s/<token> and this server fetches the real stream | WebStreamProxy | 184 |

### `network/settings/`  — packages: `com.example.util`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `NetworkStreamPrefs.kt` | Settings > Network: the HLS stream handling and whether subtitles of pasted links are loaded. Plain | NetworkSettings | 59 |

### `network/stream/`  — packages: `com.example.util`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `DirectStreamModels.kt` | A media URL accepted by the Local Network player. | NetworkStream, WebPlayback | 28 |
| `DirectStreamUrlResolver.kt` | Local Network: a direct video/audio link plays as it is; the link of any website is processed by yt-dlp (installed from | NetworkStreamResolver | 158 |
| `RecentStreamHistory.kt` | One entry of the Network tab's "Recent Streams" list. | RecentStreamEntry, RecentStreamStore | 113 |
| `RecentStreamThumbnailFrames.kt` | A link without an attached thumbnail still gets a picture in Recent Streams: a frame from a random point of the | RecentThumbnails | 56 |
| `StreamEntryRecordStore.kt` | Metadata of one imported IPTV / M3U stream entry (the playlist item itself only stores the stream URL). | StreamEntryMeta, StreamEntryStore | 132 |
| `StreamRequestHeaderOptions.kt` | Per-stream HTTP options handed to mpv right before a stream is opened. The player calls [forUrl]; | StreamRequestOptions | 36 |

### `network/ui/`  — packages: `com.example.ui.screens`, `com.example.ui.components`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `NetworkBypassSettingsCard.kt` | Network > Media: the settings that run in the background while provider pages and videos open. | NetworkBypassCard | 214 |
| `NetworkTab.kt` | "Network" bottom tab. Sections: Local Network (paste a direct media URL and play it), Media (plugin repositories) and one | NetworkTabContent | 507 |
| `RecentStreamRows.kt` | Contains RecentStreamRow, RecentActionButton | RecentStreamRow, RecentActionButton | 174 |

### `online/`  — packages: `com.example.util`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `OnlineWatchLaterAndQueueStore.kt` | Contains OnlineMediaLibrary, OnlineVideoQueue | OnlineMediaLibrary, OnlineVideoQueue | 169 |
| `SubscribedChannelStore.kt` | A channel the user subscribed to inside the app (local only - nothing is sent to YouTube). | SubscribedChannel, SubscribedChannelsManager | 69 |

### `online/model/`  — packages: `com.example.util`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `OnlineChannelResultModel.kt` | A channel/artist result for Online Music / Audio. | OnlineChannelResult | 14 |

### `online/music/`  — packages: `com.example.util`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `NextTrackSequencer.kt` | Order of the "next tracks" of Online Audio / Online Video. | OnlineTrackSequencer | 95 |
| `OnlineMusicModels.kt` | Contains OnlineTrackResult, StreamInfoWithRelated, OnlineChannelTrackPage, OnlineChannelPlaylist, OnlineChannelSection … | OnlineTrackResult, StreamInfoWithRelated, OnlineChannelTrackPage, OnlineChannelPlaylist, OnlineChannelSection, OnlineChannelDetail | 92 |
| `OnlineTrackMetaDiskCache.kt` | Persistent storage for online track metadata (thumbnails, title, artist, duration, etc.). | OnlineTrackMetadataCache | 140 |
| `OnlineTrackVideoIdMap.kt` | Contains OnlineTrackVideoIds | OnlineTrackVideoIds | 7 |
| `YouTubeMusicSearchService.kt` | Singleton — Online Music search + stream-URL resolution, backed by NewPipeExtractor against | OnlineMusicService | 1119 |

### `online/recommend/`  — packages: `com.example.util`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `MusicRecommendationBlender.kt` | Contains OnlineRecommendationMixer | OnlineRecommendationMixer | 391 |
| `VideoFeedRecommender.kt` | YouTube-like personalised feed for Online Video. | OnlineVideoRecommender | 524 |
| `ViewerInterestProfile.kt` | What the user is into, learned locally from what they open / play and what they search (Online Video and Online Audio | OnlineInterestProfile | 305 |

### `online/search/`  — packages: `com.example.util`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `OnlineSearchResultRanker.kt` | Contains OnlineSearchRanker | OnlineSearchRanker | 121 |

### `online/ui/`  — packages: `com.example.ui.components`, `com.example.ui.screens`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `ChannelDetailPage.kt` | Viaura-native Channel Detail screen shown after tapping an artist / channel result in online | ChannelDetailScreen | 1288 |
| `ChannelSearchResultCard.kt` | YouTube channel result card shown when a search query matches a creator/channel. | OnlineChannelResultCard | 207 |
| `OnlineAudioFeedHolder.kt` | Path prefix used to tag AudioTrackItem instances that came from Online Music, not disk. | OnlineSearchBridge, OnlineAudioFeedState | 172 |
| `OnlineCardSwipeToolbar.kt` | Right-to-left swipe on an online card. The card only follows the finger a little (it is NOT selected / highlighted), | DownloadRequest, OnlineQuickActionBar, DownloadRequestHost | 251 |
| `OnlineMusicHomePanel.kt` | Online Music browsing and search panel. | OnlineMusicPanel | 956 |
| `OnlineSearchSuggestionList.kt` | Type-ahead list: up to five popular searches for what is typed, refreshed on every letter that is typed or deleted. | OnlineSearchSuggestions | 211 |
| `OnlineVideoFeedHolder.kt` | Process-wide state of the Online Video folder. It lives outside composition on purpose: when the user opens a video | OnlineVideoFeedState | 151 |
| `OnlineVideoHomePanel.kt` | Online Video browsing panel (inside the "Online Video" folder): recommendation feed when the search box is empty, | OnlineVideoPanel | 669 |

### `online/video/`  — packages: `com.example.util`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `OnlineVideoModels.kt` | One selectable video quality of an online video (e.g. "1080p60"). [url] is a direct stream URL. | OnlineVideoQuality, OnlineAudioStreamInfo, OnlineChapterInfo, OnlineVideoPlayback, OnlineWatchDetails, OnlineComment … | 75 |
| `YouTubeVideoStreamService.kt` | Online Video — resolves YouTube watch URLs to direct streams through NewPipeExtractor, exactly like | OnlineVideoService | 289 |

### `player/`  — packages: `com.example.util`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `PlayerScreenshotSaver.kt` | Contains VideoScreenshotSaver | VideoScreenshotSaver | 181 |
| `PortraitHeaderButtonRequest.kt` | The three Home header buttons that stay available in the portrait pinned player header. | PlayerHeaderAction, PlayerHeaderRequest | 15 |
| `SleepTimerController.kt` | App-wide Sleep Timer (one timer shared by the video player, the offline audio player and the | SleepTimerManager | 109 |

### `player/chapters/`  — packages: `com.example.player`, `com.example.ui.components`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `ChapterSkipButton.kt` | Contains PlayerChapterSkipButton | PlayerChapterSkipButton | 127 |
| `ChapterSkipDetection.kt` | Contains ChapterSkipDetector | ChapterSkipDetector | 166 |
| `ChapterSkipModels.kt` | Contains SkipMarkerType, ChapterSkipMarker | SkipMarkerType, ChapterSkipMarker | 30 |
| `IntroSkipMarkerClients.kt` | Read-only clients for the public intro/skip marker providers exposed by Player Settings. | OnlineSkipMarkerService | 155 |

### `player/filters/`  — packages: `com.example.player`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `VideoFilterPresets.kt` | 12 Rich Real-Time Video Filter Presets | VideoFilterPreset, ManualVideoAdjustments | 146 |

### `player/layout/`  — packages: `com.example.ui.components`, `com.example.ui.state`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `ControlLayoutEditorScreen.kt` | Renders active items inside an area with touch & hold (long-press) continuous free drag reordering, | ControlLayoutEditor | 872 |
| `PlayerControlLayoutModels.kt` | Orientation modes for customizing the player controls layout. | LayoutOrientationMode, ControlArea, PlayerControlId, PlayerLayoutConfig | 503 |

### `player/mini/`  — packages: `com.example.player`, `com.example.ui.components`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `MiniVideoPlayerPopup.kt` | Height of the picture area: identical to the audio mini player's artwork, so the pill is exactly as tall as before. | MinimizedVideoPlayer | 466 |
| `VideoMiniForegroundService.kt` | Foreground service that keeps the video mini player alive (and its sound playing) when the screen is turned off | VideoMiniPlaybackService | 154 |
| `VideoMiniSessionManager.kt` | One video that keeps playing after the user leaves the full-screen video player (Settings > Player > | VideoMiniSession, VideoMiniPlayerManager | 247 |

### `player/mpv/`  — packages: `com.example.player`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `MpvEngineController.kt` | Contains MpvPlayerController | MpvPlayerController | 5333 |
| `MpvSurfaceHostView.kt` | Contains VideoPlayerMpvView | VideoPlayerMpvView | 223 |
| `MpvTrackAndChapterModels.kt` | One audio stream (original / dubbed / auto-translated) of an online video, attached as an external mpv audio track. | PlayerMediaTrack, OnlineAudioSource, LiveSeekRange, PlayerVideoChapter | 112 |

### `player/overlay/`  — packages: `com.example.ui.screens`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `ClockBatteryBadge.kt` | Clean floating time and network clock pill badge. | TimeNetworkClockBadge | 374 |
| `PlayerGlassStyling.kt` | Glass background / border / icon-tint helpers of the player overlay | LocalGlassBlurTransparency, LocalPlayerSettings, getEffectiveGlassBg, getEffectiveGlassBorder | 387 |
| `PlayerModeEnums.kt` | Player panel type, decoder mode, repeat mode and aspect-ratio enums | PlayerPanelType, DecoderMode, PlayerRepeatMode, AspectRatioMode | 317 |
| `SystemCastPicker.kt` | Opens the system-level Cast / Smart View / Wireless Display screen, which actively searches for nearby | openSystemCastPicker | 310 |
| `VideoPlayerControlsOverlay.kt` | Contains GestureLockMode, VideoPlayerOverlay | VideoPlayerOverlay | 4705 |

### `player/panels/`  — packages: `com.example.ui.components`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `ChapterSelectionPanel.kt` | 3. CHAPTER SELECTION PANEL (Matches reference UI exactly) | ChapterSelectionPanel | 311 |
| `DelayPanelControls.kt` | Shows " ms" after the typed digits inside the delay text field. | DelayMsSuffixTransformation | 310 |
| `PlayerGlassModalSheet.kt` | Contains PlayerGlassModalSheet, PlayerGlassModalSheetContent | PlayerGlassModalSheet | 622 |
| `PlayerMoreMenuPanel.kt` | 4. 3-DOT OVERFLOW MENU PANEL | MoreMenuSelectionPanel | 4488 |
| `PlayerPanelGlassTokens.kt` | Global persistent store for panel custom heights. | PlayerPanelHeightStore | 331 |
| `SleepTimerPanel.kt` | 2B-2. SLEEP TIMER PANEL | SleepTimerPanel | 459 |

### `player/screen/`  — packages: `com.example.ui.screens`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `ContainerTrackFallbacks.kt` | Contains resolveContainerAudioFallback, resolveContainerSubtitleFallback | resolveContainerAudioFallback, resolveContainerSubtitleFallback | 253 |
| `VideoPlayerScreenHost.kt` | Ultra-Fast, Zero-Lag Video Player Screen with Native MPV & ASS Subtitle Engine | VideoPlayerScreen | 5345 |

### `player/smart/`  — packages: `com.example.player`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `SmartAutoPlaybackAdvisor.kt` | Smart Auto Playback: detects what a video needs (codec, resolution, FPS, bit depth, HDR, bitrate) and what the | SmartTier, SmartDeviceProfile, SmartStreamInfo | 326 |

### `providers/`  — packages: `com.example.util.media`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `ProviderApiRepository.kt` | Loads a plugin's categories and thumbnails through [PluginJsEngine] and keeps them in memory for the session. | MediaProviderRepository | 462 |

### `providers/browser/`  — packages: `com.example.util.media`, `com.example.ui.components`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `HiddenWebViewChallengeSolver.kt` | `providerContext.openWebView`: a WebView opens the page (hidden first, shown in an in-app dialog when the user has to tap / sign in) (Cloudflare / bro | PluginWebViewSolver | 259 |
| `InAppBrowserRequestState.kt` | The in-app browser of `providerContext.openWebView` (like Vega's WafWebViewDialog): when a site needs a tap / captcha | PluginBrowserState | 44 |
| `PluginBrowserHostSheet.kt` | Shows the in-app browser of a provider (Cloudflare "Verify you are human", sign in, link account ...) over the whole app. | PluginBrowserHost | 87 |

### `providers/detail/`  — packages: `com.example.ui.screens`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `MediaDetailGrouping.kt` | One movie / episode card of the detail page. | QualityOption, AudioGroup, SeasonGroup, DetailVideo | 217 |
| `MediaDetailPage.kt` | Contains MediaDetailScreen | MediaDetailScreen | 873 |
| `MediaDetailPickers.kt` | Audio / quality / season box: a small caption, the current value and an arrow; tapping opens the bottom sheet. | DetailPill, PickerPill, OptionSheet | 199 |
| `MediaDetailState.kt` | What the Media section currently has open. It lives outside composition on purpose: after the player is closed the | MediaDetailState | 127 |

### `providers/download/`  — packages: `com.example.ui.components`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `ProviderDownloadSheet.kt` | Download panel of a movie / episode inside a provider: it first looks up which servers / storage links the provider | MediaDownloadSheet | 342 |
| `ProviderServerQualityProbe.kt` | One server / storage link of a provider title after it was really tried. | ServerCandidate, ServersState | 269 |

### `providers/hls/`  — packages: `com.example.util.media`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `HlsMasterPlaylistParser.kt` | Reads the renditions (the quality list) out of an HLS master playlist. | HlsVariants | 242 |
| `HlsPlaylistModels.kt` | One video rendition (EXT-X-STREAM-INF) of an HLS master playlist. | HlsVariant, HlsRendition, HlsMediaPlaylist | 59 |

### `providers/manga/`  — packages: `com.example.util.media`, `com.example.ui.components`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `MangaChapterDetector.kt` | Manga / comic providers: their "streams" are page images instead of a video. When a chapter's links are images, the app | MangaDetect | 38 |
| `MangaChapterReader.kt` | Pages that were read stay in memory (a few of them), so scrolling back up never downloads them again. | MangaReaderScreen | 240 |

### `providers/model/`  — packages: `com.example.util.media`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `PluginCatalogModels.kt` | One provider plugin listed in a repository: a Vega manifest.json entry, or a CloudStream plugins.json entry. | MediaPluginInfo, MediaCatalogEntry, MediaPost, PluginException, MediaDirectLink, MediaLink … | 101 |

### `providers/playback/`  — packages: `com.example.util.media`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `ParallelServerRaceProbe.kt` | Contains StreamProbe | StreamProbe | 323 |
| `PluginEpisodeQueue.kt` | Preloads and caches the next episode links and validated VideoItem for 15 minutes. | PluginQueuedEpisode, PluginQueuedEpisodes | 157 |
| `PluginPlaybackSessions.kt` | One quality of the audio (language) a title was opened with. | PluginQualityChoice, PluginPlaybackSession, PluginPlaybackSessions | 52 |
| `PluginStreamResolver.kt` | Turns a plugin's stream into something the app's player, playlists, and downloader understand. | MediaPlayback | 235 |
| `ProviderSubtitleFetcher.kt` | Contains SubtitleDownloader | SubtitleDownloader | 84 |
| `ServerReliabilityStore.kt` | Tracks server reliability, response latency, and recent failures per provider. | ServerReputationStore | 151 |

### `providers/runtime/`  — packages: `com.example.util.media`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `ProviderCookieJar.kt` | Cookie jar of the provider (plugin) requests, matched by domain only (like Vega's providerCookieJar). | PluginCookieJar | 174 |
| `ProviderJsRuntimeHost.kt` | Runs Vega provider plugins (compiled JavaScript) inside a hidden WebView. The page gets `fetch`, `axios`, `cheerio` | PluginJsEngine | 493 |
| `ProviderNativeCrypto.kt` | Native crypto for provider_js_runtime.js (`providerContext.Aes`, `Crypto`, and the `crypto-js` shim). Requests and answers are | PluginCrypto | 80 |

### `providers/settings/`  — packages: `com.example.util.media`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `ProviderStreamPreferences.kt` | Settings > Network > Media settings, applied to the provider streams: the server / stream of the chosen language and | MediaPreferences | 66 |

### `providers/store/`  — packages: `com.example.util.media`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `PluginInstallStore.kt` | Providers of the Network > Media section. A repository link (Vega manifest.json, or CloudStream repo.json / plugins.json | MediaPluginStore | 518 |
| `WatchLaterStore.kt` | A movie / episode the user saved with "Watch later" on a media plugin. [link] is the plugin's own link for getStream. | MediaSavedVideo, MediaLibraryStore | 95 |
| `WatchedTitleHistoryStore.kt` | A title (movie / series) the user last played from a media plugin. [link] is the plugin's own link for getMeta. | MediaWatchedTitle, MediaHistoryStore | 82 |

### `providers/thumbnails/`  — packages: `com.example.util.media`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `EpisodeFrameGrabber.kt` | The picture of an episode card is a frame of the episode's own video, and getting it doubles as a real playability | EpisodeFrames | 337 |

### `providers/ui/`  — packages: `com.example.ui.screens`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `PluginRepositoryManager.kt` | Last known plugin lists, so the Media page can draw immediately while the lists are re-read in the background. | MediaPluginManager | 567 |
| `ProviderHomePage.kt` | One downloaded plugin (provider) page: sliding trending banner, "Continue watching", then the provider's categories. | MediaPluginPage | 769 |
| `ProviderPostCards.kt` | Plain poster card of the grids ("See all" / search results). | MediaGridMemory | 465 |

### `settings/`  — packages: `com.example.util`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `SettingsXmlBackupManager.kt` | Robust, production-grade XML serialization and deserialization manager | ViauraSettingsXmlManager | 890 |

### `settings/about/`  — packages: `com.example.ui.components`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `AboutDashboardPanel.kt` | Dedicated About Settings Screen Composable. | AboutSettingsPanel | 702 |
| `DeviceHardwareProbe.kt` | Reads the device's real display / audio / hardware info; missing features show "None". | DetectedDisplayInfo, DetectedAudioInfo, DetectedHardwareInfo | 541 |

### `settings/advanced/`  — packages: `com.example.ui.components`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `AdvancedSettingsTabPanel.kt` | Advanced Settings Panel | AdvancedSettingsPanel | 525 |

### `settings/ai/`  — packages: `com.example.ui.components`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `AiFeaturesSettingsTab.kt` | Settings > Advanced > AI Features. | AiFeaturesSettingsPanel | 1001 |
| `AiSettingsRowWidgets.kt` | ---------------- Shared small building blocks ---------------- | SectionHeader, GlassCard, SettingsDivider, ToggleRow | 343 |

### `settings/appearance/`  — packages: `com.example.ui.components`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `ThemeGradientEditorPanel.kt` | The editor always works with 3 colours: first, a muted & desaturated second (the middle of the gradient) and third. | themeEditorThreeColors, ThemeGradientEditorPanel | 419 |
| `ThemePreviewControlsCard.kt` | Calls [onDoubleTap] after two quick taps anywhere inside this element. It only WATCHES the touches (initial pass, | ThemePreviewControls | 300 |

### `settings/dashboard/`  — packages: `com.example.ui.components`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `SettingsDashboardSheet.kt` | Splits a very large settings page into separate compiled lambdas so a single | SettingsCategoryTab, SettingsSheet | 3821 |

### `settings/folders/`  — packages: `com.example.ui.components`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `FolderSettingsRowCards.kt` | Contains FolderMetadataCard, BlockedFolderRowItem, PrivateFolderItemCard | FolderMetadataCard, BlockedFolderRowItem, PrivateFolderItemCard | 367 |
| `FoldersSettingsTab.kt` | Contains FolderSettingsSelectionMode, FoldersSettingsTabContent | FolderSettingsSelectionMode, FoldersSettingsTabContent | 1635 |

### `settings/i18n/`  — packages: `com.example.ui.i18n`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `TranslatedTextComposable.kt` | Drop-in replacement for material3 Text used by the Settings screens (their `import androidx.compose.material3.Text` is | Text, Text | 114 |
| `UiTextTranslator.kt` | Language the UI is currently shown in. Provided once at the top of the app (VideoPlayerTheme). | I18n | 112 |

### `settings/state/`  — packages: `com.example.ui.state`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `AppSettingsViewModel.kt` | Contains AppViewModel | AppViewModel | 502 |
| `AppUiState.kt` | Contains UiState, AppConfig | UiState, AppConfig | 113 |
| `LocalizedAppStrings.kt` | Contains AppStrings | AppStrings | 182 |
| `PlayerSettingsModel.kt` | Centralized player settings. Every value is persisted and consumed by the video player. | PlayerSettings | 557 |
| `SettingsPreferencesStore.kt` | Contains SettingsPreferencesManager | SettingsPreferencesManager | 780 |
| `ThemeAndDisplayOptionEnums.kt` | Custom (user edited) gradients per theme id, stored as "id=hex,hex,hex;id2=hex,hex,hex". | ThemeMode, AppThemeVariant, ThumbnailStrategy, ThumbnailQuality, ControlsAnimationStyle, VideoOpeningAnimation … | 208 |

### `settings/subtitles/`  — packages: `com.example.ui.components`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `SubtitleSettingsTab.kt` | Contains SubtitlesTab | SubtitlesTab | 4050 |

### `subtitles/ass/`  — packages: `com.example.player`, `com.example.ui.components`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `AssOverrideStores.kt` | Persistence helper: all per-subtitle overrides are stored as ONE json string inside | AssOverridesStore, AssRawTextStore | 136 |
| `AssStyleEditorContent.kt` | Advanced ASS/SSA editor: one expandable card per Style found in the CURRENT ASS/SSA subtitle | LoadedAssDocument, AdvancedAssStyleEditorContent, AdvancedAssInfoCard, AssStyleCard | 589 |
| `AssStyleFieldWidgets.kt` | SMALL BUILDING BLOCKS | AssSectionTitle, AssFieldRow, AssFieldLabel, AssTextField | 436 |
| `AssStyleModels.kt` | Advanced ASS/SSA per-style editing support. | AssFields, AssStyleInfo, AssStylesDocument, AssSubtitleOverrides, AdvancedAssSource, RawSubtitleSource | 303 |
| `AssStyleRewriteEngine.kt` | Contains AdvancedAssStyleEngine | AdvancedAssStyleEngine | 274 |

### `subtitles/engine/`  — packages: `com.example.ui.screens`, `com.example.player`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `AssSubtitleParser.kt` | ASS SUBTITLE PARSER & EXPORTER | AssParser | 420 |
| `SubtitleConversionEngine.kt` | GENERAL SUBTITLE ENGINE | SubtitleEngine | 671 |
| `SubtitleCueModels.kt` | SUBTITLE DATA MODELS | SubtitleCue, SubtitleFormat | 69 |
| `SubtitleFormatToAssConverter.kt` | Universal Subtitle Engine | UniversalSubtitleEngine | 925 |

### `subtitles/fonts/`  — packages: `com.example.player`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `GoogleFontsDownloader.kt` | Automatic Google Fonts support for ASS/SSA subtitles. | GoogleFontsManager | 469 |
| `SubtitleFontResolver.kt` | Contains SubtitleFontManager | SubtitleFontManager | 1643 |

### `subtitles/panels/`  — packages: `com.example.ui.components`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `SubtitleDelayPanel.kt` | 2C. DEDICATED SUBTITLE DELAY PANEL | SubtitleDelayPanel | 572 |
| `SubtitleSelectionPanel.kt` | Helper to clean video title into searchable keyword query | SubtitleSelectionPanel | 1413 |

### `subtitles/store/`  — packages: `com.example.util`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `ExternalSubtitleSelectionStore.kt` | CustomSubtitlePersistenceManager | CustomSubtitlePersistenceManager | 601 |
| `SubtitleSelectionRecords.kt` | Data class representing a permanently attached user-uploaded subtitle. | AttachedSubtitleRecord, SavedSubtitleRef, VideoSubtitleSelection | 46 |
| `SubtitleSessionRecall.kt` | SubtitleSessionMemory | SubtitleSessionMemory | 106 |

### `subtitles/style/`  — packages: `com.example.ui.components`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `SubtitleColorSection.kt` | 2. ENHANCED COLORS SECTION WITH 3-ARC CIRCULAR COLOR PICKER (Image 1 style) | ColorTarget | 630 |
| `SubtitleMiscAndAdvancedSections.kt` | 3. ENHANCED MISCELLANEOUS SECTION (Full, Expandable, Detailed Layout) | EnhancedMiscContent, EnhancedAdvancedContent, DetailedSwitchRow, SUBTITLE_BOTTOM_POSITION | 395 |
| `SubtitleStylePanelRoot.kt` | Enhanced, Highly-Polished Subtitle Style Editor Panel | SubtitleStyleAccordionSection, SubtitleStylePanel | 399 |
| `SubtitleTrackKindChecks.kt` | Whether [track] is a genuinely native ASS/SSA subtitle, as opposed to another text format | isNativeAssSsaTrack, isImageBasedSubtitleTrack | 134 |
| `SubtitleTypographySection.kt` | 1. ENHANCED TYPOGRAPHY SECTION (Image 2 style) | TypographySubTab | 746 |

### `subtitles/ui/`  — packages: `com.example.ui.components`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `RawSubtitleTextEditor.kt` | Draft of the raw subtitle editor. It lives in the style panel (not in the editor composable) so | RawEditorDraft | 326 |
| `SubtitleFilePickerDialog.kt` | Contains STRICT_SUBTITLE_EXTENSIONS, RESTRICTED_FOLDER_NAMES, isRestrictedPath, SubtitlePickerMemory, SubtitleFileItem … | SubtitleFileItem, SubtitleFileTreeDialog | 746 |

### `theme/`  — packages: `com.example.ui.theme`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `AccentGradientColors.kt` | Accent Gradient Colors (Sky Blue to Magenta/Pink default) | accentSkyBlueState, accentPinkState, accentStrongState, accentMidState | 63 |
| `MutedSwatchPalette.kt` | One swatch of the muted & desaturated colour palette. | MutedSwatch, MutedPalette | 55 |
| `RotatedWindowConfiguration.kt` | The window's real (rotated) configuration. | LocalRealConfiguration | 14 |
| `SurfaceAndTextColors.kt` | Wallpaper based Dynamic Color (Android 12+), active only while Settings > Interface Appearance = System. | SystemDynamic, Neutral | 117 |
| `SystemSurfaceTonePalette.kt` | Builds the "System" surface palette (backmost background, folder / video cards, every sheet, borders, text) from ONE hue. | SystemTonePalette | 88 |
| `ViauraAppTheme.kt` | Blends [base] towards [tone] by [amount] (0..1); same maths the built-in themes use for their derived tones. | VideoPlayerTheme | 279 |
| `ViauraTypography.kt` | Custom font family loaded from res/font/viaura.otf (Viaura.otf). | AppCustomFontFamily, getAppFontFamily, createViauraTypography, Typography | 143 |

### `theme/picker/`  — packages: `com.example.ui.components`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `MutedCircleColorPicker.kt` | Muted & desaturated colour space of the round picker (HSL): saturation and lightness stay in a soft range. | MutedCircularColorPicker, MutedCircularColorPickerCard | 262 |
| `SystemTonePickerPanel.kt` | Tone (hue / saturation / tint depth) that the System surfaces take from one palette swatch. | SystemTonePanel | 112 |

### `torrent/`  — packages: `com.example.torrent`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `TorrentCatalogModels.kt` | Metadata-only result used by the torrent file picker (download only). | TorrentFileItem, TorrentCatalog, ParsedMagnet, TorrentException | 61 |
| `TorrentFileDownloader.kt` | Downloads ONE selected file of a torrent (all other files are ignored) into [saveDir]. Pause = the loop is stopped | TorrentDownloadEngine | 272 |
| `TorrentFileListReader.kt` | Process wide holder of the metadata engine (Viaura has no DI container). | TorrentCatalogs, TorrentCatalogEngine | 540 |
| `TorrentLoopbackStreamServer.kt` | Plays a torrent (magnet / .torrent of a provider such as TorraStream) like Vega does: the torrent is added to a libtorrent | TorrentStreamer | 338 |
| `TorrentPickerRequests.kt` | The torrent file picker (download only) is opened from anywhere (Network tab, magnet / .torrent intent) through this state. | TorrentPickerRequest, TorrentPickerState | 56 |
| `TorrentSourceParsing.kt` | Schemes whose payload is a file already present on the device. A local `content://` or `file://` | v1HexHash, v2HexHash, v1Base32Hash, torrentMimeTypes | 334 |
| `TorrentTrackerEndpointReader.kt` | Reads only connection endpoints from bounded .torrent metadata without retaining its tree. | TorrentMetadataEndpoints | 155 |

### `torrent/ui/`  — packages: `com.example.ui.components`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `TorrentFilePickerSheet.kt` | Global torrent picker, hosted once in MainActivity. Opened through [TorrentPickerState] (Network tab, magnet / .torrent | TorrentPickerHost | 249 |

### `widgets/`  **[in zip `app/src/widgets-src.zip`]**  — packages: `com.example.ui.components`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `BottomNavigationTabs.kt` | Dynamic Floating Pill-Shaped Bottom Navigation Bar. | NavigationTab, DynamicBottomNavigation | 514 |
| `DeleteConfirmationSheet.kt` | Delete confirmation used everywhere: it slides up from the bottom of the screen (never centered) with a | DeleteConfirmSheet | 215 |
| `DeleteItemAnimation.kt` | Which list items (folder ids / video paths) are currently being deleted. | DeleteAnimState, DeleteAnimatedBox | 91 |
| `FullWidthHdImage.kt` | Sharp full-width image (detail page backdrop, hero slider). [LinkThumbnail] keeps small 480 px list thumbnails that | HighResImageLoader, HighResImage | 164 |
| `GradientPlayLogoIcon.kt` | Exact Vector representation of user SVG icon. | PlayGradientLogoIcon | 76 |
| `LinkThumbnailImage.kt` | Small thumbnail for a link / download row. Order: the thumbnail attached to the link (image URL, or a frame saved | LinkThumbnail | 72 |
| `ListGlideScroll.kt` | Smooth "finger-like" glide to an item of a lazy list / grid. | GLIDE_LEAD_ITEMS, GLIDE_DURATION_MS, glideToItem, glideToItem | 66 |
| `LongTextInputField.kt` | Single-line text field for long text (file names, API keys, URLs). | FollowCursorTextField, FollowCursorTextField | 189 |
| `MarqueeTitleText.kt` | High-performance smooth marquee for video titles in player top bar. | AutoScrollingText | 88 |
| `PlayedProgressWash.kt` | Contains playedProgressFill | playedProgressFill | 35 |
| `PressBounceModifiers.kt` | Minimum time between two accepted clicks on the same debounced control. | CLICK_DEBOUNCE_MS, rememberDebouncedClick, bounceClick, cardBounceClick | 179 |
| `ShineAppLogoIcon.kt` | The app logo with the same sliding accent-gradient + moving shine as the Settings icon. | ShineLogoIcon | 92 |
| `SkipSeekAnimatedIcon.kt` | Animated "skip back / skip forward" icon: the ring spins one full turn with a bouncy spring | SeekIcon | 158 |
| `TabSwipeNavigation.kt` | Finger-follow horizontal swipe between the top level tabs (same idea as the Settings dashboard pager). | swipeBetweenTabs | 110 |
| `TopHeaderBar.kt` | Contains HeaderBar | HeaderBar | 297 |
| `ViauraStyledIcon.kt` | Contains StyledIcon, StyledIcon | StyledIcon, StyledIcon | 183 |

### `ytdlp/`  — packages: `com.example.ytdlp`

| File | What it does | Main symbols | Lines |
|---|---|---|---|
| `YtdlpHttpsFetcher.kt` | Contains YtdlpFetch | YtdlpFetch | 152 |
| `YtdlpInstallInfoModels.kt` | Contains YtdlpReleaseChannel, YtdlpInstallationInfo | YtdlpReleaseChannel, YtdlpInstallationInfo | 56 |
| `YtdlpLinkRouting.kt` | URL classification used to decide which links are routed through yt-dlp. | YtdlpUrlRules | 54 |
| `YtdlpPersistentWorker.kt` | The worker itself could not answer (not started, timed out, rejected an option): the caller runs yt-dlp normally. | YtdlpWorkerUnavailable, YtdlpWorker | 231 |
| `YtdlpPrefsStore.kt` | One stored value; `get()`/`set()` keep the ported yt-dlp code identical to mpvRx. | YtdlPref, YtdlPreferences | 71 |
| `YtdlpRuntimeInstaller.kt` | Contains YtdlpManager | YtdlpManager | 852 |

## 5. Non-Kotlin files

| Path | What it is |
|---|---|
| `app/build.gradle.kts`, `build.gradle.kts`, `settings.gradle.kts`, `gradle/libs.versions.toml`, `gradle.properties`, `gradlew*`, `gradle/wrapper/*` | Gradle setup (`settings.gradle.kts` also unpacks the packed zips for local builds) |
| `app/proguard-rules.pro` | R8 keep rules (cloudstream-lib, JS bridge, libtorrent) |
| `app/src/main/AndroidManifest.xml` | permissions, activities, services, FileProvider |
| `app/src/main/assets/lang/lang_english.json`, `lang_hinglish.json`, `lang_hindi_devanagari.json` | UI translations (rule R8) |
| `app/src/main/assets/media/provider_js_runtime.js` | JS runtime for Vega providers (providerContext, axios/cheerio shims, crypto-js shim, WebView bridge) |
| `app/src/main/assets/media/provider_js_extractors.js` | JS extractors (hubcloud, gofile, gdflix, supervideo …) |
| `app/src/main/cpp/CMakeLists.txt`, `app/src/main/cpp/ytdl_exec_bridge.c` | native `libytdl.so` launcher for yt-dlp (QuickJS sources downloaded by CI) |
| `app/src/ytdlp/assets/ytdl/ytdl_env_setup.py`, `ytdlp_worker_loop.py`, `ytdl_launch_script` | yt-dlp Python setup, persistent worker, shell launcher (copied by `ytdlp/YtdlpRuntimeInstaller.kt`) |
| `app/src/main/res/font/viaura.otf` | app display font |
| `app/src/main/res/drawable/ic_abouticon.xml`, `ic_launcher_*.xml`, `launch_window_background.xml`, `res/mipmap-anydpi/ic_launcher.xml` (the one default launcher icon; the App Icon changer and its presets were removed), `res/values*`, `res/xml/*` | launcher icon layers, themes, backup rules, FileProvider paths |
| `app/src/main/branding/ic_homeicon.svg`, `ic_homeicon_full_reference.xml` | launcher icon source art (not packaged) |
| `tools/plugin-test/provider_runtime_harness.js`, `provider_runtime_selftest.js`, `provider_report_compare.js` | Node/Chromium harness that runs `provider_js_runtime.js` with mock network |
| `.github/signing/debug.keystore.b64` | fixed signing key (text) |
| `.github/legacy-paths.txt` | old file paths removed by CI (rule R11) |
| `README.md`, `metadata.json` | AI Studio bookkeeping |
| `assets/screenshots/**` | README screenshots |

Planned for CI download only (never committed): `app/src/main/assets/fonts/GoNotoCurrent-Regular.ttf`, `app/src/ytdlp/assets/ytdl/python313.zip`, `app/src/ytdlp/assets/ytdl/yt-dlp`, `app/src/ytdlp/assets/cacert.pem`, `app/src/ytdlp/jniLibs/<abi>/libpython.so`, `app/src/main/cpp/third_party/quickjs/`.


## 6. Icon catalog

- **Zip:** `app/src/viaura-icons.zip` — entries `drawable/viaura_*.xml` (149 vector icons), relative to `app/src/main/res`. CI unpacks them into `app/src/main/res/drawable/` (git-ignored). Edit: unzip, change, then `cd app/src/main/res && zip -X ../../viaura-icons.zip drawable/viaura_*.xml`.
- Icons are referenced statically as `R.drawable.<name>`; there is no name-based dynamic lookup. Every icon below has at least one user.
- Loose (not zipped) drawables: `ic_abouticon` (used by `widgets/TopHeaderBar.kt`, `widgets/ShineAppLogoIcon.kt`, `settings/about/AboutDashboardPanel.kt`, `app/StartupSplashScreen.kt`), `ic_launcher_background`, `ic_launcher_foreground`, `ic_launcher_monochrome` (adaptive launcher icon, `mipmap-anydpi/ic_launcher.xml`), `launch_window_background`.
- **CloudStream / provider icons:** there are NO icon files bundled for CloudStream or providers (the zip `cloudstream-lib.zip` contains only Kotlin sources, `build.gradle.kts` and a manifest). A provider/plugin icon is a remote URL (`iconUrl` of the repository manifest) read in `providers/store/PluginInstallStore.kt` and shown through `widgets/TopHeaderBar.kt` (`leadingIconUrl`) and the plugin rows in `providers/ui/PluginRepositoryManager.kt`. Provider/CloudStream placeholders use the normal `viaura_*` icons listed below.

| Icon (`R.drawable.…`) | Used in |
|---|---|
| `` | `AiProviderCatalog.kt`, `AiProviderHttpGateway.kt`, `AiSettingsPrefsStore.kt`, `AiSubtitleTranslator.kt`, `ApiKeyKeystoreVault.kt` (+361 more) |
| `viaura_ab_loop` | `AudioPlayerFullScreen.kt`, `PlayerControlLayoutModels.kt`, `VideoPlayerControlsOverlay.kt`, `ViauraStyledIcon.kt` |
| `viaura_add_circle` | `ViauraStyledIcon.kt` |
| `viaura_add_folder` | `ViauraStyledIcon.kt` |
| `viaura_aitranslate` | `PlayerMoreMenuPanel.kt`, `AiFeaturesSettingsTab.kt`, `SubtitleSelectionPanel.kt` |
| `viaura_align_horizontal_center` | `ViauraStyledIcon.kt` |
| `viaura_align_left` | `SubtitleTypographySection.kt` |
| `viaura_align_right` | `SubtitleTypographySection.kt` |
| `viaura_ambience` | `PlayerControlLayoutModels.kt`, `VideoPlayerControlsOverlay.kt`, `ViauraStyledIcon.kt` |
| `viaura_arrow_left_solid` | `AudioNotificationBuilder.kt`, `AudioPlayerFullScreen.kt`, `VideoPlayerControlsOverlay.kt`, `MediaDetailPage.kt`, `ProviderPostCards.kt` (+1 more) |
| `viaura_arrow_right_solid` | `AudioNotificationBuilder.kt`, `AudioPlayerFullScreen.kt`, `MiniAudioPlayerBar.kt`, `ChapterSkipButton.kt`, `MiniVideoPlayerPopup.kt` (+4 more) |
| `viaura_audio_circle` | `SettingsDashboardSheet.kt`, `ViauraStyledIcon.kt` |
| `viaura_audio_track` | `AudioPlayerFullScreen.kt`, `DownloadsTab.kt`, `OnlineDownloadOptionsSheet.kt`, `SharedPlaylistHeaderCard.kt`, `SharedPlaylistPage.kt` (+1 more) |
| `viaura_battery` | `ViauraStyledIcon.kt` |
| `viaura_bug` | `AboutDashboardPanel.kt` |
| `viaura_block` | `OnlineCardSwipeToolbar.kt`, `ViauraStyledIcon.kt` |
| `viaura_bookmark` | `PlayerControlLayoutModels.kt`, `VideoPlayerScreenHost.kt`, `ViauraStyledIcon.kt` |
| `viaura_calendar` | `SortAndViewOptionsPopup.kt` |
| `viaura_capture` | `VideoPlayerControlsOverlay.kt` |
| `viaura_check_circle` | `DownloadSheetWidgets.kt`, `ChannelDetailPage.kt`, `ViauraStyledIcon.kt` |
| `viaura_checklist` | `ViauraStyledIcon.kt` |
| `viaura_clock_circle` | `SortAndViewOptionsPopup.kt`, `ViauraStyledIcon.kt` |
| `viaura_close_circle` | `AudioNotificationBuilder.kt`, `DownloadForegroundNotificationService.kt`, `MiniVideoPlayerPopup.kt`, `VideoMiniForegroundService.kt`, `PluginRepositoryManager.kt` (+1 more) |
| `viaura_cloud` | `PluginRepositoryManager.kt`, `ViauraStyledIcon.kt` |
| `viaura_copy` | `VideoPlayerScreenHost.kt`, `ViauraStyledIcon.kt` |
| `viaura_count` | `SortAndViewOptionsPopup.kt` |
| `viaura_cpu` | `PlayerControlLayoutModels.kt`, `VideoPlayerControlsOverlay.kt`, `SettingsDashboardSheet.kt`, `ViauraStyledIcon.kt` |
| `viaura_device` | `SettingsDashboardSheet.kt` |
| `viaura_dislike` | `VideoPlayerScreenHost.kt` |
| `viaura_document` | `ViauraStyledIcon.kt` |
| `viaura_document_text` | `ViauraStyledIcon.kt` |
| `viaura_download_minimalistic` | `DownloadForegroundNotificationService.kt`, `DownloadSheetWidgets.kt`, `DownloadsTab.kt`, `NetworkTab.kt`, `RecentStreamRows.kt` (+5 more) |
| `viaura_download_square` | `ViauraStyledIcon.kt` |
| `viaura_downloads` | `BottomNavigationTabs.kt`, `TopHeaderBar.kt` |
| `viaura_equalizer` | `AudioTrackSelectionPanel.kt`, `AudioPlayerFullScreen.kt`, `PlayerMoreMenuPanel.kt`, `ViauraStyledIcon.kt` |
| `viaura_eraser` | `ViauraStyledIcon.kt` |
| `viaura_eye` | `ViauraStyledIcon.kt` |
| `viaura_eye_closed` | `ViauraStyledIcon.kt` |
| `viaura_favourite_heart` | `AudioPlayerFullScreen.kt`, `PlaylistRevealSwipeRow.kt`, `OnlineCardSwipeToolbar.kt` |
| `viaura_flip_vertical` | `PlayerControlLayoutModels.kt`, `VideoPlayerControlsOverlay.kt` |
| `viaura_folder` | `SettingsDashboardSheet.kt`, `ViauraStyledIcon.kt` |
| `viaura_folder_error` | `ViauraStyledIcon.kt` |
| `viaura_folder_favourite_star` | `ViauraStyledIcon.kt` |
| `viaura_folder_heart` | `FolderGlyphIcon.kt` |
| `viaura_folder_open` | `SubtitleSettingsTab.kt`, `ViauraStyledIcon.kt` |
| `viaura_folder_play` | `FolderGlyphIcon.kt` |
| `viaura_folder_playlist_audio` | `FolderGlyphIcon.kt` |
| `viaura_folder_playlist_video` | `FolderGlyphIcon.kt` |
| `viaura_folder_song` | `FolderGlyphIcon.kt` |
| `viaura_folder_with_files` | `StoragePermissionHelper.kt`, `ViauraStyledIcon.kt` |
| `viaura_full_screen_square` | `PlayerModeEnums.kt`, `VideoPlayerControlsOverlay.kt` |
| `viaura_gesture` | `SettingsDashboardSheet.kt` |
| `viaura_github` | `AboutDashboardPanel.kt` |
| `viaura_headphones_round` | `PlayerControlLayoutModels.kt`, `VideoPlayerControlsOverlay.kt`, `ViauraStyledIcon.kt` |
| `viaura_heart` | `ViauraStyledIcon.kt` |
| `viaura_high_quality` | `ViauraStyledIcon.kt` |
| `viaura_info_circle` | `ViauraStyledIcon.kt` |
| `viaura_last_in_queue` | `OnlineCardSwipeToolbar.kt` |
| `viaura_leave` | `AudioPlayerFullScreen.kt`, `VideoPlayerControlsOverlay.kt`, `ViauraStyledIcon.kt` |
| `viaura_library` | `StoragePermissionHelper.kt`, `PlayerControlLayoutModels.kt`, `VideoPlayerControlsOverlay.kt` |
| `viaura_like` | `VideoPlayerScreenHost.kt` |
| `viaura_link` | `DownloadSheetWidgets.kt`, `NetworkTab.kt`, `RecentStreamRows.kt` |
| `viaura_liquid_glass` | `SettingsDashboardSheet.kt` |
| `viaura_list` | `ProviderHomePage.kt`, `ViauraStyledIcon.kt` |
| `viaura_list_arrow_down` | `SubtitleSelectionPanel.kt`, `ViauraStyledIcon.kt` |
| `viaura_list_arrow_up` | `ViauraStyledIcon.kt` |
| `viaura_lock` | `ViauraStyledIcon.kt` |
| `viaura_lock_unlocked` | `PlayerControlLayoutModels.kt`, `VideoPlayerControlsOverlay.kt`, `ViauraStyledIcon.kt` |
| `viaura_lyrice` | `AudioPlayerFullScreen.kt` |
| `viaura_magnifer` | `ViauraStyledIcon.kt` |
| `viaura_magnifer_zoom_in` | `PlayerControlLayoutModels.kt`, `VideoPlayerControlsOverlay.kt`, `ViauraStyledIcon.kt` |
| `viaura_maximize_square` | `PlayerModeEnums.kt`, `ViauraStyledIcon.kt` |
| `viaura_maximize_square_2` | `PlayerModeEnums.kt` |
| `viaura_menu_dots` | `SharedPlaylistHeaderCard.kt`, `PlayerControlLayoutModels.kt`, `VideoPlayerControlsOverlay.kt`, `ViauraStyledIcon.kt` |
| `viaura_minimize_square_2` | `PlayerControlLayoutModels.kt`, `PlayerModeEnums.kt`, `ViauraStyledIcon.kt` |
| `viaura_minus_circle` | `ViauraStyledIcon.kt` |
| `viaura_mirror_left` | `PlayerControlLayoutModels.kt`, `VideoPlayerControlsOverlay.kt` |
| `viaura_mirror_right` | `PlayerControlLayoutModels.kt`, `VideoPlayerControlsOverlay.kt` |
| `viaura_moon` | `SettingsDashboardSheet.kt`, `ViauraStyledIcon.kt` |
| `viaura_move_to_folder` | `ViauraStyledIcon.kt` |
| `viaura_music_library` | `BottomNavigationTabs.kt` |
| `viaura_network` | `SettingsDashboardSheet.kt`, `BottomNavigationTabs.kt` |
| `viaura_next_in_queue` | `OnlineCardSwipeToolbar.kt` |
| `viaura_notification_unread` | `ViauraStyledIcon.kt` |
| `viaura_pallete_2` | `SettingsDashboardSheet.kt`, `ViauraStyledIcon.kt` |
| `viaura_paste` | `NetworkTab.kt`, `PluginRepositoryManager.kt` |
| `viaura_pasted` | `NetworkTab.kt`, `PluginRepositoryManager.kt` |
| `viaura_pause_solid` | `AppEntryActivity.kt`, `AudioNotificationBuilder.kt`, `AudioPlayerFullScreen.kt`, `MiniAudioPlayerBar.kt`, `DownloadForegroundNotificationService.kt` (+4 more) |
| `viaura_pen` | `ViauraStyledIcon.kt` |
| `viaura_pen_2` | `HomeLibraryScreen.kt`, `ViauraStyledIcon.kt` |
| `viaura_pip` | `PlayerControlLayoutModels.kt`, `VideoPlayerControlsOverlay.kt`, `ViauraStyledIcon.kt` |
| `viaura_play_circle` | `SettingsDashboardSheet.kt`, `ViauraStyledIcon.kt` |
| `viaura_play_solid` | `AppEntryActivity.kt`, `AudioNotificationBuilder.kt`, `AudioPlayerFullScreen.kt`, `MiniAudioPlayerBar.kt`, `SharedPlaylistHeaderCard.kt` (+7 more) |
| `viaura_playlist_audio` | `AudioMusicTab.kt`, `AudioPlayerFullScreen.kt` |
| `viaura_playlist_video` | `HomeLibraryScreen.kt`, `OnlineCardSwipeToolbar.kt`, `VideoPlayerScreenHost.kt`, `BottomNavigationTabs.kt`, `ViauraStyledIcon.kt` |
| `viaura_recent` | `ProviderHomePage.kt`, `BottomNavigationTabs.kt`, `ViauraStyledIcon.kt` |
| `viaura_refresh` | `ViauraStyledIcon.kt` |
| `viaura_reorder` | `ViauraStyledIcon.kt` |
| `viaura_repeat` | `PlayerControlLayoutModels.kt`, `VideoPlayerControlsOverlay.kt`, `ViauraStyledIcon.kt` |
| `viaura_repeat_one` | `VideoPlayerControlsOverlay.kt`, `ViauraStyledIcon.kt` |
| `viaura_restart` | `ViauraStyledIcon.kt` |
| `viaura_rewind_back` | `ViauraStyledIcon.kt` |
| `viaura_rewind_forward` | `ViauraStyledIcon.kt` |
| `viaura_saved_bookmark` | `PlaylistRevealSwipeRow.kt`, `VideoPlayerScreenHost.kt` |
| `viaura_screencast` | `PlayerControlLayoutModels.kt`, `VideoPlayerControlsOverlay.kt`, `ViauraStyledIcon.kt` |
| `viaura_screenshot` | `PlayerControlLayoutModels.kt`, `ViauraStyledIcon.kt` |
| `viaura_settings` | `SubtitleSettingsTab.kt`, `ViauraStyledIcon.kt` |
| `viaura_settings_minimalistic` | `StoragePermissionHelper.kt`, `SettingsDashboardSheet.kt`, `ViauraStyledIcon.kt` |
| `viaura_share` | `OnlineCardSwipeToolbar.kt`, `VideoPlayerScreenHost.kt`, `ViauraStyledIcon.kt` |
| `viaura_shield_check` | `StoragePermissionHelper.kt`, `NetworkBypassSettingsCard.kt`, `ViauraStyledIcon.kt` |
| `viaura_shield_minimalistic` | `ViauraStyledIcon.kt` |
| `viaura_shuffle` | `SharedPlaylistHeaderCard.kt`, `PlayerControlLayoutModels.kt`, `VideoPlayerControlsOverlay.kt`, `ViauraStyledIcon.kt` |
| `viaura_size` | `SortAndViewOptionsPopup.kt`, `ViauraStyledIcon.kt` |
| `viaura_sleep` | `AudioPlayerFullScreen.kt`, `PlayerMoreMenuPanel.kt` |
| `viaura_smartphone` | `ViauraStyledIcon.kt` |
| `viaura_smartphone_rotate_orientation` | `PlayerControlLayoutModels.kt`, `VideoPlayerControlsOverlay.kt`, `ViauraStyledIcon.kt` |
| `viaura_song` | `PlayerControlLayoutModels.kt`, `VideoPlayerControlsOverlay.kt`, `ViauraStyledIcon.kt` |
| `viaura_sort` | `ViauraStyledIcon.kt` |
| `viaura_sort_by_alphabet` | `ViauraStyledIcon.kt` |
| `viaura_sort_horizontal` | `ViauraStyledIcon.kt` |
| `viaura_sort_vertical` | `ViauraStyledIcon.kt` |
| `viaura_speed` | `AudioPlayerFullScreen.kt`, `PlayerControlLayoutModels.kt`, `VideoPlayerControlsOverlay.kt`, `ViauraStyledIcon.kt` |
| `viaura_subtitles` | `StoragePermissionHelper.kt`, `PlayerControlLayoutModels.kt`, `VideoPlayerControlsOverlay.kt`, `SettingsDashboardSheet.kt`, `ViauraStyledIcon.kt` |
| `viaura_sun` | `SettingsDashboardSheet.kt`, `ViauraStyledIcon.kt` |
| `viaura_sun_2` | `ViauraStyledIcon.kt` |
| `viaura_tablet` | `ControlLayoutEditorScreen.kt` |
| `viaura_telegram` | `AboutDashboardPanel.kt` |
| `viaura_text` | `SortAndViewOptionsPopup.kt`, `ViauraStyledIcon.kt` |
| `viaura_text_bold` | `ViauraStyledIcon.kt` |
| `viaura_text_field` | `ViauraStyledIcon.kt` |
| `viaura_text_italic` | `ViauraStyledIcon.kt` |
| `viaura_text_underline` | `SubtitleTypographySection.kt` |
| `viaura_translation` | `PlayerControlLayoutModels.kt`, `VideoPlayerControlsOverlay.kt`, `ViauraStyledIcon.kt` |
| `viaura_trash_bin_2` | `RecentStreamRows.kt`, `PluginRepositoryManager.kt`, `ViauraStyledIcon.kt` |
| `viaura_tuning` | `ViauraStyledIcon.kt` |
| `viaura_updates` | `UpdateSettingsCardAndSheet.kt` |
| `viaura_upi` | `AboutDashboardPanel.kt` |
| `viaura_upload_minimalistic` | `ViauraStyledIcon.kt` |
| `viaura_user` | `SettingsDashboardSheet.kt` |
| `viaura_video` | `RecentlyPlayedTab.kt`, `HomeLibraryScreen.kt`, `ProviderHomePage.kt`, `ProviderPostCards.kt`, `ViauraStyledIcon.kt` |
| `viaura_video_library` | `ProviderHomePage.kt`, `BottomNavigationTabs.kt`, `ViauraStyledIcon.kt` |
| `viaura_video_setting` | `PlayerControlLayoutModels.kt`, `VideoPlayerControlsOverlay.kt`, `PlayerMoreMenuPanel.kt` |
| `viaura_volume` | `ViauraStyledIcon.kt` |
| `viaura_volume_cross` | `ViauraStyledIcon.kt` |
| `viaura_volume_loud` | `ViauraStyledIcon.kt` |
| `viaura_volume_small` | `ViauraStyledIcon.kt` |
| `viaura_watch_later` | `OnlineCardSwipeToolbar.kt`, `ProviderPostCards.kt` |
| `viaura_widget_5` | `PlayerControlLayoutModels.kt`, `VideoPlayerControlsOverlay.kt`, `SettingsDashboardSheet.kt`, `ViauraStyledIcon.kt` |
| `viaura_wifi` | `ViauraStyledIcon.kt` |
| `viaura_winrar` | `HomeLibraryScreen.kt`, `PlaylistImportSheets.kt`, `PlaylistOptionsAndCreateSheets.kt`, `ViauraStyledIcon.kt` |

## 7. Zip map

| Zip (repo path) | Unpacks to | Contents | Unpacked by |
|---|---|---|---|
| `app/src/viaura-icons.zip` | `app/src/main/res/drawable/` | 149 `viaura_*.xml` icons | CI step "Unpack vector icons"; `settings.gradle.kts` (local) |
| `app/src/cloudstream-lib.zip` | `app/src/cloudstream-lib/` | Gradle module `:cloudstream-lib` (163 files: CloudStream library sources + `com.lagradost.viaura.CloudStreamEngine`) | CI step "Prepare CloudStream runtime module"; `settings.gradle.kts` |
| `app/src/lyrics-providers-src.zip` | `app/src/main/java/com/example/lyrics/providers/` | 21 small lyrics-provider Kotlin files | CI step "Unpack packed source folders"; `settings.gradle.kts` |
| `app/src/widgets-src.zip` | `app/src/main/java/com/example/widgets/` | 16 small reusable Compose widget files | same |
| `app/src/unit-tests-src.zip` | `app/src/test/java/` | 12 JVM unit tests (section 9) | same |

All zips keep entries relative to the folder named in their step (icons: `app/src/main/res`; cloudstream-lib and the three `-src` zips: `app/src`). The unpacked folders are git-ignored.

`cloudstream-lib.zip` layout: `cloudstream-lib/build.gradle.kts`, `src/main/AndroidManifest.xml`, `src/main/kotlin/com/lagradost/{api,cloudstream3,viaura}` — `cloudstream3/extractors/` (≈110 host extractors), `extractors/helper/`, `metaproviders/`, `network/` (BrowserChallenge, CloudflareKiller, WebViewResolver …), `plugins/`, `utils/` (M3u8Helper, JsUnpacker, …), `mvvm/`, `actions/`, `syncproviders/`, and `viaura/CloudStreamEngine.kt` (loads a `.cs3`, answers catalog/search/info/streams as JSON).

## 8. CI order (`.github/workflows/build-apk.yml`, job "Build & Package APKs")

1. Checkout
2. **Remove legacy file paths** (reads `.github/legacy-paths.txt`)
3. **Unpack packed source folders** (lyrics providers, widgets, unit tests)
4. Unpack vector icons (`viaura-icons.zip`)
5. Compute app version (run number = versionCode; release tag or `-nightly.N`)
6. Fetch bundled subtitle font (GoNotoCurrent-Regular.ttf)
7. Fetch yt-dlp Python standard library archive (python313.zip)
8. Fetch latest yt-dlp release
9. Fetch QuickJS-NG sources
10. Fetch yt-dlp CA bundle and libpython.so
11. Prepare CloudStream runtime module (`cloudstream-lib.zip`)
12. JDK 17, Android SDK, licences, resource / SDK checks (duplicate resource names)
13. Gradle setup, choose wrapper, memory settings
14. Prepare fixed signing keystore
15. `:app:assembleRelease`
16. Locate / verify APKs, upload artifacts, publish GitHub release (nightly or release tag)

## 9. Unit tests (`app/src/test/java/com/example/…`, all inside `unit-tests-src.zip`)

| Test file | Covers |
|---|---|

| `app/AppNameResourceTest.kt` (class `AppNameResourceTest`) | app name resource (Robolectric) |
| `library/EpisodeNumberParsingTest.kt` (class `EpisodeNumberParsingTest`) | `EpisodeNameParser` |
| `library/MkvExtractedTrackFileNameTest.kt` (class `MkvExtractedTrackFileNameTest`) | file names of extracted MKV tracks |
| `library/archive/ArchiveTocScanTest.kt` (class `ArchiveTocScanTest`) | `ArchiveEngine` |
| `library/m3u/M3uPlaylistTextParsingTest.kt` (class `M3uPlaylistTextParsingTest`) | `M3uPlaylistParser` |
| `online/MusicRecommendationBlendTest.kt` (class `MusicRecommendationBlendTest`) | `OnlineRecommendationMixer` |
| `online/NextTrackOrderTest.kt` (class `NextTrackOrderTest`) | `OnlineTrackSequencer` |
| `online/SearchResultOrderingTest.kt` (class `SearchResultOrderingTest`) | `OnlineSearchRanker` |
| `online/ViewerInterestLearningTest.kt` (class `ViewerInterestLearningTest`) | `OnlineInterestProfile` |
| `providers/HlsMasterPlaylistParsingTest.kt` (class `HlsMasterPlaylistParsingTest`) | `HlsVariants` master playlist parsing |
| `settings/SettingsXmlAndAssFontSanitizeTest.kt` (class `SettingsXmlAndAssFontSanitizeTest`) | ASS font sanitising + settings XML export/import |
| `subtitles/AssCueDurationResolveTest.kt` (class `AssCueDurationResolveTest`) | ASS cue duration resolving (`MkvManagerEngine`) |

## 10. Symbol index (classes, objects, interfaces, typealiases, public composables/functions starting with a capital)

Format: `Symbol` → file path under `app/src/main/java/com/example/`. Lower-case top-level helpers: search the folder of the feature.

| Symbol | File |
|---|---|
| `AboutSettingsPanel` | `settings/about/AboutDashboardPanel.kt` |
| `AddArchivePlaylistSheet` | `library/playlists/PlaylistImportSheets.kt` |
| `AddPlaylistFromUrlSheet` | `library/playlists/PlaylistImportSheets.kt` |
| `AddToPlaylistDialog` | `library/playlists/AddToPlaylistPickerDialog.kt` |
| `AdvancedAssSource` | `subtitles/ass/AssStyleModels.kt` |
| `AdvancedAssStyleEngine` | `subtitles/ass/AssStyleRewriteEngine.kt` |
| `AdvancedSettingsPanel` | `settings/advanced/AdvancedSettingsTabPanel.kt` |
| `AiFeaturesSettings` | `ai/AiSettingsPrefsStore.kt` |
| `AiFeaturesSettingsPanel` | `settings/ai/AiFeaturesSettingsTab.kt` |
| `AiFeaturesSettingsStore` | `ai/AiSettingsPrefsStore.kt` |
| `AiModelInfo` | `ai/AiProviderCatalog.kt` |
| `AiProvider` | `ai/AiProviderCatalog.kt` |
| `AiProviderService` | `ai/AiProviderHttpGateway.kt` |
| `AnimatedAudioEqualizer` | `audio/library/AudioEqualizerBars.kt` |
| `ApiKeyCrypto` | `ai/ApiKeyKeystoreVault.kt` |
| `ApiKeyVerificationState` | `ai/AiProviderCatalog.kt` |
| `AppConfig` | `settings/state/AppUiState.kt` |
| `AppHaptics` | `app/HapticFeedbackController.kt` |
| `AppLanguage` | `settings/state/ThemeAndDisplayOptionEnums.kt` |
| `AppLaunchSplash` | `app/StartupSplashScreen.kt` |
| `AppStorageManager` | `app/AppStorageFolders.kt` |
| `AppStrings` | `settings/state/LocalizedAppStrings.kt` |
| `AppThemeVariant` | `settings/state/ThemeAndDisplayOptionEnums.kt` |
| `AppUpdateHost` | `app/update/UpdateSettingsCardAndSheet.kt` |
| `AppUpdateInfo` | `app/update/AppUpdateModels.kt` |
| `AppUpdater` | `app/update/GithubReleaseUpdater.kt` |
| `AppViewModel` | `settings/state/AppSettingsViewModel.kt` |
| `ArchiveEngine` | `library/archive/ArchiveTocEngine.kt` |
| `ArchiveEntry` | `library/archive/ArchiveModels.kt` |
| `ArchiveEntryDataSource` | `library/archive/ArchiveEntryRandomAccess.kt` |
| `ArchiveException` | `library/archive/ArchiveModels.kt` |
| `ArchiveKind` | `library/archive/ArchiveModels.kt` |
| `ArchiveProbe` | `library/archive/ArchiveVideoProber.kt` |
| `ArchiveRecord` | `library/archive/ArchiveRecordStore.kt` |
| `ArchiveScan` | `library/archive/ArchiveModels.kt` |
| `ArchiveSeekableReader` | `library/archive/ArchiveEntryRandomAccess.kt` |
| `ArchiveSource` | `library/archive/ArchiveModels.kt` |
| `ArchiveStore` | `library/archive/ArchiveRecordStore.kt` |
| `ArchiveStreamServer` | `library/archive/ArchiveLoopbackStreamServer.kt` |
| `ArtworkColors` | `audio/artwork/ArtworkColorExtractor.kt` |
| `AspectRatioMode` | `player/overlay/PlayerModeEnums.kt` |
| `AssFields` | `subtitles/ass/AssStyleModels.kt` |
| `AssOverridesStore` | `subtitles/ass/AssOverrideStores.kt` |
| `AssParser` | `subtitles/engine/AssSubtitleParser.kt` |
| `AssRawTextStore` | `subtitles/ass/AssOverrideStores.kt` |
| `AssStyleInfo` | `subtitles/ass/AssStyleModels.kt` |
| `AssStylesDocument` | `subtitles/ass/AssStyleModels.kt` |
| `AssSubtitleOverrides` | `subtitles/ass/AssStyleModels.kt` |
| `AttachedSubtitleRecord` | `subtitles/store/SubtitleSelectionRecords.kt` |
| `AudioChannelMode` | `audio/settings/AudioChannelAndEqModels.kt` |
| `AudioChoice` | `download/probe/ProbeResultModels.kt` |
| `AudioDelayPanel` | `audio/panels/AudioDelayPanel.kt` |
| `AudioFileItem` | `audio/library/AudioFilePickerDialog.kt` |
| `AudioFileManager` | `audio/library/AudioFileOperations.kt` |
| `AudioFileTreeDialog` | `audio/library/AudioFilePickerDialog.kt` |
| `AudioFolderGridCard` | `audio/library/AudioFolderCards.kt` |
| `AudioFolderItem` | `audio/library/AudioLibraryModels.kt` |
| `AudioFolderListCard` | `audio/library/AudioFolderCards.kt` |
| `AudioFolderVideoSelectorSheet` | `audio/library/AudioFolderVideoPickerSheet.kt` |
| `AudioGroup` | `providers/detail/MediaDetailGrouping.kt` |
| `AudioLanguageMatcher` | `audio/settings/AudioLanguageMatching.kt` |
| `AudioLibraryCache` | `audio/library/AudioLibraryDiskCache.kt` |
| `AudioLyricsContent` | `lyrics/ui/KaraokeLyricsPanel.kt` |
| `AudioMetadataExtractor` | `audio/AudioTagMetadataReader.kt` |
| `AudioMusicTabContent` | `audio/library/AudioMusicTab.kt` |
| `AudioNotificationManager` | `audio/notification/AudioNotificationBuilder.kt` |
| `AudioNotificationReceiver` | `audio/notification/AudioNotificationActionReceiver.kt` |
| `AudioPlaybackHistoryManager` | `audio/AudioResumeHistoryStore.kt` |
| `AudioPlaybackManager` | `audio/AudioQueuePlaybackCore.kt` |
| `AudioPlaybackResume` | `audio/AudioResumeHistoryStore.kt` |
| `AudioPlaybackService` | `audio/notification/AudioPlaybackForegroundService.kt` |
| `AudioPlayerScreen` | `audio/ui/AudioPlayerFullScreen.kt` |
| `AudioPreferences` | `lyrics/LyricsPreferenceStore.kt` |
| `AudioRepeatMode` | `audio/AudioQueuePlaybackCore.kt` |
| `AudioSelectionPanel` | `audio/panels/AudioTrackSelectionPanel.kt` |
| `AudioSurroundMode` | `audio/settings/AudioChannelAndEqModels.kt` |
| `AudioTagger` | `download/tagging/AudioTagWriter.kt` |
| `AudioTrackGridCard` | `audio/library/AudioTrackCards.kt` |
| `AudioTrackItem` | `audio/library/AudioLibraryModels.kt` |
| `AudioTrackListCard` | `audio/library/AudioTrackCards.kt` |
| `AudioTrackThumbnail` | `audio/artwork/AudioArtworkLoader.kt` |
| `AutoScrollingText` | `widgets/MarqueeTitleText.kt` |
| `BestModelSelector` | `ai/AiProviderCatalog.kt` |
| `BetterLyricsProvider` | `lyrics/providers/BetterLyricsSource.kt` |
| `BiniLyricsProvider` | `lyrics/providers/BiniLyricsSource.kt` |
| `BlockedFolderRowItem` | `settings/folders/FolderSettingsRowCards.kt` |
| `BottomMoveCopyProgressBar` | `library/files/MoveCopyProgressBar.kt` |
| `BulkCaseFormat` | `library/rename/BulkRenameModels.kt` |
| `BulkRenameConfig` | `library/rename/BulkRenameModels.kt` |
| `BulkRenameEngine` | `library/rename/BulkRenameComputation.kt` |
| `BulkRenameItem` | `library/rename/BulkRenameModels.kt` |
| `BulkRenameMode` | `library/rename/BulkRenameModels.kt` |
| `BulkRenamePreview` | `library/rename/BulkRenameModels.kt` |
| `BulkRenameSheet` | `library/rename/BulkRenameSheetHost.kt` |
| `BulkRenameStatus` | `library/rename/BulkRenameModels.kt` |
| `CachedVideoMeta` | `library/VideoLibraryDiskCache.kt` |
| `CancelToken` | `download/http/FetchExceptionsAndTokens.kt` |
| `ChannelDetailScreen` | `online/ui/ChannelDetailPage.kt` |
| `ChapterSelectionPanel` | `player/panels/ChapterSelectionPanel.kt` |
| `ChapterSkipDetector` | `player/chapters/ChapterSkipDetection.kt` |
| `ChapterSkipMarker` | `player/chapters/ChapterSkipModels.kt` |
| `ColorTarget` | `subtitles/style/SubtitleColorSection.kt` |
| `ContainerSniffer` | `download/probe/MediaContainerDetector.kt` |
| `ControlArea` | `player/layout/PlayerControlLayoutModels.kt` |
| `ControlLayoutEditor` | `player/layout/ControlLayoutEditorScreen.kt` |
| `ControlsAnimationStyle` | `settings/state/ThemeAndDisplayOptionEnums.kt` |
| `CrashReportDialog` | `app/CrashReportSheet.kt` |
| `CrashReporter` | `app/CrashReasonRecorder.kt` |
| `CreateEmptyPlaylistSheet` | `library/playlists/PlaylistOptionsAndCreateSheets.kt` |
| `CustomPlaylist` | `library/playlists/CustomPlaylistStore.kt` |
| `CustomPlaylistsTabContent` | `library/playlists/CustomPlaylistsTab.kt` |
| `CustomSubtitlePersistenceManager` | `subtitles/store/ExternalSubtitleSelectionStore.kt` |
| `DecoderMode` | `player/overlay/PlayerModeEnums.kt` |
| `DelayMsSuffixTransformation` | `player/panels/DelayPanelControls.kt` |
| `DeleteAnimatedBox` | `widgets/DeleteItemAnimation.kt` |
| `DeleteAnimState` | `widgets/DeleteItemAnimation.kt` |
| `DeleteConfirmSheet` | `widgets/DeleteConfirmationSheet.kt` |
| `DetailVideo` | `providers/detail/MediaDetailGrouping.kt` |
| `DetectedAudioInfo` | `settings/about/DeviceHardwareProbe.kt` |
| `DetectedDisplayInfo` | `settings/about/DeviceHardwareProbe.kt` |
| `DetectedHardwareInfo` | `settings/about/DeviceHardwareProbe.kt` |
| `DirectoryPickerAction` | `library/files/DirectoryPickerSheet.kt` |
| `DirEntry` | `library/files/DirectoryPickerSheet.kt` |
| `DownloadFileSaver` | `download/DownloadPublicFolderSaver.kt` |
| `DownloadForegroundService` | `download/DownloadForegroundNotificationService.kt` |
| `DownloadFormat` | `download/model/DownloadItemModels.kt` |
| `DownloadGlassSheet` | `download/ui/DownloadGlassSheetFrame.kt` |
| `DownloadItem` | `download/model/DownloadItemModels.kt` |
| `DownloadManager` | `download/DownloadQueueManager.kt` |
| `DownloadModeTabs` | `download/ui/DownloadSheetWidgets.kt` |
| `DownloadOptionsSheet` | `download/ui/OnlineDownloadOptionsSheet.kt` |
| `DownloadPickerSheet` | `download/ui/DownloadSheetWidgets.kt` |
| `DownloadPlan` | `download/model/DownloadPlanModels.kt` |
| `DownloadRequest` | `online/ui/OnlineCardSwipeToolbar.kt` |
| `DownloadRequestHost` | `online/ui/OnlineCardSwipeToolbar.kt` |
| `DownloadsContentEntry` | `download/ui/DownloadsListEntries.kt` |
| `DownloadSheetHeader` | `download/ui/DownloadSheetWidgets.kt` |
| `DownloadSourceType` | `download/model/DownloadItemModels.kt` |
| `DownloadsTabContent` | `download/ui/DownloadsTab.kt` |
| `DownloadStatus` | `download/model/DownloadItemModels.kt` |
| `DownloadStore` | `download/DownloadRecordStore.kt` |
| `DynamicBottomNavigation` | `widgets/BottomNavigationTabs.kt` |
| `EdgeToEdgeSheetWindowEffect` | `glass/EdgeToEdgeDialogWindow.kt` |
| `EmbeddedLyricsExtractor` | `lyrics/LocalLyricsSidecarReader.kt` |
| `EmptyAudioFoldersInlineView` | `audio/library/AudioEmptyStateViews.kt` |
| `EmptyAudioView` | `audio/library/AudioEmptyStateViews.kt` |
| `EpisodeFrames` | `providers/thumbnails/EpisodeFrameGrabber.kt` |
| `EpisodeNameParser` | `library/EpisodeNumberParser.kt` |
| `EqualizerPreset` | `audio/settings/AudioChannelAndEqModels.kt` |
| `ExtractedMediaMetadata` | `library/metadata/ExtractedMediaMetadataModel.kt` |
| `FavouritesFolder` | `library/playlists/FavouritesFolderStore.kt` |
| `FetchCancelled` | `download/http/FetchExceptionsAndTokens.kt` |
| `FileSeekableReader` | `library/mkv/SeekableByteReaders.kt` |
| `FloatingDirectoryPickerSheet` | `library/files/DirectoryPickerSheet.kt` |
| `FolderBlockListManager` | `library/folders/FolderBlockList.kt` |
| `FolderDateUtils` | `library/folders/FolderCreationTime.kt` |
| `FolderGlyphType` | `library/folders/FolderGlyphIcon.kt` |
| `FolderIconKind` | `library/folders/FolderGlyphIcon.kt` |
| `FolderMetadataCard` | `settings/folders/FolderSettingsRowCards.kt` |
| `FolderSettingsSelectionMode` | `settings/folders/FoldersSettingsTab.kt` |
| `FoldersSettingsTabContent` | `settings/folders/FoldersSettingsTab.kt` |
| `FollowCursorTextField` | `widgets/LongTextInputField.kt` |
| `FollowCursorTextField` | `widgets/LongTextInputField.kt` |
| `GeniusProvider` | `lyrics/providers/GeniusLyricsScraper.kt` |
| `GestureSensitivityMode` | `settings/state/ThemeAndDisplayOptionEnums.kt` |
| `GoogleFontsManager` | `subtitles/fonts/GoogleFontsDownloader.kt` |
| `HeaderBar` | `widgets/TopHeaderBar.kt` |
| `HeadInfo` | `download/http/FetchExceptionsAndTokens.kt` |
| `HighResImage` | `widgets/FullWidthHdImage.kt` |
| `HighResImageLoader` | `widgets/FullWidthHdImage.kt` |
| `HlsMediaPlaylist` | `providers/hls/HlsPlaylistModels.kt` |
| `HlsRendition` | `providers/hls/HlsPlaylistModels.kt` |
| `HlsVariant` | `providers/hls/HlsPlaylistModels.kt` |
| `HlsVariants` | `providers/hls/HlsMasterPlaylistParser.kt` |
| `HomeScreen` | `library/home/HomeLibraryScreen.kt` |
| `HttpArchiveSource` | `library/archive/ArchiveByteSources.kt` |
| `HttpFetch` | `download/http/ChunkedHttpFetcher.kt` |
| `HttpStatusException` | `download/http/FetchExceptionsAndTokens.kt` |
| `HwAccelMode` | `settings/state/ThemeAndDisplayOptionEnums.kt` |
| `I18n` | `settings/i18n/UiTextTranslator.kt` |
| `InlineRenameField` | `library/rename/InlineRenameTextField.kt` |
| `InteractiveAudioScrubber` | `audio/ui/AudioSeekScrubber.kt` |
| `KaraokeLrc` | `lyrics/providers/LyricsTextParsers.kt` |
| `KuGouProvider` | `lyrics/providers/KuGouLyricsSource.kt` |
| `LayoutMode` | `library/sort/SortAndViewModels.kt` |
| `LayoutOrientationMode` | `player/layout/PlayerControlLayoutModels.kt` |
| `LimitedInputStream` | `library/archive/ArchiveByteSources.kt` |
| `LinkExpiredException` | `download/http/FetchExceptionsAndTokens.kt` |
| `LinkInfo` | `download/probe/ProbeResultModels.kt` |
| `LinkThumbnail` | `widgets/LinkThumbnailImage.kt` |
| `LiquidBackdropState` | `glass/LiquidGlassModifiers.kt` |
| `LiquidDialogBlur` | `glass/LiquidGlassModifiers.kt` |
| `LiquidGlassCard` | `glass/LiquidGlassSurfaces.kt` |
| `LiquidGlassDefaults` | `glass/LiquidGlassSurfaces.kt` |
| `LiquidGlassDialog` | `glass/LiquidGlassSurfaces.kt` |
| `LiquidGlassParams` | `glass/GlassPanelTokens.kt` |
| `LiquidGlassPreview` | `glass/LiquidGlassPreviewCanvas.kt` |
| `LiquidGlassSlider` | `glass/controls/ViauraSlidersAndSwitches.kt` |
| `LiquidGlassSwitch` | `glass/controls/ViauraSlidersAndSwitches.kt` |
| `LiquidGlassVerticalBar` | `glass/controls/ViauraSlidersAndSwitches.kt` |
| `LiquidPullToRefresh` | `glass/LiquidPullRefreshIndicator.kt` |
| `LiveSeekRange` | `player/mpv/MpvTrackAndChapterModels.kt` |
| `LocalAudioThumbnailDiskCache` | `audio/artwork/AudioArtworkDiskCaches.kt` |
| `LocalBypassProxy` | `network/bypass/LoopbackBypassProxyServer.kt` |
| `LrcLibApiService` | `lyrics/online/LrcLibHttpApi.kt` |
| `LrcLibProvider` | `lyrics/providers/LrcLibLyricsSource.kt` |
| `LrcLibResponse` | `lyrics/online/LrcLibHttpApi.kt` |
| `LrcParser` | `lyrics/LrcTextReader.kt` |
| `LrcRedProvider` | `lyrics/providers/LrcRedLyricsSource.kt` |
| `Lyrics` | `lyrics/model/LyricsDataModels.kt` |
| `LyricsFetcher` | `lyrics/LyricsWebFetcher.kt` |
| `LyricsFetchQuery` | `lyrics/providers/LyricsSourceContract.kt` |
| `LyricsPlaceholderLines` | `lyrics/ui/LyricsLoadingSkeleton.kt` |
| `LyricsPlusProvider` | `lyrics/providers/LyricsPlusSource.kt` |
| `LyricsProvider` | `lyrics/model/LyricsSourceCatalog.kt` |
| `LyricsProviderCache` | `lyrics/LyricsSourceResultCache.kt` |
| `LyricsProviderClient` | `lyrics/providers/LyricsSourceContract.kt` |
| `LyricsProviderPickerDialog` | `lyrics/ui/LyricsSourcePickerDialog.kt` |
| `LyricsProviderRegistry` | `lyrics/providers/LyricsSourceRegistry.kt` |
| `LyricsRepository` | `lyrics/LyricsLookupRepository.kt` |
| `LyricsResult` | `lyrics/LyricsLookupResults.kt` |
| `LyricsSourceType` | `lyrics/model/LyricsDataModels.kt` |
| `LyricsTextCodec` | `lyrics/LyricsToLrcConverter.kt` |
| `LyricsTrackMatcher` | `lyrics/providers/LyricsTrackMatching.kt` |
| `LyricsTranslationService` | `lyrics/translation/LyricsLineTranslator.kt` |
| `LyricsUtils` | `lyrics/LrcTimingHelpers.kt` |
| `M3uEntry` | `library/m3u/M3uPlaylistModels.kt` |
| `M3uParseResult` | `library/m3u/M3uPlaylistModels.kt` |
| `M3uPlaylistParser` | `library/m3u/M3uPlaylistTextParser.kt` |
| `MainActivity` | `app/AppEntryActivity.kt` |
| `MangaDetect` | `providers/manga/MangaChapterDetector.kt` |
| `MangaReaderScreen` | `providers/manga/MangaChapterReader.kt` |
| `ManualVideoAdjustments` | `player/filters/VideoFilterPresets.kt` |
| `MediaActor` | `providers/model/PluginCatalogModels.kt` |
| `MediaCatalogEntry` | `providers/model/PluginCatalogModels.kt` |
| `MediaDetailScreen` | `providers/detail/MediaDetailPage.kt` |
| `MediaDetailState` | `providers/detail/MediaDetailState.kt` |
| `MediaDirectLink` | `providers/model/PluginCatalogModels.kt` |
| `MediaDownloadSheet` | `providers/download/ProviderDownloadSheet.kt` |
| `MediaGridMemory` | `providers/ui/ProviderPostCards.kt` |
| `MediaHistoryStore` | `providers/store/WatchedTitleHistoryStore.kt` |
| `MediaInfo` | `providers/model/PluginCatalogModels.kt` |
| `MediaInfoData` | `library/info/MediaInfoModels.kt` |
| `MediaInformationScreen` | `library/info/MediaInformationPage.kt` |
| `MediaInfoSection` | `library/info/MediaInfoModels.kt` |
| `MediaLibraryStore` | `providers/store/WatchLaterStore.kt` |
| `MediaLink` | `providers/model/PluginCatalogModels.kt` |
| `MediaMetadataExtractor` | `library/metadata/MediaFileMetadataExtractor.kt` |
| `MediaPlayback` | `providers/playback/PluginStreamResolver.kt` |
| `MediaPluginInfo` | `providers/model/PluginCatalogModels.kt` |
| `MediaPluginManager` | `providers/ui/PluginRepositoryManager.kt` |
| `MediaPluginPage` | `providers/ui/ProviderHomePage.kt` |
| `MediaPluginStore` | `providers/store/PluginInstallStore.kt` |
| `MediaPost` | `providers/model/PluginCatalogModels.kt` |
| `MediaPreferences` | `providers/settings/ProviderStreamPreferences.kt` |
| `MediaProbe` | `download/probe/OnlineMediaProber.kt` |
| `MediaProviderRepository` | `providers/ProviderApiRepository.kt` |
| `MediaSavedVideo` | `providers/store/WatchLaterStore.kt` |
| `MediaSeenManager` | `library/history/SeenUnplayedTracker.kt` |
| `MediaSideMemory` | `library/history/VideoAudioSideMemory.kt` |
| `MediaStream` | `providers/model/PluginCatalogModels.kt` |
| `MediaSubtitle` | `providers/model/PluginCatalogModels.kt` |
| `MediaWatchedTitle` | `providers/store/WatchedTitleHistoryStore.kt` |
| `MegalobizProvider` | `lyrics/providers/MegalobizLyricsScraper.kt` |
| `MinimizedAudioPlayer` | `audio/ui/MiniAudioPlayerBar.kt` |
| `MinimizedVideoPlayer` | `player/mini/MiniVideoPlayerPopup.kt` |
| `MkvAttachmentInfo` | `library/mkv/MkvContainerModels.kt` |
| `MkvChapterInfo` | `library/mkv/MkvContainerModels.kt` |
| `MkvContainerMetadata` | `library/mkv/MkvContainerModels.kt` |
| `MkvManagerEngine` | `library/mkv/MkvEbmlEngine.kt` |
| `MkvMuxer` | `download/mux/MkvStreamMuxer.kt` |
| `MkvTrackInfo` | `library/mkv/MkvContainerModels.kt` |
| `MkvTrackType` | `library/mkv/MkvContainerModels.kt` |
| `ModernFolderIcon` | `library/folders/FolderGlyphIcon.kt` |
| `MoreMenuSelectionPanel` | `player/panels/PlayerMoreMenuPanel.kt` |
| `MpvPlayerController` | `player/mpv/MpvEngineController.kt` |
| `MusixmatchProvider` | `lyrics/providers/MusixmatchLyricsSource.kt` |
| `MutedCircularColorPicker` | `theme/picker/MutedCircleColorPicker.kt` |
| `MutedCircularColorPickerCard` | `theme/picker/MutedCircleColorPicker.kt` |
| `MutedPalette` | `theme/MutedSwatchPalette.kt` |
| `MutedSwatch` | `theme/MutedSwatchPalette.kt` |
| `MuxSource` | `download/mux/MuxInputModels.kt` |
| `MuxSubtitle` | `download/mux/MuxInputModels.kt` |
| `NaturalOrderComparator` | `library/NaturalSortComparator.kt` |
| `NavigationTab` | `widgets/BottomNavigationTabs.kt` |
| `NetworkBypass` | `network/bypass/ProviderNetworkBypassSettings.kt` |
| `NetworkBypassCard` | `network/ui/NetworkBypassSettingsCard.kt` |
| `NetworkMonitor` | `network/ConnectivityMonitor.kt` |
| `NetworkSettings` | `network/settings/NetworkStreamPrefs.kt` |
| `NetworkStream` | `network/stream/DirectStreamModels.kt` |
| `NetworkStreamResolver` | `network/stream/DirectStreamUrlResolver.kt` |
| `NetworkTabContent` | `network/ui/NetworkTab.kt` |
| `Neutral` | `theme/SurfaceAndTextColors.kt` |
| `OnlineAudioCache` | `audio/DisabledOnlineAudioCacheStub.kt` |
| `OnlineAudioFeedState` | `online/ui/OnlineAudioFeedHolder.kt` |
| `OnlineAudioSource` | `player/mpv/MpvTrackAndChapterModels.kt` |
| `OnlineAudioStreamInfo` | `online/video/OnlineVideoModels.kt` |
| `OnlineChannelDetail` | `online/music/OnlineMusicModels.kt` |
| `OnlineChannelPlaylist` | `online/music/OnlineMusicModels.kt` |
| `OnlineChannelResult` | `online/model/OnlineChannelResultModel.kt` |
| `OnlineChannelResultCard` | `online/ui/ChannelSearchResultCard.kt` |
| `OnlineChannelSection` | `online/music/OnlineMusicModels.kt` |
| `OnlineChannelTrackPage` | `online/music/OnlineMusicModels.kt` |
| `OnlineChapterInfo` | `online/video/OnlineVideoModels.kt` |
| `OnlineComment` | `online/video/OnlineVideoModels.kt` |
| `OnlineCommentPage` | `online/video/OnlineVideoModels.kt` |
| `OnlineFolderCard` | `audio/library/AudioFolderCards.kt` |
| `OnlineInterestProfile` | `online/recommend/ViewerInterestProfile.kt` |
| `OnlineLyricsResult` | `lyrics/LyricsLookupResults.kt` |
| `OnlineLyricsService` | `lyrics/OnlineLyricsGateway.kt` |
| `OnlineMediaLibrary` | `online/OnlineWatchLaterAndQueueStore.kt` |
| `OnlineMusicPanel` | `online/ui/OnlineMusicHomePanel.kt` |
| `OnlineMusicService` | `online/music/YouTubeMusicSearchService.kt` |
| `OnlineQuickActionBar` | `online/ui/OnlineCardSwipeToolbar.kt` |
| `OnlineRecommendationMixer` | `online/recommend/MusicRecommendationBlender.kt` |
| `OnlineSearchBridge` | `online/ui/OnlineAudioFeedHolder.kt` |
| `OnlineSearchRanker` | `online/search/OnlineSearchResultRanker.kt` |
| `OnlineSearchSuggestions` | `online/ui/OnlineSearchSuggestionList.kt` |
| `OnlineSkipMarkerService` | `player/chapters/IntroSkipMarkerClients.kt` |
| `OnlineTrackMetadataCache` | `online/music/OnlineTrackMetaDiskCache.kt` |
| `OnlineTrackResult` | `online/music/OnlineMusicModels.kt` |
| `OnlineTrackSequencer` | `online/music/NextTrackSequencer.kt` |
| `OnlineTrackVideoIds` | `online/music/OnlineTrackVideoIdMap.kt` |
| `OnlineVideoFeedState` | `online/ui/OnlineVideoFeedHolder.kt` |
| `OnlineVideoPanel` | `online/ui/OnlineVideoHomePanel.kt` |
| `OnlineVideoPlayback` | `online/video/OnlineVideoModels.kt` |
| `OnlineVideoQuality` | `online/video/OnlineVideoModels.kt` |
| `OnlineVideoQueue` | `online/OnlineWatchLaterAndQueueStore.kt` |
| `OnlineVideoRecommender` | `online/recommend/VideoFeedRecommender.kt` |
| `OnlineVideoService` | `online/video/YouTubeVideoStreamService.kt` |
| `OnlineWatchDetails` | `online/video/OnlineVideoModels.kt` |
| `PackagedStubVideoDetector` | `library/StubPlaceholderVideoDetector.kt` |
| `PanelScaled` | `glass/GlassPanelTokens.kt` |
| `ParsedEpisode` | `library/EpisodeNumberParser.kt` |
| `ParsedMagnet` | `torrent/TorrentCatalogModels.kt` |
| `PaxSenixApi` | `lyrics/providers/PaxSenixLyricsApi.kt` |
| `PaxSenixProvider` | `lyrics/providers/PaxSenixLyricsSource.kt` |
| `PaxSenixRoute` | `lyrics/providers/PaxSenixLyricsSource.kt` |
| `PipModeState` | `app/PictureInPictureState.kt` |
| `PlanChapter` | `download/model/DownloadPlanModels.kt` |
| `PlanPart` | `download/model/DownloadPlanModels.kt` |
| `PlanSubtitle` | `download/model/DownloadPlanModels.kt` |
| `PlaybackHistoryManager` | `library/history/VideoPlaybackHistoryStore.kt` |
| `PlayerChapterSkipButton` | `player/chapters/ChapterSkipButton.kt` |
| `PlayerControlId` | `player/layout/PlayerControlLayoutModels.kt` |
| `PlayerGlassModalSheet` | `player/panels/PlayerGlassModalSheet.kt` |
| `PlayerHeaderAction` | `player/PortraitHeaderButtonRequest.kt` |
| `PlayerHeaderRequest` | `player/PortraitHeaderButtonRequest.kt` |
| `PlayerLayoutConfig` | `player/layout/PlayerControlLayoutModels.kt` |
| `PlayerMediaTrack` | `player/mpv/MpvTrackAndChapterModels.kt` |
| `PlayerPanelHeightStore` | `player/panels/PlayerPanelGlassTokens.kt` |
| `PlayerPanelType` | `player/overlay/PlayerModeEnums.kt` |
| `PlayerRepeatMode` | `player/overlay/PlayerModeEnums.kt` |
| `PlayerSettings` | `settings/state/PlayerSettingsModel.kt` |
| `PlayerVideoChapter` | `player/mpv/MpvTrackAndChapterModels.kt` |
| `PlayGradientLogoIcon` | `widgets/GradientPlayLogoIcon.kt` |
| `PlaylistFilterTabs` | `library/playlists/PlaylistRevealSwipeRow.kt` |
| `PlaylistImporter` | `library/playlists/PlaylistFileImporter.kt` |
| `PlaylistImportResult` | `library/playlists/PlaylistImportResultModel.kt` |
| `PlaylistMarkStore` | `library/playlists/PlaylistFavouriteMarkStore.kt` |
| `PlaylistOptionsSheet` | `library/playlists/PlaylistOptionsAndCreateSheets.kt` |
| `PlaylistQuickTarget` | `library/playlists/PlaylistQuickTargets.kt` |
| `PlaylistRevealSwipe` | `library/playlists/PlaylistRevealSwipeRow.kt` |
| `PluginBrowserHost` | `providers/browser/PluginBrowserHostSheet.kt` |
| `PluginBrowserState` | `providers/browser/InAppBrowserRequestState.kt` |
| `PluginCookieJar` | `providers/runtime/ProviderCookieJar.kt` |
| `PluginCrypto` | `providers/runtime/ProviderNativeCrypto.kt` |
| `PluginException` | `providers/model/PluginCatalogModels.kt` |
| `PluginJsEngine` | `providers/runtime/ProviderJsRuntimeHost.kt` |
| `PluginPlaybackSession` | `providers/playback/PluginPlaybackSessions.kt` |
| `PluginPlaybackSessions` | `providers/playback/PluginPlaybackSessions.kt` |
| `PluginQualityChoice` | `providers/playback/PluginPlaybackSessions.kt` |
| `PluginQueuedEpisode` | `providers/playback/PluginEpisodeQueue.kt` |
| `PluginQueuedEpisodes` | `providers/playback/PluginEpisodeQueue.kt` |
| `PluginWebViewSolver` | `providers/browser/HiddenWebViewChallengeSolver.kt` |
| `PrivateFolder` | `library/vault/PrivateVaultModels.kt` |
| `PrivateFolderItemCard` | `settings/folders/FolderSettingsRowCards.kt` |
| `PrivateMediaType` | `library/vault/PrivateVaultModels.kt` |
| `PrivateVaultManager` | `library/vault/PrivateVaultStore.kt` |
| `ProviderOutcome` | `lyrics/providers/LyricsSourceRegistry.kt` |
| `QualityOption` | `providers/detail/MediaDetailGrouping.kt` |
| `QuickDownloadActivity` | `download/ui/ShareLinkQuickDownloadActivity.kt` |
| `RangeIgnored` | `download/http/FetchExceptionsAndTokens.kt` |
| `RawEditorDraft` | `subtitles/ui/RawSubtitleTextEditor.kt` |
| `RawFormat` | `download/probe/ProbeResultModels.kt` |
| `RawSubtitleSource` | `subtitles/ass/AssStyleModels.kt` |
| `RecentlyPlayedTabContent` | `library/history/RecentlyPlayedTab.kt` |
| `RecentsFolder` | `library/history/RecentlyPlayedTab.kt` |
| `RecentStreamEntry` | `network/stream/RecentStreamHistory.kt` |
| `RecentStreamStore` | `network/stream/RecentStreamHistory.kt` |
| `RecentThumbnails` | `network/stream/RecentStreamThumbnailFrames.kt` |
| `RemoteAudioThumbnailDiskCache` | `audio/artwork/AudioArtworkDiskCaches.kt` |
| `SavedAudioRef` | `audio/PerVideoAudioChoiceStore.kt` |
| `SavedSubtitleRef` | `subtitles/store/SubtitleSelectionRecords.kt` |
| `Screen` | `app/AppEntryActivity.kt` |
| `ScreenNavigationStyle` | `settings/state/ThemeAndDisplayOptionEnums.kt` |
| `SeasonGroup` | `providers/detail/MediaDetailGrouping.kt` |
| `SeekableReader` | `library/mkv/SeekableByteReaders.kt` |
| `SeekbarStyle` | `settings/state/ThemeAndDisplayOptionEnums.kt` |
| `SeekIcon` | `widgets/SkipSeekAnimatedIcon.kt` |
| `SegmentFetch` | `download/http/HlsDashSegmentFetcher.kt` |
| `SelectorRow` | `download/ui/DownloadSheetWidgets.kt` |
| `ServerCandidate` | `providers/download/ProviderServerQualityProbe.kt` |
| `ServerReputationStore` | `providers/playback/ServerReliabilityStore.kt` |
| `ServersState` | `providers/download/ProviderServerQualityProbe.kt` |
| `SettingsCategoryTab` | `settings/dashboard/SettingsDashboardSheet.kt` |
| `SettingsPreferencesManager` | `settings/state/SettingsPreferencesStore.kt` |
| `SettingsSheet` | `settings/dashboard/SettingsDashboardSheet.kt` |
| `SharedPlaylistScreen` | `library/playlists/SharedPlaylistPage.kt` |
| `SheetCaption` | `download/ui/DownloadSheetWidgets.kt` |
| `SheetCheckbox` | `download/ui/DownloadSheetWidgets.kt` |
| `SheetCheckMark` | `download/ui/DownloadSheetWidgets.kt` |
| `SheetChip` | `download/ui/DownloadSheetWidgets.kt` |
| `SheetLinkRow` | `download/ui/DownloadSheetWidgets.kt` |
| `SheetOptionCard` | `download/ui/DownloadSheetWidgets.kt` |
| `SheetSaveToCard` | `download/ui/DownloadSheetWidgets.kt` |
| `SheetSectionLabel` | `download/ui/DownloadSheetWidgets.kt` |
| `SheetTextAction` | `download/ui/DownloadSheetWidgets.kt` |
| `SheetTextField` | `download/ui/DownloadSheetWidgets.kt` |
| `SheetTrailing` | `download/ui/DownloadSheetWidgets.kt` |
| `ShineLogoIcon` | `widgets/ShineAppLogoIcon.kt` |
| `SimpMusicProvider` | `lyrics/providers/SimpMusicLyricsSource.kt` |
| `SkipMarkerType` | `player/chapters/ChapterSkipModels.kt` |
| `SleepTimerManager` | `player/SleepTimerController.kt` |
| `SleepTimerPanel` | `player/panels/SleepTimerPanel.kt` |
| `SmartDeviceProfile` | `player/smart/SmartAutoPlaybackAdvisor.kt` |
| `SmartStreamInfo` | `player/smart/SmartAutoPlaybackAdvisor.kt` |
| `SmartTier` | `player/smart/SmartAutoPlaybackAdvisor.kt` |
| `SortAndViewOptionsPopup` | `library/sort/SortAndViewOptionsPopup.kt` |
| `SortDirection` | `library/sort/SortAndViewModels.kt` |
| `SortField` | `library/sort/SortAndViewModels.kt` |
| `StoragePermissionDialog` | `app/permissions/StoragePermissionHelper.kt` |
| `StreamEntryMeta` | `network/stream/StreamEntryRecordStore.kt` |
| `StreamEntryStore` | `network/stream/StreamEntryRecordStore.kt` |
| `StreamInfoWithRelated` | `online/music/OnlineMusicModels.kt` |
| `StreamProbe` | `providers/playback/ParallelServerRaceProbe.kt` |
| `StreamRequestOptions` | `network/stream/StreamRequestHeaderOptions.kt` |
| `StyledIcon` | `widgets/ViauraStyledIcon.kt` |
| `StyledIcon` | `widgets/ViauraStyledIcon.kt` |
| `SubscribedChannel` | `online/SubscribedChannelStore.kt` |
| `SubscribedChannelsManager` | `online/SubscribedChannelStore.kt` |
| `SubtitleCue` | `subtitles/engine/SubtitleCueModels.kt` |
| `SubtitleDelayPanel` | `subtitles/panels/SubtitleDelayPanel.kt` |
| `SubtitleDownloader` | `providers/playback/ProviderSubtitleFetcher.kt` |
| `SubtitleEngine` | `subtitles/engine/SubtitleConversionEngine.kt` |
| `SubtitleFileItem` | `subtitles/ui/SubtitleFilePickerDialog.kt` |
| `SubtitleFileTreeDialog` | `subtitles/ui/SubtitleFilePickerDialog.kt` |
| `SubtitleFontManager` | `subtitles/fonts/SubtitleFontResolver.kt` |
| `SubtitleFormat` | `subtitles/engine/SubtitleCueModels.kt` |
| `SubtitleSelectionPanel` | `subtitles/panels/SubtitleSelectionPanel.kt` |
| `SubtitleSessionMemory` | `subtitles/store/SubtitleSessionRecall.kt` |
| `SubtitlesTab` | `settings/subtitles/SubtitleSettingsTab.kt` |
| `SubtitleStyleAccordionSection` | `subtitles/style/SubtitleStylePanelRoot.kt` |
| `SubtitleStylePanel` | `subtitles/style/SubtitleStylePanelRoot.kt` |
| `SubtitleTranslationService` | `ai/AiSubtitleTranslator.kt` |
| `SyncedLine` | `lyrics/model/LyricsDataModels.kt` |
| `SyncedWord` | `lyrics/model/LyricsDataModels.kt` |
| `SystemDynamic` | `theme/SurfaceAndTextColors.kt` |
| `SystemTone` | `settings/state/ThemeAndDisplayOptionEnums.kt` |
| `SystemTonePalette` | `theme/SystemSurfaceTonePalette.kt` |
| `SystemTonePanel` | `theme/picker/SystemTonePickerPanel.kt` |
| `TabNavigationStyle` | `settings/state/ThemeAndDisplayOptionEnums.kt` |
| `Text` | `settings/i18n/TranslatedTextComposable.kt` |
| `Text` | `settings/i18n/TranslatedTextComposable.kt` |
| `TextEditorNavStep` | `settings/state/ThemeAndDisplayOptionEnums.kt` |
| `ThemeMode` | `settings/state/ThemeAndDisplayOptionEnums.kt` |
| `ThemePreviewControls` | `settings/appearance/ThemePreviewControlsCard.kt` |
| `ThumbnailCache` | `library/thumbnails/VideoThumbnailCaches.kt` |
| `ThumbnailPrefetcher` | `library/thumbnails/VideoThumbnailPrefetch.kt` |
| `ThumbnailQuality` | `settings/state/ThemeAndDisplayOptionEnums.kt` |
| `ThumbnailStrategy` | `settings/state/ThemeAndDisplayOptionEnums.kt` |
| `TimedLine` | `lyrics/providers/LyricsTextParsers.kt` |
| `TimeNetworkClockBadge` | `player/overlay/ClockBatteryBadge.kt` |
| `TlsCaBundle` | `network/MpvTlsCaBundleInstaller.kt` |
| `TorrentCatalog` | `torrent/TorrentCatalogModels.kt` |
| `TorrentCatalogEngine` | `torrent/TorrentFileListReader.kt` |
| `TorrentCatalogs` | `torrent/TorrentFileListReader.kt` |
| `TorrentDownloadEngine` | `torrent/TorrentFileDownloader.kt` |
| `TorrentException` | `torrent/TorrentCatalogModels.kt` |
| `TorrentFileItem` | `torrent/TorrentCatalogModels.kt` |
| `TorrentMetadataEndpoints` | `torrent/TorrentTrackerEndpointReader.kt` |
| `TorrentPickerHost` | `torrent/ui/TorrentFilePickerSheet.kt` |
| `TorrentPickerRequest` | `torrent/TorrentPickerRequests.kt` |
| `TorrentPickerState` | `torrent/TorrentPickerRequests.kt` |
| `TorrentStreamer` | `torrent/TorrentLoopbackStreamServer.kt` |
| `TrackAudioConfig` | `audio/settings/AudioChannelAndEqModels.kt` |
| `TrackRef` | `lyrics/providers/LyricsSourceContract.kt` |
| `TrackTranslationStatus` | `ai/TranslationLanguageCatalog.kt` |
| `TranslateLanguages` | `ai/TranslationLanguageCatalog.kt` |
| `TtmlLyrics` | `lyrics/providers/TtmlLyricsParser.kt` |
| `TypographySubTab` | `subtitles/style/SubtitleTypographySection.kt` |
| `UiState` | `settings/state/AppUiState.kt` |
| `UnisonProvider` | `lyrics/providers/UnisonLyricsSource.kt` |
| `UniversalSubtitleEngine` | `subtitles/engine/SubtitleFormatToAssConverter.kt` |
| `UnsupportedMuxException` | `download/mux/MuxInputModels.kt` |
| `UpdateChannel` | `app/update/AppUpdateModels.kt` |
| `UpdatesSettingsSection` | `app/update/UpdateSettingsCardAndSheet.kt` |
| `UriArchiveSource` | `library/archive/ArchiveByteSources.kt` |
| `ViauraDns` | `network/DohDnsResolver.kt` |
| `ViauraExpressiveLoadingIndicator` | `glass/LiquidPullRefreshIndicator.kt` |
| `ViauraHttpClient` | `network/SharedOkHttpClient.kt` |
| `ViauraSettingsXmlManager` | `settings/SettingsXmlBackupManager.kt` |
| `ViauraSlider` | `glass/controls/ViauraSlidersAndSwitches.kt` |
| `ViauraSurfaceStyle` | `glass/GlassPanelTokens.kt` |
| `ViauraSwitch` | `glass/controls/ViauraSlidersAndSwitches.kt` |
| `VideoAudioSelectionMemory` | `audio/PerVideoAudioChoiceStore.kt` |
| `VideoChoice` | `download/probe/ProbeResultModels.kt` |
| `VideoFileManager` | `library/VideoFileOperations.kt` |
| `VideoFilterPreset` | `player/filters/VideoFilterPresets.kt` |
| `VideoFolder` | `library/scan/VideoLibraryModels.kt` |
| `VideoFolderScanner` | `library/scan/VideoFolderScanEngine.kt` |
| `VideoGlassSampler` | `glass/VideoFrameGlassBackdrop.kt` |
| `VideoGlassSource` | `glass/VideoFrameGlassBackdrop.kt` |
| `VideoItem` | `library/scan/VideoLibraryModels.kt` |
| `VideoItemCard` | `library/cards/VideoListItemCard.kt` |
| `VideoItemGridCard` | `library/cards/VideoGridItemCard.kt` |
| `VideoLibraryCache` | `library/VideoLibraryDiskCache.kt` |
| `VideoMiniPlaybackService` | `player/mini/VideoMiniForegroundService.kt` |
| `VideoMiniPlayerManager` | `player/mini/VideoMiniSessionManager.kt` |
| `VideoMiniSession` | `player/mini/VideoMiniSessionManager.kt` |
| `VideoOpeningAnimation` | `settings/state/ThemeAndDisplayOptionEnums.kt` |
| `VideoPlaybackRecord` | `library/history/VideoPlaybackRecordModel.kt` |
| `VideoPlayerMpvView` | `player/mpv/MpvSurfaceHostView.kt` |
| `VideoPlayerOverlay` | `player/overlay/VideoPlayerControlsOverlay.kt` |
| `VideoPlayerScreen` | `player/screen/VideoPlayerScreenHost.kt` |
| `VideoPlayerTheme` | `theme/ViauraAppTheme.kt` |
| `VideoScreenshotSaver` | `player/PlayerScreenshotSaver.kt` |
| `VideoSubtitleSelection` | `subtitles/store/SubtitleSelectionRecords.kt` |
| `VideoThumbnailCard` | `library/thumbnails/VideoThumbnailViews.kt` |
| `VideoThumbnailDiskCache` | `library/thumbnails/VideoThumbnailCaches.kt` |
| `VideoThumbnailLoader` | `library/thumbnails/VideoThumbnailViews.kt` |
| `ViewMode` | `library/sort/SortAndViewModels.kt` |
| `VisibleFields` | `library/sort/SortAndViewModels.kt` |
| `WebPlayback` | `network/stream/DirectStreamModels.kt` |
| `WebStreamProxy` | `network/bypass/WebStreamLoopbackProxy.kt` |
| `YouTubeMusicLyricsProvider` | `lyrics/providers/YouTubeMusicLyricsSource.kt` |
| `YouTubeTranscriptProvider` | `lyrics/providers/YouTubeCaptionLyricsSource.kt` |
| `YtdlpFetch` | `ytdlp/YtdlpHttpsFetcher.kt` |
| `YtdlpInstallationInfo` | `ytdlp/YtdlpInstallInfoModels.kt` |
| `YtdlpManager` | `ytdlp/YtdlpRuntimeInstaller.kt` |
| `YtdlPref` | `ytdlp/YtdlpPrefsStore.kt` |
| `YtdlPreferences` | `ytdlp/YtdlpPrefsStore.kt` |
| `YtdlpReleaseChannel` | `ytdlp/YtdlpInstallInfoModels.kt` |
| `YtdlpUrlRules` | `ytdlp/YtdlpLinkRouting.kt` |
| `YtdlpWorker` | `ytdlp/YtdlpPersistentWorker.kt` |
| `YtdlpWorkerUnavailable` | `ytdlp/YtdlpPersistentWorker.kt` |