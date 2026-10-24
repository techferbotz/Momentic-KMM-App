# Momentic — UI integration plan

Replace the placeholder UI (`ui/`, `theme/`) with the designed one, for Android and iOS, wired to the view
models in `presentation/` (contract: [`VIEWMODELS.md`](VIEWMODELS.md)).

**Source of truth for the look:** the design canvas — <https://claude.ai/artifact/VatZ35tXPVK3teSEzX3oQe>
(page *Tokens & components* for the system, page *Screens & flow* for the 25 screens and their states).
The canvas's template thumbnails and photos are placeholders: in the app, template art and photos come from
the server through Coil.

## Ground rules

- Bind to the existing view models; change them only additively, where the design needs something they don't
  expose (listed below). Every view-model change gets a `commonTest` and a line in `VIEWMODELS.md`.
- Light theme only. The current dark colour scheme goes.
- Copy lives in `composeResources/values/strings.xml` (with plurals for "1 day left" / "5 days left"). Legal
  links and the support address always come from `config.links`.
- Every screen is a stateless `XScreen(state, onIntent…)` plus an `XRoute` that gets the view model, collects
  events and navigates. `@Preview`s are built from fake states, never from a running VM.
- Accessibility as drawn: 48 dp touch targets, `contentDescription` on icon-only buttons, every status
  shown as an icon + words, layouts that survive 200 % font scale, reduced motion respected.

## Design tokens (from the canvas)

| Token | Value | Use |
| --- | --- | --- |
| background / surface | `#FDF4F5` / `#FDF8F1` | app background / cards, sheets, inputs |
| blush-100 / blush-200 | `#F6DFE2` / `#EFC9D2` | soft fills, selected chips / gradients, tracks |
| petal / petal-pressed | `#F2AFBC` / `#E69CAB` | primary buttons, with an **ink** label (8.3:1) |
| rose / rose-deep | `#B84D66` / `#8A2F47` | links, text buttons, active tab, focus, selection / badges, emphasis |
| rose-soft / error-soft | `#D68D9A` / `#DC8B88` | outlined and destructive-outlined buttons, add-photo slots |
| ink / ink-soft / line | `#2F2521` / `#6D5C53` / `#EAD7DB` | text / secondary text / borders |
| success · warning · error | `#2F6B52` · `#7A5214` · `#A3242E` | with chip fills `#E4F0E9` · `#FBEFD9` · `#FBE9EA`; "Ended" uses `#EFE8E4` |

Occasion tints are keyed by category id (birthday honey, wedding slate, love rose, anniversary lavender,
friendship sage, greeting blush — values on the canvas); an unknown id falls back to blush.
Type: Lora for Display 32/40, H1 26/32, H2 20/28 and card titles 16/24; Plus Jakarta Sans for body 16/24,
button 16/24 semibold, label 14/20 semibold and caption 12/16. Radii 20 (cards, sheets), 14 (inputs), full
(pills; buttons 52 tall). Shadows are rose-tinted: *Rest* (cards, buttons), *Raised* (hero), *Sheet*.
8-point grid, 20 dp gutters. Motion 200–300 ms ease-out; falling petals only on "Your moment is live".

## Screens → routes → view models

