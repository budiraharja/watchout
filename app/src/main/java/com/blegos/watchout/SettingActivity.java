package com.blegos.watchout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Camera.Parameters;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.blegos.watchout.CameraSurfaceView.CameraSurfaceViewListener;

public class SettingActivity extends Activity implements CameraSurfaceViewListener {
	public static final String KEY_LISTENER = "setting_listener";
	public static final String SERVICE_RECEIVER_DESTROY = "com.blegos.watchout.SERVICE_RECEIVER";

	public interface SETTING {
		public interface CONTROL {
			public static final int ZOOM_SHORTCUT = 0;
			public static final int SINGLE_TAP = 1;
			public static final int DOUBLE_TAP = 2;
			public static final int LONG_PRESS = 3;
			public static final int LAUNCHER = 4;
		}

		public interface ABOUT {
			public static final int HELP = 0;
			public static final int RATE = 1;
			public static final int VERSION = 2;
		}
	}

	public interface SettingListener {
		void onScaleChange(int scale);

		void onSettingFinish();
	}

	private CameraSurfaceView cameraSurfaceView;
	private ScrollView svSetting;
	private FrameLayout frameDisplaySetting;
	private SeekBar sbScale;
	private LinearLayout llZoom;
	private SeekBar sbZoom;
	private EditText etImagePath;
	private ListView lvSettingControl;
	private ListView lvSettingAbout;

	private SettingListener listener;

	private Bundle extras = null;
	private String[] controls;
	private String[] controlValues;
	private String[] launcherValues;
	
