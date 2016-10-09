package com.blegos.watchout;

public class Constants {
	public static final String DEFAULT_PATH = "/Watchout/";
	public static final String PRODUCT_PACKAGE = "com.blegos.watchout";
	public static final int DEFAULT_VIEW_WIDTH = 80;
	public static final int DEFAULT_VIEW_HEIGHT = 110;
	public static final int ICON_VIEW_SIZE = 40;
	public static final String SETTING_ITEM = "gs_setting_item";
	public static final String SETTING_ITEM_SUB = "gs_setting_item_sub";
	
	public interface CONTROL {
		public interface ACTION {
			public static final int NONE = 0;
			public static final int CAPTURE = 1;
			public static final int SETTINGS = 2;
			public static final int ZOOM_IN = 3;
			public static final int ZOOM_OUT = 4;
			public static final int CLOSE_APPLICATION = 5;
		}
	}
}
