/**
 * A geometry class for use in Android OpenGL ES 2.0 applications.
 *
 * @author Dennis Harms
 * @version 1.0
 */

package com.stupidcoder.gles2;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.ArrayList;

import android.content.res.Resources;
import android.opengl.GLES20;

public class Geometry {
	static private class BoundingBox {
		public float[] mMax;
		public float[] mMin;
	}

	static private class VertexFormat {
		private class VertexAttribute {
			public String mName;
			public int mOffset;
			public int mSize;

			public VertexAttribute(String pName, int pSize, int pOffset) {
				mName = pName;
				mSize = pSize;
				mOffset = pOffset;
			}
		}

		private ArrayList<VertexAttribute> mAttributes;
		private String mFormat;
		private int mSize;

		public VertexFormat() {
			mSize = 0;
			mAttributes = new ArrayList<VertexAttribute>();
		}

		public void bindToShader(Shader pShader) {
			int attributeCount = mAttributes.size();
			for (int i = 0; i < attributeCount; i++) {
				VertexAttribute attribute = mAttributes.get(i);
				int location = pShader.getAttributeLocation(attribute.mName);
				if (location != -1) {
					GLES20.glEnableVertexAttribArray(location);
					GLES20.glVertexAttribPointer(location, attribute.mSize,
							GLES20.GL_FLOAT, false, mSize * 4,
							attribute.mOffset * 4);
				}
			}
		}

		public int getAttributeOffset(String pAttributeName) {
			int attributeCount = mAttributes.size();
			for (int i = 0; i < attributeCount; i++) {
				VertexAttribute attribute = mAttributes.get(i);
				if (attribute.mName.compareToIgnoreCase(pAttributeName) == 0)
					return attribute.mOffset;
			}
			return -1;
		}

		public int getAttributeSize(String pAttributeName) {
			int attributeCount = mAttributes.size();
			for (int i = 0; i < attributeCount; i++) {
				VertexAttribute attribute = mAttributes.get(i);
				if (attribute.mName.compareToIgnoreCase(pAttributeName) == 0)
					return attribute.mSize;
			}
			return -1;
		}

		public String getFormat() {
			return mFormat;
		}

		public int getSize() {
			return mSize;
		}

		public void setFormat(String pFormat) {
			mSize = 0;
			mAttributes.clear();
			mFormat = pFormat;

			String[] attributes = pFormat.split(",");
			for (int i = 0; i < attributes.length; i++) {
				if (attributes[i].length() == 0)
					continue;
				String[] values = attributes[i].split(":");
				int size = new Integer(values[1]);
				mAttributes.add(new VertexAttribute(values[0], size, mSize));
				mSize += size;
			}
		}
	}

	public class Pass
	{
		protected Shader mShader;
		protected HashMap<String, Shader.ShaderParameter> mShaderParameters;
		protected HashMap<String, Texture> mTextures;
		protected int mFirstIndex, mIndexCount;
		protected boolean mWireframe;
		protected float mOffset;
		protected boolean mTwoSided;
		
		public Pass()
		{
			mShader = null;
			mShaderParameters = new HashMap<String, Shader.ShaderParameter>();
			mTextures = new HashMap<String, Texture>();
			mFirstIndex = mIndexCount = 0;
			mWireframe = false;
			mOffset = 0.0f;
			mTwoSided = false;
		}

		public Pass clearShaderParameters() {
			mShaderParameters.clear();
			return this;
		}

		public Pass removeShaderParameter(String pName) {
			mShaderParameters.remove(pName);
			return this;
		}

		public float[] getShaderParameter(String pName) {
			return mShaderParameters.get(pName).values;
		}

		public Pass setShaderParameter(String pName, float[] pValues) {
			mShaderParameters.put(pName, new Shader.ShaderParameter(1, pValues));
			return this;
		}

		public Pass setShaderParameter(String pName, int pCount, float[] pValues) {
			mShaderParameters.put(pName, new Shader.ShaderParameter(pCount, pValues));
			return this;
		}
		
		public Pass setTexture(String pName, Texture pTexture) {
			mTextures.put(pName, pTexture);
			return this;
		}

		public Pass setWireframe(boolean pWireframe) {
			mWireframe = pWireframe;
			return this;
		}

		public Pass setOffset(float pOffset) {
			mOffset = pOffset;
			return this;
		}

		public Pass setTwoSided(boolean pTwoSided) {
			mTwoSided = pTwoSided;
			return this;
		}
	}
	
	private BoundingBox mExtents;
	private int mIndexBuffer;
	private int[] mIndices;
	private long mLastBuffered;
	private String mName;
	private int mVertexBuffer;
	private float[] mVertexData;
	private VertexFormat mVertexFormat;
	private Pass[] mPasses;
	
