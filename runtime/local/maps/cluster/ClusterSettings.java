package local.maps.cluster;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;

/** Persistent, live settings for our instrument-cluster route overlay. */
public final class ClusterSettings {
    static final class State {
        final boolean visible, mapEnabled, vehicleGuidance, pictureInPicture, satellite, backgroundStart, quickButtons;
        final int size, side, margin, vertical, pointerVertical, pointerHorizontal, clusterTheme, logoPosition;
        State(android.content.SharedPreferences p) {
            mapEnabled=p.getBoolean("map_enabled",true);
            satellite=p.getBoolean("cluster_satellite",false);
            vehicleGuidance=p.getBoolean("vehicle_guidance",true);
            visible=p.getBoolean("visible",false);
            size=Math.max(50,Math.min(100,p.getInt("size",50)));
            side=p.getInt("side",0)==1?1:0;
            margin=Math.max(0,Math.min(20,p.getInt("margin",5)));
            vertical=Math.max(-25,Math.min(25,p.getInt("vertical",0)));
            pointerVertical=Math.max(-25,Math.min(25,p.getInt("pointer_vertical",10)));
            // 0 keeps the native horizontal anchor (the centre of the free viewport).
            pointerHorizontal=Math.max(-25,Math.min(25,p.getInt("pointer_horizontal",0)));
            int theme=p.getInt("cluster_theme",THEME_AUTO);
            clusterTheme=theme==THEME_LIGHT||theme==THEME_SYSTEM||theme==THEME_AUTO?theme:THEME_DARK;
            logoPosition=p.getInt("logo_position",LOGO_BOTTOM_LEFT)==LOGO_TOP_RIGHT?LOGO_TOP_RIGHT:LOGO_BOTTOM_LEFT;
            pictureInPicture=p.getBoolean("picture_in_picture",false);
            backgroundStart=p.getBoolean(ClusterBackgroundStart.KEY,false);
            quickButtons=p.getBoolean("dilink3_quick_buttons",true);
        }
    }
    static final int THEME_DARK=0, THEME_LIGHT=1, THEME_SYSTEM=2, THEME_AUTO=3, LOGO_BOTTOM_LEFT=0, LOGO_TOP_RIGHT=1;
    private static java.lang.ref.WeakReference<Activity> resumedMaps=new java.lang.ref.WeakReference<>(null);
    private static android.content.SharedPreferences preferences;
    private static volatile State current;
    private static final android.content.SharedPreferences.OnSharedPreferenceChangeListener changes=(p,key)->{
        if(AppCorners.KEY.equals(key)){AppCorners.refresh();return;}
        current=new State(p); CardPlacement.refresh();
        if("pointer_vertical".equals(key)||"pointer_horizontal".equals(key))ClusterHost.refreshClusterCamera();
        if("map_enabled".equals(key))ClusterHost.mapSettingChanged();
        if("vehicle_guidance".equals(key))BydGuidance.settingsChanged();
        if("cluster_theme".equals(key))ClusterHost.themeSettingChanged();
        if("cluster_satellite".equals(key))ClusterHost.refreshClusterLayers();
        if("picture_in_picture".equals(key)){Activity a=resumedMaps.get();if(a!=null)applyPictureInPicture(a);}
    };
    /** Maps enters picture-in-picture from onUserLeaveHint only while its pb.l flag (device supports PiP) is set. */
    static void applyPictureInPicture(Activity a) {
        try {
            boolean supported=a.getPackageManager().hasSystemFeature(android.content.pm.PackageManager.FEATURE_PICTURE_IN_PICTURE);
            boolean allow=current!=null&&current.pictureInPicture&&supported;
            if(supported&&android.os.Build.VERSION.SDK_INT>=31)
                a.setPictureInPictureParams(new PictureInPictureParams.Builder().setAutoEnterEnabled(allow).setAspectRatio(new android.util.Rational(16,9)).build());
            if(!allow&&supported&&android.os.Build.VERSION.SDK_INT>=31)
                a.setPictureInPictureParams(new PictureInPictureParams.Builder().setAutoEnterEnabled(false).build());
        } catch(Throwable e){android.util.Log.w("MapsClusterTest","picture-in-picture setting",e);}
    }
    public static void leavingMaps(Activity a) {
        if(!a.getClass().getName().equals("com.google.android.maps.MapsActivity"))return;
        if(current==null||!current.pictureInPicture||a.isFinishing()||a.isInPictureInPictureMode())return;
        if(!a.getPackageManager().hasSystemFeature(android.content.pm.PackageManager.FEATURE_PICTURE_IN_PICTURE))return;
        try {a.enterPictureInPictureMode(new PictureInPictureParams.Builder().setAspectRatio(new android.util.Rational(16,9)).build());}
        catch(RuntimeException e){android.util.Log.w("MapsClusterTest","AAOS PiP",e);}
    }
    static State state(){return current;}
    static boolean quickButtonsEnabled(){State s=current;return s==null||s.quickButtons;}
    static void toggleMap(){preferences.edit().putBoolean("map_enabled",!current.mapEnabled).apply();}
    static boolean untestedHud(){return preferences!=null&&preferences.getBoolean("hud_untested_firmware",false);}

