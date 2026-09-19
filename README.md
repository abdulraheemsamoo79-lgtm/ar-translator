# AR Translator

Android translator app under the AR PRIME brand. Kotlin + Jetpack Compose, ML Kit on-device translation, Room history, and a backend that powers in-app update prompts.

## Features
- 59 languages, powered by Google ML Kit (on-device)
- **Offline translation**: connect once to download a language pack, then translate with no internet, forever
- Auto language detection
- Full history — search, favorite, delete, clear
- Text-to-speech on any translation
- Copy / share results
- Swap languages with one tap
- In-app update prompts — see "Shipping updates" below

## Project layout
```
app/        Android app (Kotlin, Jetpack Compose)
backend/    Node.js/Fastify version-check API (deploy to Railway)
```

## Building the app
1. Open the `TranslatorApp` folder in Android Studio (Koala or newer).
2. Let Gradle sync — it will pull ML Kit, Room, Compose, etc.
3. Run on a device/emulator, or **Build > Generate Signed Bundle / APK** for a release APK.

## Deploying the backend
```
cd backend
npm install
npm start        # test locally on http://localhost:3000
```
Push this folder to Railway (same flow as your other Node projects — connect the repo, Railway auto-detects `npm start`). Set an environment variable `ADMIN_KEY` to something secret.

Once deployed, open `app/src/main/java/com/arprime/translator/util/UpdateChecker.kt` and set `VERSION_ENDPOINT` to your Railway URL + `/version`.

## Shipping an update (the part you asked about)
Every time you change something worth pushing to users:

1. In `app/build.gradle.kts`, bump `versionCode` (+1) and `versionName`.
2. Build the new release APK and upload it to wherever you host downloads — your download page (see below), GitHub Releases, or Railway's own static hosting.
3. Tell the backend about it:
   ```
   curl -X POST https://your-backend.up.railway.app/admin/version \
     -H "Content-Type: application/json" \
     -H "x-admin-key: your-secret" \
     -d '{
       "versionCode": 2,
       "versionName": "1.1.0",
       "downloadUrl": "https://your-download-page.com/AR-Translator-1.1.0.apk",
       "changelog": "Added camera translation and 5 new languages.",
       "forceUpdate": false
     }'
   ```
4. Every phone with the app open will now see an "Update available" dialog next time they launch it, with your changelog and a button that takes them straight to the download link. Set `forceUpdate: true` for a critical fix — then the dialog can't be dismissed until they update.

No Play Store review needed — this works for APKs distributed directly from your own download page.

## Download page
`download-page/index.html` is the public page your users land on to get the APK. Publish it wherever you like (Railway static site, Vercel, GitHub Pages) and put its live URL into `downloadUrl` above once you have a real APK link.
