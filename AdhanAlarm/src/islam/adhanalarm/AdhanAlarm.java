package islam.adhanalarm;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Vibrator;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

import net.sourceforge.jitl.astro.Dms;
import net.sourceforge.jitl.Jitl;
import net.sourceforge.jitl.Method;
import net.sourceforge.jitl.Rounding;
import net.sourceforge.jitl.Prayer;

public class AdhanAlarm extends Activity {
	public static final boolean DEBUG = false;

	private static final short DAWN = 0, FAJR = 1, SUNRISE = 2, DHUHR = 3, ASR = 4, MAGHRIB = 5, ISHAA = 6, NEXT_DAWN = 7, NEXT_FAJR = 8; // Notification Times
	private static final short DISPLAY_ONLY = 0, VIBRATE = 1, BEEP = 2, BEEP_AND_VIBRATE = 3, RECITE_ADHAN = 4; // Notification Methods
	private static final short ALERT_PRAYERS_ONLY = 0, ALERT_DAWN = 1, ALERT_SUNRISE = 2, ALERT_DAWN_AND_SUNRISE = 3; // Extra Alerts

	private static TextView[] NOTIFICATION_MARKERS = null;
	private static TextView[] ALARM_TIMES = null;
	private static String[] TIME_NAMES = null;

	private static Method[] CALCULATION_METHODS = new Method[]{Method.NONE, Method.EGYPT_SURVEY, Method.KARACHI_SHAF, Method.KARACHI_HANAF, Method.NORTH_AMERICA, Method.MUSLIM_LEAGUE, Method.UMM_ALQURRA, Method.FIXED_ISHAA};
	private static Rounding[] ROUNDING_TYPES = new Rounding[]{Rounding.NONE, Rounding.NORMAL, Rounding.SPECIAL, Rounding.AGRESSIVE};

