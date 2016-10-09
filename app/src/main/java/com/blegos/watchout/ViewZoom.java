package com.blegos.watchout;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

public class ViewZoom extends LinearLayout {
	public static final int ZOOM_IN = 0;
	public static final int ZOOM_OUT = 1;
	public static final int VIEW_SIZE = Constants.ICON_VIEW_SIZE;
	private RelativeLayout view;

	public ViewZoom(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public ViewZoom(Context context, int type) {
		super(context);
		
		int layout = R.layout.layout_zoom_in;
		if (type == ZOOM_OUT) {
			layout = R.layout.layout_zoom_out;
		}
		
		LayoutInflater  mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        view = (RelativeLayout)mInflater.inflate(layout, null, false);
        
        ViewGroup.LayoutParams params = new LayoutParams(Utility.convertDipToPx(getContext(), VIEW_SIZE), Utility.convertDipToPx(getContext(), VIEW_SIZE));
        super.addView(view, params);
	}
	
	public RelativeLayout getView() {
		return view;
	}
	
	public int getSize() {
		return Utility.convertDipToPx(getContext(), VIEW_SIZE);
	}
}
