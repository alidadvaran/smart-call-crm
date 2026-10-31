# به‌روزرسانی خودکار اپ (Self-Update)

اپ خودش به بک‌اند شما سر می‌زند و اگر نسخه‌ی جدیدتری باشد، آن را دانلود و نصب می‌کند.

## چطور کار می‌کند
1. هر بار که برنامه باز می‌شود و در هر اجرای دوره‌ی پس‌زمینه (هر ~۱۵ دقیقه)، اپ `GET /api/app/latest` را می‌زند.
2. اگر `versionCode` سرور از نسخه‌ی نصب‌شده بزرگ‌تر باشد، APK را از `GET /api/app/download` می‌گیرد.
3. نصب با `PackageInstaller`:
   - اگر اپ **Device Owner** باشد → نصب **کاملاً بی‌صدا** و بدون هیچ تأییدی.
   - در غیر این صورت → سیستم‌عامل یک بار دکمه‌ی «Update» نشان می‌دهد (محدودیت اندروید برای اپ غیرسیستمی).

> نکته‌ی مهم: نسخه‌ی نصب‌شده و نسخه‌ی جدید باید **applicationId و امضای یکسان** داشته باشند. بنابراین باید همیشه نسخه‌ی **release امضاشده با یک keystore ثابت** را توزیع کنید (نه debug).

## گام ۱ — یک‌بار: ساخت keystore ثابت
روی رایانه‌ی خودتان:
```bash
bash scripts/make-keystore.sh         # release.jks + keystore.properties می‌سازد
base64 -w0 release.jks                 # خروجی را برای راز CI کپی کنید
```

## گام ۲ — تنظیم رازهای CI (برای انتشار خودکار)
در گیت‌هاب: Settings → Secrets and variables → Actions → New repository secret، این‌ها را بسازید:

| راز | مقدار |
|-----|-------|
| `KEYSTORE_BASE64` | خروجی `base64 -w0 release.jks` |
| `KEYSTORE_PASSWORD` | رمز keystore |
| `KEY_ALIAS` | `smartcall` (یا alias شما) |
| `KEY_PASSWORD` | رمز کلید |
| `SMARTCALL_UPDATE_URL` | `https://<دامنه‌ی شما>/api/app/upload` |
| `SMARTCALL_UPLOAD_TOKEN` | همان `APP_UPLOAD_TOKEN` در `.env` سرور |
| `SMARTCALL_SERVER_URL` | `https://<دامنه‌ی شما>/` |

از این پس، **هر push به `main`** → CI نسخه‌ی release را می‌سازد، امضا می‌کند و خودکار روی بک‌اند می‌گذارد. گوشی‌ها در بررسی بعدی آپدیت می‌شوند.

> برای هر انتشار، `versionCode` را در `app/build.gradle.kts` افزایش دهید وگرنه گوشی آن را «جدیدتر» نمی‌بیند.

## گام ۳ — نصب اولیه
نسخه‌ی release را یک‌بار دستی روی گوشی‌ها نصب کنید:
- از artifact با نام **smartcall-release-apk** در صفحه‌ی همان اجرای CI، یا
- فایلی که با `./gradlew :app:assembleRelease` روی رایانه‌ی خودتان ساختید.

## گام ۴ (اختیاری، پیشرفته) — نصب کاملاً بی‌صدا با Device Owner
برای حذف همان یک تأیید سیستمی، اپ را Device Owner کنید. شرط‌ها: گوشی **تازه ریست‌شده** و **بدون هیچ حساب گوگلِ افزوده‌شده**.
```bash
# ۱) release را نصب کنید (بدون افزودن حساب گوگل)
adb install app-release.apk
# ۲) اپ را device owner کنید
adb shell dpm set-device-owner ir.smartcallcrm/.admin.DeviceOwnerReceiver
```
از این پس آپدیت‌ها بدون هیچ دکمه‌ای نصب می‌شوند و اپ تا زمانی که device-owner فعال است قابل حذف نیست.

برداشتن device owner بعداً:
```bash
adb shell dpm remove-active-admin ir.smartcallcrm/.admin.DeviceOwnerReceiver
```

## اگر Device Owner نگذارید
همه‌چیز خودکار است (بررسی + دانلود)، فقط در لحظه‌ی نصب یک بار باید «Update» را بزنید. یک‌بار هم باید «نصب از منابع ناشناس» را برای این اپ Allow کنید (اندروید ۸+).
