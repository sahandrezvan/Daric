# Release build (Daric 1.4.0)

## Version

- versionName: `1.4.0`
- versionCode: `5`
- applicationId: `ir.sahand.daric`

## قابلیت‌های جدید

- بودجه ماهانه بدون ثبت تکراری برای هر دسته
- تولید خودکار تراکنش‌های تکرارشونده در پس‌زمینه
- اعلان اقساط معوق و سررسیدهای سه روز آینده
- انتخاب بخش‌های قابل نمایش در داشبورد
- جست‌وجو با فیلتر نوع تراکنش و بازه زمانی
- گزارش‌های مالی و خروجی CSV
- ویجت موجودی، درآمد و هزینه ماه جاری
- جلوگیری از اسکرین‌شات و کنترل صحت SHA-256 فایل پشتیبان

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
