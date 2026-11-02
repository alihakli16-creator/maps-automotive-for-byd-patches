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
    private static final String TAG = "local.cluster.buttons";
    private static final int BG_COLOR = 0xFF181818; // Tam koyu mat siyah (#181818) - orijinal Google Maps butonlarıyla birebir aynı

    static void attach(Activity a) {
        if (a.isFinishing() || a.isDestroyed()) return;
        ViewGroup root = (ViewGroup) a.getWindow().getDecorView();
        if (root.findViewWithTag(TAG) != null) return;
        new Controls(a, root);
    }

    private static int id(Context c, String name) {
        return MapsResources.id(c.getResources(), name, "id");
    }

    private static int dp(Context c, int n) {
        return Math.round(n * c.getResources().getDisplayMetrics().density);
    }

    static Drawable makeCircleBackground(Context ctx) {
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(BG_COLOR);

        GradientDrawable mask = new GradientDrawable();
        mask.setShape(GradientDrawable.OVAL);
        mask.setColor(Color.WHITE);

        ColorStateList ripple = ColorStateList.valueOf(0x33FFFFFF);
        return new RippleDrawable(ripple, circle, mask);
    }

    static void applyCircleStyle(ImageButton b, Context ctx, float elevation) {
        b.setBackground(makeCircleBackground(ctx));
        b.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View v, Outline o) {
                o.setOval(0, 0, v.getWidth(), v.getHeight());
            }
        });
        b.setClipToOutline(true);
        b.setElevation(Math.max(elevation, dp(ctx, 3)));
    }

    static void applyPillStyle(View pill, Context ctx, float radius, float elevation) {
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        shape.setCornerRadius(radius);
        shape.setColor(BG_COLOR);
        pill.setBackground(shape);
        pill.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View v, Outline o) {
                o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), v.getWidth() / 2f);
            }
        });
        pill.setClipToOutline(true);
        pill.setElevation(Math.max(elevation, dp(ctx, 3)));
    }

    private static final class Controls implements ViewTreeObserver.OnPreDrawListener {
        final Activity activity; final ViewGroup root; final ScrollView column; final LinearLayout buttons;
        final ImageButton settings, map, voice, auth, restore;
        final LinearLayout zoomPill;
        final ImageButton zoomIn, zoomOut;
        final FrameLayout settingsSlot, voiceSlot, authSlot, restoreSlot, zoomSlot;
        final Icon settingsIcon = new Icon(false), mapIcon = new Icon(true), voiceIcon = new Icon(false, true),
                   authIcon = new Icon(3), restoreIcon = new Icon(4),
                   zoomInIcon = new Icon(6), zoomOutIcon = new Icon(7);
        View template, bar, bottom;
        boolean styled, wasOn; int tx, ty, tw, th;

        Controls(Activity a, ViewGroup r) {
            activity = a; root = r; column = new ScrollView(a); column.setTag(TAG); column.setFillViewport(false);
            column.setClipToPadding(false); column.setClipChildren(false); column.setVerticalScrollBarEnabled(false);
            buttons = new LinearLayout(a); buttons.setOrientation(LinearLayout.VERTICAL); buttons.setClipChildren(false);

            settings = button(settingsIcon, "Gösterge paneli ayarları");
            map = button(mapIcon, "Gösterge haritasını gizle");
            settingsSlot = (FrameLayout) settings.getParent();
            buttons.removeView(settingsSlot);

            voice = button(voiceIcon, "Gemini Live ile konuş");
            voiceSlot = (FrameLayout) voice.getParent();

            auth = button(authIcon, "Yetkilendir veya tekrar dene");
            authSlot = (FrameLayout) auth.getParent();

            restore = button(restoreIcon, "Orijinal göstergeyi geri yükle");
            restoreSlot = (FrameLayout) restore.getParent();

            // Yeşil "+ / -" dikey kapsül kontrolü
            zoomSlot = new FrameLayout(a);
            zoomSlot.setClipChildren(false);

            zoomPill = new LinearLayout(a);
            zoomPill.setOrientation(LinearLayout.VERTICAL);
            zoomPill.setGravity(Gravity.CENTER_HORIZONTAL);
            zoomPill.setTag("local.cluster.dilink3.zoom.pill");
            applyPillStyle(zoomPill, a, dp(a, 19), dp(a, 3));

            zoomIn = new ImageButton(a);
            zoomIn.setImageDrawable(zoomInIcon);
            zoomIn.setScaleType(ImageView.ScaleType.FIT_CENTER);
            zoomIn.setPadding(0, 0, 0, 0);
            zoomIn.setContentDescription("Gösterge haritasını yakınlaştır");
            zoomIn.setTooltipText("Gösterge haritasını yakınlaştır");
            zoomIn.setFocusable(true);
            zoomIn.setTag("local.cluster.dilink3.zoom_in.button");
            zoomIn.setBackground(new RippleDrawable(ColorStateList.valueOf(0x33FFFFFF), null, null));
            zoomIn.setOnClickListener(v -> {
                if (ClusterZoom.manualStep(1)) {
                    ClusterSettings.adjustClusterZoomSteps(1);
                    int s = ClusterSettings.getClusterZoomSteps();
                    Toast.makeText(activity, "Gösterge paneli haritası yakınlaştırıldı (Kalıcı: " + (s > 0 ? "+" + s : "" + s) + " kademe)", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(activity, "Gösterge paneli haritasının hazır olmasını bekleyin.", Toast.LENGTH_SHORT).show();
                }
            });

            View divider = new View(a);
            divider.setBackgroundColor(0x4DFFFFFF); // #4DFFFFFF zarif yatay ayırıcı çizgi

            zoomOut = new ImageButton(a);
            zoomOut.setImageDrawable(zoomOutIcon);
            zoomOut.setScaleType(ImageView.ScaleType.FIT_CENTER);
            zoomOut.setPadding(0, 0, 0, 0);
            zoomOut.setContentDescription("Gösterge haritasını uzaklaştır");
            zoomOut.setTooltipText("Gösterge haritasını uzaklaştır");
            zoomOut.setFocusable(true);
            zoomOut.setTag("local.cluster.dilink3.zoom_out.button");
            zoomOut.setBackground(new RippleDrawable(ColorStateList.valueOf(0x33FFFFFF), null, null));
            zoomOut.setOnClickListener(v -> {
                if (ClusterZoom.manualStep(-1)) {
                    ClusterSettings.adjustClusterZoomSteps(-1);
                    int s = ClusterSettings.getClusterZoomSteps();
                    Toast.makeText(activity, "Gösterge paneli haritası uzaklaştırıldı (Kalıcı: " + (s > 0 ? "+" + s : "" + s) + " kademe)", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(activity, "Gösterge paneli haritasının hazır olmasını bekleyin.", Toast.LENGTH_SHORT).show();
                }
            });

            zoomPill.addView(zoomIn, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
            LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(a, 1));
            divParams.leftMargin = dp(a, 6);
            divParams.rightMargin = dp(a, 6);
            zoomPill.addView(divider, divParams);
            zoomPill.addView(zoomOut, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

            zoomSlot.addView(zoomPill, new FrameLayout.LayoutParams(1, 1, Gravity.CENTER));
            buttons.addView(zoomSlot, new LinearLayout.LayoutParams(1, 1));

            settings.setTag("local.cluster.settings.button");
            map.setTag("local.cluster.map.button");
            voice.setTag("local.cluster.gemini.button");
            auth.setTag("local.cluster.dilink3.auth.button");
            restore.setTag("local.cluster.dilink3.restore.button");

            GeminiLaunch.init(a);
            voice.setOnClickListener(v -> GeminiLaunch.open(a));
            settings.setOnClickListener(v -> ClusterSettings.show(a));
            map.setOnClickListener(v -> { ClusterSettings.toggleMap(); updateState(); });

            auth.setOnClickListener(v -> {
                Toast.makeText(activity, "ADB yetkilendirme ve gösterge yansıtma başlatılıyor…", Toast.LENGTH_SHORT).show();
                DiLink3Projection.init(activity.getApplication(), null);
                DiLink3Projection.retry(activity, true, null);
            });

            restore.setOnClickListener(v -> {
                Toast.makeText(activity, "Orijinal gösterge düzenine dönüldü", Toast.LENGTH_SHORT).show();
                DiLink3Projection.init(activity.getApplication(), null);
                DiLink3Projection.restore(activity, null);
            });

            column.addView(buttons);
            column.setVisibility(View.INVISIBLE);
            root.addView(column, new FrameLayout.LayoutParams(1, 1, Gravity.TOP | Gravity.LEFT));

            settingsSlot.setVisibility(View.INVISIBLE);
            root.addView(settingsSlot, new FrameLayout.LayoutParams(1, 1, Gravity.TOP | Gravity.LEFT));

            root.getViewTreeObserver().addOnPreDrawListener(this);
            root.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                public void onViewAttachedToWindow(View v) {}
                public void onViewDetachedFromWindow(View v) {
                    if (root.getViewTreeObserver().isAlive()) {
                        root.getViewTreeObserver().removeOnPreDrawListener(Controls.this);
                    }
                }
            });
        }

        ImageButton button(Icon icon, String description) {
            ImageButton b = new ImageButton(activity);
            b.setImageDrawable(icon);
            b.setScaleType(ImageView.ScaleType.FIT_CENTER);
            b.setPadding(0, 0, 0, 0);
            b.setContentDescription(description);
            b.setTooltipText(description);
            b.setFocusable(true);
            applyCircleStyle(b, activity, dp(activity, 3));
            FrameLayout slot = new FrameLayout(activity);
            slot.setClipChildren(false);
            slot.addView(b, new FrameLayout.LayoutParams(1, 1, Gravity.CENTER));
            buttons.addView(slot, new LinearLayout.LayoutParams(1, 1));
            return b;
        }

        public boolean onPreDraw() {
            ReportButtonRecovery.observe(root);
            if (activity.isDestroyed() || activity.isFinishing()) return true;
            if (template == null || !template.isAttachedToWindow()) {
                template = root.findViewById(id(activity, "map_buttons_view_settings_button"));
                bar = root.findViewById(id(activity, "one_bar_ellipsizing_container"));
                bottom = root.findViewById(id(activity, "one_bar_bottom_group"));
                styled = false;
            }
            if (template == null || bar == null || !bar.isShown() || activity.isInPictureInPictureMode() || template.getWidth() == 0) {
                column.setVisibility(View.INVISIBLE);
                settingsSlot.setVisibility(View.INVISIBLE);
                voiceSlot.setVisibility(View.INVISIBLE);
                authSlot.setVisibility(View.INVISIBLE);
                restoreSlot.setVisibility(View.INVISIBLE);
                zoomSlot.setVisibility(View.INVISIBLE);
                return true;
            }
            Rect anchor = new Rect(), area = new Rect();
            template.getGlobalVisibleRect(anchor);
            bar.getGlobalVisibleRect(area);
            int end = anchor.bottom;
            int slot = template.getHeight();
            if (bar instanceof ViewGroup) {
                ViewGroup g = (ViewGroup) bar;
                for (int i = 0; i < g.getChildCount(); i++) {
                    View child = g.getChildAt(i);
                    Rect bounds = new Rect();
                    if (child.isShown() && child.getHeight() > 0 && child.getGlobalVisibleRect(bounds)) {
                        end = Math.max(end, bounds.bottom);
                        if (bounds.top <= anchor.top && bounds.bottom >= anchor.bottom) slot = bounds.height();
                    }
                }
            }
            int limit = area.bottom;
            Rect obstacle = new Rect();
            if (bottom != null && bottom.isShown() && bottom.getGlobalVisibleRect(obstacle)) limit = Math.min(limit, obstacle.top);
            int nativeRows = GeminiLaunch.buttonEnabled() ? 2 : 1;
            voiceSlot.setVisibility(nativeRows == 2 ? View.VISIBLE : View.GONE);

            boolean showQuick = DiLink3Projection.isCandidate() && ClusterSettings.quickButtonsEnabled();
            if (showQuick && ClusterSettings.getClusterZoomSteps() != 0 && !ClusterZoom.isHeld()) {
                ClusterZoom.scheduleRestoreSavedSteps(800);
            }
            authSlot.setVisibility(showQuick ? View.VISIBLE : View.GONE);
            restoreSlot.setVisibility(showQuick ? View.VISIBLE : View.GONE);
            zoomSlot.setVisibility(showQuick ? View.VISIBLE : View.GONE);

            int available = Math.max(0, limit - end);
            int remaining = Math.max(0, available - nativeRows * slot);

            // Yeni "+ / -" kapsülü ve "Y" / "O" dairesel butonları için boyut ve boşluk optimizasyonu
            int pillHeight = Math.max(dp(activity, 68), Math.min(dp(activity, 74), Math.round(anchor.width() * 1.8f)));
            int pillSlot = pillHeight + dp(activity, 4);
            int quickSlot = dp(activity, 44);
            if (showQuick && remaining > 0) {
                int availForCircles = remaining - pillSlot;
                if (availForCircles > 0) {
                    quickSlot = Math.min(dp(activity, 46), Math.max(dp(activity, 34), availForCircles / 2));
                }
            }
            int quickBtnSize = Math.max(dp(activity, 32), Math.min(dp(activity, 42), Math.min(anchor.width(), quickSlot - dp(activity, 4))));
            pillHeight = Math.max(dp(activity, 68), Math.min(dp(activity, 74), Math.round(quickBtnSize * 1.85f)));
            pillSlot = pillHeight + dp(activity, 4);

            int totalHeight = nativeRows * slot + (showQuick ? (2 * quickSlot + pillSlot) : 0);
            int height = Math.min(available, totalHeight);

            column.setVerticalScrollBarEnabled(totalHeight > available);
            column.setScrollbarFadingEnabled(false);
            if (totalHeight <= available) column.scrollTo(0, 0);
            int[] origin = new int[2];
            root.getLocationOnScreen(origin);
            FrameLayout.LayoutParams right = (FrameLayout.LayoutParams) settingsSlot.getLayoutParams();
            Rect visible = new Rect();
            root.getWindowVisibleDisplayFrame(visible);
            Rect sceneBounds = new Rect();
            View scene = root.findViewById(id(activity, "scene_card"));
            boolean sceneShown = scene != null && scene.getGlobalVisibleRect(sceneBounds) && scene.isShown();
            int beside = sceneShown ? ClusterButtonPlacement.besideCard(visible.left, visible.right, anchor.width(), sceneBounds.left, sceneBounds.right, dp(activity, 12), area.left, area.right) : -1;
            Rect box = new Rect();
            View search = root.findViewById(id(activity, "main_suggested_destinations_search_box"));
            if (beside < 0 || search == null || !search.isShown() || !search.getGlobalVisibleRect(box) || box.top < sceneBounds.top || box.bottom > sceneBounds.bottom) box.setEmpty();
            int settingsX = (beside >= 0 ? beside : ClusterButtonPlacement.settingsLeft(visible.left, visible.right, anchor.left, anchor.right, sceneBounds.left, sceneBounds.right)) - origin[0];
            int top = (beside >= 0 ? ClusterButtonPlacement.besideTop(anchor.top, box.top, box.bottom, anchor.height()) : anchor.top) - origin[1];
            if (right.width != anchor.width() || right.height != anchor.height() || right.leftMargin != settingsX || right.topMargin != top) {
                right.width = anchor.width(); right.height = anchor.height(); right.gravity = Gravity.TOP | Gravity.LEFT; right.leftMargin = settingsX; right.rightMargin = 0; right.topMargin = top;
                settingsSlot.setLayoutParams(right);
            }
            FrameLayout.LayoutParams sb = (FrameLayout.LayoutParams) settings.getLayoutParams();
            if (sb.width != anchor.width() || sb.height != anchor.height()) {
                sb.width = anchor.width(); sb.height = anchor.height(); sb.gravity = Gravity.CENTER;
                settings.setLayoutParams(sb);
            }
            settingsSlot.setVisibility(View.VISIBLE);
            if (height < dp(template.getContext(), 48)) {
                column.setVisibility(View.INVISIBLE);
                style();
                updateState();
                return true;
            }
            int x = area.left - origin[0], y = end - origin[1], width = area.width();
            if (tx != x || ty != y || tw != width || th != height) {
                tx = x; ty = y; tw = width; th = height;
                FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) column.getLayoutParams();
                lp.width = width; lp.height = height; lp.leftMargin = x; lp.topMargin = y;
                column.setLayoutParams(lp);
            }
            for (ImageButton b : new ImageButton[]{map, voice}) {
                View slotView = (View) b.getParent();
                ViewGroup.LayoutParams sp = slotView.getLayoutParams();
                if (sp.width != width || sp.height != slot) {
                    sp.width = width; sp.height = slot; slotView.setLayoutParams(sp);
                }
                FrameLayout.LayoutParams bp = (FrameLayout.LayoutParams) b.getLayoutParams();
                if (bp.width != anchor.width() || bp.height != template.getHeight()) {
                    bp.width = anchor.width(); bp.height = template.getHeight(); bp.gravity = Gravity.CENTER;
                    b.setLayoutParams(bp);
                }
            }
            for (ImageButton b : new ImageButton[]{auth, restore}) {
                View slotView = (View) b.getParent();
                ViewGroup.LayoutParams sp = slotView.getLayoutParams();
                if (sp.width != width || sp.height != quickSlot) {
                    sp.width = width; sp.height = quickSlot; slotView.setLayoutParams(sp);
                }
                FrameLayout.LayoutParams bp = (FrameLayout.LayoutParams) b.getLayoutParams();
                if (bp.width != quickBtnSize || bp.height != quickBtnSize) {
                    bp.width = quickBtnSize; bp.height = quickBtnSize; bp.gravity = Gravity.CENTER;
                    b.setLayoutParams(bp);
                }
            }

            // Yeşil "+ / -" dikey kapsül boyutlandırması
            ViewGroup.LayoutParams zsp = zoomSlot.getLayoutParams();
            if (zsp.width != width || zsp.height != pillSlot) {
                zsp.width = width; zsp.height = pillSlot; zoomSlot.setLayoutParams(zsp);
            }
            FrameLayout.LayoutParams pp = (FrameLayout.LayoutParams) zoomPill.getLayoutParams();
            if (pp.width != quickBtnSize || pp.height != pillHeight) {
                pp.width = quickBtnSize; pp.height = pillHeight; pp.gravity = Gravity.CENTER;
                zoomPill.setLayoutParams(pp);
                applyPillStyle(zoomPill, activity, quickBtnSize / 2f, template != null ? template.getElevation() : dp(activity, 3));
            }

            style();
            updateState();
            column.setVisibility(View.VISIBLE);
            return true;
        }

        void style() {
            if (styled) return;
            float elevation = template != null ? template.getElevation() : dp(activity, 3);
            for (ImageButton b : new ImageButton[]{settings, map, voice, auth, restore}) {
                applyCircleStyle(b, activity, elevation);
            }
            float r = zoomPill.getWidth() > 0 ? zoomPill.getWidth() / 2f : dp(activity, 19);
            applyPillStyle(zoomPill, activity, r, elevation);
            styled = true;
            wasOn = !ClusterSettings.state().mapEnabled;
            updateState();
        }

        void updateState() {
            boolean on = ClusterSettings.state().mapEnabled;
            if (wasOn == on) return;
            wasOn = on;
            mapIcon.active = on;
            mapIcon.invalidateSelf();
            String desc = on ? "Gösterge haritasını gizle" : "Gösterge haritasını göster";
            map.setContentDescription(desc);
            map.setTooltipText(desc);
        }
    }

    static final class Icon extends Drawable {
        final boolean isMap, isVoice;
        int kind = 0;
        boolean active;
        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);

        Icon(boolean map) {
            this(map, false);
        }

        Icon(boolean map, boolean voice) {
            this.isMap = map;
            this.isVoice = voice;
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
        }

        Icon(int kind) {
            this.isMap = false;
            this.isVoice = false;
            this.kind = kind;
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
        }

        @Override
        public int getIntrinsicWidth() {
            return 48;
        }

        @Override
        public int getIntrinsicHeight() {
            return 48;
        }

        public void draw(Canvas canvas) {
            Rect b = getBounds();
            if (b.width() <= 0 || b.height() <= 0) return;
            canvas.save();
            canvas.translate(b.left, b.top);
            canvas.scale(b.width() / 48f, b.height() / 48f);

            if (isVoice) {
                // Gemini Live ses dalgası (vivid cyan blue bars)
                paint.setColor(0xFF38B6FF);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(2.8f);
                paint.setStrokeCap(Paint.Cap.ROUND);
                for (int[] bar : new int[][]{{15, 20, 28}, {21, 14, 34}, {27, 11, 37}, {33, 18, 30}}) {
                    canvas.drawLine(bar[0], bar[1], bar[0], bar[2], paint);
                }
            } else if (isMap) {
                // Gösterge ekranı butonu (Dashboard screen with speedometer)
                paint.setColor(active ? 0xFF38B6FF : 0xFFE8EAED);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(2.0f);
                paint.setStrokeCap(Paint.Cap.ROUND);
                paint.setStrokeJoin(Paint.Join.ROUND);
                canvas.drawRoundRect(12, 16, 36, 32, 4, 4, paint);
                canvas.drawArc(15, 20, 23, 28, 185, 170, false, paint);
                canvas.drawArc(25, 20, 33, 28, 185, 170, false, paint);
                canvas.drawLine(19, 25, 21, 22, paint);
                canvas.drawLine(29, 25, 27, 22, paint);
                if (active) {
                    paint.setStyle(Paint.Style.FILL);
                    paint.setColor(0xFF38B6FF);
                    canvas.drawCircle(33, 33, 4.5f, paint);
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setColor(Color.WHITE);
                    paint.setStrokeWidth(1.4f);
                    Path p = new Path();
                    p.moveTo(30.8f, 33);
                    p.lineTo(32.4f, 34.5f);
                    p.lineTo(35.4f, 31.4f);
                    canvas.drawPath(p, paint);
                } else {
                    paint.setColor(0xFF9AA0A6);
                    paint.setStrokeWidth(1.8f);
                    canvas.drawLine(14, 14, 34, 34, paint);
                }
            } else if (kind == 3) {
                // "Y" icon (Yetkilendir / Auth) - Canlı Cyan Mavi
                paint.setColor(0xFF38B6FF);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(3.4f);
                paint.setStrokeCap(Paint.Cap.ROUND);
                paint.setStrokeJoin(Paint.Join.ROUND);
                Path y = new Path();
                y.moveTo(15, 14); y.lineTo(24, 24); y.lineTo(33, 14);
                y.moveTo(24, 24); y.lineTo(24, 34);
                canvas.drawPath(y, paint);
            } else if (kind == 4) {
                // "O" icon (Orijinal göstergeyi geri yükle / Restore) - Canlı Turuncu
                paint.setColor(0xFFFFA726);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(3.4f);
                paint.setStrokeCap(Paint.Cap.ROUND);
                paint.setStrokeJoin(Paint.Join.ROUND);
                canvas.drawCircle(24, 24, 10.5f, paint);
            } else if (kind == 6) {
                // "+" icon (Göstergeyi Yakınlaştır / Zoom In) - Canlı Yeşil #00E676
                paint.setColor(0xFF00E676);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(3.4f);
                paint.setStrokeCap(Paint.Cap.ROUND);
                paint.setStrokeJoin(Paint.Join.ROUND);
                canvas.drawLine(15, 24, 33, 24, paint);
                canvas.drawLine(24, 15, 24, 33, paint);
            } else if (kind == 7) {
                // "−" icon (Göstergeyi Uzaklaştır / Zoom Out) - Canlı Yeşil #00E676
                paint.setColor(0xFF00E676);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(3.4f);
                paint.setStrokeCap(Paint.Cap.ROUND);
                paint.setStrokeJoin(Paint.Join.ROUND);
                canvas.drawLine(15, 24, 33, 24, paint);
            } else if (kind == 5) {
                // "U" icon (Eski tekil buton uyumluluğu)
                paint.setColor(0xFF00E676);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(3.4f);
                paint.setStrokeCap(Paint.Cap.ROUND);
                paint.setStrokeJoin(Paint.Join.ROUND);
                Path u = new Path();
                u.moveTo(16, 14);
                u.lineTo(16, 27);
                u.quadTo(16, 34, 24, 34);
                u.quadTo(32, 34, 32, 27);
                u.lineTo(32, 14);
                canvas.drawPath(u, paint);
            } else {
                // Sağ üst köşedeki Gösterge Ayarları ikonu (Cluster Settings screen)
                paint.setColor(0xFFE8EAED);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(2.0f);
                paint.setStrokeCap(Paint.Cap.ROUND);
                paint.setStrokeJoin(Paint.Join.ROUND);
                canvas.drawRoundRect(13, 15, 35, 33, 4, 4, paint);
                canvas.drawArc(17, 19, 31, 31, 195, 150, false, paint);
                canvas.drawLine(24, 27, 28, 22, paint);
                canvas.drawLine(20, 30, 28, 30, paint);
            }
            canvas.restore();
        }

        public void setAlpha(int a) {
            paint.setAlpha(a);
        }

        public void setColorFilter(ColorFilter f) {
            paint.setColorFilter(f);
        }

        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }
}
