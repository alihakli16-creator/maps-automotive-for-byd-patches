// SPDX-License-Identifier: GPL-3.0-only
package local.maps.cluster;

import java.util.Locale;
import org.json.JSONObject;

/** Pure diagnostic presentation. Unconfirmed or unverified data is never labelled native EV data. */
final class VehicleDataText {
    private VehicleDataText(){}
    static String stale(){return "Veriler güncel değil\n\nBatarya: Kullanılamıyor\nElektrik menzili: Kullanılamıyor\nKablo: Kullanılamıyor\nŞarj: Kullanılamıyor\nAC kapak: Kullanılamıyor\nDC kapak: Kullanılamıyor\nPort kilidi geri bildirimi: Kullanılamıyor\nKalan enerji: Kullanılamıyor\n\nÖrnekler dakikada bir alınır. Değerlerin süresi 90 saniye sonra dolar.";}
    static String format(JSONObject status,String fallbackMode){
        String state=status.optString("reader_state"),protocol=status.optString("protocol","UNKNOWN");boolean fresh=status.optBoolean("fresh",false);
        JSONObject source=status.optJSONObject("source");String mode=source==null?fallbackMode:source.optString("mode",fallbackMode),confidence=source==null?"":source.optString("confidence","");
        String heading="FRESH".equals(state)?"ADB okuyucu bağlandı":
                       "STALE".equals(state)?"Veriler güncel değil":
                       "WAITING".equals(state)?"Araç verileri bekleniyor":
                       "EXPIRED".equals(state)?"Okuyucu oturumu sona erdi; otomatik okuyucu tekrar deneyecek":
                       "RESTART_REQUIRED".equals(state)?"Yeniden başlatma sonrası okuyucu bekleniyor":
                       "BOOT_UNAVAILABLE".equals(state)?"Multimedya yeniden başlatması doğrulanamıyor":
                       "STOPPED".equals(state)?"ADB okuyucu durduruldu":
                       "DISABLED".equals(state)?"BYD araç verileri kapalı":
                       "Okuyucu başlatılmadı; Otomatik okuyucuyu açın veya ADB ile bağlanın";
        JSONObject values=fresh?status.optJSONObject("values"):null;
        String text=heading+"\nProtokol: "+("CAN".equals(protocol)||"CANFD".equals(protocol)?protocol:"Bilinmiyor")+"\nOkuyucu modu: "+("legacy".equals(mode)?"Yazılım algılama (gelişmiş)":"Otomatik CAN/CANFD");
        if("candidate".equals(confidence))text+="\nDoğrulanmamış kaynak: onaylamadan önce şarj ve menzili gösterge paneliyle karşılaştırın.";
        else if("confirmed".equals(confidence))text+="\nKaynak: araç sahibi tarafından onaylandı";
        else if("firmware".equals(confidence))text+="\nKaynak: yazılım tanımlı adresler";
        else if("documented".equals(confidence))text+="\nKaynak: belgelenmiş CAN/CANFD adresleri";
        else if("unsupported".equals(confidence))text+="\nKaynak: desteklenmiyor; Haritalar'a batarya tahmini gönderilmiyor";
        if("UNKNOWN".equals(protocol)&&!"legacy".equals(mode))text+="\nCAN protokolü bilinmediğinde otomatik okuma desteklenmez.";
        text+="\n\nBatarya: "+display(values,"soc",protocol,mode)+"\nElektrik menzili: "+display(values,"range",protocol,mode)
            +"\nKablo: "+display(values,"cable",protocol,mode)+"\nŞarj: "+display(values,"charging",protocol,mode)
            +"\nAC kapak: "+display(values,"ac_port",protocol,mode)+"\nDC kapak: "+display(values,"dc_port",protocol,mode)
            +"\nPort kilidi geri bildirimi: "+display(values,"port_lock",protocol,mode)
            +"\nKalan enerji: "+display(values,"remaining_energy",protocol,mode);
        if(fresh)text+="\n\nÖrnek yaşı: "+String.format(Locale.US,"%.1f sn",status.optLong("sample_age_ms",0)/1000.0);
        return text;
    }
    static boolean confirmable(JSONObject status,JSONObject config){
        JSONObject source=status.optJSONObject("source"),values=status.optJSONObject("values");
        if(source==null||values==null||!"legacy".equals(config.optString("mode"))||!"legacy".equals(source.optString("mode"))||!"candidate".equals(source.optString("confidence"))||source.optLong("generation",-1)<0||source.optLong("generation",-1)!=config.optLong("generation",-2)||!status.optBoolean("fresh",false)||!source.optString("selection","").matches("[a-f0-9]{64}"))return false;
        JSONObject soc=values.optJSONObject("soc"),range=values.optJSONObject("range");
        return soc!=null&&range!=null&&"unconfirmed".equals(soc.optString("status"))&&"unconfirmed".equals(range.optString("status"))&&VehicleReadingRules.percent(soc.optDouble("raw",Double.NaN))&&VehicleReadingRules.range(range.optDouble("raw",Double.NaN));
    }
    static String candidate(JSONObject status){JSONObject values=status.optJSONObject("values");return "Batarya: "+display(values,"soc",status.optString("protocol"),"legacy")+"\nElektrik menzili: "+display(values,"range",status.optString("protocol"),"legacy");}
    static String display(JSONObject values,String key,String protocol,String mode){
        JSONObject row=values==null?null:values.optJSONObject(key);if(row==null)return "Kullanılamıyor";
        String status=row.optString("status");double raw=row.optDouble("raw",Double.NaN);
        if("CAN".equals(protocol)&&("cable".equals(key)||"ac_port".equals(key)||"dc_port".equals(key)||"remaining_energy".equals(key)||"port_lock".equals(key)))return "Desteklenmiyor";
        if("port_lock".equals(key)&&(!"CANFD".equals(protocol)||!"auto".equals(mode)))return "Desteklenmiyor";
        if("unsupported".equals(status))return "Desteklenmiyor";
        if("denied".equals(status))return "BYD tarafından erişim reddedildi";
        if("error".equals(status))return "Okuma hatası";
        if("ac_port".equals(key)||"dc_port".equals(key)){
            if(raw==0||raw==1)return "Doğrulanmamış (kod "+(int)raw+")";
            return unknownRaw(raw);
        }
        if("port_lock".equals(key)){
            if(raw==Math.rint(raw)&&raw>=1&&raw<=4){
                String[] feedback={"Kilitlendi","Geçersiz kilit geri bildirimi","Kilit açıldı","Geçersiz kilit açma geri bildirimi"};
                return feedback[(int)raw-1]+" (kod "+(int)raw+", doğrulanmamış)";
            }
            return unknownRaw(raw);
        }
        if("remaining_energy".equals(key))return Double.isFinite(raw)?String.format(Locale.US,"%.3f ham (birim doğrulanmadı)",raw):"Bilinmiyor (birim doğrulanmadı)";
        boolean candidate="unconfirmed".equals(status);
        if(!"available".equals(status)&&!candidate)return "error".equals(status)?"Okuma hatası":"Bilinmiyor";
        if("soc".equals(key))return VehicleReadingRules.percent(raw)?String.format(Locale.US,"%.1f %%",raw)+(candidate?" — Doğrulanmamış":""):"Bilinmiyor";
        if("range".equals(key))return VehicleReadingRules.range(raw)?String.format(Locale.US,"%.0f km",raw)+(candidate?" — Doğrulanmamış":""):"Bilinmiyor";
        if(candidate||!Double.isFinite(raw)||raw!=Math.rint(raw)||raw<Integer.MIN_VALUE||raw>Integer.MAX_VALUE)return "Bilinmiyor";
        if("charging".equals(key)&&("CAN".equals(protocol)||"legacy".equals(mode)))return raw==1?"Şarj ediliyor":"Bilinmiyor";
        return "cable".equals(key)?VehicleReadingRules.cable((int)raw):"charging".equals(key)?VehicleReadingRules.charging((int)raw):VehicleReadingRules.port((int)raw);
    }
    private static String unknownRaw(double raw){return Double.isFinite(raw)?"Bilinmiyor (ham "+String.format(Locale.US,raw==Math.rint(raw)?"%.0f":"%.3f",raw)+")":"Bilinmiyor";}
}