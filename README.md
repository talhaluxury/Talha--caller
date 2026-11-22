# TALHA CALLER ID (TALHA STUDIO)

Android app for internet-based calls to Pakistani mobile numbers (Jazz, Zong, Ufone, Telenor) through a licensed telecom provider, using only caller IDs the provider has verified for the user. Pearl-white liquid-glass design, no blue.

## What is in this repo
| Path | What |
|---|---|
| `app/` | Android app: Kotlin, Compose, Material 3, MVVM, Coroutines/Flow, Room, Retrofit |
| `backend/` | Node.js backend (no dependencies): auth, caller ID authorization, rate limits, security log, provider adapter |
| `.github/workflows/` | `android.yml` builds a debug APK and runs tests; `release.yml` builds a signed release APK |

## What works today, and what needs a provider
| Feature | Status |
|---|---|
| UI, navigation, dial pad, +92 handling, number validation and E.164 normalization | Works |
| Sign-in, verified numbers, caller name, call flow, status polling, call history (Room) | Works against the backend in **test mode** |
| Test mode (`PROVIDER=mock`) | Simulation only. Verification code is `123456`. The app labels these calls "TEST MODE". No real call is ever placed. |
| Real calls to Pakistani mobiles, real caller ID verification, real caller name approval | **Needs a real provider adapter**, provider credentials, account approval and payment. Not included, because no provider documentation was supplied. |
| Without a backend URL | Every action shows "Calling service is not configured yet." |

## Costs
- Free: building the app, GitHub Actions for a normal-size repo, running the backend locally, test mode.
- Free only within provider limits: any provider trial credit.
- Paid: call termination to Pakistani mobiles, provider-issued numbers, verification and caller name fees.
These are internet calls billed by the provider. They do not use Jazz, Zong, Ufone or Telenor SIM balance.

## Run it (test mode)
1. Backend:
   ```
   cd backend
   export JWT_SECRET=$(openssl rand -hex 32)
   export PROVIDER=mock
   npm start
   ```
2. Build the app against it. For the Android emulator:
   `gradle assembleDebug -PbackendUrl=http://10.0.2.2:8080/`
   (Android Studio: add `backendUrl=http://10.0.2.2:8080/` to `~/.gradle/gradle.properties`.) Debug builds allow http; release builds need https.
3. Create an account, add a number (for example `+442071838750`), verify with `123456`, then call `03001234567`. Numbers ending `0000` simulate a failed call.

Gradle wrapper is not committed. Android Studio and the workflows provide Gradle 8.9; run `gradle wrapper` once if you want a wrapper.

## Tests
- `cd backend && npm test`: number validation, auth, caller ID authorization (unverified and other users' IDs refused), call status progression, failed calls, caller name, no-provider message, rate limiting, private call records.
- `gradle testDebugUnitTest`: Pakistani number validation, operator detection, call state mapping.
- Not included: on-device Room tests and live provider connection tests. They need an emulator and real credentials.

## GitHub
```
git init && git add . && git commit -m "TALHA CALLER ID"
git branch -M main && git remote add origin <your repo> && git push -u origin main
```
Open Actions, run "Build debug APK", then download the APK from Artifacts.

## Release signing
1. Create a keystore (keep it private, back it up, never commit it):
   `keytool -genkeypair -v -keystore release.jks -alias talha -keyalg RSA -keysize 2048 -validity 10000`
2. Local build: create `keystore.properties` in the repo root (ignored by git):
   ```
   storeFile=release.jks
   storePassword=...
   keyAlias=talha
   keyPassword=...
   ```
   Then `gradle assembleRelease -PbackendUrl=https://your-backend/`.
3. GitHub: add secrets `KEYSTORE_BASE64` (`base64 -w0 release.jks`), `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`, `BACKEND_URL`, then run "Build signed release APK".
Without `keystore.properties` the release build is unsigned and will not install.

## Adding a real provider
See `backend/README.md`. Before going live get in writing from the provider: Pakistan mobile termination, authorized outbound caller ID rules, caller name support, pricing, API documentation and regulatory compliance (PTA). Keep every key on the backend.

## Security rules built in
- No arbitrary caller ID: a call only goes out if the caller ID belongs to the signed-in user and the provider confirmed it. Everything else is refused and logged.
- Provider keys live only on the backend. The APK contains only the backend URL.
- Rate limits on requests, sign-in and calls. Security events are logged with masked numbers.
- The app never promises the receiver sees a caller name.

## Known limits
- The sign-in token is in app-private storage. For stricter protection use EncryptedSharedPreferences.
- The backend stores data in a JSON file for simplicity. Use a real database and HTTPS before production.
- Privacy policy and terms in the app are short drafts. Have them reviewed before publishing.
- I couldn't compile the Android project where it was written. Run the first GitHub build and fix anything the compiler reports.
