package local.maps.cluster;

import android.app.*;
import android.content.*;
import android.content.res.Configuration;
import android.graphics.*;
import android.hardware.display.DisplayManager;
import android.os.*;
import android.util.Log;
import android.view.*;
import java.lang.reflect.*;
import java.util.*;

/** Hosts AAOS's native cluster Activity on BYD's shared presentation display. */
public final class ClusterHost implements DisplayManager.DisplayListener {
    private static final String TAG="MapsClusterTest";
    private static final String NATIVE="com.google.android.apps.gmm.car.embedded.auxiliarymap.EmbeddedClusterActivity";
    private static ClusterHost instance;
    private final Application app;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final DisplayManager displays;
    private Activity cluster;
    private Presentation displayPresence;
    private final Instrumentation lifecycle=new Instrumentation();
    private boolean embeddedCreated,embeddedStarted,embeddedResumed,closing;
    private int displayId=-1;
    private boolean launchPending,mainSeen;
    private long launchAt;
    public static synchronized void install(Application app) {
        if(instance!=null||!app.getPackageName().equals(Application.getProcessName()))return;
        try {
            ClusterSettings.init(app);
            Diagnostics.install(app);
            AccountAccess.install(app);
            BydGuidance.install(app);
            AppUpdater.install(app);
            instance=new ClusterHost(app);
        } catch(Throwable e){Log.e(TAG,"AAOS cluster install",e);}
    }
    private int appliedNight = -1;
    private String themeState = "";
    private android.location.Location themeLocation;
    private long themeLocationAt;
    /** Night bits for the cluster projection: the centre screen's theme by default, or forced light/dark. */
    private volatile String themeReason = "starting";
    static String themeDescription() {
        ClusterHost host = instance;
        if (host == null || host.appliedNight < 0) return "Cluster map: not shown yet.";
        return "Cluster map now: " + (host.appliedNight == Configuration.UI_MODE_NIGHT_NO ? "light" : "dark") + " (" + host.themeReason + ").";
    }
    private int clusterNight() {
        ClusterSettings.State s = ClusterSettings.state();
        int theme = s == null ? ClusterSettings.THEME_AUTO : s.clusterTheme;
        if (theme == ClusterSettings.THEME_LIGHT) { themeReason = "forced light"; return Configuration.UI_MODE_NIGHT_NO; }
        if (theme == ClusterSettings.THEME_DARK) { themeReason = "forced dark"; return Configuration.UI_MODE_NIGHT_YES; }
        if (theme == ClusterSettings.THEME_AUTO) {
            java.time.LocalTime now = java.time.LocalTime.now();
            android.location.Location where = themeLocation();
            boolean night = ScreenTheme.night(ScreenTheme.MODE_AUTO, System.currentTimeMillis(), now.getHour() * 60 + now.getMinute(), 0, 0,
                where == null ? null : where.getLatitude(), where == null ? null : where.getLongitude());
            themeReason = where == null ? "no position yet: night 18:00-06:00" : (night ? "after sunset" : "after sunrise");
            String state = "auto night=" + night + " location=" + (where != null);
            if (!state.equals(themeState)) { themeState = state; Log.i(TAG, "cluster theme " + state); }
            return night ? Configuration.UI_MODE_NIGHT_YES : Configuration.UI_MODE_NIGHT_NO;
        }
        try {
            // Maps overrides night mode in every Configuration of its process; UiModeManager reports BYD's setting.
            android.app.UiModeManager manager = (android.app.UiModeManager) app.getSystemService(Context.UI_MODE_SERVICE);
            int mode = manager.getNightMode();
            if (mode < ScreenTheme.MODE_AUTO || mode > ScreenTheme.MODE_CUSTOM) return keepTheme();
            java.time.LocalTime now = java.time.LocalTime.now();
            int start = 22 * 60, end = 6 * 60;
            if (mode == ScreenTheme.MODE_CUSTOM && Build.VERSION.SDK_INT >= 31) {
                start = manager.getCustomNightModeStart().getHour() * 60 + manager.getCustomNightModeStart().getMinute();
                end = manager.getCustomNightModeEnd().getHour() * 60 + manager.getCustomNightModeEnd().getMinute();
            }
            android.location.Location where = mode == ScreenTheme.MODE_AUTO ? themeLocation() : null;
            boolean night = ScreenTheme.night(mode, System.currentTimeMillis(), now.getHour() * 60 + now.getMinute(), start, end,
                where == null ? null : where.getLatitude(), where == null ? null : where.getLongitude());
            String state = "mode=" + mode + " night=" + night + (mode == ScreenTheme.MODE_AUTO ? " location=" + (where != null) : "");
            themeReason = "head unit " + (mode == ScreenTheme.MODE_NO ? "light" : mode == ScreenTheme.MODE_YES ? "dark" : mode == ScreenTheme.MODE_CUSTOM ? "scheduled" : "automatic");
            if (!state.equals(themeState)) { themeState = state; Log.i(TAG, "centre screen theme " + state); }
            return night ? Configuration.UI_MODE_NIGHT_YES : Configuration.UI_MODE_NIGHT_NO;
        } catch (Exception e) { Log.w(TAG, "centre screen theme", e); themeReason = "head unit theme unavailable"; return keepTheme(); }
    }
    private int keepTheme() { return appliedNight > 0 ? appliedNight : Configuration.UI_MODE_NIGHT_YES; }
    /** Local only, never logged or reported: the twilight calculation needs an approximate position. */
    private android.location.Location themeLocation() {
        long now = SystemClock.elapsedRealtime();
        if (themeLocation != null && now - themeLocationAt < 600000) return themeLocation;
        try {
            android.location.LocationManager locations = (android.location.LocationManager) app.getSystemService(Context.LOCATION_SERVICE);
            android.location.Location best = null;
            for (String provider : locations.getProviders(true)) {
                android.location.Location last = locations.getLastKnownLocation(provider);
                if (last != null && (best == null || last.getTime() > best.getTime())) best = last;
            }
            if (best != null) { themeLocation = best; themeLocationAt = now; }
        } catch (SecurityException e) { /* Without location, ScreenTheme uses its fixed schedule. */ }
        return themeLocation;
    }
    private static java.lang.ref.WeakReference<Object> viewportManager = new java.lang.ref.WeakReference<>(null);
    private static java.lang.ref.WeakReference<View> clusterRoot = new java.lang.ref.WeakReference<>(null);
    private static java.lang.ref.WeakReference<Object> layerController=new java.lang.ref.WeakReference<>(null);
    private static volatile float appliedPointerY = Float.NaN;
    public static boolean isClusterViewport(Object manager) {
        return instance != null && instance.cluster != null && manager != null && viewportManager.get() == manager;
    }
    public static float clusterPointerY(Object manager, float original) {
        ClusterSettings.State state = ClusterSettings.state();
        return isClusterViewport(manager) && state != null
            ? state.pointerVertical / 50f : original;
    }
    public static float applyClusterPointerY(Object manager, float original) {
        float y = clusterPointerY(manager, original);
        if (viewportManager.get() == manager) appliedPointerY = y;
        return y;
    }
    static void refreshClusterCamera() {
        Object manager = viewportManager.get();
        if (instance == null || instance.cluster == null || manager == null) return;
        try { manager.getClass().getMethod("q").invoke(manager); }
        catch (Exception e) { Log.w(TAG, "cluster camera refresh", e); }
        recordClusterViewport();
    }
    private static final Runnable viewportReport = () -> {
        Object manager = viewportManager.get();
        if (manager == null || instance == null) return;
        try {
            Object viewport = manager.getClass().getMethod("b").invoke(manager);
            Object bounds = manager.getClass().getMethod("c").invoke(manager);
            Object anchor = manager.getClass().getMethod("f").invoke(manager);
            ClusterSettings.State state = ClusterSettings.state();
            String result = "cardVisible=" + state.visible + " side=" + state.side + " size=" + state.size
                + " satellite=" + state.satellite + " margin=" + state.margin + " vertical=" + state.vertical + " theme=" + state.clusterTheme + " night=" + instance.appliedNight + " logo=" + state.logoPosition
                + " footer=" + state.visible + " mapViewport=" + viewport + " baseBounds=" + bounds
                + " pointerOffset=" + state.pointerVertical + " cameraAnchor=" + anchor + " liveCameraY=" + appliedPointerY
                + " appLocale=" + instance.app.getResources().getConfiguration().getLocales().toLanguageTags()
                + " clusterLocale=" + (clusterRoot.get() == null ? "unavailable" : clusterRoot.get().getResources().getConfiguration().getLocales().toLanguageTags());
            Log.i(TAG, result);
            try (java.io.FileOutputStream out = new java.io.FileOutputStream(new java.io.File(instance.app.getExternalFilesDir(null), "cluster-viewport.txt"))) {
                out.write(result.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
            View root = clusterRoot.get();
            if (root != null) {
                StringBuilder tree = new StringBuilder();
                describeView(root, tree, 0);
                try (java.io.FileOutputStream out = new java.io.FileOutputStream(new java.io.File(instance.app.getExternalFilesDir(null), "cluster-views.txt"))) {
                    out.write(tree.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                }
            }
            instance.captureClusterFrame();
        } catch (Exception e) { Log.w(TAG, "viewport snapshot unavailable", e); }
    };
    private static boolean isRouteOverlayKey(Object key) {
        String name = String.valueOf(key);
        return "NavCardConstraints".equals(name) || "StatusPanel".equals(name);
    }
    /** Called by ProjectedClusterActivity, so the main map's manager is never filtered. */
    public static void registerClusterViewport(Object manager) {
        if (instance == null || instance.cluster == null) return;
        viewportManager = new java.lang.ref.WeakReference<>(manager);
        try {
            boolean changed = false;
            for (String field : new String[]{"m", "q"}) {
                Field f=manager.getClass().getDeclaredField(field); f.setAccessible(true);
                Map<?,?> constraints = (Map<?,?>)f.get(manager);
                Iterator<?> keys = constraints.keySet().iterator();
                while (keys.hasNext()) if (isRouteOverlayKey(keys.next())) { keys.remove(); changed = true; }
            }
            if (changed) manager.getClass().getMethod("q").invoke(manager);
            Log.i(TAG, "cluster viewport registered; route card and StatusPanel camera constraints disabled");
        } catch (Exception e) { Log.w(TAG, "cluster viewport registration", e); }
        instance.main.post(ClusterHost::refreshClusterCamera);
        recordClusterViewport();
    }
    public static boolean ignoreRouteCardViewport(Object manager, Object key) {
        if (instance == null || instance.cluster == null || viewportManager.get() != manager || !isRouteOverlayKey(key)) return false;
        if (reportedViewportKeys.add(String.valueOf(key))) {
            Log.i(TAG, "cluster camera constraint ignored: " + key);
        }
        recordClusterViewport();
        return true;
    }
    private static final Set<String> reportedViewportKeys = new HashSet<>();
    static void recordClusterViewport() {
        if (instance == null) return;
        instance.main.removeCallbacks(viewportReport);
        instance.main.postDelayed(viewportReport, 200);
    }
    public static void inspectClusterRoot(View root) {
        clusterRoot = new java.lang.ref.WeakReference<>(root);
        CardPlacement.attachRoot(root);
        Runnable inspect = new Runnable() {
            int remaining = 20;
            public void run() {
                try {
                    StringBuilder tree = new StringBuilder();
                    describeView(root, tree, 0);
                    java.io.File file = new java.io.File(root.getContext().getExternalFilesDir(null), "cluster-views.txt");
                    try (java.io.FileOutputStream out = new java.io.FileOutputStream(file)) { out.write(tree.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)); }
                } catch (Exception e) { Log.w(TAG, "view inspection", e); }
                if (root.isAttachedToWindow() && --remaining > 0) root.postDelayed(this, 3000);
            }
        };
        root.postDelayed(inspect, 5000);
    }
    private static void describeView(View view, StringBuilder tree, int depth) {
        if (depth > 30) return;
        String id = Integer.toHexString(view.getId());
        if ((view.getId() >>> 24) != 0 && view.getId() != View.NO_ID)
            try { id = view.getResources().getResourceEntryName(view.getId()); } catch (Exception ignored) {}
        int[] xy = new int[2]; view.getLocationOnScreen(xy);
        for (int i=0; i<depth; i++) tree.append(" ");
        tree.append(view.getClass().getName()).append(" id=").append(id)
            .append(" xy=").append(xy[0]).append(',').append(xy[1])
            .append(" size=").append(view.getWidth()).append('x').append(view.getHeight())
            .append(" visible=").append(view.getVisibility()).append(" translation=").append(view.getTranslationX())
            .append(" translationY=").append(view.getTranslationY()).append(" scale=").append(view.getScaleX()).append('\n');
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup)view;
            for (int i=0; i<group.getChildCount(); i++) describeView(group.getChildAt(i), tree, depth+1);
        }
    }
    public static void positionTurnCard(View card) {
        if (instance != null && instance.cluster != null) CardPlacement.install(card);
    }

