// SPDX-License-Identifier: GPL-3.0-only
package local.maps.cluster;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Pure policy: no Android objects, clocks, I/O, key injection or global settings. */
public final class WheelKeyPolicy {
    private static final long ACTIVE=Long.MAX_VALUE;
    public static final int DOWN=0, UP=1;
    public static final class Binding {
        public final int code,scan;
        public final String device;
        public Binding(int code,int scan,String device) {
            if(code<=0||code>65535||scan<0||scan>65535||device==null||device.length()==0||device.length()>128)
                throw new IllegalArgumentException("Invalid wheel binding");
            for(int i=0;i<device.length();i++)if(Character.isISOControl(device.charAt(i)))throw new IllegalArgumentException("Invalid wheel device");
            this.code=code;this.scan=scan;this.device=device;
        }
        boolean matches(Event event){return code==event.code&&scan==event.scan&&device.equals(event.device);}
        boolean same(Binding other){return code==other.code&&scan==other.scan&&device.equals(other.device);}
    }
    public static final class Bindings {
        public final Binding mode,in,out;
        public Bindings(Binding mode,Binding in,Binding out){
            if(mode==null||in==null||out==null||mode.same(in)||mode.same(out)||in.same(out))throw new IllegalArgumentException("Conflicting wheel bindings");
            this.mode=mode;this.in=in;this.out=out;
        }
        public static Bindings tang(){return new Bindings(new Binding(305,300,"simulate-keys"),new Binding(291,115,"simulate-keys"),new Binding(292,114,"simulate-keys"));}
        int role(Event event){return mode.matches(event)?1:in.matches(event)?2:out.matches(event)?3:0;}
    }
    public static final class Event {
        public final int action,code,scan,deviceId,repeat;
        public final String device;
        public final long downTime;
        public final boolean canceled;
        public Event(int action,int code,int scan,int deviceId,String device,long downTime,int repeat,boolean canceled){
            this.action=action;this.code=code;this.scan=scan;this.deviceId=deviceId;this.device=device;
            this.downTime=downTime;this.repeat=repeat;this.canceled=canceled;
        }
        String identity(){return deviceId+":"+code+":"+scan+":"+downTime;}
    }
    public static final class Result {
        public final boolean consume,mode;
        public final int delta;
        Result(boolean consume,int delta,boolean mode){this.consume=consume;this.delta=delta;this.mode=mode;}
    }
    private Bindings bindings;
    private final Map<String,Integer> owned=new HashMap<>();
    private long until;
    public WheelKeyPolicy(Bindings bindings){if(bindings==null)throw new IllegalArgumentException();this.bindings=bindings;}
    public void updateBindings(Bindings next){if(next==null)throw new IllegalArgumentException();bindings=next;until=0;}
    public void endMode(){until=0;}
    public void disconnected(){until=0;owned.clear();}
    public boolean mode(long now){return until!=0&&now>=0;}
    public long deadline(){return until;}
    public Result handle(Event event,boolean enabled,boolean mapReady,boolean audioSafe,long now){
        if(!enabled||!mapReady||!audioSafe||now<0)until=0;
        String id=event.identity();Integer ownership=owned.get(id);
        // Ownership survives opt-out/call/map loss until UP. Never leak half a consumed pair.
        if(ownership!=null){
            if(event.action==UP){owned.remove(id);return new Result(true,0,mode(now));}
            return new Result(true,0,mode(now)); // A physical repeat is consumed, never a second toggle/zoom.
        }
        if(event.action!=DOWN||event.repeat!=0||event.canceled||!enabled||!mapReady||!audioSafe||now<0)
            return new Result(false,0,mode(now));
        int role=bindings.role(event);
        if(role==0||owned.size()>=32)return new Result(false,0,mode(now));
        if(role==1){
            owned.put(id,role);until=mode(now)?0:ACTIVE;
            return new Result(true,0,mode(now));
        }
        if(!mode(now))return new Result(false,0,false);
        owned.put(id,role);
        return new Result(true,role==2?1:-1,true);
    }

    /** Validated colon-list utilities. Preserve all unrelated text/order and remove only our ID. */
    public static final class Components {
        private final List<String> entries;
        private Components(List<String> entries){this.entries=entries;}
        public static Components parse(String text){
            if(text==null)throw new IllegalArgumentException("Missing accessibility setting");
            if(text.length()>2800)throw new IllegalArgumentException("Accessibility setting is too long");
            text=text.trim();List<String> entries=new ArrayList<>();
            if(text.length()==0||"null".equals(text))return new Components(entries);
            for(String entry:text.split(":",-1)){normalize(entry);entries.add(entry);if(entries.size()>128)throw new IllegalArgumentException("Too many accessibility services");}
            return new Components(entries);
        }
        public static String normalize(String value){
            if(value==null||value.length()>512)throw new IllegalArgumentException("Invalid component");
            int slash=value.indexOf('/');
            if(slash<=0||slash!=value.lastIndexOf('/')||slash==value.length()-1)throw new IllegalArgumentException("Invalid component");
            String pkg=value.substring(0,slash),cls=value.substring(slash+1);
            if(!pkg.matches("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)*"))throw new IllegalArgumentException("Invalid component package");
            if(cls.charAt(0)=='.')cls=pkg+cls;
            if(!cls.matches("[A-Za-z_$][A-Za-z0-9_$]*(\\.[A-Za-z_$][A-Za-z0-9_$]*)*"))throw new IllegalArgumentException("Invalid component class");
            return pkg+"/"+cls;
        }
        public String withOwn(String own,boolean on){
            own=normalize(own);StringBuilder result=new StringBuilder();boolean inserted=false;
            for(String entry:entries){
                if(normalize(entry).equals(own)){
                    if(!on||inserted)continue;
                    append(result,own);inserted=true;
                }else append(result,entry);
            }
            if(on&&!inserted)append(result,own);
            if(result.length()>2800)throw new IllegalArgumentException("Accessibility setting is too long");
            return result.toString();
        }
        public int count(String own){own=normalize(own);int count=0;for(String entry:entries)if(normalize(entry).equals(own))count++;return count;}
        public boolean preservesOthers(Components baseline,String own){
            own=normalize(own);
            for(String entry:baseline.entries){String normalized=normalize(entry);if(!normalized.equals(own)&&count(normalized)==0)return false;}
            return true;
        }
        private static void append(StringBuilder builder,String value){if(builder.length()>0)builder.append(':');builder.append(value);}
    }
}
