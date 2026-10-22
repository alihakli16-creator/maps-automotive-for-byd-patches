// SPDX-License-Identifier: GPL-3.0-only
package local.maps.cluster;

import android.app.Activity;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.widget.Toast;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Explicit user gesture only. Google app owns microphone capture and the Live conversation. */
final class GeminiLaunch {
 static final String PACKAGE="com.google.android.apps.bard", GOOGLE="com.google.android.googlequicksearchbox";
 static final String AUTHORITY=GOOGLE+".local.maps.assistant";
 static final String BUTTON_KEY="gemini_map_button",WHEEL_KEY="gemini_wheel_long_press",COMPACT_KEY="gemini_compact_assistant",MODE_KEY="gemini_launch_mode";
 private static final ExecutorService worker=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"MapsGeminiMode");t.setDaemon(true);return t;});
 private static final Handler ui=new Handler(Looper.getMainLooper());
 private static final AtomicBoolean opening=new AtomicBoolean();
 private static volatile boolean wheel;
 private static volatile java.lang.ref.WeakReference<Activity> resumed=new java.lang.ref.WeakReference<>(null);
 private static SharedPreferences preferences;
 private static final SharedPreferences.OnSharedPreferenceChangeListener changes=(p,key)->{if(WHEEL_KEY.equals(key))wheel=p.getBoolean(WHEEL_KEY,false);};
 static synchronized void init(Context c){if(preferences!=null)return;preferences=c.getApplicationContext().getSharedPreferences("local_cluster_settings",Context.MODE_PRIVATE);wheel=preferences.getBoolean(WHEEL_KEY,false);preferences.registerOnSharedPreferenceChangeListener(changes);GeminiRecovery.init(c.getApplicationContext());}
 static boolean wheelEnabled(){return wheel;}
 static boolean buttonEnabled(){return preferences==null||preferences.getBoolean(BUTTON_KEY,true);}
 static boolean compactEnabled(){return preferences==null||preferences.getBoolean(COMPACT_KEY,true);}
 static int mode(){return preferences==null||preferences.getInt(MODE_KEY,1)!=0?1:0;}
 static String modeName(){return mode()==1?"Conversation (Gemini Live)":"Microphone";}
 static void resumed(Activity a){resumed=new java.lang.ref.WeakReference<>(a);}
 static void paused(Activity a){if(resumed.get()==a)resumed.clear();}
 static boolean googleAssistant(Context c){try{String s=android.provider.Settings.Secure.getString(c.getContentResolver(),"voice_interaction_service");ComponentName n=s==null?null:ComponentName.unflattenFromString(s);return n!=null&&GOOGLE.equals(n.getPackageName());}catch(RuntimeException e){return false;}}
 static void assistantSettings(Activity a){try{a.startActivity(new Intent(android.provider.Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS));}catch(RuntimeException e){Toast.makeText(a,"Open Android default apps and select Google as the digital assistant.",Toast.LENGTH_LONG).show();}}
 static boolean available(Context c){try{return c.getPackageManager().getLaunchIntentForPackage(PACKAGE)!=null;}catch(RuntimeException e){return false;}}
 private static boolean prepare(Context c,String mode){
  try{
   android.content.pm.ProviderInfo p=c.getPackageManager().resolveContentProvider(AUTHORITY,0);
   if(p==null||!p.exported||!p.enabled||!GOOGLE.equals(p.packageName))return false;
   Bundle b=c.getContentResolver().call(Uri.parse("content://"+AUTHORITY),"prepare",mode,null);
   return b!=null&&b.getInt("api",0)==1&&b.getBoolean("armed",false);
  }catch(RuntimeException e){return false;}
 }
 private static void cancel(Context c){worker.execute(()->{try{c.getContentResolver().call(Uri.parse("content://"+AUTHORITY),"cancel",null,null);}catch(RuntimeException ignored){}});}
 static boolean open(Context context){
  init(context);
  if(!available(context)){Toast.makeText(context,"Install Gemini and finish its Google sign-in first.",Toast.LENGTH_LONG).show();return false;}
  if(!opening.compareAndSet(false,true))return true;
  Context app=context.getApplicationContext();Activity requested=resumed.get();
  final String selected=mode()==1?"conversation":"microphone";
  worker.execute(()->{
   // Wakes a force-stopped Google package before Android tries to bind its voice service.
   boolean armed=prepare(app,selected);
   ui.post(()->launchPrepared(context,app,requested,selected,armed,0,true));
  });
  return true;
 }
 private static void launchPrepared(Context context,Context app,Activity requested,String selected,boolean armed,int retries,boolean allowRecovery){
    boolean waiting=false;
    try{
     if(requested!=null&&(resumed.get()!=requested||requested.isFinishing()||requested.isDestroyed())){if(armed)cancel(app);return;}
     if(!armed)Toast.makeText(context,"One-tap voice needs the updated patched Google app. Choose Microphone or Live in Gemini for now.",Toast.LENGTH_LONG).show();
     if(compactEnabled()&&googleAssistant(app)&&requested!=null&&!requested.isInPictureInPictureMode()){
      try{if(requested.showAssist(new Bundle())){android.util.Log.i("MapsGemini","assistant mode requested="+selected+" armed="+armed);return;}}catch(RuntimeException ignored){}
      if(retries>0){waiting=true;ui.postDelayed(()->launchPrepared(context,app,requested,selected,armed,retries-1,false),350);return;}
     }
     if(allowRecovery&&GeminiRecovery.enabled()&&compactEnabled()&&requested!=null&&!requested.isInPictureInPictureMode()){
      waiting=true;
      worker.execute(()->{
       boolean ready=GeminiRecovery.recover(app,false,()->resumed.get()==requested&&compactEnabled()&&!requested.isFinishing()&&!requested.isDestroyed());
       boolean rearmed=prepare(app,selected);
       ui.post(()->launchPrepared(context,app,requested,selected,rearmed,ready?2:0,false));
      });
      return;
     }
     android.util.Log.i("MapsGemini","full fallback compact="+compactEnabled()+" google="+googleAssistant(app)+" resumed="+(requested!=null));
     Intent intent=app.getPackageManager().getLaunchIntentForPackage(PACKAGE);
     if(intent==null){if(armed)cancel(app);return;}
     intent.setPackage(PACKAGE);if(!(context instanceof Activity))intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
     context.startActivity(intent);
    }catch(RuntimeException e){if(armed)cancel(app);Toast.makeText(context,"Could not open Gemini. Open it from the launcher and finish setup.",Toast.LENGTH_LONG).show();}
    finally{if(!waiting)opening.set(false);}
 }
}
