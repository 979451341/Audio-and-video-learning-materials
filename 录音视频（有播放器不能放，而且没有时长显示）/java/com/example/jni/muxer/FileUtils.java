package com.example.jni.muxer;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 文件处理工具类
 * Created by renhui on 2017/9/25.
 */
public class FileUtils {

    private static final String MAIN_DIR_NAME = "/android_records";
    private static final String BASE_VIDEO = "/video/";
    private static final String BASE_EXT = ".mp4";

    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm");
    private String currentFileName = "-";
    private String nextFileName;

    public FileUtils() {
    }

    public boolean requestSwapFile() {
        return requestSwapFile(false);
    }

    public boolean requestSwapFile(boolean force) {
        //SD 卡可读写
        String fileName = getFileName();
        boolean isChanged = false;

        if (!currentFileName.equalsIgnoreCase(fileName)) {
            isChanged = true;
        }

        if (isChanged || force) {
            nextFileName = getSaveFilePath(fileName);
            return true;
        }

        return false;
    }

    public String getNextFileName() {
        return nextFileName;
    }

    private String getFileName() {
        String format = simpleDateFormat.format(System.currentTimeMillis());
        return format;
    }

    private String getSaveFilePath(String fileName) {
        currentFileName = fileName;
        StringBuilder fullPath = new StringBuilder();
        fullPath.append(getExternalStorageDirectory());
        //检查内置卡剩余空间容量,并清理
        checkSpace();
        fullPath.append(MAIN_DIR_NAME);
        fullPath.append(BASE_VIDEO);
        fullPath.append(fileName);
        fullPath.append(BASE_EXT);

        String string = fullPath.toString();
        File file = new File(string);
        File parentFile = file.getParentFile();
        if (!parentFile.exists()) {
            parentFile.mkdirs();
        }

        return string;
    }

    /**
     * 检查剩余空间
     */
    private void checkSpace() {
        StringBuilder fullPath = new StringBuilder();
        String checkPath = getExternalStorageDirectory();
        fullPath.append(checkPath);
        fullPath.append(MAIN_DIR_NAME);
        fullPath.append(BASE_VIDEO);

        if (checkCardSpace(checkPath)) {
            File file = new File(fullPath.toString());

            if (!file.exists()) {
                file.mkdirs();
            }

            String[] fileNames = file.list();
            if (fileNames.length < 1) {
                return;
            }

            List<String> fileNameLists = Arrays.asList(fileNames);
            Collections.sort(fileNameLists);

            for (int i = 0; i < fileNameLists.size() && checkCardSpace(checkPath); i++) {
                //清理视频
                String removeFileName = fileNameLists.get(i);
                File removeFile = new File(file, removeFileName);
                try {
                    removeFile.delete();
                    Log.e("angcyo-->", "删除文件 " + removeFile.getAbsolutePath());
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e("angcyo-->", "删除文件失败 " + removeFile.getAbsolutePath());
                }
            }
        }
    }

    private boolean checkCardSpace(String filePath) {
        File dir = new File(filePath);
        double totalSpace = dir.getTotalSpace();//总大小
        double freeSpace = dir.getFreeSpace();//剩余大小
        if (freeSpace < totalSpace * 0.2) {
            return true;
        }
        return false;
    }

    /**
     * 获取sdcard路径
     */
    public static String getExternalStorageDirectory() {
        return Environment.getExternalStorageDirectory().getPath();
    }

}
