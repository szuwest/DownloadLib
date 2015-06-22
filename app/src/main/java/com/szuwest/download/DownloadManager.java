package com.szuwest.download;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import com.szuwest.download.constant.DownloadConfig;
import com.szuwest.download.domain.DownLoadFile;
import com.szuwest.download.network.DownloadListener;
import com.szuwest.download.network.FileDownloader;
import com.szuwest.download.util.DeviceHelper;
import com.szuwest.download.util.FileTypeUtil;
import com.szuwest.download.util.FileUtil;
import com.szuwest.download.util.XLLog;


import org.litepal.crud.DataSupport;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/**
 * 下载管理类
 * Created by west on 15/5/26.
 */
public class DownloadManager {

    public static interface UrlCallback {
        /**
         * 如果url跟当前连接的设备ip地址不一样，返回修改过IP地址的URL
         * @param url
         * @return 如果url跟当前连接的设备ip地址不一样，返回修改过IP地址的URL
         */
        public String getRealUrl(String url);
    }

    private static class SingleTonHolder {
        private final static DownloadManager instance = new DownloadManager();
    }

    private static final String TAG = DownloadManager.class.getSimpleName();
    /**
     * SD卡需要额外的空间预留
     */
    private static final long EXTRA_SPACE_REMAIN = 1024*1024 * 20;

    public static DownloadManager getInstance() {
        return SingleTonHolder.instance;
    }

    private ExecutorService pool;
    private Handler mainHandler;
    private HandlerThread handlerThread;
    private Handler threadHandler;

    private final List<DownLoadFile> allFilesList = new LinkedList<DownLoadFile>();

    private final List<DownloadListener> listeners = new LinkedList<DownloadListener>();

    /**
     * 等待中和下载中的任务map
     */
    private Map<String, FileDownloader> downloadingMap = new ConcurrentHashMap<String, FileDownloader>();
    //等待中任务列表
    private List<FileDownloader> waitingList = new LinkedList<FileDownloader>();

    private boolean isInit = false;
    private volatile boolean isLoaded = false;

    private DownloadManager() {
        mainHandler = new Handler(Looper.getMainLooper());
        pool = Executors.newFixedThreadPool(DownloadConfig.THREAD_POOL_NUM);
        handlerThread = new HandlerThread("HelperThread");
        handlerThread.start();
        threadHandler = new Handler(handlerThread.getLooper());
    }

    public void init() {
        if (!isInit) {
            XLLog.d(TAG, "init");
            threadHandler.post(new Runnable() {
                @Override
                public void run() {
                    loadFiles();
                }
            });

            isInit = true;
        }
    }

    private void loadFiles() {
        synchronized (allFilesList) {
            allFilesList.addAll(DataSupport.findAll(DownLoadFile.class));
        }
        XLLog.d(TAG, "load com.szuwest.download tasks " + allFilesList.size());

        for (DownLoadFile downloadFile : allFilesList) {
            int state = downloadFile.getState();
            if (state == DownLoadFile.DOWNSTAT_DOWNLOAD || state == DownLoadFile.DOWNSTAT_WAIT) {
                FileDownloader loader = new FileDownloader(this, downloadFile, DownloadConfig.THREAD_NUM);
                if (state == DownLoadFile.DOWNSTAT_DOWNLOAD) {
                    downloadFile.updateState(DownLoadFile.DOWNSTAT_WAIT);
                }
                downloadingMap.put(downloadFile.getFileUrl(), loader);
                pool.submit(loader);
            }
        }

        isLoaded = true;
    }

    public boolean downloadFile(Context context, String url) {
        return newDownload(context, url) != null;
    }

