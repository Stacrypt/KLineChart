package com.github.fujianlian.klinechart;

import android.animation.ValueAnimator;
import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;

import androidx.core.view.GestureDetectorCompat;

import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import com.github.fujianlian.klinechart.base.IAdapter;
import com.github.fujianlian.klinechart.base.IChartDraw;
import com.github.fujianlian.klinechart.base.IDateTimeFormatter;
import com.github.fujianlian.klinechart.base.IValueFormatter;
import com.github.fujianlian.klinechart.draw.MainDraw;
import com.github.fujianlian.klinechart.draw.Status;
import com.github.fujianlian.klinechart.entity.IKLine;
import com.github.fujianlian.klinechart.formatter.BigValueFormatter;
import com.github.fujianlian.klinechart.formatter.TimeFormatter;
import com.github.fujianlian.klinechart.formatter.ValueFormatter;
import com.github.fujianlian.klinechart.utils.ViewUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * k线图
 * Created by tian on 2016/5/3.
 */
public abstract class BaseKLineChartView extends ScrollAndScaleView {

    private int mChildDrawPosition = -1;

    private float mTranslateX = Float.MIN_VALUE;

    private int mWidth = 0;

    private int mTopPadding;

    private int mChildPadding;

    private int mBottomPadding;

    private int textPadding;

    private float startPadding = 100F;

    private float yAxisMaxLabelLength = 0F;

    private float mMainScaleY = 1;

    private float mVolScaleY = 1;

    private float mChildScaleY = 1;

    private float mDataLen = 0;

    private float mMainMaxValue = Float.MAX_VALUE;

    private float mMainMinValue = Float.MIN_VALUE;

    private float mMainHighMaxValue = 0;

    private float mMainLowMinValue = 0;

    private int mMainMaxIndex = 0;

    private int mMainMinIndex = 0;

    private Float mVolMaxValue = Float.MAX_VALUE;

    private Float mVolMinValue = Float.MIN_VALUE;

    private Float mChildMaxValue = Float.MAX_VALUE;

    private Float mChildMinValue = Float.MIN_VALUE;

    private int mStartIndex = 0;

    private int mStopIndex = 0;

    private float mPointWidth = 6;

    private int mGridRows = 4;

    private int mGridColumns = 4;

    private Paint mGridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private Paint mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private Paint mMaxMinPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private Paint mBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private Paint mSelectedXLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private Paint mSelectedYLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private Paint mSelectPointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private Paint mSelectorFramePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private int mSelectedIndex;

    private IChartDraw mMainDraw;
    private MainDraw mainDraw;
    private IChartDraw mVolDraw;

    private IAdapter mAdapter;

    private Boolean isWR = false;
    private Boolean isShowChild = false;

    private DataSetObserver mDataSetObserver = new DataSetObserver() {
        @Override
        public void onChanged() {
            mItemCount = getAdapter().getCount();
            notifyChanged();
        }

        @Override
        public void onInvalidated() {
            mItemCount = getAdapter().getCount();
            notifyChanged();
        }
    };
    //当前点的个数
    private int mItemCount;
    private IChartDraw mChildDraw;
    private List<IChartDraw> mChildDraws = new ArrayList<>();

    private IValueFormatter mValueFormatter;
    private IDateTimeFormatter mDateTimeFormatter;

    private ValueAnimator mAnimator;

    private long mAnimationDuration = 500;

    private float mOverScrollRange = 0;

    private OnSelectedChangedListener mOnSelectedChangedListener = null;

    private Rect mMainRect;

    private Rect mVolRect;

    private Rect mChildRect;

    private float mLineWidth;

    private Bitmap watermark;

    private Typeface typeface;

    private int positiveColor, negativeColor;

    public BaseKLineChartView(Context context) {
        super(context);
        init();
    }

    public BaseKLineChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BaseKLineChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setWillNotDraw(false);
        mDetector = new GestureDetectorCompat(getContext(), this);
        mScaleDetector = new ScaleGestureDetector(getContext(), this);
        mTopPadding = (int) getResources().getDimension(R.dimen.chart_top_padding);
        mChildPadding = (int) getResources().getDimension(R.dimen.child_top_padding);
        mBottomPadding = (int) getResources().getDimension(R.dimen.chart_bottom_padding);
        textPadding = (int) getResources().getDimension(R.dimen.text_padding);

