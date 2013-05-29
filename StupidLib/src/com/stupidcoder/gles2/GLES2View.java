/**
 * An OpenGL ES 2.0 surface view for Android.
 *
 * @author Dennis Harms
 * @version 1.0
 */

package com.stupidcoder.gles2;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;

import android.content.Context;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;

public class GLES2View extends GLSurfaceView {
	private static class ConfigChooser implements
			GLSurfaceView.EGLConfigChooser {
		private static int EGL_OPENGL_ES2_BIT = 4;
		private static int[] s_configAttribs2 = { EGL10.EGL_RED_SIZE, 4,
				EGL10.EGL_GREEN_SIZE, 4, EGL10.EGL_BLUE_SIZE, 4,
				EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT, EGL10.EGL_NONE };
		protected int mAlphaSize;
		protected int mBlueSize;
		protected int mDepthSize;
		protected int mGreenSize;
		protected int mRedSize;
		protected int mSamples;

		protected int mStencilSize;

		private int[] mValue = new int[1];

		public ConfigChooser(int r, int g, int b, int a, int depth,
				int stencil, int samples) {
			mRedSize = r;
			mGreenSize = g;
			mBlueSize = b;
			mAlphaSize = a;
			mDepthSize = depth;
			mStencilSize = stencil;
			mSamples = samples;
		}

		public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {
			int[] num_config = new int[1];
			egl.eglChooseConfig(display, s_configAttribs2, null, 0, num_config);

			int numConfigs = num_config[0];

			if (numConfigs <= 0)
				throw new IllegalArgumentException(
						"No configs match configSpec");

			EGLConfig[] configs = new EGLConfig[numConfigs];
			egl.eglChooseConfig(display, s_configAttribs2, configs, numConfigs,
					num_config);

			return chooseConfig(egl, display, configs);
		}

		public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display,
				EGLConfig[] configs) {
			for (EGLConfig config : configs) {
				int d = findConfigAttrib(egl, display, config,
						EGL10.EGL_DEPTH_SIZE, 0);
				int s = findConfigAttrib(egl, display, config,
						EGL10.EGL_STENCIL_SIZE, 0);

				if (d < mDepthSize || s < mStencilSize)
					continue;

				int r = findConfigAttrib(egl, display, config,
						EGL10.EGL_RED_SIZE, 0);
				int g = findConfigAttrib(egl, display, config,
						EGL10.EGL_GREEN_SIZE, 0);
				int b = findConfigAttrib(egl, display, config,
						EGL10.EGL_BLUE_SIZE, 0);
				int a = findConfigAttrib(egl, display, config,
						EGL10.EGL_ALPHA_SIZE, 0);
				int samples = findConfigAttrib(egl, display, config,
						EGL10.EGL_SAMPLES, 0);

				if (r == mRedSize && g == mGreenSize && b == mBlueSize
						&& a == mAlphaSize && samples == mSamples)
					return config;
			}
			return null;
		}

		private int findConfigAttrib(EGL10 egl, EGLDisplay display,
				EGLConfig config, int attribute, int defaultValue) {
			if (egl.eglGetConfigAttrib(display, config, attribute, mValue))
				return mValue[0];
			return defaultValue;
		}
	}

	private static class ContextFactory implements
			GLSurfaceView.EGLContextFactory {
		private static int EGL_CONTEXT_CLIENT_VERSION = 0x3098;

		public EGLContext createContext(EGL10 egl, EGLDisplay display,
				EGLConfig eglConfig) {
			int[] attrib_list = { EGL_CONTEXT_CLIENT_VERSION, 2, EGL10.EGL_NONE };
			EGLContext context = egl.eglCreateContext(display, eglConfig,
					EGL10.EGL_NO_CONTEXT, attrib_list);
			checkEglError(egl);
			return context;
		}

		public void destroyContext(EGL10 egl, EGLDisplay display,
				EGLContext context) {
			egl.eglDestroyContext(display, context);
		}
	}

	private static void checkEglError(EGL10 egl) throws RuntimeException {
		int error;
		while ((error = egl.eglGetError()) != EGL10.EGL_SUCCESS)
			throw new RuntimeException("EGL error: " + error);
	}

	/**
	 * Creates a new translucent OpenGL ES 2.0 surface view with 24 bit depth
	 * buffer, 8 bit stencil buffer and 4x multisampling
	 * 
	 * @param context
	 *            The application context.
	 * @param renderer
	 *            The renderer that will provide the rendering operations for
	 *            this view.
	 */
	public GLES2View(Context context, GLSurfaceView.Renderer renderer) {
		super(context);
		init(renderer, true, 24, 8, 4);
	}

	/**
	 * Creates a new OpenGL ES 2.0 surface view with the specified properties
	 * 
	 * @param context
	 *            The application context.
	 * @param renderer
	 *            The renderer that will provide the rendering operations for
	 *            this view.
	 * @param translucent
	 *            Specified wether this surface view should be translucent or
	 *            opaque.
	 * @param depth
	 *            The number of bits used for the depth buffer.
	 * @param stencil
	 *            The number of bits used for the stencil buffer.
	 * @param samples
	 *            The number of samples to render per pixel.
	 */
	public GLES2View(Context context, GLSurfaceView.Renderer renderer,
			boolean translucent, int depth, int stencil, int samples) {
		super(context);
		init(renderer, translucent, depth, stencil, samples);
	}

	private void init(GLSurfaceView.Renderer renderer, boolean translucent,
			int depth, int stencil, int samples) {
		if (translucent)
			this.getHolder().setFormat(PixelFormat.TRANSLUCENT);
		setEGLContextFactory(new ContextFactory());
		setEGLConfigChooser(translucent ? new ConfigChooser(8, 8, 8, 8, depth,
				stencil, samples) : new ConfigChooser(5, 6, 5, 0, depth,
				stencil, samples));
		setRenderer(renderer);
	}
}
