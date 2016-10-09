package com.blegos.watchout;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.view.GestureDetectorCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.GestureDetector.OnGestureListener;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.blegos.watchout.SettingActivity.SettingListener;
import com.blegos.watchout.ViewDisplay.ViewDisplayListener;

public class ViewTopService extends Service implements ViewDisplayListener {
	public static final String CAMERA_PREVIEW_SERVICE = "com.blegos.watchout.CAMERA_PREVIEW_SERVICE";
	private static final String BROADCAST_ORIENTATION_CHANGED = "android.intent.action.CONFIGURATION_CHANGED";

	private static final String TAG = "ViewTopService";
	private static final int ZOOM_SCALE_FACTOR = 7;

	private static Context context;
	private static WindowManager windowManager;
	private static ViewDisplay viewDisplay;
	private static ViewClose viewClose;
	private static ViewSetting viewSetting;
	private static ViewZoom viewZoomIn;
	private static ViewZoom viewZoomOut;
	private static ViewToast viewToast;

	private static WindowManager.LayoutParams displayParams;
	private static WindowManager.LayoutParams settingParams;
	private static WindowManager.LayoutParams closeParams;
	private static WindowManager.LayoutParams zoomInParams;
	private static WindowManager.LayoutParams zoomOutParams;
	private static WindowManager.LayoutParams toastParams;

	private int screenHeight;
	private int screenWidth;
	private boolean isPictureTaken = false;
	private int defaultOrientation;
	private static boolean isSetting = false;
	private int minZoom;
	private int maxZoom;
	private static int currentZoom;
	private GestureDetectorCompat gestureDetector;
	
	private boolean zoomShortcut = false;
	private int singleTap;
	private int doubleTap;
	private int longPress;
	
	private boolean isStopped = false;
	private boolean isActionStart = false;
	private boolean isViewMove = false;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		Log.d("ViewTopService", "## onservicecreate");
		super.onCreate();

		context = this;

		if (!checkCameraHardware(this)) {
			stop();
		}

		init();
		initWindowSize();

		windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

		viewDisplay = new ViewDisplay(this, this);
		viewClose = new ViewClose(this);
		viewSetting = new ViewSetting(this);
		viewZoomIn = new ViewZoom(this, ViewZoom.ZOOM_IN);
		viewZoomOut = new ViewZoom(this, ViewZoom.ZOOM_OUT);
		viewToast = new ViewToast(this);

		viewClose.setVisibility(View.GONE);
		viewSetting.setVisibility(View.GONE);
		viewZoomIn.setVisibility(View.GONE);
		viewZoomOut.setVisibility(View.GONE);
		viewToast.setVisibility(View.GONE);
		
		displayParams = createParam();
		initDisplayParam();

		closeParams = createParam();
		initCloseParam();

		settingParams = createParam();
		initSettingParam();

		zoomInParams = createParam();
		initZoomParam(ViewZoom.ZOOM_IN);

		zoomOutParams = createParam();
		initZoomParam(ViewZoom.ZOOM_OUT);

		toastParams = createParam();
		initToastParam();

		viewDisplay.setOnTouchListener(viewTouchListener);
		gestureDetector = new GestureDetectorCompat(viewDisplay.getContext(),new GestureListener());

		windowManager.addView(viewDisplay, displayParams);
		windowManager.addView(viewClose, closeParams);
		windowManager.addView(viewSetting, settingParams);
		windowManager.addView(viewToast, toastParams);

		zoomShortcut = Preference.getInstance().loadDataBoolean(this,
				Preference.PREF_ZOOM_SHORTCUT, false);
		if (zoomShortcut) {
			windowManager.addView(viewZoomIn, zoomInParams);
			windowManager.addView(viewZoomOut, zoomOutParams);
		}
		
		singleTap = Preference.getInstance().loadDataInt(this, Preference.PREF_CONTROL_SINGLE_TAP);
		doubleTap = Preference.getInstance().loadDataInt(this, Preference.PREF_CONTROL_DOUBLE_TAP);
		longPress = Preference.getInstance().loadDataInt(this, Preference.PREF_CONTROL_LONG_PRESS);

