package id.my.personne.library;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by surya on 9/16/17.
 */

public class DrawableView extends View {
    private final Float StrokeWidth = 5f;
    private final Float HalfStrokeWidth = StrokeWidth / 2;
    private Paint paint = new Paint();
    private Path path = new Path();

    private Float lastX;
    private Float lastY;
    private final RectF rectF = new RectF();

    private OnTouchView touchEvent;

    public DrawableView(Context context) {
        super(context);
        preparePaint();
    }

    public DrawableView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        preparePaint();
    }

    public DrawableView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        preparePaint();
    }

    private void preparePaint() {
        paint.setAntiAlias(true);
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeWidth(StrokeWidth);
    }

    public void setStrokeWidth(float width) {
        paint.setStrokeWidth(width);
    }

    public void setTouchViewEvent(OnTouchView touchEvent) {
        this.touchEvent = touchEvent;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawPath(path, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        try {
            if (touchEvent != null) {
                touchEvent.onTouch(event);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        float eX = event.getX();
        float eY = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                path.moveTo(eX, eY);
                lastX = eX;
                lastY = eY;
                return true;
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_UP:
                resetDirtyRect(eX, eY);
                int historySize = event.getHistorySize();
                for (int n = 0; n < historySize; n++) {
                    float historyX = event.getHistoricalX(n);
                    float historyY = event.getHistoricalY(n);
                    expandDirtyRect(historyX, historyY);
                    path.lineTo(historyX, historyY);
                }
                path.lineTo(eX, eY);
                break;
            default:
                return false;
        }

        invalidate((int) (rectF.left - HalfStrokeWidth), (int) (rectF.top - HalfStrokeWidth),
                (int) (rectF.right + HalfStrokeWidth), (int) (rectF.bottom + HalfStrokeWidth));

        lastX = eX;
        lastY = eY;
        return true;
    }

    private void expandDirtyRect(float historicalX, float historicalY) {
        if (historicalX < rectF.left) {
            rectF.left = historicalX;
        } else if (historicalX > rectF.right) {
            rectF.right = historicalX;
        }

        if (historicalY < rectF.top) {
            rectF.top = historicalY;
        } else if (historicalY > rectF.bottom) {
            rectF.bottom = historicalY;
        }
    }

    private void resetDirtyRect(float eventX, float eventY) {
        rectF.left = Math.min(lastX, eventX);
        rectF.right = Math.max(lastX, eventX);
        rectF.top = Math.min(lastY, eventY);
        rectF.bottom = Math.max(lastY, eventY);
    }

    public Bitmap getImageBitmap() {
        Bitmap signed = Bitmap.createBitmap(this.getWidth(), this.getHeight(), Bitmap.Config.RGB_565);
        final Canvas canvas = new Canvas(signed);
        this.draw(canvas);
        return signed;
    }

    public void clearView() {
        path.reset();
        this.invalidate();
    }

    public interface OnTouchView {
        void onTouch(MotionEvent e);
    }
}
