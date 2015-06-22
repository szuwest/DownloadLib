/**
 * 文件名称 : AppInfo1.java
 * <p>
 * 作者信息 : yangxiongjie
 * <p>
 * 创建时间 : 2012-9-7, 上午10:42:20
 * <p>
 * 版权声明 : Copyright (c) 2009-2012 Hydb Ltd. All rights reserved
 * <p>
 * 评审记录 :
 * <p>
 */

package com.szuwest.download.domain;

import android.content.ContentValues;

import com.szuwest.download.util.ConvertUtil;

import org.litepal.crud.DataSupport;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 下载的文件列表
 * <p>
 */
public class DownLoadFile extends DataSupport implements Serializable
{

    /**
     * 等待下载
     */
    public static final int DOWNSTAT_WAIT = 0;
    /**
     * 暂停
     */
    public static final int DOWNSTAT_PAUSE = 1;
    
    public static final int DOWNSTAT_DOWNLOAD = 2;
    
    /**
     * 下载完成
     */
    public static final int DOWNSTAT_FINISH = 3;

    /**
     * 下载失败
     */
    public static final int DOWNSTAT_FAIL = 4;

    /**
     * 未知错误
     */
    public static final int ERROR_UNKNOWN = 99;
    /**
     * 文件目录创建失败
     */
    public static final int ERROR_MKDIR = 100;
    /**
     * 网络错误
     */
    public static final int ERROR_NETWORK = 101;
    /**
     * 无法获取文件大小
     */
    public static final int ERROR_GET_FILESIZE = 102;
    /**
     * 磁盘空间不足
     */
    public static final int ERROR_NOT_SPACE = 103;
    /**
     * 没有SD卡
     */
    public static final int ERROR_NO_SD = 104;

    public static final int ERROR_FILE_NOTFOUND = 105;

    /**
     * 
     */
    private static final long serialVersionUID = 2148600830002082483L;
    
    private int id;
    
    /**
     * 关联关系
     */
    private List<DownLoadTmpLog> downLoadTmpLogList = new ArrayList<DownLoadTmpLog>();
    
    /**
     * app的名称
     */
    private String fileName;
    
    /**
     * 下载地址
     */
    private String fileUrl;
    
    /**
     * 状态： 0等待，1 暂停，2 下载，3 完成  4 失败
     */
    private int state;
    

    /**
     * 保存路径
     */
    private String savePath;
    
   
    private long completelength;
    
    /**
     * 内容长度
     */
    private long  totalLength;


    /**
     * 失败错误码
     */
    private int failCode = 0;

    /**
     * 内容类型
     */
    private int type;

    /**
     * 任务优先级，越打越高
     */
    private int priority;

    /**
     * 任务创建时的时间
     */
    private long createTime;

    /**
     * 任务完成时 的 时间
     */
    private long completeTime;


    public DownLoadFile(String fileName, String fileUrl, String savePath, int state)
    {
        this.fileName = fileName;
        this.savePath = savePath;
        this.state = state;
        setFileUrl(fileUrl);
    }

    public DownLoadFile()
    {

    }

    public void updateState(int state)
    {
        setState(state);
        ContentValues values = new ContentValues();
        values.put("state", state);
        DataSupport.updateAll(DownLoadFile.class, values, "fileurl = ?", this.fileUrl);
    }

    public int computeProgress()
    {
        float num = (float) completelength / (float) totalLength;
        
        return (int) (num * 100);
    }

    public boolean isFinished() {
        return state == DOWNSTAT_FINISH;
    }

    public boolean isPauseed() {
        return state == DOWNSTAT_PAUSE;
    }


    public void updateFileName(String fileName)
    {
        setFileName(fileName);
        ContentValues values = new ContentValues();
        values.put("fileName", fileName);
        DataSupport.updateAll(DownLoadFile.class, values, "fileurl = ?", this.fileUrl);
    }

    public void updateFileLength(long totalLength)
    {
        setTotalLength(totalLength);
        ContentValues values = new ContentValues();
        values.put("totallength", totalLength);
        DataSupport.updateAll(DownLoadFile.class, values,  "fileurl = ?", this.fileUrl);
    }

