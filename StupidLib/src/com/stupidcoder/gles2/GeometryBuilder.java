/**
 * A helper class for easier creation of geometric objects.
 *
 * @author Dennis Harms
 * @version 1.0
 */

package com.stupidcoder.gles2;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ArrayList;
import android.util.Log;

public class GeometryBuilder {
	// ------------------------------------------------------------
	// Inner Vertex class

	static private class Vertex {
		public Vec3 pos;
		public Vec3 norm;
		public float u, v;
		
		public Vertex() {
			pos = new Vec3();
			norm = new Vec3();
			u = v = 0.0f;
		}
		
		public Vertex(Vec3 pPos, Vec3 pNorm, float pU, float pV) {
			pos = new Vec3(pPos);
			norm = new Vec3(pNorm);
			u = pU;
			v = pV;
		}
		
		public Vertex(float pX, float pY, float pZ, float pNX, float pNY, float pNZ, float pU, float pV) {
			pos = new Vec3(pX, pY, pZ);
			norm = new Vec3(pNX, pNY, pNZ);
			u = pU;
			v = pV;
		}
	}
	
	// ------------------------------------------------------------
	// Members & constructor

	private ArrayList<Vertex> mVertices;
	private Map<String, ArrayList<Integer>> mIndices;
	private String mCurrentPassID;

	public GeometryBuilder() {
		clear();
	}

	// ------------------------------------------------------------
	// Render passes

	public void setPassID(String pPassID) {
		if (!mIndices.containsKey(pPassID))
			mIndices.put(pPassID, new ArrayList<Integer>());
		mCurrentPassID = pPassID;
	}
	
	public String[] getPassIDs() {
		String[] passIDs = new String[mIndices.size()];
		int i = 0;
		Iterator<String> keys = mIndices.keySet().iterator();
		while (keys.hasNext()) {
			passIDs[i++] = keys.next();
		}
		return passIDs;
	}
	
	public int getPassFirstTriangle(String pPassID)	{
		int firstTriangle = 0;
		Iterator<String> keys = mIndices.keySet().iterator();
		while (keys.hasNext()) {
			String passID = keys.next();
			if (passID.compareTo(pPassID) == 0)
				return firstTriangle;
			firstTriangle += mIndices.get(passID).size() / 3;
		}
		return -1;
	}
	
	public int getPassNumberOfTriangles(String pPassID) {
		return mIndices.get(pPassID).size() / 3;
	}

	// ------------------------------------------------------------
	// Data access
	
	public ArrayList<Float> getPositions() {
		ArrayList<Float> positions = new ArrayList<Float>();
		for (int i = 0 ; i < mVertices.size() ; i++) {
			Vertex vertex = mVertices.get(i);
			positions.add(vertex.pos.x);
			positions.add(vertex.pos.y);
			positions.add(vertex.pos.z);
		}
		return positions;
	}

	public ArrayList<Float> getNormals() {
		ArrayList<Float> normals = new ArrayList<Float>();
		for (int i = 0 ; i < mVertices.size() ; i++) {
			Vertex vertex = mVertices.get(i);
			normals.add(vertex.norm.x);
			normals.add(vertex.norm.y);
			normals.add(vertex.norm.z);
		}
		return normals;
	}
	
	public ArrayList<Float> getTexCoords() {
		ArrayList<Float> texCoords = new ArrayList<Float>();
		for (int i = 0 ; i < mVertices.size() ; i++) {
			Vertex vertex = mVertices.get(i);
			texCoords.add(vertex.u);
			texCoords.add(vertex.v);
		}
		return texCoords;
	}

	public ArrayList<Integer> getIndices() {
		ArrayList<Integer> allIndices = new ArrayList<Integer>();
		Iterator<String> keys = mIndices.keySet().iterator();
		while (keys.hasNext()) {
			String passID = keys.next();
			allIndices.addAll(mIndices.get(passID));
		}
		return allIndices;
	}
	
	// ------------------------------------------------------------
	// Texture mapping
	