        mAnimator = ValueAnimator.ofFloat(0f, 1f);
        mAnimator.setDuration(mAnimationDuration);
        mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                invalidate();
            }
        });

        mSelectorFramePaint.setStrokeWidth(ViewUtil.Dp2Px(getContext(), 0.6f));
        mSelectorFramePaint.setStyle(Paint.Style.STROKE);
        mSelectorFramePaint.setColor(Color.WHITE);
    }


    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        this.mWidth = w;
        displayHeight = h - mTopPadding - mBottomPadding;
        initRect();
        setTranslateXFromScrollX(mScrollX);
    }

    int displayHeight = 0;

    private void initRect() {
        if (isShowChild) {
            int mMainHeight = (int) (displayHeight * 0.6f);
            int mVolHeight = (int) (displayHeight * 0.2f);
            int mChildHeight = (int) (displayHeight * 0.2f);
            mMainRect = new Rect(0, mTopPadding, mWidth, mTopPadding + mMainHeight);
            mVolRect = new Rect(0, mMainRect.bottom + mChildPadding, mWidth, mMainRect.bottom + mVolHeight);
            mChildRect = new Rect(0, mVolRect.bottom + mChildPadding, mWidth, mVolRect.bottom + mChildHeight);
        } else {
            int mMainHeight = (int) (displayHeight * 0.75f);
            int mVolHeight = (int) (displayHeight * 0.25f);
            mMainRect = new Rect(0, mTopPadding, mWidth, mTopPadding + mMainHeight);
            mVolRect = new Rect(0, mMainRect.bottom + mChildPadding, mWidth, mMainRect.bottom + mVolHeight);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(mBackgroundPaint.getColor());
        if (mWidth == 0 || mMainRect.height() == 0 || mItemCount == 0) {
            return;
        }
        drawWatermark(canvas);
        calculateValue();
        canvas.save();
        canvas.scale(1, 1);
        drawGird(canvas);
        drawK(canvas);
        drawMaxAndMin(canvas);
        drawLastValue(canvas);
        drawYAxisBackground(canvas);
        drawText(canvas);
        drawValue(canvas, isLongPress ? mSelectedIndex : mStopIndex);
        canvas.restore();
    }

    private void drawYAxisBackground(Canvas canvas) {
        float maxText = Math.max(Math.max(calculateWidth(mVolDraw.getValueFormatter().format(mVolMaxValue)), yAxisMaxLabelLength), calculateWidth(formatValue(mChildMaxValue)));
        float maxTextWidth = mWidth - maxText - textPadding;

        // Draw a background for Y-axis labels
        canvas.drawRect(maxTextWidth, 0, mWidth, mMainRect.bottom, mBackgroundPaint);

        // Draw a background for Vol labels
        if (mVolDraw != null)
            canvas.drawRect(maxTextWidth, mMainRect.bottom, mWidth, mVolRect.bottom, mBackgroundPaint);

        // Draw a background for Child view labels
        if (mChildDraw != null)
            canvas.drawRect(maxTextWidth, mVolRect.bottom, mWidth, mChildRect.bottom, mBackgroundPaint);
    }

    private void drawWatermark(Canvas canvas) {
        if (watermark == null) return;
        int xPosition = mMainRect.left + mBottomPadding;
        int yPosition = mMainRect.bottom - mBottomPadding - watermark.getHeight();
        canvas.drawBitmap(watermark, xPosition, yPosition, null);
    }

    public float getMainY(float value) {
        return (mMainMaxValue - value) * mMainScaleY + mMainRect.top;
    }

    public float getMainBottom() {
        return mMainRect.bottom;
    }

    public float getVolY(float value) {
        return (mVolMaxValue - value) * mVolScaleY + mVolRect.top;
    }

    public float getChildY(float value) {
        return (mChildMaxValue - value) * mChildScaleY + mChildRect.top;
    }

    /**
     * 解决text居中的问题
     */
    public float fixTextY(float y) {
        Paint.FontMetrics fontMetrics = mTextPaint.getFontMetrics();
        return y + fontMetrics.descent - fontMetrics.ascent;
    }

    /**
     * 解决text居中的问题
     */
    public float fixTextY1(float y) {
        Paint.FontMetrics fontMetrics = mTextPaint.getFontMetrics();
        return (y + (fontMetrics.descent - fontMetrics.ascent) / 2 - fontMetrics.descent);
    }

    /**
     * 画表格
     *
     * @param canvas
     */
    private void drawGird(Canvas canvas) {
        //-----------------------上方k线图------------------------
        //Horizontal grid
        float rowSpace = mMainRect.height() / mGridRows;
        for (int i = 0; i <= mGridRows; i++) {
            canvas.drawLine(0, rowSpace * i + mMainRect.top, mWidth, rowSpace * i + mMainRect.top, mGridPaint);
        }
        //-----------------------Subgraph below------------------------
        if (mChildDraw != null) {
            canvas.drawLine(0, mVolRect.bottom, mWidth, mVolRect.bottom, mGridPaint);
            canvas.drawLine(0, mChildRect.bottom, mWidth, mChildRect.bottom, mGridPaint);
        } else {
            canvas.drawLine(0, mVolRect.bottom, mWidth, mVolRect.bottom, mGridPaint);
        }

        // Y value divider
        float maxText = Math.max(Math.max(calculateWidth(mVolDraw.getValueFormatter().format(mVolMaxValue)), yAxisMaxLabelLength), calculateWidth(formatValue(mChildMaxValue)));
        float dividerXPosition = mWidth - maxText - textPadding;
        canvas.drawLine(dividerXPosition, 0, dividerXPosition, mMainRect.bottom, mGridPaint);
        if (mVolDraw != null)
            canvas.drawLine(dividerXPosition, mVolRect.top, dividerXPosition, mVolRect.bottom, mGridPaint);
        if (mChildDraw != null)
            canvas.drawLine(dividerXPosition, mChildRect.top, dividerXPosition, mChildRect.bottom, mGridPaint);

        //Vertical grid
        float columnSpace = dividerXPosition / mGridColumns;
        for (int i = 1; i < mGridColumns; i++) {
            canvas.drawLine(columnSpace * i, 0, columnSpace * i, mMainRect.bottom, mGridPaint);
            canvas.drawLine(columnSpace * i, mMainRect.bottom, columnSpace * i, mVolRect.bottom, mGridPaint);
            if (mChildDraw != null) {
                canvas.drawLine(columnSpace * i, mVolRect.bottom, columnSpace * i, mChildRect.bottom, mGridPaint);
            }
        }
    }

    /**
     * 画k线图
     *
     * @param canvas
     */
    private void drawK(Canvas canvas) {
        //保存之前的平移，缩放
        canvas.save();
        canvas.translate(mTranslateX * mScaleX, 0);
        canvas.scale(mScaleX, 1);
        for (int i = mStartIndex; i <= mStopIndex; i++) {
            Object currentPoint = getItem(i);
            float currentPointX = getX(i);
            Object lastPoint = i == 0 ? currentPoint : getItem(i - 1);
            float lastX = i == 0 ? currentPointX : getX(i - 1);
            if (mMainDraw != null) {
                mMainDraw.drawTranslated(lastPoint, currentPoint, lastX, currentPointX, canvas, this, i);
            }
            if (mVolDraw != null) {
                mVolDraw.drawTranslated(lastPoint, currentPoint, lastX, currentPointX, canvas, this, i);
            }
            if (mChildDraw != null) {
                mChildDraw.drawTranslated(lastPoint, currentPoint, lastX, currentPointX, canvas, this, i);
            }
        }
        //Draw selection line
        if (isLongPress) {
            IKLine point = (IKLine) getItem(mSelectedIndex);
            float x = getX(mSelectedIndex);
            float y = getMainY(point.getClosePrice());
            // k-line graph vertical line
            canvas.drawLine(x, mMainRect.top, x, mMainRect.bottom, mSelectedYLinePaint);
            // k-line graph horizontal line
            canvas.drawLine(-mTranslateX, y, -mTranslateX + mWidth / mScaleX, y, mSelectedXLinePaint);
            // Vertical bar in histogram
            canvas.drawLine(x, mMainRect.bottom, x, mVolRect.bottom, mSelectedYLinePaint);
            if (mChildDraw != null) {
                // Sub-line graph vertical line
                canvas.drawLine(x, mVolRect.bottom, x, mChildRect.bottom, mSelectedYLinePaint);
            }
        }
        //Restore pan and zoom
        canvas.restore();
    }

    /**
     * 计算文本长度
     *
     * @return
     */
    private int calculateWidth(String text) {
        Rect rect = new Rect();
        mTextPaint.getTextBounds(text, 0, text.length(), rect);
        return rect.width() + 5;
    }

    /**
     * 计算文本长度
     *
     * @return
     */
    private Rect calculateMaxMin(String text) {
        Rect rect = new Rect();
        mMaxMinPaint.getTextBounds(text, 0, text.length(), rect);
        return rect;
    }

    /**
     * 画文字
     *
     * @param canvas
     */
    private void drawText(Canvas canvas) {
        Paint.FontMetrics fm = mTextPaint.getFontMetrics();
        float textHeight = fm.descent - fm.ascent;
        float baseLine = (textHeight - fm.bottom - fm.top) / 2;
        //--------------Draw the value of the bar graph above-------------
        if (mMainDraw != null) {
            canvas.drawText(formatValue(mMainMaxValue), mWidth - calculateWidth(formatValue(mMainMaxValue)), baseLine + mMainRect.top, mTextPaint);
            canvas.drawText(formatValue(mMainMinValue), mWidth - calculateWidth(formatValue(mMainMinValue)), mMainRect.bottom - textHeight + baseLine, mTextPaint);
            float rowValue = (mMainMaxValue - mMainMinValue) / mGridRows;
            float rowSpace = mMainRect.height() / mGridRows;
            for (int i = 1; i < mGridRows; i++) {
                String text = formatValue(rowValue * (mGridRows - i) + mMainMinValue);
                canvas.drawText(text, mWidth - calculateWidth(text), fixTextY(rowSpace * i + mMainRect.top), mTextPaint);
            }
            // Set start padding based on Y axis value length
            startPadding = (float) calculateWidth(formatValue(mMainMaxValue)) + 50;
        }
        //--------------Draw the value of the middle subgraph-------------
        if (mVolDraw != null) {
            canvas.drawText(mVolDraw.getValueFormatter().format(mVolMaxValue),
                    mWidth - calculateWidth(mVolDraw.getValueFormatter().format(mVolMaxValue)), mMainRect.bottom + baseLine, mTextPaint);
        }
        //--------------Draw the value of the subgraph below-------------
        if (mChildDraw != null) {
            canvas.drawText(mChildDraw.getValueFormatter().format(mChildMaxValue),
                    mWidth - calculateWidth(formatValue(mChildMaxValue)), mVolRect.bottom + baseLine, mTextPaint);
        }
        //--------------Draw time---------------------
        float columnSpace = mWidth / mGridColumns;
        float y;
        if (isShowChild) {
            y = mChildRect.bottom + baseLine + 5;
        } else {
            y = mVolRect.bottom + baseLine + 5;
        }

        float startX = getX(mStartIndex) - mPointWidth / 2;
        float stopX = getX(mStopIndex) + mPointWidth / 2;

        for (int i = 1; i < mGridColumns; i++) {
            float translateX = xToTranslateX(columnSpace * i);
            if (translateX >= startX && translateX <= stopX) {
                int index = indexOfTranslateX(translateX);
                String text = mAdapter.getDate(index);
                canvas.drawText(text, columnSpace * i - mTextPaint.measureText(text) / 2, y, mTextPaint);
            }
        }

        float translateX = xToTranslateX(0);
        if (translateX >= startX && translateX <= stopX) {
            canvas.drawText(getAdapter().getDate(mStartIndex), 0, y, mTextPaint);
        }
        translateX = xToTranslateX(mWidth);
        if (translateX >= startX && translateX <= stopX) {
            String text = getAdapter().getDate(mStopIndex);
            canvas.drawText(text, mWidth - mTextPaint.measureText(text), y, mTextPaint);
        }
        if (isLongPress) {
            // Draw Y value
            IKLine point = (IKLine) getItem(mSelectedIndex);
            float w1 = ViewUtil.Dp2Px(getContext(), 5);
            float w2 = ViewUtil.Dp2Px(getContext(), 3);
            float r = textHeight / 2 + w2;
            y = getMainY(point.getClosePrice());
            float x;
            String text = formatValue(point.getClosePrice());
            float textWidth = mTextPaint.measureText(text);
            if (translateXtoX(getX(mSelectedIndex)) < getChartWidth() / 2) {
                x = 1;
                Path path = new Path();
                path.moveTo(x, y - r);
                path.lineTo(x, y + r);
                path.lineTo(textWidth + 2 * w1, y + r);
                path.lineTo(textWidth + 2 * w1 + w2, y);
                path.lineTo(textWidth + 2 * w1, y - r);
                path.close();
                canvas.drawPath(path, mSelectPointPaint);
                canvas.drawPath(path, mSelectorFramePaint);
                canvas.drawText(text, x + w1, fixTextY1(y), mTextPaint);
            } else {
                x = mWidth - textWidth - 1 - 2 * w1 - w2;
                Path path = new Path();
                path.moveTo(x, y);
                path.lineTo(x + w2, y + r);
                path.lineTo(mWidth - 2, y + r);
                path.lineTo(mWidth - 2, y - r);
                path.lineTo(x + w2, y - r);
                path.close();
                canvas.drawPath(path, mSelectPointPaint);
                canvas.drawPath(path, mSelectorFramePaint);
                canvas.drawText(text, x + w1 + w2, fixTextY1(y), mTextPaint);
            }

            // 画X值
            String date = mAdapter.getDate(mSelectedIndex);
            textWidth = mTextPaint.measureText(date);
            r = textHeight / 2;
            x = translateXtoX(getX(mSelectedIndex));
            if (isShowChild) {
                y = mChildRect.bottom;
            } else {
                y = mVolRect.bottom;
            }

            if (x < textWidth + 2 * w1) {
                x = 1 + textWidth / 2 + w1;
            } else if (mWidth - x < textWidth + 2 * w1) {
                x = mWidth - 1 - textWidth / 2 - w1;
            }

            canvas.drawRect(x - textWidth / 2 - w1, y, x + textWidth / 2 + w1, y + baseLine + r, mSelectPointPaint);
            canvas.drawRect(x - textWidth / 2 - w1, y, x + textWidth / 2 + w1, y + baseLine + r, mSelectorFramePaint);
            canvas.drawText(date, x - textWidth / 2, y + baseLine + 5, mTextPaint);
        }
    }

    /**
     * 画文字
     *
     * @param canvas
     */
    private void drawMaxAndMin(Canvas canvas) {
        if (!mainDraw.isLine()) {
            IKLine maxEntry = null, minEntry = null;
            boolean firstInit = true;
            BigValueFormatter formatter = new BigValueFormatter();

            //Plot the maximum and minimum values
            float x = translateXtoX(getX(mMainMinIndex));
            float y = getMainY(mMainLowMinValue);
            String LowString = "── " + formatter.format(mMainLowMinValue);
            //Calculate display position
            //Calculate text width
            int lowStringWidth = calculateMaxMin(LowString).width();
            int lowStringHeight = calculateMaxMin(LowString).height();
            if (x < getWidth() / 2) {
                //Draw right
                canvas.drawText(LowString, x, y + lowStringHeight / 2, mMaxMinPaint);
            } else {
                //Draw left
                LowString = formatter.format(mMainLowMinValue) + " ──";
                canvas.drawText(LowString, x - lowStringWidth, y + lowStringHeight / 2, mMaxMinPaint);
            }

            x = translateXtoX(getX(mMainMaxIndex));
            y = getMainY(mMainHighMaxValue);

            String highString = "── " + formatter.format(mMainHighMaxValue);
            int highStringWidth = calculateMaxMin(highString).width();
            int highStringHeight = calculateMaxMin(highString).height();
            if (x < getWidth() / 2) {
                //Draw right
                canvas.drawText(highString, x, y + highStringHeight / 2, mMaxMinPaint);
            } else {
                //Draw left
                highString = formatter.format(mMainHighMaxValue) + " ──";
                canvas.drawText(highString, x - highStringWidth, y + highStringHeight / 2, mMaxMinPaint);
            }

        }
    }

    private void drawLastValue(Canvas canvas) {
        int lastIndex = mAdapter.getCount() - 1;
        // Don't draw if the last index is visible
        if (mStopIndex >= lastIndex) return;

        // Draw horizontal line
        canvas.save();
        canvas.translate(mTranslateX * mScaleX, 0);
        canvas.scale(mScaleX, 1);
        IKLine point = (IKLine) getItem(lastIndex);
        float y = (getMainY(point.getClosePrice()) < mMainRect.centerY()) ?
                Math.max(getMainY(point.getClosePrice()), getMainY(mMainHighMaxValue)) :
                Math.min(getMainY(point.getClosePrice()), getMainY(mMainMinValue));
        // k-line graph horizontal line
        canvas.drawLine(-mTranslateX, y, -mTranslateX + mWidth / mScaleX, y, mSelectedXLinePaint);
        canvas.restore();

        // Draw last value
        Paint.FontMetrics fm = mTextPaint.getFontMetrics();
        float textHeight = fm.descent - fm.ascent;
        float w1 = ViewUtil.Dp2Px(getContext(), 5);
        float w2 = ViewUtil.Dp2Px(getContext(), 3);
        float r = textHeight / 2 + w2;
        float x;
        String text = formatValue(point.getClosePrice());
        float textWidth = mTextPaint.measureText(text);
        x = mMainRect.centerX() - textWidth;
        Path path = new Path();
        path.moveTo(x, y - r);
        path.lineTo(x - w2, y);
        path.lineTo(x, y + r);
        path.lineTo(x + textWidth + 2 * w1, y + r);
        path.lineTo(x + textWidth + 2 * w1 + w2, y);
        path.lineTo(x + textWidth + 2 * w1, y - r);
        path.close();
        canvas.drawPath(path, mSelectPointPaint);
        canvas.drawPath(path, mSelectorFramePaint);
        canvas.drawText(text, x + w1, fixTextY1(y), mTextPaint);

    }

    /**
     * 画值
     *
     * @param canvas
     * @param position Display the value of a certain point
     */
    private void drawValue(Canvas canvas, int position) {
        Paint.FontMetrics fm = mTextPaint.getFontMetrics();
        float textHeight = fm.descent - fm.ascent;
        float baseLine = (textHeight - fm.bottom - fm.top) / 2;
        if (position >= 0 && position < mItemCount) {
            if (mMainDraw != null) {
                float y = mMainRect.top + baseLine - textHeight;
                mMainDraw.drawText(canvas, this, position, 0, y);
            }
            if (mVolDraw != null) {
                float y = mMainRect.bottom + baseLine;
                mVolDraw.drawText(canvas, this, position, 0, y);
            }
            if (mChildDraw != null) {
                float y = mVolRect.bottom + baseLine;
                mChildDraw.drawText(canvas, this, position, 0, y);
            }
        }
    }

    public int dp2px(float dp) {
        final float scale = getContext().getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

    public int sp2px(float spValue) {
        final float fontScale = getContext().getResources().getDisplayMetrics().scaledDensity;
        return (int) (spValue * fontScale + 0.5f);
    }

    /**
     * 格式化值
     */
    public String formatValue(float value) {
        if (getValueFormatter() == null) {
            setValueFormatter(new ValueFormatter());
        }
        return getValueFormatter().format(value);
    }

    /**
     * Recalculate and refresh the line
     */
    public void notifyChanged() {
        if (isShowChild && mChildDrawPosition == -1) {
            mChildDraw = mChildDraws.get(0);
            mChildDrawPosition = 0;
        }
        if (mItemCount != 0) {
            mDataLen = (mItemCount - 1) * mPointWidth;
            checkAndFixScrollX();
            setTranslateXFromScrollX(mScrollX);
        } else {
            setScrollX(0);
        }
        invalidate();
    }

    /**
     * MA/BOLL切换及隐藏
     *
     * @param status MA/BOLL/NONE
     */
    public void changeMainDrawType(Status status) {
        if (mainDraw != null && mainDraw.getStatus() != status) {
            mainDraw.setStatus(status);
            invalidate();
        }
    }

    private void calculateSelectedX(float x) {
        mSelectedIndex = indexOfTranslateX(xToTranslateX(x));
        if (mSelectedIndex < mStartIndex) {
            mSelectedIndex = mStartIndex;
        }
        if (mSelectedIndex > mStopIndex) {
            mSelectedIndex = mStopIndex;
        }
    }

    @Override
    public void onLongPress(MotionEvent e) {
        super.onLongPress(e);
        int lastIndex = mSelectedIndex;
        calculateSelectedX(e.getX());
        if (lastIndex != mSelectedIndex) {
            onSelectedChanged(this, getItem(mSelectedIndex), mSelectedIndex);
        }
        invalidate();
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        setTranslateXFromScrollX(mScrollX);
    }

    @Override
    protected void onScaleChanged(float scale, float oldScale) {
        checkAndFixScrollX();
        setTranslateXFromScrollX(mScrollX);
        super.onScaleChanged(scale, oldScale);
    }

    /**
     * 计算当前的显示区域
     */
    private void calculateValue() {
        if (!isLongPress()) {
            mSelectedIndex = -1;
        }
        mMainMaxValue = Float.MIN_VALUE;
        mMainMinValue = Float.MAX_VALUE;
        mVolMaxValue = Float.MIN_VALUE;
        mVolMinValue = Float.MAX_VALUE;
        mChildMaxValue = Float.MIN_VALUE;
        mChildMinValue = Float.MAX_VALUE;
        mStartIndex = indexOfTranslateX(xToTranslateX(0));
        mStopIndex = indexOfTranslateX(xToTranslateX(mWidth));
        mMainMaxIndex = mStartIndex;
        mMainMinIndex = mStartIndex;
        mMainHighMaxValue = Float.MIN_VALUE;
        mMainLowMinValue = Float.MAX_VALUE;
        for (int i = mStartIndex; i <= mStopIndex; i++) {
            IKLine point = (IKLine) getItem(i);
            if (mMainDraw != null) {
                mMainMaxValue = Math.max(mMainMaxValue, mMainDraw.getMaxValue(point));
                mMainMinValue = Math.min(mMainMinValue, mMainDraw.getMinValue(point));
                if (mMainHighMaxValue != Math.max(mMainHighMaxValue, point.getHighPrice())) {
                    mMainHighMaxValue = point.getHighPrice();
                    mMainMaxIndex = i;
                }
                if (mMainLowMinValue != Math.min(mMainLowMinValue, point.getLowPrice())) {
                    mMainLowMinValue = point.getLowPrice();
                    mMainMinIndex = i;
                }
            }
            if (mVolDraw != null) {
                mVolMaxValue = Math.max(mVolMaxValue, mVolDraw.getMaxValue(point));
                mVolMinValue = Math.min(mVolMinValue, mVolDraw.getMinValue(point));
            }
            if (mChildDraw != null) {
                mChildMaxValue = Math.max(mChildMaxValue, mChildDraw.getMaxValue(point));
                mChildMinValue = Math.min(mChildMinValue, mChildDraw.getMinValue(point));
            }
        }
        if (mMainMaxValue != mMainMinValue) {
            float padding = (mMainMaxValue - mMainMinValue) * 0.05f;
            mMainMaxValue += padding;
            mMainMinValue -= padding;
        } else {
            //When the maximum and minimum values are equal, increase the maximum value and decrease the minimum value respectively
            mMainMaxValue += Math.abs(mMainMaxValue * 0.05f);
            mMainMinValue -= Math.abs(mMainMinValue * 0.05f);
            if (mMainMaxValue == 0) {
                mMainMaxValue = 1;
            }
        }

        if (Math.abs(mVolMaxValue) < 0.01) {
            mVolMaxValue = 15.00f;
        }

        if (Math.abs(mChildMaxValue) < 0.01 && Math.abs(mChildMinValue) < 0.01) {
            mChildMaxValue = 1f;
        } else if (mChildMaxValue.equals(mChildMinValue)) {
            //When the maximum and minimum values are equal, increase the maximum value and decrease the minimum value respectively
            mChildMaxValue += Math.abs(mChildMaxValue * 0.05f);
            mChildMinValue -= Math.abs(mChildMinValue * 0.05f);
            if (mChildMaxValue == 0) {
                mChildMaxValue = 1f;
            }
        }

        if (isWR) {
            mChildMaxValue = 0f;
            if (Math.abs(mChildMinValue) < 0.01)
                mChildMinValue = -10.00f;
        }
        mMainScaleY = mMainRect.height() * 1f / (mMainMaxValue - mMainMinValue);
        mVolScaleY = mVolRect.height() * 1f / (mVolMaxValue - mVolMinValue);
        if (mChildRect != null)
            mChildScaleY = mChildRect.height() * 1f / (mChildMaxValue - mChildMinValue);
        if (mAnimator.isRunning()) {
            float value = (float) mAnimator.getAnimatedValue();
            mStopIndex = mStartIndex + Math.round(value * (mStopIndex - mStartIndex));
        }

        for (int i = 0; i < mAdapter.getCount(); i++) {
            IKLine point = (IKLine) getItem(i);
            float maxValue = Math.max(yAxisMaxLabelLength, mMainDraw.getMaxValue(point));
            yAxisMaxLabelLength = calculateWidth(formatValue(maxValue));
        }

    }

    /**
     * Get the minimum value of translation
     *
     * @return
     */
    private float getMinTranslateX() {
        return -mDataLen + mWidth / mScaleX - mPointWidth / 2 - startPadding;
    }

    /**
     * Get the maximum value of translation
     *
     * @return
     */
    private float getMaxTranslateX() {
        if (!isFullScreen()) {
            return getMinTranslateX();
        }
        return mPointWidth / 2 + startPadding;
    }

    @Override
    public int getMinScrollX() {
        return (int) -(mOverScrollRange / mScaleX);
    }

    public int getMaxScrollX() {
        return Math.round(getMaxTranslateX() - getMinTranslateX());
    }

    public int indexOfTranslateX(float translateX) {
        return indexOfTranslateX(translateX, 0, mItemCount - 1);
    }

    /**
     * 在主区域画线
     *
     * @param startX    开始点的横坐标
     * @param stopX     开始点的值
     * @param stopX     结束点的横坐标
     * @param stopValue 结束点的值
     */
    public void drawMainLine(Canvas canvas, Paint paint, float startX, float startValue, float stopX, float stopValue) {
        canvas.drawLine(startX, getMainY(startValue), stopX, getMainY(stopValue), paint);
    }


    /**
     * 在主区域画分时线
     *
     * @param startX    开始点的横坐标
     * @param stopX     开始点的值
     * @param stopX     结束点的横坐标
     * @param stopValue 结束点的值
     */
    public void drawMainMinuteLine(Canvas canvas, Paint paint, float startX, float startValue, float stopX, float stopValue) {
        Path path5 = new Path();
        path5.moveTo(startX, displayHeight + mTopPadding + mBottomPadding);
        path5.lineTo(startX, getMainY(startValue));
        path5.lineTo(stopX, getMainY(stopValue));
        path5.lineTo(stopX, displayHeight + mTopPadding + mBottomPadding);
        path5.close();
        canvas.drawPath(path5, paint);
    }

    /**
     * 在子区域画线
     *
     * @param startX     开始点的横坐标
     * @param startValue 开始点的值
     * @param stopX      结束点的横坐标
     * @param stopValue  结束点的值
     */
    public void drawChildLine(Canvas canvas, Paint paint, float startX, float startValue, float stopX, float stopValue) {
        canvas.drawLine(startX, getChildY(startValue), stopX, getChildY(stopValue), paint);
    }

    /**
     * 在子区域画线
     *
     * @param startX     开始点的横坐标
     * @param startValue 开始点的值
     * @param stopX      结束点的横坐标
     * @param stopValue  结束点的值
     */
    public void drawVolLine(Canvas canvas, Paint paint, float startX, float startValue, float stopX, float stopValue) {
        canvas.drawLine(startX, getVolY(startValue), stopX, getVolY(stopValue), paint);
    }

    /**
     * Get entities based on index
     *
     * @param position 索引值
     * @return
     */
    public Object getItem(int position) {
        if (mAdapter != null) {
            return mAdapter.getItem(position);
        } else {
            return null;
        }
    }

    /**
     * 根据索引索取x坐标
     *
     * @param position 索引值
     * @return
     */
    public float getX(int position) {
        return position * mPointWidth - startPadding;
    }

    /**
     * 获取适配器
     *
     * @return
     */
    public IAdapter getAdapter() {
        return mAdapter;
    }

    /**
     * 设置当前子图
     *
     * @param position
     */
    public void setChildDraw(int position) {
        if (mChildDrawPosition != position) {
            if (!isShowChild) {
                isShowChild = true;
                initRect();
            }
            mChildDraw = mChildDraws.get(position);
            mChildDrawPosition = position;
            isWR = position == 5;
            invalidate();
        }
    }

    /**
     * 隐藏子图
     */
    public void hideChildDraw() {
        mChildDrawPosition = -1;
        isShowChild = false;
        mChildDraw = null;
        initRect();
        invalidate();
    }

    /**
     * 给子区域添加画图方法
     *
     * @param childDraw IChartDraw
     */
    public void addChildDraw(IChartDraw childDraw) {
        mChildDraws.add(childDraw);
    }

    /**
     * scrollX Convert to TranslateX
     *
     * @param scrollX
     */
    private void setTranslateXFromScrollX(int scrollX) {
        mTranslateX = scrollX + getMinTranslateX();
    }

    /**
     * 获取ValueFormatter
     *
     * @return
     */
    public IValueFormatter getValueFormatter() {
        return mValueFormatter;
    }

    /**
     * 设置ValueFormatter
     *
     * @param valueFormatter value格式化器
     */
    public void setValueFormatter(IValueFormatter valueFormatter) {
        this.mValueFormatter = valueFormatter;
    }

    /**
     * 获取DatetimeFormatter
     *
     * @return 时间格式化器
     */
    public IDateTimeFormatter getDateTimeFormatter() {
        return mDateTimeFormatter;
    }

    /**
     * 设置dateTimeFormatter
     *
     * @param dateTimeFormatter 时间格式化器
     */
    public void setDateTimeFormatter(IDateTimeFormatter dateTimeFormatter) {
        mDateTimeFormatter = dateTimeFormatter;
    }

    /**
     * 格式化时间
     *
     * @param date
     */
    public String formatDateTime(Date date) {
        if (getDateTimeFormatter() == null) {
            setDateTimeFormatter(new TimeFormatter());
        }
        return getDateTimeFormatter().format(date);
    }

    /**
     * 获取主区域的 IChartDraw
     *
     * @return IChartDraw
     */
    public IChartDraw getMainDraw() {
        return mMainDraw;
    }

    /**
     * 设置主区域的 IChartDraw
     *
     * @param mainDraw IChartDraw
     */
    public void setMainDraw(IChartDraw mainDraw) {
        mMainDraw = mainDraw;
        this.mainDraw = (MainDraw) mMainDraw;
    }

    public IChartDraw getVolDraw() {
        return mVolDraw;
    }

    public void setVolDraw(IChartDraw mVolDraw) {
        this.mVolDraw = mVolDraw;
    }

    /**
     * 二分查找当前值的index
     *
     * @return
     */
    public int indexOfTranslateX(float translateX, int start, int end) {
        if (end == start) {
            return start;
        }
        if (end - start == 1) {
            float startValue = getX(start);
            float endValue = getX(end);
            return Math.abs(translateX - startValue) < Math.abs(translateX - endValue) ? start : end;
        }
        int mid = start + (end - start) / 2;
        float midValue = getX(mid);
        if (translateX < midValue) {
            return indexOfTranslateX(translateX, start, mid);
        } else if (translateX > midValue) {
            return indexOfTranslateX(translateX, mid, end);
        } else {
            return mid;
        }
    }

    /**
     * 设置数据适配器
     */
    public void setAdapter(IAdapter adapter) {
        if (mAdapter != null && mDataSetObserver != null) {
            mAdapter.unregisterDataSetObserver(mDataSetObserver);
        }
        mAdapter = adapter;
        if (mAdapter != null) {
            mAdapter.registerDataSetObserver(mDataSetObserver);
            mItemCount = mAdapter.getCount();
        } else {
            mItemCount = 0;
        }
        notifyChanged();
    }

    /**
     * 开始动画
     */
    public void startAnimation() {
        if (mAnimator != null) {
            mAnimator.start();
        }
    }

    /**
     * 设置动画时间
     */
    public void setAnimationDuration(long duration) {
        if (mAnimator != null) {
            mAnimator.setDuration(duration);
        }
    }

    /**
     * 设置表格行数
     */
    public void setGridRows(int gridRows) {
        if (gridRows < 1) {
            gridRows = 1;
        }
        mGridRows = gridRows;
    }

    /**
     * 设置表格列数
     */
    public void setGridColumns(int gridColumns) {
        if (gridColumns < 1) {
            gridColumns = 1;
        }
        mGridColumns = gridColumns;
    }

    /**
     * view中的x转化为TranslateX
     *
     * @param x
     * @return
     */
    public float xToTranslateX(float x) {
        return -mTranslateX + x / mScaleX;
    }

    /**
     * translateX Converted to x in the view
     *
     * @param translateX
     * @return
     */
    public float translateXtoX(float translateX) {
        return (translateX + mTranslateX) * mScaleX;
    }

    /**
     * 获取上方padding
     */
    public float getTopPadding() {
        return mTopPadding;
    }

    /**
     * 获取上方padding
     */
    public float getChildPadding() {
        return mChildPadding;
    }

    /**
     * 获取子试图上方padding
     */
    public float getmChildScaleYPadding() {
        return mChildPadding;
    }

    /**
     * 获取图的宽度
     *
     * @return
     */
    public int getChartWidth() {
        return mWidth;
    }

    /**
     * 是否长按
     */
    public boolean isLongPress() {
        return isLongPress;
    }

    /**
     * 获取选择索引
     */
    public int getSelectedIndex() {
        return mSelectedIndex;
    }

    public Rect getChildRect() {
        return mChildRect;
    }

    public Rect getVolRect() {
        return mVolRect;
    }

    /**
     * 设置选择监听
     */
    public void setOnSelectedChangedListener(OnSelectedChangedListener l) {
        this.mOnSelectedChangedListener = l;
    }

    public void onSelectedChanged(BaseKLineChartView view, Object point, int index) {
        if (this.mOnSelectedChangedListener != null) {
            mOnSelectedChangedListener.onSelectedChanged(view, point, index);
        }
    }

    /**
     * Does the data fill the screen
     *
     * @return
     */
    public boolean isFullScreen() {
        return mDataLen >= mWidth / mScaleX;
    }

    /**
     * 设置超出右方后可滑动的范围
     */
    public void setOverScrollRange(float overScrollRange) {
        if (overScrollRange < 0) {
            overScrollRange = 0;
        }
        mOverScrollRange = overScrollRange;
    }

    /**
     * 设置上方padding
     *
     * @param topPadding
     */
    public void setTopPadding(int topPadding) {
        mTopPadding = topPadding;
    }

    /**
     * 设置下方padding
     *
     * @param bottomPadding
     */
    public void setBottomPadding(int bottomPadding) {
        mBottomPadding = bottomPadding;
    }

    /**
     * 设置表格线宽度
     */
    public void setGridLineWidth(float width) {
        mGridPaint.setStrokeWidth(width);
    }

    /**
     * 设置表格线颜色
     */
    public void setGridLineColor(int color) {
        mGridPaint.setColor(color);
    }

    /**
     * 设置选择器横线宽度
     */
    public void setSelectedXLineWidth(float width) {
        mSelectedXLinePaint.setStrokeWidth(width);
    }

    /**
     * 设置选择器横线颜色
     */
    public void setSelectedXLineColor(int color) {
        mSelectedXLinePaint.setColor(color);
    }

    /**
     * 设置选择器竖线宽度
     */
    public void setSelectedYLineWidth(float width) {
        mSelectedYLinePaint.setStrokeWidth(width);
    }

    /**
     * 设置选择器竖线颜色
     */
    public void setSelectedYLineColor(int color) {
        mSelectedYLinePaint.setColor(color);
    }

    /**
     * 设置文字颜色
     */
    public void setTextColor(int color) {
        mTextPaint.setColor(color);
    }

    /**
     * 设置文字大小
     */
    public void setTextSize(float textSize) {
        mTextPaint.setTextSize(textSize);
    }

    /**
     * 设置最大值/最小值文字颜色
     */
    public void setMTextColor(int color) {
        mMaxMinPaint.setColor(color);
    }

    /**
     * 设置最大值/最小值文字大小
     */
    public void setMTextSize(float textSize) {
        mMaxMinPaint.setTextSize(textSize);
    }

    /**
     * 设置背景颜色
     */
    public void setBackgroundColor(int color) {
        mBackgroundPaint.setColor(color);
    }

    /**
     * 设置选中point 值显示背景
     */
    public void setSelectPointColor(int color) {
        mSelectPointPaint.setColor(color);
    }

    public void setSelectorFrameColor(int color) {
        mSelectorFramePaint.setColor(color);
    }

    public void setWatermark(Bitmap watermark) {
        this.watermark = watermark;
    }

    public Bitmap getWatermark() {
        return watermark;
    }

    public float getStartPadding() {
        return startPadding;
    }

    public Typeface getTypeface() {
        return typeface;
    }

    public void setTypeface(Typeface typeface) {
        this.typeface = typeface;
        if (typeface != null) {
            mMaxMinPaint.setTypeface(typeface);
            mTextPaint.setTypeface(typeface);
            invalidate();
        }
    }

    public int getPositiveColor() {
        return positiveColor;
    }

    public void setPositiveColor(int positiveColor) {
        this.positiveColor = positiveColor;
        if (mainDraw != null)
            mainDraw.setPositiveColor(positiveColor);
        if (mVolDraw != null)
            mVolDraw.setPositiveColor(positiveColor);
        if (mChildDraws != null)
            for (int i = 0; i < mChildDraws.size(); i++)
                mChildDraws.get(i).setPositiveColor(positiveColor);
    }

    public int getNegativeColor() {
        return negativeColor;
    }

    public void setNegativeColor(int negativeColor) {
        this.negativeColor = negativeColor;
        if (mainDraw != null)
            mainDraw.setNegativeColor(negativeColor);
        if (mVolDraw != null)
            mVolDraw.setNegativeColor(negativeColor);
        if (mChildDraws != null)
            for (int i = 0; i < mChildDraws.size(); i++)
                mChildDraws.get(i).setNegativeColor(negativeColor);
    }

    /**
     * 选中点变化时的监听
     */
    public interface OnSelectedChangedListener {
        /**
         * 当选点中变化时
         *
         * @param view  当前view
         * @param point 选中的点
         * @param index 选中点的索引
         */
        void onSelectedChanged(BaseKLineChartView view, Object point, int index);
    }

    /**
     * 获取文字大小
     */
    public float getTextSize() {
        return mTextPaint.getTextSize();
    }

    /**
     * 获取曲线宽度
     */
    public float getLineWidth() {
        return mLineWidth;
    }

    /**
     * 设置曲线的宽度
     */
    public void setLineWidth(float lineWidth) {
        mLineWidth = lineWidth;
    }

    /**
     * 设置每个点的宽度
     */
    public void setPointWidth(float pointWidth) {
        mPointWidth = pointWidth;
    }

    public Paint getGridPaint() {
        return mGridPaint;
    }

    public Paint getTextPaint() {
        return mTextPaint;
    }

    public Paint getBackgroundPaint() {
        return mBackgroundPaint;
    }

    public int getDisplayHeight() {
        return displayHeight + mTopPadding + mBottomPadding;
    }
}
