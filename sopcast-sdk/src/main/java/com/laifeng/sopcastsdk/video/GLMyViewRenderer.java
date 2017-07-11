package com.laifeng.sopcastsdk.video;

import android.annotation.TargetApi;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLException;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Looper;
import android.view.TextureView;

import com.chillingvan.canvasgl.glview.GLView;
import com.chillingvan.canvasgl.glview.texture.GLViewRenderer;
import com.laifeng.sopcastsdk.camera.CameraHolder;
import com.laifeng.sopcastsdk.camera.CameraListener;
import com.laifeng.sopcastsdk.camera.CameraUtils;
import com.laifeng.sopcastsdk.camera.exception.CameraDisabledException;
import com.laifeng.sopcastsdk.camera.exception.CameraHardwareException;
import com.laifeng.sopcastsdk.camera.exception.CameraNotSupportException;
import com.laifeng.sopcastsdk.camera.exception.NoCameraException;
import com.laifeng.sopcastsdk.configuration.VideoConfiguration;
import com.laifeng.sopcastsdk.entity.Watermark;
import com.laifeng.sopcastsdk.mediacodec.VideoMediaCodec;
import com.laifeng.sopcastsdk.stream.sender.rtmp.packets.Handshake;
import com.laifeng.sopcastsdk.ui.GLTextureSurfaceView;
import com.laifeng.sopcastsdk.utils.WeakHandler;
import com.laifeng.sopcastsdk.video.effect.Effect;
import com.laifeng.sopcastsdk.video.effect.NullEffect;

import java.nio.IntBuffer;

import javax.microedition.khronos.opengles.GL10;

