# راه‌اندازی بک‌اند با Docker (از صفر)

این راهنما کل بک‌اند (PostgreSQL + اپ Node + Caddy با HTTPS خودکار) را با **یک دستور** بالا می‌آورد. مناسب کسی که هنوز سرور و دامنه ندارد.

نتیجه‌ی نهایی: یک آدرس امن مثل `https://alicrm.duckdns.org/` که اپ موبایل به آن وصل می‌شود و یک داشبورد وب.

---

## گام ۰ — تهیه‌ی یک VPS
- از هر ارائه‌دهنده‌ای یک سرور **Ubuntu 22.04/24.04** با حداقل **۱ گیگ RAM** بگیرید.
- **IP عمومی** سرور را یادداشت کنید.
- با SSH وارد شوید:
  ```bash
  ssh root@IP_SERVER
  ```

## گام ۱ — ساب‌دامنه‌ی رایگان (DuckDNS)
چون دامنه ندارید، یک ساب‌دامنه‌ی رایگان بسازید (برای گواهی HTTPS لازم است):
1. به <https://www.duckdns.org> بروید و با گوگل/گیت‌هاب وارد شوید.
2. یک ساب‌دامنه بسازید، مثلاً `alicrm` → دامنه‌ی شما می‌شود `alicrm.duckdns.org`.
3. در کادر **current ip**، همان **IP عمومی سرور** را بگذارید و **update ip** را بزنید.
4. چند دقیقه صبر کنید تا DNS منتشر شود. تست:
   ```bash
   ping alicrm.duckdns.org   # باید IP سرور را نشان دهد
   ```

## گام ۲ — باز کردن پورت‌ها (فایروال)
```bash
sudo ufw allow 22/tcp
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw --force enable
```
> پورت ۸۰ برای دریافت گواهی Let's Encrypt لازم است؛ آن را باز بگذارید.

## گام ۳ — نصب Docker
```bash
curl -fsSL https://get.docker.com | sudo sh
sudo systemctl enable --now docker
docker --version && docker compose version
```

## گام ۴ — گرفتن کد روی سرور
مخزن خصوصی است؛ با یک **Personal Access Token** گیت‌هاب (دسترسی read روی همین ریپو) clone کنید:
```bash
cd /opt
sudo git clone https://github.com/alidadvaran/smart-call-crm.git
# Username: alidadvaran
# Password: <توکن PAT خود را اینجا بچسبانید>
cd smart-call-crm/deploy
```
> ساخت توکن: GitHub → Settings → Developer settings → Personal access tokens → Fine-grained → دسترسی read به مخزن `smart-call-crm`.
> جایگزین: پوشه را با `scp` از رایانه‌ی خودتان کپی کنید.

## گام ۵ — تنظیم مقادیر
```bash
cp .env.example .env
nano .env
```
این مقادیر را پر کنید:
- `DOMAIN=alicrm.duckdns.org`  ← ساب‌دامنه‌ی گام ۱
- `DB_PASSWORD=` یک رمز قوی
- `GMAIL_USER` و `GMAIL_APP_PASSWORD` ← طبق `docs/EMAIL_SETUP.md` (App Password جیمیل)
- `REPORT_RECIPIENT=a.dadvaran@gmail.com`
- `DASHBOARD_USER` و `DASHBOARD_PASS` برای ورود به داشبورد وب

## گام ۶ — اجرا (یک دستور)
```bash
sudo docker compose up -d --build
```
بار اول چند دقیقه طول می‌کشد (ساخت ایمیج + گرفتن گواهی TLS).

## گام ۷ — بررسی سلامت
```bash
sudo docker compose ps                 # هر سه سرویس باید Up باشند
curl https://alicrm.duckdns.org/healthz # → {"ok":true}
sudo docker compose logs -f app         # لاگ زنده‌ی اپ
```
- داشبورد وب: در مرورگر `https://alicrm.duckdns.org/` را باز کنید و با `DASHBOARD_USER`/`DASHBOARD_PASS` وارد شوید.

## گام ۸ — اتصال اپ موبایل
روی هر دو گوشی: برنامه → **تنظیمات** → **آدرس سرور** = `https://alicrm.duckdns.org/` → ذخیره → **همگام‌سازی دستی**.
سپس در بخش **دستگاه‌ها** نقش هر گوشی (اصلی/دوم) را تعیین کنید. داده‌ها در داشبورد وب ظاهر می‌شوند.

## به‌روزرسانی سرور در آینده
```bash
cd /opt/smart-call-crm && sudo git pull && cd deploy && sudo docker compose up -d --build
```
داده‌ها در volume دیتابیس حفظ می‌شوند.

## پشتیبان‌گیری از دیتابیس
```bash
sudo docker compose exec db pg_dump -U smartcall smartcall > backup-$(date +%F).sql
```

## رفع اشکال
- **گواهی TLS نمی‌آید:** مطمئن شوید DNS به IP سرور اشاره می‌کند و پورت‌های ۸۰/۴۴۳ باز و آزادند؛ `sudo docker compose logs caddy` را ببینید.
- **ایمیل ارسال نمی‌شود:** `GMAIL_APP_PASSWORD` باید App Password باشد (نه رمز اصلی)؛ `docs/EMAIL_SETUP.md`.
- **اپ وصل نمی‌شود:** آدرس باید دقیقاً `https://...` و با گواهی معتبر باشد (اپ cleartext/گواهی نامعتبر را رد می‌کند).
