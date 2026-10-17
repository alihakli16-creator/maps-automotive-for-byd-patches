package local.maps.cluster;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.*;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;

/** Explicit local vehicle profile. Nothing here is measured battery capacity or charge power. */
final class VehicleEvProfile {
    static final class Profile {
        final String model,connector;final int capacityWh;final boolean enabled;final long epoch;
        Profile(String model,int wh,String connector,boolean enabled,long epoch){
            if(model==null||model.length()>80||wh<1000||wh>300000||!"CCS2".equals(connector))throw new IllegalArgumentException("Set a battery capacity and compatible station connector");
            this.model=model;capacityWh=wh;this.connector=connector;this.enabled=enabled;this.epoch=epoch;
        }
        String summary(){return model+" · "+String.format(java.util.Locale.US,"%.1f kWh",capacityWh/1000.0)+" · CCS2";}
    }
    private static volatile Profile current;
    private static volatile long epoch;
    private static final Object GUARD=new Object(),WRITES=new Object();
    private static boolean pending;
    static Profile current(){return current;}
    static long epoch(){return epoch;}
    /** Fast revocation at the UI request, before queuing any disk work. */
    static long invalidate(){synchronized(GUARD){++epoch;pending=true;current=null;return epoch;}}
    /** Called only on the vehicle worker, never from a native callback or UI. */
    static void load(Context c)throws Exception{
        final long token;synchronized(GUARD){if(pending)return;token=epoch;}
        SharedPreferences p=c.getSharedPreferences("local_vehicle_ev_profile",Context.MODE_PRIVATE);
        if(!p.getBoolean("configured",false)){
            // Optional signed private-build seed. Public code defaults to unconfigured.
            try(InputStream in=c.getAssets().open("local/vehicle-profile.json");ByteArrayOutputStream out=new ByteArrayOutputStream()){
                byte[] bytes=new byte[1024];int n;while((n=in.read(bytes))!=-1){if(out.size()+n>4096)throw new IOException("Profile too large");out.write(bytes,0,n);}
                JSONObject seed=new JSONObject(new String(out.toByteArray(),StandardCharsets.UTF_8));
                save(c,seed.getString("model"),seed.getInt("capacity_wh"),seed.getString("station_connector"),seed.getBoolean("routing_enabled"),token);return;
            }catch(FileNotFoundException absent){synchronized(GUARD){if(epoch==token&&!pending)current=null;}return;}
        }
        Profile value=new Profile(p.getString("model",""),p.getInt("capacity_wh",0),p.getString("station_connector",""),p.getBoolean("routing_enabled",false),token);
        synchronized(GUARD){if(epoch==token&&!pending)current=value;}
    }
    /** The token belongs to the original UI request, never to the eventual worker start. */
    static boolean save(Context c,String model,int wh,String connector,boolean enabled,long expectedEpoch)throws Exception{
        Profile value=new Profile(model,wh,connector,enabled,expectedEpoch);
        synchronized(WRITES){
            synchronized(GUARD){if(epoch!=expectedEpoch)return false;}
            SharedPreferences preferences=c.getSharedPreferences("local_vehicle_ev_profile",Context.MODE_PRIVATE);
            synchronized(GUARD){if(epoch!=expectedEpoch)return false;}
            if(!preferences.edit().putBoolean("configured",true).putString("model",model).putInt("capacity_wh",wh).putString("station_connector",connector).putBoolean("routing_enabled",enabled).commit())throw new IOException("Profile not saved");
            synchronized(GUARD){
                if(epoch!=expectedEpoch)return false;
                current=value;pending=false;return true;
            }
        }
    }
}