/**
 * Created by admin on 2017/7/6.
 */

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class GLMyViewRenderer implements GLViewRenderer, SurfaceTexture.OnFrameAvailableListener {

    private int mSurfaceTextureId = -1;
    private SurfaceTexture mSurfaceTexture;
    private Watermark mWatermark;
    private RenderScreen mRenderScreen;
    private RenderSrfTex mRenderSrfTex;

    private CameraListener mCameraOpenListener;
    private WeakHandler mHandler = new WeakHandler(Looper.getMainLooper());
    private GLTextureSurfaceView mView;
    private boolean isCameraOpen;
    private Effect mEffect;
    private int mEffectTextureId;
    private VideoConfiguration mVideoConfiguration;

    private boolean updateSurface = false;
    private final float[] mTexMtx = GlUtil.createIdentityMtx();

    private int mVideoWidth;
    private int mVideoHeight;
    private boolean isTakePicture = false;
    private int mSurfaceWidth = 0;
    private int mSurfaceHeight = 0;
    private Bitmap bmp;

    public GLMyViewRenderer(GLTextureSurfaceView view) {
        mView = view;
        mEffect = new NullEffect(mView.getContext());
    }

    public void setCameraOpenListener(CameraListener cameraOpenListener) {
        this.mCameraOpenListener = cameraOpenListener;
    }

    public void setVideoConfiguration(VideoConfiguration videoConfiguration) {
        mVideoConfiguration = videoConfiguration;
        mVideoWidth = VideoMediaCodec.getVideoSize(mVideoConfiguration.width);
        mVideoHeight = VideoMediaCodec.getVideoSize(mVideoConfiguration.height);
        if (mRenderScreen != null) {
            mRenderScreen.setVideoSize(mVideoWidth, mVideoHeight);
        }
    }

    public void setRecorder(MyRecorder recorder) {
        synchronized (this) {
            if (recorder != null) {
                mRenderSrfTex = new RenderSrfTex(mEffectTextureId, recorder);
                mRenderSrfTex.setVideoSize(mVideoWidth, mVideoHeight);
                if (mWatermark != null) {
                    mRenderSrfTex.setWatermark(mWatermark);
                }
            } else {
                mRenderSrfTex = null;
            }
        }
    }


    @Override
    public void onSurfaceCreated() {
        initSurfaceTexture();
    }

    @Override
    public void onSurfaceChanged(int width, int height) {
        startCameraPreview();
        if (isCameraOpen) {
            if (mRenderScreen == null) {
                initScreenTexture();
            }
            mSurfaceWidth = width;
            mSurfaceHeight = height;
            mRenderScreen.setSreenSize(width, height);
            if (mVideoConfiguration != null) {
                mRenderScreen.setVideoSize(mVideoWidth, mVideoHeight);
            }
            if (mWatermark != null) {
                mRenderScreen.setWatermark(mWatermark);
            }
        }
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        synchronized (this) {
            if (updateSurface) {
                mSurfaceTexture.updateTexImage();
                mSurfaceTexture.getTransformMatrix(mTexMtx);
                updateSurface = false;
            }
        }
        mEffect.draw(mTexMtx);
        if (mRenderScreen != null) {
            mRenderScreen.draw();
        }
        if (mRenderSrfTex != null) {
            mRenderSrfTex.draw();
        }
    }





    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        synchronized (this) {
            updateSurface = true;
        }
        mView.requestRender();
    }


    private void initSurfaceTexture() {
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        mSurfaceTextureId = textures[0];
        mSurfaceTexture = new SurfaceTexture(mSurfaceTextureId);
        mSurfaceTexture.setOnFrameAvailableListener(this);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glDisable(GLES20.GL_CULL_FACE);
        GLES20.glDisable(GLES20.GL_BLEND);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mSurfaceTextureId);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
    }

    private void initScreenTexture() {
        mEffect.setTextureId(mSurfaceTextureId);
        mEffect.prepare();
        mEffectTextureId = mEffect.getEffertedTextureId();
        mRenderScreen = new RenderScreen(mEffectTextureId);
    }

    private void startCameraPreview() {
        try {
            CameraUtils.checkCameraService(mView.getContext());
        } catch (CameraDisabledException e) {
            postOpenCameraError(CameraListener.CAMERA_DISABLED);
            e.printStackTrace();
            return;
        } catch (NoCameraException e) {
            postOpenCameraError(CameraListener.NO_CAMERA);
            e.printStackTrace();
            return;
        }
        CameraHolder.State state = CameraHolder.instance().getState();
        CameraHolder.instance().setSurfaceTexture(mSurfaceTexture);
        if (state != CameraHolder.State.PREVIEW) {
            try {
                CameraHolder.instance().openCamera();
                CameraHolder.instance().startPreview();
                if (mCameraOpenListener != null) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mCameraOpenListener.onOpenSuccess();
                        }
                    });
                }
                isCameraOpen = true;
            } catch (CameraHardwareException e) {
                e.printStackTrace();
                postOpenCameraError(CameraListener.CAMERA_OPEN_FAILED);
            } catch (CameraNotSupportException e) {
                e.printStackTrace();
                postOpenCameraError(CameraListener.CAMERA_NOT_SUPPORT);
            }
        }
    }

    private void postOpenCameraError(final int error) {
        if (mCameraOpenListener != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mCameraOpenListener != null) {
                        mCameraOpenListener.onOpenFail(error);
                    }
                }
            });
        }
    }

    public boolean isCameraOpen() {
        return isCameraOpen;
    }

    public void setWatermark(Watermark watermark) {
        mWatermark = watermark;
        if (mRenderScreen != null) {
            mRenderScreen.setWatermark(watermark);
        }
        if (mRenderSrfTex != null) {
            mRenderSrfTex.setWatermark(watermark);
        }
    }

    public void setEffect(Effect effect) {
        mEffect.release();
        mEffect = effect;
        effect.setTextureId(mSurfaceTextureId);
        effect.prepare();
        mEffectTextureId = effect.getEffertedTextureId();
        if (mRenderScreen != null) {
            mRenderScreen.setTextureId(mEffectTextureId);
        }
        if (mRenderSrfTex != null) {
            mRenderSrfTex.setTextureId(mEffectTextureId);
        }
    }
}
