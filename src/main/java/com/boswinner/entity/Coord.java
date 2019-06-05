package com.boswinner.entity;

public class Coord
{

    public int x;
    public int y;

    public Coord(int x, int y)
    {
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == null) return false;
        if (obj instanceof Coord)
        {
            Coord c = (Coord) obj;
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

