package com.github.douwe.bikenolight;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

public class BikeToggleButton extends View {

    private final int BLOCK_SIZE = 1000;
    private final Paint mPaintBlue;
    private final Paint mPaintLightBlue;
    private final Paint mPaintFillCenter;
    private final Paint mPaintFillCenterHovered;

    private int rotationFactors[] = new int[8];

    private boolean isActive = false;
    private Listener mListener;
    private boolean pressedDown = false;

    public BikeToggleButton(Context context) {
        this(context, null);
    }

    public BikeToggleButton(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mPaintBlue = createBluePaint();
        mPaintLightBlue = createLightBluePaint();
        mPaintFillCenter = createPaintFillCenter(false);
        mPaintFillCenterHovered = createPaintFillCenter(true);
        setOnClickListener(v -> {
            isActive = !isActive;
            if (mListener != null) {
                mListener.toggled(isActive);
            }
        });

        Thread t = new Thread(() -> {
            runLoop();
        });
        t.start();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                pressedDown = true;
                break;
            case MotionEvent.ACTION_UP:
                pressedDown = false;
                break;

        }
        return super.onTouchEvent(event);
    }

    public void setListener(Listener mListener) {
        this.mListener = mListener;
        if (mListener != null) {
            mListener.toggled(isActive);
        }
    }

    private Paint createPaintFillCenter(boolean hovering) {
        Paint result = new Paint();
        result.setStyle(Paint.Style.FILL);
        result.setColor(hovering ? Color.GREEN : Color.WHITE);
        return result;
    }

    private void runLoop() {

        boolean oldIsActive = false;
        while(true) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (oldIsActive!=isActive) {
                oldIsActive = isActive;
                if (!oldIsActive) {
                    for(int idx=0; idx<rotationFactors.length; idx++) {
                        rotationFactors[idx] = rotationFactors[idx]% BLOCK_SIZE;
                    }
                }
            }
            if (isActive) {
                rotationFactors[0] += 29;
                rotationFactors[1] += 13;
                rotationFactors[2] += 17;
                rotationFactors[3] += 11;
                rotationFactors[4] += 11;
                rotationFactors[5] += 17;
                rotationFactors[6] += 23;
                rotationFactors[7] += 13;
            } else {
                for(int idx=0; idx<rotationFactors.length; idx++) {
                    rotationFactors[idx] = (rotationFactors[idx]*4)/5;
                }
            }
            Handler handler = getHandler();
            if (handler != null) {
                handler.post(() -> {
                    invalidate();
                });
            }
        }
    }

    private Paint createBluePaint() {
        Paint result = new Paint();
        result.setStrokeWidth(60);
        result.setStyle(Paint.Style.STROKE);
        result.setColor(Color.BLUE);
        return result;
    }

    private Paint createLightBluePaint() {
        Paint result = new Paint();
        result.setStrokeWidth(60);
        result.setStyle(Paint.Style.STROKE);
        result.setColor(Color.CYAN);
        result.setAlpha(128);
        return result;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();

        int size = width<height ? width : height;
        float size2 = size / 2f;

        float centerX = width/2f;
        float centerY = height/2f;


        float sw = mPaintBlue.getStrokeWidth();

        float insets = 20;


        float r[] = new float[8];
        for(int idx=0; idx<r.length; idx++) {
            double alpha = rotationFactors[idx] * Math.PI / BLOCK_SIZE;
            r[idx] = (float) (Math.sin(alpha)*insets);
        }

//        float r1 = (float) Math.sin(rotation*0.0643/Math.PI)*insets;
//        float r2 = (float) Math.sin(rotation*0.073/Math.PI)*insets;
//        float r3 = (float) Math.sin(rotation*0.0713/Math.PI)*insets;
//        float r4 = (float) Math.sin(rotation*0.0693/Math.PI)*insets;

        insets += sw/2;

        float bx0 = centerX - size2 + insets + r[0];
        float by0 = centerY - size2 + insets + r[1];
        float bx1 = centerX + size2 - insets + r[2];
        float by1 = centerY + size2 - insets + r[3];
        canvas.drawArc(bx0,by0,bx1,by1,0,360,true, mPaintBlue);

        bx0 = centerX - size2 + insets + r[4];
        by0 = centerY - size2 + insets + r[5];
        bx1 = centerX + size2 - insets + r[6];
        by1 = centerY + size2 - insets + r[7];
        canvas.drawArc(bx0,by0,bx1,by1,0,360,true, mPaintLightBlue);

        size2 = size2*0.8f;
        bx0 = centerX - size2;
        by0 = centerY - size2;
        bx1 = centerX + size2;
        by1 = centerY + size2;
        canvas.drawArc(bx0,by0,bx1,by1,0,360,true, pressedDown ? mPaintFillCenterHovered : mPaintFillCenter);

        Paint p = new Paint();
        Rect bounds = new Rect();
        p.setTextSize(80);
        p.getTextBounds("BK", 0, 2, bounds);
        int w = bounds.width();

        canvas.drawText("BK", centerX-w/2, centerY, p);
    }

    @FunctionalInterface
    public interface Listener {
        public void toggled(boolean isActive);
    }

}
