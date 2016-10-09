package com.blegos.watchout;

import java.io.IOException;
import java.util.List;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class CameraSurfaceView extends SurfaceView implements
		SurfaceHolder.Callback {
	public interface CameraSurfaceViewListener {
		void onSurfaceCreated();
		void onSurfaceChanged();
		void onSurfaceError(String message);
	}
	
	private CameraSurfaceViewListener listener;
	
	private SurfaceHolder holder;
	private Camera camera;
	private Parameters cameraParameters;
	private byte[] frameData;
	
	public CameraSurfaceView(Context context) {
		this(context, null);
	}

	public CameraSurfaceView(Context context, CameraSurfaceViewListener listener) {
		super(context);

		// Initiate the Surface Holder properly
		this.listener = listener;
		this.holder = this.getHolder();
		this.holder.addCallback(this);
		this.holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		Log.d(VIEW_LOG_TAG, "## onSurfaceCreated");
		try {
			// Open the Camera in preview mode
			this.camera = Camera.open();
			this.camera.setPreviewDisplay(this.holder);
			
			//if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
				this.camera.setDisplayOrientation(ViewTopService.getRotationDegree());
			//} else {
			//	this.camera.setDisplayOrientation(90);
			//}
			
			cameraParameters = camera.getParameters();
			
			if (listener != null) {
				listener.onSurfaceCreated();
			}
		} catch (Exception e) {
			e.printStackTrace();
			if (listener != null) {
				listener.onSurfaceError(e.getMessage());
			}
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		Log.d(VIEW_LOG_TAG, "## onSurfaceChanged");
		if (holder.getSurface() == null) {
			return;
		}

		// stop preview before making changes
		try {
			camera.stopPreview();
		} catch (Exception e) {
		}
		
		if (camera != null && cameraParameters != null) {
			List<Size> list = cameraParameters.getSupportedPictureSizes();
			int max = Integer.MIN_VALUE;
			Size maxSize = null;
			for (Size size:list) {
				if ((size.width * size.height) > max) {
					maxSize = size;
					max = (size.width * size.height);
				}
			}
			
			//cameraParameters.setPreviewSize(width, height);
			int zoom = Preference.getInstance().loadDataInt(getContext(), Preference.PREF_ZOOM_SCALE);
			
			cameraParameters.setPreviewFormat(ImageFormat.NV21);
			cameraParameters.setPictureFormat(ImageFormat.JPEG);
			cameraParameters.setJpegQuality(100);
			cameraParameters.setZoom(zoom);
			
			if (cameraParameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
				cameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
			}
			if (maxSize != null) {
				cameraParameters.setPictureSize(maxSize.width, maxSize.height);
			}
			Log.d(VIEW_LOG_TAG, "params:" + cameraParameters.getPictureSize().width + "x" + cameraParameters.getPictureSize().height);
			
			try {
				camera.setParameters(cameraParameters);
			} catch (RuntimeException e) {
				Log.e(VIEW_LOG_TAG, e.getMessage());
				e.printStackTrace();
			}
			
			try {
				camera.setPreviewDisplay(holder);
				camera.startPreview();
				if (listener != null) {
					listener.onSurfaceChanged();
				}
			} catch (IOException e) {
				e.printStackTrace();
				if (listener != null) {
					listener.onSurfaceError(e.getMessage());
				}
			}
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.d(VIEW_LOG_TAG, "## onSurfaceDestroyed");
		if (camera != null) {
			camera.stopPreview();
			camera.cancelAutoFocus();
			camera.release();
		}
	}

	public Camera getCamera() {
		return this.camera;
	}
	
	public Parameters getCameraParameters() {
		return this.cameraParameters;
	}

	public byte[] getFrameData() {
		return frameData;
	}
}
