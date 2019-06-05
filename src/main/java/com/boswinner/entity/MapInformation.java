package com.boswinner.entity;

import java.util.List;

public class MapInformation
{
    public int[][] maps; // 二维数组的地图
    public int width; // 地图的宽
    public int hight; // 地图的高
    public Node start; // 起始结点
    public List<Node> end; // 最终结点

    public MapInformation(int[][] maps, int width, int hight, Node start, List<Node> end)
    {
        this.maps = maps;
        this.width = width;
        this.hight = hight;
        this.start = start;
        this.end = end;
    }
}

