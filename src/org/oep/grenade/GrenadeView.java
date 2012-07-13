package org.oep.grenade;

import java.util.ArrayList;
import java.util.Random;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.TextView;

public class GrenadeView extends SurfaceView implements OnTouchListener,
		SensorEventListener, OnCompletionListener {

	public static final int MAX_SLOPE = 5;
	public static final Random RNG = new Random();

	/**
	 * The minimum distance a grenade must achieve before it will do some damage
	 */
	public static final int BLAST_RADIUS = 55;

	/** The maximum velocity a grenade will launch an enemy */
	public static final float BLAST_POWER = 865f;

	/** The imprecision applied to BLAST_POWER */
	public static final int BLAST_SPREAD = 90;

	/** The distance at which a grenade will 'kill' an enemy */
	public static final int KILL_RADIUS = 5;

	/** The maximum damage a grenade can deal */
	public static final int GRENADE_MAX_DMG = Player.MAX_HEALTH / 2;
	
	/** The maximum fall damage you can take */
	public static final int MAX_FALL_DMG = Player.MAX_HEALTH / 4;
	
	/** Fall damage threshold speed */
	public static final int FALL_THRESHOLD = 25;
	
	/** Max fall damage achieved at this velocity */
	public static final int FALL_MAX = 50;

	/** The grenade will blow up after this many frames */
	public static final int GRENADE_FUSE = 150;

	/** A factor we apply to accelerometer values to calculate the throw speed */
	public static final int THROW_FACTOR = 1000;

	/** A choice of giblets */
	public static final int[] GIBLETS = new int[] { R.drawable.giblet_heart,
			R.drawable.giblet_cake, R.drawable.giblet_candy };

	/** Frames per second, of course */
	public static final int FRAMES_PER_SECOND = 30;

	public static final int TURN_RED = 0;
	public static final int TURN_BLUE = 1;

	/**
	 * This is a linear array of integers specifying the height at each x
	 * position.
	 */
	private Terrain mTerrain;

	private int mCurrentTurn;

	/** These are the objects that keep up with our players */
	private Player mRedPlayer = new Player();
	private Player mBluePlayer = new Player();

	/** These are all the drawables we will need */
	private Drawable mGrenadeDrawable;
	private Drawable mRedPlayerDrawable;
	private Drawable mRedArrow;
	private Drawable mBluePlayerDrawable;
	private Drawable mBlueArrow;
	private Drawable mCloudDrawable;
	private Drawable mGrenadeButton;
	private Drawable mGreenArrow;
	private Drawable mCrosshair;

	/** Sounds that we wish to use */
	private MediaPlayer mExplosionSound;
	private MediaPlayer mSplatSound;

	private TextView mMessenger;

	private boolean mInitialized = false;

	private Paint mPaint = new Paint();

	private long mDelay = 1000 / FRAMES_PER_SECOND;
	private float mGravity = 9.8f;

	private long mDebugLogicTime;

	private ArrayList<Cloud> mClouds = new ArrayList<Cloud>();
	private ArrayList<RigidBody> mGiblets = new ArrayList<RigidBody>();

	/** The accelerometer updates these values */
	private float mGX, mGY, mGZ, mDGX, mDGY, mDGZ;

	/** The vibrator service */
	private Vibrator mVibrator;

	/** Are we in throwing mode */
	private boolean mThrowingMode = false;

	/** Has the player initiated the throwing procedure */
	private boolean mPlayerThrowing = false;

	/** The crosshair's position */
	private float mCrosshairX, mCrosshairY;

	/** The last timestamp when the player touched down */
	private long mLastTouchDown;

	/** Updates the state of the view (not the show) */
	private RefreshHandler mRedrawHandler = new RefreshHandler();

	/** Should we keep updating? */
	private boolean mContinue = true;

	/** The grenade! */
	private RigidBody mGrenade;

	/** When was the grenade pin pulled? */
	private int mGrenadeTimer;
	private TextView mCountdown;
	private int TRACKBALL_SENSITIVITY = 20;

	class RefreshHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			GrenadeView.this.update();
			GrenadeView.this.invalidate(); // Mark the view as 'dirty'
		}

		public void sleep(long delay) {
			this.removeMessages(0);
			this.sendMessageDelayed(obtainMessage(0), delay);
		}
	}

	/**
	 * Just use the default View constructor...
	 * 
	 * @param context
	 * @param attrs
	 */
	public GrenadeView(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.setBackgroundColor(Color.WHITE);
		this.setOnTouchListener(this);
		initGrenadeView();
	}

	private void initGrenadeView() {
		GrenadeGame c = (GrenadeGame) getContext();
		Resources r = c.getResources();

		mRedPlayerDrawable = r.getDrawable(R.drawable.red_guy);
		mBluePlayerDrawable = r.getDrawable(R.drawable.blue_guy);
		mGrenadeDrawable = r.getDrawable(R.drawable.grenade);
		mCloudDrawable = r.getDrawable(R.drawable.cloud);
		mGrenadeButton = r.getDrawable(R.drawable.grenade_button);
		mRedArrow = r.getDrawable(R.drawable.red_arrow);
		mBlueArrow = r.getDrawable(R.drawable.blue_arrow);
		mGreenArrow = r.getDrawable(R.drawable.green_arrow);
		mCrosshair = r.getDrawable(R.drawable.crosshair);

		mRedPlayer.setDrawable(mRedPlayerDrawable);
		mBluePlayer.setDrawable(mBluePlayerDrawable);

		mRedPlayer.setGravity(mGravity);
		mBluePlayer.setGravity(mGravity);

		mRedPlayer.setPhysics(false);
		mBluePlayer.setPhysics(false);

		mRedPlayer.setFriction(0.2f);
		mRedPlayer.setElasticity(0.3f);

		mBluePlayer.setFriction(0.2f);
		mBluePlayer.setElasticity(0.3f);

		// Randomly decide whose turn it is
		mCurrentTurn = (RNG.nextInt(2) == 0) ? TURN_BLUE : TURN_RED;

		mVibrator = (Vibrator) getContext().getSystemService(
				Context.VIBRATOR_SERVICE);

		// Load sound(s)
		mExplosionSound = loadSound(R.raw.explosion);
		mSplatSound = loadSound(R.raw.splat);

		setFocusable(true);
	}

	public void update() {
		long now = System.currentTimeMillis();

		if (mInitialized)
			doGameLogic();

		if (mContinue) {
			long diff = mDebugLogicTime = System.currentTimeMillis() - now;
			mRedrawHandler.sleep(Math.max(0, mDelay - diff));
		}
	}

	private void doGameLogic() {
		loadViews();
		doGrenadeLogic();
		doCloudLogic();
		doPlayerLogic(mRedPlayer);
		doPlayerLogic(mBluePlayer);
		doGibletLogic();

		if (mCountdown != null && mCountdown.getVisibility() == View.VISIBLE) {
			mCountdown.setText(getCountdownString());
		}
	}

	private void doGibletLogic() {
		for (RigidBody giblet : mGiblets) {
			moveBody(giblet);
		}
	}

	private void loadViews() {
		GrenadeGame c = (GrenadeGame) getContext();
		if (mMessenger == null)
			mMessenger = (TextView) c.findViewById(R.id.gameMessage);
		if (mCountdown == null)
			mCountdown = (TextView) c.findViewById(R.id.grenadeCountdown);
	}

	private String getCountdownString() {
		return Float.toString(Math.max(0, (mGrenadeTimer * mDelay / 1000f)));
	}

	private void doPlayerLogic(Player player) {
		if(player.isDead() == true) {
			// The player is dead. There is nothing we can do for him.
			// Just move on, buddy. It'll be all right...
			
			return;
		}
	
		boolean bounced = moveBody(player);
		int x = (int) player.getX();
		int y = (int) player.getY();

		float dx = Math.abs(player.getVX());
		float dy = Math.abs(player.getVX());
		
		if(bounced) {
			// Get a normal vector at this point
			Vector2D unitNormal = mTerrain.getSlopeAt(x).getNormal().getUnitVector();
			
			// Dot it to see how much of the velocity was normal
			float dmgVelocity = Math.abs( unitNormal.dot( player.getVelocity() ) );
			
			int dmg = (int) Math.max(0, Math.min(MAX_FALL_DMG, MAX_FALL_DMG * (dmgVelocity - FALL_THRESHOLD) / (FALL_MAX - FALL_THRESHOLD)));
			player.takeDamage(dmg);
			
			if(player.getHealth() == 0) {
				playSound(mSplatSound);
				makeMovingGiblets(player);
				player.setDead(true);
			}
		}

		// If the player has stopped moving or its physics timer is up and it is
		// sitting on the ground, stop!
		if (player.getPhysics()
				&& ((dx < 2 && dy < 2) || player.getPhysicsTimer() == 0)
				&& y == getHeight() - mTerrain.at(x)) {
			player.setPhysics(false);
			player.setPosition(x, y);
		}

		if (!player.getPhysics()) {
			player.setPosition(x, getHeight() - mTerrain.at(x));
			player.nextFrame();
		}
	}

	private void doGrenadeLogic() {
		mGrenadeTimer = Math.max(0, mGrenadeTimer - 1);

		if (mGrenade == null)
			return;
		moveBody(mGrenade);

		int x = (int) mGrenade.getX();
		int y = (int) mGrenade.getY();

		// Blow it up if needed
		if (mGrenadeTimer <= 0) {
			int leftBound = Math.max(0, x - BLAST_RADIUS);
			int rightBound = Math
					.min(mTerrain.getWidth() - 1, x + BLAST_RADIUS);

			// Play dat sound
			playSound(mExplosionSound);

			// A doozie of a for-loop. Loop through the x pixels of a circle
			// formed by the blast radius
			// bounded by the dimensions of mTerrain.
			for (int i = leftBound; i < rightBound; i++) {
				int dx = x - i;
				int blastY = (int) Math.sqrt(BLAST_RADIUS * BLAST_RADIUS - dx
						* dx);

				int terrainHeight = getHeight() - mTerrain.at(i);

				// Subtract off any terrain that may have been consumed in the
				// blast
				mTerrain.offset(i, -Math.max(0, y + blastY
						- Math.max(terrainHeight, y - blastY)));
			}

			// Make the blast cloud
			Cloud c = new Cloud(x, y);
			mClouds.add(c);

			// We could have hit a player...
			blastPlayer(mRedPlayer);
			blastPlayer(mBluePlayer);

			// There is no grenade
			mGrenade = null;
		}
	}

	private void doCloudLogic() {
		for (Cloud c : mClouds) {
			c.update();

			if (c.getAlpha() == 0) {
				mClouds.remove(c);
			}
		}
	}

	private void blastPlayer(Player player) {
		if (player.isDead() == true)
			return;

		double distance = player.distance(mGrenade);
		if (distance > BLAST_RADIUS)
			return;

		double scale = Math.max(0, Math.min((BLAST_RADIUS - distance)
				/ (BLAST_RADIUS - KILL_RADIUS), 1));
		int dmg = (int) (scale * GRENADE_MAX_DMG);
		player.takeDamage(dmg);

		// If the player is dead we don't need to blast him any more. But we
		// will blast his giblets.
		if (player.getHealth() == 0) {
			makeExplodedGiblets(player);
			player.setDead(true);
			playSound(mSplatSound);
		} else {
			blastBody(player);
		}
	}

	private RigidBody[] makeGiblets(Rect bounds) {
		RigidBody[] giblets = new RigidBody[Player.GIBLET_CHUNKS];

		for (int i = 0; i < Player.GIBLET_CHUNKS; i++) {
			RigidBody giblet = new RigidBody();
			giblet.setGravity(mGravity);
			giblet.setElasticity(0.3f);
			giblet.setFriction(0.3f);
			giblet.setDrawable(getGiblet());

			giblet.setPosition(bounds.left + RNG.nextInt(bounds.width()),
					bounds.top + RNG.nextInt(bounds.height()));
			mGiblets.add(giblet);
			giblets[i] = giblet;
		}

		return giblets;
	}
	
	private RigidBody[] makeExplodedGiblets(RigidBody player) {
		RigidBody[] giblets = makeGiblets(player.getBounds());
		
		for(int i = 0; i < giblets.length; i++) {
			blastBody(giblets[i]);
		}
		
		return giblets;
	}
	
	private RigidBody[] makeMovingGiblets(RigidBody player) {
		RigidBody[] giblets = makeGiblets(player.getBounds());
		
		for(int i = 0; i < giblets.length; i++) {
			giblets[i].setVelocity(player.getVX(), player.getVY());
		}
		
		return giblets;
	}

	private void blastBody(RigidBody body) {
		double distance = body.distance(mGrenade);
		if (distance > BLAST_RADIUS)
			return;

		double scale = Math.max(0, Math.min((BLAST_RADIUS - distance)
				/ (BLAST_RADIUS - KILL_RADIUS), 1));
		float power = (float) (scale * BLAST_POWER) + RNG.nextInt(BLAST_SPREAD);

		// Get a vector in the direction the thingy will travel
		float dx = body.getX() - mGrenade.getX();
		float dy = body.getY() - mGrenade.getY();

		// In case we get a zero vector, go straight up and in some other
		// direction
		if (dx == 0 && dy == 0) {
			dy = -1;
			dx = -50 + RNG.nextInt(100);
		}

		// Calculate magnitude of said vector
		float magnitude = (float) Math.sqrt(dx * dx + dy * dy);

		// And use it to get the unit vector
		float ux = dx / magnitude, uy = dy / magnitude;

		// And set it!
		body.setVelocity(ux * power, uy * power);

		// Enable physics and the world flies away with you!!!
		body.setPhysics(true);
		body.setPhysicsTimer(50);
	}

	public void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
		super.onSizeChanged(width, height, oldWidth, oldHeight);
		mTerrain = new Terrain(width, 3 * height / 4, height / 4, height);
		positionPlayers(width);

		int w = mGrenadeButton.getIntrinsicWidth();
		int h = mGrenadeButton.getIntrinsicHeight();
		mGrenadeButton.setBounds(width / 2 - w / 2, height / 2 - h / 2, width
				/ 2 + w / 2, height / 2 + h / 2);

		mCrosshairX = width / 2;
		mCrosshairY = height / 2;

		mInitialized = true;
	}

	private void positionPlayers(int viewWidth) {
		// This is invalid if the terrain isn't initialized.
		if (mTerrain == null)
			return;

		int rx = RNG.nextInt(mTerrain.getWidth());
		int bx = RNG.nextInt(mTerrain.getWidth());

		int h = getHeight();
		mRedPlayer.setPosition(rx, h - mTerrain.at(rx));
		mBluePlayer.setPosition(bx, h - mTerrain.at(bx));

		// TODO: Do something in case the players are too close
	}

	public void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		if (mInitialized) {
			mPaint.setColor(Color.BLACK);
			drawTerrain(canvas);
			drawPlayers(canvas);
			drawBodies(canvas);
			drawClouds(canvas);
			drawDebug(canvas);
			drawCrosshair(canvas);

			if (mThrowingMode) {
				drawThrowingScreen(canvas);
			}

		}
	}

	private void drawCrosshair(Canvas canvas) {
		mCrosshair.draw(canvas);
	}

	private void drawThrowingScreen(Canvas canvas) {
		float power = getThrowPower();

		mPaint.setColor(Color.BLACK);
		mPaint.setAlpha(190);
		canvas.drawRect(0, 0, getWidth(), getHeight(), mPaint);

		mPaint.setColor(Color.RED);
		canvas.drawRect(0, getHeight() - power, 10, getHeight(), mPaint);

		mPaint.setColor(Color.WHITE);
		mGrenadeButton.draw(canvas);
	}

	private void drawClouds(Canvas canvas) {
		for (Cloud c : mClouds) {
			c.draw(canvas, mCloudDrawable);
		}
	}

	private void drawDebug(Canvas canvas) {

	}

	private void drawBodies(Canvas canvas) {
		drawBody(mGrenade, mGreenArrow, canvas);

		for (RigidBody g : mGiblets) {
			drawBody(g, null, canvas);
		}
	}

	private void drawPlayers(Canvas canvas) {
		// This requires the terrain be initialized
		if (mTerrain == null)
			return;

		drawBody(mRedPlayer, mRedArrow, canvas);
		drawBody(mBluePlayer, mBlueArrow, canvas);
	}

	private void drawBody(RigidBody body, Drawable arrow, Canvas canvas) {
		if (body == null)
			return;

		if (body.getY() >= 0)
			body.draw(canvas);
		else if (arrow != null) {
			int w = arrow.getIntrinsicWidth();
			int h = arrow.getIntrinsicHeight();
			arrow.setBounds((int) body.getX() - w / 2, 0, (int) body.getX() + w
					/ 2, h);
			arrow.draw(canvas);
		}
	}

	private void drawTerrain(Canvas canvas) {
		// Just skip the rest if the View has not initialized
		if (mTerrain == null)
			return;

		mPaint.setColor(Color.BLACK);
		mTerrain.draw(canvas, mPaint);
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		int x = (int) event.getX();
		int y = (int) event.getY();

		boolean crosshairContains = mCrosshair.getBounds().contains(x, y);
		boolean buttonContains = mGrenadeButton.getBounds().contains(x, y);

		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:

			if (mThrowingMode && buttonContains) {
				requestStartFuse();
			}

			else if (!mThrowingMode && crosshairContains) {
				requestThrowingMode();
			}

			else if (!mThrowingMode) {
				centerDrawable(mCrosshair, x, y);
			}

			break;

		case MotionEvent.ACTION_MOVE:
			if (!mThrowingMode) {
				centerDrawable(mCrosshair, x, y);
			}
			break;

		case MotionEvent.ACTION_UP:
			if (mPlayerThrowing) {
				Rect r = mCrosshair.getBounds();
				float dx = r.centerX() - mBluePlayer.getX();
				float dy = r.centerY() - mBluePlayer.getY();
				float mag = (float) Math.hypot(dx, dy);

				float ux = dx / mag;
				float uy = dy / mag;

				float power = getThrowPower();
				createGrenade(mBluePlayer.getX(), mBluePlayer.getY() - 10);
				mGrenade.setVelocity(power * ux, power * uy);
				mPlayerThrowing = false;
				mThrowingMode = false;

				mCountdown.setVisibility(View.INVISIBLE);
				mMessenger.setVisibility(View.INVISIBLE);
			}

			break;
		}

		return true;
	}

	private void requestStartFuse() {
		if (mVibrator != null) {
			mVibrator.vibrate(50);
		}

		mPlayerThrowing = true;
		mGrenadeTimer = GRENADE_FUSE;
		mMessenger.setText(R.string.toss);
		mCountdown.setVisibility(View.VISIBLE);
	}

	private void requestThrowingMode() {
		// Don't enter throwing mode if there is a grenade or either of the
		// players is still bouncing around
		if (mGrenade == null && !mBluePlayer.getPhysics()
				&& !mRedPlayer.getPhysics() && !mThrowingMode) {
			mThrowingMode = true;
			mMessenger.setVisibility(View.VISIBLE);
			mMessenger.setText(R.string.pullPin);
		}
	}

	@Override
	public boolean onTrackballEvent(MotionEvent event) {
		int dx = (int) (TRACKBALL_SENSITIVITY * event.getX());
		int dy = (int) (TRACKBALL_SENSITIVITY * event.getY());

		int x = mCrosshair.getBounds().centerX();
		int y = mCrosshair.getBounds().centerY();

		switch (event.getAction()) {

		case MotionEvent.ACTION_MOVE:
			if (mThrowingMode || mGrenade != null || mBluePlayer.getPhysics()
					|| mRedPlayer.getPhysics())
				break;

			x = Math.max(0, Math.min(getWidth(), x + dx));
			y = Math.max(0, Math.min(getHeight(), y + dy));
			centerDrawable(mCrosshair, x, y);
			break;

		case MotionEvent.ACTION_DOWN:
			if (!mThrowingMode) {
				requestThrowingMode();
			}
		}

		return true;
	}

	private void createGrenade(float x, float y) {
		Resources r = getContext().getResources();
		mGrenade = new RigidBody();
		mGrenade.setPosition(x, y);
		mGrenade.setDrawable(r.getDrawable(R.drawable.grenade));
		mGrenade.setGravity(mGravity);
		mGrenade.setElasticity(0.5f);
		mGrenade.setFriction(0.5f);
	}

	/**
	 * Move a RigidBody object and control its bounce as well.
	 * 
	 * @param body
	 * @return true if bounced
	 */
	private boolean moveBody(RigidBody body) {
		if (body.getPhysics() == false)
			return false;

		body.tickPhysics(-1);

		body.move(mDelay);
		PointF current = body.getPoint();

		boolean bounced = false;
		if (mTerrain.isIllegal(current.x, getHeight() - current.y)) {
			Point warp = mTerrain.getWarpPoint(current);
			body.setPosition(warp.x, warp.y);
			body.bounce(mTerrain, warp.x, warp.y);
			bounced = true;
		}
		return bounced;
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// I don't care.
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		mGX = event.values[0];
		mGY = event.values[1];
		mGZ = event.values[2];
	}

	public void onDestroy() {
		mContinue = false;
	}

	public float getThrowPower() {
		return Math
				.abs(THROW_FACTOR
						* (float) ((Math
								.sqrt(mGX * mGX + mGY * mGY + mGZ * mGZ) / 9.8) - 1));
	}

	private MediaPlayer loadSound(int rid) {
		MediaPlayer mp = MediaPlayer.create(getContext(), rid);
		mp.setOnCompletionListener(this);
		return mp;
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		mp.seekTo(0);
	}

	private void playSound(MediaPlayer mp) {
		if (!mp.isPlaying()) {
			mp.start();
		}
	}

	private Drawable getGiblet() {
		return getContext().getResources().getDrawable(
				GIBLETS[RNG.nextInt(GIBLETS.length)]);
	}

	private void centerDrawable(Drawable d, int x, int y) {
		int w = d.getIntrinsicWidth();
		int h = d.getIntrinsicHeight();
		d.setBounds(x - w / 2, y - h / 2, x + w / 2, y + h / 2);
	}
}
