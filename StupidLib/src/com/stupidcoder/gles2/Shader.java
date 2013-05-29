/**
 * A shader class for use in Android OpenGL ES 2.0 applications.
 *
 * @author Dennis Harms
 * @version 1.0
 */

package com.stupidcoder.gles2;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import android.content.res.Resources;
import android.opengl.GLES20;

public class Shader {
	private String mVertexSource;
	private String mFragmentSource;

	private int mProgram;	
	private long mLastBuild;
	private Map<String, Integer> mLocations;
	
	public static class ShaderParameter {
		public int count;
		public float[] values;
		
		public ShaderParameter(int pCount, float[] pValues) {
			count = pCount;
			values = pValues;
		}
	}

	private Map<String, ShaderParameter> mParameters;
	private Map<String, Texture> mTextures;
	
	/**
	 * Creates a new empty shader.
	 */
	public Shader() {
		mLastBuild = 0;
		mVertexSource = "";
		mFragmentSource = "";
		mProgram = -1;
		mParameters = new HashMap<String, ShaderParameter>();
		mTextures = new HashMap<String, Texture>();
		mLocations = new HashMap<String, Integer>();
	}

	private void buildIfNeeded(long pSurfaceCreationTime) {
		if (mLastBuild >= pSurfaceCreationTime)
			return;

		mProgram = createProgram(mVertexSource, mFragmentSource);
		mLastBuild = Tools.currentTime();
	}

