/*
 * ELEC390 and COEN390: TI SensorTag Library for Android
 * Example application: Weather Station
 * Author: Marc-Alexandre Chan <marcalexc@arenthil.net>
 * Institution: Concordia University
 */
package ca.concordia.sensortag.weather;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.UUID;

import ca.concordia.sensortag.weather.helper.SessionManager;
import ti.android.ble.sensortag.Sensor;
import ca.concordia.sensortag.SensorTagListener;
import ca.concordia.sensortag.SensorTagLoggerListener;
import ca.concordia.sensortag.SensorTagManager;
import ca.concordia.sensortag.SensorTagManager.ErrorType;
import ca.concordia.sensortag.SensorTagManager.StatusType;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Main WeatherStation activity. This class controls the main activity ("window") for the Weather
 * Station, that shows the temperature, humidity and pressure from the SensorTag sensors. It is
 * responsible for managing a connection to the SensorTag and receiving sensor measurements from it
 * (via the {@link SensorTagManager} class), as well as converting it to the right unit and showing
 * it on the screen.
 */
public class WeatherStationActivity extends Activity {

	static public final String TAG = "WeatherSt"; // Tag for Android's logcat
	static protected final int UPDATE_PERIOD_MS = 2000; // How often measurements should be taken

    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

	// Define formatters for converting the sensor measurement into a string to show on the screen
	private final static DecimalFormat tempFormat = new DecimalFormat("###0.0;-##0.0");
	private final static DecimalFormat humiFormat = new DecimalFormat("##0.0");


    private TextView mTemperatureView;
    private TextView mTemperatureUnitView;
    private EditText IdealTempEdit;
	private TextView IdealTempUnitView;
	private TextView mHumidityView;
	@SuppressWarnings("unused")
	private TextView mHumidityUnitView;

	private Button SetTempButton;
	private Switch mTemperatureUnitSwitch;

	private double minRange = Double.NaN;
	private double maxRange = Double.NaN;

	private double mLastTemperature = Double.NaN;
	@SuppressWarnings("unused")
	private double mLastHumidity = Double.NaN;

	// Unit setting - these values are used to determine which unit to show for temp/pressure.
	// They are changed when the user clicks the switches to change the unit.
	private boolean mIsTempFahrenheit = false;


	// Bluetooth communication with the SensorTag
	private BluetoothDevice mBtDevice;
	private SensorTagManager mStManager;
	private SensorTagListener mStListener;

