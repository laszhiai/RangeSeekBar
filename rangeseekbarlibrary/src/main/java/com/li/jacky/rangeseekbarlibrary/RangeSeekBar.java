package com.li.jacky.rangeseekbarlibrary;

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
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import java.math.BigDecimal;

/**
 * Created by Jacky on 2017/6/20.
 * 区间滑块
 */

public class RangeSeekBar<T extends Number> extends View{

    private static final Integer DEFAULT_MINIMUM = 0;
    private static final Integer DEFAULT_MAXIMUM = 100;
    private T absoluteMinValue, absoluteMaxValue;
    private double minValuePrim, maxValuePrim;//最值
    private RectF mRect;
    private Paint mPaint;
    private Bitmap thumbImage;
    private Bitmap thumbImagePress;
    private OnRangeSeekBarChangeListener<T> listener;
    private int mActivePointerId;
    private float mDownMotionX;
    private Thumb pressedThumb = null;
    private NumberType mNumberType;
    private boolean mIsDragging;
    private boolean mIsSingleMode;

    private int outRadius;
    private int innerRadius;
    private int distance;
    private int DEFAULT_TEXT_SIZE_IN_DP = 14;
    private int SEEK_BAR_OUTER_LAYER = 24;
    private int SEEK_BAR_INNER_LAYER = 8;
    private int mPadding = 0;
    private int mTextSize;
    private int TEXT_OFFSET = 10;
    private int mScaledTouchSlop;
    public int ACTION_POINTER_INDEX_MASK = 0x0000ff00, ACTION_POINTER_INDEX_SHIFT = 8;
    private int outerSeekBarColor = Color.parseColor("#F2F4F5");
    private int innerSeekBarColor = Color.parseColor("#cccccc");
    private int innerActiveSeekBarColor = Color.parseColor("#007aff");
    private float thumbWidth;
    private float thumbHalfWidth;
    private float thumbHalfHeight;
    private double normalizedMinValue = 0d;
    private double normalizedMaxValue = 1d;

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
            mIsSingleMode = ta.getBoolean(R.styleable.IFBIRangeSeekBar_singleThumb, false);
            ta.recycle();
        }

        initRes(context);

    }

    private void initRes(Context context) {
        mRect = new RectF();
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        thumbImage = BitmapFactory.decodeResource(getResources(), R.drawable.seek_thumb_normal);
        thumbImagePress = BitmapFactory.decodeResource(getResources(), R.drawable.seek_thumb_press);
        thumbWidth = thumbImage.getWidth();
        thumbHalfWidth = 0.5f * thumbWidth;
        thumbHalfHeight = 0.5f * thumbImage.getHeight();
        mTextSize = PixelUtil.dpToPx(context, DEFAULT_TEXT_SIZE_IN_DP);
        mPadding = (getPaddingLeft() + getPaddingRight()) / 2;
        outRadius = PixelUtil.dpToPx(getContext(), SEEK_BAR_OUTER_LAYER / 2);
        innerRadius = PixelUtil.dpToPx(getContext(), SEEK_BAR_INNER_LAYER / 2);
        distance = PixelUtil.dpToPx(getContext(), (SEEK_BAR_OUTER_LAYER - SEEK_BAR_INNER_LAYER) / 2);
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
        mNumberType = NumberType.fromNumber(absoluteMaxValue);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(measure(widthMeasureSpec, true), measure(heightMeasureSpec, false));
    }

    private int measure(int measureSpec, boolean isWidth) {
        int result;
        int mode = MeasureSpec.getMode(measureSpec);
        int size = MeasureSpec.getSize(measureSpec);
        int padding = isWidth ? getPaddingLeft() + getPaddingRight() : getPaddingTop() + getPaddingBottom();
        if (mode == MeasureSpec.EXACTLY) {
            result = size;
        } else {
            result = isWidth ? getSuggestedMinimumWidth() : getSuggestedMinimumHeight();
            result += padding;
            if (mode == MeasureSpec.AT_MOST) {
                if (isWidth) {
                    result = Math.max(result, size);
                } else {
                    result = Math.min(result, size);
                }
            }
        }
        return result;
    }

    @Override
    protected int getSuggestedMinimumWidth() {
        return thumbImage.getHeight() * 2;
    }

    @Override
    protected int getSuggestedMinimumHeight() {
        return thumbImage.getHeight() + PixelUtil.dpToPx(getContext(), TEXT_OFFSET);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float minSeekValue = getSeekPosition(normalizedMinValue);
        float maxSeekValue = getSeekPosition(normalizedMaxValue);

        drawSeekBar(canvas, minSeekValue, maxSeekValue);
        //绘制按钮
        if (!mIsSingleMode) {
            drawThumb(minSeekValue, canvas, Thumb.MIN);
        }
        drawThumb(maxSeekValue, canvas, Thumb.MAX);
        //绘制文字
        mPaint.setTextSize(mTextSize);
        mPaint.setColor(Color.parseColor("#1a1a1a"));
        if (!mIsSingleMode) {
            setThumbValue(normalizedToValue(normalizedMinValue) + "", minSeekValue, canvas);
        }
        setThumbValue(normalizedToValue(normalizedMaxValue) + "", maxSeekValue, canvas);

    }

    private void drawSeekBar(Canvas canvas, float minSeekValue, float maxSeekValue) {
        mPaint.setStyle(Style.FILL);

        mPaint.setColor(outerSeekBarColor);
        float outLeft = mPadding + thumbHalfWidth;
        int outTop = getHeight() / 2 - outRadius;
        float outRight = getWidth() - mPadding - thumbHalfWidth;
        int outBottom = getHeight() / 2 + outRadius;
        mRect.set(outLeft - distance, outTop, outRight + distance, outBottom);
        canvas.drawRoundRect(mRect, outRadius, outRadius, mPaint);

        mPaint.setColor(innerSeekBarColor);
        mRect.set(outLeft, outTop + distance, outRight, outBottom - distance);
        canvas.drawRoundRect(mRect, innerRadius, innerRadius, mPaint);

        mPaint.setColor(innerActiveSeekBarColor);
        mRect.set(minSeekValue, outTop + distance, maxSeekValue, outBottom - distance);
        canvas.drawRoundRect(mRect, innerRadius, innerRadius, mPaint);
    }

    private void setThumbValue(String text, float seekValue, Canvas canvas) {
        float textWidth = mPaint.measureText(text);
        canvas.drawText(text, seekValue - textWidth / 2,
            getHeight() / 2 - thumbHalfHeight - PixelUtil.dpToPx(getContext(), TEXT_OFFSET), mPaint);
    }

    /**
     * 绘制滑块
     *
     * @param seekValue 滑动百分比
     */
    private void drawThumb(float seekValue, Canvas canvas, Thumb thumb) {
        if (pressedThumb == thumb) {
            canvas.drawBitmap(thumbImagePress, seekValue - thumbHalfWidth, getHeight() / 2 - thumbHalfHeight, mPaint);
        } else {
            canvas.drawBitmap(thumbImage, seekValue - thumbHalfWidth, getHeight() / 2 - thumbHalfHeight, mPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int pointerIndex;
        int action = event.getAction();
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

                    if (listener != null) {
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
                int index = event.getPointerCount() - 1;
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

    /**
     * @param xPosition x坐标
     * @return seekbar滑动百分比
     */
    private double screenToNormalized(float xPosition) {
        int width = getWidth();
        if (width <= 2 * mPadding) {
            return 0d;
        } else {
            double result = (xPosition - mPadding - thumbHalfWidth) / (width - 2 * mPadding - thumbWidth);
            return Math.min(1d, Math.max(0d, result));
        }
    }

    private void attemptClaimDrag() {
        if (getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(true);
        }
    }

    /**
     * @param seekPercent seekbar移动的百分比
     * @return 横坐标
     */
    private float getSeekPosition(double seekPercent) {
        return (float) (seekPercent * (getWidth() - 2 * mPadding - thumbWidth) + mPadding + thumbHalfWidth);
    }

    /**
     * @param normalized seekbar移动的百分比
     * @return 显示值
     */
    @SuppressWarnings("unchecked")
    private T normalizedToValue(double normalized) {
        double v = minValuePrim + normalized * (maxValuePrim - minValuePrim);
        return (T) mNumberType.toNumber(Math.round(v * 100) / 100d);
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

    private enum Thumb {
        MIN, MAX
    }

    private enum NumberType{
        INTEGER, FLOAT, DOUBLE, BIG_DECIMAL;

        public static <E extends Number> NumberType fromNumber(E value) throws IllegalArgumentException {
            if (value instanceof Double) {
                return DOUBLE;
            }
            if (value instanceof Float) {
                return FLOAT;
            }
            if (value instanceof Integer) {
                return INTEGER;
            }
            if (value instanceof BigDecimal) {
                return BIG_DECIMAL;
            }
            throw new IllegalArgumentException("Number class '" + value.getClass().getName() + "' is not supported");
        }

        public Number toNumber(double value) {
            switch (this) {
                case DOUBLE:
                    return value;
                case FLOAT:
                    return (float) value;
                case INTEGER:
                    return (int) value;
                case BIG_DECIMAL:
                    return BigDecimal.valueOf(value);
            }
            throw new InstantiationError("can't convert " + this + " to a Number object");
        }
    }
}
