// SPDX-License-Identifier: GPL-3.0-only
package local.maps.cluster;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.IOException;

/** Preference I/O belongs to WheelServiceSetup's worker, never a key callback. */
final class WheelZoomSettings {
    private static final String PREFS="local_wheel_zoom";
    final boolean enabled;
    final WheelKeyPolicy.Bindings bindings;
    WheelZoomSettings(boolean enabled,WheelKeyPolicy.Bindings bindings){this.enabled=enabled;this.bindings=bindings;}
    static WheelZoomSettings defaults(){return new WheelZoomSettings(false,WheelKeyPolicy.Bindings.tang());}
    WheelZoomSettings enabled(boolean value){return new WheelZoomSettings(value,bindings);}
    static WheelZoomSettings read(Context context) throws IOException {
        try {
            SharedPreferences preferences=context.getSharedPreferences(PREFS,0);
            WheelKeyPolicy.Bindings defaults=WheelKeyPolicy.Bindings.tang();
            return new WheelZoomSettings(preferences.getBoolean("enabled",false),new WheelKeyPolicy.Bindings(
                read(preferences,"mode",defaults.mode),read(preferences,"in",defaults.in),read(preferences,"out",defaults.out)));
        }catch(RuntimeException error){throw new IOException("Wheel settings unavailable",error);}
    }
    private static WheelKeyPolicy.Binding read(SharedPreferences preferences,String role,WheelKeyPolicy.Binding fallback){
        return new WheelKeyPolicy.Binding(preferences.getInt(role+"_code",fallback.code),preferences.getInt(role+"_scan",fallback.scan),preferences.getString(role+"_device",fallback.device));
    }
    void save(Context context) throws IOException {
        try {
            SharedPreferences.Editor edit=context.getSharedPreferences(PREFS,0).edit().putBoolean("enabled",enabled);
            write(edit,"mode",bindings.mode);write(edit,"in",bindings.in);write(edit,"out",bindings.out);
            if(!edit.commit())throw new IOException("Could not save wheel settings");
        }catch(RuntimeException error){throw new IOException("Could not save wheel settings",error);}
    }
    private static void write(SharedPreferences.Editor edit,String role,WheelKeyPolicy.Binding binding){
        edit.putInt(role+"_code",binding.code).putInt(role+"_scan",binding.scan).putString(role+"_device",binding.device);
    }
}
