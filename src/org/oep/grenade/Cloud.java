package org.oep.grenade;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

public class Cloud {
	private int mAlpha;
	private int mX, mY;
	
	public Cloud(int x, int y) {
		mAlpha = 255;
		mX = x;
		mY = y;
	}
	
	public void update() {
		mAlpha = Math.max(0, mAlpha - 10);
	}
	
	public void offset(int dx, int dy) {
		mX += dx;
		mY += dy;
	}
	
	public int getAlpha() { 
		return mAlpha;
	}

	public void draw(Canvas canvas, Drawable drawable) {
		int w = drawable.getIntrinsicWidth();
		int h = drawable.getIntrinsicHeight();
		
		Rect r = new Rect(mX - w / 2, mY - h / 2, mX + w / 2, mY + h / 2);
		
		drawable.setBounds(r);
		drawable.setAlpha(mAlpha);
		drawable.draw(canvas);
	}
}