	private static SharedPreferences settings = null;
	private static MediaPlayer mediaPlayer = null;
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.main);

		NOTIFICATION_MARKERS = new TextView[]{(TextView)findViewById(R.id.mark_dawn), (TextView)findViewById(R.id.mark_fajr), (TextView)findViewById(R.id.mark_sunrise), (TextView)findViewById(R.id.mark_dhuhr), (TextView)findViewById(R.id.mark_asr), (TextView)findViewById(R.id.mark_maghrib), (TextView)findViewById(R.id.mark_ishaa), (TextView)findViewById(R.id.mark_next_dawn), (TextView)findViewById(R.id.mark_next_fajr)};
		ALARM_TIMES = new TextView[]{(TextView)findViewById(R.id.dawn), (TextView)findViewById(R.id.fajr), (TextView)findViewById(R.id.sunrise), (TextView)findViewById(R.id.dhuhr), (TextView)findViewById(R.id.asr), (TextView)findViewById(R.id.maghrib), (TextView)findViewById(R.id.ishaa), (TextView)findViewById(R.id.next_dawn), (TextView)findViewById(R.id.next_fajr)};
		TIME_NAMES = new String[]{getString(R.string.dawn), getString(R.string.fajr), getString(R.string.sunrise), getString(R.string.dhuhr), getString(R.string.asr), getString(R.string.maghrib), getString(R.string.ishaa), getString(R.string.next_dawn), getString(R.string.next_fajr)};

		settings = getSharedPreferences("settingsFile", MODE_PRIVATE);

		Spinner notification_methods = (Spinner)findViewById(R.id.notification_methods);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.notification_methods, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		notification_methods.setAdapter(adapter);
		notification_methods.setSelection(settings.getInt("notificationMethodIndex", BEEP));

		Spinner extra_alerts = (Spinner)findViewById(R.id.extra_alerts);
		adapter = ArrayAdapter.createFromResource(this, R.array.extra_alerts, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		extra_alerts.setAdapter(adapter);
		extra_alerts.setSelection(settings.getInt("extraAlertsIndex", ALERT_PRAYERS_ONLY));

		((EditText)findViewById(R.id.latitude)).setText(Float.toString(settings.getFloat("latitude", (float)51.477222)));
		((EditText)findViewById(R.id.longitude)).setText(Float.toString(settings.getFloat("longitude", (float)-122.132)));
		((EditText)findViewById(R.id.altitude)).setText(Float.toString(settings.getFloat("altitude", 0)));

		Spinner calculation_methods = (Spinner)findViewById(R.id.calculation_methods);
		adapter = ArrayAdapter.createFromResource(this, R.array.calculation_methods, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		calculation_methods.setAdapter(adapter);
		calculation_methods.setSelection(settings.getInt("calculationMethodsIndex", 0));

		Spinner rounding_types = (Spinner)findViewById(R.id.rounding_types);
		adapter = ArrayAdapter.createFromResource(this, R.array.rounding_types, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		rounding_types.setAdapter(adapter);
		rounding_types.setSelection(settings.getInt("roundingTypesIndex", 2));

		((EditText)findViewById(R.id.pressure)).setText(Float.toString(settings.getFloat("pressure", 1010)));
		((EditText)findViewById(R.id.temperature)).setText(Float.toString(settings.getFloat("temperature", 10)));

		double gmtOffset = getGMTOffset();
		String plusMinusGMT = gmtOffset < 0 ? "" + gmtOffset : "+" + gmtOffset;
		String daylightTime = isDaylightSavings() ? " " + getString(R.string.daylight_savings) : "";
		((TextView)findViewById(R.id.display_time_zone)).setText(getString(R.string.system_time_zone) + ": " + getString(R.string.gmt) + plusMinusGMT + " (" + new GregorianCalendar().getTimeZone().getDisplayName() + daylightTime + ")");

		TabHost tabs = (TabHost)findViewById(R.id.tabs);
		tabs.setup();

		TabHost.TabSpec one = tabs.newTabSpec("one");
		one.setContent(R.id.content1);
		one.setIndicator(getString(R.string.today), getResources().getDrawable(R.drawable.calendar));
		tabs.addTab(one);

		TabHost.TabSpec two = tabs.newTabSpec("two");
		two.setContent(R.id.content2);
		two.setIndicator(getString(R.string.alert), getResources().getDrawable(R.drawable.volume));
		tabs.addTab(two);

		TabHost.TabSpec three = tabs.newTabSpec("three");
		three.setContent(R.id.content3);
		three.setIndicator(getString(R.string.place), getResources().getDrawable(R.drawable.globe));
		tabs.addTab(three);

		TabHost.TabSpec four = tabs.newTabSpec("four");
		four.setContent(R.id.content4);
		four.setIndicator(getString(R.string.extra), getResources().getDrawable(R.drawable.calculator));
		tabs.addTab(four);

		Button playStop = (Button)findViewById(R.id.play_stop);
		playStop.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				if(mediaPlayer != null && mediaPlayer.isPlaying()) {
					((Button)findViewById(R.id.play_stop)).setText(getString(R.string.preview_next_alarm));
					mediaPlayer.stop();
					//((Vibrator)getSystemService("vibrator")).cancel();
				} else {
					playAlertIfAppropriate(getNextNotificationTime());
				}
			}
		});

		Button lookupGPS = (Button)findViewById(R.id.lookup_gps);
		lookupGPS.setOnClickListener(new Button.OnClickListener() {  
			public void onClick(View v) {
				EditText latitude = (EditText)findViewById(R.id.latitude);
				EditText longitude = (EditText)findViewById(R.id.longitude);

				LocationManager locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
				Location location = locationManager.getLastKnownLocation("gps");

				if(location != null) {
					latitude.setText(Double.toString(location.getLatitude()));
					longitude.setText(Double.toString(location.getLongitude()));
				} else {
					latitude.setText(Float.toString(settings.getFloat("latitude", (float)51.477222))); // default greenwich
					longitude.setText(Float.toString(settings.getFloat("longitude", (float)-122.132)));
				}
			}
		});

		Button saveAndApplyAlert = (Button)findViewById(R.id.save_and_apply_alert);
		saveAndApplyAlert.setOnClickListener(new Button.OnClickListener() {  
			public void onClick(View v) {
				SharedPreferences.Editor editor = settings.edit();
				editor.putInt("notificationMethodIndex", ((Spinner)findViewById(R.id.notification_methods)).getSelectedItemPosition());
				editor.putInt("extraAlertsIndex", ((Spinner)findViewById(R.id.extra_alerts)).getSelectedItemPosition());
				editor.commit();
				updateScheduleAndNotification();
				((TabHost)findViewById(R.id.tabs)).setCurrentTab(0);
			}
		});

		Button saveAndApplyPlace = (Button)findViewById(R.id.save_and_apply_place);
		saveAndApplyPlace.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				SharedPreferences.Editor editor = settings.edit();
				editor.putString("latitude", ((EditText)findViewById(R.id.latitude)).getText().toString());
				editor.putString("longitude", ((EditText)findViewById(R.id.longitude)).getText().toString());
				editor.putFloat("altitude", Float.parseFloat(((EditText)findViewById(R.id.altitude)).getText().toString()));
				editor.commit();
				updateScheduleAndNotification();
				((TabHost)findViewById(R.id.tabs)).setCurrentTab(0);
			}
		});

		Button resetExtra = (Button)findViewById(R.id.reset_extra);
		resetExtra.setOnClickListener(new Button.OnClickListener() {  
			public void onClick(View v) {
				((Spinner)findViewById(R.id.calculation_methods)).setSelection(0);
				((Spinner)findViewById(R.id.rounding_types)).setSelection(1);
				((EditText)findViewById(R.id.pressure)).setText("1010.0");
				((EditText)findViewById(R.id.temperature)).setText("10.0");
			}
		});

		Button saveAndApplyExtra = (Button)findViewById(R.id.save_and_apply_extra);
		saveAndApplyExtra.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				SharedPreferences.Editor editor = settings.edit();
				editor.putInt("calculationMethodsIndex", ((Spinner)findViewById(R.id.calculation_methods)).getSelectedItemPosition());
				editor.putInt("roundingTypesIndex", ((Spinner)findViewById(R.id.rounding_types)).getSelectedItemPosition());
				editor.putFloat("pressure", Float.parseFloat(((EditText)findViewById(R.id.pressure)).getText().toString()));
				editor.putFloat("temperature", Float.parseFloat(((EditText)findViewById(R.id.temperature)).getText().toString()));
				editor.commit();
				updateScheduleAndNotification();
				((TabHost)findViewById(R.id.tabs)).setCurrentTab(0);
			}
		});
	}

	public void onResume() {
		short notificationTime = getIntent().getShortExtra("nextNotificationTime", (short)-1);
		if(notificationTime > 0) {
			Toast.makeText(this, getString(R.string.time_for) + " " + TIME_NAMES[notificationTime], Toast.LENGTH_LONG).show();
			playAlertIfAppropriate(notificationTime);
		}
		getIntent().removeExtra("nextNotificationTime");
		updateScheduleAndNotification();
		((TabHost)findViewById(R.id.tabs)).setCurrentTab(0);
		super.onResume();
	}

	public void onNewIntent(Intent intent) {
		setIntent(intent);
		super.onNewIntent(intent);
	}

	private double getGMTOffset() {
		Calendar currentTime = new GregorianCalendar();
		int gmtOffset = currentTime.getTimeZone().getOffset(currentTime.getTimeInMillis());
		return gmtOffset / 3600000;
	}

	private boolean isDaylightSavings() {
		Calendar currentTime = new GregorianCalendar();
		return currentTime.getTimeZone().inDaylightTime(currentTime.getTime());
	}

	private void playAlertIfAppropriate(short time) {
		int notificationMethod = settings.getInt("notificationMethodIndex", BEEP);
		if(notificationMethod == DISPLAY_ONLY) return;
		if(notificationMethod == VIBRATE || notificationMethod == BEEP_AND_VIBRATE) {
			//((Vibrator)getSystemService("vibrator")).vibrate(new long[]{0, 1000}, 15000);
		}
		if(notificationMethod != VIBRATE) {
			int alarm = R.raw.beep;
			int extraAlerts = settings.getInt("extraAlertsIndex", ALERT_PRAYERS_ONLY);
			if(notificationMethod == RECITE_ADHAN && (time == DHUHR || time == ASR || time == MAGHRIB || time == ISHAA || ((extraAlerts == ALERT_PRAYERS_ONLY || extraAlerts == ALERT_DAWN) && time == SUNRISE))) {
				alarm = R.raw.adhan;
			} else if(notificationMethod == RECITE_ADHAN && (time == FAJR || time == NEXT_FAJR || ((extraAlerts == ALERT_PRAYERS_ONLY || extraAlerts == ALERT_SUNRISE) && (time == DAWN || time == NEXT_DAWN)))) {
				alarm = R.raw.adhan_fajr;
			}
			mediaPlayer = MediaPlayer.create(AdhanAlarm.this, alarm);
			try {
				((Button)findViewById(R.id.play_stop)).setText(getString(R.string.stop));
				mediaPlayer.start();
				mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
					public void onCompletion(MediaPlayer mp) {
						((Button)findViewById(R.id.play_stop)).setText(getString(R.string.preview_next_alarm));
					}
				});
			} catch(Exception ex) {
				((TextView)findViewById(R.id.notes)).setText(getString(R.string.error_playing_alert));
			}
		}
		/*
		NotificationManager nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		nm.cancelAll();
		Notification notification = new Notification(R.drawable.icon, getString(R.string.time_for) + " " + getTimeName(time), System.currentTimeMillis());
		notification.setLatestEventInfo(this, getString(R.string.app_name), getString(R.string.time_for) + " " + getTimeName(time), PendingIntent.getActivity(this, 0, new Intent(this, AdhanAlarm.class), 0));
		if(notificationMethod != VIBRATE && notificationMethod != DISPLAY_ONLY) {
			notification.audioStreamType = Notification.STREAM_DEFAULT;
			notification.sound = android.net.Uri.parse("android.resource://islam.adhanalarm/" + getString(alarm));
		}
		if(notificationMethod == VIBRATE || notificationMethod == BEEP_AND_VIBRATE) {
			notification.vibrate = new long[] {100, 250, 100, 500};
		}
		nm.notify(1, notification);
		 */
	}

	private short getNextNotificationTime() {
		for(short i = DAWN; i <= NEXT_FAJR; i++) {
			if(NOTIFICATION_MARKERS[i].getText() == getString(R.string.next_time_marker)) return i;
		}
		return -1;
	}

	private void indicateNextNotificationAndAlarmTimes(short nextNotificationTime) {
		TextView note = (TextView)findViewById(R.id.notes);

		for(short i = DAWN; i <= NEXT_FAJR; i++) NOTIFICATION_MARKERS[i].setText(""); // Clear all existing markers in case it was left from the previous day or while phone was turned off
		NOTIFICATION_MARKERS[nextNotificationTime].setText(getString(R.string.next_time_marker));

		int extraAlerts = settings.getInt("extraAlertsIndex", ALERT_PRAYERS_ONLY);
		String nextAlert = TIME_NAMES[nextNotificationTime];
		if(nextNotificationTime == DAWN && extraAlerts != ALERT_DAWN && extraAlerts != ALERT_DAWN_AND_SUNRISE) {
			nextAlert = TIME_NAMES[FAJR];
		} else if(nextNotificationTime == SUNRISE && extraAlerts != ALERT_SUNRISE && extraAlerts != ALERT_DAWN_AND_SUNRISE) {
			nextAlert = TIME_NAMES[DHUHR];
		} else if(nextNotificationTime == NEXT_DAWN && extraAlerts != ALERT_DAWN && extraAlerts != ALERT_DAWN_AND_SUNRISE) {
			nextAlert = TIME_NAMES[NEXT_FAJR];
		}
		note.setText(getString(R.string.next_alert) + ": " + nextAlert);
	}

	private void updateScheduleAndNotification() {
		Method method = CALCULATION_METHODS[settings.getInt("calculationMethodsIndex", 0)].copy();
		method.setRound(ROUNDING_TYPES[settings.getInt("roundingTypesIndex", 2)]);

		net.sourceforge.jitl.astro.Location location = new net.sourceforge.jitl.astro.Location(settings.getFloat("latitude", (float)51.477222), settings.getFloat("longitude", (float)-122.132), getGMTOffset(), isDaylightSavings() ? 1 : 0);
		location.setSeaLevel(settings.getFloat("altitude", 0));
		location.setPressure(settings.getFloat("pressure", 1010));
		location.setTemperature(settings.getFloat("temperature", 10));

		Jitl itl = DEBUG ? new DummyJitl(location, method) : new Jitl(location, method);
		GregorianCalendar today = new GregorianCalendar();
		Prayer[] dayPrayers = itl.getPrayerTimes(today).getPrayers();
		Prayer[] allTimes = new Prayer[]{itl.getImsaak(today), dayPrayers[0], dayPrayers[1], dayPrayers[2], dayPrayers[3], dayPrayers[4], dayPrayers[5], itl.getNextDayImsaak(today), itl.getNextDayFajr(today)};

		Calendar currentTime = Calendar.getInstance();
		DateFormat timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT);
		GregorianCalendar[] notificationTimes = new GregorianCalendar[9];
		short nextNotificationTime = -1;
		for(short i = DAWN; i <= NEXT_FAJR; i++) { // Set the times on the schedule
			notificationTimes[i] = new GregorianCalendar(currentTime.get(Calendar.YEAR), currentTime.get(Calendar.MONTH), currentTime.get(Calendar.DAY_OF_MONTH), allTimes[i].getHour(), allTimes[i].getMinute(), allTimes[i].getSecond());
			ALARM_TIMES[i].setText(timeFormat.format(notificationTimes[i].getTime()) + (allTimes[i].isExtreme() ? "*" : ""));
			if(nextNotificationTime < 0 && (currentTime.compareTo(notificationTimes[i]) < 0 || i == NEXT_FAJR)) {
				nextNotificationTime = i;
			}
		}
		indicateNextNotificationAndAlarmTimes(nextNotificationTime);

		// Add Latitude, Longitude and Qibla DMS location to front panel
		((TextView)findViewById(R.id.current_latitude)).setText(new Dms(location.getDegreeLat()).toString());
		((TextView)findViewById(R.id.current_longitude)).setText(new Dms(location.getDegreeLong()).toString());
		((TextView)findViewById(R.id.current_qibla)).setText(itl.getNorthQibla().toString());

		setNextNotificationTime(nextNotificationTime, notificationTimes[nextNotificationTime].getTimeInMillis());
	}

	private void setNextNotificationTime(short nextNotificationTime, long actualTimestamp) {
		if(DEBUG) ((TextView)findViewById(R.id.notes)).setText(((TextView)findViewById(R.id.notes)).getText() + ", Debug: " + Math.random());

		Intent intent = new Intent(this, WakeUpAndDoSomething.class);
		intent.putExtra("nextNotificationTime", nextNotificationTime);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		AlarmManager am = (AlarmManager)getSystemService(ALARM_SERVICE);
		am.set(AlarmManager.RTC_WAKEUP, actualTimestamp, PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_ONE_SHOT));
	}
}