	/**
	 * Creates a new empty geometry.
	 */
	public Geometry() {
		mLastBuffered = 0;
		mVertexData = new float[0];
		mIndices = new int[0];
		mVertexFormat = new VertexFormat();
		mExtents = new BoundingBox();
		mVertexBuffer = -1;
		mIndexBuffer = -1;
		mPasses = new Pass[0];
	}
	
	public Pass addPass(Shader pShader)
	{
		Pass pass = new Pass();
		pass.mShader = pShader;
		pass.mIndexCount = mIndices.length;
		addPass(pass);
		return pass;
	}

	public Pass addPass(Shader pShader, int pFirstTriangle, int pNumberOfTriangles)
	{
		Pass pass = new Pass();
		pass.mShader = pShader;
		pass.mFirstIndex = pFirstTriangle * 3;
		pass.mIndexCount = pNumberOfTriangles * 3;
		addPass(pass);
		return pass;
	}
	
	public int getNumberOfPasses()
	{
		return mPasses.length;
	}
	
	public Pass getPass(int pIndex)
	{
		if (pIndex < 0 || pIndex >= mPasses.length)
			return null;
		return mPasses[pIndex];
	}
	
	public int getNumberOfTriangles()
	{
		return mIndices.length / 3;
	}
	
	private void addPass(Pass pPass)
	{
		Pass[] newPasses = new Pass[mPasses.length + 1];
		for (int i = 0 ; i < mPasses.length ; i++)
			newPasses[i] = mPasses[i];
		newPasses[mPasses.length] = pPass;
		mPasses = newPasses;
	}

	private void bufferIfNeeded(long pSurfaceCreationTime) {
		if (mLastBuffered >= pSurfaceCreationTime)
			return;

		if (mVertexFormat.getSize() <= 0 || mVertexData.length < mVertexFormat.mSize || mIndices.length < 3)
			return;

		int[] buffers = new int[2];
		GLES20.glGenBuffers(2, buffers, 0);
		mVertexBuffer = buffers[0];
		mIndexBuffer = buffers[1];

		FloatBuffer vertexData = ByteBuffer.allocateDirect(mVertexData.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
		vertexData.put(mVertexData);
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVertexBuffer);
		GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, mVertexData.length * 4, vertexData.position(0), GLES20.GL_STATIC_DRAW);