    public synchronized void updateCompleSize(long completelength)
    {
        setCompletelength(completelength);
        ContentValues values = new ContentValues();
        values.put("completelength", completelength);
        DataSupport.updateAll(DownLoadFile.class, values, "fileurl = ?", this.fileUrl);
    }

    /**
     * 根据id查询响应的记录
     * 
     * @return
     */
    public List<DownLoadTmpLog> getDownLoadTmpLogs()
    {
        return DataSupport.where("fileurl = ?", this.fileUrl).find(DownLoadTmpLog.class);
    }
    
    public Map<Integer, Long> getDownLoadTmpLogMap()
    {
        List<DownLoadTmpLog> downLoadTmpLogList = getDownLoadTmpLogs();
        
        HashMap<Integer, Long> h = new HashMap<Integer, Long>();
        
        for (DownLoadTmpLog d : downLoadTmpLogList)
        {
            h.put(d.getThreadId(), d.getDownLength());
        }
        
        return h;
    }
    
    public void saveDownLoadTmpLogs(Map<Integer, Long> tmpLogsMap)
    {
        
        for (Map.Entry<Integer, Long> entry : tmpLogsMap.entrySet())
        {
            DownLoadTmpLog downLoadTmpLog = new DownLoadTmpLog(entry.getKey(), entry.getValue(), fileUrl);
            
            downLoadTmpLog.save();
            
            this.getDownLoadTmpLogList().add(downLoadTmpLog);
        }
       this.updateAll("fileurl = ?", this.fileUrl);
    }
    
    public void updateDownLoadTmpLogs(Map<Integer, Long> tmpLogsMap)
    {
        for (Map.Entry<Integer, Long> entry : tmpLogsMap.entrySet())
        {
            ContentValues values = new ContentValues();
            values.put("downlength", entry.getValue());
            DataSupport.updateAll(DownLoadTmpLog.class, values, "threadid = ? and  fileurl = ?", entry.getKey() + "", this.fileUrl);
        }
    }
    
    public void deleteDownLoadTmpLogs()
    {
        //删除对应的下载记录
        DataSupport.deleteAll(DownLoadTmpLog.class, "fileurl = ?", this.fileUrl);
    }

    public List<DownLoadTmpLog> getDownLoadTmpLogList()
    {
        return downLoadTmpLogList;
    }
    
    public void setDownLoadTmpLogList(List<DownLoadTmpLog> downLoadTmpLogList)
    {
        this.downLoadTmpLogList = downLoadTmpLogList;
    }
    
    public String getFileName()
    {
        return fileName;
    }
    
    public void setFileName(String fileName)
    {
        this.fileName = fileName;
    }
    
    public int getState()
    {
        return state;
    }
    
    public void setState(int state)
    {
        this.state = state;
    }
    
    public String getSavePath()
    {
        return savePath;
    }

    public String getAbsolutePath() {
        return savePath + fileName;
    }
    
    public void setSavePath(String savePath)
    {
        this.savePath = savePath;
    }
    
    public String getFileUrl()
    {
        return fileUrl;
    }
    
    public void setFileUrl(String fileUrl)
    {
        this.fileUrl = fileUrl;
    }
    
    public int getId()
    {
        return id;
    }
    
    public void setId(int id)
    {
        this.id = id;
    }

    public int getType()
    {
        return type;
    }

    public void setType(int type)
    {
        this.type = type;
    }

    public long getCompletelength()
    {
        return completelength;
    }

    public void setCompletelength(long completelength)
    {
        this.completelength = completelength;
    }

    public long getTotalLength()
    {
        return totalLength;
    }

    public void setTotalLength(long totalLength)
    {
        this.totalLength = totalLength;
    }

    public int getFailCode() {
        return failCode;
    }

    public void setFailCode(int failCode) {
        this.failCode = failCode;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public long getCompleteTime() {
        return completeTime;
    }

    public void setCompleteTime(long completeTime) {
        this.completeTime = completeTime;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    private String thumbnailUrl;

    public long getVideoDuration() {
        return videoDuration;
    }

    public void setVideoDuration(long videoDuration) {
        this.videoDuration = videoDuration;
    }

    private long videoDuration;

    public String getDownloadSizeString() {
        return ConvertUtil.byteConvert(getCompletelength()) + "/" + ConvertUtil.byteConvert(getTotalLength());
    }
}