    // 返回一个DownLoadFile从而可以直接观察下载状态
    public DownLoadFile newDownload(Context context, String url) {
        if (!DeviceHelper.isSDCardExist()) {
//            if (context instanceof Activity) {
//                Toast.makeText(context, R.string.sdcard_no, Toast.LENGTH_SHORT).show();
//            }
            XLLog.d(TAG, "no sdcard");
            return null;
        }

        DownLoadFile task = getDownloadFile(url);
        if (task != null) {
//            if (context instanceof Activity) {
//                Toast.makeText(context, R.string.download_task_exist, Toast.LENGTH_SHORT).show();
//            }
            return null;
        }

        task = new DownLoadFile();
        task.setFileUrl(url);
        task.setFileName(FileUtil.getFileNameByUrl(url));
        task.setSavePath(DownloadConfig.getDefaultSavePath());
        task.setType(FileTypeUtil.getFileType(task.getFileName()));
        task.setState(DownLoadFile.DOWNSTAT_WAIT);
        task.setCreateTime(System.currentTimeMillis() / 1000);
        final DownLoadFile finalTask = task;
        threadHandler.post(new Runnable() {
            @Override
            public void run() {
                finalTask.save();
            }
        });
        downloadFile(finalTask);
        return task;
    }

    public boolean downloadFile(Context context, final DownLoadFile downLoadFile) {
        if (!DeviceHelper.isSDCardExist()) {
//            if (context instanceof Activity) {
//                Toast.makeText(context, R.string.sdcard_no, Toast.LENGTH_SHORT).show();
//            }
            XLLog.d(TAG, "no sdcard");
            return false;
        }
        DownLoadFile task = getDownloadFile(downLoadFile.getFileUrl());
        if (task != null) {
//            if (context instanceof Activity) {
//                Toast.makeText(context, R.string.download_task_exist, Toast.LENGTH_SHORT).show();
//            }
            return false;
        }
        downLoadFile.setState(DownLoadFile.DOWNSTAT_WAIT);
        threadHandler.post(new Runnable() {
            @Override
            public void run() {
                downLoadFile.save();
            }
        });
        downloadFile(downLoadFile);

        return true;
    }

    public boolean pauseTask(final DownLoadFile downloadFile) {
        XLLog.d(TAG, "pause com.szuwest.download url=" + downloadFile.getFileUrl());

        FileDownloader fileDownloader = downloadingMap.get(downloadFile.getFileUrl());
        if (fileDownloader != null) {
            fileDownloader.cancel(true);
        }

        downloadFile.setState(DownLoadFile.DOWNSTAT_PAUSE);

        return true;
    }

    public boolean resumeTask(DownLoadFile downloadFile) {
        DownLoadFile task = getDownloadFile(downloadFile.getFileUrl());
        if (task != null) {
            downloadFile(task);
        }
        return true;
    }

    public void deleteDownload(DownLoadFile downloadFile, boolean deleteSource) {
        XLLog.d(TAG, "delete file=" + downloadFile.getFileUrl());

        DownLoadFile file = getDownloadFile(downloadFile.getFileUrl());
        if (file != null) {
            if (file.getState() == DownLoadFile.DOWNSTAT_DOWNLOAD || file.getState() == DownLoadFile.DOWNSTAT_WAIT) {
                pauseTask(file);
            }
            downloadingMap.remove(file.getFileUrl());
            allFilesList.remove(file);
        }

        downloadFile.delete();
        downloadFile.deleteDownLoadTmpLogs();
        FileDownloader.deleteDltmpFile(downloadFile);
        if (deleteSource) {
            FileUtil.deleteFile(downloadFile.getSavePath() + "/" + downloadFile.getFileName());
        }
    }

    /**
     * 获取下载任务
     * @param url URL
     * @return 如果不存在，则返回null
     */
    public DownLoadFile getDownloadFile(String url) {
        if (!isLoaded) {
            return getDownloadFileFromDB(url);
        }
        for (DownLoadFile downloadFile : allFilesList) {
            if (downloadFile.getFileUrl().equals(url)) return downloadFile;
        }
        return null;
    }

    private DownLoadFile getDownloadFileFromDB(String url) {
        //首先查找数据库是否存在这一条记录
        List<DownLoadFile> downLoadFileList = DataSupport.where("fileurl = ?", url).find(DownLoadFile.class);

        if (downLoadFileList != null && downLoadFileList.size() > 0)
        {
            return downLoadFileList.get(0);
        }
        return null;
    }

