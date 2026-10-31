# تنظیم ایمیل گزارش (Gmail)

گزارش‌ها از **سمت سرور** و با SMTP جیمیل ارسال می‌شوند. رمز/کلید ایمیل فقط در فایل `.env` سرور قرار می‌گیرد و **هرگز داخل APK نیست**.

## ۱. ساخت App Password در گوگل
1. برای حساب فرستنده، **تأیید دو‌مرحله‌ای (2-Step Verification)** را روشن کنید:
   https://myaccount.google.com/security
2. به https://myaccount.google.com/apppasswords بروید.
3. یک App Password بسازید (نام دلخواه، مثلاً «SmartCall»). یک رشته‌ی ۱۶ حرفی می‌گیرید.

## ۲. تنظیم در `.env` سرور
```
GMAIL_USER=youraccount@gmail.com
GMAIL_APP_PASSWORD=رشته‌ی_۱۶_حرفی_بدون_فاصله
REPORT_RECIPIENT=a.dadvaran@gmail.com
ENABLE_DAILY_REPORT=true
ENABLE_WEEKLY_REPORT=true
TZ=Asia/Tehran
```
سپس سرویس را ری‌استارت کنید:
```bash
sudo systemctl restart smartcall
```

## ۳. آزمایش ارسال
- از داخل برنامه (بخش **گزارش‌ها**) دکمه‌ی «گزارش دستی (اکنون)» را بزنید،
- یا مستقیم:
  ```bash
  curl -X POST https://crm.example.com/api/reports/send \
    -H "Authorization: Bearer <TOKEN_دستگاه>" \
    -H "Content-Type: application/json" \
    -d '{"type":"MANUAL","recipient":"a.dadvaran@gmail.com"}'
  ```

## محتوای گزارش
- تعداد تماس‌های هر گوشی و مجموع، تماس‌های ازدست‌رفته، مدت مکالمات، شماره‌های پرتکرار، مشتریان جدید و پیگیری‌های معوق.
- قالب: **HTML** (بدنه‌ی ایمیل) + **CSV** (پیوست، با BOM برای نمایش صحیح فارسی در Excel).
- **PDF**: در این نسخه فعال نیست (به‌دلیل پیچیدگی شکل‌دهی متن فارسی در PDF). به‌عنوان افزوده‌ی اختیاری آتی در نظر گرفته شده است.

## گزارش تجمیعی داده‌های معوق
چون همگام‌سازی idempotent است، وقتی اینترنت برمی‌گردد داده‌های معوق یک‌بار ارسال می‌شوند و گزارش بعدی آن‌ها را **بدون تکرار** در بر می‌گیرد.