		registerReceiver(mOrientaionChangedReceiver, new IntentFilter(
				BROADCAST_ORIENTATION_CHANGED));
		initOrientation();
	}

	private boolean checkCameraHardware(Context context) {
		if (context.getPackageManager().hasSystemFeature(
				PackageManager.FEATURE_CAMERA)) {
			return true;
		} else {
			return false;
		}
	}

	private BroadcastReceiver mOrientaionChangedReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(BROADCAST_ORIENTATION_CHANGED)) {
				if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
					Log.d(TAG, "LANDSCAPE");
				} else {
					Log.d(TAG, "PORTRAIT");
				}

				initWindowSize();

				initDisplayParam();
				windowManager.updateViewLayout(viewDisplay, displayParams);

				initCloseParam();
				windowManager.updateViewLayout(viewClose, closeParams);

				initSettingParam();
				windowManager.updateViewLayout(viewSetting, settingParams);

				try {
					// some devices doesn't support zoom
					int maxZoom = viewDisplay.getCameraParameters()
							.getMaxZoom();
					if (maxZoom > 0) {
						initZoomParam(ViewZoom.ZOOM_IN);
						windowManager
								.updateViewLayout(viewZoomIn, zoomInParams);

						initZoomParam(ViewZoom.ZOOM_OUT);
						windowManager.updateViewLayout(viewZoomOut,
								zoomOutParams);
					}
				} catch (Exception e) {
				}

			}
		}
	};
	
	private void stop() {
		isActionStart = false;
		isStopped = true;
		stopSelf();
	}

	/**
	 * ViewDisplayListener
	 */
	@Override
	public void onSurfaceCreated() {
	}

	@Override
	public void onSurfaceChanged() {
		if (viewDisplay != null && viewDisplay.getCameraParameters() != null
				&& viewDisplay.getCameraParameters().isZoomSupported()) {
			minZoom = Preference.getInstance().loadDataInt(this,
					Preference.PREF_ZOOM_SCALE);
			maxZoom = viewDisplay.getCameraParameters().getMaxZoom();
			currentZoom = minZoom;
		}
	}

	@Override
	public void onSurfaceError() {
	}

	private void init() {
		boolean loaded = Preference.getInstance().loadDataBoolean(this,
				Preference.PREF_LOADED, false);
		if (!loaded) {
			Preference.getInstance().saveData(this, Preference.PREF_LOADED,
					true);
			Preference.getInstance().saveData(this,
					Preference.PREF_DISPLAY_WIDTH, 0);
			Preference.getInstance().saveData(this, Preference.PREF_FILE_PATH,
					Constants.DEFAULT_PATH);
			Preference.getInstance().saveData(this, Preference.PREF_SCALE, 0);
		}
	}

	private void initOrientation() {
		int rotation = windowManager.getDefaultDisplay().getRotation();
		int degrees = 0;

		switch (rotation) {
		case Surface.ROTATION_0:
			degrees = 90;
			defaultOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
			break;
		case Surface.ROTATION_90:
			degrees = 0;
			if (Build.VERSION.SDK_INT > Build.VERSION_CODES.FROYO) {
				defaultOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;
			} else {
				defaultOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
			}
			break;
		case Surface.ROTATION_180:
			degrees = 270;
			if (Build.VERSION.SDK_INT > Build.VERSION_CODES.FROYO) {
				defaultOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
			} else {
				defaultOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
			}
			break;
		case Surface.ROTATION_270:
			degrees = 180;
			if (Build.VERSION.SDK_INT > Build.VERSION_CODES.FROYO) {
				defaultOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
			} else {
				defaultOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
			}
			break;
		}
	}

	private void initWindowSize() {
		DisplayMetrics metrics = Utility.getDisplayMetrics(this);
		int rowWidth = metrics.widthPixels;
		int rowHeight = metrics.heightPixels;

		screenHeight = Math.max(rowWidth, rowHeight);
		screenWidth = Math.min(rowWidth, rowHeight);

		Log.d(TAG, "screenHeight:" + screenHeight + "; screenWidth:"
				+ screenWidth);
	}

	private WindowManager.LayoutParams createParam() {
		WindowManager.LayoutParams params = new WindowManager.LayoutParams(
				WindowManager.LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.TYPE_PHONE,
				WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
				PixelFormat.TRANSLUCENT);
		params.gravity = Gravity.TOP | Gravity.LEFT;

		return params;
	}

	public static int getRotationDegree() {
		int rotation = windowManager.getDefaultDisplay().getRotation();
		int degrees = 90;
		switch (rotation) {
		case Surface.ROTATION_0:
			degrees = 90;
			break;
		case Surface.ROTATION_90:
			degrees = 0;
			break;
		case Surface.ROTATION_180:
			degrees = 270;
			break;
		case Surface.ROTATION_270:
			degrees = 180;
			break;
		}

		return degrees;
	}

	private void updateCameraDisplayOrientation() {
		if (viewDisplay != null && viewDisplay.getCamera() != null) {
			try {
				viewDisplay.getCamera().stopPreview();
				viewDisplay.getCamera().setDisplayOrientation(
						getRotationDegree());
			} catch (Exception e) {
			} finally {
				viewDisplay.getCamera().startPreview();
			}
		}
	}

	private void initDisplayParam() {
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
			if (viewDisplay != null) {
				viewDisplay.updateView(viewDisplay.getViewHeight(),
						viewDisplay.getViewWidth());
				if (viewDisplay.getCamera() != null) {
					updateCameraDisplayOrientation();
				}
			}

			displayParams.x = Math.round(((float) screenHeight / (float) 2)
					- ((float) viewDisplay.getViewHeight() / (float) 2));
			displayParams.y = Math.round(((float) screenWidth / (float) 2)
					- ((float) viewDisplay.getViewWidth() / (float) 2));
		} else {
			if (viewDisplay != null) {
				viewDisplay.updateView(viewDisplay.getViewWidth(),
						viewDisplay.getViewHeight());
				if (viewDisplay.getCamera() != null) {
					updateCameraDisplayOrientation();
				}
			}

			// displayParams.screenOrientation =
			// ActivityInfo.SCREEN_ORIENTATION_SENSOR;
			displayParams.x = Math.round(((float) screenWidth / (float) 2)
					- ((float) viewDisplay.getViewWidth() / (float) 2));
			displayParams.y = Math.round(((float) screenHeight / (float) 2)
					- ((float) viewDisplay.getViewHeight() / (float) 2));
		}
	}

	private void initSettingParam() {
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
			settingParams.x = Math.round((float) screenHeight / (float) 2)
					- (viewSetting.getSize() / 2);
			settingParams.y = Utility.convertDipToPx(this, 5);
		} else {
			settingParams.x = Math.round((float) screenWidth / (float) 2)
					- (viewSetting.getSize() / 2);
			settingParams.y = Utility.convertDipToPx(this, 10);
		}
	}

	private void initCloseParam() {
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
			closeParams.x = Math.round((float) screenHeight / (float) 2)
					- (viewClose.getSize() / 2);
			closeParams.y = screenWidth - Utility.convertDipToPx(this, 75);
		} else {
			closeParams.x = Math.round((float) screenWidth / (float) 2)
					- (viewClose.getSize() / 2);
			closeParams.y = screenHeight - Utility.convertDipToPx(this, 75);
		}
	}

	private void initZoomParam(int type) {
		switch (type) {
		case ViewZoom.ZOOM_IN:
			if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
				zoomInParams.x = screenHeight - viewZoomIn.getSize()
						- Utility.convertDipToPx(this, 10);
				zoomInParams.y = Math.round((float) screenWidth / (float) 2)
						- (viewZoomIn.getSize() / 2);
			} else {
				zoomInParams.x = screenWidth - viewZoomIn.getSize()
						- Utility.convertDipToPx(this, 10);
				zoomInParams.y = Math.round((float) screenHeight / (float) 2)
						- (viewZoomIn.getSize() / 2);
			}
			break;
		case ViewZoom.ZOOM_OUT:
			if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
				zoomOutParams.x = Utility.convertDipToPx(this, 10);
				zoomOutParams.y = Math.round((float) screenWidth / (float) 2)
						- (viewZoomOut.getSize() / 2);
			} else {
				zoomOutParams.x = Utility.convertDipToPx(this, 10);
				zoomOutParams.y = Math.round((float) screenHeight / (float) 2)
						- (viewZoomOut.getSize() / 2);
			}
			break;

		default:
			break;
		}
	}

	private void initToastParam() {
		toastParams.x = Math.round((float) screenWidth / (float) 2)
				- (viewClose.getSize() / 2);
		toastParams.y = screenHeight - Utility.convertDipToPx(this, 50);
	}
	
	private void zoomIn(boolean updateLayout) {
		if ((currentZoom + ZOOM_SCALE_FACTOR) < maxZoom) {
			currentZoom += ZOOM_SCALE_FACTOR;
		} else if (currentZoom < maxZoom) {
			currentZoom = maxZoom;
		}
		updateCameraZoom(updateLayout);
	}
	
	private void zoomOut(boolean updateLayout) {
		if ((currentZoom - ZOOM_SCALE_FACTOR) > minZoom) {
			currentZoom -= ZOOM_SCALE_FACTOR;
		} else if (currentZoom > minZoom) {
			currentZoom = minZoom;
		}
		updateCameraZoom(updateLayout);
	}

	View.OnTouchListener viewTouchListener = new View.OnTouchListener() {
		private int initialX;
		private int initialY;
		private float initialTouchX;
		private float initialTouchY;
		private int moveX;
		private int moveY;

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			gestureDetector.onTouchEvent(event);
			
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				initialX = displayParams.x;
				initialY = displayParams.y;
				initialTouchX = event.getRawX();
				initialTouchY = event.getRawY();

				viewDisplay.setSelected(true);
				return true;
			case MotionEvent.ACTION_UP:
				isViewMove = false;
				
				viewDisplay.setSelected(false);

				/*
				if (Math.abs(displayParams.x - initialX) < Utility
						.convertDipToPx(getApplicationContext(), 1)
						&& Math.abs(displayParams.y - initialY) < Utility
								.convertDipToPx(getApplicationContext(), 1)) {
					Log.d(TAG, "take picture");
					if (!isPictureTaken) {
						isPictureTaken = true;
						takePicture();
					}
				}
				*/

				if (isCameraViewInCloseSection()) {
					viewClose.setSelected(false);
					stop();
				} else if (isCameraViewInSettingSection()) {
					viewSetting.setSelected(false);
					startSetting();

					initDisplayParam();
					windowManager.updateViewLayout(viewDisplay, displayParams);
				} else if (isCameraViewInZoomInSection()) {
					zoomIn(true);
				} else if (isCameraViewInZoomOutSection()) {
					zoomOut(true);
				}

				viewClose.setVisibility(View.GONE);
				viewSetting.setVisibility(View.GONE);
				viewZoomIn.setVisibility(View.GONE);
				viewZoomOut.setVisibility(View.GONE);

				return true;
			case MotionEvent.ACTION_MOVE:
				isViewMove = true;
				
				displayParams.x = initialX
						+ (int) (event.getRawX() - initialTouchX);
				displayParams.y = initialY
						+ (int) (event.getRawY() - initialTouchY);
				
				boolean isValidMove = true;
				
				if (event.getRawX() - initialX < 5 && event.getRawY() - initialY < 5) {
					isValidMove = false;
				}
				
				if (isValidMove) {
					if (!isActionStart && !isStopped) {
						try {
							windowManager.updateViewLayout(viewDisplay, displayParams);
						} catch (Exception e) {
						}
					}
	
					if (!isSetting && !isPictureTaken) {
						viewClose.setVisibility(View.VISIBLE);
						viewSetting.setVisibility(View.VISIBLE);
						viewZoomIn.setVisibility(View.VISIBLE);
						viewZoomOut.setVisibility(View.VISIBLE);
	
						if (isCameraViewInCloseSection()) {
							viewClose.setSelected(true);
						} else if (isCameraViewInSettingSection()) {
							viewSetting.setSelected(true);
						} else if (isCameraViewInZoomInSection()) {
							if (currentZoom < maxZoom) {
								viewZoomIn.setSelected(true);
							}
						} else if (isCameraViewInZoomOutSection()) {
							if (currentZoom > minZoom) {
								viewZoomOut.setSelected(true);
							}
						} else {
							viewClose.setSelected(false);
							viewSetting.setSelected(false);
							viewZoomIn.setSelected(false);
							viewZoomOut.setSelected(false);
						}
					}
				}

				return true;
			}
			return false;
		}
	};
	
	class GestureListener implements OnGestureListener, OnDoubleTapListener {

		@Override
		public boolean onDoubleTap(MotionEvent e) {
			if (!isViewMove) {
				Log.d(TAG, "## onDoubleTap");
				isActionStart = true;
				gestureAction(doubleTap);
			}
			return false;
		}

		@Override
		public boolean onDoubleTapEvent(MotionEvent e) {
			return false;
		}

		@Override
		public boolean onSingleTapConfirmed(MotionEvent e) {
			if (!isViewMove) {
				Log.d(TAG, "## onSingleTapConfirmed");
				isActionStart = true;
				gestureAction(singleTap);
			}
			return false;
		}

		@Override
		public boolean onDown(MotionEvent e) {
			return false;
		}

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
				float velocityY) {
			return false;
		}

		@Override
		public void onLongPress(MotionEvent e) {
			if (!isViewMove) {
				Log.d(TAG, "## onLongPress");
				isActionStart = true;
				gestureAction(longPress);
			}
		}

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2,
				float distanceX, float distanceY) {
			return false;
		}

		@Override
		public void onShowPress(MotionEvent e) {
		}

		@Override
		public boolean onSingleTapUp(MotionEvent e) {
			return false;
		}
	}
	
	private void gestureAction(int action) {
		if (!isStopped && isActionStart) {
			switch (action) {
			case Constants.CONTROL.ACTION.NONE:
				break;
			case Constants.CONTROL.ACTION.CAPTURE:
				if (!isPictureTaken) {
					isPictureTaken = true;
					takePicture();
				}
				break;
			case Constants.CONTROL.ACTION.SETTINGS:
				startSetting();
				break;
			case Constants.CONTROL.ACTION.ZOOM_IN:
				zoomIn(false);
				break;
			case Constants.CONTROL.ACTION.ZOOM_OUT:
				zoomOut(false);
				break;
			case Constants.CONTROL.ACTION.CLOSE_APPLICATION:
				stop();
				break;
	
			default:
				break;
			}
		}
	}

	private boolean isCameraViewInSettingSection() {
		int displayWidth = viewDisplay.getViewWidth();
		int displayHeight = viewDisplay.getViewHeight();

		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
			displayWidth = viewDisplay.getViewHeight();
			displayHeight = viewDisplay.getViewWidth();
		}

		return (displayParams.x <= settingParams.x && (displayParams.x + displayWidth) >= (settingParams.x + viewSetting
				.getSize()))
				&& (displayParams.y <= settingParams.y && (displayParams.y + displayHeight) >= (settingParams.y + viewSetting
						.getSize()));
	}

	private boolean isCameraViewInCloseSection() {
		int displayWidth = viewDisplay.getViewWidth();
		int displayHeight = viewDisplay.getViewHeight();

		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
			displayWidth = viewDisplay.getViewHeight();
			displayHeight = viewDisplay.getViewWidth();
		}

		return (displayParams.x <= closeParams.x && (displayParams.x + displayWidth) >= (closeParams.x + viewClose
				.getSize()))
				&& (displayParams.y <= closeParams.y && (displayParams.y + displayHeight) >= (closeParams.y + viewClose
						.getSize()));
	}

	private boolean isCameraViewInZoomOutSection() {
		int displayWidth = viewDisplay.getViewWidth();
		int displayHeight = viewDisplay.getViewHeight();

		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
			displayWidth = viewDisplay.getViewHeight();
			displayHeight = viewDisplay.getViewWidth();
		}

		return (zoomShortcut && displayParams.x <= zoomOutParams.x && (displayParams.x + displayWidth) >= (zoomOutParams.x + viewZoomOut
				.getSize()))
				&& (displayParams.y <= zoomOutParams.y && (displayParams.y + displayHeight) >= (zoomOutParams.y + viewZoomOut
						.getSize()));
	}

	private boolean isCameraViewInZoomInSection() {
		int displayWidth = viewDisplay.getViewWidth();
		int displayHeight = viewDisplay.getViewHeight();

		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
			displayWidth = viewDisplay.getViewHeight();
			displayHeight = viewDisplay.getViewWidth();
		}

		return (zoomShortcut && displayParams.x <= zoomInParams.x && (displayParams.x + displayWidth) >= (zoomInParams.x + viewZoomIn
				.getSize()))
				&& (displayParams.y <= zoomInParams.y && (displayParams.y + displayHeight) >= (zoomInParams.y + viewZoomIn
						.getSize()));
	}

	private void updateCameraZoom(final boolean updateLayout) {
		Parameters cameraParameters = viewDisplay.getCameraParameters();
		
		if (cameraParameters.isZoomSupported()) {
			if (cameraParameters.isSmoothZoomSupported() && !updateLayout) {
				try {
					viewDisplay.getCamera().startSmoothZoom(currentZoom);
				} catch (Exception e) {
				}
			} else {
				cameraParameters.setZoom(currentZoom);
				viewDisplay.getCamera().setParameters(cameraParameters);
				if (updateLayout) {
					initDisplayParam();
					windowManager.updateViewLayout(viewDisplay, displayParams);
				}
			}
		}
		
		isActionStart = false;
	}

	private static class ViewSettingListener implements SettingListener,
			Parcelable {
		@Override
		public int describeContents() {
			return 0;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
		}

		@Override
		public void onScaleChange(int scale) {
		}

		@Override
		public void onSettingFinish() {
			isSetting = false;
		}

		public static final Parcelable.Creator<ViewSettingListener> CREATOR = new Creator<ViewSettingListener>() {

			@Override
			public ViewSettingListener createFromParcel(Parcel source) {
				return new ViewSettingListener();
			}

			@Override
			public ViewSettingListener[] newArray(int size) {
				return new ViewSettingListener[size];
			}
		};
	}

	private void startSetting() {
		isSetting = true;

		Intent intent = new Intent(this, SettingActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.putExtra(SettingActivity.KEY_LISTENER, new ViewSettingListener());
		startActivity(intent);
		
		isActionStart = false;
	}

	ShutterCallback shutterCallback = new ShutterCallback() {

		@Override
		public void onShutter() {
		}
	};

	PictureCallback pictureCallbackJpg = new PictureCallback() {

		@Override
		public void onPictureTaken(byte[] data, Camera camera) {
			Log.d(TAG, "onPictureTaken");
			saveImage(data, camera);
		}
	};

	Camera.AutoFocusCallback autoFocusCallback = new Camera.AutoFocusCallback() {
		@Override
		public void onAutoFocus(boolean success, Camera camera) {
			Log.d(TAG, "OnAutoFocus");
			if (viewDisplay != null && viewDisplay.getCamera() != null) {
				viewDisplay.getCamera().takePicture(shutterCallback, null,
						null, pictureCallbackJpg);
			}
		}
	};

	private void takePicture() {
		Log.d(TAG, "takePicture");
		if (viewDisplay != null && viewDisplay.getCamera() != null) {
			viewDisplay.getCamera().autoFocus(autoFocusCallback);
		}
	}

	private void saveImage(final byte[] data, Camera camera) {
		Log.d(TAG, "saveImage");
		camera.startPreview();

		new Thread(new Runnable() {

			@Override
			public void run() {
				int[] imageRotate = new int[2];
				if (defaultOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
					imageRotate[0] = 90;
					imageRotate[1] = -90;
				} else if (defaultOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
					imageRotate[0] = 0;
					imageRotate[1] = 0;
				} else if (defaultOrientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT) {
					imageRotate[0] = 180;
					imageRotate[1] = 180;
				} else if (defaultOrientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT) {
					imageRotate[0] = -90;
					imageRotate[1] = 90;
				}

				int rotation = imageRotate[0];

				String path = Environment.getExternalStorageDirectory()
						.getPath()
						+ Preference.getInstance().loadDataString(
								getApplicationContext(),
								Preference.PREF_FILE_PATH);
				File filePath = new File(path);

				if (!filePath.exists()) {
					filePath.mkdirs();
				}

				String fileName = getString(R.string.app_name)
						+ "_"
						+ String.valueOf(Calendar.getInstance()
								.getTimeInMillis()) + ".jpg";
				File file = new File(path, fileName);
				FileOutputStream fOut = null;

				try {
					fOut = new FileOutputStream(file);
					fOut.write(data);
					fOut.close();

					if (rotation != 0) {
						rotateImageFile(file, rotation);
					}

					addFileToMediaScanner(file);
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					try {
						if (fOut != null) {
							fOut.close();
						}
					} catch (IOException e) {
					}
				}
			}
		}).start();

		isPictureTaken = false;
		isActionStart = false;
	}

	private boolean rotateImageFile(File file, int rotation)
			throws FileNotFoundException {
		Log.d(TAG, "rotateImageFile(rotation:" + rotation + ")");
		Boolean rotationSuccess = false;
		Bitmap bitmapOri = null;
		Bitmap bitmapRotate = null;
		FileOutputStream filecon = null;
		try {
			bitmapOri = BitmapFactory.decodeFile(file.getAbsolutePath());
			Matrix mat = new Matrix();
			mat.postRotate(rotation);
			bitmapRotate = Bitmap.createBitmap(bitmapOri, 0, 0,
					bitmapOri.getWidth(), bitmapOri.getHeight(), mat, true);
			bitmapOri.recycle();
			bitmapOri = null;
			filecon = new FileOutputStream(file);
			bitmapRotate.compress(CompressFormat.JPEG, 100, filecon);
			bitmapRotate.recycle();
			bitmapRotate = null;
			rotationSuccess = true;
		} catch (OutOfMemoryError e) {
			System.gc();
		} finally {
			if (bitmapOri != null) {
				bitmapOri.recycle();
				bitmapOri = null;
			}
			if (bitmapRotate != null) {
				bitmapRotate.recycle();
				bitmapRotate = null;
			}
			if (filecon != null) {
				try {
					filecon.close();
				} catch (IOException e) {
				}
				filecon = null;
			}
		}
		return rotationSuccess;
	}

	private void addFileToMediaScanner(File f) {
		Intent mediaScanIntent = new Intent(
				Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
		Uri contentUri = Uri.fromFile(f);
		mediaScanIntent.setData(contentUri);
		sendBroadcast(mediaScanIntent);
	}

	private static int toastTimeSecond = 0;
	private static final int TOAST_MIN_TIME_SECOND = 5;

	private void showToast(String message) {
		ViewToastHandler handler = new ViewToastHandler();

		Message msg = Message.obtain();
		msg.what = ViewToastHandler.WHAT_SHOW_TOAST;
		msg.obj = message;
		handler.sendMessage(msg);
	}

	private class ViewToastHandler extends Handler {
		public static final int WHAT_SHOW_TOAST = 0;

		public ViewToastHandler() {
			super(Looper.getMainLooper());
		}

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case WHAT_SHOW_TOAST:
				if (msg.obj != null) {
					showToast((String) msg.obj);
				}
				break;

			default:
				break;
			}
		}

		private void showToast(String message) {
			if (viewToast != null) {
				viewToast.setText(message);
				viewToast.setVisibility(View.VISIBLE);

				Timer versioningTimer = new Timer();
				try {
					versioningTimer.scheduleAtFixedRate(new TimerTask() {

						@Override
						public void run() {
							toastTimeSecond++;
							if (toastTimeSecond >= TOAST_MIN_TIME_SECOND) {
								viewToast.setVisibility(View.GONE);
							}

						}

					}, 0, 1000);
				} catch (Exception e) {
				}
			}

		}
	}

	@Override
	public void onDestroy() {
		Log.d("ViewTopService", "## onservicedestroy");
		isStopped = true;
		super.onDestroy();
		unregisterReceiver(mOrientaionChangedReceiver);
		
		if (viewDisplay != null) {
			FrameLayout preview = (FrameLayout) viewDisplay.findViewById(viewDisplay.getFrameId());
			preview.removeView(viewDisplay.getCameraSurfaceView());
			
			windowManager.removeView(viewDisplay);
		}
		if (viewClose != null) {
			windowManager.removeView(viewClose);
		}
		if (viewSetting != null) {
			windowManager.removeView(viewSetting);
		}
		if (viewZoomIn != null) {
			try {
				windowManager.removeView(viewZoomIn);
			} catch (Exception e) {
			}
		}
		if (viewZoomOut != null) {
			try {
				windowManager.removeView(viewZoomOut);
			} catch (Exception e) {
			}
		}
		
		Intent intent = new Intent(SettingActivity.SERVICE_RECEIVER_DESTROY);
		sendBroadcast(intent);
	}
}