		IntBuffer indices = ByteBuffer.allocateDirect(mIndices.length * 4).order(ByteOrder.nativeOrder()).asIntBuffer();
		indices.put(mIndices);
		GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, mIndexBuffer);
		GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, mIndices.length * 4, indices.position(0), GLES20.GL_STATIC_DRAW);

		mLastBuffered = Tools.currentTime();
	}

	/**
	 * Draws the geometry using the specified camera.
	 * 
	 * @param pCamera
	 *            The camera to use the matrices from.
	 * @param pMatrix
	 * 			  The model matrix to use.
	 * @param pSurfaceCreationTime
	 *            The time at which the GL surface has been created. This value
	 *            is used to determine if the vertex and index buffers have to
	 *            be recreated after losing and reacquiring the GL surface.
	 */
	public void draw(Camera pCamera, float[] pMatrix, long pSurfaceCreationTime) {
		bufferIfNeeded(pSurfaceCreationTime);
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVertexBuffer);
		GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, mIndexBuffer);

		for (int i = 0 ; i < mPasses.length ; i++)
		{
			Pass pass = mPasses[i];
			
			pass.mShader.setShaderParameter("u_projection", pCamera.getProjection());
			pass.mShader.setShaderParameter("u_view", pCamera.getView());
			pass.mShader.setShaderParameter("u_model", pMatrix);
			pass.mShader.use(pSurfaceCreationTime);
			pass.mShader.setUniforms(pass.mShaderParameters);
			pass.mShader.setTextures(pass.mTextures, pSurfaceCreationTime);

			mVertexFormat.bindToShader(pass.mShader);

			if (!pass.mTwoSided) {
				GLES20.glEnable(GLES20.GL_CULL_FACE);
				GLES20.glCullFace(GLES20.GL_BACK);
			}
			else
				GLES20.glDisable(GLES20.GL_CULL_FACE);
			
			if (pass.mOffset != 0.0f) {
				GLES20.glEnable(GLES20.GL_POLYGON_OFFSET_FILL);
				GLES20.glPolygonOffset(pass.mOffset, pass.mOffset);
			}
			else
				GLES20.glDisable(GLES20.GL_POLYGON_OFFSET_FILL);
			
			if (pass.mWireframe) {
				for (int j = 0 ; j < pass.mIndexCount ; j += 3)
					GLES20.glDrawElements(GLES20.GL_LINE_LOOP, 3, GLES20.GL_UNSIGNED_INT, (pass.mFirstIndex + j) * 4);
			}
			else {
				GLES20.glDrawElements(GLES20.GL_TRIANGLES, pass.mIndexCount, GLES20.GL_UNSIGNED_INT, pass.mFirstIndex * 4);
			}
		}
	}

	/**
	 * Gets the currently used vertex format.
	 * 
	 * @param pFormat
	 * @return The format of the vertex data. This string contains a comma
	 *         separated list of vertex attributes. Each vertex attribute
	 *         consists of a name and a size, divided by a colon. For example:
	 *         a_position:3,a_normal:3,a_texCoord:2
	 */
	public String getVertexFormat() {
		return mVertexFormat.getFormat();
	}

	/**
	 * Loads geometry data in SMF format from the resources.
	 * 
	 * @param pRes
	 *            The resources instance of the application's package.
	 * @param pId
	 *            The ID of the resource containing the SMF geometry data.
	 * @throws IOException
	 *             if a problem occurs while reading the data.
	 */
	public void loadSMF(Resources pRes, int pId) throws IOException {
		DataInputStream input = new DataInputStream(pRes.openRawResource(pId));
		if (input.readByte() != 'S' || input.readByte() != 'M'
				|| input.readByte() != 'F' || input.readByte() != 0)
			return;
		int version = input.read();
		if (version != 1 || input.readByte() != 'B')
			return;
		mName = input.readUTF();

		String vertexFormat = input.readUTF();
		this.setVertexFormat(vertexFormat);

		mExtents.mMin = new float[mVertexFormat.mSize];
		mExtents.mMax = new float[mVertexFormat.mSize];
		for (int i = 0; i < mVertexFormat.mSize; i++) {
			mExtents.mMin[i] = input.readFloat();
			mExtents.mMax[i] = input.readFloat();
		}

		int vertexCount = input.readInt();
		mVertexData = new float[vertexCount];
		for (int i = 0; i < vertexCount; i++)
			mVertexData[i] = input.readFloat(); // TODO: Optimize

		int indexCount = input.readInt();
		mIndices = new int[indexCount];
		for (int i = 0; i < indexCount; i++)
			mIndices[i] = input.readInt(); // TODO: Optimize

		input.close();
		mLastBuffered = 0;
	}

	/**
	 * Saves geometry data in SMF format to a file.
	 * 
	 * @param pFilename
	 *            The path and filename under which to save the data.
	 * @throws IOException
	 *             if a problem occurs while reading the data.
	 */
	public void saveSMF(String pFilename) throws IOException {
		updateExtents();
		RandomAccessFile file = new RandomAccessFile(pFilename, "rw");
		file.write(new byte[] { 'S', 'M', 'F', 0 }); // File identifier
		file.writeByte(1); // File format version
		file.writeByte('B'); // Big endian
		file.writeUTF(mName);
		file.writeUTF(mVertexFormat.getFormat());
		for (int i = 0; i < mVertexFormat.mSize; i++) {
			file.writeFloat(mExtents.mMin[i]);
			file.writeFloat(mExtents.mMax[i]);
		}
		file.writeInt(mVertexData.length);
		for (int i = 0; i < mVertexData.length; i++)
			file.writeFloat(mVertexData[i]); // TODO: Optimize
		file.writeInt(mIndices.length);
		for (int i = 0; i < mIndices.length; i++)
			file.writeInt(mIndices[i]); // TODO: Optimize
		file.close();
	}

	/**
	 * Copies a list of vertex indices into the geometry.
	 * 
	 * @param pValues
	 *            The vertex indices as an array of integer values.
	 */
	public void setIndices(int[] pValues) {
		mIndices = pValues.clone();
		mLastBuffered = 0;
	}

	/**
	 * Copies a list of vertex indices into the geometry.
	 * 
	 * @param pValues
	 *            The vertex indices as a vector of integer values.
	 */
	public void setIndices(ArrayList<Integer> pValues) {
		mIndices = new int[pValues.size()];
		for (int i = 0; i < mIndices.length; i++)
			mIndices[i] = pValues.get(i).intValue();
		mLastBuffered = 0;
	}

	/**
	 * Copies vertex data from the specified array into the geometry.
	 * 
	 * @param pValues
	 *            The vertex data as an array of float values. The layout of the
	 *            data must match the specified vertex format string.
	 */
	public void setVertexData(float[] pValues) {
		mVertexData = pValues.clone();
		mLastBuffered = 0;
	}

	/**
	 * Copies vertex data from the specified array into the geometry.
	 * 
	 * @param pValues
	 *            The vertex data as a vector of float objects. The layout of
	 *            the data must match the specified vertex format string.
	 */
	public void setVertexData(ArrayList<Float> pValues) {
		mVertexData = new float[pValues.size()];
		for (int i = 0; i < mVertexData.length; i++)
			mVertexData[i] = pValues.get(i).floatValue();
		mLastBuffered = 0;
	}

	/**
	 * Copies vertex data from the specified array into the given attribute of
	 * the vertices, starting at a certain vertex. If the current vertex data
	 * array is not large enough to hold the given data, the array will be grown
	 * automatically.
	 * 
	 * @param pAttribute
	 *            The name of the attribute as given in the vertex format.
	 * @param pFirstVertex
	 *            The first vertex in which to set the attributes.
	 * @param pValues
	 *            The new vertex attribute data.
	 */
	public void setVertexAttributeData(String pAttribute, int pFirstVertex, ArrayList<Float> pValues) {
		int vertexSize = mVertexFormat.getSize();
		int attributeOffset = mVertexFormat.getAttributeOffset(pAttribute);
		int attributeSize = mVertexFormat.getAttributeSize(pAttribute);
		int numberOfValues = pValues.size() / attributeSize;
		if (pFirstVertex + numberOfValues >= mVertexData.length / vertexSize)
			growVertexData(pFirstVertex + numberOfValues);

		for (int i = 0; i < numberOfValues; i++)
			for (int j = 0; j < attributeSize; j++)
				mVertexData[(pFirstVertex + i) * vertexSize + attributeOffset
						+ j] = pValues.get(i * attributeSize + j);

		mLastBuffered = 0;
	}
	
	public float[] getVertexAttribute(String pAttribute, int pIndex) {
		int vertexSize = mVertexFormat.getSize();
		int attributeOffset = mVertexFormat.getAttributeOffset(pAttribute);
		int attributeSize = mVertexFormat.getAttributeSize(pAttribute);
		int index = pIndex * vertexSize + attributeOffset;
		float[] values = new float[attributeSize];
		for (int i = 0 ; i < attributeSize ; i++)
			values[i] = mVertexData[index + i];
		return values;
	}

	public float[] getVertexAttributes(String pAttribute, int pFirstIndex, int pVertexCount) {
		int vertexSize = mVertexFormat.getSize();
		int attributeOffset = mVertexFormat.getAttributeOffset(pAttribute);
		int attributeSize = mVertexFormat.getAttributeSize(pAttribute);
		int index = pFirstIndex * vertexSize + attributeOffset;
		float[] values = new float[attributeSize * pVertexCount];
		for (int j = 0 ; j < pVertexCount ; j++)
			for (int i = 0 ; i < attributeSize ; i++)
				values[j * attributeSize + i] = mVertexData[index + j * vertexSize + i];
		return values;
	}
	
	/**
	 * Removes all vertex data from this geometry.
	 */
	public void clearVertexData() {
		mVertexData = new float[0];
		mLastBuffered = 0;
	}

	/**
	 * Increases the size of the vertex data array to accommodate the specified
	 * number of vertices.
	 * 
	 * @param pSize
	 *            The number of vertices that should fit into the array.
	 */
	public void growVertexData(int pSize) {
		float[] newData = new float[pSize * mVertexFormat.getSize()];
		for (int i = 0; i < mVertexData.length; i++)
			newData[i] = mVertexData[i];
		mVertexData = newData;
	}

	/**
	 * Sets the format of the vertex data.
	 * 
	 * @param pFormat
	 *            The format of the vertex data. This string contains a comma
	 *            separated list of vertex attributes. Each vertex attribute
	 *            consists of a name and a size, divided by a colon. For
	 *            example: a_position:3,a_normal:3,a_texCoord:2
	 */
	public void setVertexFormat(String pFormat) {
		mVertexFormat.setFormat(pFormat);
		mLastBuffered = 0;
	}

	private void updateExtents() {
		int vertexSize = mVertexFormat.mSize;

		mExtents.mMin = new float[vertexSize];
		mExtents.mMax = new float[vertexSize];
		for (int i = 0; i < vertexSize; i++) {
			mExtents.mMin[i] = mVertexData[i];
			mExtents.mMax[i] = mVertexData[i];
		}

		for (int i = vertexSize; i < mVertexData.length; i++) {
			mExtents.mMin[i % vertexSize] = Math.min(mVertexData[i],
					mExtents.mMin[i % vertexSize]);
			mExtents.mMax[i % vertexSize] = Math.max(mVertexData[i],
					mExtents.mMax[i % vertexSize]);
		}
	}
}
