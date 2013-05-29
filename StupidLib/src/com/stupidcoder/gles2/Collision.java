package com.stupidcoder.gles2;

import java.util.Vector;

public class Collision
{
	private Vec3[] mGeometry;
	private float mSqrDistToTriangle, mBarycentricS, mBarycentricT;

	public Collision(Vector<Float> pVertices)
	{
		float[] vertices = new float[pVertices.size()];
		for (int i = 0 ; i < vertices.length ; i++)
			vertices[i] = pVertices.get(i);
		setGeometry(vertices);
	}
	
	public Collision(float[] pVertices)
	{
		setGeometry(pVertices);
	}
	
	private void setGeometry(float[] pVertices)
	{
		int numberOfTriangles = pVertices.length / 9;
		mGeometry = new Vec3[numberOfTriangles * 6];
		
		for (int i = 0 ; i < numberOfTriangles ; i++)
		{
			// The three vertices
			mGeometry[i*6+0] = new Vec3(pVertices[i*9+0], pVertices[i*9+1], pVertices[i*9+2]);
			mGeometry[i*6+1] = new Vec3(pVertices[i*9+3], pVertices[i*9+4], pVertices[i*9+5]);
			mGeometry[i*6+2] = new Vec3(pVertices[i*9+6], pVertices[i*9+7], pVertices[i*9+8]);

			// The two edge vectors
			mGeometry[i*6+3] = mGeometry[i*6+1].sub(mGeometry[i*6+0]); 
			mGeometry[i*6+4] = mGeometry[i*6+2].sub(mGeometry[i*6+0]);
			
			// The normal vector
			Vec3 edge01 = new Vec3(mGeometry[i*6+3]);
			Vec3 edge02 = new Vec3(mGeometry[i*6+4]);
			edge01.normalize();
			edge02.normalize();
			mGeometry[i*6+5] = edge02.cross(edge01);
			mGeometry[i*6+5].normalize();
		}
	}

	public boolean resolveIntersection(Scene.Node pNode, float pRadius)
	{
		Vec3 position = pNode.getPosition();
		if (resolveIntersection(position, pRadius))
		{
			pNode.setPosition(position);
			return true;
		}
		return false;
	}
	
	public boolean resolveIntersection(Vec3 pPoint, float pRadius)
	{
		boolean moved = false;
		
		for (int i = 0 ; i < mGeometry.length ; i += 6)
		{
			float distToPlane = distanceFromPointToPlane(i, pPoint);
			float penetration = pRadius - distToPlane;
	        if (penetration <= 0.0f)
	        	continue;
	        
			closestPointOnTriangle(i, pPoint);
			if (mSqrDistToTriangle > pRadius * pRadius)
				continue;

			Vec3 normal = mGeometry[i+5]; 
			pPoint.x += normal.x * penetration;
			pPoint.y += normal.y * penetration;
			pPoint.z += normal.z * penetration;
			
			moved = true;
		}
		
		return moved;
	}
	
	private float distanceFromPointToPlane(int pIndex, Vec3 pPoint)
	{
		return (mGeometry[pIndex+5].x*pPoint.x + mGeometry[pIndex+5].y*pPoint.y + mGeometry[pIndex+5].z*pPoint.z + (-mGeometry[pIndex+5].x*mGeometry[pIndex+0].x - mGeometry[pIndex+5].y*mGeometry[pIndex+0].y - mGeometry[pIndex+5].z*mGeometry[pIndex+0].z));
	}

