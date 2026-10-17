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
        MapsSwitch control=switches.get(key);if(control!=null){boolean checked=p.getBoolean(key,false);if(control.isChecked()!=checked)control.setChecked(checked);}
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
        title=text("Instrument cluster",30,ink);title.setSingleLine(true);title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        header.addView(title,new LinearLayout.LayoutParams(0,-1,1));
        header.addView(headerButton(true),new LinearLayout.LayoutParams(dp(72),-1));panel.addView(header,new LinearLayout.LayoutParams(-1,dp(72)));
        View line=new View(context);line.setBackgroundColor(divider);panel.addView(line,new LinearLayout.LayoutParams(-1,dp(2)));
        scroll=new ScrollView(context);scroll.setFillViewport(false);scroll.setClipToPadding(false);scroll.setPadding(0,0,dp(24),0);scroll.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        body=new LinearLayout(context);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(0,0,0,dp(12));scroll.addView(body);
        panel.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));dialog.setContentView(panel);
        dialog.setOnKeyListener((d,key,e)->{if(key!=KeyEvent.KEYCODE_BACK)return false;if(e.getAction()==KeyEvent.ACTION_UP)back();return true;});
        dialog.setOnDismissListener(d->{prefs.unregisterOnSharedPreferenceChangeListener(changes);if(open==this)open=null;});
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
        b.setContentDescription(close?"Close instrument cluster settings":"Back");b.setTooltipText(close?"Close":"Back");b.setOnClickListener(v->{if(close)dialog.dismiss();else back();});return b;
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
        switches.put(key,sw);sw.setOnCheckedChangeListener((b,on)->{if(prefs.getBoolean(key,!on)!=on)prefs.edit().putBoolean(key,on).apply();});
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
    private String theme(int n){return n==ClusterSettings.THEME_AUTO?"Auto":n==ClusterSettings.THEME_SYSTEM?"Match screen":n==ClusterSettings.THEME_LIGHT?"Light":"Dark";}
    private void main(){
        start("main","Instrument cluster");ClusterSettings.State s=ClusterSettings.state();
        toggle("Show cluster map","Show map on instrument cluster","map_enabled",s.mapEnabled);
        toggle("Turn guidance","Send turn guidance to cluster and HUD","vehicle_guidance",s.vehicleGuidance);
        action("Map colours",theme(s.clusterTheme),()->choices("Map colours","cluster_theme",new String[]{"Auto","Match screen","Light","Dark"},new int[]{3,2,1,0},"Auto follows sunrise and sunset at the car's position. Match screen follows the head unit. During navigation, Maps chooses its own colours. "+ClusterHost.themeDescription()));
        toggle("Satellite map","Satellite map on instrument cluster","cluster_satellite",s.satellite);
        action("Map layout",null,this::layout);
        action("Route card",s.visible?s.size+"% · "+(s.side==0?"Left":"Right"):"Off",this::card);
        toggle("Picture-in-picture","Picture-in-picture when leaving Maps","picture_in_picture",s.pictureInPicture);
        if(BydGuidance.untestedHudAvailable())action("HUD output",null,this::hud);
        action("Google account",SignInAccess.description().replace("Google sign-in: ",""),()->{
            if(AccountAccess.restartNeeded(activity))AccountAccess.restart(activity);
            else if(AccountAccess.microg(activity)&&!AccountAccess.granted(activity))AccountAccess.request(activity);
            else SignInAccess.show(activity);
        });
        action("Updates",BuildInfo.VERSION,()->AppUpdater.show(activity));
        action("Support",null,()->Diagnostics.show(activity));
        action("BYD vehicle data","ADB battery and electric range",this::vehicle);
        if(EvTest.available())action("Test EV data","Synthetic values",()->EvTest.show(activity));
        note("Changes are saved automatically.");
    }
    private void layout(){
        start("layout","Map layout");ClusterSettings.State s=ClusterSettings.state();
        action("Google Maps logo",s.logoPosition==0?"Bottom left":"Top right",()->choices("Google Maps logo","logo_position",new String[]{"Bottom left","Top right"},new int[]{0,1},null));
        slider("Pointer vertical offset","pointer_vertical",-25,25,s.pointerVertical);note("Negative moves the pointer up; positive moves it down.");
    }
    private void card(){
        start("card","Route card");ClusterSettings.State s=ClusterSettings.state();
        toggle("Show route card","Route card, arrival time and distance","visible",s.visible);
        action("Card size",s.size+"%",()->choices("Card size","size",new String[]{"50%","75%","100%"},new int[]{50,75,100},null));
        action("Position",s.side==0?"Left":"Right",()->choices("Position","side",new String[]{"Left","Right"},new int[]{0,1},null));
        slider("Edge margin","margin",0,20,s.margin);slider("Card vertical offset","vertical",-25,25,s.vertical);
        note("Negative moves the card up; positive moves it down.");
    }
    private void vehicleDataToggle(){
        LinearLayout r=row("Use BYD vehicle data","Read-only battery and electric range via ADB");
        MapsSwitch control=new MapsSwitch(context);control.setChecked(VehicleDataStore.enabled(activity));
        control.setContentDescription("Use BYD vehicle data");
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(64),dp(48));lp.leftMargin=dp(12);r.addView(control,lp);
        control.setOnCheckedChangeListener((button,on)->{if(control.isEnabled())VehicleDataPage.setEnabled(activity,control,on);});
        VehicleDataPage.bindToggle(activity,control);
        r.setBackground(ripple(0));r.setOnClickListener(v->{if(control.isEnabled())control.performClick();});rule();
    }
    private void autoReader(){
        start("autoreader","Automatic reader");
        note("Start the BYD reader on this head unit after restart or wake, without a computer. BYD vehicle data must also be on. ADB must remain enabled on this head unit.");
        note("Maps creates a private key for this installation. Approve the standard ADB prompt once and select Always allow. No computer key or extra app is used.");
        row("Status",VehicleAutoReader.description());rule();
        boolean on=VehicleAutoReader.enabled();
        action(on?"Disable automatic reader":"Enable automatic reader",on?"Stop automatic startup and its own reader":"Set up local ADB and automatic startup",()->VehicleAutoReader.configure(activity,!on,true,()->{if(dialog.isShowing()&&"autoreader".equals(page))autoReader();}));
        if(on)action("Authorize or retry", "Approve the Maps ADB key on this head unit",()->VehicleAutoReader.configure(activity,true,true,()->{if(dialog.isShowing()&&"autoreader".equals(page))autoReader();}));
        note("Uses only 127.0.0.1:5555. If the head unit blocks boot startup, allow Maps Automotive in its app startup settings. Force-stop requires opening Maps again.");
    }
    private boolean readerPageAlive(long generation){return generation==pageGeneration&&"readercompat".equals(page)&&dialog.isShowing()&&body.isAttachedToWindow()&&!activity.isFinishing()&&!activity.isDestroyed();}
    private static boolean candidateLive(VehicleDataStore.Access access,long epoch,String identity,long deadline){return access.enabled&&access.readerActive&&access.epoch==epoch&&access.sourceKnown&&identity!=null&&!identity.isEmpty()&&identity.equals(access.sourceIdentity)&&deadline>0&&SystemClock.elapsedRealtime()<=deadline;}
    private void changeReaderMode(String mode,boolean redetect,String confirmed){
        start("readercompat","Reader compatibility");note("Updating reader settings…");final long generation=pageGeneration;
        try{VehicleReaderSettings.configure(activity,mode,redetect,confirmed,()->{if(readerPageAlive(generation))readerCompatibility();});}
        catch(IllegalArgumentException invalid){Toast.makeText(activity,"Reader selection changed. Detect again before confirming.",Toast.LENGTH_LONG).show();if(readerPageAlive(generation))readerCompatibility();}
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
        start("readercompat","Reader compatibility");note("Loading reader mode and current source…");final long generation=pageGeneration;Context app=activity.getApplicationContext();final Context application=app==null?activity:app;
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
                readerModeRow("Automatic CAN/CANFD","Recommended. Uses protocol-specific documented addresses; unknown protocol stays unsupported.","auto",selected);
                readerModeRow("Firmware detection (advanced)","Opt in to firmware-defined addresses and conservative fallback discovery.","legacy",selected);
                action("Detect again","Discard cached addresses and previous confirmation",()->changeReaderMode(selected,true,""));
                note("Only reader addresses are cached. No battery readings are kept in this compatibility cache. Changing mode or detecting again withdraws old EV data and starts a new reader session.");
                if(problem!=null)note(problem);
                if(offerConfirmation&&candidateLive(access,requestedEpoch,sourceIdentity,validUntil)){
                    TextView candidateView=text("Unconfirmed readings\n"+readings,24,ink);candidateView.setPadding(dp(18),dp(12),dp(18),dp(18));body.addView(candidateView);
                    LinearLayout confirm=action("Confirm readings","First check that charge and range match the instrument cluster",()->{
                        if(!readerPageAlive(generation)||!candidateLive(access,requestedEpoch,sourceIdentity,validUntil)){Toast.makeText(activity,"These readings are no longer current. Refresh and check again.",Toast.LENGTH_LONG).show();if(readerPageAlive(generation))readerCompatibility();return;}
                        changeReaderMode(selected,false,chosen);
                    });
                    Runnable expire=()->{if(readerPageAlive(generation)&&!candidateLive(access,requestedEpoch,sourceIdentity,validUntil)){confirm.setEnabled(false);candidateView.setText("Candidate readings are no longer current. Refresh to check a new sample.");}};
                    candidateView.getViewTreeObserver().addOnPreDrawListener(()->{if(readerPageAlive(generation)&&!candidateLive(access,requestedEpoch,sourceIdentity,validUntil)&&confirm.isEnabled()){expire.run();return false;}return true;});
                    readerMain.postDelayed(expire,Math.max(1,validUntil-SystemClock.elapsedRealtime()+1));
                }else if("legacy".equals(selected))note("Fallback charge and range are unconfirmed until a fresh sample is checked here. They are not used for EV routes before confirmation.");
                note("The reader samples once per minute. Values older than 90 seconds are hidden. This mode only reads vehicle data; it does not change the car.");
                action("Refresh",null,this::readerCompatibility);
            });
        });
    }
    private void evProfile(){
        start("evprofile","EV routing profile");
        VehicleEvProfile.Profile profile=VehicleEvProfile.current();
        note("Battery capacity is entered by the owner. The car supplies charge and estimated electric range. Charging power and charging time are unknown.");
        row("Vehicle",profile==null?"Not configured":profile.model);rule();
        row("Station connector","CCS2 (car uses GB/T through an adapter)");rule();
        LinearLayout entry=new LinearLayout(context);entry.setOrientation(LinearLayout.VERTICAL);entry.setPadding(dp(18),dp(12),dp(18),dp(12));
        entry.addView(text("Battery capacity (kWh)",28,ink));
        EditText capacity=new EditText(context);capacity.setTextColor(ink);capacity.setTextSize(28);capacity.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        if(profile!=null)capacity.setText(String.format(java.util.Locale.US,"%.1f",profile.capacityWh/1000.0));capacity.setContentDescription("Owner entered battery capacity in kWh");entry.addView(capacity);body.addView(entry);rule();
        action("Save and enable EV routes",null,()->{
            try{double kwh=Double.parseDouble(capacity.getText().toString().trim().replace(',','.'));int wh=(int)Math.round(kwh*1000);if(!Double.isFinite(kwh)||wh<1000||wh>300000)throw new IllegalArgumentException();
                VehicleEvBridge.configure(activity,profile==null?"BYD":profile.model,wh,true,()->{if(dialog.isShowing()&&"evprofile".equals(page))evProfile();});
            }catch(Exception invalid){Toast.makeText(activity,"Enter a battery capacity between 1 and 300 kWh.",Toast.LENGTH_LONG).show();}
        });
        if(profile!=null)action("Disable EV route data",null,()->VehicleEvBridge.configure(activity,profile.model,profile.capacityWh,false,()->{if(dialog.isShowing()&&"evprofile".equals(page))evProfile();}));
        note(VehicleEvBridge.description());
    }
    private void vehicle(){
        start("vehicle","BYD vehicle data");
        note("Read battery charge and electric range with the optional local ADB reader. Enable Automatic reader for startup and recovery without a computer.");
        vehicleDataToggle();
        action("EV routing profile",VehicleEvBridge.profileSummary(),this::evProfile);
        action("Automatic reader",VehicleAutoReader.description(),this::autoReader);
        action("Reader compatibility",VehicleReaderSettings.summary(),this::readerCompatibility);
        TextView status=text("Checking ADB reader…",24,ink);status.setPadding(dp(18),dp(12),dp(18),dp(18));body.addView(status);
        action("Refresh",null,this::vehicle);
        note("Use BYD vehicle data and Automatic reader are separate opt-ins. Sampling is once per minute; values expire after 90 seconds. With the EV routing profile enabled, fresh trusted readings are sent to Maps. Advanced fallback readings require confirmation against the instrument cluster first. Unknown CAN protocol stays unsupported in Automatic mode. Battery capacity is entered by the owner; remaining energy and distance consumption used by Maps are estimates. The raw remaining-energy signal has no verified unit. Charging power is unknown. Stale data withdraws the battery prediction.");
        VehicleDataPage.bind(activity,status);
    }
    private void hud(){
        start("hud","HUD output");toggle("Standalone HUD","Try standalone HUD on this firmware (experimental)","hud_untested_firmware",ClusterSettings.untestedHud());
        note("Experimental on this firmware. First turn off HUD output in OpenBYD or any other HUD app. Restart Maps to apply.");
        note(BydGuidance.outputDescription());action("Restart Maps",null,()->ClusterSettings.restartMaps(activity));
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
