# Momentic — the presentation contract

What the UI builds against: one view model per screen, each exposing an immutable `uiState: StateFlow<…UiState>`,
intent functions, and (where a screen must act once — navigate, open a page, share) an `events: Flow<…Event>`.
All of it lives in `shared/src/commonMain/kotlin/com/ferbotz/momentic/presentation/`.

## Getting a view model

```kotlin
val vm: EditorViewModel = koinViewModel { parametersOf(templateId, creationId) }   // creationId may be null
val state by vm.uiState.collectAsStateWithLifecycle()
LaunchedEffect(vm) { vm.events.collect { event -> /* navigate, open, share */ } }
```

`koinViewModel()` / `koinGet()` are in `presentation/di/PresentationModule.kt`. A view model is scoped to its
NavHost back-stack entry; `koinViewModel(owner)` scopes it to another entry instead — Explore and Category share
one `FeedViewModel` through the Explore graph's entry. `koinViewModel(key = …)` keeps two of one class apart in
the same entry (the Extend sheet's paywall starts afresh each time it opens). Parameters are listed per screen below.

Conventions every screen shares:

- `message: String?` — a one-off note for a snackbar. Show it, then call `messageShown()`.
- `error` / `loadError: String?` — a full-screen failure (nothing to show); pair it with the screen's retry intent.
- Every user-facing string from a failure is already written (`presentation/util/ErrorText.kt`).
- Remote config is available everywhere below the gate as `LocalRemoteConfig.current` (feature switches, limits,
  legal links, web origin) — view models already apply it, so the UI rarely needs it directly.

## The flow

```
Root ── RemoteConfigViewModel (gate) + AppSessionViewModel (sign-out notice)
  Feed ─ FeedViewModel
    └─ Template ─ TemplateDetailViewModel(templateId)          Preview → web page · Use → Editor
         └─ Editor ─ EditorViewModel(templateId, creationId?)  Preview → web page · Continue → Publish
              └─ Publish ─ PublishViewModel(creationId)        needs sign-in → SignIn, then purchase()
                   └─ SignIn ─ SignInViewModel
  My moments ─ MomentsViewModel
    └─ Moment ─ MomentDetailViewModel(creationId)              Edit → Editor · Publish/Renew → Publish
         └─ RSVPs ─ RsvpsViewModel(creationId)
  Account ─ AccountViewModel                                   legal pages · support · sign out · delete
```

Creating is anonymous: nothing asks for an account until **Publish**.

---

## Root

### `RemoteConfigViewModel` — the gate (HOA protocol 11)
Wrap the whole UI. Already wired in `App.kt` (`RemoteConfigGate`); keep it when replacing the UI.

| State | Meaning |
| --- | --- |
| `config` | The live document — provide it as `LocalRemoteConfig` |
| `isUnderMaintenance`, `maintenanceMessage` | Replace the app with a maintenance screen + "Try again" (`retry()`) |
| `isUpdateRequired`, `updateMessage`, `storeUrl` | Replace the app with "update to continue"; button only when `storeUrl != null` |
| `showUpdateNudge` | A dismissible dialog over the app; `dismissUpdateNudge()` |

Call `onAppForegrounded()` on every return to the foreground (`LifecycleStartEffect`).

### `AppSessionViewModel`
`session: StateFlow<SessionState>` (`SignedOut` / `SignedIn(user)`), and `notices: Flow<String>` — show as a
snackbar ("You've been signed out…" after a rejected token). Finishes unconfirmed paid purchases on launch.

### First launch
No view model: `AppPreferences.hasSeenWelcome` / `markWelcomeSeen()` (per device, kept on sign-out) decide
whether the welcome shows before the app (`feature/gate/WelcomeGate`).

---

## Browse

### `FeedViewModel`
State: `sections: List<FeedSection>` (category + `TemplateCard`s), `isLoading`, `isRefreshing`, `error`,
`isOffline` (that first load failed for want of a connection — Explore shows its offline state and calls
`refresh()` every 15 s), `message`.
Intents: `refresh()` (pull-to-refresh), `retry()`, `messageShown()`.
A card's `thumbnailUrl` is absolute (or null — five templates have no art yet; show a placeholder).
`displayPerDay` is the backend's price copy ("from Rs.100/day"): show it as is.

### `TemplateDetailViewModel` — param `templateId`
State: `template: TemplateDetail?`, `isLoading`, `error`. Intent: `retry()`.
`TemplateDetail` has the description, `previewImageUrls`, `tags`, `supportsRsvp`, `pricing` (label, bundles of
days), `fields` (what the form will ask — worth summarising), and `canBeFilledIn` (false ⇒ "update the app to use
this template").
"What you'll add" is `presentation/template/whatYoullAdd(fields)`: one line per top-level field, by kind (a name,
photos, words, a note, a date, a place, a choice), with a list's min–max; switches are left out.
**See a live example** opens `resolveWebUrl(config.web.origin, template.previewCode)` in the app's own web
frame (`WebPageRoute`, built on `platform/WebPage`). Only `greeting_open_letter` has a page on the web so far;
the others show the renderer's branded "not available" page (contract changelog, 2026-10-09).

---

## Create

### `EditorViewModel` — params `templateId`, `creationId?`
The form is **generated from `template.fields`** — never hard-code a template's inputs.

State: `template`, `creationId` (null until first saved), `values`, `errors`, `examples`, `isSaving`,
`isOpeningPreview`, `hasUnsavedChanges`, `uploadsInFlight`, `autosave` (`Idle` / `Saving` / `Saved` / `Offline` /
`NotSaved` — the top bar's note), `confirmLeave`, `isLoading`, `loadError`, `message`.
Helpers: `state.valueOf(path)`, `state.errorFor(path)`, `state.exampleFor(path)` ("e.g. Divya" placeholders).

Addressing an input — `FieldPath(key)` for a top-level field, `FieldPath(groupKey, entryId, childKey)` for a child
of one group entry (`entryId` = `GroupEntry.id`).

One input per field, by `FieldDefinition.type`; write values with `update(path, value)`:

| `type` | Value class | Input |
| --- | --- | --- |
| `TEXT` / `LONG_TEXT` | `FieldValue.Text(text)` | text field; `maxLength` counter; `placeholder` / example |
| `NUMBER` | `FieldValue.Number(text)` | numeric keyboard; `min`/`max` |
| `BOOLEAN` | `FieldValue.Toggle(checked)` | switch |
| `DATE` | `FieldValue.Date(LocalDate)` | date picker |
| `DATE_TIME` | `FieldValue.DateTime(LocalDateTime, offset)` | date + time picker; keep `offset` when editing |
| `COLOR` | `FieldValue.Color("#rrggbb")` | swatches / hex |
| `CHOICE` | `FieldValue.Choice(value)` | chips/radios showing `options[].label`, storing `options[].value` |
| `LOCATION` | `FieldValue.Location(address, lat?, lng?)` | address field (coordinates optional) |
| `IMAGE` | `FieldValue.Photo(slot)` | one photo — see below |
| `IMAGE_LIST` | `FieldValue.Photos(slots)` | `minItems`…`maxItems` photos; reorder with `movePhoto(path, from, to)` |
| `GROUP_LIST` | `FieldValue.Group(entries)` | repeating cards of `fields` (one level); `addEntry(key)`, `removeEntry(key, entryId)`, `moveEntry(key, from, to)` |
| `UNKNOWN` | `FieldValue.Unsupported` | not editable in this build — say so |

Show `label`, `helpText`, and "optional" when `!required`.

**Photos.** The UI's picker (`platform/rememberPhotoPicker`) reads the picked images' bytes and calls
`addPhotos(path, listOf(bytes…))`. The view model makes each upright, crops it to the field's `aspectRatio`
(centred), shrinks it and uploads it — one at a time, as picked. A field with an aspect ratio goes through the
crop screen first (`feature/editor/crop/CropFlow`), which hands back `PickedPhoto(bytes, PhotoFraming)` — the
quarter turns and the crop rectangle as fractions of the turned photo — to `addFramedPhotos(path, picked)`;
`ImageProcessor.prepare(…, framing)` applies them. Render each `PhotoSlot`:
`Uploading` (spinner), `Uploaded(thumbUrl)` (the thumbnail; may be null for an old draft), `Failed(message,
retryable)` (`retryPhoto(path, slotId)` when retryable). `removePhoto(path, slotId)` frees it on the server.

**Actions.**
- **Autosave** — every change saves quietly after `AUTOSAVE_DELAY_MS` (1.5 s) without one; the first save creates
  the draft. Malformed values wait (nothing is marked); offline, it tries again after `AUTOSAVE_RETRY_MS`; a
  refusal (say, the drafts limit) is a `message`, and autosave pauses until a save the person asked for works.
- `save()` — keeps a partial draft (only malformed values are refused, shown in `errors`).
- `preview()` — requires every required field ("needed before you preview"), saves, then emits
  `EditorEvent.OpenPreview(url)` → the Preview screen.
- `continueToPublish()` — requires every required field (errors appear in `errors`), saves, then emits
  `EditorEvent.ContinueToPublish(creationId)` → navigate to Publish.
- `close()` — Back (the bar's, and the system's through `platform/SystemBack`): saves what changed, then emits
  `EditorEvent.Closed`. When that can't be saved it sets `confirmLeave` (and marks a malformed value):
  `keepEditing()` or `leaveWithoutSaving()` (lets go of the photos no save used).
- `delete()` — deletes the draft (or discards a never-saved form) and emits `EditorEvent.Closed`; a pending
  autosave is dropped.

---

## Publish

### `PublishViewModel` — param `creationId`
For a draft this **publishes**; for a published moment it **renews** (`kind`) — same link, more time.

State: `creation`, `template` (the summary's art, name and tier), `kind`, `tierLabel`, `displayPerDay`,
`options: List<DurationOption>` (days, productId, store `price` and `perDay` — "₹86", from
`StoreProduct.amountMicros` — when known), `selectedProductId`, `isSignedIn`, `isAvailable` + `unavailableReason`,
`missingFields`, `stage` (`CHOOSING` → `PURCHASING` → `CONFIRMING` → `DONE`), `pendingPurchase`, `confirmError`
(why a paid purchase's confirmation failed — "We couldn't make your link yet" shows it), `result` (`shareUrl`,
`expiresAt`), `canPurchase`, `message`. `liveUntil(days, now, expiry, zone)` is the date each option reaches
(renewing a live link counts on from its end).

Intents: `select(productId)`, `purchase()`, `share()`, `editDraft()`, `retryLoad()`, `messageShown()`.
Events: `SignInRequired` → show sign-in, then call `purchase()` again · `Published(result)` · `Share(url, text)` →
system share sheet · `EditDraft(templateId, creationId)` → back to the editor (shown when `missingFields` isn't empty).

`pendingPurchase != null` means the user already paid and the confirmation didn't land: the button should read
"Finish publishing"; `purchase()` resumes it with the same transaction — never a second charge.

Purchases go through `StoreBilling`; until the RevenueCat keys are in the build it answers
`isAvailable = false` (and the backend answers `BILLING_UNAVAILABLE` today anyway).

### `SignInViewModel`
State: `isSigningIn`, `googleSignInEnabled` (hide the button when false), `termsUrl`, `privacyUrl`, `error`.
The UI runs the platform Google sign-in for the backend's **web** client id and reports back:
`onGoogleSignInStarted()`, `onGoogleIdToken(idToken)`, `onGoogleSignInCancelled()`, `onGoogleSignInFailed(msg)`.
Show the consent line "By continuing you agree to our Terms & Conditions and Privacy Policy" with
`openTerms()` / `openPrivacyPolicy()`. Events: `SignedIn(result)` (the device's drafts moved onto the account —
`result.claimedCreations`) · `OpenUrl(url)`.

---

## My moments

### `MomentsViewModel`
State: `creations` (newest first) with `drafts`, `live`, `ended` views; `templates` (the `TemplateCard` behind each
creation, for art and name); `canLoadMore`, `isLoadingMore`, `isLoading`, `isRefreshing`, `error`, `isSignedIn`,
`pendingPurchases`, `message`.
Intents: `refresh()`, `retry()`, `loadMore()` (at the end of the list), `delete(creationId)` (optimistic),
`finishPendingPurchases()`, `messageShown()`. Stays current by itself after saves, publishes and deletes elsewhere.
Creations have no title yet: `Creation.interimTitle()` is the first short line of text ("Divya"), and
`dayAgo(…)` writes "edited yesterday" (`presentation/moments/MomentText.kt`).

### `MomentDetailViewModel` — param `creationId`
State: `creation`, `template`, `stats` (views, `daysRemaining`, `dailyViews`, `rsvp` summary) when `showStats`,
`canPublish`, `canRenew`, `linkPreview` (a live moment's `og` title and image, read from its public page — the
GET never counts a view), `isOpening`, `isDeleting`, `error`, `statsError`, `message`.
Intents: `open()` (draft → fresh preview link; published → its link), `share()`, `edit()`, `publishOrRenew()`,
`viewRsvps()`, `delete()`, `refresh()`.
Events: `OpenUrl`, `Share`, `Edit(templateId, creationId)`, `Publish(creationId)`, `ViewRsvps(creationId)`, `Deleted`.

### `RsvpsViewModel` — param `creationId`
State: `rsvps` (guest name, `attending` YES/NO/MAYBE, `guestCount`, `message`, `contactPhone`), `summary`,
`canLoadMore`, `isLoadingMore`, `isLoading`, `error`. Intents: `refresh()`, `loadMore()`.

---

## Account

### `AccountViewModel`
State: `session` (user name/email/avatar when signed in), `links` (privacy, terms, delete-account, support email
— from remote config, never hard-coded), `appVersion`, `isSigningOut`, `isDeletingAccount`, `message`.
Intents: `openPrivacyPolicy()`, `openTerms()`, `openDeleteAccountPage()`, `contactSupport()`, `signOut()`,
`deleteAccount()` (confirm first), `refreshProfile()`.
Events: `OpenUrl(url)` · `ComposeEmail(address, subject, body)` → `rememberLinkOpener().composeEmail(…)` ·
`SignedOut` · `AccountDeleted`.

---

## Platform pieces the UI provides

| Need | Status |
| --- | --- |
| In-app browser (previews, legal pages) and `mailto:` | Ready: `rememberLinkOpener()` (Custom Tabs / `SFSafariViewController`); `openOutside(url)` hands store links to the store app |
| Embedded web page (live example, draft preview) | Ready: `platform/WebPage(rememberWebPageState(url))` (`WebView` / `WKWebView`) with loading progress, a failed state and `reload()`; `feature/web/WebFrame` puts the bars round it |
| Photo picker → `ByteArray`s | Ready: `platform/rememberPhotoPicker { bytes -> }`, `pick(max)` (the system photo picker / `PHPickerViewController`, in the order picked); `decodePhotoPreview(bytes)` for the crop screen |
| Full-screen window over everything (the crop screen) | Ready: `platform/FullScreenWindow` — edge to edge, behind clear system bars |
| Google sign-in → id token | Seam ready: `platform/rememberGoogleSignIn()` answers "unavailable" until Credential Manager + googleid (Android) and GoogleSignIn (iOS) go in; needs the backend's web client id |
| Share sheet | Ready: `platform/rememberShareSheet()` (`ACTION_SEND` chooser / `UIActivityViewController`); `shareText(text, url)`; `shareTo(ShareTarget.WhatsApp / Instagram / Messages, text)` opens that app, or the sheet when it isn't installed |
| Copy to the clipboard | Ready: `platform/rememberTextCopier()` |
| Save an image to the photo library (the QR code) | Ready: `platform/rememberImageSaver()` (MediaStore `Pictures/Momentic` / `PHPhotoLibrary`, add-only permission) |
| Reduced motion | Ready: `platform/rememberReduceMotion()` |
| Back that does something first (the form saves) | Ready: `platform/SystemBack(enabled) { … }` — the activity's handler on Android, Compose's on iOS |
| Call a guest | Ready: `LinkOpener.call(phone)` — the dialer with the number ready (`tel:`), iOS asks first |
| Store purchases | Data layer: a RevenueCat `StoreBilling` once the public SDK keys exist |
