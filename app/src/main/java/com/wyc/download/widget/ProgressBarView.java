package com.wyc.download.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.ProgressBar;

import com.wyc.download.R;

public class ProgressBarView extends ProgressBar {

    //字体大小
    protected int mTextSize = 12;
    //字体颜色
    protected int mTextColor = Color.BLACK;
    //没有到达（右边progress的颜色）
    protected int mUnReachColor = Color.GREEN;
    //progressbar的高度
    protected int mProgressHeight = 5;
    //progressbar进度的颜色
    protected int mReachColor = mTextColor;
    //字体间距
    protected int mTextOffset = 10;

    protected Paint mPaint;
    //progress真实的宽度
    protected int mRealWidth;

    public ProgressBarView(Context context) {
        this(context, null);
    }

    public ProgressBarView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ProgressBarView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.ProgressBarView);
        mTextSize = array.getDimensionPixelSize(R.styleable.ProgressBarView_progress_text_size, mTextSize);
        mTextColor = array.getColor(R.styleable.ProgressBarView_progress_text_color, mTextColor);
        mUnReachColor = array.getColor(R.styleable.ProgressBarView_progress_unreach_color, mUnReachColor);
        mReachColor = array.getColor(R.styleable.ProgressBarView_progress_reach_color, mReachColor);
        mProgressHeight = (int) array.getDimension(R.styleable.ProgressBarView_progress_height, mProgressHeight);
        mTextOffset = (int) array.getDimension(R.styleable.ProgressBarView_progress_text_offset, mTextOffset);
        array.recycle();

        mPaint = new Paint();
        mPaint.setTextSize(mTextSize);
        mPaint.setAntiAlias(true);
    }

    @Override
    protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = measureHeight(heightMeasureSpec);
        setMeasuredDimension(width, height);
        mRealWidth = getMeasuredWidth() - getPaddingLeft() - getPaddingRight();
    }

    private int measureHeight(int heightMeasureSpec) {
        int result;
        //获取高度模式
        int mode = MeasureSpec.getMode(heightMeasureSpec);
        int size = MeasureSpec.getSize(heightMeasureSpec);
        if (mode == MeasureSpec.EXACTLY) {
            //精准模式 用户设置为 比如80dp  match_parent fill_parent
            result = size;
        } else {
            //计算中间文字的高度
            int textHeight = (int) (mPaint.descent() - mPaint.ascent());
            result = getPaddingTop() + getPaddingBottom() + Math.max(mProgressHeight, Math.abs(textHeight));
            if (mode == MeasureSpec.AT_MOST) {
                result = Math.min(result, size);
            }
        }
        return result;
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        //保存画布
        canvas.save();
        //移动画布
        canvas.translate(getPaddingLeft(), getHeight() / 2);
        //定义变量用来控制是否要绘制右边progressbar  如果宽度不够的时候就不进行绘制
        boolean isNotNeedUnReach = false;
        //计算左边进度的真个控件跨度的占比
        float radio = getProgress() * 1.0f / getMax();
        //获取左边进度的宽度
        float progressX = radio * mRealWidth;
        //中间文字
        String text = getProgress() + "%";
        //获取文字的宽度
        int textWidth = (int) mPaint.measureText(text);
        if (progressX + textWidth > mRealWidth) {
            //左边进度+文字的宽度超过progressbar的宽度 重新计算左边进度的宽度 这个时候也就意味着不需要绘制右边进度
            progressX = mRealWidth - textWidth;
            isNotNeedUnReach = true;
        }
        //计算左边进度条结束的位置 如果结束的位置小于0就不需要绘制左边的进度
        float endX = progressX - mTextOffset / 2;
        if (endX > 0) {
            //绘制左边的进度
            mPaint.setColor(mReachColor);
            mPaint.setStrokeWidth(mProgressHeight);
            canvas.drawLine(0, 0, endX, 0, mPaint);
        }
        mPaint.setColor(mTextColor);
        if (getProgress() != 0) {
            //计算文字基线
            int y = (int) (-(mPaint.descent() + mPaint.ascent()) / 2);
            //绘制文字
            canvas.drawText(text, progressX, y, mPaint);
        }
        if (!isNotNeedUnReach) {
            //右边进度的开始位置=左边进度+文字间距的一半+文字宽度
            float start;
            if (getProgress() == 0) {
                start = progressX;
            } else {
                start = progressX + mTextOffset / 2 + textWidth;
            }
            mPaint.setColor(mUnReachColor);
            mPaint.setStrokeWidth(mProgressHeight);
            //绘制右边进度
            canvas.drawLine(start, 0, mRealWidth, 0, mPaint);
        }
        //重置画布
        canvas.restore();
    }


    public void setTextSize(int mTextSize) {
        this.mTextSize = mTextSize;
        invalidate();
    }

    public void setTextColor(int mTextColor) {
        this.mTextColor = mTextColor;
        invalidate();
    }


    public void setUnReachColor(int mUnReachColor) {
        this.mUnReachColor = mUnReachColor;
        invalidate();
    }


    public void setProgressHeight(int mProgressHeight) {
        this.mProgressHeight = mProgressHeight;
        invalidate();
    }


    public void setReachColor(int mReachColor) {
        this.mReachColor = mReachColor;
        invalidate();
    }


    public void setTextOffset(int mTextOffset) {
        this.mTextOffset = mTextOffset;
        invalidate();
    }

    protected int dp2px(int dip) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, getResources().getDisplayMetrics());
    }

    protected int sp2px(int sp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, getResources().getDisplayMetrics());
    }
}
