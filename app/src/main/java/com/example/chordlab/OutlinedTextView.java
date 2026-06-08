package com.example.chordlab;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.TextPaint;
import android.util.AttributeSet;
import androidx.appcompat.widget.AppCompatTextView;

public class OutlinedTextView extends AppCompatTextView {

    private static final int   OUTLINE_COLOR    = 0xFFE91E8C; // Pink outline
    private static final float OUTLINE_WIDTH_DP = 3f;
    private boolean isDrawing = false;

    public OutlinedTextView(Context context) { super(context); }
    public OutlinedTextView(Context context, AttributeSet attrs) { super(context, attrs); }
    public OutlinedTextView(Context context, AttributeSet attrs, int defStyleAttr) { super(context, attrs, defStyleAttr); }

    @Override
    public void invalidate() {
        if (isDrawing) return;
        super.invalidate();
    }

    @Override
    public void requestLayout() {
        if (isDrawing) return;
        super.requestLayout();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // ── LAYOUT PREVIEW FALLBACK ──
        if (isInEditMode()) {
            // Renders standard plain text in Android Studio so it doesn't vanish
            super.onDraw(canvas);
            return;
        }

        // ── LIVE DEVICE RUNTIME DRAWING ──
        isDrawing = true;

        // Save original text color configuration
        int originalColor = getCurrentTextColor();
        TextPaint paint = getPaint();

        // Pass 1: Render thick outer stroke outline
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(OUTLINE_WIDTH_DP * getResources().getDisplayMetrics().density);
        paint.setStrokeJoin(Paint.Join.ROUND);
        setTextColor(OUTLINE_COLOR);
        super.onDraw(canvas);

        // Pass 2: Render standard text fill right on top of the stroke
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(0);
        setTextColor(originalColor);
        super.onDraw(canvas);

        isDrawing = false;
    }
}