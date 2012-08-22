package com.erakk.lnreader.callback;

public class DownloadCallbackEventData extends CallbackEventData implements ICallbackEventData {

	private long totalSize;
	private long downloadedSize;
	private String url;
	private String filePath;
	
	public long getTotalSize() {
		return totalSize;
	}
	public void setTotalSize(long totalSize) {
		this.totalSize = totalSize;
	}
	public long getDownloadedSize() {
		return downloadedSize;
	}
	public void setDownloadedSize(long downloadedSize) {
		this.downloadedSize = downloadedSize;
	}	
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getFilePath() {
		return filePath;
	}
	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}
	
	public int getPercentage() {
		if(totalSize > 0) {
			int percent = (int) (this.downloadedSize * 100 / this.totalSize ); 
			return percent < 100 ? percent : 100;
		}
		else return -1;
	}
	
	@Override
	public String getMessage() {
		if(this.message == null || this.message.length() == 0) {
			this.message = "Downloading: " + this.url + "\nProgress: " + downloadedSize + " bytes";
			if(getPercentage() > -1) {
				this.message += " of " + this.totalSize + " bytes (" + this.getPercentage() + "%)"; 
			}
		}
		return message;
	}	
}
