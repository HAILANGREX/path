package com.boswinner.entity;

public class vertexpoi {
    private double x;
    private double y;
    private double z;

    public vertexpoi(){
        super();
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public double getX() {
        return x;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof vertexpoi)
        {
            vertexpoi poi = (vertexpoi) obj;
            return this.x==poi.x&&this.y==poi.y&&this.z==poi.z;
        }
        return super.equals(obj);
    }
}
