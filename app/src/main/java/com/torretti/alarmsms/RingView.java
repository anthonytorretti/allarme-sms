package com.torretti.alarmsms;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.animation.LinearInterpolator;

/** Big status ring: pulsing outer halo, coloured ring, icon in the middle, spinner while busy. */
public class RingView extends View {
    private final Paint halo = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint thin = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF arc = new RectF();
    private Drawable icon;
    private int color = BaseSmsActivity.GREY;
    private boolean busy;
    private float phase;      // 0..1 halo pulse
    private float spin;       // degrees
    private final ValueAnimator anim;

    public RingView(Context c) {
        super(c);
        halo.setStyle(Paint.Style.STROKE);
        thin.setStyle(Paint.Style.STROKE);
        thin.setColor(BaseSmsActivity.BORDER);
        ring.setStyle(Paint.Style.STROKE);
        ring.setStrokeCap(Paint.Cap.ROUND);
        fill.setColor(BaseSmsActivity.CARD);
        anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(2400);
        anim.setRepeatCount(ValueAnimator.INFINITE);
        anim.setInterpolator(new LinearInterpolator());
        anim.addUpdateListener(a -> {
            phase = (float) a.getAnimatedValue();
            spin = (spin + 9f) % 360f;
            invalidate();
        });
    }

    public void setState(int color, Drawable icon, boolean busy) {
        this.color = color;
        this.busy = busy;
        this.icon = icon == null ? null : icon.mutate();
        if (this.icon != null) this.icon.setTint(color);
        invalidate();
    }

    @Override protected void onAttachedToWindow() { super.onAttachedToWindow(); anim.start(); }
    @Override protected void onDetachedFromWindow() { anim.cancel(); super.onDetachedFromWindow(); }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight();
        float cx = w / 2f, cy = h / 2f, r = Math.min(w, h) / 2f;
        float d = getResources().getDisplayMetrics().density;

        // pulsing halo
        float base = r * 0.76f;
        halo.setStrokeWidth(2 * d);
        halo.setColor(color);
        halo.setAlpha((int) (140 * (1f - phase)));
        c.drawCircle(cx, cy, base + (r - base) * phase, halo);

        // thin guide ring
        thin.setStrokeWidth(1 * d);
        c.drawCircle(cx, cy, r * 0.86f, thin);

        // main disc + coloured ring
        float inner = r * 0.74f;
        c.drawCircle(cx, cy, inner, fill);
        ring.setStrokeWidth(6 * d);
        ring.setColor(color);
        if (busy) {
            ring.setAlpha(60);
            c.drawCircle(cx, cy, inner - 3 * d, ring);
            ring.setAlpha(255);
            arc.set(cx - inner + 3 * d, cy - inner + 3 * d, cx + inner - 3 * d, cy + inner - 3 * d);
            c.drawArc(arc, spin, 90, false, ring);
        } else {
            c.drawCircle(cx, cy, inner - 3 * d, ring);
        }

        if (icon != null && !busy) {
            int s = (int) (inner * 0.75f);
            icon.setBounds((int) (cx - s / 2f), (int) (cy - s / 2f), (int) (cx + s / 2f), (int) (cy + s / 2f));
            icon.draw(c);
        }
    }
}
