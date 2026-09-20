#!/usr/bin/env python3
import os
import sys
from http.server import HTTPServer, BaseHTTPRequestHandler

APK_PATH = "/app/applet/app/build/outputs/apk/debug/app-debug.apk"
if not os.path.exists(APK_PATH):
    APK_PATH = os.path.abspath("app/build/outputs/apk/debug/app-debug.apk")

HTML_TEMPLATE = """<!DOCTYPE html>
<html lang="ar" dir="rtl">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>تنزيل تطبيق SceneView Demo (3D & AR)</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Cairo:wght@400;600;700;800&display=swap" rel="stylesheet">
    <style>
        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
            font-family: 'Cairo', sans-serif;
        }
        body {
            background: linear-gradient(135deg, #0f172a 0%, #1e1b4b 50%, #0f172a 100%);
            color: #f8fafc;
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
            padding: 24px;
        }
        .container {
            background: rgba(255, 255, 255, 0.05);
            backdrop-filter: blur(16px);
            border: 1px solid rgba(255, 255, 255, 0.12);
            border-radius: 28px;
            max-width: 520px;
            width: 100%;
            padding: 40px 32px;
            text-align: center;
            box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);
        }
        .app-icon {
            width: 96px;
            height: 96px;
            background: linear-gradient(135deg, #6366f1 0%, #a855f7 100%);
            border-radius: 24px;
            margin: 0 auto 24px;
            display: flex;
            align-items: center;
            justify-content: center;
            box-shadow: 0 10px 25px -5px rgba(99, 102, 241, 0.5);
        }
        .app-icon svg {
            width: 52px;
            height: 52px;
            fill: #ffffff;
        }
        h1 {
            font-size: 26px;
            font-weight: 800;
            margin-bottom: 8px;
            background: linear-gradient(to left, #ffffff, #c7d2fe);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
        }
        .subtitle {
            color: #94a3b8;
            font-size: 15px;
            margin-bottom: 28px;
            line-height: 1.6;
        }
        .badge-row {
            display: flex;
            justify-content: center;
            gap: 12px;
            margin-bottom: 32px;
            flex-wrap: wrap;
        }
        .badge {
            background: rgba(99, 102, 241, 0.15);
            color: #a5b4fc;
            padding: 6px 14px;
            border-radius: 9999px;
            font-size: 13px;
            font-weight: 600;
            border: 1px solid rgba(99, 102, 241, 0.3);
        }
        .download-btn {
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 12px;
            background: linear-gradient(135deg, #4f46e5 0%, #7c3aed 100%);
            color: #ffffff;
            text-decoration: none;
            padding: 18px 28px;
            border-radius: 18px;
            font-size: 18px;
            font-weight: 700;
            box-shadow: 0 12px 28px -6px rgba(79, 70, 229, 0.5);
            transition: all 0.2s ease;
            margin-bottom: 24px;
            border: 1px solid rgba(255, 255, 255, 0.2);
        }
        .download-btn:hover {
            transform: translateY(-2px);
            box-shadow: 0 16px 32px -6px rgba(79, 70, 229, 0.7);
            background: linear-gradient(135deg, #4338ca 0%, #6d28d9 100%);
        }
        .download-btn svg {
            width: 24px;
            height: 24px;
            fill: currentColor;
        }
        .info-box {
            background: rgba(15, 23, 42, 0.5);
            border-radius: 16px;
            padding: 16px;
            margin-top: 20px;
            text-align: right;
            border: 1px solid rgba(255, 255, 255, 0.06);
        }
        .info-title {
            font-size: 14px;
            font-weight: 700;
            color: #e2e8f0;
            margin-bottom: 8px;
            display: flex;
            align-items: center;
            gap: 6px;
        }
        .info-steps {
            font-size: 13px;
            color: #94a3b8;
            padding-right: 18px;
            line-height: 1.8;
        }
        .version {
            font-size: 12px;
            color: #64748b;
            margin-top: 20px;
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="app-icon">
            <svg viewBox="0 0 24 24">
                <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5"/>
            </svg>
        </div>
        <h1>SceneView Demo (3D & AR)</h1>
        <p class="subtitle">تطبيق الواقع المعزز وعرض النماذج ثلاثية الأبعاد المتقدم لنظام أندرويد</p>
        
        <div class="badge-row">
            <span class="badge">APK أندرويد</span>
            <span class="badge">حجم الملف: 56.5 ميجابايت</span>
            <span class="badge">جاهز للتثبيت</span>
        </div>

        <a href="/download" class="download-btn">
            <svg viewBox="0 0 24 24">
                <path d="M19.35 10.04C18.67 6.59 15.64 4 12 4 9.11 4 6.6 5.64 5.35 8.04 2.34 8.36 0 10.91 0 14c0 3.31 2.69 6 6 6h13c2.76 0 5-2.24 5-5 0-2.64-2.05-4.78-4.65-4.96zM17 13l-5 5-5-5h3V9h4v4h3z"/>
            </svg>
            تحميل ملف APK الآن (Download)
        </a>

        <div class="info-box">
            <div class="info-title">
                💡 طريقة التثبيت على الهاتف:
            </div>
            <ol class="info-steps">
                <li>اضغط على زر <b>تحميل ملف APK</b> أعلاه.</li>
                <li>بعد اكتمال التنزيل، افتح الملف من قائمة التنزيلات.</li>
                <li>إذا طُلب منك السماح بتثبيت تطبيقات من هذا المصدر، اضغط <b>موافق / سماح</b>.</li>
                <li>اضغط <b>تثبيت (Install)</b> واستمتع بالتطبيق!</li>
            </ol>
        </div>

        <div class="version">
            الإصدار 1.0 (Debug APK) • جاهز للاستخدام الفوري
        </div>
    </div>
</body>
</html>
"""

class APKServerHandler(BaseHTTPRequestHandler):
    def do_HEAD(self):
        self.handle_request(head_only=True)

    def do_GET(self):
        self.handle_request(head_only=False)

    def handle_request(self, head_only=False):
        clean_path = self.path.split('?')[0]

        if clean_path in ['/download', '/app-debug.apk', '/app.apk']:
            if not os.path.exists(APK_PATH):
                self.send_error(404, "APK not found")
                return

            file_size = os.path.getsize(APK_PATH)
            self.send_response(200)
            self.send_header("Content-Type", "application/vnd.android.package-archive")
            self.send_header("Content-Disposition", 'attachment; filename="SceneViewDemo.apk"')
            self.send_header("Content-Length", str(file_size))
            self.send_header("Cache-Control", "no-cache")
            self.end_headers()

            if not head_only:
                with open(APK_PATH, "rb") as f:
                    while chunk := f.read(65536):
                        try:
                            self.wfile.write(chunk)
                        except (BrokenPipeError, ConnectionResetError):
                            break
            return

        # Default: Landing Page
        content = HTML_TEMPLATE.encode("utf-8")
        self.send_response(200)
        self.send_header("Content-Type", "text/html; charset=utf-8")
        self.send_header("Content-Length", str(len(content)))
        self.send_header("Cache-Control", "no-cache")
        self.end_headers()
        if not head_only:
            self.wfile.write(content)

    def log_message(self, format, *args):
        # Silence default logging
        sys.stderr.write("%s - - [%s] %s\n" % (self.address_string(), self.log_date_time_string(), format % args))

def run():
    server_address = ('0.0.0.0', 3000)
    httpd = HTTPServer(server_address, APKServerHandler)
    print("Serving APK web download page on port 3000...")
    httpd.serve_forever()

if __name__ == '__main__':
    run()
