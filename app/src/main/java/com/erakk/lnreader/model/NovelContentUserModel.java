package com.erakk.lnreader.model;

import java.util.Date;

public class NovelContentUserModel {
    private int id = -1;
    private String page;
    private int lastXScroll;
    private int lastYScroll;
    private double lastZoom;
    private Date lastUpdate;
    private Date lastCheck;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPage() {
        return page;
    }

    public void setPage(String page) {
        this.page = page;
    }

    public int getLastXScroll() {
        return lastXScroll;
    }

    public void setLastXScroll(int lastXScroll) {
        this.lastXScroll = lastXScroll;
    }

    public int getLastYScroll() {
        return lastYScroll;
    }

    public void setLastYScroll(int lastYScroll) {
        this.lastYScroll = lastYScroll;
    }

    public double getLastZoom() {
        return lastZoom;
    }

    public void setLastZoom(double lastZoom) {
        this.lastZoom = lastZoom;
    }

    public Date getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(Date lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public Date getLastCheck() {
        return lastCheck;
    }

    public void setLastCheck(Date lastCheck) {
        this.lastCheck = lastCheck;
    }
}
