package com.chillingvan.canvasgl.glview.texture;

import javax.microedition.khronos.opengles.GL10;

/**
 * Created by Chilling on 2016/12/29.
 */

public interface GLViewRenderer {

    void onSurfaceCreated();

    void onSurfaceChanged(int width, int height);

    void onDrawFrame(GL10 gl);
}
