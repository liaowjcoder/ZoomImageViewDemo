package com.zeal.zoomimageviewdemo.view;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

/**
 * Created by zeal on 16/12/3.
 * <p>
 * 1.图片什么完全显示的监听：OnGlobalLayoutListener
 * 2.图片的大小和展示的控件大小比较
 * a.若是小于控件的大小，那么就拉伸到控件大小
 * b.若是比控件大，那么就缩小
 * 3.计算scale值
 * 4.将图片移动到控件的中心
 * 5.Matrix实现scale和translate操作，使用相应的api就可以了，尽量不要用调用setValue
 * 6.多点触控
 * 7.以当前触摸点为中心坐标进行缩放操作
 */

public class ZoomImageView extends ImageView implements View.OnTouchListener, ViewTreeObserver.OnGlobalLayoutListener, ScaleGestureDetector.OnScaleGestureListener {

    /**
     * 当布局layout完毕之后，使用mOnce作为判断是否需要
     * 去处理图片的大小和实际控件的大小的比较的关系
     */
    private boolean mOnce;

    /**
     * 初始放大的比例
     */
    private float mInitScale;

    /**
     * 双击放大所到达的值
     */
    private float mMidScale;

    /**
     * 最大的缩放比例值
     */
    private float mMaxScale;


    private Matrix mMatrix;


    private ScaleGestureDetector mScaleGetureDetector;

    public ZoomImageView(Context context) {
        this(context, null);
    }

    public ZoomImageView(Context context, AttributeSet attrs) {
        super(context, attrs);


        mMatrix = new Matrix();

        //代码设置，尽量不要在布局文件中调用
        setScaleType(ScaleType.MATRIX);

        //实例化多点触控对象
        mScaleGetureDetector = new ScaleGestureDetector(getContext(), this);

        //设置触摸监听
        setOnTouchListener(this);
    }


    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        getViewTreeObserver().addOnGlobalLayoutListener(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        //api16新方法，我们这里使用旧的api兼容
        //getViewTreeObserver().removeOnGlobalLayoutListener(this);
        getViewTreeObserver().removeGlobalOnLayoutListener(this);
    }

    @Override
    public void onGlobalLayout() {
        //当view完成布局之后的回调
        if (!mOnce) {//表示第一次处理
            //取得承载图片的控件的大小
            int width = getWidth();
            int height = getHeight();


            //取得图片的大小
            Drawable drawable = getDrawable();
            if (drawable != null) {
                int dWidth = drawable.getIntrinsicWidth();
                int dHeight = drawable.getIntrinsicHeight();

                //定义一个缩放值
                float scale = 1.0f;

                if (dWidth > width && dHeight < height) {
                    //图片的宽度大于控件的宽度，但是图片的高度却小于控件的高度
                    //dWidth = 200 dHeight = 50
                    //width = 100 height = 100
                    //100*1.0f/200=0.5f
                    scale = width * 1.0f / dWidth;
                }

                if (dWidth < width && dHeight > height) {
                    scale = height * 1.0f / dHeight;
                }

                if (dWidth > width && dHeight > height) {
//                    图片的大小大于控件的大小

                    //dWidth = 200 dHeight = 300
                    //width = 100 height = 100
                    //0.5            0.3333   取两者最小值作为缩放比例

                    scale = Math.min(width * 1.0f / dWidth, height * 1.0f / dHeight);
                }

                if (width > dWidth && height > dHeight) {
                    //控件大小均比图片大小都大，需要对图片进行放大显示
                    //dWidth = 100 dHeight = 100
                    //width = 200 height = 300

//                    200/100 = 2
//                            300/100=3
//                                    放大2倍  200*200 这种方式才是对的
//                            放大3倍  300*300

                    scale = Math.min(width * 1.0f / dWidth, height * 1.0f / dHeight);

                }

                //初始化3个scale的值
                mInitScale = scale;
                mMidScale = scale * 2;
                mMaxScale = mInitScale * 4;


                //将图片移动到控件的中间
                int dx = getWidth() / 2 - dWidth / 2;
                int dy = getHeight() / 2 - dHeight / 2;

                //Matrix类3*3的矩阵
                //xScale xSkew sTrans
                //ySkew  yScale yTrans
                // 0  0 0

                //调用api去进行移动和缩放操作
                mMatrix.postTranslate(dx, dy);
                mMatrix.postScale(mInitScale, mInitScale, getWidth() / 2, getHeight() / 2);
                setImageMatrix(mMatrix);

                mOnce = true;//表示第一次已经处理完毕
            }

        }
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        //缩放代码

//1.xx 0.xx
        float scaleFactor = detector.getScaleFactor();

        //当前的缩放比例
//        mInitScale mMaxScale

        float scale = getScale();
        Log.e("zeal", "onScale: scale=" + scale);


        if (getDrawable() == null) {
            return true;
        }


        //控制缩放范围
        float scaleTem = scale;
        if ((scale >= mInitScale && scaleFactor < 1.0f)||(scale <= mMaxScale && scaleFactor > 1.0f)) {
            scaleTem = scaleFactor;
            if (scale * scaleFactor < mInitScale) {
                scaleTem = mInitScale / scale;
            }
            if (scale * scaleFactor > mMaxScale) {
                scaleTem = mMaxScale / scale;
            }

            //这里在缩放时永远是以控件中心
            //mMatrix.postScale(scaleTem, scaleTem, getWidth() / 2, getHeight() / 2);
            //以触摸点为当前缩放的中心坐标
            mMatrix.postScale(scaleTem,scaleTem,detector.getFocusX(),detector.getFocusY());

            //缩放的同时检测屏幕的边界
            checkBodrderAndWhenScale();


            setImageMatrix(mMatrix);
        }


        return true;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {

    }


    /**
     * 获取当前图片的缩放值
     *
     * @return
     */
    public float getScale() {
        float[] values = new float[9];
        mMatrix.getValues(values);
        return values[Matrix.MSCALE_X];//y和x的scale都是一样的
    }


    @Override
    public boolean onTouch(View v, MotionEvent event) {

        mScaleGetureDetector.onTouchEvent(event);

        return true;
    }


    /**
     * 缩放的时检测边界
     */
    private void checkBodrderAndWhenScale() {
        RectF rectF = getMatrixRectF();


        //需要位移的值
        float dx = 0;
        float dy = 0;


        int width = getWidth();
        int height = getHeight();


        if(rectF.width()>=width)  {
            //缩放后的宽度比控件宽度还要大

            if(rectF.left>0) {
                //左侧有空隙
                dx = -rectF.left;//需要左移
            }
            if(rectF.right<width) {
                //右侧有空隙
                dx = width -rectF.right;
            }
        }

        if(rectF.height()>height) {
            if(rectF.top>0) {
                //说明顶部有空隙
                dy =  -rectF.top;
            }

            if(rectF.bottom<height) {
                //说明底部有空隙
                dy = height-rectF.bottom;
            }
        }

        if(rectF.width()<width) {
            dx = width/2f-rectF.right+rectF.width()/2f;
        }

        if(rectF.height()<=height) {
            dy = height/2-rectF.bottom+rectF.height()/2f;
        }

        mMatrix.postTranslate(dx,dy);
    }

    /**
     * 获得缩放后的RectF值
     * @return
     */
    private RectF getMatrixRectF() {
        Matrix matrix = mMatrix;

        Drawable d = getDrawable();

        RectF rectF = new RectF();
        if(d!=null) {
            rectF.set(0,0,d.getIntrinsicWidth(),d.getIntrinsicHeight());
            matrix.mapRect(rectF);
        }

        return rectF;
    }
}
