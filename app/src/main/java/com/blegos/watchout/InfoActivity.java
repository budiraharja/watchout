package com.blegos.watchout;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CheckBox;

public class InfoActivity extends Activity {
	public static final String EXTRA_IS_INITIAL = "info_is_initial";
	public static final String EXTRA_ACTION = "info_action";
	
	public interface ACTION {
		public static final int HELP = 0;
		public static final int VERSION = 1;
	}
	
	private boolean isInitial;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_info);
		
		WebView webView = (WebView) findViewById(R.id.wv_watchout_info);
		final CheckBox check = (CheckBox) findViewById(R.id.cb_watchout_info);
		Button btnClose = (Button) findViewById(R.id.bt_watchout_ok);
		
		webView.setDrawingCacheEnabled(false);
		
		String url = "file:///android_asset/info/watchout_info.html";
		
		isInitial = true;
		
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			if (extras.containsKey(InfoActivity.EXTRA_IS_INITIAL)) {
				isInitial = extras.getBoolean(InfoActivity.EXTRA_IS_INITIAL, true);
				if (!isInitial) {
					check.setVisibility(View.GONE);
				}
			}
			
			if (extras.containsKey(InfoActivity.EXTRA_ACTION)) {
				int action = extras.getInt(EXTRA_ACTION);
				switch (action) {
				case ACTION.HELP:
					break;
				case ACTION.VERSION:
					url = "file:///android_asset/info/watchout_version.html";
					break;

				default:
					break;
				}
			}
		}
		
		webView.loadUrl(url);
		
		btnClose.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (isInitial) {
					Preference.getInstance().saveData(getApplicationContext(), Preference.PREF_DONT_SHOW_INFO, check.isChecked());
				}
				
				finish();
			}
		});
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (isInitial) {
			Utility.startService(this);
			finish();
		}
	}

}
