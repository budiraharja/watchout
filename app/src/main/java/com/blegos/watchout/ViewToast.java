package com.blegos.watchout;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class ViewToast extends LinearLayout {
	private RelativeLayout view;
	private TextView text;

	public ViewToast(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public ViewToast(Context context) {
		super(context);
		
		LayoutInflater  mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        view = (RelativeLayout)mInflater.inflate(R.layout.layout_toast, null, false);
        text = (TextView) view.findViewById(R.id.tv_toast_text);
        
        super.addView(view);
	}
	
	public RelativeLayout getView() {
		return view;
	}

	public TextView getTextView() {
		return text;
	}
	
	public void setText(String message) {
		if (text != null && text.getText() != null) {
			text.setText(message);
		}
	}
}
