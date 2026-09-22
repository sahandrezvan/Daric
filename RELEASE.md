# Release build (Daric 1.3.0)

## Version

- versionName: `1.3.0`
- versionCode: `4`
- applicationId: `ir.sahand.daric`

## Build locally

```bash
export ANDROID_HOME=...
export KEYSTORE_PATH=/path/to/upload.jks
export STORE_PASSWORD=...
export KEY_PASSWORD=...
export KEY_ALIAS=upload
./gradlew :app:assembleRelease :app:bundleRelease
```

Outputs:

- `app/build/outputs/apk/release/app-release.apk`
- `app/build/outputs/bundle/release/app-release.aab`
