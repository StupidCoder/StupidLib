/**
 * A scene graph class which implements the GLSurfaceView.Renderer interface for use in Android OpenGL ES 2.0 applications.
 *
 * @author Dennis Harms
 * @version 1.0
 */

package com.stupidcoder.gles2;

import java.util.ArrayList;
import java.util.HashMap;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

public class Scene implements GLSurfaceView.Renderer {
	static public class Node {
		private ArrayList<Node> mChildren;
		private float[] mCurrentMatrix;
		private Geometry mGeometry;
		private float[] mIdentityMatrix;
		private float[] mMatrix;
		private boolean mVisible;

		/**
		 * Creates a new scene graph node without geometry or shader and with an
		 * identity matrix.
		 */
		public Node() {
			mMatrix = new float[16];
			Matrix.setIdentityM(mMatrix, 0);
			mIdentityMatrix = new float[16];
			Matrix.setIdentityM(mIdentityMatrix, 0);
			mCurrentMatrix = new float[16];
			mVisible = true;
			mGeometry = null;
			mChildren = new ArrayList<Node>();
		}

		protected void draw(Camera pCamera, float[] pMatrix, long pSurfaceCreationTime) {
			Matrix.multiplyMM(mCurrentMatrix, 0, pMatrix, 0, mMatrix, 0);

			if (mGeometry != null && mVisible)
				mGeometry.draw(pCamera, mCurrentMatrix, pSurfaceCreationTime);

			for (int i = 0; i < mChildren.size(); i++)
				mChildren.get(i).draw(pCamera, mCurrentMatrix, pSurfaceCreationTime);
		}

		protected void draw(Camera pCamera, long pSurfaceCreationTime) {
			draw(pCamera, mIdentityMatrix, pSurfaceCreationTime);
		}

		/**
		 * Gets the list of children of this scene graph node.
		 * 
		 * @return The child nodes.
		 */
		public ArrayList<Node> getChildren() {
			return mChildren;
		}

		/**
		 * Gets the currently attached geometry.
		 * 
		 * @return The currently attached geometry.
		 */
		public Geometry getGeometry() {
			return mGeometry;
		}

		/**
		 * Gets the current transformation matrix of this scene graph node.
		 * 
		 * @return The current transformation matrix.
		 */
		public float[] getMatrix() {
			return mMatrix;
		}

		public Vec3 getPosition() {
			return new Vec3(mMatrix[12], mMatrix[13], mMatrix[14]);
		}

		public void setPosition(Vec3 pPosition) {
			mMatrix[12] = pPosition.x;
			mMatrix[13] = pPosition.y;
			mMatrix[14] = pPosition.z;
		}

		public void setPosition(float pX, float pY, float pZ) {
			mMatrix[12] = pX;
			mMatrix[13] = pY;
			mMatrix[14] = pZ;
		}
		
		/**
		 * Attaches a new geometry to this scene graph node.
		 * 
		 * @param pGeometry
		 *            The new geometry to use.
		 */
		public void setGeometry(Geometry pGeometry) {
			mGeometry = pGeometry;
		}

		/**
		 * Sets the transformation matrix of this scene graph node to the given
		 * value.
		 * 
		 * @param pMatrix
		 *            The new transformation matrix to use.
		 */
		public void setMatrix(float[] pMatrix) {
			mMatrix = pMatrix.clone();
		}
		
		public void setVisible(boolean pVisible) {
			mVisible = pVisible;
		}
	}

