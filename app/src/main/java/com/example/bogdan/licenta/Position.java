package com.example.bogdan.licenta;

import android.content.Context;

public class Position {

    public Double CoordX;
    public Double CoordY;
    public Integer Level;
    public Integer Orientation;
    public String Cluster;

    public Position() {
        CoordX=null;
        CoordY=null;
        Level=null;
        Orientation=null;
        Cluster=null;
    }

    public Position(Double coordX,Double coordY,Integer level,Integer orientation,String cluster) {
        CoordX=coordX;
        CoordY=coordY;
        Level=level;
        Orientation=orientation;
        Cluster=cluster;
    }

    @Override
    public String toString() {
        return "CoordX: "+CoordX+" CoordY: "+CoordY+" Orientation: "+Orientation+" Cluster "+Cluster;
    }
}
