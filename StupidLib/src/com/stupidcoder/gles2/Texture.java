package com.stupidcoder.gles2;

import java.io.IOException;
import java.io.InputStream;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;

public class Texture {
	private int mTexture;
	private Bitmap mBitmap;
	private long mLastBuffered;

	public Texture(Bitmap pBitmap) {
		mLastBuffered = 0;
		mBitmap = pBitmap;
	}

	public Texture(Resources pResources, int pResourceID) {
		mLastBuffered = 0;
		try
		{
			InputStream stream = pResources.openRawResource(pResourceID);
			mBitmap = BitmapFactory.decodeStream(stream);
			stream.close();
		}
		catch (java.io.IOException e)
		{
			Log.e("StupidLib", "An IO exception occurred while loading a texture!");
		}
	}

	private void bufferIfNeeded(long pSurfaceCreationTime) {
		if (mLastBuffered >= pSurfaceCreationTime)
			return;

		int[] textures = new int[1];
		GLES20.glGenTextures(1, textures, 0);
		mTexture = textures[0];

		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexture);

		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR);
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);

		GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, mBitmap, 0);
		GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);

		mLastBuffered = Tools.currentTime();
	}

	public int use(long pSurfaceCreationTime) {
		bufferIfNeeded(pSurfaceCreationTime);
		return mTexture;
	}
}
