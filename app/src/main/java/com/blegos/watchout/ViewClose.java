package com.blegos.watchout;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

public class ViewClose extends LinearLayout {
	public static final int VIEW_SIZE = Constants.ICON_VIEW_SIZE;
	private RelativeLayout view;

	public ViewClose(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public ViewClose(Context context) {
		super(context);
		
		LayoutInflater  mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        view = (RelativeLayout)mInflater.inflate(R.layout.layout_close, null, false);
        
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
