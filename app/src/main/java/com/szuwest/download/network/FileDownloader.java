/**
 * 文件名称 : FileDownloader.java
 * <p>
 * 作者信息 : liuzongyao
 * <p>
 * 创建时间 : 2012-9-7, 上午11:46:28
 * <p>
 * 版权声明 : Copyright (c) 2009-2012 Hydb Ltd. All rights reserved
 * <p>
 * 评审记录 :
 * <p>
 */

package com.szuwest.download.network;

import android.os.Build;
import android.text.TextUtils;

import com.szuwest.download.DownloadManager;
import com.szuwest.download.constant.DownloadConfig;
import com.szuwest.download.domain.DownLoadFile;
import com.szuwest.download.util.DeviceHelper;
import com.szuwest.download.util.FileUtil;
import com.szuwest.download.util.XLLog;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class FileDownloader implements Runnable
{
    private static final String TAG = "FileDownloader";
    private static final String TMP = ".dltmp";

    private static final int RETRY_TIME = 0;

    private File saveFile;
    
    /**
     * 已下载文件长度
     */
    private volatile long downloadSize = 0;
    
    /**
     * 原始文件长度
     */
    private long fileSize = 0;
    
    /**
     * 线程数
     */
    private DownloadThread[] threads;
    
    
    /**
     * 缓存各线程下载的长度
     */
    private Map<Integer, Long> data = new ConcurrentHashMap<Integer, Long>();
    
    /**
     * 每条线程下载的长度
     */
    private long block;

    private DownLoadFile downLoadFile;

    private boolean isInitial = false;

    private volatile boolean isCancel = false;

    private int threadNum = DownloadConfig.THREAD_NUM;

    private DownloadManager downloadManager;

    public void setUrlCallback(DownloadManager.UrlCallback urlCallback) {
        this.urlCallback = urlCallback;
    }

    private DownloadManager.UrlCallback urlCallback;

    /**
     * 构建文件下载器
     * @param downLoadFile 需要下载的文件
     * @param threadNum 分多少个线程下载
     */
    public FileDownloader(DownloadManager downloadManager, DownLoadFile downLoadFile, int threadNum)
    {
        this.downloadManager = downloadManager;
        this.downLoadFile = downLoadFile;
        this.threadNum = threadNum;

//        isInitial = initial(threadNum);
    }

    private String getDownloadUrl() {
        return downLoadFile.getFileUrl();
    }

    @Override
    public void run() {
        downLoadFile.setState(DownLoadFile.DOWNSTAT_DOWNLOAD);
        downloadManager.onDownloadStart(this);
        try {
            download();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 设定好各种参数
     */
    private boolean initial()
    {
        XLLog.d(TAG, "initial");
        String fileStr = downLoadFile.getSavePath();

        File fileSaveDir = new File(fileStr);

        if (!fileSaveDir.exists())
        {
            if (!fileSaveDir.mkdirs())
            {
                downLoadFile.setFailCode(DownLoadFile.ERROR_MKDIR);
                return false;
            }
        }

        if (downLoadFile.getTotalLength() <= 0 || TextUtils.isEmpty(downLoadFile.getFileName()))
        {
            boolean ret = getFileSizeAndName();
            if (!ret) return false;
        }

        this.fileSize = downLoadFile.getTotalLength();

        //构建保存文件
//        this.saveFile = new File(fileSaveDir, downLoadFile.getFileName());
        //先下载为临时文件，下载完成后重命名
        this.saveFile = new File(fileSaveDir, downLoadFile.getFileName() + TMP);

        Map<Integer, Long> logdata = downLoadFile.getDownLoadTmpLogMap();//获取下载记录

        if (logdata.size() > 0)
        {
            this.threads = new DownloadThread[logdata.size()];
            //如果存在下载记录
            for (Map.Entry<Integer, Long> entry : logdata.entrySet())
                data.put(entry.getKey(), entry.getValue());//把各条线程已经下载的数据长度放入data中
        }
        else
        {
            this.threads = new DownloadThread[this.threadNum];
        }

        this.downloadSize = 0;
        if (this.data.size() == this.threads.length)
        {
            //下面计算所有线程已经下载的数据长度
            for (int i = 0; i < this.threads.length; i++)
            {
                this.downloadSize += this.data.get(i + 1);
            }

            downLoadFile.updateCompleSize(this.downloadSize);

            print("已经下载的长度" + this.downloadSize);
        }

        //计算每条线程下载的数据长度, 不能整除的情况+1，在真正下载的时候，最后一个线程会限制不超出
        this.block = (this.fileSize % this.threads.length) == 0 ? this.fileSize / this.threads.length : this.fileSize / this.threads.length + 1;

        return true;
    }

    /**
     * 累计已下载大小
     *
     * @param size
     */
    protected synchronized void append(long size)
    {
        downloadSize += size;
    }

    /**
     * 更新指定线程最后下载的位置
     *
     * @param threadId
     *            线程id
     * @param pos
     *            最后下载的位置
     */
    protected synchronized void update(int threadId, long pos)
    {
        this.data.put(threadId, pos);
        downLoadFile.updateDownLoadTmpLogs(data);
    }

    public void cancel(boolean isCancel)
    {
        if (isCancel && threads != null) {
            //取消分块线程下载
            for (DownloadThread thread : threads) {
                if (thread != null) thread.cancel();
            }
            downLoadFile.setState(DownLoadFile.DOWNSTAT_PAUSE);
            downloadManager.onDownloadCancel(this);
        }
        this.isCancel = isCancel;
    }

    private boolean getFileSizeAndName() {
        XLLog.d(TAG, "getFileSizeAndName");
        try {
            URL url = new URL(getDownloadUrl());

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(DownloadConfig.CONNECTION_TIME);
            conn.setRequestMethod(DownloadConfig.FILE_DOWN_METHOD);
            conn.setRequestProperty("Accept",
                    "image/gif, image/jpeg, image/pjpeg, image/pjpeg, application/x-shockwave-flash, application/xaml+xml, application/vnd.ms-xpsdocument, application/x-ms-xbap, application/x-ms-application, application/vnd.ms-excel, application/vnd.ms-powerpoint, application/msword, */*");
            conn.setRequestProperty("Accept-Language", "zh-CN");
            conn.setRequestProperty("Referer", getDownloadUrl());
            conn.setRequestProperty("Charset", "UTF-8");
            conn.setRequestProperty("User-Agent",
                    "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.2; Trident/4.0; .NET CLR 1.1.4322; .NET CLR 2.0.50727; .NET CLR 3.0.04506.30; .NET CLR 3.0.4506.2152; .NET CLR 3.5.30729)");
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("Accept-Encoding", "identity");
            conn.connect();
            printResponseHeader(conn);

            if (conn.getResponseCode() == 200)
            {
                //因为conn.getContentLength()返回的是int类型，如果文件大小超过int范围会返回-1.所以自己解析比较靠谱
                String contenLength = conn.getHeaderField("Content-Length");
                if (!TextUtils.isEmpty(contenLength)) {
                    this.fileSize = Long.parseLong(contenLength);
                } else {
                    this.fileSize = conn.getContentLength();//根据响应获取文件大小
                }

                if (this.fileSize <= 0) {
                    downLoadFile.setFailCode(DownLoadFile.ERROR_GET_FILESIZE);
                    return false;
                } else {
                    downLoadFile.updateFileLength(this.fileSize);
                }
                String name = downLoadFile.getFileName();
                if (TextUtils.isEmpty(name)) {
                    downLoadFile.updateFileName(getFileName(conn));
                }
                return true;
            }
            else
            {
                downLoadFile.setFailCode(DownLoadFile.ERROR_NETWORK);

            }
        }
        catch (Exception e)
        {
            if (e instanceof FileNotFoundException) {
                downLoadFile.setFailCode(DownLoadFile.ERROR_FILE_NOTFOUND);
            } else if ( e instanceof ConnectException || e instanceof SocketException) {
                downLoadFile.setFailCode(DownLoadFile.ERROR_NETWORK);
            }  else {
                downLoadFile.setFailCode(DownLoadFile.ERROR_UNKNOWN);
            }
            print(e.toString());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 开始下载文件
     * @return 已下载文件大小
     * @throws Exception
     */
    private long download() throws Exception {
        XLLog.d(TAG, "com/szuwest/download");
        if (isCancel) {
            XLLog.d(TAG, "cancel");
            downLoadFile.setState(DownLoadFile.DOWNSTAT_PAUSE);
            downloadManager.onDownloadCancel(this);
            return 0;
        }

        if (!isInitial) {
            isInitial = initial();
            if (!isInitial) {
                downLoadFile.setState(DownLoadFile.DOWNSTAT_FAIL);
                XLLog.d(TAG, "initial failed");
                downloadManager.onDownloadFail(this);
                return 0;
            }
        }

        if (!DeviceHelper.isSDCardExist()) {
            downLoadFile.setFailCode(DownLoadFile.ERROR_NO_SD);
            downLoadFile.setState(DownLoadFile.DOWNSTAT_FAIL);
            XLLog.d(TAG, "no sd card");
            downloadManager.onDownloadFail(this);
            return 0;
        }

        File file2Download = new File(downLoadFile.getSavePath(), downLoadFile.getFileName());
        if (file2Download.exists()) {
            if (file2Download.length() == this.fileSize) {
                this.downloadSize = this.fileSize;
                downLoadFile.updateCompleSize(this.downloadSize);
                downLoadFile.setState(DownLoadFile.DOWNSTAT_FINISH);
                downloadManager.onDownloadSuccess(this);
                return downloadSize;
            }
            //存在但是大小不一样，删除掉，重新下。
            file2Download.delete();
        }

        //判断是否够空间下载
        if (!saveFile.exists() && !DownloadManager.checkSDEnoughSize(this.fileSize)) {
            downLoadFile.setFailCode(DownLoadFile.ERROR_NOT_SPACE);
            downLoadFile.setState(DownLoadFile.DOWNSTAT_FAIL);
            XLLog.d(TAG, "sd card not enought space!");
            downloadManager.onDownloadFail(this);
            return 0;
        }

        try
        {
            //创建临时文件
            //android 4.4 系统有bug，当文件大小为超过2G的时候（4G没问题），RandomAccessFile.setLength 会抛异常
            //所有采取别的方式写
            if (Build.VERSION.SDK_INT == 19 && this.fileSize > Integer.MAX_VALUE) {
                if (!(this.saveFile.exists() && this.saveFile.length() == this.fileSize)) {//如果已存在，就不必再创建了，否则会把数据抹掉
                    boolean ret = FileUtil.createFile(this.saveFile, this.fileSize);
                    if (!ret) {
                        downLoadFile.setFailCode(DownLoadFile.ERROR_UNKNOWN);
                        downLoadFile.setState(DownLoadFile.DOWNSTAT_FAIL);
                        downloadManager.onDownloadFail(this);
                        return 0;
                    }
                }
            } else {
                RandomAccessFile randOut = new RandomAccessFile(this.saveFile, "rw");
                if (this.fileSize > 0) {
                    randOut.setLength(this.fileSize);
                }
                randOut.close();
            }

            URL url = new URL(getDownloadUrl());
            if (this.data.size() != this.threads.length)
            {
                this.data.clear();
                
                for (int i = 0; i < this.threads.length; i++)
                {
                    this.data.put(i + 1, 0l);//初始化每条线程已经下载的数据长度为0
                }
            }
            for (int i = 0; i < this.threads.length; i++)
            {
                
                //开启线程进行下载
                long downLength = this.data.get(i + 1);
                
                if (downLength < this.block
                        && this.downloadSize < this.fileSize)
                {
                    //判断线程是否已经完成下载,否则继续下载
                    this.threads[i] = new DownloadThread(this, url,
                            this.saveFile, this.block, this.data.get(i + 1),
                            i + 1);
                    this.threads[i].setPriority(Thread.MIN_PRIORITY);
                    this.threads[i].start();
                }
                else
                {
                    this.threads[i] = null;
                }
            }

            downLoadFile.saveDownLoadTmpLogs(this.data);

            boolean notFinish = true;//下载未完成
            int retryCount = 0;
            while (notFinish && !isCancel)
            {
                // 循环判断所有线程是否完成下载
                Thread.sleep(900);
                notFinish = false;//假定全部线程下载完成

                for (int i = 0; i < this.threads.length; i++)
                {
                    if (this.threads[i] != null && !this.threads[i].isFinish() && !this.threads[i].isCancel())
                    {
                        
                        //如果发现线程未完成下载
                        notFinish = true;//设置标志为下载没有完成

                        //如果发现线程下载失败，重试
                        if (this.threads[i].getDownLength() == -1)
                        {
                            if (retryCount < RETRY_TIME)
                            {
                                retryCount++;
                                XLLog.d(TAG, "thread " + (i + 1) + "failed, restart");
                                //如果下载失败,再重新下载
                                this.threads[i] = new DownloadThread(this, url,
                                        this.saveFile, this.block,
                                        this.data.get(i + 1), i + 1);
                                this.threads[i].setPriority(Thread.MIN_PRIORITY);
                                this.threads[i].start();
                            }
                            else
                            {
                                this.threads[i].cancel();
                            }
                        }
                    }
                }

                downLoadFile.updateCompleSize(downloadSize);
                downloadManager.onDownloadProgress(this);
//                if (listener != null) listener.onDownloadUpdate(this.downLoadFile, this.downloadSize);//通知目前已经下载完成的数据长度
            }

            if (isCancel)
            {
                threads = null;
                isInitial = false;
                XLLog.d(TAG, "cancel already");
                downLoadFile.setState(DownLoadFile.DOWNSTAT_PAUSE);
                downloadManager.onDownloadCancel(this);
            }
            else if (downLoadFile.getCompletelength() >= downLoadFile.getTotalLength())
            {
                XLLog.d(TAG, "finish, downloadSize=" + downloadSize);
                //重命名文件
                File realFile = new File(downLoadFile.getSavePath(), downLoadFile.getFileName());
                this.saveFile.renameTo(realFile);
                downLoadFile.deleteDownLoadTmpLogs();
                downLoadFile.setState(DownLoadFile.DOWNSTAT_FINISH);
                downloadManager.onDownloadSuccess(this);
            }
            else
            {
                if (downLoadFile.getFailCode() == 0) downLoadFile.setFailCode(DownLoadFile.ERROR_UNKNOWN);
                downLoadFile.setState(DownLoadFile.DOWNSTAT_FAIL);
                threads = null;
                isInitial = false;
                downloadManager.onDownloadFail(this);
            }

        }
        catch (Exception e)
        {
            if (!DeviceHelper.isSDCardExist()) downLoadFile.setFailCode(DownLoadFile.ERROR_NO_SD);
            if (downLoadFile.getFailCode() == 0) downLoadFile.setFailCode(DownLoadFile.ERROR_UNKNOWN);
            downLoadFile.setState(DownLoadFile.DOWNSTAT_FAIL);
            threads = null;
            isInitial = false;
            downloadManager.onDownloadFail(this);
            e.printStackTrace();
            print(e.toString());
        }

        return this.downloadSize;
    }


    /**
     * 获取文件名
     * @param conn
     * @return filename
     */
    private String getFileName(HttpURLConnection conn) {

        String filename = this.downLoadFile.getFileUrl().substring(this.downLoadFile.getFileUrl().lastIndexOf('/') + 1);

        if(TextUtils.isEmpty(filename)){//如果获取不到文件名称
            for (int i = 0;; i++) {
                String mine = conn.getHeaderField(i);

                if (mine == null) break;

                if("content-disposition".equals(conn.getHeaderFieldKey(i).toLowerCase())){
                    Matcher m = Pattern.compile(".*filename=(.*)").matcher(mine.toLowerCase());
                    if(m.find()) return m.group(1);
                }
            }

            filename = UUID.randomUUID()+ ".tmp";//默认取一个文件名
        }

        return filename;
    }

    /**
     * 获取Http响应头字段
     * 
     * @param http
     * @return
     */
    private static Map<String, String> getHttpResponseHeader(HttpURLConnection http)
    {
        Map<String, String> header = new LinkedHashMap<String, String>();
        
        for (int i = 0;; i++)
        {
            String mine = http.getHeaderField(i);
            if (mine == null)
                break;
            header.put(http.getHeaderFieldKey(i), mine);
        }
        
        return header;
    }
    
    /**
     * 打印Http头字段
     * 
     * @param http
     */
    private static void printResponseHeader(HttpURLConnection http)
    {
        Map<String, String> header = getHttpResponseHeader(http);
        
        for (Map.Entry<String, String> entry : header.entrySet())
        {
            String key = entry.getKey() != null ? entry.getKey() + ":" : "";
            print(key + entry.getValue());
        }
    }

    /**
     * 获取文件大小
     *
     * @return
     */
    public long getFileSize()
    {
        return fileSize;
    }

    public File getSaveFile()
    {
        return saveFile;
    }

    public void setSaveFile(File saveFile)
    {
        this.saveFile = saveFile;
    }

    private static void print(String msg)
    {
        XLLog.i(TAG, msg);
    }

    public DownLoadFile getDownLoadFile() {
        return downLoadFile;
    }

    public static void deleteDltmpFile(DownLoadFile downLoadFile) {
        File dltmp = new File(downLoadFile.getSavePath(), downLoadFile.getFileName() + TMP);
        if (dltmp.exists()) dltmp.delete();
    }
}