	public void planarTexture(Vec3 pAnchor, Vec3 pU, Vec3 pV) {
		Vec3 u = new Vec3(pU.x != 0.0f ? 1.0f / pU.x : 0.0f, pU.y != 0.0f ? 1.0f / pU.y : 0.0f, pU.z != 0.0f ? 1.0f / pU.z : 0.0f);
		Vec3 v = new Vec3(pV.x != 0.0f ? 1.0f / pV.x : 0.0f, pV.y != 0.0f ? 1.0f / pV.y : 0.0f, pV.z != 0.0f ? 1.0f / pV.z : 0.0f);

		for (int i = 0 ; i < mVertices.size() ; i++)
		{
			Vertex vertex = mVertices.get(i);
			Vec3 relPos = vertex.pos.sub(pAnchor);
			vertex.u = relPos.dot(u);
			vertex.v = relPos.dot(v);
		}
	}
	
	public void sphericalTexture(Vec3 pCenter) {
		for (int i = 0 ; i < mVertices.size() ; i++)
		{
			Vertex vertex = mVertices.get(i);
			Vec3 relPos = vertex.pos.sub(pCenter);

			float horizontalDistance = (float) Math.sqrt(relPos.x * relPos.x + relPos.z * relPos.z);
			double yaw = Math.atan2(relPos.x / horizontalDistance, relPos.z / horizontalDistance) - Math.PI;
			double pitch = -Math.atan2(relPos.y, horizontalDistance);

			vertex.u = (float)(yaw / Math.PI);
			vertex.v = (float)(pitch / Math.PI);
		}		
	}
	
	public void scaleTexture(float pFactorU, float pFactorV) {
		for (int i = 0 ; i < mVertices.size() ; i++)
		{
			Vertex vertex = mVertices.get(i);
			vertex.u *= pFactorU;
			vertex.v *= pFactorV;
		}		
	}
	
	public void translateTexture(float pDeltaU, float pDeltaV) {
		for (int i = 0 ; i < mVertices.size() ; i++)
		{
			Vertex vertex = mVertices.get(i);
			vertex.u += pDeltaU;
			vertex.v += pDeltaV;
		}		
	}

	// ------------------------------------------------------------
	// Normal vectors
	
	public void calculateNormals() {
		for (int i = 0 ; i < mVertices.size() ; i++)
		{
			Vertex vertex = mVertices.get(i);
			vertex.norm.clear();
		}
		
		ArrayList<Integer> indices = getIndices();
		int numberOfTriangles = indices.size() / 3;
		int[] index = new int[3];

		for (int i = 0; i < numberOfTriangles; i++) {
			// Get the three vertices of each triangle and calculate its edge vectors
			for (int j = 0; j < 3; j++)
				index[j] = indices.get(i * 3 + j);
			Vec3 edge01 = mVertices.get(index[1]).pos.sub(mVertices.get(index[0]).pos);
			Vec3 edge02 = mVertices.get(index[2]).pos.sub(mVertices.get(index[0]).pos);
			Vec3 edge12 = mVertices.get(index[2]).pos.sub(mVertices.get(index[1]).pos);

			// Calculate each triangles surface area (using Heron's Formula) and normal vector
			float a = edge01.length();
			float b = edge02.length();
			float c = edge12.length();
			float s = (a + b + c) / 2.0f;
			float triangleArea = (float) Math.sqrt(s * (s - a) * (s - b) * (s - c));
			edge01.normalize();
			edge02.normalize();
			Vec3 triangleNormal = edge02.cross(edge01);
			triangleNormal.normalize();

			// Add that normal vector to each of the three vertices normal
			// vectors (multiplied by the surface area as a weighting factor)
			for (int j = 0; j < 3; j++) {
				Vertex vertex = mVertices.get(index[j]);
				vertex.norm.x += triangleNormal.x * triangleArea;
				vertex.norm.y += triangleNormal.y * triangleArea;
				vertex.norm.z += triangleNormal.z * triangleArea;
			}
		}

		// Normalize each vertices normal vector
		for (int i = 0 ; i < mVertices.size() ; i++)
			mVertices.get(i).norm.normalize();
	}
	
