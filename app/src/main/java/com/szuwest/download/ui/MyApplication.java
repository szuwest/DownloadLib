package com.szuwest.download.ui;

import com.szuwest.download.service.AppDownService;

import org.litepal.LitePalApplication;

/**
 * Created by west on 15/6/22.
 */
public class MyApplication extends LitePalApplication {

    @Override
    public void onCreate() {
        super.onCreate();
        AppDownService.start(this);
    }

}
