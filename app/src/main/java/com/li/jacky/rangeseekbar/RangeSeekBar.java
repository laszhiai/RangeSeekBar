package com.li.jacky.rangeseekbar;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

/**
 * Created by Jacky on 2017/6/20.
 *  区间滑块
 */

public class RangeSeekBar<T extends Number> extends AppCompatImageView {

    private static final Integer DEFAULT_MINIMUM = 0;
    private static final Integer DEFAULT_MAXIMUM = 100;
    private T absoluteMinValue, absoluteMaxValue;
    private double minValuePrim, maxValuePrim;//最值
    private static final int DEFAULT_TEXT_SIZE_IN_DP = 14;
    private static final int DEFAULT_TEXT_DISTANCE_TO_TOP_IN_DP = 10;
    private static final int SEEKBAR_OUTER_LAYER = 24;
    private static final int SEEKBAR_INTER_LAYER = 8;
    private static final int PADDING = 40;
    private int mTextSize;
    private static final int TEXT_OFFSET = 10;
    private int mDistanceToTop;
    private int mScaledTouchSlop;
    private RectF mRect;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Bitmap thumbImage = BitmapFactory.decodeResource(getResources(), R.drawable.seek_thumb_normal);
    private final float thumbWidth = thumbImage.getWidth();
    private final float thumbHalfWidth = 0.5f * thumbWidth;
    private final float thumbHalfHeight = 0.5f * thumbImage.getHeight();
    private double normalizedMinValue = 0d;
    private double normalizedMaxValue = 1d;
    private OnRangeSeekBarChangeListener<T> listener;
    private int mActivePointerId;
    private float mDownMotionX;
    private Thumb pressedThumb = null;
    private boolean mIsDragging;
    private boolean notifyWhileDragging;
    public static final int ACTION_POINTER_UP = 0x6, ACTION_POINTER_INDEX_MASK = 0x0000ff00, ACTION_POINTER_INDEX_SHIFT = 8;

    public RangeSeekBar(Context context) {
        super(context);
        init(context, null);
    }

