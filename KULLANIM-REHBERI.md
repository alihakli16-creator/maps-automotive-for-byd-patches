# BYD Seal U DM-i & DiLink İçin Türkçe Google Haritalar Kullanım Rehberi

Bu proje, **BYD Seal U DM-i** (ve diğer DiLink 3.0, 4.0, 5.0 sistemli BYD araçlar) için geliştirilmiş, gösterge paneli entegrasyonu, hızlı işlem butonları, haritada 3D araç desteği ve tam Türkçe arayüze sahip özel Google Haritalar projesidir.

---

## 🚀 1. GitHub Actions ile Otomatik Hazır APK Oluşturma (Morphe Desktop Gerekmez!)

Artık bilgisayarınızda Morphe Desktop kurmak, Java yüklemek veya manuel yama yapmakla uğraşmanıza gerek yoktur!

1. Bu projeyi kendi GitHub deponuza `git push` ile gönderdiğinizde veya **Actions** sekmesinden **Run workflow** butonuna bastığınızda:
   - GitHub Actions arka planda otomatik olarak Android SDK ve Morphe araçlarını hazırlar.
   - Yama paketini derler.
   - Google Maps Automotive APK'sını yamalar, hizalar (zipalign) ve imzalar.
   - Kuruluma tamamen hazır **`BYD-Maps-TR-Ready.apk`** dosyasını hem **Artifacts (İş Çıktıları)** hem de **GitHub Releases** bölümüne yükler.
2. Oluşan `BYD-Maps-TR-Ready.apk` dosyasını indirip bir USB belleğe atarak veya telefonunuzdan Bluetooth/web yoluyla aracınıza aktarıp doğrudan kurabilirsiniz.

> **İpucu:** Kendi deponuzda GitHub Secrets kısmına `MAPS_APK_URL` adıyla orijinal Maps APK'sının doğrudan indirme bağlantısını eklerseniz veya `base-apk` adında bir release etiketi oluşturup APK'yı oraya yüklerseniz GitHub Actions her push işleminde hazır APK'yı anında üretir.

---

## 🚗 2. Haritada 3D Araç Özelliği (Navatars)

Harita üzerindeki sıradan mavi üçgen ok simgesi yerine kendi aracınızın 3D modelini kullanabilirsiniz.

### Nasıl Kullanılır?
1. Haritada sağ/sol üstteki **Ayarlar** simgesine dokunun.
2. Menüden **"Haritada 3D Araç"** seçeneğini açın.
3. **3D aracı göster** anahtarını açık konuma getirin.
4. **Model Seçimi:**
   - **BYD Seal U DM-i** (Varsayılan)
   - **BYD Tang 2024**
5. **Renk Seçimi:**
   - **Orijinal** (Modelin fabrika rengi)
   - **Titanyum Gri** (Seal U DM-i'nin özel rengi)
   - Gümüş, Beyaz, Siyah, Gri, Mavi, Kırmızı, Yeşil
6. En alttaki **"Şimdi uygula"** butonuna dokunun. Haritalar yeniden başlatılacak ve haritada seçtiğiniz araç görünecektir.

---

## ⚡ 3. Sol Paneldeki Hızlı İşlem Butonları (Y / O / U)

Sürüş esnasında menülere girmekle uğraşmamanız için harita ekranının solundaki dikey sütuna 3 adet hızlı erişim butonu yerleştirilmiştir:

- **"Y" Butonu (Yetkilendirme / Yansıtma):**  
  Tek dokunuşla yerel ADB bağlantısını onaylar ve haritayı direksiyon arkasındaki dijital gösterge paneline yansıtır.
  
- **"O" Butonu (Orijinal Göstergeye Geri Dön):**  
  Tek dokunuşla gösterge paneli yansıtmasını durdurur ve aracın orijinal fabrika gösterge ekranına geri döner.
  
- **"U" Butonu (Gösterge Paneli Haritasını Uzaklaştır / Yakınlaştır):**  
  Gösterge panelindeki harita fabrikasyon olarak çok yakın göründüğü için yolları ve detayları görmeyi zorlaştırabilir.
  - **Tek Dokunuş (Kısa Tık):** Gösterge panelindeki haritayı her basışta 1 kademe uzaklaştırır.
  - **Basılı Tutma (Uzun Tık):** Çok fazla uzaklaştırırsanız parmağınızı basılı tuttuğunuzda haritayı 1 kademe yakınlaştırır.
  - **Kalıcı Hafıza:** "U" butonuyla ayarladığınız uzaklaştırma seviyesi hafızaya kaydedilir. Aracı kapatıp yeniden çalıştırsanız dahi harita her zaman son bıraktığınız uzaklıkta açılır!

---

## 📱 4. Kurulum ve microG Gereksinimleri

1. Aracınızda **MicroG-RE** kurulu olmalıdır (v7.2.1 veya üstü).
2. `BYD-Maps-TR-Ready.apk` dosyasını aracınızın dosya yöneticisinden açıp yükleyin.
3. Uygulamayı ilk açtığınızda Konum ve Kişiler izinlerini onaylayın.
4. Google hesabınızla giriş yaparak favori konumlarınıza ve navigasyon geçmişinize doğrudan erişebilirsiniz.
