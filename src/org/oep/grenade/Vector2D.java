package org.oep.grenade;

public class Vector2D {
	public float x = 0.0f, y = 0.0f;
	
	public Vector2D(float x, float y) {
		this.x = x;
		this.y = y;
	}
	
	public float getLength() {
		return (float) Math.hypot(x,y);
	}
	
	public Vector2D getUnitVector() {
		float l = getLength();
		
		if(l == 0) {
			return new Vector2D(0,0);
		}
		
		return new Vector2D(x / l, y / l);
	}
	
	public Vector2D getNormal() {
		return new Vector2D(-y, x);
	}
	
	public float dot(Vector2D v) {
		return x * v.x + y * v.y;
	}
}
