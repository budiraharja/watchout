package com.blegos.watchout;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.blegos.watchout.CameraSurfaceView.CameraSurfaceViewListener;

public class ViewDisplay extends LinearLayout implements CameraSurfaceViewListener {
	public interface ViewDisplayListener {
		void onSurfaceCreated();
		void onSurfaceChanged();
		void onSurfaceError();
	}
	
	private ViewDisplayListener listener;
	private LinearLayout view;
	private CameraSurfaceView cameraSurfaceView;

	private int width;
	private int height;
	private int viewId = 0;
	private int frameId = 0;
	
	public ViewDisplay(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public ViewDisplay(Context context) {
		super(context);
		init();
	}
	
	public ViewDisplay(Context context, ViewDisplayListener listener) {
		this(context);
		this.listener = listener;
	}
	
	public ViewDisplay(Context context, ViewDisplayListener listener, int viewId, int frameId) {
		this(context, listener);
		this.viewId = viewId;
		this.frameId = frameId;
		
		init();
	}
	
	private void init() {
		width = Preference.getInstance().loadDataInt(getContext(),
				Preference.PREF_DISPLAY_WIDTH);
		if (width > 0) {
			height = (int)(((double)Constants.DEFAULT_VIEW_HEIGHT/(double)Constants.DEFAULT_VIEW_WIDTH) * width);
		}
		
		Log.d(VIEW_LOG_TAG, "width:" + width + " height:" + height);
		
		if (width == 0) {
			width = Utility.convertDipToPx(getContext(), Constants.DEFAULT_VIEW_WIDTH);
		}
		
		if (height == 0) {
			height = Utility.convertDipToPx(getContext(), Constants.DEFAULT_VIEW_HEIGHT);
		}
		
		Log.d(VIEW_LOG_TAG, "width:" + width + " height:" + height);
		
		LayoutInflater mInflater = (LayoutInflater) getContext()
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		if (viewId == 0) {
			viewId = R.layout.layout_display;
		}
		
		view = (LinearLayout) mInflater.inflate(viewId, null,
				false);
		
		if (frameId == 0) {
			frameId = R.id.fl_candid;
		}
		
		cameraSurfaceView = new CameraSurfaceView(getContext(), this);
		FrameLayout preview = (FrameLayout) view.findViewById(frameId);
		preview.addView(cameraSurfaceView);
		
		ViewGroup.LayoutParams params = new LayoutParams(width, height);
		
		super.addView(view, params);
	}
	
	public void updateView(int width, int height) {
		//this.width = width;
		//this.height = height;
		
		view.setLayoutParams(new LayoutParams(width, height));
	}
	
	public ViewDisplayListener getListener() {
		return listener;
	}

	public void setListener(ViewDisplayListener listener) {
		this.listener = listener;
	}

	public LinearLayout getView() {
		return view;
	}

	public int getViewWidth() {
		return this.width;
	}

	public int getViewHeight() {
		return this.height;
	}

	public Camera getCamera() {
		if (cameraSurfaceView != null) {
			return cameraSurfaceView.getCamera();
		}

		return null;
	}

	public Parameters getCameraParameters() {
		if (cameraSurfaceView != null) {
			return cameraSurfaceView.getCameraParameters();
		}

		return null;
	}

	public byte[] getFrameData() {
		if (cameraSurfaceView != null) {
			return cameraSurfaceView.getFrameData();
		}

		return null;
	}
	
	public CameraSurfaceView getCameraSurfaceView() {
		return cameraSurfaceView;
	}

	public int getViewId() {
		return viewId;
	}

	public int getFrameId() {
		return frameId;
	}

	/**
	 * CameraSurfaceViewListener
	 */
	@Override
	public void onSurfaceCreated() {
		if (listener != null) {
			listener.onSurfaceCreated();
		}
	}

	@Override
	public void onSurfaceChanged() {
		if (listener != null) {
			listener.onSurfaceChanged();
		}
	}

	@Override
	public void onSurfaceError(String message) {
		if (listener != null) {
			listener.onSurfaceError();
		}
	}
}
