/*
 * 文件名称 : FileDownLog.java
 * <p>
 * 作者信息 : admin
 * <p>
 * 创建时间 : 2015-4-9, 上午11:00:00
 * <p>
 * 版权声明 : Copyright (c) 2009-2012 Hydb Ltd. All rights reserved
 * <p>
 * 评审记录 :
 * <p>
 */

package com.szuwest.download.domain;

import org.litepal.crud.DataSupport;

import java.io.Serializable;


/**
 * 下载中的文件信息临时记录
 * <p>
 */
public class DownLoadTmpLog extends DataSupport implements Serializable
{
    /**
     * 
     */
    private static final long serialVersionUID = -6246740664713057824L;

   
    private int id;  
 
    /**
     * 线程id
     */
    private int threadId;
    
    
    /**
     * 下载长度
     */
    private long downLength;
    
    
    /**
     * 下载地址
     */
    private String fileUrl;
    
    
    /**
     * 关联关系
     */
    private DownLoadFile downLoadFile;
    
    public DownLoadTmpLog() {}
    
    
    public DownLoadTmpLog(Integer threadId, Long downLength, String fileUrl)
    {
        this.threadId = threadId;
        this.downLength = downLength;
        this.fileUrl = fileUrl;
    }
    
    

    public int getThreadId()
    {
        return threadId;
    }


    public void setThreadId(int threadId)
    {
        this.threadId = threadId;
    }


    public long getDownLength()
    {
        return downLength;
    }


    public void setDownLength(long downLength)
    {
        this.downLength = downLength;
    }


    public DownLoadFile getDownLoadFile()
    {
        return downLoadFile;
    }


    public void setDownLoadFile(DownLoadFile downLoadFile)
    {
        this.downLoadFile = downLoadFile;
    }


    public int getId()
    {
        return id;
    }


    public void setId(int id)
    {
        this.id = id;
    }


    public String getFileUrl()
    {
        return fileUrl;
    }



    public void setFileUrl(String fileUrl)
    {
        this.fileUrl = fileUrl;
    }
    
}
