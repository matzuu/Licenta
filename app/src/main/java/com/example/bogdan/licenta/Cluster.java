package com.example.bogdan.licenta;

public class Cluster {

    public String clusterName;
    public String clusterType;
    public byte[] imgMap;

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

    public Cluster(String clusterName, String clusterType, byte[] imgMap) {
        this.clusterName = clusterName;
        this.clusterType = clusterType;
        this.imgMap = imgMap;
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
