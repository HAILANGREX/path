package com.boswinner.entity;

public class Entrance {

    private int x;
    private int y;

    public Integer getX() {
        return x;
    }

    public void setX(Integer x) {
        this.x = x;
    }

    public Integer getY() {
        return y;
    }

    public void setY(Integer y) {
        this.y = y;
    }

    public Entrance(){
        super();
    }

    public Entrance(Integer x, Integer y) {
        this.x = x;
        this.y = y;
    }
    @Override
    public boolean equals(Object obj)
    {
        if (obj == null) return false;
        if (obj instanceof Entrance)
        {
            Entrance c = (Entrance) obj;
            return x == c.x && y == c.y;
        }
        return false;
    }
    @Override
    public final int hashCode() {
        int hashcode = 17;
        hashcode = hashcode * 31 + this.x;
        hashcode = hashcode * 31 + this.y;
        return hashcode;
    }
}
