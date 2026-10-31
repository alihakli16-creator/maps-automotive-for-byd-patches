package local.maps.cluster;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;

/** Cluster, voice, and DiLink 3 quick-projection controls anchored to the measured native one-bar column. */
final class ClusterButtons {
    private static final String TAG="local.cluster.buttons";
    static void attach(Activity a) {
        if(a.isFinishing()||a.isDestroyed())return;
        ViewGroup root=(ViewGroup)a.getWindow().getDecorView();
        if(root.findViewWithTag(TAG)!=null)return;
        new Controls(a,root);
    }
    private static int id(Context c,String name){return MapsResources.id(c.getResources(),name,"id");}
    private static int dp(Context c,int n){return Math.round(n*c.getResources().getDisplayMetrics().density);}
    private static final class Controls implements ViewTreeObserver.OnPreDrawListener {
        final Activity activity; final ViewGroup root; final ScrollView column; final LinearLayout buttons;
        final ImageButton settings,map,voice,auth,restore,zoom;
        final FrameLayout settingsSlot,voiceSlot,authSlot,restoreSlot,zoomSlot;
        final Icon settingsIcon=new Icon(false),mapIcon=new Icon(true),voiceIcon=new Icon(false,true),authIcon=new Icon(3),restoreIcon=new Icon(4),zoomIcon=new Icon(5);
        View template,bar,bottom; Drawable nativeBackground; int lastColour,lastSurface,lastActive;
        boolean styled,wasOn; int tx,ty,tw,th;
        Controls(Activity a,ViewGroup r) {
            activity=a;root=r;column=new ScrollView(a);column.setTag(TAG);column.setFillViewport(false);
            column.setClipToPadding(false);column.setClipChildren(false);column.setVerticalScrollBarEnabled(false);
            buttons=new LinearLayout(a);buttons.setOrientation(LinearLayout.VERTICAL);buttons.setClipChildren(false);
            settings=button(settingsIcon,"Instrument cluster settings");map=button(mapIcon,"Hide map on instrument cluster");
            settingsSlot=(FrameLayout)settings.getParent();buttons.removeView(settingsSlot);
            voice=button(voiceIcon,"Talk to Gemini Live");voiceSlot=(FrameLayout)voice.getParent();
            auth=button(authIcon,"Yetkilendir veya tekrar dene");authSlot=(FrameLayout)auth.getParent();
            restore=button(restoreIcon,"Orijinal göstergeyi geri yükle");restoreSlot=(FrameLayout)restore.getParent();
            zoom=button(zoomIcon,"Gösterge haritasını uzaklaştır (uzun basış: yakınlaştır)");zoomSlot=(FrameLayout)zoom.getParent();
            settings.setTag("local.cluster.settings.button");map.setTag("local.cluster.map.button");voice.setTag("local.cluster.gemini.button");
            auth.setTag("local.cluster.dilink3.auth.button");restore.setTag("local.cluster.dilink3.restore.button");
            zoom.setTag("local.cluster.dilink3.zoom.button");
            GeminiLaunch.init(a);voice.setOnClickListener(v->GeminiLaunch.open(a));
            settings.setOnClickListener(v->ClusterSettings.show(a));
            map.setOnClickListener(v->{ClusterSettings.toggleMap();updateState();});
            auth.setOnClickListener(v->{
                Toast.makeText(activity,"ADB yetkilendirme ve gösterge yansıtma başlatılıyor…",Toast.LENGTH_SHORT).show();
                DiLink3Projection.init(activity.getApplication(),null);
                DiLink3Projection.retry(activity,true,null);
            });
            restore.setOnClickListener(v->{
                Toast.makeText(activity,"Orijinal gösterge düzenine dönüldü",Toast.LENGTH_SHORT).show();
                DiLink3Projection.init(activity.getApplication(),null);
                DiLink3Projection.restore(activity,null);
            });
            zoom.setOnClickListener(v->{
                if(ClusterZoom.manualStep(-1)){
                    ClusterSettings.adjustClusterZoomSteps(-1);
                    int s=ClusterSettings.getClusterZoomSteps();
                    Toast.makeText(activity,"Gösterge paneli haritası uzaklaştırıldı (Kalıcı: "+s+")",Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(activity,"Gösterge paneli haritasının hazır olmasını bekleyin.",Toast.LENGTH_SHORT).show();
                }
            });
            zoom.setOnLongClickListener(v->{
                if(ClusterZoom.manualStep(1)){
                    ClusterSettings.adjustClusterZoomSteps(1);
                    int s=ClusterSettings.getClusterZoomSteps();
                    Toast.makeText(activity,"Gösterge paneli haritası yakınlaştırıldı (Kalıcı: "+s+")",Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(activity,"Gösterge paneli haritasının hazır olmasını bekleyin.",Toast.LENGTH_SHORT).show();
                }
                return true;
            });
            column.addView(buttons);column.setVisibility(View.INVISIBLE);
            root.addView(column,new FrameLayout.LayoutParams(1,1,Gravity.TOP|Gravity.LEFT));
            settingsSlot.setVisibility(View.INVISIBLE);root.addView(settingsSlot,new FrameLayout.LayoutParams(1,1,Gravity.TOP|Gravity.LEFT));
            root.getViewTreeObserver().addOnPreDrawListener(this);
            root.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener(){
                public void onViewAttachedToWindow(View v){}
                public void onViewDetachedFromWindow(View v){if(root.getViewTreeObserver().isAlive())root.getViewTreeObserver().removeOnPreDrawListener(Controls.this);}
            });
        }
        ImageButton button(Icon icon,String description){
            ImageButton b=new ImageButton(activity);b.setImageDrawable(icon);b.setScaleType(ImageView.ScaleType.FIT_CENTER);
            b.setPadding(0,0,0,0);b.setContentDescription(description);b.setTooltipText(description);b.setFocusable(true);
            FrameLayout slot=new FrameLayout(activity);slot.setClipChildren(false);slot.addView(b,new FrameLayout.LayoutParams(1,1,Gravity.CENTER));
            buttons.addView(slot,new LinearLayout.LayoutParams(1,1));return b;
        }
        public boolean onPreDraw(){
            ReportButtonRecovery.observe(root);
            if(activity.isDestroyed()||activity.isFinishing())return true;
            if(template==null||!template.isAttachedToWindow()){
                template=root.findViewById(id(activity,"map_buttons_view_settings_button"));
                bar=root.findViewById(id(activity,"one_bar_ellipsizing_container"));
                bottom=root.findViewById(id(activity,"one_bar_bottom_group"));styled=false;
            }
            if(template==null||bar==null||!bar.isShown()||activity.isInPictureInPictureMode()||template.getWidth()==0){
                column.setVisibility(View.INVISIBLE);settingsSlot.setVisibility(View.INVISIBLE);voiceSlot.setVisibility(View.INVISIBLE);
                authSlot.setVisibility(View.INVISIBLE);restoreSlot.setVisibility(View.INVISIBLE);zoomSlot.setVisibility(View.INVISIBLE);
                return true;
            }
            Rect anchor=new Rect(),area=new Rect();template.getGlobalVisibleRect(anchor);bar.getGlobalVisibleRect(area);
            int end=anchor.bottom;int slot=template.getHeight();
            if(bar instanceof ViewGroup){ViewGroup g=(ViewGroup)bar;for(int i=0;i<g.getChildCount();i++){
                View child=g.getChildAt(i);Rect bounds=new Rect();
                if(child.isShown()&&child.getHeight()>0&&child.getGlobalVisibleRect(bounds)){
                    end=Math.max(end,bounds.bottom);if(bounds.top<=anchor.top&&bounds.bottom>=anchor.bottom)slot=bounds.height();
                }
            }}
            int limit=area.bottom;Rect obstacle=new Rect();if(bottom!=null&&bottom.isShown()&&bottom.getGlobalVisibleRect(obstacle))limit=Math.min(limit,obstacle.top);
            int nativeRows=GeminiLaunch.buttonEnabled()?2:1;
            voiceSlot.setVisibility(nativeRows==2?View.VISIBLE:View.GONE);

            boolean showQuick=DiLink3Projection.isCandidate()&&ClusterSettings.quickButtonsEnabled();
            if(showQuick&&ClusterSettings.getClusterZoomSteps()!=0&&!ClusterZoom.isHeld()){
                ClusterZoom.scheduleRestoreSavedSteps(800);
            }
            authSlot.setVisibility(showQuick?View.VISIBLE:View.GONE);
            restoreSlot.setVisibility(showQuick?View.VISIBLE:View.GONE);
            zoomSlot.setVisibility(showQuick?View.VISIBLE:View.GONE);

            int available=Math.max(0,limit-end);
            int remaining=Math.max(0,available-nativeRows*slot);
            int quickSlot=dp(activity,44);
            if(showQuick&&remaining>0){
                quickSlot=Math.min(dp(activity,46),Math.max(dp(activity,36),remaining/3));
            }
            int quickBtnSize=Math.max(dp(activity,32),Math.min(dp(activity,42),Math.min(anchor.width(),quickSlot-dp(activity,4))));
            int totalHeight=nativeRows*slot+(showQuick?3*quickSlot:0);
            int height=Math.min(available,totalHeight);

            column.setVerticalScrollBarEnabled(totalHeight>available);column.setScrollbarFadingEnabled(false);
            if(totalHeight<=available)column.scrollTo(0,0);
            int[] origin=new int[2];root.getLocationOnScreen(origin);
            FrameLayout.LayoutParams right=(FrameLayout.LayoutParams)settingsSlot.getLayoutParams();
            // Mirror against the actual native column, including Maps' per-app language.
            // anchor.left is an outer inset only in LTR; in RTL it spans almost the whole screen.
            Rect visible=new Rect();root.getWindowVisibleDisplayFrame(visible);
            Rect sceneBounds=new Rect();View scene=root.findViewById(id(activity,"scene_card"));
            boolean sceneShown=scene!=null&&scene.getGlobalVisibleRect(sceneBounds)&&scene.isShown();
            // Next to Search: the opposite corner was hard to reach from the driver's seat.
            int beside=sceneShown?ClusterButtonPlacement.besideCard(visible.left,visible.right,anchor.width(),sceneBounds.left,sceneBounds.right,dp(activity,12),area.left,area.right):-1;
            Rect box=new Rect();View search=root.findViewById(id(activity,"main_suggested_destinations_search_box"));
            if(beside<0||search==null||!search.isShown()||!search.getGlobalVisibleRect(box)||box.top<sceneBounds.top||box.bottom>sceneBounds.bottom)box.setEmpty();
            int settingsX=(beside>=0?beside:ClusterButtonPlacement.settingsLeft(visible.left,visible.right,anchor.left,anchor.right,sceneBounds.left,sceneBounds.right))-origin[0];
            int top=(beside>=0?ClusterButtonPlacement.besideTop(anchor.top,box.top,box.bottom,anchor.height()):anchor.top)-origin[1];
            if(right.width!=anchor.width()||right.height!=anchor.height()||right.leftMargin!=settingsX||right.topMargin!=top){right.width=anchor.width();right.height=anchor.height();right.gravity=Gravity.TOP|Gravity.LEFT;right.leftMargin=settingsX;right.rightMargin=0;right.topMargin=top;settingsSlot.setLayoutParams(right);}
            FrameLayout.LayoutParams sb=(FrameLayout.LayoutParams)settings.getLayoutParams();if(sb.width!=anchor.width()||sb.height!=anchor.height()){sb.width=anchor.width();sb.height=anchor.height();settings.setLayoutParams(sb);}
            settingsSlot.setVisibility(View.VISIBLE);
            if(height<dp(template.getContext(),48)){column.setVisibility(View.INVISIBLE);style();updateState();return true;}
            int x=area.left-origin[0], y=end-origin[1], width=area.width();
            if(tx!=x||ty!=y||tw!=width||th!=height){
                tx=x;ty=y;tw=width;th=height;FrameLayout.LayoutParams lp=(FrameLayout.LayoutParams)column.getLayoutParams();lp.width=width;lp.height=height;lp.leftMargin=x;lp.topMargin=y;column.setLayoutParams(lp);
            }
            for(ImageButton b:new ImageButton[]{map,voice}){
                View slotView=(View)b.getParent();ViewGroup.LayoutParams sp=slotView.getLayoutParams();
                if(sp.width!=width||sp.height!=slot){sp.width=width;sp.height=slot;slotView.setLayoutParams(sp);}
                FrameLayout.LayoutParams bp=(FrameLayout.LayoutParams)b.getLayoutParams();
                if(bp.width!=anchor.width()||bp.height!=template.getHeight()){bp.width=anchor.width();bp.height=template.getHeight();b.setLayoutParams(bp);}
            }
            for(ImageButton b:new ImageButton[]{auth,restore,zoom}){
                View slotView=(View)b.getParent();ViewGroup.LayoutParams sp=slotView.getLayoutParams();
                if(sp.width!=width||sp.height!=quickSlot){sp.width=width;sp.height=quickSlot;slotView.setLayoutParams(sp);}
                FrameLayout.LayoutParams bp=(FrameLayout.LayoutParams)b.getLayoutParams();
                if(bp.width!=quickBtnSize||bp.height!=quickBtnSize){bp.width=quickBtnSize;bp.height=quickBtnSize;bp.gravity=Gravity.CENTER;b.setLayoutParams(bp);}
            }
            style();updateState();column.setVisibility(View.VISIBLE);
            return true;
        }
        int colour(String name,int fallback){try{int i=MapsResources.id(activity.getResources(),name,"color");return i==0?fallback:activity.getResources().getColor(i,activity.getTheme());}catch(RuntimeException e){return fallback;}}
        void style(){
            int ink=colour("geo_sys_color_on_surface",0xff303134),surface=colour("geo_sys_color_surface",Color.WHITE),active=colour("geo_sys_color_primary",0xff0b57d0);
            if(styled&&ink==lastColour&&surface==lastSurface&&active==lastActive)return;
            lastColour=ink;lastSurface=surface;lastActive=active;nativeBackground=null;
            View v=template;for(int i=0;i<4&&v!=bar;i++){
                if(v.getBackground()!=null&&v.getBackground().getConstantState()!=null){nativeBackground=v.getBackground();break;}
                if(!(v.getParent() instanceof View))break;v=(View)v.getParent();
            }
            for(ImageButton b:new ImageButton[]{settings,map,voice,auth,restore,zoom}){b.setBackground(background(surface));b.setClipToOutline(true);}
            float elevation=template.getElevation();for(ImageButton b:new ImageButton[]{settings,map,voice,auth,restore,zoom})b.setElevation(Math.max(elevation,dp(activity,2)));
            settingsIcon.colour=ink;settingsIcon.invalidateSelf();voiceIcon.colour=ink;voiceIcon.invalidateSelf();styled=true;wasOn=!ClusterSettings.state().mapEnabled;
            authIcon.colour=0xff38B6FF;authIcon.invalidateSelf();
            restoreIcon.colour=0xffFF914D;restoreIcon.invalidateSelf();
            zoomIcon.colour=0xff00C853;zoomIcon.invalidateSelf();
            updateState();
        }
        Drawable background(int colour){
            if(nativeBackground!=null)try{return nativeBackground.getConstantState().newDrawable().mutate();}catch(RuntimeException ignored){}
            float r=dp(activity,36);GradientDrawable shape=new GradientDrawable();shape.setColor(colour);shape.setCornerRadius(r);
            return new RippleDrawable(ColorStateList.valueOf((lastColour&0xffffff)|0x20000000),shape,null);
        }
        void updateState(){
            boolean on=ClusterSettings.state().mapEnabled;if(wasOn==on)return;wasOn=on;
            mapIcon.active=on;mapIcon.colour=on?lastActive:lastColour;mapIcon.invalidateSelf();
            map.setContentDescription(on?"Gsterge haritasn gizle":"Gsterge haritasn gster");
            map.setTooltipText(on?"Gsterge haritasn gizle":"Gsterge haritasn gster");
        }
    }
    static final class Icon extends Drawable {
        final boolean isMap,isVoice; int kind=0; boolean active; int colour=0xff303134; final Paint paint=new Paint(1);
        Icon(boolean map){this(map,false);}
        Icon(boolean map,boolean voice){isMap=map;isVoice=voice;paint.setStyle(Paint.Style.STROKE);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);}
        Icon(int kind){this.isMap=false;this.isVoice=false;this.kind=kind;paint.setStyle(Paint.Style.STROKE);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);}
        public void draw(Canvas canvas){
            Rect b=getBounds();if(b.width()<=0||b.height()<=0)return;
            canvas.save();canvas.translate(b.left,b.top);canvas.scale(b.width()/48f,b.height()/48f);
            paint.setColor(colour);paint.setStrokeWidth(2.6f);
            if(isVoice){
                paint.setStrokeWidth(2.8f);
                for(int[] bar:new int[][]{{15,20,28},{21,14,34},{27,11,37},{33,18,30}}){canvas.drawLine(bar[0],bar[1],bar[0],bar[2],paint);}
            }else if(isMap){
                paint.setStyle(active?Paint.Style.FILL:Paint.Style.STROKE);
                Path p=new Path();p.moveTo(13,15);p.lineTo(13,31);p.lineTo(19,34);p.lineTo(29,29);p.lineTo(35,32);p.lineTo(35,17);p.lineTo(29,14);p.lineTo(19,19);p.close();canvas.drawPath(p,paint);
                canvas.drawLine(19,19,19,34,paint);canvas.drawLine(29,14,29,29,paint);
            }else if(kind==3){
                // "Y" icon (Yetkilendir)
                paint.setStrokeWidth(3.2f);
                Path y=new Path();
                y.moveTo(15,14);y.lineTo(24,24);y.lineTo(33,14);
                y.moveTo(24,24);y.lineTo(24,34);
                canvas.drawPath(y,paint);
            }else if(kind==4){
                // "O" icon (Orijinal gösterge)
                paint.setStrokeWidth(3.2f);
                canvas.drawCircle(24,24,10,paint);
            }else if(kind==5){
                // "U" icon (Uzaklaştır)
                paint.setStrokeWidth(3.2f);
                Path u=new Path();
                u.moveTo(16,14);u.lineTo(16,27);
                u.quadTo(16,34,24,34);
                u.quadTo(32,34,32,27);
                u.lineTo(32,14);
                canvas.drawPath(u,paint);
            }else{
                canvas.drawRoundRect(13,15,35,33,4,4,paint);canvas.drawArc(17,19,31,31,195,150,false,paint);
                canvas.drawLine(24,27,28,22,paint);canvas.drawLine(20,30,28,30,paint);
            }
            canvas.restore();
        }
        public void setAlpha(int a){paint.setAlpha(a);}public void setColorFilter(ColorFilter f){paint.setColorFilter(f);}public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    }
}
