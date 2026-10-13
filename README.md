# Calm Browser
B&W-only, ad-light, adult-blocking WebView browser.

## Build the APK
A) Android Studio: File > Open this folder > Build > Build APK(s).
   Output: app/build/outputs/apk/debug/app-debug.apk
B) No Android Studio: push this folder to a GitHub repo. The "Build APK" action runs
   automatically; download the APK from Actions > latest run > Artifacts.

## Stronger adult blocking (recommended)
Android Settings > Network > Private DNS > "Private DNS provider hostname":
    family.cloudflare-dns.com
This blocks adult domains system-wide, which the in-app keyword filter can't match.
