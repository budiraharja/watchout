package com.blegos.watchout;

import android.content.Context;
import android.content.SharedPreferences;


public class Preference {
	private final String SP_COMMON = "com.blegos.candid";
	public static final String PREF_LOADED = "pref_loaded";
	public static final String PREF_SCALE = "pref_view_scale";
	public static final String PREF_DEFAULT_DISPLAY_WIDTH = "pref_view_default_display_width_new";
	public static final String PREF_DEFAULT_DISPLAY_HEIGHT = "pref_view_default_display_height_new";
	public static final String PREF_DISPLAY_WIDTH = "pref_view_display_width_new";
	public static final String PREF_FILE_PATH = "pref_file_path";
	public static final String PREF_DONT_SHOW_INFO = "pref_show_info_110";
	public static final String PREF_ZOOM_SCALE = "pref_zoom_scale";
	public static final String PREF_LAUNCHER = "pref_launcher";
	public static final String PREF_ZOOM_SHORTCUT = "pref_zoom_shortcut";
	public static final String PREF_APP_VERSION = "pref_app_version";
	public static final String PREF_CONTROL_SINGLE_TAP = "pref_single_tap";
	public static final String PREF_CONTROL_DOUBLE_TAP = "pref_double_tap";
	public static final String PREF_CONTROL_LONG_PRESS = "pref_long_press";
	public static final String PREF_ZOOM_SUPPORTED = "pref_zoom_supported";
	
	private static Preference instance;
	
	protected Preference() {
	}
	
	public static Preference getInstance() {
		if(instance == null) {
			return new Preference();
		}
		return instance;
	}
	
	private SharedPreferences sharedPreferences(Context context) {
		return context.getSharedPreferences(SP_COMMON, Context.MODE_PRIVATE);
	}
	
	public void saveData(Context context, String key, int value) {
		sharedPreferences(context).edit().putInt(key, value).commit();
	}
	
	public void saveData(Context context, String key, String value) {
		sharedPreferences(context).edit().putString(key, value).commit();
	}
	
	public void saveData(Context context, String key, boolean value) {
		sharedPreferences(context).edit().putBoolean(key, value).commit();
	}
	
	public int loadDataInt(Context context, String key) {
		return sharedPreferences(context).getInt(key , 0);
	}
	
	public int loadDataInt(Context context, String key, int defaultValue) {
		return sharedPreferences(context).getInt(key , defaultValue);
	}
	
	public String loadDataString(Context context, String key) {
		return sharedPreferences(context).getString(key, "");
	}
	
	public Boolean loadDataBoolean(Context context, String key) {
		return loadDataBoolean(context, key, true);
	}
	
	public Boolean loadDataBoolean(Context context, String key, boolean defaultValue) {
		return sharedPreferences(context).getBoolean(key, defaultValue);
	}
}