    private SessionManager session;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather_station);
        // Session manager
        session = new SessionManager(getApplicationContext());

		// Get the Bluetooth device selected by the user - should be set by DeviceSelectActivity
		Intent receivedIntent = getIntent();
        mBtDevice = (BluetoothDevice) receivedIntent
				.getParcelableExtra(DeviceSelectActivity.EXTRA_DEVICE);

 //       address = receivedIntent.getStringExtra(PairHC.EXTRA_ADDRESS); //receive the address of the bluetooth device

		// If we didn't get a device, we can't do anything! Warn the user, log and exit.
		if (mBtDevice == null) {
			Log.e(TAG, "No BluetoothDevice extra [" + DeviceSelectActivity.EXTRA_DEVICE
					+ "] provided in Intent.");
			Toast.makeText(this, "No Bluetooth Device selected", Toast.LENGTH_SHORT).show();
			finish();
			return;
		}


		// Prepare the SensorTag
		mStManager = new SensorTagManager(this, mBtDevice);
		mStListener = new ManagerListener();
		mStManager.addListener(mStListener);

		mStManager.initServices();
		if (!mStManager.isServicesReady()) { // initServices failed or took too long
			Log.e(TAG, "Discover failed - exiting");
			finish();
			return;
		}

		boolean res = true;
		if (mStManager.isPeriodSupported(Sensor.IR_TEMPERATURE))
			res = res && mStManager.enableSensor(Sensor.IR_TEMPERATURE, UPDATE_PERIOD_MS);
		else
			res = res && mStManager.enableSensor(Sensor.IR_TEMPERATURE);


		if (mStManager.isPeriodSupported(Sensor.HUMIDITY))
			res = res && mStManager.enableSensor(Sensor.HUMIDITY, UPDATE_PERIOD_MS);
		else
			res = res && mStManager.enableSensor(Sensor.HUMIDITY);

		// If any of the enableSensor() calls failed, show/log an error and exit.
		if (!res) {
			Log.e(TAG, "Sensor configuration failed - exiting");
			Toast.makeText(this, "Sensor configuration failed - exiting", Toast.LENGTH_LONG).show();
			finish();
		}

		// Get references to the GUI text box objects
		mTemperatureView = (TextView) findViewById(R.id.value_temp);
		mTemperatureUnitView = (TextView) findViewById(R.id.unit_temp);
		mHumidityView = (TextView) findViewById(R.id.value_humi);
		mHumidityUnitView = (TextView) findViewById(R.id.unit_humi);
		IdealTempEdit = (EditText) findViewById(R.id.ideal_temp);
		IdealTempUnitView = (TextView) findViewById(R.id.unit_temp2);

		SetTempButton = (Button) findViewById(R.id.idealtemp_button);

		// Set up the GUI switches for displayed measurement units
		mTemperatureUnitSwitch = (Switch) findViewById(R.id.temp_unit_switch);

		mTemperatureUnitSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			/**
			 * Called when the mTemperatureUnitSwitch switch is clicked, causing the state to
			 * change. Saves the Celsius/Fahrenheit setting and updates the display to show the
			 * desired unit.
			 * 
			 * @param buttonView The mTemperatureUnitSwitch switch object
			 * @param isChecked  Whether the switch is in the checked (Fahrenheit) or unchecked
			 * (Celsius) state.
			 */
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				// Save the unit so we know what to use when a new measurement comes in.
				mIsTempFahrenheit = isChecked;
				
				// And update the measurements that are shown on the screen right now to use the
				// changed unit.
				updateDisplayedUnits();
			}

		});

		SetTempButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {

				minRange = Double.parseDouble(IdealTempEdit.getText().toString()) - convertTemperatureUnit(2);
				maxRange = Double.parseDouble(IdealTempEdit.getText().toString()) + convertTemperatureUnit(2);
                Toast.makeText(getApplicationContext(), "Desired Temperature is Set.", Toast.LENGTH_SHORT).show();

			}
		});


		// Initial values for the measurements on the GUI: before the SensorTag is all ready to go
		// and has sent its first sensor values, we want to show dashes as a placeholder.
		mTemperatureView.setText("--.-");
		IdealTempEdit.setText("22");
		mHumidityView.setText("--.-");
    }



    /**
	 * Called by Android when the Activity is started again. This is shown just for completion:
	 * since there is no code in onStart(), it does not need to be overridden here.
	 * 
	 * @see https://developer.android.com/reference/android/app/Activity.html#ActivityLifecycle
	 */
	@Override
	protected void onStart() {
		super.onStart();
	}

	/**
	 * Called by Android when the Activity comes back into the foreground. When called, enables
	 * processing sensor measurements (which are received by {@link ManagerListener}).
	 * 
	 * @see https://developer.android.com/reference/android/app/Activity.html#ActivityLifecycle
	 */
	@Override
	protected void onResume() {
		super.onResume();
		if (mStManager != null) mStManager.enableUpdates();
	}


	@Override
	protected void onPause() {
		super.onPause();
		if (mStManager != null) mStManager.disableUpdates();
	}


	@Override
	protected void onStop() {
		super.onStop();
	}


	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mStManager != null) mStManager.close();
	}

	/**
	 * Updates the units shown on the screen to match the current unit settings, and recalculates
	 * the measurement values in the correct unit. This method must be called from the main (UI)
	 * thread, as it modifies the GUI directly.
	 */
	private void updateDisplayedUnits() {
		// Update the unit text for the temperature (desired unit = mIsTempFahrenheit)
		if (mIsTempFahrenheit) {
			mTemperatureUnitView.setText(getString(R.string.temperature_f_unit));
			IdealTempUnitView.setText(getString(R.string.temperature_f_unit));
		}
		else {
			mTemperatureUnitView.setText(getString(R.string.temperature_c_unit));
			IdealTempUnitView.setText(getString(R.string.temperature_c_unit));
		}

		// Recalculate the temperature in the desired unit (desired unit = mIsTempFahrenheit)
		double displayTemp = convertTemperatureUnit(mLastTemperature);
		double tempedit = Double.parseDouble(IdealTempEdit.getText().toString());
		double displayTemp2 = convertTemperatureUnitB(tempedit);

		// Take the calculated temperature and show it on the GUI
		mTemperatureView.setText(tempFormat.format(displayTemp));
		IdealTempEdit.setText(tempFormat.format(displayTemp2));

	}

	private double convertTemperatureUnit(double celsius) {
		return mIsTempFahrenheit ? (1.8 * celsius + 32) : celsius;
	}

	private  double convertTemperatureUnitB(double temp) {
		if (!mIsTempFahrenheit)
			return (temp -32)/1.8;
		else
			return (1.8*temp)+32;
	}


	/**
	 * Handles events from the SensorTagManager: SensorTag status updates, sensor measurements, etc.
	 */
	public class ManagerListener extends SensorTagLoggerListener implements SensorTagListener {

		/*
		 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		 * Remember: if you want to use other sensors here, you have to enable them in onCreate and
		 * then add the onUpdate method corresponding to that sensor! See the SensorTagListener
		 * interface for the list of all the onUpdate methods.
		 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		 */

		/**
		 * Called on receiving a new ambient temperature measurement. Displays the new value.
		 * 
		 * @see ca.concordia.sensortag.SensorTagLoggerListener#onUpdateAmbientTemperature(ca.concordia.sensortag.SensorTagManager,
		 *      double)
		 */
		@Override
		public void onUpdateAmbientTemperature(SensorTagManager mgr, double temp) {
			super.onUpdateAmbientTemperature(mgr, temp);

			// Save this measurement value. If the user changes the desired unit between two
			// measurements, we  retrieve this measurement and convert it to the new unit desired
			// by the user.
			mLastTemperature = temp;

			// Convert the measurement to the unit desired by the user.
			final double displayTemp = convertTemperatureUnit(temp);

			// Format the temperature (double) into a string, with one digit after the decimal
			// point (this format is defined by tempFormat: see declaration at top of file).
			final String tempText = tempFormat.format(displayTemp);

			// The UI elements can only be modified on the main ("UI") thread:
			// We use Activity's runOnUiThread() method to schedule this update on the main thread
			// See the Minimal example for more details.
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					// Change the text in the temperature TextView to the new value
					mTemperatureView.setText(tempText);

					VerifyRange(displayTemp);

				}

			});

		}

		/**
		 * Called on receiving a new humidity measurement. Displays the new value.
		 * 
		 * @see ca.concordia.sensortag.SensorTagLoggerListener#onUpdateHumidity(ca.concordia.sensortag.SensorTagManager,
		 *      double)
		 */
		@Override
		public void onUpdateHumidity(SensorTagManager mgr, double rh) {
			super.onUpdateHumidity(mgr, rh);

			// This is the same idea as onUpdateAmbientTemperature() above.
			mLastHumidity = rh;
			final String humiText = humiFormat.format(rh);
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					mHumidityView.setText(humiText);
				}

			});
		}

		/**
		 * Called on receiving a SensorTag-related error. Displays a Toast showing a message to the
		 * user.
		 * 
		 * @todo Reserve an area on the GUI instead so that errors are displayed statically?
		 * @see ca.concordia.sensortag.SensorTagBaseListener#onError(ca.concordia.sensortag.SensorTagManager,
		 *      ca.concordia.sensortag.SensorTagManager .ErrorType, java.lang.String)
		 */
		@Override
		public void onError(SensorTagManager mgr, ErrorType type, String msg) {
			super.onError(mgr, type, msg);
			

			String text = null;
			switch (type) {
			case GATT_REQUEST_FAILED:
				text = "Error: Request failed: " + msg;
				break;
			case GATT_UNKNOWN_MESSAGE:
				text = "Error: Unknown GATT message (Programmer error): " + msg;
				break;
			case SENSOR_CONFIG_FAILED:
				text = "Error: Failed to configure sensor: " + msg;
				break;
			case SERVICE_DISCOVERY_FAILED:
				text = "Error: Failed to discover sensors: " + msg;
				break;
			case UNDEFINED:
				text = "Error: Unknown error: " + msg;
				break;
			default:
				break;
			}
			if (text != null)
				Toast.makeText(WeatherStationActivity.this, text, Toast.LENGTH_LONG).show();
		}

		/**
		 * Called on receiver a SensorTag-related status message. Displays a Toast showing a message
		 * to the user, if relevant.
		 * 
		 * @see ca.concordia.sensortag.SensorTagBaseListener#onStatus(ca.concordia.sensortag.SensorTagManager,
		 *      ca.concordia.sensortag.SensorTagManager .StatusType, java.lang.String)
		 */
		@Override
		public void onStatus(SensorTagManager mgr, StatusType type, String msg) {
			super.onStatus(mgr, type, msg);
			
			// This is similar to onError() above, and we do the same thing: we pick a user-friendly
			// message and (if we have one we want to show) we show the message in a Toast.
			String text = null;
			switch (type) {
			case SERVICE_DISCOVERY_COMPLETED:
				// This message doesn't really need to be announced to the user.
				break;
			case SERVICE_DISCOVERY_STARTED:
				text = "Preparing SensorTag";
				break;
			case UNDEFINED:
				text = "Unknown status";
				break;
			default: // ignore other cases
				break;

			}
			if (text != null)
				Toast.makeText(WeatherStationActivity.this, text, Toast.LENGTH_SHORT).show();
		}

	}


    // creating action button
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.toolbar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            // When logout is clicked
            case R.id.action_logout:
                session.setLogin(false);
                gotoLoginActivity();
                return true;

            // When AddSensoris clicked
            case R.id.action_addSensor:
                gotoPairHC();
                return true;


            default:
                return super.onOptionsItemSelected(item);
        }
    }



    public void gotoLoginActivity(){
        Intent i = new Intent(getApplicationContext(), LoginActivity.class);
        startActivity(i);
    }


    private void gotoPairHC(){
        mBtDevice = null;
        mStManager = null;
        mStListener = null;

        Intent i = new Intent(getApplicationContext(), PairHC.class);
        startActivity(i);
    }

    public void VerifyRange(double temp){
		if(convertTemperatureUnit(minRange) > temp){
			Toast.makeText(getApplicationContext(),
					"Area is cold! ", Toast.LENGTH_SHORT)
					.show();
		}
		if (convertTemperatureUnit(maxRange) < temp) {
			Toast.makeText(getApplicationContext(),
					"Area is hot!", Toast.LENGTH_SHORT)
					.show();
		}
	}

	public void onBackPressed(){
		// Disabling OS back button
	}

}
