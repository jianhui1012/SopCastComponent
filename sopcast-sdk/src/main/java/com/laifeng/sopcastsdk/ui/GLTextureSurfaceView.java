package com.laifeng.sopcastsdk.ui;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.util.AttributeSet;
import android.view.TextureView;

import com.chillingvan.canvasgl.ICanvasGL;
import com.chillingvan.canvasgl.glview.texture.GLTextureView;
import com.chillingvan.canvasgl.glview.texture.gles.GLThread;
import com.laifeng.sopcastsdk.camera.CameraHolder;
import com.laifeng.sopcastsdk.constant.SopCastConstant;
import com.laifeng.sopcastsdk.utils.SopCastLog;
import com.laifeng.sopcastsdk.video.GLMyViewRenderer;
import com.laifeng.sopcastsdk.video.MyRenderer;
import com.laifeng.sopcastsdk.video.effect.Effect;
import com.laifeng.sopcastsdk.video.effect.NullEffect;

/**
 * Created by admin on 2017/7/6.
 */
@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class GLTextureSurfaceView extends GLTextureView {

    private GLMyViewRenderer mRenderer;

    public GLTextureSurfaceView(Context context) {
        super(context);
        initData();
    }

    public GLTextureSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initData();
    }


    public GLMyViewRenderer getRenderer() {
        return mRenderer;
    }

    public void setZOrderMediaOverlay(boolean isFlag){
        //not deal with
    }

    private void initData() {
        mRenderer = new GLMyViewRenderer(this);
        setRenderer(mRenderer);
        setSurfaceTextureListener(new SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                SopCastLog.d(SopCastConstant.TAG, "SurfaceView width:" + width + " height:" + height);
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                SopCastLog.d(SopCastConstant.TAG, "SurfaceView width:" + width + " height:" + height);
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                SopCastLog.d(SopCastConstant.TAG, "SurfaceView destroy");
                CameraHolder.instance().stopPreview();
                CameraHolder.instance().releaseCamera();
                //mRenderer = new GLMyViewRenderer(GLTextureSurfaceView.this);
                //mRenderer.setEffect(new NullEffect(GLTextureSurfaceView.this.getContext()));
                //setRenderer(mRenderer);
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
                SopCastLog.d(SopCastConstant.TAG, "SurfaceView");
            }
        });
    }

    public void setEffect(final Effect effect) {
        this.queueEvent(new Runnable() {
            @Override
            public void run() {
                if (null != mRenderer) {
                    mRenderer.setEffect(effect);
                }
            }
        });
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
    }


    @Override
    protected void onGLDraw(ICanvasGL canvas) {

    }

    @Override
    protected void surfaceDestroyed() {
        super.surfaceDestroyed();
    }



}