    public RangeSeekBar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public RangeSeekBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        if (attrs == null) {
            setRangeToDefaultValues();
        } else {
            TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.IFBIRangeSeekBar, 0, 0);
            setNumberRange(
                getNumericValueFromAttributes(ta, R.styleable.IFBIRangeSeekBar_MinValue, DEFAULT_MINIMUM),
                getNumericValueFromAttributes(ta, R.styleable.IFBIRangeSeekBar_MaxValue, DEFAULT_MAXIMUM));
            ta.recycle();
        }

        mTextSize = PixelUtil.dpToPx(context, DEFAULT_TEXT_SIZE_IN_DP);
        mRect = new RectF();


        setFocusable(true);
        setFocusableInTouchMode(true);
        mScaledTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();

    }

    private void setNumberRange(T minValue, T maxValue) {
        this.absoluteMinValue = minValue;
        this.absoluteMaxValue = maxValue;
        setValuePrimAndNumberType();
    }

    @SuppressWarnings("unchecked")
    private T getNumericValueFromAttributes(TypedArray a, int attribute, int defaultValue) {
        TypedValue tv = a.peekValue(attribute);
        if (tv == null) {
            return (T) Integer.valueOf(defaultValue);
        }

        int type = tv.type;
        if (type == TypedValue.TYPE_FLOAT) {
            return (T) Float.valueOf(a.getFloat(attribute, defaultValue));
        } else {
            return (T) Integer.valueOf(a.getInteger(attribute, defaultValue));
        }
    }

    @SuppressWarnings("unchecked")
    private void setRangeToDefaultValues() {
        this.absoluteMinValue = (T) DEFAULT_MINIMUM;
        this.absoluteMaxValue = (T) DEFAULT_MAXIMUM;
        setValuePrimAndNumberType();
    }

    private void setValuePrimAndNumberType() {
        minValuePrim = absoluteMinValue.doubleValue();
        maxValuePrim = absoluteMaxValue.doubleValue();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = 200;
        if (MeasureSpec.UNSPECIFIED != MeasureSpec.getMode(widthMeasureSpec)) {
            width = MeasureSpec.getSize(widthMeasureSpec);
        }

        int height = 300;
        if (MeasureSpec.UNSPECIFIED != MeasureSpec.getMode(heightMeasureSpec)) {
            height = Math.max(height, MeasureSpec.getSize(heightMeasureSpec));
        }
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        drawSeekBar(canvas);
        //绘制按钮
        float minSeekValue = getSeekPosition(normalizedMinValue);
        float maxSeekValue = getSeekPosition(normalizedMaxValue);
        drawThumb(minSeekValue, canvas);
        drawThumb(maxSeekValue, canvas);
        //绘制文字
        paint.setTextSize(mTextSize);
        paint.setColor(Color.parseColor("#1a1a1a"));
        setThumbValue(absoluteMinValue.toString(), minSeekValue, canvas);
        setThumbValue(absoluteMaxValue.toString(), maxSeekValue, canvas);

    }

    private void drawSeekBar(Canvas canvas) {
        paint.setStyle(Style.FILL);
        boolean selectedValuesAreDefault = (getSelectedMinValue().equals(getAbsoluteMinValue()) &&
            getSelectedMaxValue().equals(getAbsoluteMaxValue()));

        int colorToUseForButtonsAndHighlightedLine = selectedValuesAreDefault ?
            Color.GRAY : Color.BLUE;

        paint.setColor(colorToUseForButtonsAndHighlightedLine);
        mRect.set(PADDING+ thumbHalfWidth, getHeight() / 2 - PixelUtil.dpToPx(getContext(), SEEKBAR_OUTER_LAYER / 2),
            getWidth() - PADDING - thumbHalfWidth, getHeight() / 2 + PixelUtil.dpToPx(getContext(), SEEKBAR_OUTER_LAYER / 2));
        canvas.drawRect(mRect, paint);
    }

    private void setThumbValue(String text, float seekValue, Canvas canvas) {
        float minTextWidth = paint.measureText(text);
        canvas.drawText(text, seekValue - minTextWidth / 2,
            getHeight() / 2 - thumbHalfHeight - PixelUtil.dpToPx(getContext(), TEXT_OFFSET), paint);
    }

    /**
     * 绘制滑块
     * @param seekValue 滑动百分比
     */
    private void drawThumb(float seekValue, Canvas canvas) {
        canvas.drawBitmap(thumbImage, seekValue - thumbHalfWidth, getHeight() / 2 - thumbHalfHeight, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int pointerIndex;
        int action = event.getAction();
        Log.i("mtag", "onTouchEvent:   "+action);
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mActivePointerId = event.getPointerId(event.getPointerCount() - 1);
                pointerIndex = event.findPointerIndex(mActivePointerId);
                mDownMotionX = event.getX(pointerIndex);
                pressedThumb = evalPressedThumb(mDownMotionX);

                if (pressedThumb == null) {
                    return super.onTouchEvent(event);
                }
                setPressed(true);
                onStartTrackingTouch();
                trackTouchEvent(event);
                attemptClaimDrag();
                break;
            case MotionEvent.ACTION_MOVE:
                if (pressedThumb != null) {

                    if (mIsDragging) {
                        trackTouchEvent(event);
                    } else {
                        // Scroll to follow the motion event
                        pointerIndex = event.findPointerIndex(mActivePointerId);
                        final float x = event.getX(pointerIndex);

                        if (Math.abs(x - mDownMotionX) > mScaledTouchSlop) {
                            setPressed(true);
                            invalidate();
                            onStartTrackingTouch();
                            trackTouchEvent(event);
                            attemptClaimDrag();
                        }
                    }

                    if (notifyWhileDragging && listener != null) {
                        listener.onRangeSeekBarValuesChanged(this, getSelectedMinValue(), getSelectedMaxValue());
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mIsDragging) {
                    trackTouchEvent(event);
                    onStopTrackingTouch();
                    setPressed(false);
                } else {
                    onStartTrackingTouch();
                    trackTouchEvent(event);
                    onStopTrackingTouch();
                }

                pressedThumb = null;
                invalidate();
                if (listener != null) {
                    listener.onRangeSeekBarValuesChanged(this, getSelectedMinValue(), getSelectedMaxValue());
                }
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                final int index = event.getPointerCount() - 1;
                mDownMotionX = event.getX(index);
                mActivePointerId = event.getPointerId(index);
                invalidate();
                break;
            case MotionEvent.ACTION_POINTER_UP:
                onSecondaryPointerUp(event);
                invalidate();
                break;
            case MotionEvent.ACTION_CANCEL:
                if (mIsDragging) {
                onStopTrackingTouch();
                setPressed(false);
                }
                invalidate();
                break;
        }
        return true;
    }

    /**
     * @param touchX 按压的位置
     * @return 拖动的滑块
     */
    private Thumb evalPressedThumb(float touchX) {
        Thumb result = null;
        boolean minThumbPressed = isInThumbRange(touchX, normalizedMinValue);
        boolean maxThumbPressed = isInThumbRange(touchX, normalizedMaxValue);
        if (minThumbPressed && maxThumbPressed) {
            result = (touchX / getWidth() > 0.5f) ? Thumb.MIN : Thumb.MAX;
        } else if (minThumbPressed) {
            result = Thumb.MIN;
        } else if (maxThumbPressed) {
            result = Thumb.MAX;
        }
        return result;
    }

    private boolean isInThumbRange(float touchX, double normalizedThumbValue) {
        return Math.abs(touchX - getSeekPosition(normalizedThumbValue)) <= thumbHalfWidth;
    }

    void onStartTrackingTouch() {
        mIsDragging = true;
    }

    void onStopTrackingTouch() {
        mIsDragging = false;
    }

    private final void trackTouchEvent(MotionEvent event) {
        final int pointerIndex = event.findPointerIndex(mActivePointerId);
        final float x = event.getX(pointerIndex);

        if (Thumb.MIN.equals(pressedThumb)) {
            setNormalizedMinValue(screenToNormalized(x));
        } else if (Thumb.MAX.equals(pressedThumb)) {
            setNormalizedMaxValue(screenToNormalized(x));
        }
    }

    private void setNormalizedMinValue(double value) {
        normalizedMinValue = Math.max(0d, Math.min(1d, Math.min(value, normalizedMaxValue)));
        invalidate();
    }

    private void setNormalizedMaxValue(double value) {
        normalizedMaxValue = Math.max(0d, Math.min(1d, Math.max(value, normalizedMinValue)));
        invalidate();
    }

    private double screenToNormalized(float xPosition) {
        int width = getWidth();
        if (width <= 2 * PADDING) {
            return 0d;
        } else {
            double result = (xPosition - PADDING - thumbHalfWidth) / (width - 2 * PADDING - thumbWidth);
            return Math.min(1d, Math.max(0d, result));
        }
    }

    private void attemptClaimDrag() {
        if (getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(true);
        }
    }

    private float getSeekPosition(double seekPercent) {
        return (float) (seekPercent * (getWidth() - 2 * PADDING - thumbWidth) + PADDING + thumbHalfWidth);
    }

    private T normalizedToValue(double normalized) {
        double v = minValuePrim + normalized * (maxValuePrim - minValuePrim);
        //return (T) numberType.toNumber(Math.round(v * 100) / 100d);
        return (T) Double.valueOf(v);
    }

    private final void onSecondaryPointerUp(MotionEvent ev) {
        final int pointerIndex = (ev.getAction() & ACTION_POINTER_INDEX_MASK) >> ACTION_POINTER_INDEX_SHIFT;

        final int pointerId = ev.getPointerId(pointerIndex);
        if (pointerId == mActivePointerId) {
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mDownMotionX = ev.getX(newPointerIndex);
            mActivePointerId = ev.getPointerId(newPointerIndex);
        }
    }

    public void setOnRangeSeekBarChangeListener(OnRangeSeekBarChangeListener<T> listener) {
        this.listener = listener;
    }

    public boolean isNotifyWhileDragging() {
        return notifyWhileDragging;
    }

    public void setNotifyWhileDragging(boolean notifyWhileDragging) {
        this.notifyWhileDragging = notifyWhileDragging;
    }

    public T getSelectedMaxValue() {
        return normalizedToValue(normalizedMaxValue);
    }

    private T getSelectedMinValue() {
        return normalizedToValue(normalizedMinValue);
    }


    public T getAbsoluteMinValue() {
        return absoluteMinValue;
    }

    public T getAbsoluteMaxValue() {
        return absoluteMaxValue;
    }

    public interface OnRangeSeekBarChangeListener<T> {

        void onRangeSeekBarValuesChanged(RangeSeekBar<?> bar, T minValue, T maxValue);
    }

    private static enum Thumb {
        MIN, MAX
    }
}
