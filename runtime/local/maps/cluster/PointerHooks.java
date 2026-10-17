package local.maps.cluster;

/** Exact AAOS 26.09 anchor schema. Keep X; override Y only in the cluster. */
public final class PointerHooks {
    public static Object adjustCamera(Object manager,Object point) {
        Object adjusted=adjust(manager,point);
        if(ClusterHost.isClusterViewport(manager))try {
            ClusterHost.applyClusterPointerY(manager,adjusted.getClass().getField("c").getFloat(adjusted));
        } catch(ReflectiveOperationException e){android.util.Log.w("MapsClusterTest","AAOS camera anchor",e);}
        return adjusted;
    }
    public static Object adjust(Object manager,Object point) {
        if(!ClusterHost.isClusterViewport(manager))return point;
        try {
            float x=point.getClass().getField("b").getFloat(point);
            float old=point.getClass().getField("c").getFloat(point);
            float y=ClusterHost.clusterPointerY(manager,old);
            return old==y?point:point.getClass().getConstructor(float.class,float.class).newInstance(x,y);
        } catch(ReflectiveOperationException e) { android.util.Log.w("MapsClusterTest","AAOS anchor schema",e);return point; }
    }
}
