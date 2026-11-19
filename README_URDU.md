# KIKO AI — مکمل Android سورس پروجیکٹ

یہ ایک بنیادی Android ایپ پروجیکٹ ہے جس میں اردو UI، Gemini API کے ذریعے ٹیکسٹ چیٹ، Android Speech Recognizer کے ذریعے آواز سے سوال، Android Text-to-Speech سے جواب سنانا، اور اپڈیٹ چیک کرنے کا ابتدائی ماڈیول شامل ہے۔

> اہم: یہ مکمل buildable starter project ہے، ہر موبائل کام خودکار کرنے والا مکمل JARVIS نہیں۔ یہ ایپ خود سے فون unlock، دوسری ایپس پر کنٹرول، کال/پیغام بھیجنے یا خفیہ کارروائی نہیں کرتی۔ ایسی صلاحیتوں کے لیے الگ Android permissions، user consent، مخصوص integrations اور مزید development درکار ہے۔

## GitHub پر اپلوڈ کرنے کا طریقہ

1. اس ZIP کو موبائل میں ڈاؤن لوڈ کریں۔
2. ZArchiver یا کسی دوسری ZIP ایپ سے **Extract** کریں۔
3. Extract شدہ فولڈر کے اندر موجود تمام فائلیں اور فولڈر اپنی GitHub repository کے **root** میں اپلوڈ کریں۔ ZIP فائل کو خود repository میں اکیلا اپلوڈ نہ کریں۔
4. خاص طور پر `app` اور `.github` فولڈرز اپنی جگہ سمیت اپلوڈ ہوں۔
5. GitHub میں `Actions` کھولیں اور `Build KIKO AI Android APK` چلائیں۔ `main` branch پر push کرنے سے بھی workflow خود چلتا ہے۔
6. کامیاب run کے بعد `KIKO-AI-Android-APK` نام کا Artifact ڈاؤن لوڈ کریں۔ Artifact ZIP کے اندر `app-debug.apk` ہوگا۔

## لازمی Repository ساخت

```text
KIKO_AI1.2.1/
├── .github/
│   └── workflows/
│       └── main.yml
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/tgr/kiko/
│       │   ├── MainActivity.kt
│       │   └── UpdateManager.kt
│       └── res/xml/file_paths.xml
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── .gitignore
├── README_URDU.md
└── update.json.example
```

## Gemini API سیٹ اپ

1. Google AI Studio سے اپنی Gemini API key حاصل کریں۔
2. KIKO AI ایپ کھولیں اور `Gemini API Key سیٹ کریں` دبائیں۔
3. اپنی key ایپ میں درج کریں۔ اسے GitHub پر commit نہ کریں اور کسی کے ساتھ شیئر نہ کریں۔

یہ demo ایپ key کو فون کی SharedPreferences میں محفوظ کرتی ہے۔ عوامی/تجارتی ایپ میں client app کے اندر API key رکھنا محفوظ نہیں؛ بہتر ہے اپنی authenticated backend service استعمال کریں۔ API model کی دستیابی اور Google کی شرائط وقت کے ساتھ بدل سکتی ہیں۔

## آواز

مائیک بٹن Android کے Speech Recognizer کو کھولتا ہے۔ اردو شناخت فون کی speech service اور دستیاب زبانوں پر منحصر ہے۔ Text-to-Speech بھی فون میں اردو voice دستیاب ہونے پر منحصر ہے۔ پہلی بار مائیک permission مانگی جا سکتی ہے۔

## اپڈیٹ سسٹم — ابھی configuration درکار ہے

`UpdateManager.kt` میں `MANIFEST_URL` فی الحال `OWNER/REPO` placeholder ہے۔ خودکار اپڈیٹ چیک کام کرنے سے پہلے:

1. اسے اپنی repository کے درست raw `update.json` URL سے تبدیل کریں۔
2. `update.json.example` کی کاپی `update.json` نام سے repository میں رکھیں۔
3. ہر نئی release کے لیے `versionCode`, `versionName`, `apkUrl` اور APK کا درست 64-character SHA-256 hash درج کریں۔
4. APK کو GitHub Release میں publish کریں اور `apkUrl` میں اس کا HTTPS download URL دیں۔
5. تمام releases کو ایک ہی محفوظ Android signing key سے sign کریں۔ مختلف signing key والی APK موجودہ ایپ کو update نہیں کر سکے گی۔

Android صارف کی تصدیق کے بغیر APK خاموشی سے install نہیں کرتا؛ updater صرف verified APK ڈاؤن لوڈ کرکے Android installer کھولتا ہے۔ موجودہ GitHub Actions workflow **debug APK** بناتا ہے؛ debug signing key ہر CI run میں مستقل نہیں ہوتی، اس لیے اسے production auto-update کے لیے استعمال نہ کریں۔ حقیقی auto-update کے لیے مستقل محفوظ release keystore اور الگ signed release workflow درکار ہے۔

## Build کی تکنیکی تفصیل

- Android Gradle Plugin: 8.7.3
- Kotlin Android plugin: 2.0.21
- Gradle: 8.9
- Java: 17
- compileSdk / targetSdk: 35
- minSdk: 26 (Android 8.0+)

یہ ZIP سورس کوڈ اور GitHub Actions workflow فراہم کرتی ہے؛ پہلے سے تیار یا signed APK شامل نہیں۔ GitHub Actions کی کامیابی repository میں فائلیں درست اپلوڈ ہونے اور remote dependencies دستیاب ہونے پر منحصر ہے۔
