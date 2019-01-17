package com.example.bogdan.licenta;

public class Cluster {

    public String clusterName;
    public String clusterType;
    public int clusterImageUrl;
    public int startPixX;
    public int startPixY;
    public Double distancePx;

    public Cluster(String clusterName, String clusterType, int clusterImageUrl, int startPixX, int startPixY, Double distancePx) {
        this.clusterName = clusterName;
        this.clusterType = clusterType;
        this.clusterImageUrl = clusterImageUrl;
        this.startPixX = startPixX;
        this.startPixY = startPixY;
        this.distancePx = distancePx;
    }

    public Cluster(String clusterName, String clusterType, int startPixX, int startPixY, Double distancePx) {
        this.clusterName = clusterName;
        this.clusterType = clusterType;
        this.startPixX = startPixX;
        this.startPixY = startPixY;
        this.distancePx = distancePx;
    }

    public Cluster (){
        clusterType = "default";
    }

    public Cluster(String clusterName) {
        this.clusterName = clusterName;
        clusterType = "default";
    }

    public Cluster(String clusterName, String clusterType) {
        this.clusterName = clusterName;
        this.clusterType = clusterType;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public String getClusterType() {
        return clusterType;
    }

    public void setClusterType(String clusterType) {
        this.clusterType = clusterType;
    }
}