	private int createProgram(String vertexSource, String fragmentSource) {
		int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
		if (vertexShader == 0)
			return 0;

		int pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
		if (pixelShader == 0)
			return 0;

		int program = GLES20.glCreateProgram();
		if (program != 0) {
			GLES20.glAttachShader(program, vertexShader);
			GLES20.glAttachShader(program, pixelShader);
			GLES20.glLinkProgram(program);
			int[] linkStatus = new int[1];
			GLES20
					.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus,
							0);
			if (linkStatus[0] != GLES20.GL_TRUE) {
				String programLog = GLES20.glGetShaderInfoLog(program);
				throw new RuntimeException("Could not link shader program: "
						+ programLog);
			}
		}
		return program;
	}

	private int findUniform(String pName) {
		Integer cachedLocation = mLocations.get(pName);
		if (cachedLocation != null)
			return cachedLocation.intValue();
		int location = GLES20.glGetUniformLocation(mProgram, pName);
		mLocations.put(pName, new Integer(location));
		return location;
	}

	/**
	 * Gets the location of the specified vertex attribute.
	 * 
	 * @param pName
	 *            The name of the attribute as used in the shader source code.
	 * @return The location of the attribute or -1 if the attribute doesn't
	 *         exist.
	 */
	public int getAttributeLocation(String pName) {
		Integer cachedLocation = mLocations.get(pName);
		if (cachedLocation != null)
			return cachedLocation.intValue();
		int location = GLES20.glGetAttribLocation(mProgram, pName);
		mLocations.put(pName, new Integer(location));
		return location;
	}

	/**
	 * Gets the currently used fragment shader source code.
	 * 
	 * @return The currently used fragment shader source code.
	 */
	public String getFragmentSource() {
		return mFragmentSource;
	}

	/**
	 * Gets the current value of the specified shader parameter.
	 * 
	 * @param pName
	 *            The name of the (uniform) shader parameter as used in the
	 *            shader source code.
	 * @return The current value as an array of float values. The size of this
	 *         array can be 1, 2, 3 or 4 values for vectors, or 16 values for
	 *         matrices.
	 */
	public float[] getShaderParameter(String pName) {
		return mParameters.get(pName).values;
	}

	/**
	 * Gets the currently used vertex shader source code.
	 * 
	 * @return The currently used vertex shader source code.
	 */
	public String getVertexSource() {
		return mVertexSource;
	}

	private int loadShader(int shaderType, String source) {
		int shader = GLES20.glCreateShader(shaderType);
		if (shader != 0) {
			GLES20.glShaderSource(shader, source);
			GLES20.glCompileShader(shader);
			int[] compiled = new int[1];
			GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
			if (compiled[0] == 0) {
				String shaderLog = GLES20.glGetShaderInfoLog(shader);
				throw new RuntimeException("Could not compile shader "
						+ shaderType + ": " + shaderLog);
			}
		}
		return shader;
	}

	/**
	 * Sets the source code for the fragment shader.
	 * 
	 * @param pRes
	 *            The resources instance of the application's package.
	 * @param pId
	 *            The ID of the resource containing the new fragment shader
	 *            source code.
	 */
	public void setFragmentSource(Resources pRes, int pId) {
		mLastBuild = 0;
		mParameters.clear();
		mLocations.clear();
		try {
			mFragmentSource = Tools.getTextFromResource(pRes, pId);
		} catch (IOException e) {
			mFragmentSource = "";
		}
	}

	/**
	 * Sets the source code for the fragment shader.
	 * 
	 * @param pSource
	 *            The new fragment shader source code.
	 */
	public void setFragmentSource(String pSource) {
		mLastBuild = 0;
		mFragmentSource = pSource;
		mParameters.clear();
		mLocations.clear();
	}

	/**
	 * Sets one of the shaders uniform parameters. The actual value will get set
	 * the next time the shader gets used.
	 * 
	 * @param pName
	 *            The name of the shader parameter as used in the shader source
	 *            code.
	 * @param pValues
	 *            The value to be set as an array of float values. The size of
	 *            this array can be 1, 2, 3 or 4 values for vectors, or 16
	 *            values for matrices.
	 */
	public void setShaderParameter(String pName, float[] pValues) {
		mParameters.put(pName, new ShaderParameter(1, pValues));
	}

	public void setShaderParameter(String pName, int pCount, float[] pValues) {
		mParameters.put(pName, new ShaderParameter(pCount, pValues));
	}
	
	public void clearShaderParameters() {
		mParameters.clear();
	}

	public void removeShaderParameter(String pName) {
		mParameters.remove(pName);
	}

	public void setTexture(String pName, Texture pTexture) {
		mTextures.put(pName, pTexture);
	}
	
	protected void setUniforms(Map<String, ShaderParameter> pParameters) {
		Iterator<String> keys = pParameters.keySet().iterator();
		while (keys.hasNext()) {
			String name = keys.next();
			int location = findUniform(name);
			if (location != -1) {
				ShaderParameter parameter = pParameters.get(name);
				int count = parameter.count;
				float[] values = parameter.values;
				switch (values.length / count) {
				case 1:
					GLES20.glUniform1fv(location, count, values, 0);
					break;
				case 2:
					GLES20.glUniform2fv(location, count, values, 0);
					break;
				case 3:
					GLES20.glUniform3fv(location, count, values, 0);
					break;
				case 4:
					GLES20.glUniform4fv(location, count, values, 0);
					break;
				case 16:
					GLES20.glUniformMatrix4fv(location, count, false, values, 0);
					break;
				}
			}
		}
	}

	protected void setTextures(Map<String, Texture> pTextures, long pSurfaceCreationTime) {
		int nextTexture = 0;
		Iterator<String> keys = pTextures.keySet().iterator();
		while (keys.hasNext()) {
			String name = keys.next();
			int location = findUniform(name);
			if (location != -1) {
				GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + nextTexture);
				GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, pTextures.get(name).use(pSurfaceCreationTime));
				GLES20.glUniform1i(location, nextTexture++);
			}
		}
	}
	
	/**
	 * Sets the source code for the vertex shader.
	 * 
	 * @param pRes
	 *            The resources instance of the application's package.
	 * @param pId
	 *            The ID of the resource containing the new vertex shader source
	 *            code.
	 */
	public void setVertexSource(Resources pRes, int pId) {
		mLastBuild = 0;
		mParameters.clear();
		mLocations.clear();
		try {
			mVertexSource = Tools.getTextFromResource(pRes, pId);
		} catch (IOException e) {
			mVertexSource = "";
		}
	}

	/**
	 * Sets the source code for the vertex shader.
	 * 
	 * @param pSource
	 *            The new vertex shader source code.
	 */
	public void setVertexSource(String pSource) {
		mLastBuild = 0;
		mVertexSource = pSource;
		mParameters.clear();
		mLocations.clear();
	}

	/**
	 * Activates the shader and sets all uniform parameters, compiling it if the
	 * shader source codes have been changed since the last activation.
	 * 
	 * @param pSurfaceCreationTime
	 *            The time at which the GL surface has been created. This value
	 *            is used to determine if the shader has to be recompiled after
	 *            losing and reaquiring the GL surface.
	 */
	public void use(long pSurfaceCreationTime) {
		buildIfNeeded(pSurfaceCreationTime);
		GLES20.glUseProgram(mProgram);
		setUniforms(mParameters);
		setTextures(mTextures, pSurfaceCreationTime);
	}
}
