package local.maps.cluster;

import android.app.*;
import android.content.*;
import android.content.res.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.TypedValue;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.*;
import java.util.*;
import java.util.concurrent.*;
import org.json.JSONObject;

/** Cluster preferences in the same measured scene-card layout and theme as Maps' settings. */
final class ClusterMenu {
    private static ClusterMenu open;
    private static final Handler readerMain=new Handler(Looper.getMainLooper());
    private static final ExecutorService readerWorker=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"MapsVehicleReaderPage");t.setDaemon(true);return t;});
    private final Activity activity;
    private final Context context;
    private final SharedPreferences prefs;
    private final Dialog dialog;
    private final LinearLayout body,header;
    private final TextView title;
    private final ScrollView scroll;
    private final int surface,ink,secondary,accent,divider;
    private String page="main";
    private long pageGeneration;
    private final Map<String,MapsSwitch> switches=new HashMap<>();
    private final SharedPreferences.OnSharedPreferenceChangeListener changes=(p,key)->{
        MapsSwitch control=switches.get(key);if(control!=null){boolean checked=AppCorners.KEY.equals(key)?AppCorners.enabled(p):p.getBoolean(key,false);if(control.isChecked()!=checked)control.setChecked(checked);}
    };
    static void show(Activity activity,SharedPreferences prefs){
        if(activity.isFinishing()||activity.isDestroyed())return;
        if(open!=null){if(open.activity==activity&&open.dialog.isShowing())return;open.dialog.dismiss();}
        open=new ClusterMenu(activity,prefs);open.display();
    }
    static void destroyed(Activity a){if(open!=null&&open.activity==a)open.dialog.dismiss();}
    private ClusterMenu(Activity a,SharedPreferences p){
        activity=a;prefs=p;
        View card=a.findViewById(id(a,"scene_card"));context=card==null?a:card.getContext();
        boolean dark=(context.getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES;
        surface=colour("geoColorSurface","geo_sys_color_surface",dark?0xff121212:Color.WHITE);
        ink=colour("geoColorOnSurface","geo_sys_color_on_surface",dark?0xffe3e3e3:0xff202124);
        secondary=colour("geoColorOnSurfaceVariant","geo_sys_color_on_surface_variant",dark?0xffc4c7c5:0xff5f6368);
        // Maps' car scene can override these attributes independently from the Activity theme.
        // The pinned AAOS settings scene uses teal; the Activity's primary attribute is Google blue.
        accent=dark?0xff58b9ca:0xff007382;
        divider=(ink&0x00ffffff)|0x24000000;
        dialog=new Dialog(a,android.R.style.Theme_DeviceDefault_NoActionBar);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);dialog.setCanceledOnTouchOutside(true);
        LinearLayout panel=new LinearLayout(context);panel.setOrientation(LinearLayout.VERTICAL);
        panel.setLayoutDirection(View.LAYOUT_DIRECTION_LTR); // These settings are currently English.
        panel.setBackground(round(surface,18));panel.setClipToOutline(true);panel.setElevation(dp(8));
        header=new LinearLayout(context);header.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(headerButton(false),new LinearLayout.LayoutParams(dp(72),-1));
        title=text("Gösterge paneli",30,ink);title.setSingleLine(true);title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        header.addView(title,new LinearLayout.LayoutParams(0,-1,1));
        header.addView(headerButton(true),new LinearLayout.LayoutParams(dp(72),-1));panel.addView(header,new LinearLayout.LayoutParams(-1,dp(72)));
        View line=new View(context);line.setBackgroundColor(divider);panel.addView(line,new LinearLayout.LayoutParams(-1,dp(2)));
        scroll=new ScrollView(context);scroll.setFillViewport(false);scroll.setClipToPadding(false);scroll.setPadding(0,0,dp(24),0);scroll.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        body=new LinearLayout(context);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(0,0,0,dp(12));scroll.addView(body);
        panel.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));dialog.setContentView(panel);
        dialog.setOnKeyListener((d,key,e)->{if(key!=KeyEvent.KEYCODE_BACK)return false;if(e.getAction()==KeyEvent.ACTION_UP)back();return true;});
        dialog.setOnDismissListener(d->{WheelServiceSetup.cancelLearning();prefs.unregisterOnSharedPreferenceChangeListener(changes);if(open==this)open=null;});
        prefs.registerOnSharedPreferenceChangeListener(changes);main();
    }
    private static int id(Context c,String name){return MapsResources.id(c.getResources(),name,"id");}
    private int dp(int n){return Math.round(n*context.getResources().getDisplayMetrics().density);}
    private int attribute(String name,int fallback){
        int id=MapsResources.id(context.getResources(),name,"attr");TypedValue value=new TypedValue();
        try{if(id!=0&&context.getTheme().resolveAttribute(id,value,true)){
            if(value.type>=TypedValue.TYPE_FIRST_COLOR_INT&&value.type<=TypedValue.TYPE_LAST_COLOR_INT)return value.data;
            if(value.resourceId!=0)return context.getResources().getColorStateList(value.resourceId,context.getTheme()).getColorForState(new int[]{android.R.attr.state_enabled},fallback);
        }}catch(RuntimeException ignored){}return fallback;
    }
    private int colour(String attr,String name,int fallback){
        int resource=MapsResources.id(context.getResources(),name,"color");
        try{if(resource!=0)fallback=context.getResources().getColor(resource,context.getTheme());}catch(RuntimeException ignored){}
        return attribute(attr,fallback);
    }
    private GradientDrawable round(int colour,int radius){GradientDrawable d=new GradientDrawable();d.setColor(colour);d.setCornerRadius(dp(radius));return d;}
    private Drawable ripple(int radius){return new RippleDrawable(ColorStateList.valueOf((ink&0xffffff)|0x20000000),null,round(Color.WHITE,radius));}
    private TextView text(String value,int size,int colour){TextView v=new TextView(context);v.setText(value);v.setTextSize(TypedValue.COMPLEX_UNIT_DIP,size>=30?28:size);v.setIncludeFontPadding(false);v.setTextColor(colour);v.setGravity(Gravity.CENTER_VERTICAL);v.setFontFeatureSettings("kern");return v;}
    private ImageButton headerButton(boolean close){
        ImageButton b=new ImageButton(context);b.setImageDrawable(new Mark(close?1:0,ink));b.setBackground(ripple(40));b.setPadding(dp(16),dp(16),dp(16),dp(16));
        b.setContentDescription(close?"Gösterge paneli ayarlarını kapat":"Geri");b.setTooltipText(close?"Kapat":"Geri");b.setOnClickListener(v->{if(close)dialog.dismiss();else back();});return b;
    }
    private void display(){
        dialog.show();Window window=dialog.getWindow();window.setBackgroundDrawableResource(android.R.color.transparent);window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);window.setDimAmount(0);window.setElevation(dp(8));
        View decor=activity.getWindow().getDecorView();Rect screen=new Rect();decor.getWindowVisibleDisplayFrame(screen);
        View scene=activity.findViewById(id(activity,"scene_card"));Rect card=new Rect();
        if(scene==null||!scene.getGlobalVisibleRect(card)||card.width()<dp(280)){
            View gear=activity.findViewById(id(activity,"map_buttons_view_settings_button"));Rect anchor=new Rect();if(gear!=null)gear.getGlobalVisibleRect(anchor);
            int left=anchor.width()>0?(anchor.centerX()>screen.centerX()?anchor.left-dp(16)-dp(474):anchor.right+dp(16)):screen.left+dp(16);
            card.set(left,Math.max(screen.top+dp(16),anchor.top),left+dp(474),0);
        }
        int width=Math.min(card.width(),screen.width()-dp(32));int x=Math.max(screen.left+dp(8),Math.min(card.left,screen.right-width-dp(8)));
        int y=Math.max(screen.top+dp(8),card.top);int height=screen.bottom-y-dp(16);int[] origin=new int[2];decor.getLocationOnScreen(origin);
        WindowManager.LayoutParams lp=window.getAttributes();lp.gravity=Gravity.TOP|Gravity.LEFT;lp.x=x-origin[0];lp.y=y-origin[1];lp.width=width;lp.height=Math.max(dp(200),height);window.setAttributes(lp);
    }
    private void back(){if("main".equals(page))dialog.dismiss();else if("size".equals(page)||"side".equals(page))card();else if("logo_position".equals(page))layout();else if("evprofile".equals(page)||"autoreader".equals(page)||"readercompat".equals(page))vehicle();else main();}
    private void start(String name,String heading){page=name;++pageGeneration;title.setText(heading);switches.clear();body.removeAllViews();scroll.scrollTo(0,0);}
    private void rule(){View line=new View(context);line.setBackgroundColor(divider);body.addView(line,new LinearLayout.LayoutParams(-1,dp(2)));}
    private LinearLayout row(String name,String summary){
        LinearLayout row=new LinearLayout(context);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(16),dp(12),dp(16),dp(12));row.setMinimumHeight(dp(72));
        LinearLayout labels=new LinearLayout(context);labels.setOrientation(LinearLayout.VERTICAL);
        labels.addView(text(name,32,ink),new LinearLayout.LayoutParams(-1,-2));
        if(summary!=null&&!summary.isEmpty()){TextView detail=text(summary,24,secondary);detail.setPadding(0,dp(2),0,0);labels.addView(detail,new LinearLayout.LayoutParams(-1,-2));}
        row.addView(labels,new LinearLayout.LayoutParams(0,-2,1));body.addView(row,new LinearLayout.LayoutParams(-1,-2));return row;
    }
    private LinearLayout action(String name,String summary,Runnable action){
        LinearLayout r=row(name,summary);ImageView arrow=new ImageView(context);arrow.setImageDrawable(new Mark(2,ink));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(24),dp(32));lp.leftMargin=dp(12);r.addView(arrow,lp);
        r.setBackground(ripple(0));r.setFocusable(true);r.setOnClickListener(v->action.run());rule();return r;
    }
    private void toggle(String name,String description,String key,boolean checked){
        LinearLayout r=row(name,null);MapsSwitch sw=new MapsSwitch(context);sw.setChecked(checked);sw.setContentDescription(description);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(64),dp(48));lp.leftMargin=dp(12);r.addView(sw,lp);
        switches.put(key,sw);sw.setOnCheckedChangeListener((b,on)->{if((AppCorners.KEY.equals(key)?AppCorners.enabled(prefs):prefs.getBoolean(key,!on))!=on)prefs.edit().putBoolean(key,on).apply();});
        r.setBackground(ripple(0));r.setOnClickListener(v->sw.performClick());rule();
    }
    private void note(String value){TextView v=text(value,22,secondary);v.setPadding(dp(18),dp(18),dp(18),dp(18));body.addView(v,new LinearLayout.LayoutParams(-1,-2));}
    private void choices(String name,String key,String[] labels,int[] values,String explanation){
        start(key,name);int selected=prefs.getInt(key,values[0]);
        for(int i=0;i<labels.length;i++){final int value=values[i];LinearLayout r=row(labels[i],null);
            ImageView radio=new ImageView(context);radio.setImageDrawable(new Mark(value==selected?4:3,value==selected?accent:ink));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(28),dp(36));lp.leftMargin=dp(12);r.addView(radio,lp);
            r.setBackground(ripple(0));r.setFocusable(true);r.setContentDescription(labels[i]+(value==selected?", selected":""));r.setOnClickListener(v->{prefs.edit().putInt(key,value).apply();if("card".equals(name)||"size".equals(key)||"side".equals(key))card();else if("logo_position".equals(key))layout();else main();});rule();
        }
        if(explanation!=null)note(explanation);
    }
    private void slider(String name,String key,int min,int max,int current){
        LinearLayout labels=new LinearLayout(context);labels.setOrientation(LinearLayout.VERTICAL);labels.setPadding(dp(18),dp(16),dp(18),dp(18));
        TextView heading=text(name,30,ink),value=text(current+"%",24,accent);labels.addView(heading);labels.addView(value);
        SeekBar seek=new SeekBar(context);seek.setContentDescription(name);seek.setMax(max-min);seek.setProgress(current-min);seek.setThumbTintList(ColorStateList.valueOf(accent));seek.setProgressTintList(ColorStateList.valueOf(accent));seek.setProgressBackgroundTintList(ColorStateList.valueOf(divider));
        seek.setPadding(dp(8),0,dp(8),0);seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar s,int progress,boolean user){int n=min+progress;value.setText(n+"%");if(user)prefs.edit().putInt(key,n).apply();}
            public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}
        });labels.addView(seek,new LinearLayout.LayoutParams(-1,dp(64)));body.addView(labels);rule();
    }
    private String theme(int n){return n==ClusterSettings.THEME_AUTO?"Otomatik":n==ClusterSettings.THEME_SYSTEM?"Ekranla eşle":n==ClusterSettings.THEME_LIGHT?"Açık":"Koyu";}
    private void main(){
        start("main","Gösterge paneli");ClusterSettings.State s=ClusterSettings.state();
        toggle("Gösterge haritasını göster","Haritayı gösterge panelinde göster","map_enabled",s.mapEnabled);
        if(!DiLink51Setup.supported()&&!DiLink3Projection.isCandidate())toggle("Haritayı açmadan gösterge haritasını başlat","Harita arka planda başladığında gösterge haritasını göster",ClusterBackgroundStart.KEY,s.backgroundStart);
        toggle("Dönüş yönlendirmesi","Dönüş yönlendirmesini göstergeye ve HUD'a gönder","vehicle_guidance",s.vehicleGuidance);
        action("Harita renkleri",theme(s.clusterTheme),()->choices("Harita renkleri","cluster_theme",new String[]{"Otomatik","Ekranla eşle","Açık","Koyu"},new int[]{3,2,1,0},"Otomatik modu gün doğumu ve gün batımını takip eder. Ekranla eşle multimedya temasını izler. Navigasyon sırasında Haritalar kendi renklerini belirler. "+ClusterHost.themeDescription()));
        toggle("Uydu haritası","Gösterge panelinde uydu haritası","cluster_satellite",s.satellite);
        action("Harita düzeni",null,this::layout);
        if(DiLink51Setup.supported()){DiLink51Setup.init(activity.getApplication(),null);action("BYD gösterge teması","DiLink 5.1 · tam ve yan harita katmanları",this::oemLayout);}
        action("Direksiyondan yakınlaştırma",WheelServiceSetup.enabled()?"Açık · yalnızca gösterge paneli":"Kapalı",this::wheel);
        action("Gemini sesli asistanı",GeminiLaunch.wheelEnabled()?"Mikrofona uzun basma etkin":"Harita butonu ve direksiyon",this::gemini);
        action("Araca gönder",SendToCarSetup.summary(activity),this::sendToCar);
        if(DiLink3Projection.isCandidate())action("DiLink 3 gösterge testi",DiLink3Projection.enabled()?"Deneysel yansıtma açık":"Kapalı",this::diLink3);
        action("Rota kartı",s.visible?"%"+s.size+" · "+(s.side==0?"Sol":"Sağ"):"Kapalı",this::card);
        toggle("Pencere içinde pencere (PiP)","Haritalar'dan çıkarken pencere içinde pencere modu","picture_in_picture",s.pictureInPicture);
        if(BydGuidance.untestedHudAvailable())action("HUD (Hayalet ekran) çıkışı",null,this::hud);
        action("Google hesabı",SignInAccess.description().replace("Google sign-in: ",""),()->{
            if(AccountAccess.restartNeeded(activity))AccountAccess.restart(activity);
            else if(AccountAccess.microg(activity)&&!AccountAccess.granted(activity))AccountAccess.request(activity);
            else SignInAccess.show(activity);
        });
        action("Arayüz boyutu","%"+MainUiScale.selected(prefs)+" · ana Haritalar ekranı",this::interfaceSize);
        toggle("Yuvarlatılmış uygulama köşeleri","Merkezi Haritalar ekranlarında yuvarlatılmış dış köşeler",AppCorners.KEY,AppCorners.enabled(prefs));
        action("Güncellemeler",BuildInfo.VERSION,()->AppUpdater.show(activity));
        action("Destek ve tanılama",null,()->Diagnostics.show(activity));
        action("BYD araç verileri","ADB batarya ve elektrik menzili",this::vehicle);
        if(EvTest.available())action("EV verilerini test et","Yapay test değerleri",()->EvTest.show(activity));
        note("Değişiklikler otomatik olarak kaydedilir.");
    }
    private void diLink3(){
        start("dilink3","DiLink 3 gösterge testi");
        DiLink3Projection.init(activity.getApplication(),null);
        LinearLayout enabledRow=row("DiLink 3 harita yansıtmasını etkinleştir","Deneysel · yalnızca Android 10 / DiLink 3");
        MapsSwitch control=new MapsSwitch(context);control.setChecked(DiLink3Projection.enabled());
        control.setContentDescription("DiLink 3 harita yansıtmasını etkinleştir");
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(64),dp(48));lp.leftMargin=dp(12);enabledRow.addView(control,lp);
        control.setOnCheckedChangeListener((button,on)->{
            if(!control.isEnabled())return;control.setEnabled(false);
            Runnable done=()->{if(dialog.isShowing()&&"dilink3".equals(page))diLink3();};
            if(on)new AlertDialog.Builder(activity).setTitle("DiLink 3 gösterge haritası test edilsin mi?")
                .setMessage("Use only on a parked DiLink 3 head unit running Android 10. Maps will use local ADB to switch the instrument cluster into projection mode. Its map area can be blank for several seconds while the display is prepared.\n\nKeep ADB enabled and approve the Maps key if prompted. This is an unverified car test. Turn this option off or use Restore stock cluster if the result is not usable. A forced stop can leave projection active until Maps is opened again for recovery.")
                .setNegativeButton("İptal",(d,w)->done.run()).setOnCancelListener(d->done.run())
                .setPositiveButton("Testi başlat",(d,w)->DiLink3Projection.configure(activity,true,true,done)).show();
            else DiLink3Projection.configure(activity,false,false,done);
        });
        enabledRow.setBackground(ripple(0));enabledRow.setOnClickListener(v->{if(control.isEnabled())control.performClick();});rule();
        LinearLayout state=row("Yansıtma durumu",DiLink3Projection.status());rule();
        final long generation=pageGeneration;
        Runnable status=new Runnable(){public void run(){
            if(!dialog.isShowing()||!"dilink3".equals(page)||pageGeneration!=generation)return;
            summary(state,DiLink3Projection.status());readerMain.postDelayed(this,1000);
        }};readerMain.postDelayed(status,1000);
        action("Yetkilendir veya tekrar dene","Bu multimedyanın yerel ADB bağlantısını onaylayın",()->DiLink3Projection.retry(activity,true,()->{if(dialog.isShowing()&&"dilink3".equals(page))diLink3();}));
        action("Orijinal göstergeyi geri yükle","Yansıtmayı duraklat ve orijinal gösterge düzenine dön",()->DiLink3Projection.restore(activity,()->{if(dialog.isShowing()&&"dilink3".equals(page))diLink3();}));
        note("Also keep Show cluster map enabled. This test creates the known DiLink 3 projection display and sends the native Google cluster screen there. It does not enable the BYD battery reader and does not claim HUD support.");
        note("127.0.0.1:5555 is this head unit. Keep it unchanged. Send the exact status, firmware version and whether a map is physically visible. No uninstall or account reset is needed.");
        note("Restore pauses this session. Turn the test option off after testing to keep it off on the next Maps launch.");
    }
    private void sendToCar(){
        start("sendtocar","Araca gönder");
        LinearLayout keepRow=row("Telefon rotalarının gelmesini sağla","MicroG-RE'yi pil optimizasyonundan muaf tutar; yerel ADB kullanır");
        MapsSwitch keep=new MapsSwitch(context);keep.setChecked(SendToCarSetup.exempt(activity));keep.setContentDescription("Keep phone routes arriving");
        LinearLayout.LayoutParams keepLp=new LinearLayout.LayoutParams(dp(64),dp(48));keepLp.leftMargin=dp(12);keepRow.addView(keep,keepLp);
        Runnable refresh=()->{if(dialog.isShowing()&&"sendtocar".equals(page))sendToCar();};
        keep.setOnCheckedChangeListener((button,on)->{
            if(!keep.isEnabled())return;keep.setEnabled(false);
            SendToCarSetup.configure(activity,on,refresh);
        });
        keepRow.setBackground(ripple(0));keepRow.setOnClickListener(v->{if(keep.isEnabled())keep.performClick();});rule();
        String status=SendToCarSetup.status();if(!status.isEmpty())note(status);
        action("Şimdi yeniden bağlan","MicroG-RE'den anlık bildirimleri yeniden bağlamasını iste",()->SendToCarSetup.reconnect(activity,refresh));
        note("In Google Maps on your phone, choose Send to car and select this car. The route opens here with Start.");
        note("After the car starts, Android 12 does not let MicroG-RE restart its push connection in the background. Turn on Keep phone routes arriving once: Maps uses its local ADB key to exempt MicroG-RE from battery optimization. Approve the Maps ADB key with Always allow if asked. Turning it off removes the exemption.");
    }
    private void gemini(){
        start("gemini","Gemini sesli asistanı");
        toggle("Gemini butonunu göster","Yan sütundaki gösterge haritası düğmesinin altında Gemini Live butonu",GeminiLaunch.BUTTON_KEY,prefs.getBoolean(GeminiLaunch.BUTTON_KEY,true));
        toggle("Direksiyondan Gemini butonu","Atanan direksiyon butonuyla Gemini'yi açın",GeminiLaunch.WHEEL_KEY,prefs.getBoolean(GeminiLaunch.WHEEL_KEY,false));
        LinearLayout voiceKey=action("Gemini butonu",WheelServiceSetup.describe(WheelServiceSetup.geminiBinding())+" · dokunun, ardından direksiyon butonuna basın",()->{
            if(!WheelServiceSetup.startLearning(4))Toast.makeText(activity,WheelServiceSetup.learningStatus(),Toast.LENGTH_LONG).show();});
        LinearLayout voiceLearning=row("Buton ataması",WheelServiceSetup.learningStatus());rule();
        action("Tang mikrofon butonunu geri yükle","Uzun basma, tuş 312/312",WheelServiceSetup::restoreTangGemini);
        LinearLayout voiceReceived=row("Son tuşlar",WheelKeyService.recentKeys());rule();
        toggle("Kompakt asistan","Google varsayılan dijital asistan olduğunda Haritalar üzerinde Gemini'yi göster",GeminiLaunch.COMPACT_KEY,prefs.getBoolean(GeminiLaunch.COMPACT_KEY,true));
        LinearLayout recoveryRow=row("Otomatik kompakt kurtarma","BYD asistanı sıfırladığında Google'ı geri yükle; yerel ADB gerektirir");
        MapsSwitch recovery=new MapsSwitch(context);recovery.setChecked(GeminiRecovery.enabled());recovery.setContentDescription("Automatic compact recovery");
        LinearLayout.LayoutParams recoveryLp=new LinearLayout.LayoutParams(dp(64),dp(48));recoveryLp.leftMargin=dp(12);recoveryRow.addView(recovery,recoveryLp);
        recovery.setOnCheckedChangeListener((button,on)->{
            if(!recovery.isEnabled())return;recovery.setEnabled(false);
            GeminiRecovery.configure(activity,on,()->{if(dialog.isShowing()&&"gemini".equals(page))gemini();});
        });
        recoveryRow.setBackground(ripple(0));recoveryRow.setOnClickListener(v->{if(recovery.isEnabled())recovery.performClick();});rule();
        note(GeminiRecovery.status());
        action("Dijital asistan",GeminiLaunch.googleAssistant(activity)?"Google · kompakt mod kullanılabilir":"Kompakt mod için Google'ı seçin",()->GeminiLaunch.assistantSettings(activity));
        action("Gemini'yi Aç",GeminiLaunch.available(activity)?"Gemini Live":"Önce Gemini'yi yükleyip oturum açmayı tamamlayın",()->GeminiLaunch.open(activity));
        action("Direksiyon tuş servisi",WheelServiceSetup.connected()?"Bağlı":"Tuş servisini etkinleştirmek için direksiyon kurulumunu açın",this::wheel);
        note("By default the Gemini button is the Tang DiLink 5.0 long press on the microphone key; the short press keeps the BYD assistant and the custom star keeps cluster zoom. On other firmware, tap Gemini button and press the wheel button you want within 10 seconds; that press is not passed to the car. Press the Gemini button again to close compact Gemini. A zoom button cannot also open Gemini. Recent keys shows what this head unit sends to Android. The map and cluster do not need an active route. Keys pass through during phone calls.");
        final long expected=pageGeneration;
        readerMain.postDelayed(new Runnable(){public void run(){
            if(!dialog.isShowing()||!"gemini".equals(page)){WheelServiceSetup.cancelLearning();return;}
            if(pageGeneration!=expected)return;
            summary(voiceKey,WheelServiceSetup.describe(WheelServiceSetup.geminiBinding())+" · tap, then press the wheel button");
            summary(voiceLearning,WheelServiceSetup.learningStatus());summary(voiceReceived,WheelKeyService.recentKeys());readerMain.postDelayed(this,500);
        }},500);
        note("Automatic compact recovery is optional. Approve the Maps ADB key once on this head unit, using Always allow. After a BYD reset, tapping the Maps voice button or holding the wheel microphone restores Google and opens the selected voice mode. A computer is not needed. Another selected digital assistant is kept. No recovery runs while Maps is in the background or without a voice-button tap.");
        note("Uses the existing wheel key service enabled in Steering-wheel zoom. Keep that service enabled. Compact mode uses the Android assistant over Maps while Maps is on screen. Select Google as the default digital assistant once. Otherwise, or outside Maps, the full Gemini app opens. One tap starts Gemini Live. This requires the updated patched Google app. Google app and Gemini must already be set up with MicroG-RE. Allow Contacts and Location for MicroG-RE, and Microphone for Google.");
    }
    private void interfaceSize(){
        start("interface_size","Arayüz boyutu");int selected=MainUiScale.selected(prefs);
        int[] values={100,90,80};String[] names={"%100 · Standart","%90 · Kompakt","%80 · Daha küçük"};
        for(int i=0;i<values.length;i++){
            final int value=values[i];LinearLayout r=row(names[i],null);
            ImageView radio=new ImageView(context);radio.setImageDrawable(new Mark(value==selected?4:3,value==selected?accent:ink));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(28),dp(36));lp.leftMargin=dp(12);r.addView(radio,lp);
            r.setBackground(ripple(0));r.setFocusable(true);r.setContentDescription(names[i]+(value==selected?", selected":""));
            r.setOnClickListener(v->{prefs.edit().putInt(MainUiScale.KEY,value).apply();interfaceSize();MainUiScale.changed(activity);});rule();
        }
        note("Changes the central Maps interface, icons and text. The instrument cluster and Android settings keep their own size. Your Android font-size preference is preserved.");
        note("Saved: "+selected+"%. Applied to this screen: "+MainUiScale.applied(activity)+"%. Changes apply automatically when Maps is ready and navigation is inactive. During navigation, your choice is saved and applied afterwards while Maps is on screen.");
        note("Local experiment. Choose 100% to restore standard Maps size.");
    }
    private void layout(){
        start("layout","Harita düzeni");ClusterSettings.State s=ClusterSettings.state();
        action("Google Haritalar logosu",s.logoPosition==0?"Sol alt":"Sağ üst",()->choices("Google Haritalar logosu","logo_position",new String[]{"Sol alt","Sağ üst"},new int[]{0,1},null));
        slider("İmleç dikey ofseti","pointer_vertical",-25,25,s.pointerVertical);note("Negatif değer imleci yukarı taşır; pozitif değer aşağı taşır.");
        slider("İmleç yatay ofseti","pointer_horizontal",-25,25,s.pointerHorizontal);note("Negatif değer imleci sola taşır; pozitif değer sağa taşır. 0 standart merkezi korur.");
        action("İmleç konumunu sıfırla","Dikey %10, yatay %0",()->{prefs.edit().putInt("pointer_vertical",10).putInt("pointer_horizontal",0).apply();layout();});
    }
    private void oemLayout(){
        if(!DiLink51Setup.supported()){main();return;}
        DiLink51Setup.init(activity.getApplication(),null);DiLink51Setup.refreshStatus();
        start("oem_layout","BYD gösterge teması");final boolean loaded=DiLink51Setup.loaded();
        LinearLayout autoRow=row("Gösterge temasını takip et","Tam harita ve yan kartı takip etmek için orijinal BYD etkinlik görünürlüğünü kullanın");
        MapsSwitch control=new MapsSwitch(context);control.setChecked(DiLink51Setup.automatic());control.setEnabled(loaded);control.setContentDescription("Follow instrument theme");
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(64),dp(48));lp.leftMargin=dp(12);autoRow.addView(control,lp);
        control.setOnCheckedChangeListener((button,on)->{if(control.isEnabled()){DiLink51Setup.setAutomatic(on);oemLayout();}});
        autoRow.setBackground(ripple(0));autoRow.setOnClickListener(v->{if(control.isEnabled())control.performClick();});rule();
        LinearLayout status=row("Usage Access",DiLink51Setup.status());rule();
        action("Kullanım Erişimine İzin Ver · ADB","Yalnızca Haritalar için tek seferlik kurulum; onay penceresini inceleyin",()->DiLink51Setup.prepareUsageAccess(activity,true,()->{if(dialog.isShowing()&&"oem_layout".equals(page))oemLayout();}));
        note("Manuel düzen");
        DiLink51Profile.Theme selected=DiLink51Setup.manualTheme();
        DiLink51Profile.Theme[] values={DiLink51Profile.Theme.MAP,DiLink51Profile.Theme.SCENARIO,DiLink51Profile.Theme.SIMPLE};
        String[] names={"Harita · tam genişlikte harita","Senaryo · sağ taraf haritası","Basit · sağ taraf haritası"};
        for(int i=0;i<values.length;i++){
            final DiLink51Profile.Theme value=values[i];LinearLayout choice=row(names[i],null);
            ImageView radio=new ImageView(context);radio.setImageDrawable(new Mark(value==selected?4:3,value==selected?accent:ink));LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(dp(28),dp(36));rp.leftMargin=dp(12);choice.addView(radio,rp);
            choice.setEnabled(loaded);choice.setBackground(ripple(0));choice.setFocusable(true);choice.setContentDescription(names[i]+(value==selected?", selected":""));
            choice.setOnClickListener(v->{if(choice.isEnabled()){DiLink51Setup.setTheme(value);oemLayout();}});rule();
        }
        note("Manual mode uses your selected layer. It cannot tell whether the driver can see that map, so native zoom controls require automatic following, Usage Access and a known visible stock layout.");
        note("Automatic following is off by default. It reads only the four supported stock cluster activities. Unknown visibility hides the map and releases wheel controls. Allowing Usage Access does not enable following; use the switch separately. Turning it off stops reading and keeps the Android grant.");
        note("This profile is restricted to two exact DiLink 5.1/UI7 firmware builds. The side layer is never replaced with the full layer when it is unavailable. UI7 and the new layout still need an on-car test.");
        final long expected=pageGeneration;
        readerMain.postDelayed(new Runnable(){public void run(){
            if(!dialog.isShowing()||!"oem_layout".equals(page)||pageGeneration!=expected)return;
            if(loaded!=DiLink51Setup.loaded()){oemLayout();return;}
            summary(status,DiLink51Setup.status());readerMain.postDelayed(this,500);
        }},500);
    }
    private void wheel(){
        start("wheel","Direksiyondan yakınlaştırma");
        ClusterZoom.prepare();
        LinearLayout enabledRow=row("Direksiyondan yakınlaştırmayı etkinleştir","Gösterge paneli haritası için atanan butonları kullanın");
        MapsSwitch control=new MapsSwitch(context);control.setChecked(WheelServiceSetup.enabled());
        control.setContentDescription("Enable steering-wheel zoom");
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(64),dp(48));lp.leftMargin=dp(12);enabledRow.addView(control,lp);
        control.setOnCheckedChangeListener((button,on)->{
            if(!control.isEnabled())return;
            control.setEnabled(false);
            WheelServiceSetup.configure(activity,on,on,()->{if(dialog.isShowing()&&"wheel".equals(page))wheel();});
        });
        enabledRow.setBackground(ripple(0));enabledRow.setOnClickListener(v->{if(control.isEnabled())control.performClick();});rule();
        LinearLayout service=row("Servis",WheelServiceSetup.status());rule();
        LinearLayout camera=row("Harita kontrolleri",ClusterZoom.description());rule();
        LinearLayout key=row("Son kontrol tuşu",WheelKeyService.lastKey());rule();
        WheelKeyPolicy.Bindings assigned=WheelServiceSetup.bindings();
        LinearLayout[] buttons=new LinearLayout[3];
        for(int role=1;role<=3;role++){final int which=role;WheelKeyPolicy.Binding b=which==1?assigned.mode:which==2?assigned.in:assigned.out;
            buttons[which-1]=action(WheelServiceSetup.roleName(which)+" butonu",WheelServiceSetup.describe(b)+" · dokunun, ardından direksiyon butonuna basın",()->{
                if(!WheelServiceSetup.startLearning(which))Toast.makeText(activity,WheelServiceSetup.learningStatus(),Toast.LENGTH_LONG).show();});}
        LinearLayout learning=row("Buton ataması",WheelServiceSetup.learningStatus());rule();
        action("BYD Tang butonlarını geri yükle","Özel 305, ses + 291, ses − 292",WheelServiceSetup::restoreTangButtons);
        LinearLayout received=row("Son tuşlar",WheelKeyService.recentKeys());rule();
        if(WheelServiceSetup.enabled())action("Yetkilendir veya tekrar dene","Bu multimedyada Haritalar ADB anahtarına izin verin",()->
            WheelServiceSetup.configure(activity,true,true,()->{if(dialog.isShowing()&&"wheel".equals(page))wheel();}));
        action("Yakınlaştır","Yalnızca gösterge paneli haritası",()->clusterZoom(1));
        action("Uzaklaştır","Yalnızca gösterge paneli haritası",()->clusterZoom(-1));
        action("Gösterge yakınlaştırmasını sıfırla","Otomatik harita yakınlaştırmasına dön",()->{ClusterZoom.reset();wheel();});
        note("Press the custom steering wheel button to turn cluster zoom controls on, then use volume + or − to zoom the instrument cluster map. Press the custom button again to restore volume controls. A call, unavailable cluster map or turning this feature off ends the control mode; press the custom button again afterwards.");
        note("The chosen map zoom stays until Reset cluster zoom or the cluster map is recreated. The central map keeps its own scale. Outside the control mode, the volume buttons work normally.");
        note("Default buttons are for BYD Tang (DiLink 5.0). The console volume control may send the same events and zoom too. On other firmware, tap a button above and press the wheel button you want within 10 seconds; that press is not passed to the car. Recent keys lists what this head unit sends to Android and whether Maps kept it. If a wheel button never appears there, the head unit handles it itself and it cannot zoom the map.");
        note("Setup uses ADB on this head unit and a key-only accessibility service. Approve the standard ADB prompt once. It does not read screen content. ADB must remain enabled for service restoration after force-stop; open Maps again after force-stop.");
        final long expected=pageGeneration;
        readerMain.postDelayed(new Runnable(){public void run(){
            if(!dialog.isShowing()||!"wheel".equals(page)){WheelServiceSetup.cancelLearning();return;}
            if(pageGeneration!=expected)return;
            summary(service,WheelServiceSetup.status());
            summary(camera,(WheelKeyService.modeActive()?"Controls active · ":"")+ClusterZoom.description());
            summary(key,WheelKeyService.lastKey());
            WheelKeyPolicy.Bindings now=WheelServiceSetup.bindings();
            for(int role=1;role<=3;role++)summary(buttons[role-1],WheelServiceSetup.describe(role==1?now.mode:role==2?now.in:now.out)+" · tap, then press the wheel button");
            summary(learning,WheelServiceSetup.learningStatus());summary(received,WheelKeyService.recentKeys());readerMain.postDelayed(this,500);
        }},500);
    }
    private void clusterZoom(int direction){
        if(!ClusterZoom.manualStep(direction))Toast.makeText(activity,"Gösterge paneli haritasının hazır olmasını bekleyin.",Toast.LENGTH_SHORT).show();
    }
    private static void summary(LinearLayout row,String value){
        View labels=row.getChildAt(0);
        if(labels instanceof LinearLayout&&((LinearLayout)labels).getChildCount()>1){
            View detail=((LinearLayout)labels).getChildAt(1);if(detail instanceof TextView)((TextView)detail).setText(value);
        }
    }
    private void card(){
        start("card","Rota kartı");ClusterSettings.State s=ClusterSettings.state();
        toggle("Rota kartını göster","Rota kartı, varış zamanı ve mesafe","visible",s.visible);
        action("Kart boyutu","%"+s.size,()->choices("Kart boyutu","size",new String[]{"%50","%75","%100"},new int[]{50,75,100},null));
        action("Konum",s.side==0?"Sol":"Sağ",()->choices("Konum","side",new String[]{"Sol","Sağ"},new int[]{0,1},null));
        slider("Kenar boşluğu","margin",0,20,s.margin);slider("Kart dikey ofseti","vertical",-25,25,s.vertical);
        note("Negatif değer kartı yukarı taşır; pozitif değer aşağı taşır.");
    }
    private void vehicleDataToggle(){
        LinearLayout r=row("BYD araç verilerini kullan","ADB üzerinden salt okunur batarya ve elektrik menzili");
        MapsSwitch control=new MapsSwitch(context);control.setChecked(VehicleDataStore.enabled(activity));
        control.setContentDescription("Use BYD vehicle data");
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(64),dp(48));lp.leftMargin=dp(12);r.addView(control,lp);
        control.setOnCheckedChangeListener((button,on)->{if(control.isEnabled())VehicleDataPage.setEnabled(activity,control,on);});
        VehicleDataPage.bindToggle(activity,control);
        r.setBackground(ripple(0));r.setOnClickListener(v->{if(control.isEnabled())control.performClick();});rule();
    }
    private void autoReader(){
        start("autoreader","Otomatik okuyucu");
        note("Start the BYD reader on this head unit after restart or wake, without a computer. BYD vehicle data must also be on. ADB must remain enabled on this head unit.");
        note("Maps creates a private key for this installation. Approve the standard ADB prompt once and select Always allow. No computer key or extra app is used.");
        row("Durum",VehicleAutoReader.description());rule();
        boolean on=VehicleAutoReader.enabled();
        action(on?"Otomatik okuyucuyu kapat":"Otomatik okuyucuyu aç",on?"Otomatik başlatmayı ve kendi okuyucusunu durdur":"Yerel ADB ve otomatik başlatmayı ayarla",()->VehicleAutoReader.configure(activity,!on,true,()->{if(dialog.isShowing()&&"autoreader".equals(page))autoReader();}));
        if(on)action("Yetkilendir veya tekrar dene", "Bu multimedyada Haritalar ADB anahtarını onaylayın",()->VehicleAutoReader.configure(activity,true,true,()->{if(dialog.isShowing()&&"autoreader".equals(page))autoReader();}));
        note("Uses only 127.0.0.1:5555. If the head unit blocks boot startup, allow Maps Automotive in its app startup settings. Force-stop requires opening Maps again.");
    }
    private boolean readerPageAlive(long generation){return generation==pageGeneration&&"readercompat".equals(page)&&dialog.isShowing()&&body.isAttachedToWindow()&&!activity.isFinishing()&&!activity.isDestroyed();}
    private static boolean candidateLive(VehicleDataStore.Access access,long epoch,String identity,long deadline){return access.enabled&&access.readerActive&&access.epoch==epoch&&access.sourceKnown&&identity!=null&&!identity.isEmpty()&&identity.equals(access.sourceIdentity)&&deadline>0&&SystemClock.elapsedRealtime()<=deadline;}
    private void changeReaderMode(String mode,boolean redetect,String confirmed){
        start("readercompat","Okuyucu uyumluluğu");note("Updating reader settings…");final long generation=pageGeneration;
        try{VehicleReaderSettings.configure(activity,mode,redetect,confirmed,()->{if(readerPageAlive(generation))readerCompatibility();});}
        catch(IllegalArgumentException invalid){Toast.makeText(activity,"Okuyucu seçimi değişti. Onaylamadan önce tekrar algılayın.",Toast.LENGTH_LONG).show();if(readerPageAlive(generation))readerCompatibility();}
    }
    private void selectReaderMode(String mode){
        if(!"legacy".equals(mode)){changeReaderMode(mode,false,"");return;}
        final long generation=pageGeneration;
        new AlertDialog.Builder(activity).setTitle("Enable firmware detection?")
            .setMessage("This advanced mode probes a small set of read-only firmware addresses. Fallback battery charge and range remain unconfirmed. Compare both with the instrument cluster, then confirm them here before Maps can use them for EV routes.")
            .setNegativeButton("Cancel",null).setPositiveButton("Enable",(d,w)->{if(readerPageAlive(generation))changeReaderMode("legacy",false,"");}).show();
    }
    private void readerModeRow(String name,String description,String mode,String selected){
        LinearLayout r=row(name,description);boolean checked=mode.equals(selected);ImageView radio=new ImageView(context);radio.setImageDrawable(new Mark(checked?4:3,checked?accent:ink));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(28),dp(36));lp.leftMargin=dp(12);r.addView(radio,lp);
        r.setBackground(ripple(0));r.setFocusable(true);r.setContentDescription(name+(checked?", selected":""));r.setOnClickListener(v->{if(!checked)selectReaderMode(mode);});rule();
    }
    private void readerCompatibility(){
        start("readercompat","Okuyucu uyumluluğu");note("Loading reader mode and current source…");final long generation=pageGeneration;Context app=activity.getApplicationContext();final Context application=app==null?activity:app;
        final VehicleDataStore.Access access=VehicleDataStore.access(application);final long requestedEpoch=access.epoch;
        readerWorker.execute(()->{
            String mode="auto",error=null,candidateText=null,selection=null,identity=null;long deadline=0;boolean candidate=false;
            try{
                JSONObject config=VehicleReaderSettings.view(application),status=VehicleDataStore.status(application);mode=config.getString("mode");
                if(VehicleDataText.confirmable(status,config)){
                    JSONObject source=status.getJSONObject("source");selection=source.getString("selection");identity=status.optString("source_identity","");deadline=status.optLong("valid_until_elapsed_ms",0);candidate=true;candidateText=VehicleDataText.candidate(status);
                }
            }catch(Exception unavailable){error="Reader settings are unavailable. Check Automatic reader, then try again.";}
            final String selected=mode,problem=error,readings=candidateText,chosen=selection,sourceIdentity=identity;final long validUntil=deadline;final boolean offerConfirmation=candidate;
            readerMain.post(()->{
                if(!readerPageAlive(generation))return;if(access.epoch!=requestedEpoch){readerCompatibility();return;}body.removeAllViews();
                readerModeRow("Otomatik CAN/CANFD","Önerilen. Protokole özgü belgelenmiş adresleri kullanır; bilinmeyen protokol desteklenmez.","auto",selected);
                readerModeRow("Yazılım algılama (gelişmiş)","Yazılım tanımlı adresleri ve dikkatli yedek keşfini etkinleştirin.","legacy",selected);
                action("Tekrar algıla","Önbelleğe alınan adresleri ve önceki onayı sil",()->changeReaderMode(selected,true,""));
                note("Only reader addresses are cached. No battery readings are kept in this compatibility cache. Changing mode or detecting again withdraws old EV data and starts a new reader session.");
                if(problem!=null)note(problem);
                if(offerConfirmation&&candidateLive(access,requestedEpoch,sourceIdentity,validUntil)){
                    TextView candidateView=text("Doğrulanmamış veriler\n"+readings,24,ink);candidateView.setPadding(dp(18),dp(12),dp(18),dp(18));body.addView(candidateView);
                    LinearLayout confirm=action("Verileri onayla","Önce şarj ve menzilin gösterge paneliyle eşleştiğini kontrol edin",()->{
                        if(!readerPageAlive(generation)||!candidateLive(access,requestedEpoch,sourceIdentity,validUntil)){Toast.makeText(activity,"Bu veriler artık güncel değil. Yenileyip tekrar kontrol edin.",Toast.LENGTH_LONG).show();if(readerPageAlive(generation))readerCompatibility();return;}
                        changeReaderMode(selected,false,chosen);
                    });
                    Runnable expire=()->{if(readerPageAlive(generation)&&!candidateLive(access,requestedEpoch,sourceIdentity,validUntil)){confirm.setEnabled(false);candidateView.setText("Aday veriler artık güncel değil. Yeni bir örneği kontrol etmek için yenileyin.");}};
                    candidateView.getViewTreeObserver().addOnPreDrawListener(()->{if(readerPageAlive(generation)&&!candidateLive(access,requestedEpoch,sourceIdentity,validUntil)&&confirm.isEnabled()){expire.run();return false;}return true;});
                    readerMain.postDelayed(expire,Math.max(1,validUntil-SystemClock.elapsedRealtime()+1));
                }else if("legacy".equals(selected))note("Fallback charge and range are unconfirmed until a fresh sample is checked here. They are not used for EV routes before confirmation.");
                note("The reader samples once per minute. Values older than 90 seconds are hidden. This mode only reads vehicle data; it does not change the car.");
                action("Yenile",null,this::readerCompatibility);
            });
        });
    }
    private static String powertrainLabel(VehiclePowertrain type){
        switch(type){
            case BEV:return "BEV — Tam elektrikli";
            case PHEV:return "PHEV — Şarj edilebilir hibrit";
            case HEV:return "HEV — Şarj edilmeyen hibrit";
            default:return "Bilinmiyor / araç tipini değiştirmeden bırak";
        }
    }
    private static String powertrainDetail(VehiclePowertrain type){
        switch(type){
            case BEV:return "Yalnızca elektrik. Canlı batarya rota verisi isteğe bağlıdır.";
            case PHEV:return "Benzin ve elektrik, harici şarj portu bulunur. Yalnızca batarya tahminlerimizi etkinleştirmez.";
            case HEV:return "Harici şarj portu yoktur. Şarj durakları ve batarya rota modelimiz kapalı kalır.";
            default:return "Haritalar'a araç kimliği sağlanmaz.";
        }
    }
    private static String connectionDetail(VehicleEvConnection connection){
        if(connection==VehicleEvConnection.GBT_CCS2)return "GB/T DC and CCS2 stations; only with a compatible CCS2 adapter";
        if(connection==VehicleEvConnection.GBT)return "GB/T DC stations";
        if(connection==VehicleEvConnection.CCS2)return "CCS2 stations";
        if(connection==VehicleEvConnection.CCS1)return "CCS1 stations";
        return "Select only plugs usable by your car; an adapter is never assumed. AC station matching still needs a road test.";
    }
    private void evProfile(){
        start("evprofile","Araç profili");
        final VehicleEvProfile.Profile profile=VehicleEvProfile.current();
        note("Enter the actual vehicle type and charging connection. The reader does not identify usable battery capacity or all compatible plugs. Save confirms your selection; no automatic type detection is performed in Maps.");
        LinearLayout entry=new LinearLayout(context);entry.setOrientation(LinearLayout.VERTICAL);entry.setPadding(dp(18),dp(12),dp(18),dp(12));
        entry.addView(text("Araç adı (isteğe bağlı)",28,ink));
        EditText model=new EditText(context);model.setTextColor(ink);model.setTextSize(28);model.setSingleLine(true);model.setHint("Araç adınız");model.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(80)});
        if(profile!=null)model.setText(profile.model);model.setContentDescription("Optional owner entered vehicle name");entry.addView(model);
        note("This name is shared with Google Maps Connected cars when Maps next syncs vehicle information. An empty name keeps the standard name. The registration code stays the same.");
        TextView capacityLabel=text("Kullanılabilir batarya kapasitesi (kWh)",28,ink);entry.addView(capacityLabel);
        EditText capacity=new EditText(context);capacity.setTextColor(ink);capacity.setTextSize(28);capacity.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        if(profile!=null&&profile.capacityWh>0)capacity.setText(java.math.BigDecimal.valueOf(profile.capacityWh,3).stripTrailingZeros().toPlainString());capacity.setContentDescription("Owner entered usable battery capacity in kWh");entry.addView(capacity);body.addView(entry);rule();
        final VehiclePowertrain[] selectedType={profile!=null&&profile.typeConfirmed?profile.powertrain:VehiclePowertrain.UNKNOWN};
        final String[] selected={profile==null?VehicleEvConnection.UNKNOWN:profile.connector};
        final VehiclePowertrain[] types={VehiclePowertrain.UNKNOWN,VehiclePowertrain.BEV,VehiclePowertrain.PHEV,VehiclePowertrain.HEV};
        final ImageView[] typeRadios=new ImageView[types.length];final LinearLayout[] typeRows=new LinearLayout[types.length];
        for(int i=0;i<types.length;i++){
            LinearLayout r=row(powertrainLabel(types[i]),powertrainDetail(types[i]));typeRows[i]=r;
            ImageView radio=new ImageView(context);typeRadios[i]=radio;LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(28),dp(36));lp.leftMargin=dp(12);r.addView(radio,lp);
            r.setBackground(ripple(0));r.setFocusable(true);rule();
        }
        LinearLayout routeRow=row("BEV rotaları için canlı batarya verilerini kullan","Yalnızca BEV. Güncel güvenilir şarj ve elektrik menzili gerektirir.");
        MapsSwitch routing=new MapsSwitch(context);routing.setChecked(profile!=null&&profile.enabled);routing.setContentDescription("Use live battery data for BEV routes");
        LinearLayout.LayoutParams routeLp=new LinearLayout.LayoutParams(dp(64),dp(48));routeLp.leftMargin=dp(12);routeRow.addView(routing,routeLp);routeRow.setBackground(ripple(0));routeRow.setOnClickListener(v->{if(routing.isEnabled())routing.performClick();});rule();
        java.util.LinkedHashSet<VehicleEvConnection> allConnections=new java.util.LinkedHashSet<>();
        java.util.Collections.addAll(allConnections,VehicleEvConnection.choices(VehiclePowertrain.BEV));
        java.util.Collections.addAll(allConnections,VehicleEvConnection.choices(VehiclePowertrain.PHEV));
        final VehicleEvConnection[] connections=allConnections.toArray(new VehicleEvConnection[0]);
        final ImageView[] radios=new ImageView[connections.length];final LinearLayout[] rows=new LinearLayout[connections.length];final View[] lines=new View[connections.length];
        for(int i=0;i<connections.length;i++){
            final VehicleEvConnection connection=connections[i];
            LinearLayout r=row(connection.label,connectionDetail(connection));rows[i]=r;
            ImageView radio=new ImageView(context);radios[i]=radio;LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(28),dp(36));lp.leftMargin=dp(12);r.addView(radio,lp);
            r.setBackground(ripple(0));r.setFocusable(true);rule();lines[i]=body.getChildAt(body.getChildCount()-1);
        }
        Runnable update=()->{
            VehiclePowertrain type=selectedType[0];
            boolean bev=type==VehiclePowertrain.BEV;
            routing.setEnabled(bev);routeRow.setEnabled(bev);if(!bev)routing.setChecked(false);
            capacityLabel.setText(bev?"Usable battery capacity (kWh)":"Usable battery capacity (kWh, optional)");
            for(int i=0;i<types.length;i++){boolean chosen=types[i]==type;typeRadios[i].setImageDrawable(new Mark(chosen?4:3,chosen?accent:ink));typeRows[i].setContentDescription(powertrainLabel(types[i])+(chosen?", selected":""));}
            for(int i=0;i<connections.length;i++){
                boolean visible=type==VehiclePowertrain.BEV||type==VehiclePowertrain.PHEV;
                visible=visible&&connections[i].supportsIdentity(type);
                rows[i].setVisibility(visible?View.VISIBLE:View.GONE);lines[i].setVisibility(visible?View.VISIBLE:View.GONE);
                boolean chosen=connections[i].id.equals(selected[0]);radios[i].setImageDrawable(new Mark(chosen?4:3,chosen?accent:ink));rows[i].setContentDescription(connections[i].label+(chosen?", selected":""));
            }
        };
        for(int i=0;i<types.length;i++){
            final VehiclePowertrain type=types[i];typeRows[i].setOnClickListener(v->{
                selectedType[0]=type;
                VehicleEvConnection current=VehicleEvConnection.get(selected[0]);
                if(type==VehiclePowertrain.HEV)selected[0]="NONE";
                else if(type==VehiclePowertrain.UNKNOWN||current==null||!current.supportsIdentity(type))selected[0]=VehicleEvConnection.UNKNOWN;
                update.run();
            });
        }
        for(int i=0;i<connections.length;i++){
            final VehicleEvConnection connection=connections[i];rows[i].setOnClickListener(v->{selected[0]=connection.id;update.run();});
        }
        update.run();
        note("PHEV: Maps receives a petrol + electric vehicle identity and the selected compatible plugs. Battery capacity is stored for your profile; our combined fuel/battery forecast and automatic charging-stop model are not implemented. AC station matching and hybrid routing still need testing. The BYD charge and electric-range display remains available.");
        action("Araç profilini kaydet","Yukarıda seçilen tipi ve bağlantıyı onaylayın",()->{
            try{
                final String name=VehicleEvProfile.validModel(model.getText().toString());final VehiclePowertrain type=selectedType[0];final boolean confirmed=type!=VehiclePowertrain.UNKNOWN;
                final int wh=VehicleEvProfile.capacityWh(capacity.getText().toString(),type==VehiclePowertrain.BEV);
                final String mode=type==VehiclePowertrain.UNKNOWN?VehicleEvConnection.UNKNOWN:type==VehiclePowertrain.HEV?"NONE":selected[0];final boolean enabled=type==VehiclePowertrain.BEV&&routing.isChecked();
                // Validate before invalidating a previously working profile.
                new VehicleEvProfile.Profile(name,wh,mode,type,confirmed,enabled,0);
                final boolean identityChanged=profile==null||profile.typeConfirmed!=confirmed||profile.powertrain!=type||!profile.connector.equals(mode);
                VehicleEvBridge.configure(activity,name,wh,mode,type,confirmed,enabled,()->{
                    VehicleEvProfile.Profile saved=VehicleEvProfile.current();
                    if(identityChanged&&saved!=null&&saved.model.equals(name)&&saved.capacityWh==wh&&saved.powertrain==type&&saved.connector.equals(mode))
                        Toast.makeText(activity,"Profil kaydedildi. Değişiklikleri uygulamak için Haritalar'ı zorla durdurup tekrar açın.",Toast.LENGTH_LONG).show();
                    if(dialog.isShowing()&&"evprofile".equals(page))evProfile();
                });
            }catch(IllegalArgumentException invalid){Toast.makeText(activity,invalid.getMessage(),Toast.LENGTH_LONG).show();}
        });
        if(profile!=null&&profile.enabled)action("BEV rota verilerini devre dışı bırak",null,()->VehicleEvBridge.configure(activity,profile.model,profile.capacityWh,profile.connector,profile.powertrain,profile.typeConfirmed,false,()->{if(dialog.isShowing()&&"evprofile".equals(page))evProfile();}));
        note("A change of vehicle type or charging connection requires force stop in Android app settings, then reopen Maps. Home and reopen is not a full restart. No restart is performed automatically. Changing the name, capacity or live-data switch alone does not change the vehicle identity.");
        note(VehicleEvBridge.description());
    }
    private void vehicle(){
        start("vehicle","BYD araç verileri");
        note("Read battery charge and electric range with the optional local ADB reader. Enable Automatic reader for startup and recovery without a computer.");
        vehicleDataToggle();
        action("Araç profili",VehicleEvBridge.profileSummary(),this::evProfile);
        action("Otomatik okuyucu",VehicleAutoReader.description(),this::autoReader);
        action("Okuyucu uyumluluğu",VehicleReaderSettings.summary(),this::readerCompatibility);
        TextView status=text("ADB okuyucu kontrol ediliyor…",24,ink);status.setPadding(dp(18),dp(12),dp(18),dp(18));body.addView(status);
        action("Yenile",null,this::vehicle);
        note("Use BYD vehicle data and Automatic reader are separate opt-ins. Sampling is once per minute; values expire after 90 seconds. For a confirmed BEV with live route data enabled, fresh trusted readings are sent to Maps. PHEV and HEV do not use our battery-only forecast. Advanced fallback readings require confirmation against the instrument cluster first. Unknown CAN protocol stays unsupported in Automatic mode. Battery capacity is entered by the owner; remaining energy and distance consumption used by Maps are estimates. The raw remaining-energy signal has no verified unit. Charging power is unknown. Stale data withdraws the battery prediction.");
        VehicleDataPage.bind(activity,status);
    }
    private void hud(){
        start("hud","HUD çıkışı");toggle("Bağımsız HUD","Bu yazılımda bağımsız HUD'u deneyin (deneysel)","hud_untested_firmware",ClusterSettings.untestedHud());
        note("Experimental on this firmware. First turn off HUD output in OpenBYD or any other HUD app. Restart Maps to apply.");
        note(BydGuidance.outputDescription());action("Haritalar'ı Yeniden Başlat",null,()->ClusterSettings.restartMaps(activity));
    }
    /** Native car-style toggle, with the full row as a touch target and standard switch accessibility. */
    private final class MapsSwitch extends CompoundButton {
        final Paint paint=new Paint(3);
        MapsSwitch(Context c){super(c);setButtonDrawable((Drawable)null);setBackground(ripple(28));setFocusable(true);}
        protected void onDraw(Canvas canvas){
            super.onDraw(canvas);canvas.save();canvas.translate((getWidth()-dp(64))/2f,(getHeight()-dp(48))/2f);canvas.scale(dp(64)/64f,dp(48)/48f);
            paint.setStrokeWidth(2);paint.setStyle(isChecked()?Paint.Style.FILL:Paint.Style.STROKE);paint.setColor(isChecked()?accent:ink);canvas.drawRoundRect(6,8,58,40,16,16,paint);
            paint.setStyle(Paint.Style.FILL);paint.setColor(isChecked()?surface:ink);canvas.drawCircle(isChecked()?42:22,24,isChecked()?11.5f:8,paint);canvas.restore();
        }
        public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info){super.onInitializeAccessibilityNodeInfo(info);info.setClassName("android.widget.Switch");info.setCheckable(true);info.setChecked(isChecked());}
    }
    private static final class Mark extends Drawable {
        final int kind,colour;final Paint paint=new Paint(3);
        Mark(int kind,int colour){this.kind=kind;this.colour=colour;}
        public void draw(Canvas canvas){Rect b=getBounds();canvas.save();canvas.translate(b.left,b.top);canvas.scale(b.width()/32f,b.height()/32f);paint.setColor(colour);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2.6f);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);Path p=new Path();
            if(kind==0){p.moveTo(22,7);p.lineTo(12,16);p.lineTo(22,25);canvas.drawPath(p,paint);canvas.drawLine(12,16,28,16,paint);}
            else if(kind==1){canvas.drawLine(8,8,24,24,paint);canvas.drawLine(24,8,8,24,paint);}
            else if(kind==2){p.moveTo(12,9);p.lineTo(19,16);p.lineTo(12,23);canvas.drawPath(p,paint);}
            else{canvas.drawCircle(16,16,11,paint);if(kind==4){paint.setStyle(Paint.Style.FILL);canvas.drawCircle(16,16,6,paint);}}
            canvas.restore();
        }
        public void setAlpha(int alpha){paint.setAlpha(alpha);}public void setColorFilter(ColorFilter f){paint.setColorFilter(f);}public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    }
}
