# Momentic — app

Kotlin Multiplatform app (Android + iOS) for **Momentic**: people make a personalized, interactive web page for a
moment (a first birthday, a wedding invitation, a proposal, an anniversary, a letter) from a template, preview it,
sign in, pay, and share it as `momentic.ferbotz.com/<8-char code>`; the link stays live for the days bought.
Recipients open it in a browser — the pages are rendered by the web renderer in the backend repository. **This app
is the creator's tool**: browse templates, fill the generated form, preview on the web, publish, share, track
views and RSVPs. The product was called Momentica until 2026-10-23.

## House of Apps protocols

This project follows the HOA playbook in `G:\My Drive\HOA Protocols\` (on this Mac:
`~/Library/CloudStorage/GoogleDrive-techferbotz@gmail.com/My Drive/HOA Protocols/`). Read `README.md` there first.
The ones that bind this repo:

- **01 — Contract.** The only channel to the backend is `My Drive/Momentic/Momentic-Contract/`. Write only in
  `app/` (`APP-nnn` in `app/REQUESTS.md`; the `app/applied.md` watermark). Build against `backend/API.md` and
  `backend/RENDER.md`; **never open the backend repository.**
- **04 — Auth.** Sign-in returns one access token that never expires; there is no refresh. A `401` on a signed-in
  request means the token was rejected: discard it (`SessionManager`) and send the user through sign-in again.
- **10/11 — Remote config.** `GET /api/v1/config` (under `/api/v1` here, not the root). The app half is
  `RemoteConfig*` in `data/` and `presentation/config/`; the compiled-in defaults equal the backend's (pinned by
  `RemoteConfigParsingTest.productionDocumentEqualsTheCompiledInDefaults`). Fetch on cold start, on foreground after
  `ttlSeconds`, and on maintenance retry — never block launch on it.
- **12 — Legal pages.** Privacy, Terms, Delete-account and the support address come from `config.links`, never
  literals; shown in account settings, on the sign-in consent line, and on the paywall.

## Where the contract and the live backend differ (as of 2026-10-23)

- Hosts are one constant each in `ApiConfig` (BE-003): API `https://momentic.ferbotz.com/api/v1/`, web origin
  `https://momentic.ferbotz.com`. Template art and sample images are **web** paths — resolve them against
  `config.web.origin`, never the API base (BE-002).
- `API.md` still describes refresh tokens; the live backend has none (`/auth/refresh` is 404). The backend's own
  docs mirrored in `My Drive/Momentic/` (README, CLAUDE, `docs/REMOTE_CONFIG.md`) are current, and are what the
  auth and config code follow.
- Response shapes the contract leaves open are read leniently: the stats `series`/`rsvp` objects, the RSVP list's
  item and summary names, and creations returned bare or as `{ creation }`. `GET /creations/:id/entitlements` and
  `GET /me/purchases` are not used — their shapes aren't documented.

## Stack

Kotlin 2.4.20 · Compose Multiplatform 1.12.1 · AGP 9.1.1 (built-in Kotlin; `com.android.kotlin.multiplatform.library`)
· Ktor 3.5 (OkHttp / Darwin) · kotlinx.serialization 1.11 · coroutines 1.11 · kotlinx-datetime 0.7.1 ·
multiplatform-settings 1.3 · Koin 4.2.1 (core only, plus the `koinViewModel()` helper) · Kermit 2.1 · Coil 3.5 ·
JetBrains navigation-compose 2.9.2. Versions follow the house's other KMP apps (Curiously, AuraPix).

## Commands

```bash
./gradlew :androidApp:assembleDebug          # Android app
./gradlew :shared:testAndroidHostTest        # every commonTest suite, on the JVM
./gradlew :shared:compileKotlinIosArm64      # iOS compile check
```

## Layout

```
shared/src/commonMain/kotlin/com/ferbotz/momentic/
  data/network/        ApiConfig (origins, headers, every path), HttpClientFactory, unwrap(), dto/, datasource/
  data/result/         AppResult, AppError + ErrorCode, runApiCatching
  data/local/          AppPreferences: install id, draft token, access token, config cache, pending purchases
  data/model/, mapper/ domain models; lenient wire → domain
  data/repository/     one repository per concern; PrincipalGuard (draft token), CreationEvents (app-wide bus)
  data/session/        SessionManager — the one owner of signed-in state
  data/billing/        StoreBilling — RevenueCat goes here; UnavailableStoreBilling until the keys exist
  data/media/          ImageProcessor (expect/actual): upright, crop to aspectRatio, ≤ 2048 px, JPEG under the limit
  data/di/             Koin data modules, initKoin()
  presentation/        one view model (+ UiState, events) per screen — see docs/VIEWMODELS.md
  presentation/editor/ FormEngine: field contract ⇄ form values ⇄ the creation's `data`
  ui/, theme/, App.kt  placeholder UI (feed, template, gate screens) — to be replaced by the real UI
shared/src/androidMain, iosMain   HTTP engines, build info, image processor, link opener
```

## Conventions

- Repositories return `AppResult`, never throw. Branch on `ErrorCode`, never on the message.
- Every request carries `X-Device-Id`, `X-App-Platform/Version/Build`, and exactly one principal: the bearer token
  when signed in, otherwise `X-Draft-Token`. Creating is anonymous; only publish needs a user.
- Wire DTOs are all-nullable; mappers default or drop what they can't use — one bad entry never fails a screen.
- The form is generated from the template's `fields`; never hard-code a template's inputs. Optional empty fields
  are omitted, never `null`. Photos are written as `{ "imageId" }`, uploaded as picked, cropped first.
- Publishing: the purchase is recorded as pending **before** the call and retried with the same transaction; it is
  cleared only on success or a refusal no retry can fix. Never charge twice.
- New logic gets a commonTest fixture; the suites are the acceptance criteria.

## Gotchas

- **Intel Mac.** No iOS simulator here: Compose Multiplatform 1.12 publishes no `iosX64`, and
  `iosSimulatorArm64Test` can't run on an x86_64 host. iOS runs on a physical iPhone (set `TEAM_ID` in
  `iosApp/Configuration/Config.xcconfig`). Kotlin/Native warns the `macos_x64` host is deprecated.
- `runTest` + the production client's `HttpTimeout`: virtual time fires the timeout at once — run client calls on
  a real dispatcher (see `BuildHeadersTest.runClientTest`).
- Not built yet: the real UI, the platform Google sign-in (needs the backend's web OAuth client id), and the
  RevenueCat `StoreBilling` (needs public SDK keys and the Swift package in Xcode).
