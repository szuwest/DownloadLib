/**
 * 文件名称 : DownloadProgressListener.java
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


import com.szuwest.download.domain.DownLoadFile;

public interface DownloadListener
{
    public void onDownloadStart(DownLoadFile task);

    public void onDownloadUpdate(DownLoadFile task, long completeSize);

    public void onDownloadStop(DownLoadFile task);

    public void onDownloadSuccess(DownLoadFile task);

    public void onDownloadFail(DownLoadFile task);

}
