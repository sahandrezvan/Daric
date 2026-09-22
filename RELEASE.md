# Release build (Daric 1.1.0)

## Build locally

```bash
export ANDROID_HOME=...
export KEYSTORE_PATH=/path/to/my-upload-key.jks
export STORE_PASSWORD=...
export KEY_PASSWORD=...
./gradlew :app:assembleRelease :app:bundleRelease
```

Outputs:
- `app/build/outputs/apk/release/app-release.apk`
- `app/build/outputs/bundle/release/app-release.aab`

## Version

- versionName: `1.1.0`
- versionCode: `2`
- applicationId: `com.aistudio.daricfinance.app`
