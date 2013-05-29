package com.stupidcoder.stupidlibtest;

import com.stupidcoder.gles2.*;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.util.Log;

import android.app.Activity;
import android.graphics.Point;
import android.opengl.Matrix;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.Display;
import android.view.Window;
import android.view.WindowManager;
import android.view.SurfaceHolder;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.opengl.GLSurfaceView;

public class main extends Activity implements Scene.RenderEvent {
	final static float UNDERSAMPLING = 1.0f;
	
	private GLSurfaceView mGLView;
	private Scene mScene;
	private long mTotalTime = 0;
	private float mPrevTouchX, mPrevTouchY;
	
    @Override public void onCreate(Bundle savedInstanceState) {
		// Create the main window without a title bar and in fullscreen mode
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

		// Check if OpenGL ES 2.0 is supported and quit otherwise
		final ActivityManager activityManager = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
		final ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();
		final boolean supportsEs2 = configurationInfo.reqGlEsVersion >= 0x20000;
		if (!supportsEs2)
			return;

		// Create the test scene
		mScene = setupScene();
		
		// Figure out the display resolution and calculate the backbuffer size
		Point displaySize = new Point();
		Display display = getWindowManager().getDefaultDisplay();
		display.getSize(displaySize);
		int backbufferWidth = (int)((float)displaySize.x / UNDERSAMPLING);
		int backbufferHeight = (int)((float)displaySize.y / UNDERSAMPLING);
		Log.i("StupidLibTest", "Screen resolution: " + displaySize.x + " x " + displaySize.y + " @ " + display.getRefreshRate() + " Hz");
		Log.i("StupidLibTest", "Rendering with " + UNDERSAMPLING + "x undersampling at " + backbufferWidth + " x " + backbufferHeight);
		
		// Create the GLES 2.0 surface, set the backbuffer size and disable the screensaver
		mGLView = new GLSurfaceView(this);
		mGLView.setEGLContextClientVersion(2);
		mGLView.getHolder().setFixedSize(backbufferWidth, backbufferHeight);
		mGLView.getHolder().setKeepScreenOn(true);

		// The scene class implements the GL surface renderer interface and will be our renderer
		mGLView.setRenderer(mScene);

		// Put the GLES surface into our main window
		setContentView(mGLView);
    }

	private Scene setupScene() {
		mScene = new Scene(this);
		mScene.setBackgroundColor(0.0f, 0.0f, 0.0f, 1.0f);

		// ---------------------------------------------------------------------------------
		// Shaders

		Shader phong = mScene.createShader("phong");
		phong.setVertexSource(getResources(), R.raw.phong_vsh);
		phong.setFragmentSource(getResources(), R.raw.phong_fsh);
		phong.setShaderParameter("u_lightColor", 2, new float[] { 1.0f, 0.7f, 0.7f, 0.7f, 1.0f, 0.7f });

		Shader solid = mScene.createShader("solid");
		solid.setVertexSource(getResources(), R.raw.solid_vsh);
		solid.setFragmentSource(getResources(), R.raw.solid_fsh);
		solid.setShaderParameter("u_color", new float[] { 1.0f, 0.0f, 0.0f, 1.0f });

		// ---------------------------------------------------------------------------------
		// Floor

		float floorSize = 10.0f;
		float floorHeight = 2.25f;

		GeometryBuilder builder = new GeometryBuilder();
		builder.quad(-floorSize, floorHeight, -floorSize, -floorSize, floorHeight, floorSize, floorSize, floorHeight, floorSize, floorSize, floorHeight, -floorSize);
		builder.planarTexture(new Vec3(0.0f, 0.0f, 0.0f), new Vec3(4.0f, 0.0f, 0.0f), new Vec3(0.0f, 0.0f, 4.0f));

		Geometry geo = new Geometry();
		geo.setVertexFormat("a_position:3,a_texCoord:2,a_normal:3");
		geo.setVertexAttributeData("a_position", 0, builder.getPositions());
		geo.setVertexAttributeData("a_normal", 0, builder.getNormals());
		geo.setVertexAttributeData("a_texCoord", 0, builder.getTexCoords());
		geo.setIndices(builder.getIndices());
		geo.addPass(phong).setTexture("s_texture", new Texture(getResources(), R.raw.tiles));

		Scene.Node node = mScene.createSceneNode("floor");
		node.setGeometry(geo);
		mScene.getRoot().getChildren().add(node);

		// ---------------------------------------------------------------------------------
		// Object

		builder = new GeometryBuilder();
		builder.sphere(new Vec3(0.0f, 0.0f, 0.0f), 2.0f, 4);
		builder.planarTexture(new Vec3(-2.0f, -2.0f, 0.0f), new Vec3(4.0f, 0.0f, 0.0f), new Vec3(0.0f, 4.0f, 0.0f));

		geo = new Geometry();
		geo.setVertexFormat("a_position:3,a_texCoord:2,a_normal:3");
		geo.setVertexAttributeData("a_position", 0, builder.getPositions());
		geo.setVertexAttributeData("a_normal", 0, builder.getNormals());
		geo.setVertexAttributeData("a_texCoord", 0, builder.getTexCoords());
		geo.setIndices(builder.getIndices());
		geo.addPass(phong).setOffset(1.0f).setTexture("s_texture", new Texture(getResources(), R.raw.uvgrid));
		//geo.addPass(solid).setWireframe(true);

		node = mScene.createSceneNode("object");
		node.setGeometry(geo);
		mScene.getRoot().getChildren().add(node);

		// ---------------------------------------------------------------------------------
		// Lights

		builder = new GeometryBuilder();
		builder.sphere(new Vec3(0.0f, 0.0f, 0.0f), 0.05f, 2);

		geo = new Geometry();
		geo.setVertexFormat("a_position:3,a_texCoord:2,a_normal:3");
		geo.setVertexAttributeData("a_position", 0, builder.getPositions());
		geo.setVertexAttributeData("a_normal", 0, builder.getNormals());
		geo.setIndices(builder.getIndices());
		geo.addPass(solid).setShaderParameter("u_color", new float[] { 1.0f, 0.7f, 0.7f, 1.0f });

		node = mScene.createSceneNode("light1");
		node.setGeometry(geo);
		mScene.getRoot().getChildren().add(node);

		geo = new Geometry();
		geo.setVertexFormat("a_position:3,a_texCoord:2,a_normal:3");
		geo.setVertexAttributeData("a_position", 0, builder.getPositions());
		geo.setVertexAttributeData("a_normal", 0, builder.getNormals());
		geo.setIndices(builder.getIndices());
		geo.addPass(solid).setShaderParameter("u_color", new float[] { 0.7f, 1.0f, 0.7f, 1.0f });

		node = mScene.createSceneNode("light2");
		node.setGeometry(geo);
		mScene.getRoot().getChildren().add(node);

		// ---------------------------------------------------------------------------------
		// Camera

		mScene.getCamera().setLookAt(0.0f, 0.0f, -15.0f, 0.0f, 0.0f, 0.0f);
		mScene.getCamera().setFieldOfView(30.0f);

		Point displaySize = new Point();
		getWindowManager().getDefaultDisplay().getSize(displaySize);
		mScene.getCamera().setAspect((float)displaySize.x / (float)displaySize.y);

		return mScene;
	}
	