    static int getClusterZoomSteps(){return preferences!=null?preferences.getInt("cluster_zoom_steps",0):0;}
    static void adjustClusterZoomSteps(int delta){
        if(preferences==null)return;
        int current=getClusterZoomSteps();
        int updated=Math.max(-10,Math.min(10,current+delta));
        preferences.edit().putInt("cluster_zoom_steps",updated).apply();
    }
    static void resetClusterZoomSteps(){if(preferences!=null)preferences.edit().putInt("cluster_zoom_steps",0).apply();}

    /** Restarts Maps; a killed guidance session is cleared by the next launch. */
    static void restartMaps(Activity a) {
        Intent launch=a.getPackageManager().getLaunchIntentForPackage(a.getPackageName());
        if(launch==null||launch.getComponent()==null){Toast.makeText(a,"Haritalar'ı tamamen kapatıp tekrar açın.",Toast.LENGTH_LONG).show();return;}
        android.util.Log.i("MapsClusterTest","restarting Maps from settings");
        a.startActivity(Intent.makeRestartActivityTask(launch.getComponent()));
        Runtime.getRuntime().exit(0);
    }
    static void init(Application app) {
        if(preferences!=null)return;
        preferences=app.getSharedPreferences("local_cluster_settings",Context.MODE_PRIVATE);
        current=new State(preferences); preferences.registerOnSharedPreferenceChangeListener(changes);GeminiLaunch.init(app);
        app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks(){
            public void onActivityResumed(Activity a){
                AppCorners.resumed(a);
                if(a.getClass().getName().equals("com.google.android.maps.MapsActivity")) {
                    resumedMaps=new java.lang.ref.WeakReference<>(a);GeminiLaunch.resumed(a);applyPictureInPicture(a);MainUiScale.resumed(a);
                    a.getWindow().getDecorView().post(()->{attach(a);Diagnostics.resumed(a);AccountAccess.resumed(a);SignInAccess.resumed(a);});
                }
            }
            public void onActivityCreated(Activity a,Bundle b){}
            public void onActivityStarted(Activity a){}
            public void onActivityPaused(Activity a){if(a.getClass().getName().equals("com.google.android.maps.MapsActivity")){GeminiLaunch.paused(a);MainUiScale.paused(a);AccountAccess.paused();SignInAccess.paused();}}
            public void onActivityStopped(Activity a){if(a.getClass().getName().equals("com.google.android.maps.MapsActivity"))GeminiLaunch.stopped(a);}
            public void onActivitySaveInstanceState(Activity a,Bundle b){}
            public void onActivityDestroyed(Activity a){GeminiLaunch.paused(a);AppCorners.destroyed(a);MainUiScale.destroyed(a);ClusterMenu.destroyed(a);}
        });
    }
    private static int dp(Context c,int value){return Math.round(value*c.getResources().getDisplayMetrics().density);}
    private static void attach(Activity activity){
        ClusterButtons.attach(activity);
        EvTest.attach(activity);
    }
    static void show(Activity activity){ClusterMenu.show(activity,preferences);}
}

/** 3D Car model on map (Navatars custom chevron integration for BYD). */
final class Navatars {
    static final String KEY = "navatars_3d_car";
    static final String MODEL_KEY = "navatars_model";
    static final String COLOUR_KEY = "navatars_colour";
    static final String TAG = "MapsNavatars";
    static final int GRID = 16;
    static final int CELL = 8;

    static final String[] COLOUR_NAMES = {
        "Orijinal", "Titanyum Gri", "Gümüş", "Beyaz", "Siyah", "Gri", "Mavi", "Kırmızı", "Yeşil"
    };
    static final int[] COLOURS = {
        0x00000000, 0x006E737B, 0x009DA2A8, 0x00E6E7E8, 0x00232427, 0x005F646B, 0x001E4F91, 0x00A01C22, 0x002E6048
    };

    private static boolean enabled;
    private static boolean loggedEnabled;
    private static String lastAnimation;

    public static boolean enabled() {
        if (!loggedEnabled) {
            loggedEnabled = true;
            android.util.Log.i(TAG, "custom chevron " + (enabled ? "on" : "off"));
        }
        return enabled;
    }