	private void closestPointOnTriangle(int pIndex, Vec3 pPoint)
	{
	    Vec3 diff = mGeometry[pIndex+0].sub(pPoint);
	    Vec3 edge0 = mGeometry[pIndex+3];
	    Vec3 edge1 = mGeometry[pIndex+4];
	    float a00 = edge0.dot(edge0);
	    float a01 = edge0.dot(edge1);
	    float a11 = edge1.dot(edge1);
	    float b0 = diff.dot(edge0);
	    float b1 = diff.dot(edge1);
	    float c = diff.dot(diff);
	    float det = Math.abs(a00*a11 - a01*a01);
	    mBarycentricS = a01*b1 - a11*b0;
	    mBarycentricT = a01*b0 - a00*b1;

	    if (mBarycentricS + mBarycentricT <= det)
	    {
	        if (mBarycentricS < 0.0f)
	        {
	            if (mBarycentricT < 0.0f)  // region 4
	            {
	                if (b0 < 0.0f)
	                {
	                    mBarycentricT = 0.0f;
	                    if (-b0 >= a00)
	                    {
	                        mBarycentricS = 1.0f;
	                        mSqrDistToTriangle = a00 + 2.0f * b0 + c;
	                    }
	                    else
	                    {
	                        mBarycentricS = -b0/a00;
	                        mSqrDistToTriangle = b0*mBarycentricS + c;
	                    }
	                }
	                else
	                {
	                    mBarycentricS = 0.0f;
	                    if (b1 >= 0.0f)
	                    {
	                        mBarycentricT = 0.0f;
	                        mSqrDistToTriangle = c;
	                    }
	                    else if (-b1 >= a11)
	                    {
	                        mBarycentricT = 1.0f;
	                        mSqrDistToTriangle = a11 + 2.0f*b1 + c;
	                    }
	                    else
	                    {
	                        mBarycentricT = -b1/a11;
	                        mSqrDistToTriangle = b1*mBarycentricT + c;
	                    }
	                }
	            }
	            else  // region 3
	            {
	                mBarycentricS = 0.0f;
	                if (b1 >= 0.0f)
	                {
	                    mBarycentricT = 0.0f;
	                    mSqrDistToTriangle = c;
	                }
	                else if (-b1 >= a11)
	                {
	                    mBarycentricT = 1.0f;
	                    mSqrDistToTriangle = a11 + 2.0f*b1 + c;
	                }
	                else
	                {
	                    mBarycentricT = -b1/a11;
	                    mSqrDistToTriangle = b1*mBarycentricT + c;
	                }
	            }
	        }
	        else if (mBarycentricT < 0.0f)  // region 5
	        {
	            mBarycentricT = 0.0f;
	            if (b0 >= 0.0f)
	            {
	                mBarycentricS = 0.0f;
	                mSqrDistToTriangle = c;
	            }
	            else if (-b0 >= a00)
	            {
	                mBarycentricS = 1.0f;
	                mSqrDistToTriangle = a00 + 2.0f*b0 + c;
	            }
	            else
	            {
	                mBarycentricS = -b0/a00;
	                mSqrDistToTriangle = b0*mBarycentricS + c;
	            }
	        }
	        else  // region 0
	        {
	            // minimum at interior point
	            float invDet = 1.0f/det;
	            mBarycentricS *= invDet;
	            mBarycentricT *= invDet;
	            mSqrDistToTriangle = mBarycentricS*(a00*mBarycentricS + a01*mBarycentricT + 2.0f*b0) +
	                mBarycentricT*(a01*mBarycentricS + a11*mBarycentricT + 2.0f*b1) + c;
	        }
	    }
	    else
	    {
	        float tmp0, tmp1, numer, denom;

	        if (mBarycentricS < 0.0f)  // region 2
	        {
	            tmp0 = a01 + b0;
	            tmp1 = a11 + b1;
	            if (tmp1 > tmp0)
	            {
	                numer = tmp1 - tmp0;
	                denom = a00 - 2.0f*a01 + a11;
	                if (numer >= denom)
	                {
	                    mBarycentricS = 1.0f;
	                    mBarycentricT = 0.0f;
	                    mSqrDistToTriangle = a00 + 2.0f*b0 + c;
	                }
	                else
	                {
	                    mBarycentricS = numer/denom;
	                    mBarycentricT = 1.0f - mBarycentricS;
	                    mSqrDistToTriangle = mBarycentricS*(a00*mBarycentricS + a01*mBarycentricT + 2.0f*b0) +
	                        mBarycentricT*(a01*mBarycentricS + a11*mBarycentricT + 2.0f*b1) + c;
	                }
	            }
	            else
	            {
	                mBarycentricS = 0.0f;
	                if (tmp1 <= 0.0f)
	                {
	                    mBarycentricT = 1.0f;
	                    mSqrDistToTriangle = a11 + 2.0f*b1 + c;
	                }
	                else if (b1 >= 0.0f)
	                {
	                    mBarycentricT = 0.0f;
	                    mSqrDistToTriangle = c;
	                }
	                else
	                {
	                    mBarycentricT = -b1/a11;
	                    mSqrDistToTriangle = b1*mBarycentricT + c;
	                }
	            }
	        }
	        else if (mBarycentricT < 0.0f)  // region 6
	        {
	            tmp0 = a01 + b1;
	            tmp1 = a00 + b0;
	            if (tmp1 > tmp0)
	            {
	                numer = tmp1 - tmp0;
	                denom = a00 - 2.0f*a01 + a11;
	                if (numer >= denom)
	                {
	                    mBarycentricT = 1.0f;
	                    mBarycentricS = 0.0f;
	                    mSqrDistToTriangle = a11 + 2.0f*b1 + c;
	                }
	                else
	                {
	                    mBarycentricT = numer/denom;
	                    mBarycentricS = 1.0f - mBarycentricT;
	                    mSqrDistToTriangle = mBarycentricS*(a00*mBarycentricS + a01*mBarycentricT + 2.0f*b0) +
	                        mBarycentricT*(a01*mBarycentricS + a11*mBarycentricT + 2.0f*b1) + c;
	                }
	            }
	            else
	            {
	                mBarycentricT = 0.0f;
	                if (tmp1 <= 0.0f)
	                {
	                    mBarycentricS = 1.0f;
	                    mSqrDistToTriangle = a00 + 2.0f*b0 + c;
	                }
	                else if (b0 >= 0.0f)
	                {
	                    mBarycentricS = 0.0f;
	                    mSqrDistToTriangle = c;
	                }
	                else
	                {
	                    mBarycentricS = -b0/a00;
	                    mSqrDistToTriangle = b0*mBarycentricS + c;
	                }
	            }
	        }
	        else  // region 1
	        {
	            numer = a11 + b1 - a01 - b0;
	            if (numer <= 0.0f)
	            {
	                mBarycentricS = 0.0f;
	                mBarycentricT = 1.0f;
	                mSqrDistToTriangle = a11 + 2.0f*b1 + c;
	            }
	            else
	            {
	                denom = a00 - 2.0f*a01 + a11;
	                if (numer >= denom)
	                {
	                    mBarycentricS = 1.0f;
	                    mBarycentricT = 0.0f;
	                    mSqrDistToTriangle = a00 + 2.0f*b0 + c;
	                }
	                else
	                {
	                    mBarycentricS = numer/denom;
	                    mBarycentricT = 1.0f - mBarycentricS;
	                    mSqrDistToTriangle = mBarycentricS*(a00*mBarycentricS + a01*mBarycentricT + 2.0f*b0) +
	                        mBarycentricT*(a01*mBarycentricS + a11*mBarycentricT + 2.0f*b1) + c;
	                }
	            }
	        }
	    }

	    if (mSqrDistToTriangle < 0.0f)
	        mSqrDistToTriangle = 0.0f;

//	    return new Vector3(mGeometry[pIndex+0].x + edge0.x * s + edge1.x * t, mGeometry[pIndex+0].y + edge0.y * s + edge1.y * t, mGeometry[pIndex+0].z + edge0.z * s + edge1.z * t);
	}
}
