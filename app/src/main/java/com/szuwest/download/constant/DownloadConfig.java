/**
 * 文件名称 : DownloadConstant.java
 * <p>
 * 作者信息 : liuzongyao
 * <p>
 * 创建时间 : 2012-9-17, 下午5:42:32
 * <p>
 * 版权声明 : Copyright (c) 2009-2012 Hydb Ltd. All rights reserved
 * <p>
 * 评审记录 :
 * <p>
 */

package com.szuwest.download.constant;

import com.szuwest.download.util.FileUtil;

import java.io.File;

/**
 * 下载模块的常数
 */
public final class DownloadConfig
{
    
    /**
     * 应用在t卡中的根目录
     */
    public static final String DOWNLOAD_ROOT = "TimeCloud";

    public static String getDefaultSavePath() {
        return FileUtil.getSDCardPath() + File.separator + DownloadConfig.DOWNLOAD_ROOT + File.separator;
    }

    public static final String FILE_DOWN_METHOD = "GET";
    
    public static final int CONNECTION_TIME = 5 * 1000;

    public static final int THREAD_NUM = 2;

    public static final int THREAD_POOL_NUM = 2;

}