    public static boolean enabled(SharedPreferences prefs) {
        return prefs != null && prefs.getBoolean(KEY, true);
    }

    public static int colour(SharedPreferences prefs) {
        int c = prefs != null ? prefs.getInt(COLOUR_KEY, 0) : 0;
        return (c >= 0 && c < COLOURS.length) ? c : 0;
    }

    public static String summary(Context context, SharedPreferences prefs) {
        if (!enabled(prefs)) return "Kapalı";
        List<Model> list = models(context);
        if (list.isEmpty()) return "Açık";
        Model sel = selected(list, prefs);
        int col = colour(prefs);
        String colName = (col >= 0 && col < COLOUR_NAMES.length) ? COLOUR_NAMES[col] : "";
        return sel.label + (sel.paint != null && sel.paint.length > 0 && !colName.isEmpty() ? " · " + colName : "");
    }

    public static List<Model> models(Context context) {
        List<Model> result = new ArrayList<>();
        try {
            byte[] bytes = asset(context, "navatars/models.json");
            String json = new String(bytes, "UTF-8");
            org.json.JSONObject obj = new org.json.JSONObject(json);
            org.json.JSONArray arr = obj.getJSONArray("models");
            for (int i = 0; i < arr.length(); i++) {
                org.json.JSONObject m = arr.getJSONObject(i);
                org.json.JSONArray paintArr = m.optJSONArray("paint");
                int[] paint = null;
                if (paintArr != null) {
                    paint = new int[paintArr.length()];
                    for (int p = 0; p < paintArr.length(); p++) paint[p] = paintArr.getInt(p);
                }
                String id = m.getString("id");
                String label = m.optString("label", id);
                result.add(new Model(id, label, paint));
            }
        } catch (Exception e) {
            android.util.Log.w(TAG, "models.json: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        return result;
    }

    public static Model selected(List<Model> list, SharedPreferences prefs) {
        String id = prefs != null ? prefs.getString(MODEL_KEY, null) : null;
        if (id != null) {
            for (Model m : list) {
                if (id.equals(m.id)) return m;
            }
        }
        if (!list.isEmpty()) return list.get(0);
        throw new IllegalStateException("no bundled 3D car");
    }

    public static void installEarly(Context context) {
        try {
            if (!android.app.Application.getProcessName().equals(context.getPackageName())) return;
            SharedPreferences clusterPrefs = context.getSharedPreferences("local_cluster_settings", Context.MODE_PRIVATE);
            enabled = enabled(clusterPrefs);
            if (!enabled) {
                try {
                    SharedPreferences mapsSettings = context.getSharedPreferences("settings_preference", Context.MODE_PRIVATE);
                    mapsSettings.edit().putBoolean("custom_chevron_opt_in", false).apply();
                } catch (Throwable ignored) {}
                android.util.Log.i(TAG, "3D car off");
                return;
            }
            List<Model> modelList = models(context);
            Model model = selected(modelList, clusterPrefs);
            int col = colour(clusterPrefs);
            File dir = context.getDir("customchevron", Context.MODE_PRIVATE);
            int updated = 0;

            byte[] manifestBytes = asset(context, "navatars/manifest_1.json");
            updated += write(manifestBytes, new File(dir, "manifest_1.json"));

            byte[] fbxBytes;
            try {
                fbxBytes = asset(context, "navatars/" + model.id + ".fbx");
            } catch (Exception e) {
                fbxBytes = asset(context, "navatars/tang2024.fbx");
            }
            updated += write(fbxBytes, new File(dir, "model.fbx"));
            updated += write(fbxBytes, new File(dir, "suv.fbx"));

            byte[] pngBytes;
            try {
                pngBytes = asset(context, "navatars/" + model.id + ".png");
            } catch (Exception e) {
                pngBytes = asset(context, "navatars/tang2024.png");
            }
            byte[] textureBytes = texture(pngBytes, model.paint, col);
            updated += write(textureBytes, new File(dir, "texture.png"));
            updated += write(textureBytes, new File(dir, "texture_dark.png"));

            SharedPreferences mapsSettings = context.getSharedPreferences("settings_preference", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = mapsSettings.edit();
            boolean needSet = !"suv".equals(mapsSettings.getString("custom_chevron_model_name", null))
                || mapsSettings.getInt("custom_chevron_model_color_id", -1) != 1
                || !mapsSettings.getBoolean("custom_chevron_opt_in", false);
            editor.putString("custom_chevron_model_name", "suv");
            editor.putInt("custom_chevron_model_color_id", 1);
            editor.putBoolean("custom_chevron_opt_in", true);
            editor.commit();

            String colName = (col >= 0 && col < COLOUR_NAMES.length) ? COLOUR_NAMES[col] : "";
            android.util.Log.i(TAG, "3D car ready: " + model.id + ", " + colName + "; " + updated + " file(s) updated, settings " + (needSet ? "set" : "unchanged"));
        } catch (Throwable t) {
            enabled = false;
            android.util.Log.w(TAG, "3D car setup failed: " + t.getClass().getSimpleName() + ": " + t.getMessage());
        }
    }

    public static void keepLocalModel(Object obj) {
        android.util.Log.i(TAG, "model selected; using local files, no download");
    }

    public static void loadLocalManifest(Object obj, int id) {
        try {
            java.lang.reflect.Method m = obj.getClass().getDeclaredMethod("q", int.class);
            m.setAccessible(true);
            Object res = m.invoke(obj, id);
            android.util.Log.i(TAG, "local manifest " + id + ": " + res);
        } catch (Throwable t) {
            Throwable cause = t.getCause() != null ? t.getCause() : t;
            android.util.Log.w(TAG, "local manifest " + id + " failed: " + cause.getClass().getSimpleName() + ": " + cause.getMessage());
        }
    }

    public static void animation(String anim) {
        if (anim != null && !anim.equals(lastAnimation)) {
            lastAnimation = anim;
            android.util.Log.i(TAG, "animation " + anim);
        }
    }

    private static byte[] asset(Context context, String path) throws IOException {
        try (InputStream in = context.getAssets().open(path)) {
            return read(in);
        }
    }

    private static byte[] read(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[16384];
        int n;
        while ((n = in.read(buf)) >= 0) out.write(buf, 0, n);
        return out.toByteArray();
    }

    private static int write(byte[] data, File file) throws IOException {
        if (file.exists() && file.length() == data.length) {
            try (FileInputStream in = new FileInputStream(file)) {
                if (Arrays.equals(read(in), data)) return 0;
            } catch (Throwable ignored) {}
        }
        File tmp = new File(file.getPath() + ".tmp");
        try (FileOutputStream out = new FileOutputStream(tmp)) {
            out.write(data);
        }
        if (!tmp.renameTo(file)) {
            tmp.delete();
            try (FileOutputStream out = new FileOutputStream(file)) {
                out.write(data);
            }
        }
        return 1;
    }

    private static byte[] texture(byte[] original, int[] paint, int colour) {
        if (colour == 0 || paint == null || paint.length == 0 || colour >= COLOURS.length) return original;
        try {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inMutable = true;
            opts.inScaled = false;
            Bitmap bmp = BitmapFactory.decodeByteArray(original, 0, original.length, opts);
            if (bmp == null || bmp.getWidth() != 128 || bmp.getHeight() != 128) {
                if (bmp != null) bmp.recycle();
                return original;
            }
            double maxLum = 0.0;
            for (int cell : paint) {
                int x0 = (cell % 16) * 8;
                int y0 = (cell / 16) * 8;
                int pixel = bmp.getPixel(x0, y0);
                int r = (pixel >> 16) & 0xFF;
                int g = (pixel >> 8) & 0xFF;
                int b = pixel & 0xFF;
                double lum = 0.2126 * r + 0.7152 * g + 0.0722 * b;
                maxLum = Math.max(maxLum, lum);
            }
            int targetCol = COLOURS[colour];
            int tr = (targetCol >> 16) & 0xFF;
            int tg = (targetCol >> 8) & 0xFF;
            int tb = targetCol & 0xFF;
            int[] cellPixels = new int[64];
            for (int cell : paint) {
                int x0 = (cell % 16) * 8;
                int y0 = (cell / 16) * 8;
                int pixel = bmp.getPixel(x0, y0);
                int r = (pixel >> 16) & 0xFF;
                int g = (pixel >> 8) & 0xFF;
                int b = pixel & 0xFF;
                double lum = 0.2126 * r + 0.7152 * g + 0.0722 * b;
                double factor = maxLum > 0.0 ? (lum / maxLum) : 1.0;
                int nr = (int) Math.round(tr * factor);
                int ng = (int) Math.round(tg * factor);
                int nb = (int) Math.round(tb * factor);
                int newPixel = 0xFF000000 | ((nr & 0xFF) << 16) | ((ng & 0xFF) << 8) | (nb & 0xFF);
                Arrays.fill(cellPixels, newPixel);
                bmp.setPixels(cellPixels, 0, 8, x0, y0, 8, 8);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
            bmp.recycle();
            return out.toByteArray();
        } catch (Throwable t) {
            return original;
        }
    }

    static final class Model {
        final String id;
        final String label;
        final int[] paint;
        Model(String id, String label, int[] paint) {
            this.id = id;
            this.label = label;
            this.paint = paint;
        }
    }
}
