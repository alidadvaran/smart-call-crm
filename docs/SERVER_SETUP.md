# راه‌اندازی سرور مرکزی (VPS)

راهنمای زیر برای Ubuntu 22.04/24.04 است. فرض: یک دامنه (مثلاً `crm.example.com`) به IP سرور اشاره می‌کند.

## ۱. نصب پیش‌نیازها
```bash
sudo apt update
sudo apt install -y postgresql nodejs npm
# توصیه: Node 20 LTS از nodesource اگر نسخه‌ی مخزن قدیمی بود.
node -v    # باید >= 18 باشد
```

## ۲. ساخت دیتابیس
```bash
sudo -u postgres psql <<'SQL'
CREATE USER smartcall WITH PASSWORD 'یک_رمز_قوی';
CREATE DATABASE smartcall OWNER smartcall;
SQL
```

## ۳. گرفتن کد و نصب وابستگی‌ها
```bash
# سورس را روی سرور قرار دهید (git clone یا کپی). سپس:
cd server
cp .env.example .env
nano .env     # مقادیر را پر کنید (DATABASE_URL، Gmail، DASHBOARD_*)
npm install --omit=dev
```

### مقدار `DATABASE_URL`
```
DATABASE_URL=postgres://smartcall:همان_رمز@localhost:5432/smartcall
```

## ۴. اجرای مهاجرت دیتابیس
```bash
npm run migrate
```

## ۵. اجرای سرویس با systemd
فایل `/etc/systemd/system/smartcall.service`:
```ini
[Unit]
Description=Smart Call CRM
After=network.target postgresql.service

[Service]
WorkingDirectory=/opt/smartcall/server
ExecStart=/usr/bin/node src/server.js
EnvironmentFile=/opt/smartcall/server/.env
Restart=always
User=www-data

[Install]
WantedBy=multi-user.target
```
سپس:
```bash
sudo systemctl daemon-reload
sudo systemctl enable --now smartcall
sudo systemctl status smartcall
```
سرویس روی پورت ۸۰۸۰ (محلی) گوش می‌دهد.

## ۶. HTTPS با Caddy (ساده‌ترین)
```bash
sudo apt install -y caddy
```
فایل `/etc/caddy/Caddyfile`:
```
crm.example.com {
    reverse_proxy 127.0.0.1:8080
}
```
```bash
sudo systemctl restart caddy
```
Caddy گواهی Let's Encrypt را خودکار می‌گیرد و تمدید می‌کند. حالا `https://crm.example.com/` فعال است.

> جایگزین: Nginx + certbot. هر راهی که TLS معتبر بدهد کافی است؛ اپ فقط HTTPS با گواهی معتبر را می‌پذیرد (cleartext ممنوع است).

## ۷. بررسی سلامت
```bash
curl https://crm.example.com/healthz   # {"ok":true}
```
داشبورد: `https://crm.example.com/` با `DASHBOARD_USER`/`DASHBOARD_PASS`.

## امنیت
- فقط پورت‌های ۲۲/۴۴۳ را باز بگذارید؛ پورت ۸۰۸۰ را از بیرون نبندید (پشت Caddy بماند).
- `.env` را محرمانه نگه دارید؛ هرگز در گیت قرار ندهید.
- هر دستگاه هنگام ثبت یک **توکن** می‌گیرد که فقط hash آن در دیتابیس ذخیره می‌شود؛ توکن در حافظه‌ی رمزنگاری‌شده‌ی گوشی نگهداری می‌شود.
