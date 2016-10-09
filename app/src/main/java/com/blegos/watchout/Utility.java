package com.blegos.watchout;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.DecelerateInterpolator;

public class Utility {
	public static Animation getAnimationFadeIn(int durationMilliseconds) {
		Animation animation = new AlphaAnimation(0, 1);
		animation.setInterpolator(new DecelerateInterpolator());
		animation.setStartOffset(durationMilliseconds);
		animation.setDuration(durationMilliseconds);
		
		return animation;
	}
	
	public static Animation getAnimationFadeIn() {
		return getAnimationFadeIn(1000);
	}
	
	public static Animation getAnimationFadeInVisible(final View view, int durationMilliseconds) {
		Animation animation = getAnimationFadeIn(durationMilliseconds);
		animation.setAnimationListener(new AnimationListener() {
			@Override
			public void onAnimationEnd(Animation animation) {
				view.setVisibility(View.VISIBLE);
			}

			@Override
			public void onAnimationRepeat(Animation animation) {
			}

			@Override
			public void onAnimationStart(Animation animation) {
			}
		});
		
		return animation;
	}
	
	public static Animation getAnimationFadeOut(int durationMilliseconds) {
		Animation animation = new AlphaAnimation(1, 0);
		animation.setInterpolator(new AccelerateInterpolator()); 
		animation.setStartOffset(durationMilliseconds);
		animation.setDuration(durationMilliseconds);
		
		return animation;
	}
	
	public static Animation getAnimationFadeOut() {
		return getAnimationFadeOut(1000);
	}
	
	public static Animation getAnimationFadeOutGone(final View view, int durationMilliseconds) {
		Animation animation = getAnimationFadeOut(durationMilliseconds);
		animation.setAnimationListener(new AnimationListener() {
			@Override
			public void onAnimationEnd(Animation animation) {
				view.setVisibility(View.GONE);
			}

			@Override
			public void onAnimationRepeat(Animation animation) {
			}

			@Override
			public void onAnimationStart(Animation animation) {
			}
		});
		
		return animation;
	}
	
	public static DisplayMetrics getDisplayMetrics(Context context) {
		return context.getResources().getDisplayMetrics();
	}
	
	public static int convertDipToPx(Context context, int dip) {
		DisplayMetrics metrics = getDisplayMetrics(context);
		int px = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, metrics);
		
		return px;
	}
	
	public static boolean isServiceRunning(Activity context) {
		boolean isRunning = false;
		
	    ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
	    for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
	        if (ViewTopService.class.getName().equals(service.service.getClassName())) {
	            isRunning = true;
	            break;
	        }
	    }
	    
	    return isRunning;
	}
	
	public static void stopService(Activity context) {
		if (isServiceRunning(context)) {
			Intent intent = new Intent(ViewTopService.CAMERA_PREVIEW_SERVICE);
			context.getApplicationContext().stopService(intent);
		}
	}
	
	public static void startService(Activity context) {
		if (!isServiceRunning(context)) {
			Intent intent = new Intent(ViewTopService.CAMERA_PREVIEW_SERVICE);
			context.getApplicationContext().startService(intent);
		}
	}
}
