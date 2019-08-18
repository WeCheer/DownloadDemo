package com.wyc.download.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;

import com.wyc.download.R;


public class RoundProgressBar extends ProgressBarView {
    //半径
    private int mRadius = 30;
    private int mMaxPaintWidth;

    public RoundProgressBar(Context context) {
        this(context, null);
    }

    public RoundProgressBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RoundProgressBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        //绘制圆形进度条的宽度   这里设置为长方形进度条高度的1.5倍
        mRealWidth = (int) (mProgressHeight * 1.5f);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.RoundProgressBar);
        mRadius = (int) typedArray.getDimension(R.styleable.RoundProgressBar_radius, dp2px(mRadius));
        typedArray.recycle();
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
    }

    @Override
    protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        mMaxPaintWidth = mProgressHeight;
        //计算控件的精准值
        int expect = mRadius * 2 + mMaxPaintWidth + getPaddingLeft() + getPaddingRight();
        int width = resolveSize(expect, widthMeasureSpec);
        int height = resolveSize(expect, heightMeasureSpec);
        int readWidth = Math.min(width, height);
        mRadius = (readWidth - getPaddingRight() - getPaddingLeft() - mMaxPaintWidth) / 2;
        setMeasuredDimension(readWidth, readWidth);
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        //获取当前进度
        String text = getProgress() + "%";
        //测量文字的宽度
        float textWidth = mPaint.measureText(text);
        //文字的高度
        float textHeight = (mPaint.descent() + mPaint.ascent()) / 2;
        //保存画布
        canvas.save();
        //平移画布位置
        canvas.translate(getPaddingLeft() + mMaxPaintWidth / 2, getPaddingTop() + mMaxPaintWidth / 2);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(mUnReachColor);
        mPaint.setStrokeWidth(mRealWidth);
        //绘制圆 要注意绘制圆的x y 因为上面对画布进行了平移所以这里就不需要计算了，如果画布没有进行平移需要计算 x y
        canvas.drawCircle(mRadius, mRadius, mRadius, mPaint);
        //绘制圆弧
        mPaint.setColor(mReachColor);
        mPaint.setStrokeWidth(mReachColor);
        //计算圆弧的扫过的幅度
        float sweepAngle = getProgress() * 1.0f / getMax() * 360;
        RectF rectF = new RectF(0, 0, mRadius * 2, mRadius * 2);
        canvas.drawArc(rectF, 0, sweepAngle, false, mPaint);

        //绘制中间文字
        mPaint.setColor(mTextColor);
        mPaint.setStyle(Paint.Style.FILL);
        canvas.drawText(text, mRadius - textWidth / 2, mRadius - textHeight, mPaint);
        canvas.restore();
    }
}