	// ------------------------------------------------------------
	// Geometry manipulation

	public void clear() {
		mVertices = new ArrayList<Vertex>();
		mIndices = new HashMap<String, ArrayList<Integer>>();
		setPassID("default");
	}

	public void flip() {
		Iterator<String> keys = mIndices.keySet().iterator();
		while (keys.hasNext()) {
			String passID = keys.next();
			ArrayList<Integer> indices = mIndices.get(passID);

			for (int i = 0; i < indices.size(); i += 3) {
				int temp = indices.get(i);
				indices.set(i, indices.get(i + 2));
				indices.set(i + 2, temp);
			}
		}
	}

	// ------------------------------------------------------------
	// Basic primitives

	public void triangle(float pX1, float pY1, float pZ1, float pX2, float pY2, float pZ2, float pX3, float pY3, float pZ3) {
		int firstIndex = mVertices.size();
		ArrayList<Integer> indices = mIndices.get(mCurrentPassID);
		
		Vertex v1 = new Vertex(pX1, pY1, pZ1, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f);
		Vertex v2 = new Vertex(pX2, pY2, pZ2, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);
		Vertex v3 = new Vertex(pX3, pY3, pZ3, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f);

		Vec3 edge12 = v2.pos.sub(v1.pos).normalize();
		Vec3 edge13 = v3.pos.sub(v1.pos).normalize();
		Vec3 normal = edge13.cross(edge12);
		
		v1.norm.set(normal);
		v2.norm.set(normal);
		v3.norm.set(normal);
		
		mVertices.add(v1); mVertices.add(v2); mVertices.add(v3);
		indices.add(firstIndex + 0); indices.add(firstIndex + 1); indices.add(firstIndex + 2);
	}

	public void quad(float pX1, float pY1, float pZ1, float pX2, float pY2, float pZ2, float pX3, float pY3, float pZ3, float pX4, float pY4, float pZ4) {
		int firstIndex = mVertices.size();
		ArrayList<Integer> indices = mIndices.get(mCurrentPassID);

		// TODO: Verify texture coordinates!
		Vertex v1 = new Vertex(pX1, pY1, pZ1, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f);
		Vertex v2 = new Vertex(pX2, pY2, pZ2, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);
		Vertex v3 = new Vertex(pX3, pY3, pZ3, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f);
		Vertex v4 = new Vertex(pX4, pY4, pZ4, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f);

		Vec3 edge12 = v2.pos.sub(v1.pos).normalize();
		Vec3 edge13 = v3.pos.sub(v1.pos).normalize();
		Vec3 normal = edge13.cross(edge12);

		v1.norm.set(normal);
		v2.norm.set(normal);
		v3.norm.set(normal);
		v4.norm.set(normal);
		
		mVertices.add(v1); mVertices.add(v2); mVertices.add(v3); mVertices.add(v4);
		indices.add(firstIndex + 0); indices.add(firstIndex + 1); indices.add(firstIndex + 2);
		indices.add(firstIndex + 0); indices.add(firstIndex + 2); indices.add(firstIndex + 3);
	}

	// ------------------------------------------------------------
	// Basic shapes

	public void cube(float pCenterX, float pCenterY, float pCenterZ, float pSizeX, float pSizeY, float pSizeZ) {
		float hiX = pCenterX + pSizeX;
		float loX = pCenterX - pSizeX;
		float hiY = pCenterY + pSizeY;
		float loY = pCenterY - pSizeY;
		float hiZ = pCenterZ + pSizeZ;
		float loZ = pCenterZ - pSizeZ;

		quad(loX, loY, loZ, loX, loY, hiZ, hiX, loY, hiZ, hiX, loY, loZ);
		quad(loX, hiY, loZ, hiX, hiY, loZ, hiX, hiY, hiZ, loX, hiY, hiZ);
		quad(loX, loY, loZ, hiX, loY, loZ, hiX, hiY, loZ, loX, hiY, loZ);
		quad(loX, loY, hiZ, loX, hiY, hiZ, hiX, hiY, hiZ, hiX, loY, hiZ);
		quad(loX, loY, loZ, loX, hiY, loZ, loX, hiY, hiZ, loX, loY, hiZ);
		quad(hiX, loY, loZ, hiX, loY, hiZ, hiX, hiY, hiZ, hiX, hiY, loZ);
	}

