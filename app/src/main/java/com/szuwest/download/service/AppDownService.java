/**
 * 文件名称 : AppDownService.java
 * <p>
 * 作者信息 : liuzongyao
 * <p>
 * 创建时间 : 2012-11-13, 下午2:56:17
 * <p>
 * 版权声明 : Copyright (c) 2009-2012 Hydb Ltd. All rights reserved
 * <p>
 * 评审记录 :
 * <p>
 */

package com.szuwest.download.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import com.szuwest.download.DownloadManager;
import com.szuwest.download.domain.DownLoadFile;


/**
 * 请在这里增加文件描述
 * <p>
 */
public class AppDownService extends Service
{
    
    //启动
    public static void start(Context context)
    {
        Intent intent = new Intent();
        intent.setClass(context, AppDownService.class);
        context.startService(intent);
    }
    
    public static void stop(Context context)
    {
        Intent intent = new Intent();
        intent.setClass(context, AppDownService.class);
        context.stopService(intent);
    }
    
    /**
     * 
     * @param context
     * @param downLoadFile
     *            DownLoadFile.DOWNSTAT_PAUSE
     *            DownLoadFile.DOWNSTAT_DELETE
     * @param operate
     */
    public static void start(Context context, DownLoadFile downLoadFile, int operate)
    {
        Intent intent = new Intent(context, AppDownService.class);
        intent.putExtra("operater", operate);
        intent.putExtra("downloadApp", downLoadFile);
        context.startService(intent);
    }
    
    @Override
    public void onCreate()
    {
        super.onCreate();
        DownloadManager.getInstance().init();
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        
        //需要判断是否第一次加载， 如果不是需要加载历史记录
        if (intent != null)
        {
            int operater = intent.getIntExtra("operater", -1);
            
            DownLoadFile downLoadFile = (DownLoadFile) intent.getSerializableExtra("downloadApp");
            
            //下载
            if (operater == DownLoadFile.DOWNSTAT_DOWNLOAD)
            {
                DownloadManager.getInstance().downloadFile(this, downLoadFile);
            }
            
            //暂停
            else if (operater == DownLoadFile.DOWNSTAT_PAUSE)
            {
                DownloadManager.getInstance().pauseTask(downLoadFile);
            }
            
        }
        
        return super.onStartCommand(intent, flags, startId);
    }
    
    @Override
    public void onDestroy()
    {
        DownloadManager.getInstance().close();
        super.onDestroy();
    }
}