	public void prerender(Scene pScene, long pElapsedTime)
	{
		// Keep track of the total time elapsed since app start (used for animation)
		mTotalTime += pElapsedTime;
		Log.v("StupidLibTest", "Last frame is " + pElapsedTime + "ms ago: " + (1.0f / ((float)pElapsedTime / 1000.0f)) + " FPS");
		
		// Let the object in the center of the scene slowly spin around two axes
		float[] objectMatrix = mScene.getSceneNode("object").getMatrix();
		float objectAngle = mTotalTime / 100.0f;
		Matrix.setRotateM(objectMatrix, 0, objectAngle / 2.0f, -1.0f, 0.0f, 0.0f);
		Matrix.rotateM(objectMatrix, 0, objectAngle, 0.0f, 1.0f, 0.0f);

		// Let the first light fly a counterclockwise sine curve around the center object
		float[] light1Matrix = mScene.getSceneNode("light1").getMatrix();
		float light1Angle = mTotalTime / 1000.0f;
		Matrix.setIdentityM(light1Matrix, 0);
		Matrix.translateM(light1Matrix, 0, (float)Math.sin(light1Angle) * 3.0f, 1.0f + (float)Math.sin(light1Angle * 3.0f) * 0.5f, (float)Math.cos(light1Angle) * 3.0f);

		// Let the second light fly a clockwise sine curve around the center object
		float[] light2Matrix = mScene.getSceneNode("light2").getMatrix();
		float light2Angle = mTotalTime / 734.0f;
		Matrix.setIdentityM(light2Matrix, 0);
		Matrix.translateM(light2Matrix, 0, (float)Math.cos(light2Angle) * 3.0f, 1.0f + (float)Math.cos(light2Angle * 2.0f) * 0.5f, (float)Math.sin(light2Angle) * 3.0f);
		
		// Plug the current camera and light positions into the Phong shader
		Vec3 cameraPos = mScene.getCamera().getPosition();
		Shader phong = mScene.getShader("phong");
		phong.setShaderParameter("u_cameraPos", new float[] { cameraPos.x, cameraPos.y, cameraPos.z });
		phong.setShaderParameter("u_lightPos", 2, new float[] { light1Matrix[12], light1Matrix[13], light1Matrix[14], light2Matrix[12], light2Matrix[13], light2Matrix[14] });
	}

	@Override
	public boolean onTouchEvent(MotionEvent e) {
		float x = e.getX();
		float y = e.getY();
		
		switch (e.getAction()) {
			// Finger movements orbit the camera
			case MotionEvent.ACTION_MOVE:
				mScene.getCamera().orbit((mPrevTouchX - x) / 10.0f, (mPrevTouchY - y) / 10.0f, 0.0f);
				break;
		}
		
		mPrevTouchX = x;
		mPrevTouchY = y;
		return true;
	}
}
