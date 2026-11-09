# Cüzdanım (Android)

Yapılacaklar listesi + harcama, taksit ve düzenli ödeme takibi. Veriler yalnızca telefonda saklanır.

## APK'yı almak
GitHub'a her push'ta **Actions** sekmesindeki "APK derle" iş akışı çalışır (~3-4 dk).
Bitince **Releases** bölümünde `Cuzdanim.apk` belirir. Telefondan indirip açın;
"bilinmeyen kaynaklardan yükleme" izni istenirse verin.

## Android Studio ile
Klasörü Android Studio'da açın → Build → Build APK(s).

## Arayüzü değiştirmek
Tüm ekranlar `app/src/main/assets/www/index.html` içinde.

## Not
`keystore/` imza anahtarı repoda tutuluyor, böylece yeni sürümler eskisinin üstüne
(veriler silinmeden) kurulabiliyor. Repoyu **private** tutun.
