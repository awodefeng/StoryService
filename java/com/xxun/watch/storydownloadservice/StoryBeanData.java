package com.xxun.watch.storydownloadservice;

/**
 * Created by zhangjun5 on 2017/9/12.
 */

public class StoryBeanData {
    private int storyOptype;
    private String storyEid;
    private String storyFile;
    private String storyGid;
    private int storySize;
    private String stroyData;
    private int storySn;
    private int storyType;
    private String storyUpdateTS;
    private String storyUrl;
    private int storyStatus;
    private String storyMd5;
    private long downloadId;

    public long getDownloadId() {
        return downloadId;
    }

    public void setDownloadId(long downloadId) {
        this.downloadId = downloadId;
    }

    public int getStoryOptype() {
        return storyOptype;
    }

    public void setStoryOptype(int storyOptype) {
        this.storyOptype = storyOptype;
    }

    public String getStoryEid() {
        return storyEid;
    }

    public void setStoryEid(String storyEid) {
        this.storyEid = storyEid;
    }

    public String getStoryFile() {
        return storyFile;
    }

    public void setStoryFile(String storyFile) {
        this.storyFile = storyFile;
    }

    public String getStoryGid() {
        return storyGid;
    }

    public void setStoryGid(String storyGid) {
        this.storyGid = storyGid;
    }

    public int getStorySize() {
        return storySize;
    }

    public void setStorySize(int storySize) {
        this.storySize = storySize;
    }

    public String getStroyData() {
        return stroyData;
    }

    public void setStroyData(String stroyData) {
        this.stroyData = stroyData;
    }

    public int getStorySn() {
        return storySn;
    }

    public void setStorySn(int storySn) {
        this.storySn = storySn;
    }

    public int getStoryType() {
        return storyType;
    }

    public void setStoryType(int storyType) {
        this.storyType = storyType;
    }

    public String getStoryUpdateTS() {
        return storyUpdateTS;
    }

    public void setStoryUpdateTS(String storyUpdateTS) {
        this.storyUpdateTS = storyUpdateTS;
    }

    public String getStoryUrl() {
        return storyUrl;
    }

    public void setStoryUrl(String storyUrl) {
        this.storyUrl = storyUrl;
    }

    public int getStoryStatus() {
        return storyStatus;
    }

    public void setStoryStatus(int storyStatus) {
        this.storyStatus = storyStatus;
    }

    public String getStoryMd5() {
        return storyMd5;
    }

    public void setStoryMd5(String storyMd5) {
        this.storyMd5 = storyMd5;
    }
}