	private int width;
	private int height;
	private boolean holdService = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_setting);
		
		svSetting = (ScrollView) findViewById(R.id.sv_candid_setting);
		frameDisplaySetting = (FrameLayout) findViewById(R.id.fl_candid_setting);
		sbScale = (SeekBar) findViewById(R.id.sb_setting_preview_size);
		llZoom = (LinearLayout) findViewById(R.id.ll_setting_init_zoom);
		sbZoom = (SeekBar) findViewById(R.id.sb_setting_init_zoom);
		etImagePath = (EditText) findViewById(R.id.et_setting_capture_path);
		lvSettingControl = (ListView) findViewById(R.id.lv_setting_control);
		lvSettingAbout = (ListView) findViewById(R.id.lv_setting_about);

		cameraSurfaceView = new CameraSurfaceView(this, this);
		extras = getIntent().getExtras();
		
		svSetting.pageScroll(View.FOCUS_UP);
		this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		
		initCameraView();
	}
	
	private void initView() {
		if (extras != null) {
			SettingListAdapter settingControlAdapter = new SettingListAdapter(
					this, initSettingControl(), false);
			SettingListAdapter settingAboutAdapter = new SettingListAdapter(
					this, initSettingAbout(), true);

			lvSettingControl.setAdapter(settingControlAdapter);
			lvSettingAbout.setAdapter(settingAboutAdapter);

			setListViewHeightBasedOnChildren(lvSettingControl);
			setListViewHeightBasedOnChildren(lvSettingAbout);

			lvSettingControl
					.setOnItemClickListener(new SettingControlOnClickListener());
			lvSettingAbout
					.setOnItemClickListener(new SettingAboutOnClickListener());

			listener = (SettingListener) extras.getParcelable(KEY_LISTENER);

			String path = Preference.getInstance().loadDataString(this,
					Preference.PREF_FILE_PATH);
			etImagePath.setText(path);

			int scale = Preference.getInstance().loadDataInt(this,
					Preference.PREF_SCALE);
			sbScale.setProgress(scale);

			sbScale.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

				@Override
				public void onStopTrackingTouch(SeekBar seekBar) {
					int progress = seekBar.getProgress();

					Preference.getInstance().saveData(getApplicationContext(),
							Preference.PREF_SCALE, progress);
					changeScale(progress);

					if (listener != null) {
						listener.onScaleChange(progress);
					}
				}

				@Override
				public void onStartTrackingTouch(SeekBar seekBar) {
				}

				@Override
				public void onProgressChanged(SeekBar seekBar, int progress,
						boolean fromUser) {
				}
			});
		} else {
			finish();
		}
	}
	
	private void initCameraView() {
		width = Preference.getInstance().loadDataInt(this,
				Preference.PREF_DISPLAY_WIDTH);
		if (width > 0) {
			height = (int)(((double)Constants.DEFAULT_VIEW_HEIGHT/(double)Constants.DEFAULT_VIEW_WIDTH) * width);
		}
		
		if (width == 0) {
			width = Utility.convertDipToPx(this, Constants.DEFAULT_VIEW_WIDTH);
		}
		
		if (height == 0) {
			height = Utility.convertDipToPx(this, Constants.DEFAULT_VIEW_HEIGHT);
		}
		
		ViewGroup.LayoutParams params = new LayoutParams(width, height);
		frameDisplaySetting.setLayoutParams(params);
	}
	
	private ServiceReceiver serviceReceiver = new ServiceReceiver();
	
	private class ServiceReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(SERVICE_RECEIVER_DESTROY)) {
				Log.d("Test", "## onReceive receiver");
				
				frameDisplaySetting.addView(cameraSurfaceView);

				initView();
			}
		}
		
	}

	@Override
	protected void onResume() {
		if (!holdService) {
			IntentFilter intentFilter = new IntentFilter(SERVICE_RECEIVER_DESTROY);
			registerReceiver(serviceReceiver, intentFilter);
	
			Utility.stopService(this);
		}
		
		holdService = false;
		super.onResume();
	}
	
	@Override
	protected void onStart() {
		super.onStart();
	}

	@Override
	protected void onPause() {
		if (!holdService) {
			unregisterReceiver(serviceReceiver);
			
			if (frameDisplaySetting != null && cameraSurfaceView != null) {
				frameDisplaySetting.removeView(cameraSurfaceView);
			}
			
			Utility.startService(this);
		}
		
		super.onPause();
	}

	@Override
	protected void onStop() {
		String path = etImagePath.getText().toString();

		if (!"".equals(path)) {
			if (!path.startsWith("/")) {
				path = "/" + path;
			}
			Preference.getInstance().saveData(this, Preference.PREF_FILE_PATH,
					path);
		}
		
		if (listener != null) {
			listener.onSettingFinish();
		}

		super.onStop();
	}
	
	private void toastInvalidZoomSupport() {
		Toast.makeText(getApplicationContext(), getString(R.string.candid_zoom_not_supported), Toast.LENGTH_SHORT).show();
	}

	private void changeScale(int scale) {
		width = Utility.convertDipToPx(this, Constants.DEFAULT_VIEW_WIDTH)
				+ (Utility.convertDipToPx(this, scale * 10));
		height = (int) (((double) Utility.convertDipToPx(this,
				Constants.DEFAULT_VIEW_HEIGHT) / (double) Utility
				.convertDipToPx(this, Constants.DEFAULT_VIEW_WIDTH)) * width);

		Preference.getInstance().saveData(this, Preference.PREF_DISPLAY_WIDTH,
				width);
		
		initCameraView();
	}

	private List<Map<String, String>> initSettingControl() {
		List<Map<String, String>> list = new ArrayList<Map<String, String>>();

		controls = getResources().getStringArray(R.array.gs_setting_control);
		controlValues = getResources().getStringArray(
				R.array.gs_setting_control_action);
		launcherValues = getResources().getStringArray(
				R.array.gs_setting_control_launcher);
		
		int launcher = Preference.getInstance().loadDataInt(this,
				Preference.PREF_LAUNCHER);
		boolean zoomShorcut = Preference.getInstance().loadDataBoolean(this,
				Preference.PREF_ZOOM_SHORTCUT, false);
		int singleTap = Preference.getInstance().loadDataInt(this,
				Preference.PREF_CONTROL_SINGLE_TAP, -1);
		int doubleTap = Preference.getInstance().loadDataInt(this,
				Preference.PREF_CONTROL_DOUBLE_TAP);
		int longPress = Preference.getInstance().loadDataInt(this,
				Preference.PREF_CONTROL_LONG_PRESS);
		
		if (singleTap == -1) {
			Preference.getInstance().saveData(this,
					Preference.PREF_CONTROL_SINGLE_TAP, 1);
			singleTap = 1;
		}

		int index = 0;
		for (String control : controls) {
			Map<String, String> map = new HashMap<String, String>();
			map.put(Constants.SETTING_ITEM, control);

			switch (index++) {
			case 0:
				map.put(Constants.SETTING_ITEM_SUB, (zoomShorcut ? getString(R.string.candid_setting_zoom_shortcut_show)
						: getString(R.string.candid_setting_zoom_shortcut_hide)));
				break;
			case 1:
				map.put(Constants.SETTING_ITEM_SUB, controlValues[singleTap]);
				break;
			case 2:
				map.put(Constants.SETTING_ITEM_SUB, controlValues[doubleTap]);
				break;
			case 3:
				map.put(Constants.SETTING_ITEM_SUB, controlValues[longPress]);
				break;
			case 4:
				map.put(Constants.SETTING_ITEM_SUB, launcherValues[launcher]);
				break;

			default:
				break;
			}

			list.add(map);
		}

		return list;
	}

	private List<Map<String, String>> initSettingAbout() {
		List<Map<String, String>> list = new ArrayList<Map<String, String>>();

		String[] abouts = getResources().getStringArray(
				R.array.gs_setting_about);

		int index = 0;
		for (String about : abouts) {
			Map<String, String> map = new HashMap<String, String>();
			map.put(Constants.SETTING_ITEM, about);
			
			list.add(map);
		}

		return list;
	}

	public static void setListViewHeightBasedOnChildren(ListView listView) {

		ListAdapter listAdapter = listView.getAdapter();

		if (listAdapter == null) {
			return;
		}

		int totalHeight = 0;

		for (int i = 0; i < listAdapter.getCount(); i++) {
			View listItem = listAdapter.getView(i, null, listView);
			listItem.measure(0, 0);
			totalHeight += listItem.getMeasuredHeight();
		}

		ViewGroup.LayoutParams params = listView.getLayoutParams();
		params.height = totalHeight
				+ (listView.getDividerHeight() * (listAdapter.getCount() - 1));
		listView.setLayoutParams(params);
		listView.requestLayout();
	}

	class SettingControlOnClickListener implements OnItemClickListener {

		@Override
		public void onItemClick(AdapterView<?> adapterView, View view,
				int position, long id) {
			if (position == SETTING.CONTROL.ZOOM_SHORTCUT
					&& !validateZoomSupported()) {
				toastInvalidZoomSupport();
			} else {
				showDialogControl(position, controls[position]);
			}
		}
	}

	class SettingAboutOnClickListener implements OnItemClickListener {

		@Override
		public void onItemClick(AdapterView<?> adapterView, View view,
				int position, long id) {
			switch (position) {
			case SETTING.ABOUT.HELP: {
					holdService = true;
					
					Intent intent = new Intent(getApplicationContext(), InfoActivity.class);
					intent.putExtra(InfoActivity.EXTRA_IS_INITIAL, false);
					intent.putExtra(InfoActivity.EXTRA_ACTION, InfoActivity.ACTION.HELP);
	
					startActivity(intent);
				}
				break;
			case SETTING.ABOUT.RATE: {
					Intent intent = new Intent(Intent.ACTION_VIEW,
							Uri.parse("market://details?id="
									+ Constants.PRODUCT_PACKAGE));
	
					try {
						startActivity(intent);
					} catch (android.content.ActivityNotFoundException anfe) {
						startActivity(new Intent(
								Intent.ACTION_VIEW,
								Uri.parse("http://play.google.com/store/apps/details?id="
										+ Constants.PRODUCT_PACKAGE)));
					}
				}
				break;
			case SETTING.ABOUT.VERSION: {
					holdService = true;
					
					Intent intent = new Intent(getApplicationContext(), InfoActivity.class);
					intent.putExtra(InfoActivity.EXTRA_IS_INITIAL, false);
					intent.putExtra(InfoActivity.EXTRA_ACTION, InfoActivity.ACTION.VERSION);
	
					startActivity(intent);
				}
				break;

			default:
				break;
			}
		}
	}
	
	private boolean validateZoomSupported() {
		if (cameraSurfaceView != null 
				&& cameraSurfaceView.getCameraParameters() != null
				&& cameraSurfaceView.getCameraParameters().isZoomSupported()) {
			return true;
		}
		
		return false;
	}

	private void showDialogControl(final int action, String title) {
		int arrays = R.array.gs_setting_control_action;
		
		if (!validateZoomSupported()) {
			arrays = R.array.gs_setting_control_action_zoom_not_supported;
		}

		if (action == SETTING.CONTROL.ZOOM_SHORTCUT) {
			arrays = R.array.gs_setting_zoom_control;
		} else if (action == SETTING.CONTROL.LAUNCHER) {
			arrays = R.array.gs_setting_control_launcher;
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(title).setItems(arrays,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						switch (action) {
						case SETTING.CONTROL.ZOOM_SHORTCUT:
							Preference.getInstance().saveData(getApplicationContext(), Preference.PREF_ZOOM_SHORTCUT, which == 1 ? true : false);
							break;
						case SETTING.CONTROL.LAUNCHER:
							Preference.getInstance().saveData(getApplicationContext(), Preference.PREF_LAUNCHER, which);
							break;
						case SETTING.CONTROL.SINGLE_TAP:
							if ((which == Constants.CONTROL.ACTION.ZOOM_IN || which == Constants.CONTROL.ACTION.ZOOM_OUT) 
									&& !validateZoomSupported()) {
								toastInvalidZoomSupport();
							} else {
								Preference.getInstance().saveData(getApplicationContext(), Preference.PREF_CONTROL_SINGLE_TAP, which);
							}
							break;
						case SETTING.CONTROL.DOUBLE_TAP:
							if ((which == Constants.CONTROL.ACTION.ZOOM_IN || which == Constants.CONTROL.ACTION.ZOOM_OUT) 
									&& !validateZoomSupported()) {
								toastInvalidZoomSupport();
							} else {
								Preference.getInstance().saveData(getApplicationContext(), Preference.PREF_CONTROL_DOUBLE_TAP, which);
							}
							break;
						case SETTING.CONTROL.LONG_PRESS:
							if ((which == Constants.CONTROL.ACTION.ZOOM_IN || which == Constants.CONTROL.ACTION.ZOOM_OUT) 
									&& !validateZoomSupported()) {
								toastInvalidZoomSupport();
							} else {
								Preference.getInstance().saveData(getApplicationContext(), Preference.PREF_CONTROL_LONG_PRESS, which);
							}
							break;

						default:
							break;
						}
						
						lvSettingControl.setAdapter(new SettingListAdapter(getApplicationContext(), initSettingControl(), false));
						
						dialog.dismiss();
					}
				});
		Dialog dialog = builder.create();
		dialog.show();
	}

	class SettingListAdapter extends BaseAdapter {
		private Context context;
		private List<Map<String, String>> data;
		private boolean isSingle;

		public SettingListAdapter(Context context,
				List<Map<String, String>> data, boolean isSingle) {
			this.context = context;
			this.data = data;
			this.isSingle = isSingle;
		}

		public void update(List<Map<String, String>> data) {
			this.data = data;
			notifyDataSetChanged();
		}
		
		public void update(Map<String, String> data, int position) {
			this.data.set(position, data);
			notifyDataSetChanged();
		}

		@Override
		public int getCount() {
			return data.size();
		}

		@Override
		public Object getItem(int position) {
			return data.get(position);
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = getLayoutInflater().inflate(
						R.layout.layout_setting_item, null, false);
				TextView tvItem = (TextView) convertView
						.findViewById(R.id.tv_setting_item);
				TextView tvItemSub = (TextView) convertView
						.findViewById(R.id.tv_setting_item_sub);

				Map<String, String> map = data.get(position);
				if (map != null) {
					tvItem.setText(map.get(Constants.SETTING_ITEM));
					if (!isSingle) {
						tvItemSub.setText(map.get(Constants.SETTING_ITEM_SUB));
					} else {
						tvItemSub.setVisibility(View.GONE);
					}
				}
			}

			return convertView;
		}
	}
	
	/**
	 * Camera Surface View Listener
	 */
	@Override
	public void onSurfaceCreated() {
	}

	@Override
	public void onSurfaceChanged() {
		if (!validateZoomSupported()) {
			boolean zoomShorcut = Preference.getInstance().loadDataBoolean(this,
					Preference.PREF_ZOOM_SHORTCUT, false);
			int singleTap = Preference.getInstance().loadDataInt(this,
					Preference.PREF_CONTROL_SINGLE_TAP, -1);
			int doubleTap = Preference.getInstance().loadDataInt(this,
					Preference.PREF_CONTROL_DOUBLE_TAP);
			int longPress = Preference.getInstance().loadDataInt(this,
					Preference.PREF_CONTROL_LONG_PRESS);
			
			Preference.getInstance().saveData(this, Preference.PREF_ZOOM_SHORTCUT, false);
			zoomShorcut = false;
			
			if (singleTap == Constants.CONTROL.ACTION.ZOOM_IN || singleTap == Constants.CONTROL.ACTION.ZOOM_OUT) {
				singleTap = Constants.CONTROL.ACTION.NONE;
				Preference.getInstance().saveData(this, Preference.PREF_CONTROL_SINGLE_TAP, singleTap);
			}
			if (doubleTap == Constants.CONTROL.ACTION.ZOOM_IN || singleTap == Constants.CONTROL.ACTION.ZOOM_OUT) {
				doubleTap = Constants.CONTROL.ACTION.NONE;
				Preference.getInstance().saveData(this, Preference.PREF_CONTROL_DOUBLE_TAP, doubleTap);
			}
			if (longPress == Constants.CONTROL.ACTION.ZOOM_IN || singleTap == Constants.CONTROL.ACTION.ZOOM_OUT) {
				longPress = Constants.CONTROL.ACTION.NONE;
				Preference.getInstance().saveData(this, Preference.PREF_CONTROL_LONG_PRESS, longPress);
			}
		}
		
		if (cameraSurfaceView != null 
				&& cameraSurfaceView.getCameraParameters() != null 
				&& cameraSurfaceView.getCameraParameters().isZoomSupported()) {
			int zoom = Preference.getInstance().loadDataInt(getApplicationContext(), Preference.PREF_ZOOM_SCALE);
			int maxZoom = cameraSurfaceView.getCameraParameters().getMaxZoom();
			if (maxZoom > 0) {
				llZoom.setVisibility(View.VISIBLE);
					
				boolean zoomShortcut = Preference.getInstance().loadDataBoolean(this, Preference.PREF_ZOOM_SHORTCUT, false);
					
				sbZoom.setMax(maxZoom);
				sbZoom.setProgress(zoom);
					
				sbZoom.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
						
					@Override
					public void onStopTrackingTouch(SeekBar seekBar) {
						int progress = seekBar.getProgress();
							
						Parameters cameraParameters = cameraSurfaceView.getCameraParameters();
						
						if (cameraParameters.isSmoothZoomSupported()) {
							try {
								cameraSurfaceView.getCamera().startSmoothZoom(progress);
							} catch (Exception e) {
								cameraParameters.setZoom(progress);
							}
						} else {
							cameraParameters.setZoom(progress);
						}
							
						Preference.getInstance().saveData(getApplicationContext(), Preference.PREF_ZOOM_SCALE, progress);
							
						cameraSurfaceView.getCamera().setParameters(cameraParameters);
					}
						
					@Override
					public void onStartTrackingTouch(SeekBar seekBar) {
					}
						
					@Override
					public void onProgressChanged(SeekBar seekBar, int progress,
							boolean fromUser) {
					}
				});
			}
		}
	}

	@Override
	public void onSurfaceError(String message) {
		Toast.makeText(this, getString(R.string.candid_camera_preview_error), Toast.LENGTH_LONG).show();
		//finish();
	}
}
