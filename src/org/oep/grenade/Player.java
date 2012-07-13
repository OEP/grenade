package org.oep.grenade;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Paint.Style;

public class Player extends RigidBody {
	public static final int MAX_HEALTH = 100;

	public static final int GIBLET_CHUNKS = 12;
	
	private int mHealth = MAX_HEALTH;
	private int mAnimationHealth = MAX_HEALTH;

	private boolean mDead = false;
	
	public void draw(Canvas canvas) {
		if(mDead) return;
		
		super.draw(canvas);
		
		if(!mPhysics) {
			Paint p = new Paint();
			p.setStrokeWidth(4);
			
			int r = (mAnimationHealth == 0 || MAX_HEALTH / mAnimationHealth < 2)
				? 255 * ( (MAX_HEALTH / 2) - mAnimationHealth ) / (MAX_HEALTH / 2)
				: 255;
			
			int g = (mAnimationHealth == 0 || MAX_HEALTH / mAnimationHealth >= 2) ? 255 * ( mAnimationHealth ) / (MAX_HEALTH / 2) :
				255;
			
			
			float arcSweep = 360f * mAnimationHealth / MAX_HEALTH;
			p.setColor(Color.argb(0xFF, r, g, 0));
			p.setStyle(Style.STROKE);
			
			Rect rect = mDrawable.getBounds();
			int biggest = Math.max(rect.width(), rect.height());
			
			RectF oval = new RectF(mPosition.x - biggest / 2, mPosition.y - mDrawable.getIntrinsicHeight() / 2 - biggest / 2,
					mPosition.x + biggest / 2, mPosition.y - mDrawable.getIntrinsicHeight() / 2 + biggest / 2); 
			
			canvas.drawArc(oval, 0f, arcSweep, false, p);
		}
	}
	
	public void takeDamage(int damage) {
		mHealth = Math.max(0, mHealth - Math.abs(damage)); 
	}
	
	public void nextFrame() {
		mAnimationHealth = mHealth + (mAnimationHealth - mHealth) * 99 / 100;
	}

	public int getHealth() {
		return mHealth;
	}

	public void setDead(boolean b) {
		mDead = b;
	}
	
	public boolean isDead() {
		return mDead;
	}
}
