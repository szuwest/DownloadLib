/**
 * 文件名称 : FileUtil.java
 * <p/>
 * 作者信息 : liuzongyao
 * <p/>
 * 创建时间 : 2015-4-6, 下午2:17:45
 * <p/>
 * 版权声明 : Copyright (c) 2009-2012 Hydb Ltd. All rights reserved
 * <p/>
 * 评审记录 :
 * <p/>
 */

package com.szuwest.download.util;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.util.UUID;


/**
 * 请在这里增加文件描述
 * <p/>
 */
public class FileUtil {

    /**
     * 得到sdcard目录
     */
    public static String getSDCardPath() {
        return Environment.getExternalStorageDirectory().getPath();
    }


    public static boolean isFileExist(String path) {
        File file = new File(path);
        if (!file.exists()) {
            return false;
        }
        return true;
    }

    public static boolean deleteFile(String path) {
        if (null == path) {
            return false;
        }
        boolean ret = false;

        File file = new File(path);
        if (file.exists()) {
            ret = file.delete();
        }
        return ret;
    }


    /**
     * The full path name of the file to delete.
     *
     * @param path name
     * @return
     */
    public int deleteTarget(String path) {
        File target = new File(path);

        if (target.exists() && target.isFile() && target.canWrite()) {
            target.delete();
            return 0;
        } else if (target.exists() && target.isDirectory() && target.canRead()) {
            String[] file_list = target.list();

            if (file_list != null && file_list.length == 0) {
                target.delete();
                return 0;

            } else if (file_list != null && file_list.length > 0) {

                for (int i = 0; i < file_list.length; i++) {
                    File temp_f = new File(target.getAbsolutePath() + "/" + file_list[i]);

                    if (temp_f.isDirectory())
                        deleteTarget(temp_f.getAbsolutePath());
                    else if (temp_f.isFile())
                        temp_f.delete();
                }
            }
            if (target.exists())
                if (target.delete())
                    return 0;
        }
        return -1;
    }

    /**
     * get file name from url
     *
     * @param url
     * @return
     */
    public static String getFileNameByUrl(String url) {
        if (TextUtils.isEmpty(url)) {
            return "";
        }
        int index = url.lastIndexOf('?');
        int index2 = url.lastIndexOf("/");
        if (index > 0 && index2 >= index) {
            return UUID.randomUUID().toString();
        }
        return url.substring(index2 + 1, index < 0 ? url.length() : index);
    }

    /**
     * get file extend name
     *
     * @param fileName
     * @return
     */
    public static String getFileExtendName(String fileName) {
        if (TextUtils.isEmpty(fileName)) {
            return null;
        }
        int index = fileName.lastIndexOf('.');
        if (index < 0) {
            return "unknown";
        } else {
            return fileName.substring(index + 1);
        }
    }
    public static long KBSIZE = 1024;
    public static long MBSIZE1 = 1024 * 1024;
    public static long MBSIZE10 = 1024L * 1024 * 10;

    public static boolean createFile(File file, long fileLength) {
        FileOutputStream fos = null;
        try {

            if (!file.exists()) {
                boolean ret = file.createNewFile();
                if (!ret) return false;
            }

            long batchSize = 0;
            batchSize = fileLength;
            if (fileLength > KBSIZE) {
                batchSize = KBSIZE;
            }
            if (fileLength > MBSIZE1) {
                batchSize = MBSIZE1;
            }
            if (fileLength > MBSIZE10) {
                batchSize = MBSIZE10;
            }
            long count = fileLength / batchSize;
            long last = fileLength % batchSize;


            fos = new FileOutputStream(file);
            FileChannel fileChannel = fos.getChannel();
            for (int i = 0; i < count; i++) {
                ByteBuffer buffer = ByteBuffer.allocate((int) batchSize);
                fileChannel.write(buffer);
            }
            ByteBuffer buffer = ByteBuffer.allocate((int) last);
            fileChannel.write(buffer);


            fos.close();
            return true;

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public static void installApk(Activity context, String apkPath) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(new File(apkPath)), "application/vnd.android.package-archive");
        context.startActivity(intent);
    }


    public static String getMd5(File file) {
        if (file == null || !file.exists()) {
            return null;
        }


        String value = null;
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            MappedByteBuffer byteBuffer = in.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, file.length());
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(byteBuffer);
            BigInteger bi = new BigInteger(1, md5.digest());
            value = bi.toString(16);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != in) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return value;
    }
}
