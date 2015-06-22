/**
 * 文件名称 : DownloadThread.java
 * <p>
 * 作者信息 : liuzongyao
 * <p>
 * 创建时间 : 2012-9-20, 下午02:18:50
 * <p>
 * 版权声明 : Copyright (c) 2009-2012 HyDb Ltd. All rights reserved
 * <p>
 * 评审记录 :
 * <p>
 */
package com.szuwest.download.network;

import com.szuwest.download.constant.DownloadConfig;
import com.szuwest.download.domain.DownLoadFile;
import com.szuwest.download.util.XLLog;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;

public class DownloadThread extends Thread
{
    /**
     * 打印日志标志
     */
    private static final String TAG = "DownloadThread";

    private static final int BUFFER_SIZE = 256*1024;

    // 100 kb
    private static final long REFRESH_INTEVAL_SIZE = 100 * 1024;
    
    /**
     * 需保存文件
     */
    private File saveFile;
    
    /**
     * 下载url
     */
    private URL downUrl;
    
    /**
     * 下载文件长度
     */
    private long block;
    
    /**
     * 下载开始位置
     */
    private int threadId = -1;
    
    /**
     * 下载了多少
     */
    private long downLength;
    
    /**
     * 是否文成
     */
    private volatile boolean finish = false;
    
    /**
     * 文件下载器
     */
    private FileDownloader downloader;
    
    /**
     * 下载状态
     */
    private volatile boolean isCancel = false;
    
    /**
     * @param downloader
     *            :下载器
     * @param downUrl
     *            :下载地址
     * @param saveFile
     *            :下载路径
     * 
     */
    public DownloadThread(FileDownloader downloader, URL downUrl,
            File saveFile, long block, long downLength, int threadId)
    {
        this.downUrl = downUrl;
        this.saveFile = saveFile;
        this.block = block;
        this.downloader = downloader;
        this.threadId = threadId;
        this.downLength = downLength;
        this.isCancel = false;
    }
    
    @Override
    public void run()
    {
        if (downLength < block)
        {
            //未下载完成
            try
            {
                //使用Get方式下载
                HttpURLConnection http = (HttpURLConnection) downUrl.openConnection();

//                print(downUrl+"\n"+http);

                http.setConnectTimeout(DownloadConfig.CONNECTION_TIME);
                http.setRequestMethod(DownloadConfig.FILE_DOWN_METHOD);
                        http.setRequestProperty("Accept",
                                "image/gif, image/jpeg, image/pjpeg, image/pjpeg, application/x-shockwave-flash, application/xaml+xml, application/vnd.ms-xpsdocument, application/x-ms-xbap, application/x-ms-application, application/vnd.ms-excel, application/vnd.ms-powerpoint, application/msword, */*");
                http.setRequestProperty("Accept-Language", "zh-CN");
                http.setRequestProperty("Referer", downUrl.toString());
                http.setRequestProperty("Charset", "UTF-8");
                http.setRequestProperty("Accept-Encoding", "identity");

                long startPos = block * (threadId - 1) + downLength;//开始位置
                long endPos = block * threadId - 1;//结束位置
                //
                if (endPos > downloader.getFileSize()) endPos = downloader.getFileSize();

                http.setRequestProperty("Range", "bytes=" + startPos + "-"
                        + endPos);//设置获取实体数据的范围
                http.setRequestProperty("User-Agent",
                        "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.2; Trident/4.0; .NET CLR 1.1.4322; .NET CLR 2.0.50727; .NET CLR 3.0.04506.30; .NET CLR 3.0.4506.2152; .NET CLR 3.5.30729)");
                http.setRequestProperty("Connection", "Keep-Alive");
                
                InputStream inStream = http.getInputStream();
                byte[] buffer = new byte[BUFFER_SIZE];
                int offset = 0;
                print("Thread " + this.threadId + " start com.szuwest.download from position " + startPos);
                RandomAccessFile threadfile = new RandomAccessFile(this.saveFile, "rwd");
                threadfile.seek(startPos);

//                long lastLength = downLength;
                while (!isCancel && (offset = inStream.read(buffer, 0, BUFFER_SIZE)) != -1)
                {
                    threadfile.write(buffer, 0, offset);
                    downLength += offset;
                    downloader.append(offset);

                    //不必每次都更新数据库
//                    long tempSize = downLength - lastLength;
//                    if(tempSize > REFRESH_INTEVAL_SIZE) {
                        downloader.update(this.threadId, downLength);
//                        lastLength = downLength;
//                    }
                }

                threadfile.close();
                inStream.close();

                if (isCancel) {
                    print("Thread " + this.threadId + " cancel: com.szuwest.download length=" + downLength);
                    //
                    if (downLength > 0) downloader.update(this.threadId, downLength);
                } else {
                    print("Thread " + this.threadId + " com.szuwest.download finish");
                    this.finish = true;
                }
            }
            catch (Exception e)
            {
                if (e instanceof FileNotFoundException) {
                    downloader.getDownLoadFile().setFailCode(DownLoadFile.ERROR_FILE_NOTFOUND);
                } else if ( e instanceof ConnectException || e instanceof SocketException) {
                    downloader.getDownLoadFile().setFailCode(DownLoadFile.ERROR_NETWORK);
                } else if (e instanceof IOException) {
                    if (e.getMessage().contains("ENOSPC")) {
                        downloader.getDownLoadFile().setFailCode(DownLoadFile.ERROR_NOT_SPACE);
                    }
                }
                e.printStackTrace();
                this.downLength = -1;
                print("Thread " + this.threadId + " error:" + e.getMessage());
            }
        } else {
            finish = true;
        }
    }
    
    /**
     * 打印日志信息
     * 
     * @param msg
     */
    private static void print(String msg)
    {
        XLLog.i(TAG, msg);
    }
    
    /**
     * 下载是否完成
     * 
     * @return
     */
    public boolean isFinish()
    {
        return finish;
    }
    
    /**
     * 已经下载的内容大小
     *
     * @return if faile return -1
     */
    public long getDownLength()
    {
        return downLength;
    }

    /**
     * cancel com.szuwest.download
     */
    public void cancel()
    {
        this.isCancel = true;
    }

    public boolean isCancel()
    {
        return isCancel;
    }
}