	/**
	 * Checks if any OpenGL errors have occured since the last call.
	 * 
	 * @throws RuntimeException
	 *             if any OpenGL errors have occured.
	 */
	static public void checkGlError() throws RuntimeException {
		int error;
		while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR)
			throw new RuntimeException("glError: " + error);
	}

	private float mAspect;

	private float[] mBackgroundColor;
	private Camera mCamera;
	private Node mRoot;
	private long mSurfaceCreationTime, mLastFrameTime;
	private boolean mStereo;
	private int mWidth, mHeight;
	
	private HashMap<String, Shader> mShaders;
	private HashMap<String, Scene.Node> mSceneNodes;
	
	public interface RenderEvent {
		public void prerender(Scene pScene, long pElapsedTime);
	}
	RenderEvent mEventReceiver;
	
	/**
	 * Creates a new scene graph with an empty root node and a default camera.
	 */
	public Scene(RenderEvent pEventReceiver) {
		super();
		mRoot = new Node();
		mCamera = new Camera();
		mSurfaceCreationTime = 0;
		mLastFrameTime = 0;
		mWidth = mHeight = 0;
		mAspect = 1.0f;
		mBackgroundColor = new float[] { 0.0f, 0.0f, 0.0f, 1.0f };
		mEventReceiver = pEventReceiver;
		mStereo = false;
		
		mShaders = new HashMap<String, Shader>();
		mSceneNodes = new HashMap<String, Scene.Node>();
	}

	/**
	 * Gets the currently active camera.
	 * 
	 * @return The camera that is currently used for rendering.
	 */
	public Camera getCamera() {
		return mCamera;
	}

	/**
	 * Gets the current root node of the scene graph.
	 * 
	 * @return The current root node of the scene graph.
	 */
	public Node getRoot() {
		return mRoot;
	}
	
	public boolean getStereo() {
		return mStereo;
	}
	
	public void setStereo(boolean pStereo) {
		mStereo = pStereo;
	}

	public void onDrawFrame(GL10 gl) {
		long currentTime = Tools.currentTime();
		mEventReceiver.prerender(this, currentTime - mLastFrameTime);
		mLastFrameTime = currentTime;
		
		GLES20.glClearColor(mBackgroundColor[0], mBackgroundColor[1], mBackgroundColor[2], mBackgroundColor[3]);
		GLES20.glClearDepthf(1.0f);
		GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);

		GLES20.glEnable(GLES20.GL_DEPTH_TEST);
		GLES20.glDepthFunc(GLES20.GL_LEQUAL);
		
		if (mRoot != null && mCamera != null) {
			mCamera.setAspect(mAspect);

			if (!mStereo)
			{
				mCamera.updateCamera();
				mRoot.draw(mCamera, mSurfaceCreationTime);
			}
			else
			{
				mCamera.pan(0.1f, 0.0f, 0.0f);
				mCamera.updateCamera();
				GLES20.glColorMask(true, false, true, true);
				mRoot.draw(mCamera, mSurfaceCreationTime);
				
				mCamera.pan(-0.2f, 0.0f, 0.0f);
				mCamera.updateCamera();
				GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT);
				GLES20.glColorMask(false, true, false, true);
				mRoot.draw(mCamera, mSurfaceCreationTime);
				
				mCamera.pan(0.1f, 0.0f, 0.0f);
				GLES20.glColorMask(true, true, true, true);
			}
		}

		checkGlError();
	}

	public void onSurfaceChanged(GL10 gl, int width, int height) {
		mWidth = width;
		mHeight = height;
		mAspect = (float) mWidth / (float) mHeight;
		GLES20.glViewport(0, 0, mWidth, mHeight);
	}

	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		mSurfaceCreationTime = Tools.currentTime();
		mLastFrameTime = Tools.currentTime();
		GLES20.glDepthMask(true);
		GLES20.glEnable(GLES20.GL_DEPTH_TEST);
		GLES20.glDepthFunc(GLES20.GL_LESS);
		GLES20.glFrontFace(GLES20.GL_CCW);
		GLES20.glEnable(GLES20.GL_CULL_FACE);
		GLES20.glCullFace(GLES20.GL_BACK);
	}

	/**
	 * Sets a new background color for this scene.
	 * 
	 * @param pR
	 *            The red component of the new background color.
	 * @param pG
	 *            The blue component of the new background color.
	 * @param pB
	 *            The green component of the new background color.
	 * @param pA
	 *            The alpha component of the new background color.
	 */
	public void setBackgroundColor(float pR, float pG, float pB, float pA) {
		mBackgroundColor[0] = pR;
		mBackgroundColor[1] = pG;
		mBackgroundColor[2] = pB;
		mBackgroundColor[3] = pA;
	}

	/**
	 * Sets the active camera to the specified one.
	 * 
	 * @param pCamera
	 *            The new camera to use for rendering.
	 */
	public void setCamera(Camera pCamera) {
		mCamera = pCamera;
	}

	/**
	 * Sets the root node of the scene graph.
	 * 
	 * @param pRoot
	 *            The new root node for the scene graph.
	 */
	public void setRoot(Node pRoot) {
		mRoot = pRoot;
	}
	
	public Shader createShader(String pName) {
		Shader shader = new Shader();
		mShaders.put(pName, shader);
		return shader;
	}
	
	public Shader getShader(String pName) {
		return mShaders.get(pName);
	}

	public Scene.Node createSceneNode(String pName) {
		Scene.Node sceneNode = new Scene.Node();
		mSceneNodes.put(pName, sceneNode);
		return sceneNode;
	}

	public Scene.Node getSceneNode(String pName) {
		return mSceneNodes.get(pName);
	}
}
