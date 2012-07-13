package org.oep.grenade;

import java.util.ArrayList;
import java.util.Random;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;

public class Terrain {
	public static final int MAX_SLOPE = 5;
	
	private Random RNG = new Random();
	
	private int[] mTerrain;
	private int mMinimum, mMaximum;
	private int mBase;
	private Bitmap mCachedImage;

	private boolean mDirty;
	
	public Terrain(int width, int maxHeight, int minHeight, int base) {
		mTerrain = new int[width];
		mBase = base;
		
		if(mTerrain == null) return;
		
		// Really just a soft boundary for when slopeChange is forced into being -1
		// viewHeight is the hard boundary for height
		int slopeChange = -1 + RNG.nextInt(3);
		int slope = -MAX_SLOPE + RNG.nextInt(2 * MAX_SLOPE + 1);
		int height = minHeight + RNG.nextInt(maxHeight - minHeight);
		mMinimum = mMaximum = height;
		
		for(int i = 0; i < mTerrain.length; i++) {
			mTerrain[i] = height;
			
			height = Math.max(0, height + slope);
			slope = Math.max(-MAX_SLOPE, Math.min(MAX_SLOPE, slope + slopeChange));
			
			mMinimum = Math.min(mMinimum, height);
			mMaximum = Math.max(mMaximum, height);
			
			if(height >= maxHeight) {
				slopeChange = -1;
			}
			else if(height <= minHeight) {
				slopeChange = 1;
			}
			else
				slopeChange = -1 + RNG.nextInt(3);
		}
	}
	
	/**
	 * Given a height in our local orientation, return a set of points
	 * where intersections exist in graphics orientation.
	 * @param y
	 * @return
	 */
	public ArrayList<Point> getIntersections(int y) {
		// We assume y is passed in as our orientation
		
		ArrayList<Point> points = new ArrayList<Point>();
		if(y < mMinimum || y > mMaximum) return points;
		
		
		boolean previous = y < mTerrain[0];
		for(int i = 1; i < mTerrain.length; i++) {
			boolean b = y < mTerrain[i];
			
			if(previous != b)
				points.add(new Point(i, mBase - y));
			previous = b;
		}
		
		return points;
	}
	

	public Point getWarpPoint(PointF p) {
		return getWarpPoint(new Point((int) p.x, (int) p.y));
	}
	
	/**
	 * Find the nearest warp point from a given (presumably illegal) position
	 * @param p
	 * @return
	 */
	public Point getWarpPoint(Point p) {
		int searchY = Math.max(mMinimum, Math.min(mMaximum, mBase - p.y));
		ArrayList<Point> points = getIntersections(searchY);
		
		int boundedX = Math.max(0, Math.min(mTerrain.length - 1, p.x));
		points.add(new Point(boundedX, Math.min(p.y, mBase - mTerrain[boundedX])));
				
		Point selection = points.get(0);
		double distance = Math.hypot(p.x - selection.x, p.y - selection.y);
		
		for(int i = 0; i < points.size(); i++) {
			Point q = points.get(i);
			double d = Math.hypot(q.x - p.x, q.y - p.y);
			
			if(d < distance) {
				distance = d;
				selection = q;
			}
		}
		
		return selection;
	}
	
	public int getMinimum() {
		return mMinimum;
	}
	
	public boolean isIllegal(PointF p) {
		return isIllegal((int)p.x, (int)p.y);
	}
	
	public boolean isIllegal(Point p) {
		return isIllegal(p.x,p.y);
	}
	
	public boolean isIllegal(float x, float y) {
		return isIllegal((int)x,(int)y);
	}
	
	public boolean isIllegal(int x, int y) {
		return x < 0 || x >= mTerrain.length || y < mTerrain[x];
	}

	public int at(int x) {
		if(x < 0 || x >= mTerrain.length) throw new IllegalArgumentException("Bad index passed");
		return mTerrain[x];
	}
	
	public int getWidth() {
		return mTerrain.length;
	}

	public void offset(int i, int dh) {
		if(i < 0 || i >= mTerrain.length) throw new IllegalArgumentException("Bad index passed");
		mTerrain[i] = Math.max(0, mTerrain[i] + dh);
		mDirty = true;
	}
	
	private boolean recache() {
		if(mTerrain == null) return false;

		Bitmap bm = Bitmap.createBitmap(mTerrain.length, mMaximum, Bitmap.Config.ARGB_8888);
			
		Canvas c = new Canvas(bm);
		Paint p = new Paint();
		
		for(int i = 0; i < bm.getWidth(); i++) {
			c.drawLine(i, bm.getHeight(), i, bm.getHeight() - mTerrain[i], p);
		}

		mCachedImage = bm;
		mDirty = false;
		return true;
	}

	private void recalculate() {
		mMinimum = mMaximum = mTerrain[0];
		
		for(int i = 1; i < mTerrain.length; i++) {
			mMinimum = Math.min(mMinimum, mTerrain[i]);
			mMaximum = Math.max(mMaximum, mTerrain[i]);
		}
	}

	public void draw(Canvas canvas, Paint paint) {
		if(mCachedImage == null || mDirty) {
			recalculate();
			if(recache() == false) return;
		}
		
		canvas.drawBitmap(mCachedImage, 0, mBase - mCachedImage.getHeight(), paint);
	}

	public int absAt(int x) {
		return mBase - at(x);
	}

	

	public Vector2D getSlopeAt(int x) {
		int slopeX, slopeY;
		// A note: these differences are swapped out of order because
		// the Cartesian plane we are using is mirrored across the x-axis.
		if(x == 0){
			slopeY = mTerrain[x] - mTerrain[x+1]; slopeX = 1;
		}
		else if(x == mTerrain.length - 1) {
			slopeY = mTerrain[x-1] - mTerrain[x]; slopeX = 1;
		}
		else {
			slopeY = mTerrain[x-1] - mTerrain[x+1]; slopeX = 2;
		}
		return new Vector2D(slopeX, slopeY);
	}
}
