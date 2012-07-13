package org.oep.grenade;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

public class RigidBody {
	protected Drawable mDrawable;
	
	protected PointF mPosition = new PointF();
	
	protected Vector2D mVelocity = new Vector2D(0,0);
	
	private float mGravity;
	
	protected boolean mPhysics = true;
	private boolean mTimedPhysics = false;
	private int mPhysicsTimer = 0;

	private float mElasticity = 1;

	private float mFriction = 1;
	
	public RigidBody() { }
	
	public void bounce(Terrain terrain, int x, int y) {
		int slopeX, slopeY;
		// A note: these differences are swapped out of order because
		// the Cartesian plane we are using is mirrored across the x-axis.
		if(x == 0){
			if(y >= terrain.absAt(x)) {
				slopeY = terrain.at(x) - terrain.at(x+1); slopeX = 1;
			}
			else {
				slopeY = 1; slopeX = 0;
			}
		}
		else if(x == terrain.getWidth() - 1) {
			if(y >= terrain.absAt(x)) {
				slopeY = terrain.at(x-1) - terrain.at(x); slopeX = 1;
			}
			else {
				slopeY = 1; slopeX = 0;
			}
		}
		else {
			slopeY = terrain.at(x-1) - terrain.at(x+1); slopeX = 2;
		}

		bounce(slopeX, slopeY);
	}
	
	public void bounce(double tx, double ty) {
		if(!mPhysics) return;
		
		double magnitude = Math.hypot(tx, ty);
		
		// Compute the tangent vector
		double utx = tx / magnitude;
		double uty = ty / magnitude;
		
		// Derive the normal vector
		double unx = uty;
		double uny = -utx;
		
		// Find the normal and tangential velocities (of the rigid body only)
		// A note: mVY is negatived
		double nv = unx * mVelocity.x + uny * mVelocity.y;
		double tv = utx * mVelocity.x + uty * mVelocity.y;
		
		// The velocities are manipulated
		nv = mElasticity * -nv;
		tv = mFriction * tv;
		
		// Convert the normal and tangential velocities into vectors
		double nvx = nv * unx;
		double nvy = nv * uny;
		double tvx = tv * utx;
		double tvy = tv * uty;
		
		// Assign the new velocity vector
		mVelocity.x = (float) (nvx + tvx);
		mVelocity.y = (float) (nvy + tvy);
	}
	
	public void setDrawable(Drawable dr) {
		mDrawable = dr;
	}
	
	public void draw(Canvas canvas) {
		draw(canvas, mPosition.x, mPosition.y);
	}
	
	public void draw(Canvas canvas, float x, float y) {
		if(mDrawable == null ) return;
		int w = mDrawable.getIntrinsicWidth(), h = mDrawable.getIntrinsicHeight();
		mDrawable.setBounds((int) (x - w / 2), (int) (y - h), (int) (x + w / 2), (int) y );
		mDrawable.draw(canvas);
	}

	public void setPosition(float x, float y) {
		mPosition.x = x;
		mPosition.y = y;
	}
	
	public void setVelocity(float vx, float vy) {
		mVelocity.x = vx;
		mVelocity.y = vy;
	}
	
	public void setGravity(float gravity) {
		mGravity = gravity;
	}
	
	public float getY() { return mPosition.y; }
	public float getX() { return mPosition.x; }
	public PointF getPoint() { return new PointF(mPosition.x, mPosition.y); }
	
	public String toString() {
		return "RigidBody: (" + mPosition.x + ", " + mPosition.y + ")";
	}
	
	public void move(long ms) {
		float dt = ms / 1000f;
		
		mPosition.x += dt * mVelocity.x;
		mPosition.y += dt * mVelocity.y;
		
		mVelocity.y += mGravity;
	}
	
	public float getVX() {
		return mVelocity.x;
	}
	
	public void setPhysics(boolean b) {
		mPhysics = b;
	}
	
	public void setPhysicsTimer(int time) {
		mPhysicsTimer = time;
	}
	
	public boolean getPhysics() {
		return mPhysics;
	}
	
	public int getPhysicsTimer() {
		return mPhysicsTimer;
	}

	public void setX(float x) {
		mPosition.x = x;
	}
	
	public void tickPhysics(int dt) {
		mPhysicsTimer = Math.max(0, mPhysicsTimer + dt);
	}

	public double distance(RigidBody body) {
		float dx = body.getX() - mPosition.x, dy = body.getY() - mPosition.y;
		return Math.sqrt( dx * dx + dy * dy );
	}

	public void setElasticity(float f) {
		mElasticity = Math.max(0, Math.min(1, f));
	}
	
	public void setFriction(float f) {
		mFriction = Math.max(0, Math.min(1, f));
	}
	
	public float getElasticity() {
		return mElasticity;
	}
	
	public float getFriction() {
		return mFriction;
	}

	public float getVY() {
		return mVelocity.y;
	}

	public boolean contains(float x, float y) {
		return mDrawable.getBounds().contains((int)x,(int)y);
	}
	
	public Vector2D getVelocity() {
		return mVelocity;
	}
	
	public Rect getBounds() {
		return mDrawable.getBounds();
	}
}