    private ClusterHost(Application app) {
        this.app=app;displays=(DisplayManager)app.getSystemService(Context.DISPLAY_SERVICE);
        displays.registerDisplayListener(this,main);
        app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks(){
            public void onActivityPreCreated(Activity a,Bundle b){
                if(a.getClass().getName().equals(NATIVE)) {
                    cluster=a;launchPending=false;displayId=a.getDisplay().getDisplayId();
                    a.getWindow().addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                }
            }
            public void onActivityCreated(Activity a,Bundle b){
                if(a.getClass().getName().equals(NATIVE)) {
                    cluster=a;launchPending=false;
                    View root=a.getWindow().getDecorView();
                    root.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                    inspectClusterRoot(root);
                    Log.i(TAG,"native AAOS cluster created display="+a.getDisplay().getDisplayId());
                } else if(a.getClass().getName().equals("com.google.android.maps.MapsActivity")) {
                    mainSeen=true;main.postDelayed(()->reconcile(),1500);
                }
            }
            public void onActivityDestroyed(Activity a){if(a==cluster&&!closing){cluster=null;clearViewport();main.postDelayed(()->reconcile(),1000);}}
            public void onActivityStarted(Activity a){}
            public void onActivityResumed(Activity a){}
            public void onActivityPaused(Activity a){}
            public void onActivityStopped(Activity a){}
            public void onActivitySaveInstanceState(Activity a,Bundle b){}
        });
        app.registerComponentCallbacks(new ComponentCallbacks(){
            public void onConfigurationChanged(Configuration c){main.post(()->restartForTheme());}
            public void onLowMemory(){}
        });
        main.postDelayed(new Runnable(){public void run(){reconcile();restartForTheme();recordClusterViewport();main.postDelayed(this,2000);}},3000);
        Log.i(TAG,"installed "+BuildInfo.VERSION+"; native AAOS cluster host");
    }
    private void clearViewport(){layerController=new java.lang.ref.WeakReference<>(null);viewportManager=new java.lang.ref.WeakReference<>(null);clusterRoot=new java.lang.ref.WeakReference<>(null);appliedPointerY=Float.NaN;}
    private void reconcile() {
        if(!mainSeen||closing)return;
        boolean enabled=ClusterSettings.state().mapEnabled&&!new java.io.File(app.getExternalFilesDir(null),"cluster-disabled").exists();
        Display target=null;
        for(Display d:displays.getDisplays())if((d.getName().startsWith("shared_")&&d.getName().contains("XDJAScreenProjection"))||(BuildInfo.VERSION.startsWith("patch-lab.")&&d.getName().startsWith("Overlay #"))){target=d;break;}
        if(!enabled||target==null){closePresence();return;}
        if(cluster!=null&&displayPresence!=null&&displayPresence.isShowing()&&displayId==target.getDisplayId())return;
        if(launchPending||SystemClock.elapsedRealtime()-launchAt<15000)return;
        closePresence();
        launchPending=true;launchAt=SystemClock.elapsedRealtime();displayId=target.getDisplayId();
        final Display selected=target;
        try {
            displayPresence=new Presentation(app,selected){
                protected void onCreate(Bundle saved){
                    super.onCreate(saved);
                    getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.BLACK));
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                    android.widget.FrameLayout holder=new android.widget.FrameLayout(getContext());
                    setContentView(holder);
                    try {long began=SystemClock.uptimeMillis();
                        try{createEmbedded(getContext(),selected,holder);}
                        finally{Log.i("MapsNavStartup","clusterCreate ms="+(SystemClock.uptimeMillis()-began));}}
                    catch(Throwable e){Log.e(TAG,"embedded native cluster",e);main.post(()->closePresence());}
                }
            };
            displayPresence.show();
        } catch(Throwable e){Log.e(TAG,"cluster presentation",e);closePresence();}
        finally{launchPending=false;}
    }
    /** Public Instrumentation attaches the native screen to our permitted Presentation context.
     * Its decor is a child view; no ActivityTaskManager launch on BYD's private display occurs. */
    @SuppressWarnings({"deprecation","unchecked"})
    private void createEmbedded(Context context,Display target,android.widget.FrameLayout holder)throws Exception {
        android.util.DisplayMetrics metrics=new android.util.DisplayMetrics();target.getRealMetrics(metrics);
        Bundle state=new Bundle();state.putParcelable("android.car:activityState.unobscured",new Rect(32,32,metrics.widthPixels-32,metrics.heightPixels-32));
        Intent intent=new Intent(Intent.ACTION_MAIN).setComponent(new ComponentName(app.getPackageName(),NATIVE))
            .putExtra("android.car.cluster.ClusterActivityState",state)
            .putExtra("com.google.android.gms.car.EXTRA_SHOW_TURN_CARD",true);
        android.content.pm.ActivityInfo info=app.getPackageManager().getActivityInfo(intent.getComponent(),0);
        Class<? extends Activity> type=(Class<? extends Activity>)Class.forName(NATIVE,true,app.getClassLoader());
        cluster=lifecycle.newActivity(type,clusterContext(context),null,app,intent,info,"Instrument cluster",null,null,null);
        cluster.setTheme(info.getThemeResource());
        cluster.getWindow().addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        lifecycle.callActivityOnCreate(cluster,null);embeddedCreated=true;
        lifecycle.callActivityOnPostCreate(cluster,null);
        // Native onStart updates an already-attached decor as a top-level WindowManager view.
        // Start while detached, then host the decor as a child of the Presentation instead.
        lifecycle.callActivityOnStart(cluster);embeddedStarted=true;
        View decor=cluster.getWindow().getDecorView();
        decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        holder.addView(decor,new android.widget.FrameLayout.LayoutParams(-1,-1));
        lifecycle.callActivityOnResume(cluster);embeddedResumed=true;
        registerClusterLayers();
        inspectClusterRoot(decor);
        Log.i(TAG,"native cluster embedded in Presentation display="+target.getDisplayId());
    }
    private void closePresence(){
        if(closing)return;
        closing=true;
        Activity previous=cluster;Presentation window=displayPresence;
        displayPresence=null;
        if(previous!=null){
            if(embeddedResumed)try{lifecycle.callActivityOnPause(previous);}catch(Throwable e){Log.w(TAG,"cluster pause",e);}
            View decor=previous.getWindow().getDecorView();
            if(decor.getParent() instanceof ViewGroup)((ViewGroup)decor.getParent()).removeView(decor);
            if(embeddedStarted)try{lifecycle.callActivityOnStop(previous);}catch(Throwable e){Log.w(TAG,"cluster stop",e);}
            if(embeddedCreated)try{lifecycle.callActivityOnDestroy(previous);}catch(Throwable e){Log.w(TAG,"cluster destroy",e);}
        }
        cluster=null;embeddedCreated=embeddedStarted=embeddedResumed=false;clearViewport();
        if(window!=null)try{window.dismiss();}catch(RuntimeException ignored){}
        closing=false;
    }
    private void registerClusterLayers(){
        try{
            Object delegate=cluster.getClass().getField("r").get(cluster);
            Object manager=delegate.getClass().getField("n").get(delegate);
            Object layers=manager.getClass().getMethod("b").invoke(manager);
            if(layers.getClass().getName().equals("nyk"))layers=layers.getClass().getField("a").get(layers);
            if(!layers.getClass().getName().equals("nym"))throw new IllegalStateException("Unexpected cluster layer controller");
            layerController=new java.lang.ref.WeakReference<>(layers);
            refreshClusterLayers();
        }catch(Exception e){Log.w(TAG,"cluster layer controller",e);}
    }
    static void refreshClusterLayers(){
        if(instance==null)return;
        instance.main.post(()->{
            Object layers=layerController.get();if(layers==null||instance.cluster==null)return;
            try{layers.getClass().getMethod("f",boolean.class).invoke(layers,true);}
            catch(Exception e){Log.w(TAG,"cluster layer refresh",e);}
            recordClusterViewport();
        });
    }
    @SuppressWarnings({"rawtypes","unchecked"})
    public static void configureClusterLayers(Object controller,Set layers){
        if(instance==null||instance.cluster==null||layerController.get()!=controller)return;
        try{
            for(Object value:Class.forName("nyo",false,instance.app.getClassLoader()).getEnumConstants()){
                String name=((Enum)value).name();
                if(name.equals("SATELLITE")){if(ClusterSettings.state().satellite)layers.add(value);else layers.remove(value);}
                else if((name.equals("TERRAIN")||name.equals("THREE_DIMENSIONAL"))&&ClusterSettings.state().satellite)layers.remove(value);
            }
            Log.i(TAG,"cluster satellite layer="+ClusterSettings.state().satellite);
        }catch(Exception e){Log.w(TAG,"cluster satellite",e);}
    }
    public static boolean nativeClusterActive(){return instance!=null&&instance.cluster!=null;}
    public static boolean nativeNight(){
        if(instance==null)return true;
        instance.appliedNight=instance.clusterNight();
        return instance.appliedNight==Configuration.UI_MODE_NIGHT_YES;
    }
    public static Context clusterContext(Context base){
        if(instance==null)return base;
        Configuration c=new Configuration(base.getResources().getConfiguration());
        AppLanguage.configure(c);instance.appliedNight=instance.clusterNight();
        c.uiMode=Configuration.UI_MODE_TYPE_CAR|instance.appliedNight;
        return new ClusterContext(base,c);
    }
    private static final class ClusterContext extends ContextWrapper {
        private final Context configured;
        ClusterContext(Context base,Configuration configuration){super(base);configured=base.createConfigurationContext(configuration);}
        public android.content.res.Resources getResources(){return configured.getResources();}
        public android.content.res.AssetManager getAssets(){return configured.getAssets();}
        public android.content.res.Resources.Theme getTheme(){return configured.getTheme();}
        public Object getSystemService(String name){
            if(Context.LAYOUT_INFLATER_SERVICE.equals(name))return ((LayoutInflater)super.getSystemService(name)).cloneInContext(this);
            return super.getSystemService(name);
        }
    }
    public static void registerViewportCandidate(Object manager){
        if(instance==null)return;
        try {
            Field f=manager.getClass().getDeclaredField("e");f.setAccessible(true);Context context=(Context)f.get(manager);
            for(int i=0;i<12&&context!=null;i++) {
                if(context instanceof Activity&&context.getClass().getName().equals(NATIVE)) {registerClusterViewport(manager);return;}
                if(!(context instanceof ContextWrapper))return;
                Context next=((ContextWrapper)context).getBaseContext();if(next==context)return;context=next;
            }
        } catch(ReflectiveOperationException e){Log.w(TAG,"viewport owner",e);}
    }
    static void mapSettingChanged(){if(instance!=null)instance.main.post(()->{instance.launchAt=0;instance.reconcile();});}
    static void themeSettingChanged(){if(instance!=null)instance.main.post(()->instance.restartForTheme());}
    private void restartForTheme(){
        if(cluster==null||cluster.isFinishing()||clusterNight()==appliedNight)return;
        closePresence();launchPending=false;launchAt=0;
        main.postDelayed(()->reconcile(),500);
    }
    public void onDisplayAdded(int id){reconcile();}
    public void onDisplayRemoved(int id){reconcile();}
    public void onDisplayChanged(int id){reconcile();}
    private void captureClusterFrame(){
        java.io.File marker=new java.io.File(app.getExternalFilesDir(null),"capture-cluster");
        if(!marker.exists()||cluster==null)return;
        View root=cluster.getWindow().getDecorView();if(root.getWidth()==0||!root.isAttachedToWindow())return;
        if(!marker.delete())return;
        // Explicit local diagnostic marker only: the native dump can contain route details.
        try(java.io.PrintWriter writer=new java.io.PrintWriter(new java.io.File(app.getExternalFilesDir(null),"cluster-native.private.txt"))){
            Object delegate=cluster.getClass().getField("r").get(cluster);
            delegate.getClass().getMethod("kM",String.class,java.io.PrintWriter.class).invoke(delegate,"",writer);
        }catch(Exception e){Log.w(TAG,"native cluster dump",e);}
        Bitmap bitmap=Bitmap.createBitmap(root.getWidth(),root.getHeight(),Bitmap.Config.ARGB_8888);
        try {PixelCopy.request(displayPresence.getWindow(),bitmap,result->{
            try {if(result==PixelCopy.SUCCESS)try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(app.getExternalFilesDir(null),"cluster-frame.png"))){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}
            else Log.w(TAG,"cluster capture result="+result);}catch(Exception e){Log.w(TAG,"cluster capture",e);}finally{bitmap.recycle();}
        },main);}catch(Exception e){bitmap.recycle();Log.w(TAG,"cluster capture request",e);}
        SurfaceView surface=largestSurface(root,null);
        for(View window:android.view.inspector.WindowInspector.getGlobalWindowViews()){
            Display on=window.getDisplay();
            if(on!=null&&on.getDisplayId()==displayId)surface=largestSurface(window,surface);
        }
        if(surface!=null&&surface.getHolder().getSurface().isValid()){
            Bitmap map=Bitmap.createBitmap(surface.getWidth(),surface.getHeight(),Bitmap.Config.ARGB_8888);
            try{PixelCopy.request(surface,map,result->{
                try{if(result==PixelCopy.SUCCESS)try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(app.getExternalFilesDir(null),"cluster-map-surface.png"))){map.compress(Bitmap.CompressFormat.PNG,100,out);}}
                catch(Exception e){Log.w(TAG,"map surface capture",e);}finally{map.recycle();}
            },main);}catch(Exception e){map.recycle();Log.w(TAG,"map surface capture request",e);}
        }
    }
    private static SurfaceView largestSurface(View view,SurfaceView found){
        if(view instanceof SurfaceView&&view.getWidth()>0&&view.getHeight()>0&&(found==null||view.getWidth()*view.getHeight()>found.getWidth()*found.getHeight()))found=(SurfaceView)view;
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++)found=largestSurface(group.getChildAt(i),found);}
        return found;
    }
}
