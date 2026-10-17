// SPDX-License-Identifier: GPL-3.0-only
package local.maps.cluster;

import android.os.SystemClock;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import org.json.JSONObject;

/** Manual app_process entry point from this same APK, always shell UID2000. */
public final class VehicleShellSampler {
    private static volatile Process delivery;
    private static final String URI="content://com.google.android.apps.maps.automotive.local.vehicle";
    public static void main(String[] args)throws Exception{
        if(android.os.Process.myUid()!=2000)throw new SecurityException("Authorized ADB reader session required");
        boolean continuous=VehicleReadingRules.continuousMode(args);
        Runtime.getRuntime().addShutdownHook(new Thread(()->{Process child=delivery;if(child!=null)child.destroy();},"vehicle-reader-shutdown"));
        long started=SystemClock.elapsedRealtime();
        System.out.println(continuous?"continuous_vehicle_reader_started":"bounded_vehicle_reader_started");
        try{
            for(int cycle=0;VehicleReadingRules.continueSampling(continuous,cycle,SystemClock.elapsedRealtime()-started);cycle++){
                if(continuous){
                    boolean renewed;try{renewed=call("renew",new JSONObject().put("session",args[0]));}catch(Exception unavailable){renewed=false;}
                    // Off, stopped, expired, rebooted or mismatched sessions cannot be resurrected.
                    if(!renewed){System.out.println("reader_renewal_rejected_or_unavailable");break;}
                }
                JSONObject sample;
                try{sample=VehicleBinderReader.read(args[0]);}catch(Exception unavailable){sample=null;}
                if(!continuous&&SystemClock.elapsedRealtime()-started>=VehicleReadingRules.LEASE_MS)break;
                if(sample==null){call("end",new JSONObject().put("session",args[0]));System.out.println("vehicle_source_unavailable_session_ended");break;}
                if(sample!=null){
                    boolean sent;try{sent=call("sample",sample);}catch(Exception unavailable){sent=false;}
                    if(!sent){call("end",new JSONObject().put("session",args[0]));System.out.println("sample_rejected_or_delivery_unavailable");break;}
                    System.out.println("vehicle_sample_delivered");
                }else System.out.println("vehicle_source_unavailable");
                Thread.sleep(VehicleReadingRules.PERIOD_MS);
            }
        }finally{Process child=delivery;if(child!=null)child.destroy();}
        System.out.println(continuous?"continuous_vehicle_reader_finished":"bounded_vehicle_reader_finished");
    }
    static JSONObject options(JSONObject packet)throws Exception{return exchange("reader_options",packet,"options");}
    static void cache(JSONObject packet)throws Exception{exchange("reader_cache",packet,null);}
    private static boolean call(String method,JSONObject packet)throws Exception{
        if(!"sample".equals(method)&&!"renew".equals(method)&&!"end".equals(method))throw new IllegalArgumentException("Unsupported reader operation");
        try{exchange(method,packet,null);return true;}catch(Exception rejected){return false;}
    }
    static JSONObject replyObject(String output,String field)throws Exception{
        String text=output.trim(),prefix="Result: Bundle[{",suffix="}]";
        if(!text.startsWith(prefix)||!text.endsWith(suffix))throw new IOException("Invalid vehicle options reply");
        org.json.JSONTokener parser=new org.json.JSONTokener(text.substring(prefix.length(),text.length()-suffix.length()));
        boolean accepted=false,seen=false;JSONObject result=null;
        for(int i=0;i<2;i++){
            String key=parser.nextTo('=').trim();if(parser.next()!='=')throw new IOException("Invalid options field");Object value=parser.nextValue();
            if("accepted".equals(key)&&!seen){if(!Boolean.TRUE.equals(value))throw new IOException("Options rejected");accepted=true;seen=true;}
            else if(field.equals(key)&&result==null&&value instanceof JSONObject)result=(JSONObject)value;
            else throw new IOException("Unexpected options field");
            char next=parser.nextClean();if(i==0&&next!=','||i==1&&next!=0)throw new IOException("Invalid options delimiter");
        }
        if(!accepted||result==null)throw new IOException("Incomplete options reply");return result;
    }
    private static JSONObject exchange(String method,JSONObject packet,String field)throws Exception{
        if(!java.util.Arrays.asList("sample","renew","end","reader_options","reader_cache").contains(method))throw new IllegalArgumentException("Unsupported reader operation");
        Process child=new ProcessBuilder("/system/bin/content","call","--uri",URI,"--method",method,"--arg",packet.toString()).redirectErrorStream(true).start();delivery=child;
        try{
            if(!child.waitFor(10,TimeUnit.SECONDS)){child.destroyForcibly();throw new IOException("Vehicle provider timed out");}
            ByteArrayOutputStream reply=new ByteArrayOutputStream();
            try(InputStream in=child.getInputStream()){
                byte[] buffer=new byte[512];int n;while(reply.size()<16384&&(n=in.read(buffer,0,Math.min(buffer.length,16384-reply.size())))!=-1)reply.write(buffer,0,n);
            }
            String output=new String(reply.toByteArray(),StandardCharsets.UTF_8);
            if(child.exitValue()!=0)throw new IOException("Vehicle provider rejected request");
            if(field==null){if(!"Result: Bundle[{accepted=true}]".equals(output.trim()))throw new IOException("Invalid vehicle acknowledgement");return null;}
            return replyObject(output,field);
        }finally{delivery=null;child.destroy();}
    }
}
