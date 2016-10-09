package com.blegos.watchout;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.widget.Toast;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		int appVersion = Preference.getInstance().loadDataInt(this, Preference.PREF_APP_VERSION);
		boolean dontShowInfo = Preference.getInstance().loadDataBoolean(this, Preference.PREF_DONT_SHOW_INFO, false);
		
		try {
			PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
			int currentVersion = pInfo.versionCode;
			if (appVersion != currentVersion) {
				dontShowInfo = false;
			}
			
			Preference.getInstance().saveData(this, Preference.PREF_APP_VERSION, currentVersion);
		} catch (Exception e) {
		}
		
		if (Utility.isServiceRunning(this)) {
			Toast.makeText(this, getString(R.string.candid_service_running), Toast.LENGTH_SHORT).show();
		} else {
			if (dontShowInfo) {
				Utility.startService(this);
			} else {
				Intent intent = new Intent(this, InfoActivity.class);
				intent.putExtra(InfoActivity.EXTRA_ACTION, 0);
				startActivity(intent);
			}
		}
		
		finish();
	}
	
}
