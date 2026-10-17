package local.maps.cluster;

/** Physical coordinates, independent of system and per-app language/layout direction. */
final class ClusterButtonPlacement {
    static int settingsLeft(int left,int right,int anchorLeft,int anchorRight,int cardLeft,int cardRight){
        int width=anchorRight-anchorLeft;
        boolean columnRight=anchorLeft+anchorRight>left+right;
        int gap=Math.max(0,columnRight?right-anchorRight:anchorLeft-left);
        boolean onLeft=cardRight>cardLeft?cardLeft+cardRight>left+right:columnRight;
        int x=onLeft?left+gap:right-gap-width;
        // Some car layouts move the search card without mirroring the native button column.
        if(x<anchorRight&&x+width>anchorLeft)x=onLeft?anchorRight+gap:anchorLeft-gap-width;
        return Math.max(left,Math.min(x,right-width));
    }
}
