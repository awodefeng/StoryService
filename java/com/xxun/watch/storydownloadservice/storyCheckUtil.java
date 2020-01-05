package com.xxun.watch.storydownloadservice;

import android.app.DownloadManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import android.os.StatFs;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import java.io.File;
import java.util.ArrayList;
import android.os.SystemProperties;

/**
 * Created by zhangjun5 on 2017/9/13.
 */

public class storyCheckUtil {
    public static boolean checkStoryRepeat(ArrayList<StoryBeanData> storyList,ArrayList<String> playList,
                                           StoryBeanData storyBeanData){
        boolean isRepeat = false;
        for(int i = 0; i<storyList.size();i++){
            StoryBeanData beanData = storyList.get(i);
            JSONObject jsonObject = (JSONObject) (JSONValue.parse(beanData.getStroyData()));
            String dataList = jsonObject.get("track_id").toString();
            jsonObject = (JSONObject) (JSONValue.parse(storyBeanData.getStroyData()));
            String dataJson = jsonObject.get("track_id").toString();
            if(dataList.equals(dataJson)){
                isRepeat = true;
                break;
            }
        }
        if(!isRepeat){
            for(int i = 0; i<playList.size();i++) {
                String storyBean = playList.get(i);
                String[] beanArray = storyBean.split("_");
                if(beanArray.length <= 1){
                    continue;
                }
                JSONObject jsonObject = (JSONObject) (JSONValue.parse(storyBeanData.getStroyData()));
                String dataJson = jsonObject.get("track_id").toString();
                if(beanArray[beanArray.length-1].contains(dataJson)){
                    isRepeat = true;
                    break;
                }
            }
        }
        return isRepeat;
    }
        
    public static void transJsonToStoryBean(JSONObject storyData, StoryBeanData storyBeanData){
        storyBeanData.setStoryOptype((int)storyData.get("optype"));
        storyBeanData.setStoryEid((String)storyData.get("EID"));
        storyBeanData.setStoryGid((String)storyData.get("GID"));

        storyBeanData.setStoryFile((String)storyData.get("file"));
        storyBeanData.setStroyData((String)storyData.get("data"));
        storyBeanData.setStorySize((int)storyData.get("size"));
        storyBeanData.setStorySn((int)storyData.get("sn"));
        storyBeanData.setStoryType((int)storyData.get("type"));
        storyBeanData.setStoryUpdateTS((String)storyData.get("updateTS"));
        storyBeanData.setStoryUrl((String)storyData.get("url"));
        storyBeanData.setStoryMd5((String)storyData.get("md5"));
        storyBeanData.setStoryStatus((int)storyData.get("status"));
        if(storyData.get("downloadId") == null){
            storyBeanData.setDownloadId(10000000);
        }else{
            storyBeanData.setDownloadId((int)storyData.get("downloadId"));
        }
    }

    public static void getStoryListToArrayList(ArrayList<StoryBeanData> storyList,JSONArray jsonArray){
        for(int i = 0;i < jsonArray.size();i++) {
            JSONObject storyData = (JSONObject) jsonArray.get(i);
            StoryBeanData storyBeanData = new StoryBeanData();
            transJsonToStoryBean(storyData, storyBeanData);
            storyList.add(storyBeanData);
        }
    }
    
    public static long calcTwoTimeStampInterval(String mDateStart, String mDateEnd) {
        long result = 0;
        if (mDateStart == null || mDateEnd == null) {
            return 0;
        }
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        Date startTime = null;
        Date endTime = null;
        try {
            startTime = format.parse(mDateStart);
            endTime = format.parse(mDateEnd);
            result = (endTime.getTime() - startTime.getTime()) / 1000;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static JSONArray getStoryListToJSONArray(ArrayList<StoryBeanData> storyList){
        JSONArray array = new JSONArray();
        for(int i = 0;i < storyList.size();i++){
            StoryBeanData beanData = storyList.get(i);
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("optype", beanData.getStoryOptype());
            jsonObject.put("EID",beanData.getStoryEid());
            jsonObject.put("GID",beanData.getStoryGid());
            jsonObject.put("file",beanData.getStoryFile());
            jsonObject.put("data",beanData.getStroyData());
            jsonObject.put("size",beanData.getStorySize());
            jsonObject.put("sn",beanData.getStorySn());
            jsonObject.put("type",beanData.getStoryType());
            jsonObject.put("updateTS",beanData.getStoryUpdateTS());
            jsonObject.put("url",beanData.getStoryUrl());
            jsonObject.put("md5",beanData.getStoryMd5());
            jsonObject.put("status",beanData.getStoryStatus());
            jsonObject.put("downloadId",beanData.getDownloadId());
            array.add(jsonObject);
        }
        return array;
    }
        
    public static long getAvailSpace(String path){
        StatFs statfs = new StatFs(path);
        long size = statfs.getBlockSize();//获取分区的大小
        long count = statfs.getAvailableBlocks();//获取可用分区块的个数
        return size*count;
    }

    public static boolean isCanLoadDataFromServer() {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSize();
        long availableBlocks = stat.getAvailableBlocks();
        return (double)(availableBlocks * blockSize)/1024/1024 > 50;
    }

    public static JSONObject getStoryListToJSONArray(StoryBeanData beanData,int newOptype,
                                                     int newStatus,String newUpdateTs,String reason){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("optype", newOptype);
        jsonObject.put("EID",beanData.getStoryEid());
        jsonObject.put("GID",beanData.getStoryGid());
        jsonObject.put("file",beanData.getStoryFile());
        jsonObject.put("data",beanData.getStroyData());
        jsonObject.put("size",beanData.getStorySize());
        jsonObject.put("sn",beanData.getStorySn());
        jsonObject.put("type",beanData.getStoryType());
        jsonObject.put("updateTS",newUpdateTs);
        jsonObject.put("url",beanData.getStoryUrl());
        jsonObject.put("md5",beanData.getStoryMd5());
        jsonObject.put("status",newStatus);
        jsonObject.put("reason",reason);
        return jsonObject;
    }

    public static JSONObject getStoryStateToJSONArray(StoryBeanData beanData
            ,int newOptype, int newStatus){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("optype", newOptype);
        jsonObject.put("EID",beanData.getStoryEid());
        jsonObject.put("GID",beanData.getStoryGid());
        jsonObject.put("file",beanData.getStoryFile());
        jsonObject.put("data",beanData.getStroyData());
        jsonObject.put("size",beanData.getStorySize());
        jsonObject.put("sn",beanData.getStorySn());
        jsonObject.put("type",beanData.getStoryType());
        jsonObject.put("updateTS",beanData.getStoryUpdateTS());
        jsonObject.put("url",beanData.getStoryUrl());
        jsonObject.put("md5",beanData.getStoryMd5());
        jsonObject.put("status",newStatus);
        return jsonObject;
    }

    public static void setValue(Context context,String key, String value) {
        final SharedPreferences preferences = context.getSharedPreferences(Const.pref_file_name, Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = preferences.edit();
        editor.putString(key, value);
        editor.commit();
    }
    public static String getStringValue(Context context,String key, String defValue) {
        String str = context.getSharedPreferences(Const.pref_file_name, Context.MODE_PRIVATE )
                .getString(key, defValue);
        return str;
    }
}
