package islam.adhanalarm;

import android.content.SharedPreferences;

public class VARIABLE {

	public static SharedPreferences settings;
	public static boolean mainActivityIsRunning = false;

	public static String[] TIME_NAMES; // Convenience array to store for different languages
	public static Schedule today = null; // Only valid during startup of activity

	public static float qiblaDirection = 0;

	public static boolean themeDirty = false;
	public static boolean languageDirty = false;

	private VARIABLE() {
		// Private constructor to enforce un-instantiability.
	}

	public static boolean alertSunrise() {
		if(settings == null) return false;
		return settings.getInt("extraAlertsIndex", CONSTANT.NO_EXTRA_ALERTS) == CONSTANT.ALERT_SUNRISE;
	}
}