	public void sphere(Vec3 pCenter, float pRadius, int pTesselation) {
		ArrayList<Vec3> vertices = new ArrayList<Vec3>();
		ArrayList<Integer> tempIndices = new ArrayList<Integer>();

		vertices.add(new Vec3(0, -1, 0));
		vertices.add(new Vec3(-1, 0, 0));
		vertices.add(new Vec3(0, 0, -1));
		vertices.add(new Vec3(+1, 0, 0));
		vertices.add(new Vec3(0, 0, +1));
		vertices.add(new Vec3(0, +1, 0));

		tempIndices.add(2); tempIndices.add(1); tempIndices.add(0);
		tempIndices.add(3); tempIndices.add(2); tempIndices.add(0);
		tempIndices.add(4); tempIndices.add(3); tempIndices.add(0);
		tempIndices.add(1); tempIndices.add(4); tempIndices.add(0);
		tempIndices.add(5); tempIndices.add(1); tempIndices.add(2);
		tempIndices.add(5); tempIndices.add(2); tempIndices.add(3);
		tempIndices.add(5); tempIndices.add(3); tempIndices.add(4);
		tempIndices.add(5); tempIndices.add(4); tempIndices.add(1);

		for (int i = 0; i < pTesselation; i++) {
			ArrayList<Integer> newIndices = new ArrayList<Integer>();
			for (int j = 0; j < tempIndices.size(); j += 3) {
				int indexA = tempIndices.get(j + 0);
	 			int indexB = tempIndices.get(j + 1);
	 			int indexC = tempIndices.get(j + 2);

				int indexAB = newSphereVertexBetween(vertices, indexA, indexB);
				int indexAC = newSphereVertexBetween(vertices, indexA, indexC);
				int indexBC = newSphereVertexBetween(vertices, indexB, indexC);

				newIndices.add( indexA); newIndices.add(indexAB); newIndices.add(indexAC);
				newIndices.add(indexAB); newIndices.add( indexB); newIndices.add(indexBC);
				newIndices.add(indexAC); newIndices.add(indexBC); newIndices.add( indexC);
				newIndices.add(indexAC); newIndices.add(indexAB); newIndices.add(indexBC);
			}
			tempIndices = newIndices;
		}

		int firstIndex = mVertices.size();
		for (int i = 0; i < vertices.size(); i++) {
			Vec3 position = vertices.get(i);
			// TODO: Set proper texture coordinates!
			mVertices.add(new Vertex(pCenter.add(position.mul(pRadius)), position, 0.0f, 0.0f));
		}

		ArrayList<Integer> indices = mIndices.get(mCurrentPassID);
		for (int i = 0; i < tempIndices.size(); i++)
			indices.add(firstIndex + tempIndices.get(i));
	}

	private int newSphereVertexBetween(ArrayList<Vec3> pVertices, int pIndexA, int pIndexB) {
		Vec3 vertexA = pVertices.get(pIndexA);
		Vec3 vertexB = pVertices.get(pIndexB);
		Vec3 center = new Vec3((vertexA.x + vertexB.x) / 2.0f, (vertexA.y + vertexB.y) / 2.0f, (vertexA.z + vertexB.z) / 2.0f);
		center.normalize();

		int numberOfVertices = pVertices.size();
		for (int i = 0; i < numberOfVertices; i++) {
			Vec3 vertex = pVertices.get(i);
			if (vertex.x == center.x && vertex.y == center.y && vertex.z == center.z)
				return i;
		}
		
		int newIndex = pVertices.size();
		pVertices.add(center);
		return newIndex;
	}
}