| # | Screen | Lives in | View model | What's missing for the design |
| --- | --- | --- | --- | --- |
| 01 | Splash | native splash (Android SplashScreen API, iOS launch screen) | — | Compose can't own the cold-start frame |
| 02 | Welcome | `WelcomeRoute`, first launch only | — | a `hasSeenWelcome` preference |
| 03 | Maintenance | gate | `RemoteConfigViewModel` | design has no button → retry on a timer |
| 04 | Update required · banner | gate · top of Explore | `RemoteConfigViewModel` | the nudge becomes a banner, not a dialog |
| 05 | Explore | tab `ExploreRoute` | `FeedViewModel` | no "featured" template; cards carry `tierKey`, not the tier label |
| 06 | Category | `CategoryRoute(categoryId)` | `FeedViewModel`, scoped to the Explore graph | no per-category endpoint — reuse the feed |
| 07 | Template detail | `TemplateRoute` | `TemplateDetailViewModel` | "What you'll add" summarised from `fields` (UI helper) |
| 08 | Live example | `WebPageRoute(url)` | — | an embedded web view (Custom Tabs can't host our chrome) |
| 09 | Personalise | `EditorRoute` | `EditorViewModel` | autosave + "Saved" state; colour presets (colour fields have no options) |
| 10 | Add photo · crop | overlay inside the editor | `EditorViewModel` | picker; user-chosen crop; upload % (optional) |
| 11 | Moment item sheet | sheet inside the editor | `EditorViewModel` (group paths) | — |
| 12 | Preview | `WebPageRoute(url)` + Edit / Publish | `EditorViewModel` (`OpenPreview`) | "Send a preview link" needs a share intent |
| 13 | Sign in to publish | sheet | `SignInViewModel` | platform Google sign-in (needs the web client id) |
| 14 | Choose how long | `PublishRoute(creationId)` | `PublishViewModel` | per-day prices need a numeric store price; "Most chosen" rule; the not-available state |
| 15 | Publishing · couldn't finish | `PublishRoute` stages | `PublishViewModel` | — |
| 16 | Your moment is live · QR | `PublishedRoute(creationId)` | `PublishViewModel.result` | the link-preview title/image (render endpoint's `og`); QR + save image |
| 17 | My Moments | tab `MomentsRoute` | `MomentsViewModel` | creations have **no title** and only a `coverImageId` |
| 18 · 21 | Moment detail · ended | `MomentRoute(creationId)` | `MomentDetailViewModel` | — |
| 19 | RSVPs | `RsvpsRoute(creationId)` | `RsvpsViewModel` | the yes / maybe / no filter is client-side |
| 20 | Extend | sheet over 18 | `PublishViewModel` (renew) | — |
| 22 | Delete moment | dialog over 18 | `MomentDetailViewModel.delete()` | — |
| 23 | Settings, signed in / out | tab `SettingsRoute` | `AccountViewModel` + `SignInViewModel` | — |
| 24 | Purchases | `PurchasesRoute` | **none** | `GET /me/purchases` shape isn't documented |
| 25 | Delete account · confirm | `DeleteAccountRoute` + dialog | `AccountViewModel` | — |

The bottom bar shows on the three tab roots and Category; pushed screens hide it. Sheets and dialogs are
screen state (`ModalBottomSheet`, `AlertDialog`), not routes. After `Published`, the back stack pops to the
tab so Back from "Your moment is live" lands on My Moments.

## Phases

Each phase ends with something that runs on Android (emulator) and iPhone (device — no simulator on this Mac).

**0 · Foundation** *(M)* — fonts in `composeResources/font` (Lora, Plus Jakarta Sans; OFL), `MomenticTheme`
with the tokens as `CompositionLocal`s mapped onto the Material 3 scheme, typography, shapes, spacing,
rose-tinted shadows, ~45 line icons as `ImageVector`s copied from the canvas paths, edge-to-edge insets.
Exit: a debug gallery screen showing the tokens.

**1 · Components** *(L)* — buttons (primary, secondary, text, destructive, icon × default / pressed /
disabled / loading), occasion chip, tier badge, status chip, tag, segmented control, bottom bar, top bars
(with the Saving / Saved / Offline indicator), template card (carousel + grid), hero card, moment card, sheet,
dialog, snackbar, inline banners, empty states, skeletons, progress ring, views bar chart (Canvas, tap for a
tooltip), petals. Exit: the gallery matches the *Components* and *Sheets, dialogs, messages* boards.

*Phase 0–1 status (24 Oct):* built under `theme/` and `designsystem/`. The gallery opens from the debug-only
"Momentic gallery" icon on Android, or with the `-designGallery` launch argument on iOS. Checked on the
emulator; the iPhone run waits for `TEAM_ID`. Where the app departs from the canvas, on purpose: the chart's
tooltip sizes to its text and floats above the tallest bar under it, with a stem down to the day, and the peak
label breaks the gridline (the canvas's fixed tooltip covered taller neighbours and would cut longer dates); a
long button label wraps to a second line instead of being cut; the dialog dims with the scrim token (black at
40 % on Android, where the window does the dimming).

**2 · Field kit** *(M)* — one composable per `FieldType` (text, long text, number stepper, toggle row, date,
date-time, colour swatches, choice chips, location, photo, photo list, group list) plus a `FieldRenderer`
that walks `template.fields` and binds to `EditorUiState` (`valueOf` / `errorFor` / `exampleFor`). Each field
has empty / filled / focus / error states. Exit: matches the *Field kit* board.

**3 · Navigation shell** *(S)* — routes for the table above, a tab scaffold, the gate, Welcome, a snackbar
host for every `message`, event collection per route. Exit: every route reachable with stub screens.

**4 · Platform pieces** *(M — can run in parallel with 5)* — expect/actual for: embedded web view (Android
`WebView`, iOS `WKWebView`); photo picker (`PickVisualMedia` / `PHPickerViewController`); crop screen
(pan + zoom under a locked frame); share sheet plus WhatsApp / Messages targets with a system-sheet
fallback; QR code (encode in common code) with save-to-photos; reduced-motion flag. Blocked until you supply
them: Google sign-in (web client id) and RevenueCat (`StoreBilling`, public keys + the Swift package).

**5 · Screens, flow by flow** *(L)* — Explore → Create → Publish → My Moments → Settings, each screen with its
loading, empty, error and success states from the canvas.

**6 · Polish and QA** *(M)* — motion, an accessibility pass (TalkBack, VoiceOver, 200 % text), strings and
plurals, delete the placeholder UI and old theme, update `CLAUDE.md` and `VIEWMODELS.md`.

## View-model and data additions (small, additive, each tested)

1. `EditorViewModel` autosave: debounced `save()` after edits, and a save state for the top-bar indicator.
2. `ImageProcessor.prepare` / `addPhotos`: an optional crop rectangle from the crop screen (centre crop stays
   the default). Optional: upload progress on `PhotoSlot.Uploading`.
3. `AppPreferences.hasSeenWelcome`.
4. A "send preview link" intent on the editor that emits `Share(url)`.
5. Surface `limits.maxDrafts` / `maxImages` as the "up to 20 drafts" and "60 photos" messages.
6. `StoreProduct`: amount + currency next to `formattedPrice`, for "₹86 a day".
7. Interim titles and covers until the backend sends them: title = the template name plus the first short
   text value; cover = the template's art.

## Contract requests (protocol 01 — `app/REQUESTS.md`, APP-001 onwards)

- A creation **title** (the renderer already receives it as `og.title`) on creation responses.
- A **cover thumbnail URL** on creations, beside `coverImageId`.
- The **`GET /me/purchases`** response shape, for the Purchases screen.
- Optional: a **featured** template in the feed and a **recommended** duration ("Most chosen").

## Decisions

| Question | Recommendation |
| --- | --- |
| Fonts | Download Lora + Plus Jakarta Sans from `google/fonts` (OFL) into `composeResources` |
| Location field | Address field only for now; a map preview needs a maps SDK and key |
| Drag to reorder (photos, moments) | A small KMP reorder library rather than hand-written drag |
| QR code | A small KMP QR library, or our own encoder if you'd rather avoid a dependency |
| Colour fields | App-side preset swatches + custom hex until the field contract carries options |
| Featured / Most chosen | First template of the first section; 7-day bundle — until the backend says |

Work happens on a new branch (`feature/ui`) off `feature/data-layer`; nothing is committed without asking.
