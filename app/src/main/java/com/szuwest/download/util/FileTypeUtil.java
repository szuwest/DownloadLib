package com.szuwest.download.util;

import android.text.TextUtils;

import java.util.HashMap;

/**
 * Created by shuailongcheng on 20/5/15.
 * 类把支持的扩展名都放到一个静态的map里，在map里查找输入的文件的扩展名（作为map key)，得到相应的type（FILE_TYPE)
 */
public class FileTypeUtil {

    private static final String add_classify= ".BT,.RAR,.zip";
    private static final String video_suffix=".3gp,.avi,.flv,.m4v,.mkv,.mov,.mp4,.rm,.rmvb,.swf,.xv,.3g2,.asf,.ask,.c3d,.dat,.divx,.dvr-ms,.f4v,.flc,.fli,.flx,.m2p,.m2t,.m2ts,.m2v,.mlv,.mpe,.mpeg,.mpg,.mpv,.mts,.ogm,.qt,.ra,.tp,.trp,.ts,.uis,.uisx,.uvp,.vob,.vsp,.webm,.wmv,.wmvhd,.wtv,.xvid";

//    video_suffix=.mp4,.3gp,.avi,.flv,.m4v,.mkv,.mov,.rmvb,.swf,.asf,.dat,.f4v,.ts,.mpg,.wmv
    private static final String music_suffix=".APE,.flac,.mp3,.wav,.wma";
    private static final String document_suffix=".txt,.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.html,.wps,.wpt,.xps";
    private static final String image_suffix=".jpg,.jpeg,.png,.bmp,.gif,.ico,.raw,.pcx";

    private static final HashMap<String, Integer> mTypeMap = new HashMap<String, Integer>();

    public interface FILE_TYPE {
        int UN_KNOWN = 0;
        int CLASSIFY = 1;
        int VIDEO = 2;
        int MUSIC = 3;
        int DOC = 4;
        int IMAGE = 5;
    }
    static {
        // init mTypeMap
        addToMap(add_classify, FILE_TYPE.CLASSIFY);
        addToMap(video_suffix, FILE_TYPE.VIDEO);
        addToMap(music_suffix, FILE_TYPE.MUSIC);
        addToMap(document_suffix, FILE_TYPE.DOC);
        addToMap(image_suffix, FILE_TYPE.IMAGE);
    }

    private static void addToMap(String suffixCollection, int fileType) {
        String[] split = suffixCollection.split(",");
        for (int i = 0; i < split.length; i++) {
            mTypeMap.put(split[i].toLowerCase(), fileType);
        }
    }

    /**
     * 判断是否未知类型的文件
     * @param dirName
     * @return
     */
    public static boolean isUnknownType(String dirName) {
        if (TextUtils.isEmpty(dirName)) {
            return true;
        }

        String suffix = getSuffix(dirName);
        if (TextUtils.isEmpty(suffix)) {
            return true;
        }

        Integer ret = mTypeMap.get(suffix.toLowerCase());
        return  ret == null;
    }
    /**
     * 获取文件的扩展名
     * @param dirName 文件名或文件路径均可
     * @return
     */
    private static String getSuffix(String dirName) {
        if (dirName == null) {
            return null;
        }

        int lastIndexOfDot = dirName.lastIndexOf('.');
        if (lastIndexOfDot < 0) {
            return null;
        }

        return dirName.substring(lastIndexOfDot);
    }

    /**
     * 通过扩展名判断文件是否是图片文件
     * @param fileName  文件名或者路径都行
     * @return
     */
    public static boolean isImageFile(String fileName) {
        return getFileType(fileName) == FILE_TYPE.IMAGE;
    }

    /**
     *
     * @param fileName 文件名或者路径都行
     * @return 文件类型，FILE_TYPE内的值
     */
    public static int getFileType(String fileName) {
        try {

            Integer fileType = mTypeMap.get(getSuffix(fileName).toLowerCase());
            return fileType == null ? FILE_TYPE.UN_KNOWN : fileType;
        } catch (NullPointerException ignore) {
            return FILE_TYPE.UN_KNOWN;
        }
    }

    /**
     * 比较 扩展名， 无扩展名是最小，有扩展名的按字母顺序排序
     * @param fileName1
     * @param fileName2
     * @return <0 表示fileName1更小，反则反之，相等则返回0
     */
    public static int compareSuffix(String fileName1, String fileName2) {
        String suffix1 = getSuffix(fileName1);
        String suffix2 = getSuffix(fileName2);
        if (null == suffix1 && null == suffix2) {
            return 0;
        } else {
            if (null == getSuffix((fileName1))) {
                return -1;
            } else if (null == getSuffix(fileName2)) {
                return 1;
            } else {
                return getSuffix(fileName1).substring(1).compareToIgnoreCase(getSuffix(fileName2).substring(1));
            }
        }
    }

}
