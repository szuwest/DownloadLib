package com.szuwest.download.util;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Environment;
import android.os.Looper;
import android.os.StatFs;
import android.util.DisplayMetrics;
import android.view.WindowManager;


import java.io.File;

public abstract class DeviceHelper {
	
	//Screen
	public static int getScreenWidth(Context c) {
		DisplayMetrics dm = new DisplayMetrics(); 
    	WindowManager wm = (WindowManager)c.getSystemService(Context.WINDOW_SERVICE);
    	wm.getDefaultDisplay().getMetrics(dm);
    	return dm.widthPixels;
	}
	
	public static int getScreenHeight(Context c) {
		DisplayMetrics dm = new DisplayMetrics(); 
    	WindowManager wm = (WindowManager)c.getSystemService(Context.WINDOW_SERVICE);
    	wm.getDefaultDisplay().getMetrics(dm);
    	return dm.heightPixels;
	}
	
	//Memeroy
	public static long getAvailableExternalMemorySize() {
		File path = Environment.getExternalStorageDirectory();
		StatFs stat = new StatFs(path.getPath());
		long blockSize = stat.getBlockSize();
		long availableBlocks = stat.getAvailableBlocks();
		return availableBlocks * blockSize;
	}

	public static long getTotalExternalMemorySize() {
		File path = Environment.getExternalStorageDirectory();
		Environment.getExternalStorageState();
		StatFs stat = new StatFs(path.getPath());
		long blockSize = stat.getBlockSize();
		long totalBlocks = stat.getBlockCount();
		return totalBlocks * blockSize;
	}
	
	//SDCard
	public static String getSDCardDir() {
		String sdcardPath = Environment.getExternalStorageDirectory().getPath();
		if(null != sdcardPath && !sdcardPath.endsWith("/")) {
			sdcardPath = sdcardPath + "/";
		}
		return sdcardPath;
	}

	public static boolean isSDCardExist() {
		return Environment.MEDIA_MOUNTED.equalsIgnoreCase(Environment
				.getExternalStorageState());
	}

    public static boolean isInMainThread() {
        Looper myLooper = Looper.myLooper();
        Looper mainLooper = Looper.getMainLooper();
        return myLooper == mainLooper;
    }

    /**
     * 判断是否是平板（google 官方做法）
     * @param context
     * @return
     */
    public static boolean isTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

}
