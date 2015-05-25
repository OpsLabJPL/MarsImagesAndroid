package rajawali;

import rajawali.bounds.BoundingBox;
import rajawali.math.Number3D;
import rajawali.math.Plane;
import rajawali.math.Plane.PlaneSide;
import rajawali.primitives.Sphere;

public class Frustum {
	private Number3D[] mTmp = new Number3D[8];
	protected Sphere mVisualSphere;
	protected float[] mTmpMatrix = new float[16];
	protected static final Number3D[] mClipSpacePlanePoints = { 
		new Number3D(-1, -1, -1), 
		new Number3D( 1, -1, -1), 
		new Number3D( 1,  1, -1), 
		new Number3D(-1,  1, -1), 
		new Number3D(-1, -1,  1), 
		new Number3D( 1, -1,  1), 
		new Number3D( 1,  1,  1),
		new Number3D(-1,  1,  1)}; 

	public final Plane[] planes = new Plane[6];     

	protected final Number3D[] planePoints = { new Number3D(), new Number3D(), new Number3D(), new Number3D(), 
			new Number3D(), new Number3D(), new Number3D(), new Number3D() 
	};      

	public Frustum() {
		for(int i = 0; i < 6; i++) {
			planes[i] = new Plane(new Number3D(), 0);
		}
		for(int i=0;i<8;i++){
			mTmp[i]=new Number3D();
		}
	}

	public void update(float[] m) {
		planes[0].setAll(m[12] - m[0], m[13] - m[1], m[14] - m[2], m[15] - m[3]);
		planes[1].setAll(m[12] + m[0], m[13] + m[1], m[14] + m[2], m[15] + m[3]);
		planes[2].setAll(m[12] + m[4], m[13] + m[5], m[14] + m[6], m[15] + m[7]);
		planes[3].setAll(m[12] - m[4], m[13] - m[5], m[14] - m[6], m[15] - m[7]);
		planes[4].setAll(m[12] - m[8], m[13] - m[9], m[14] - m[10], m[15] - m[11]);
		planes[5].setAll(m[12] + m[8], m[13] + m[9], m[14] + m[10], m[15] + m[11]);

//		planes[0].normalize();
//		planes[1].normalize();
//		planes[2].normalize();
//		planes[3].normalize();
//		planes[4].normalize();
//		planes[5].normalize();
	}       


	public boolean sphereInFrustum (Number3D center, float radius) {
		for (int i = 0; i < planes.length; i++)
			if (planes[i].distance(center) < -radius) return false;

		return true;
	}

	Number3D mPoint1 = new Number3D(), mPoint2 = new Number3D();

	public boolean boundsInFrustum (BoundingBox bounds) {
		for(int i=0; i<6; i++) {
			Plane p = planes[i];
			mPoint1.x = p.getNormal().x > 0 ? bounds.getMin().x : bounds.getMax().x;
			mPoint2.x = p.getNormal().x > 0 ? bounds.getMax().x : bounds.getMin().x;
			mPoint1.y = p.getNormal().y > 0 ? bounds.getMin().y : bounds.getMax().y;
			mPoint2.y = p.getNormal().y > 0 ? bounds.getMax().y : bounds.getMin().y;
			mPoint1.z = p.getNormal().z > 0 ? bounds.getMin().z : bounds.getMax().z;
			mPoint2.z = p.getNormal().z > 0 ? bounds.getMax().z : bounds.getMin().z;

			double distance1 = p.distance(mPoint1);
			double distance2 = p.distance(mPoint2);

			if ( distance1 < 0 && distance2 < 0 )
				return false;
		}

		return true;
	}

	public boolean pointInFrustum (Number3D point) {
		for (int i = 0; i < planes.length; i++) {
			PlaneSide result = planes[i].getPointSide(point);
			if (result == PlaneSide.Back) {return false;}
		}
		return true;
	}
}