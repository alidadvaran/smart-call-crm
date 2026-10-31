# ساخت APK

سه راه برای ساخت APK وجود دارد. هر سه از یک سورس یکسان استفاده می‌کنند.

## پیش‌نیاز مشترک
- JDK 17 (یا 21)
- Android SDK با پلتفرم `android-34` و build-tools؛ متغیر `ANDROID_HOME` تنظیم شده باشد.

> نکته‌ی مهم درباره‌ی شبکه: تمام آرتیفکت‌های اندروید (AGP، AndroidX، Compose، aapt2) از دامنه‌ی `dl.google.com` دانلود می‌شوند. اگر در محیطی می‌سازید که خروجی شبکه محدود است، باید `dl.google.com` (و `maven.google.com`) مجاز باشند.

---

## راه ۱ — ماشین خودتان (ساده‌ترین)
1. پروژه را در **Android Studio** (نسخه‌ی جدید) باز کنید؛ Gradle Sync را بزنید.
2. برای APK دیباگ:
   ```bash
   ./gradlew :app:assembleDebug
   ```
   خروجی: `app/build/outputs/apk/debug/app-debug.apk`
3. برای APK ریلیز امضاشده، ابتدا کلید بسازید (یک‌بار):
   ```bash
   bash scripts/make-keystore.sh
   ```
   سپس:
   ```bash
   ./gradlew :app:assembleRelease
   ```
   خروجی: `app/build/outputs/apk/release/app-release.apk`

آدرس سرور را می‌توانید هنگام ساخت تزریق کنید:
```bash
./gradlew :app:assembleDebug -PserverUrl="https://crm.example.com/"
```
یا بعداً در خودِ برنامه از بخش **تنظیمات** تغییر دهید (مقدار ذخیره‌شده در زمان اجرا اولویت دارد).

## راه ۲ — GitHub Actions (بدون نصب محلی)
1. پروژه را به یک ریپازیتوری گیت‌هاب push کنید.
2. workflow موجود در `.github/workflows/android.yml` به‌صورت خودکار اجرا می‌شود (یا از تب Actions دستی Run کنید و `serverUrl` را بدهید).
3. پس از پایان، از همان اجرا فایل **smartcall-debug-apk** را دانلود کنید.

رانرهای گیت‌هاب به `dl.google.com` دسترسی دارند و SDK را خودکار نصب می‌کنند.

## راه ۳ — همین محیط ابری (پس از آزادسازی دامنه)
اگر از Claude Code روی وب استفاده می‌کنید و `dl.google.com` را در تنظیمات شبکه‌ی محیط (Network access → Allowed domains) اضافه کنید، همان دستور `./gradlew :app:assembleDebug` اینجا هم کار خواهد کرد.

---

## امضای APK و به‌روزرسانی
- فایل `keystore.properties` و فایل `.jks` را **گم نکنید**؛ برای ارائه‌ی به‌روزرسانی روی همان نصب، باید با همان کلید امضا شوند (در غیر این صورت اندروید اجازه‌ی نصب روی نسخه‌ی قبلی را نمی‌دهد و داده‌ها پاک می‌شوند).
- جزئیات به‌روزرسانی بدون حذف داده در `docs/UPDATE.md`.
