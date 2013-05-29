/**
 * A camera class for use in Android OpenGL applications.
 *
 * @author Dennis Harms
 * @version 1.0
 */

package com.stupidcoder.gles2;

import android.opengl.Matrix;

public class Camera {
	private float mFOV, mAspect;
	private Vec3 mFront, mRight, mUp;
	private float mNear, mFar;
	private Vec3 mPosition, mTarget;

	private float[] mProjection;
	private boolean mRebuildProjection;
	private float[] mView;
	private float mYaw, mPitch, mDistance;

	/**
	 * Creates a new camera with an identity view matrix.
	 */
	public Camera() {
		mFOV = 90.0f;
		mAspect = 1.0f;
		mNear = 0.1f;
		mFar = 300.0f;
		mRebuildProjection = true;
		mProjection = new float[16];

		mPosition = new Vec3();
		mTarget = new Vec3(0.0f, 0.0f, 0.0f);
		mYaw = mPitch = 0.0f;
		mDistance = 10.0f;
		mView = new float[16];
		positionFromTargetOrbit();
		directionsFromTargetOrbit();
	}

	private void directionsFromTargetOrbit() {
		mFront = mTarget.sub(mPosition);
		mFront.normalize();
		mRight = mFront.cross(new Vec3(0.0f, 1.0f, 0.0f));
		mRight.normalize();
		mUp = mFront.cross(mRight);
		mUp.normalize();
	}

	/**
	 * Gets the projection matrix, recalculating it if any parameters have
	 * changed since the last call.
	 * 
	 * @return The projection matrix as an array of 16 float values.
	 */
	public float[] getProjection() {
		if (mRebuildProjection) {
			float height = (float) Math.tan(mFOV / 360.0f * (float) Math.PI)
					* mNear;
			float width = height * mAspect;
			Matrix.frustumM(mProjection, 0, -width, width, height, -height,
					mNear, mFar);
			mRebuildProjection = false;
		}
		return mProjection;
	}

	/**
	 * Gets the view matrix.
	 * 
	 * @return The view matrix as an array of 16 float values.
	 */
	public float[] getView() {
		return mView;
	}
	
	public void updateCamera() {
		Matrix.setLookAtM(mView, 0, mPosition.x, mPosition.y, mPosition.z,
				mTarget.x, mTarget.y, mTarget.z, 0.0f, 1.0f, 0.0f);
	}

	/**
	 * Gets the current view direction vector of the camera.
	 * 
	 * @return The current view direction of the camera.
	 */
	public Vec3 getDirection() {
		return mFront;
	}

	/**
	 * Rotates the camera around it's target point by the specified angles and
	 * moves it towards it by the specified distance.
	 * 
	 * @param pYaw
	 *            The relative yaw angle in degrees.
	 * @param pPitch
	 *            The relative pitch angle in degrees.
	 * @param pDistance
	 *            The relative distance.
	 */
	public void orbit(float pYaw, float pPitch, float pDistance) {
		mYaw = (mYaw + pYaw) % 360.0f;
		mPitch = Math.min(89.0f, Math.max(-89.0f, mPitch + pPitch));
		mDistance = Math.max(1.0f, mDistance + pDistance);
		positionFromTargetOrbit();
		directionsFromTargetOrbit();
	}

	public void rotate(float pYaw, float pPitch) {
		mTarget = new Vec3((mYaw + pYaw) % 360.0f, Math.min(89.0f, Math.max(-89.0f, mPitch + pPitch)));
		mTarget.scale(-mDistance);
		mTarget = mTarget.add(mPosition);
		orbitFromPositionTarget();
		directionsFromTargetOrbit();
	}
	
	private void orbitFromPositionTarget() {
		Vec3 direction = mTarget.sub(mPosition);
		mDistance = direction.length();
		direction.scale(1.0f / mDistance);

		float horizontalDistance = (float) Math.sqrt(direction.x * direction.x + direction.z * direction.z);
		double yaw = Math.atan2(direction.x / horizontalDistance, direction.z / horizontalDistance) - Math.PI;
		double pitch = -Math.atan2(direction.y, horizontalDistance);

		mYaw = (float) (yaw / Math.PI * 180.0);
		mPitch = (float) (pitch / Math.PI * 180.0);
	}

	public void pan(float pX, float pY, float pZ) {
		mTarget.x += mRight.x * pX + mUp.x * pY + mFront.x * pZ;
		mTarget.y += mRight.y * pX + mUp.y * pY + mFront.y * pZ;
		mTarget.z += mRight.z * pX + mUp.z * pY + mFront.z * pZ;
		positionFromTargetOrbit();
		directionsFromTargetOrbit();
	}
	
	public void setTargetY(float pY) {
		mTarget.y = pY;
		positionFromTargetOrbit();
	}
	
	public Vec3 getPosition() {
		return mPosition;
	}

	public void panWorld(float pX, float pY, float pZ) {
		mTarget.x += pX;
		mTarget.y += pY;
		mTarget.z += pZ;
		positionFromTargetOrbit();
	}

	private void positionFromTargetOrbit() {
		double yaw = mYaw / 180.0 * Math.PI;
		double pitch = mPitch / 180.0 * Math.PI;
		mPosition.x = (float) (mTarget.x + Math.sin(yaw) * Math.cos(pitch)
				* mDistance);
		mPosition.y = (float) (mTarget.y + Math.sin(pitch) * mDistance);
		mPosition.z = (float) (mTarget.z + Math.cos(yaw) * Math.cos(pitch)
				* mDistance);
	}

	/**
	 * Sets the aspect ratio.
	 * 
	 * @param pAspect
	 *            The new aspect ratio (width/height.
	 */
	public void setAspect(float pAspect) {
		mAspect = pAspect;
		mRebuildProjection = true;
	}

	/**
	 * Sets near and far clip distances
	 * 
	 * @param pNear
	 *            The distance of the near clip plane.
	 * @param pFar
	 *            The distance of the far clip plane.
	 */
	public void setClipDistances(float pNear, float pFar) {
		if (pNear >= pFar)
			return;
		mNear = pNear;
		mFar = pFar;
		mRebuildProjection = true;
	}

	/**
	 * Sets the vertical field of view angle
	 * 
	 * @param pFOV
	 *            The new vertical field of view angle in degrees.
	 */
	public void setFieldOfView(float pFOV) {
		mFOV = Math.min(170.0f, Math.max(10.0f, pFOV));
		mRebuildProjection = true;
	}

	/**
	 * Positions the camera at the given location, letting it look at the given
	 * target point.
	 * 
	 * @param pEyeX
	 *            The X component of the camera position.
	 * @param pEyeY
	 *            The Y component of the camera position.
	 * @param pEyeZ
	 *            The Z component of the camera position.
	 * @param pCenterX
	 *            The X component of the camera target point.
	 * @param pCenterY
	 *            The Y component of the camera target point.
	 * @param pCenterZ
	 *            The Z component of the camera target point.
	 */
	public void setLookAt(float pEyeX, float pEyeY, float pEyeZ,
			float pCenterX, float pCenterY, float pCenterZ) {
		mPosition.x = pEyeX;
		mPosition.y = pEyeY;
		mPosition.z = pEyeZ;
		mTarget.x = pCenterX;
		mTarget.y = pCenterY;
		mTarget.z = pCenterZ;
		orbitFromPositionTarget();
		directionsFromTargetOrbit();
	}
}
