package com.erakk.lnreader.model;

public class DownloadModel {
	private String downloadName;
    private Integer downloadProgress;
    private String downloadId;
    
    public DownloadModel (String id, String name, Integer progress) {
    	downloadName = name;
    	downloadProgress = progress;
    	downloadId = id;
    }
    
    public String getDownloadName() {
        return downloadName;
    }
    public void setDownloadName(String downloadName) {
        this.downloadName = downloadName;
    }
    
    public String getDownloadId() {
        return this.downloadId;
    }
    public void setDownloadId(String downloadId) {
        this.downloadId = downloadId;
    }
    
    public Integer getDownloadProgress() {
        return downloadProgress;
    }
    public void setDownloadProgress(Integer	progress) {
        this.downloadProgress = progress;
    }
}
