package org.oep.grenade;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.Window;

public class GrenadeGame extends Activity {
	
	private GrenadeView mGrenadeView;
	private SensorManager mSensorManager;
	private Sensor mDefaultAccelerometer;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        setContentView(R.layout.main);
        
        mGrenadeView = (GrenadeView) findViewById(R.id.game_view);
        mGrenadeView.update();
        
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		mDefaultAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mSensorManager.registerListener(mGrenadeView, mDefaultAccelerometer, SensorManager.SENSOR_DELAY_GAME);
		
		this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }
    
    public void onDestroy() {
    	super.onDestroy();
//    	mGrenadeView.die();
    	mSensorManager.unregisterListener(mGrenadeView);
    }
}