    private boolean downloadFile(DownLoadFile downLoadFile) {
        XLLog.d(TAG, "start com.szuwest.download url=" + downLoadFile.getFileUrl());
        final FileDownloader downloader = downloadingMap.get(downLoadFile.getFileUrl());
        if (downloader != null) {
            downLoadFile.setState(DownLoadFile.DOWNSTAT_WAIT);
            downloader.cancel(false);

            pool.submit(downloader);
//            new Thread(downloader).start();
        } else {
            downLoad(downLoadFile);
        }
        return true;
    }

    private void downLoad(final DownLoadFile downloadFile) {
        if (!allFilesList.contains(downloadFile))
            allFilesList.add(downloadFile);
        FileDownloader loader = new FileDownloader(this, downloadFile, DownloadConfig.THREAD_NUM);
        downloadingMap.put(downloadFile.getFileUrl(), loader);
        downloadFile.setState(DownLoadFile.DOWNSTAT_WAIT);
//        new Thread(loader).start();
        pool.submit(loader);
    }


    public void onDownloadStart(final FileDownloader downloader) {
        downloader.getDownLoadFile().save();
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                for (DownloadListener listener : listeners) {
                    listener.onDownloadStart(downloader.getDownLoadFile());
                }
            }
        });
    }

    public void onDownloadProgress(final FileDownloader downloader) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                for (DownloadListener listener : listeners) {
                    listener.onDownloadUpdate(downloader.getDownLoadFile(), downloader.getDownLoadFile().getCompletelength());
                }
            }
        });
    }

    public void onDownloadSuccess(final FileDownloader downloader) {
        downloader.getDownLoadFile().setCompletelength(System.currentTimeMillis()/1000);
        downloader.getDownLoadFile().save();
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                downloadingMap.remove(downloader.getDownLoadFile().getFileUrl());
                for (DownloadListener listener : listeners) {
                    listener.onDownloadSuccess(downloader.getDownLoadFile());
                }
            }
        });
    }

    public void onDownloadFail(final FileDownloader downloader) {
        downloader.getDownLoadFile().save();
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                downloadingMap.remove(downloader.getDownLoadFile().getFileUrl());
                for (DownloadListener listener : listeners) {
                    listener.onDownloadFail(downloader.getDownLoadFile());
                }
            }
        });
    }

    public void onDownloadCancel(final FileDownloader downloader) {
        downloader.getDownLoadFile().save();
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                downloadingMap.remove(downloader.getDownLoadFile().getFileUrl());
                for (DownloadListener listener : listeners) {
                    listener.onDownloadStop(downloader.getDownLoadFile());
                }
            }
        });
    }

    public List<DownLoadFile> getAllDownLoadFiles() {
        return allFilesList;
    }

    public void addDownloadListener(DownloadListener listener) {
        if (listener != null) {
            if (!listeners.contains(listener)) listeners.add(listener);
        }
    }

    public void removeDownloadListener(DownloadListener listener) {
        listeners.remove(listener);
    }

    public void close() {
        for (Map.Entry<String, FileDownloader> entry : downloadingMap.entrySet()) {
            FileDownloader fileDownloader = entry.getValue();
            if (fileDownloader.getDownLoadFile().getState() == DownLoadFile.DOWNSTAT_DOWNLOAD) {
                fileDownloader.cancel(true);
            }
        }
        pool.shutdown();
        handlerThread.quit();
    }

    public static boolean checkSDEnoughSize(long needSize) {
        long available = DeviceHelper.getAvailableExternalMemorySize();
        return available > (needSize + EXTRA_SPACE_REMAIN);//需要额外的10M空间
    }

    private final static Pattern IP_REGX = Pattern.compile("http://\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");


    /**
     * 获取文件的本地磁盘上的路径，如果该URL没有被下载，返回为空串
     * @param url
     * @return 本地磁盘上的路径
     */
    public static String localPathOfUrl(String url) {
        File file1 = new File(DownloadConfig.getDefaultSavePath(), FileUtil.getFileNameByUrl(url));
        if (file1.exists()) return file1.getAbsolutePath();
        return "";
    }
}
