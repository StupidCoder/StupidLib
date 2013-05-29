/**
 * A 3D vector math class for Android
 *
 * @author Dennis Harms
 * @version 1.0
 */

package com.stupidcoder.gles2;

public class Vec3 {
	public float x, y, z;

	/**
	 * Creates a new empty vector.
	 */
	public Vec3() {
		x = y = z = 0.0f;
	}
	
	/**
	 * Creates a new vector from a yaw and a pitch angle.
	 * 
	 * @param pYaw
	 *            The yaw angle in degrees.
	 * @param pPitch
	 *            The pitch angle in degrees.
	 */
	public Vec3(float pYaw, float pPitch) {
		double yaw = pYaw / 180.0 * Math.PI;
		double pitch = pPitch / 180.0 * Math.PI;
		x = (float) (Math.sin(yaw) * Math.cos(pitch));
		y = (float) (Math.sin(pitch));
		z = (float) (Math.cos(yaw) * Math.cos(pitch));
	}

	/**
	 * Creates a new vector with the specified values.
	 * 
	 * @param pX
	 *            The X value for the new vector.
	 * @param pY
	 *            The Y value for the new vector.
	 * @param pZ
	 *            The Z value for the new vector.
	 */
	public Vec3(float pX, float pY, float pZ) {
		x = pX;
		y = pY;
		z = pZ;
	}

	/**
	 * Creates a new vector with the specified values.
	 * 
	 * @param pValues
	 *            The values for the new vector.
	 */
	public Vec3(float[] pValues) {
		x = pValues[0];
		y = pValues[1];
		z = pValues[2];
	}

	/**
	 * Creates a copy of the specified vector.
	 * 
	 * @param pVector
	 *            The vector to copy.
	 */
	public Vec3(Vec3 pVector) {
		x = pVector.x;
		y = pVector.y;
		z = pVector.z;
	}
	
	public void clear() {
		x = y = z = 0.0f;
	}
	
	public void set(Vec3 pVector) {
		x = pVector.x;
		y = pVector.y;
		z = pVector.z;
	}

	public void set(float pX, float pY, float pZ) {
		x = pX;
		y = pY;
		z = pZ;
	}

	/**
	 * Calculates the cross product from this and the specified vector,
	 * returning the result as a new vector.
	 * 
	 * @param pVector
	 *            The vector to calculate the cross product with.
	 * @return A new vector containing the cross product.
	 */
	public Vec3 cross(Vec3 pVector) {
		Vec3 result = new Vec3();
		result.x = y * pVector.z - z * pVector.y;
		result.y = z * pVector.x - x * pVector.z;
		result.z = x * pVector.y - y * pVector.x;
		return result;
	}

	/**
	 * Calculates the dot product from this and the specified vector.
	 * 
	 * @param pVector
	 *            The vector to calculate the dot product with.
	 * @return The dot product of the two vectors.
	 */
	public float dot(Vec3 pVector) {
		return x * pVector.x + y * pVector.y + z * pVector.z;
	}

	/**
	 * Calculates the length of the vector.
	 * 
	 * @return The length of the vector.
	 */
	public float length() {
		return (float) Math.sqrt(dot(this));
	}

	/**
	 * Scales the vector to unit length.
	 */
	public Vec3 normalize() {
		float length = (float) Math.sqrt(dot(this));
		if (length == 0.0f)
			return this;
		scale(1.0f / length);
		return this;
	}

	/**
	 * Scales the vector by the specified factor.
	 * 
	 * @param pFactor
	 *            The scaling factor.
	 */
	public void scale(float pFactor) {
		x *= pFactor;
		y *= pFactor;
		z *= pFactor;
	}

	/**
	 * Subtracts the specified vector from this vector, returning the result as
	 * a new vector.
	 * 
	 * @param pVector
	 *            The vector to subtract from this vector.
	 * @return A new vector containing the result.
	 */
	public Vec3 sub(Vec3 pVector) {
		return new Vec3(x - pVector.x, y - pVector.y, z - pVector.z);
	}

	public Vec3 add(Vec3 pVector) {
		return new Vec3(x + pVector.x, y + pVector.y, z + pVector.z);
	}

	public Vec3 add(float pX, float pY, float pZ) {
		return new Vec3(x + pX, y + pY, z + pZ);
	}

	public Vec3 mul(float pFactor) {
		return new Vec3(x * pFactor, y * pFactor, z * pFactor);
	}
}
