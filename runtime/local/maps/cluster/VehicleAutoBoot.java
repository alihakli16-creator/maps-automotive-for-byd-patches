// SPDX-License-Identifier: GPL-3.0-only
package local.maps.cluster;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/** Queues opt-in checks and optionally auto-starts Maps on vehicle boot. */
public final class VehicleAutoBoot extends BroadcastReceiver {
    private static final String TAG="MapsVehicleAuto";
    private static final String PREF_AUTO_BOOT="auto_boot_enabled";
    private static final long LAUNCH_DELAY_MS=3000L;

    @Override public void onReceive(final Context context,Intent intent){
        if(context==null||intent==null)return;
        String action=intent.getAction();
        if(action==null)return;
        if(!Intent.ACTION_BOOT_COMPLETED.equals(action)
                &&!"android.intent.action.LOCKED_BOOT_COMPLETED".equals(action)
                &&!Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)
                &&!"android.intent.action.QUICKBOOT_POWERON".equals(action)
                &&!"com.htc.intent.action.QUICKBOOT_POWERON".equals(action))return;

        Log.i(TAG,"Vehicle boot broadcast received: "+action);

        try{VehicleAutoReader.trigger(context,false);}
        catch(RuntimeException failure){Log.w(TAG,"Startup request unavailable: "+failure.getClass().getSimpleName());}

        try{
            SharedPreferences prefs=context.getSharedPreferences("local_cluster_settings",Context.MODE_PRIVATE);
            if(!prefs.getBoolean(PREF_AUTO_BOOT,false)){
                Log.i(TAG,"Auto-boot is disabled in settings; skipping launch.");
                return;
            }

            final PendingResult pending=goAsync();
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable(){
                @Override public void run(){
                    try{
                        Intent launch=context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
                        if(launch!=null){
                            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                            context.startActivity(launch);
                            Log.i(TAG,"Maps launched automatically on vehicle boot.");
                        }else{
                            Log.w(TAG,"Launch intent for package unavailable.");
                        }
                    }catch(Throwable t){
                        Log.w(TAG,"Failed to launch Maps on boot: "+t.getMessage());
                    }finally{
                        try{pending.finish();}catch(Throwable ignored){}
                    }
                }
            },LAUNCH_DELAY_MS);
        }catch(Throwable t){
            Log.w(TAG,"Auto-boot handler error: "+t.getMessage());
        }
    }
}
