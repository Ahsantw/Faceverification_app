package com.example.myapplication;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import java.util.Collections;
import java.util.List;

public class OverlayView extends View {
    private List<RectF> boxes = Collections.emptyList();
    private float cosSim = 0;
    private Paint boxPaint;

    public OverlayView(Context context) {
        super(context);
        init();
    }

    public OverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        boxPaint = new Paint();
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(6f);
        boxPaint.setAlpha(200);
    }

    public void setBoxes(List<RectF> boxes, float cosSim) {
        this.boxes = boxes;
        this.cosSim = cosSim;
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        for (RectF box : boxes) {
            if (cosSim == 0) {
                boxPaint.setColor(Color.RED); // No known face
            } else if (cosSim > 0.8) {
                boxPaint.setColor(Color.GREEN); // Good match
            } else {
                boxPaint.setColor(Color.YELLOW); // Uncertain
            }

            canvas.drawRect(box, boxPaint);
        }
    }
}
