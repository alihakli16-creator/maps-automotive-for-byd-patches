# Haritada 3D Araç (Navatars) — Türkçe notlar

Bu sürümde (preview 11 tabanlı) Maps Automotive'e **3D araç** özelliği eklenmiştir. Özellik, Google Haritalar'ın
kendi "Navatars" özelliğini sunucu tarafında kapalıyken yamayla açar; model dosyaları APK içine gömülür,
internetten bir şey indirilmez.

## Menü
Gösterge paneli ayarları → **Haritada 3D Araç**
- **3D aracı göster**: açık/kapalı (kapalıyken düz üstten görünümlü araç simgesi kullanılır)
- **Model**: listedeki modeller (`assets/navatars/models.json`)
- **Renk**: Orijinal, Gümüş, Beyaz, Siyah, Gri, Mavi, Kırmızı, Yeşil (gövde boyası paletin 15. hücresidir)
- **Şimdi uygula**: Haritalar'ı yeniden başlatır (aktif rota sonlanır). Değişiklikler yoksa bir sonraki açılışta uygulanır.

## Derleme
`main` dalına push yapınca GitHub Actions `.mpp` dosyasını derler ve `turkish-preview.11` sürümüne ekler.
Morphe'ye bu `.mpp`'yi yerel kaynak olarak ekleyin; orijinal **Maps Automotive 26.10.120001.E** APK'sını yamalayın ve
aynı imza anahtarıyla kurun.

## Seal U DM-i modeli hakkında (önemli)
`assets/navatars/sealu.fbx` ve `sealu.png` şu an **Tang 2024 dosyalarının birebir kopyasıdır**; menüde
"BYD Seal U DM-i (geçici: Tang gövdesi)" olarak görünür ve araç haritada Tang gibi çizilir.
Gerçek Seal U modeli için:
1. Lisansı uygun bir Seal U DM-i 3D modeli bulun.
2. Modeli Navatars biçimine çevirin (aşağıdaki gereksinimler).
3. `sealu.fbx` ve `sealu.png` dosyalarını değiştirip `models.json` içindeki etiketi düzeltin.

Navatars model gereksinimleri (Tang dosyasından ölçülmüştür):
- İkili FBX 7500; hiyerarşi `Navatar (Null) → Rig → Body → CarMesh`; mesh `Body` kemiğine tek kümeyle bağlı
- Tek malzeme (`Paint`), tek **128×128 palet dokusu** (16×16 hücre, her hücre 8×8 piksel); gövde boyası hücre 15
- Yaklaşık 4.000 üçgen, iç mekân yok; uzunluk ≈ 2,7 birim, ön taraf ileri, Z yukarı
- Dört animasyon takısı: `idle`, `drive`, `left`, `right` (hareketsiz olabilir)

## Lisanslar
- Yama kodu: GPL-3.0 (`LICENSE`). Bu depo, `romanchukg-cloud/maps-automotive-for-byd-patches` deposunun türevidir (`UPSTREAM-NOTICE`).
- Tang 2024 modeli: CC BY-NC-SA 4.0, ticari olmayan kullanım, atıf ve aynı lisansla paylaşım (`assets/navatars/tang2024-LICENSE.txt`).
- Google Haritalar APK'sı ve Google'a ait dosyalar (yazı tipi, GPU listeleri vb.) bu depoya **konmamalıdır**.
