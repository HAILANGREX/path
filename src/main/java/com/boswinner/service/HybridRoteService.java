package com.boswinner.service;

import com.boswinner.entity.*;

import java.util.*;

public class HybridRoteService {

    public final static int BAR = 0; // 障碍值
    public final static int PATH = 3; // 路径
    public final static int TOPO = 2; // 路径
    public final static int DIRECT_VALUE = 10;
    public final static int OBLIQUE_VALUE = 14;
    private static int COLUMN_NUM;
    private Map<Double, BosGrids> gridsMap = new HashMap<>();

    Queue<Node> openList = new PriorityQueue<Node>(); // 优先队列(升序)
    List<Node> closeList = new ArrayList<Node>();
    List<Entrance> pathList = new ArrayList<>();



    public List<Entrance> start(MapInformation mapInfo)
    {
        if(mapInfo==null) return null;
        // clean
        openList.clear();
        closeList.clear();
        // 开始搜索
        setTOPO(mapInfo);
        openList.add(mapInfo.start);
        moveNodes(mapInfo);
        Collections.reverse(pathList);
//        System.out.print(pathList+"\n");
        return pathList;
    }



    /**
     * @Description: 将拓扑点映射到栅格地图中
     * @Param: [mapInformation]
     * @return: void
     * @Author: Wang
     * @Date: 2019/4/26
     */

    private void setTOPO(MapInformation mapInformation)
    {
        List<Node> topoPoint = mapInformation.end;
        for(Node node:topoPoint)
        {
            if(mapInformation.maps[node.coord.y][node.coord.x]!=0)
            {
                mapInformation.maps[node.coord.y][node.coord.x] = 2;
            }

        }
    }

    private String getgridpath(vertexpoi poi){
        Double min=Double.POSITIVE_INFINITY ;
        for(Double h:gridsMap.keySet())
        {
            if( Math.abs((poi.getZ()-h)) <min )
            {

                min = h;
            }
        }
        return min+","+gridsMap.get(min).getPath();
    }


    /**
     * 初始化栅格表
     * **/
    private  void getGrids( Iterable<BosGrids> grids){

        for(BosGrids bosGrid:grids)
        {
            gridsMap.put(Double.valueOf(bosGrid.getGridSettings().getHeight()),bosGrid);
        }
    }

//    private  List<BosGrids> bosGrids(Iterable<BosGrids> grids ){
//        List<BosGrids> bosGrids = Lists.newArrayList(grids);
//        return bosGrids;
//    }

    /**
    * @Description: 判断end是否在list中
    * @Param: [mapInfo]
    * @return: void
    * @Author: Wang
    * @Date: 2019/4/26
    */

    private void moveNodes(MapInformation mapInfo)
    {
        loop:while (!openList.isEmpty())
        {		List<Node> endlist = mapInfo.end;
            for (Node node:endlist) {
                if (isCoordInClose(node.coord))
                {
                    drawPath(mapInfo.maps, node);
                    break loop;
                }
            }
            Node current = openList.poll();
            closeList.add(current);
            addNeighborNodeInOpen(mapInfo,current);
        }
    }


    private Map<Coord,Node> getEndList(List<Node> list)
    {

        Map<Coord,Node> endlist = new HashMap<>();
        for (Node node:list)
        {
            endlist.put(node.coord,node);
        }
        return endlist;
    }



    private void drawPath(int[][] maps, Node end)
    {
        if(end==null||maps==null) return;
//        System.out.println("总代价：" + end.G);
        while (end != null)
        {
            Coord c = end.coord;
//			maps[c.y][c.x] = PATH;
            Entrance pis = new Entrance();
            pis.setX(c.x);
            pis.setY(c.y);
            pathList.add(pis);
            end = end.parent;
        }
    }


    private void addNeighborNodeInOpen(MapInformation mapInfo,Node current)
    {
        int x = current.coord.x;
        int y = current.coord.y;
        // 左
        addNeighborNodeInOpen(mapInfo,current, x - 1, y, DIRECT_VALUE);
        // 上
        addNeighborNodeInOpen(mapInfo,current, x, y - 1, DIRECT_VALUE);
        // 右
        addNeighborNodeInOpen(mapInfo,current, x + 1, y, DIRECT_VALUE);
        // 下
        addNeighborNodeInOpen(mapInfo,current, x, y + 1, DIRECT_VALUE);
        // 左上
        addNeighborNodeInOpen(mapInfo,current, x - 1, y - 1, OBLIQUE_VALUE);
        // 右上
        addNeighborNodeInOpen(mapInfo,current, x + 1, y - 1, OBLIQUE_VALUE);
        // 右下
        addNeighborNodeInOpen(mapInfo,current, x + 1, y + 1, OBLIQUE_VALUE);
        // 左下
        addNeighborNodeInOpen(mapInfo,current, x - 1, y + 1, OBLIQUE_VALUE);
    }


    private void addNeighborNodeInOpen(MapInformation mapInfo,Node current, int x, int y, int value)
    {
        if (canAddNodeToOpen(mapInfo,x, y))
        {
            Map<Coord,Node> end = getEndList(mapInfo.end);
            Coord coord = new Coord(x, y);
            int G = current.G + value; // 计算邻结点的G值
            Node child = findNodeInOpen(coord);
            if (child == null)
            {
                int H=0; // 计算H值
//                if(isEndNodeContain(end,coord))
                if(mapInfo.maps[coord.y][coord.x] == TOPO)
                {
                    child= end.get(coord);
                    child.parent=current;
                    child.G=G;
                    child.H=H;
                }
                else
                {
                    child = new Node(coord, current, G, H);
                }
                openList.add(child);
            }
            else if (child.G > G)
            {
                child.G = G;
                child.parent = current;
                openList.add(child);
            }
        }
    }


    private Node findNodeInOpen(Coord coord)
    {
        if (coord == null || openList.isEmpty()) return null;
        for (Node node : openList)
        {
            if (node.coord.equals(coord))
            {
                return node;
            }
        }
        return null;
    }


    private boolean isEndNodeContain(Map<Coord,Node> end,Coord coord)
    {
        for (Coord coord1:end.keySet())
        {
            if(coord1.equals(coord))
            {
                return true;
            }

        }

        return false;
    }

    private int calcH(Coord end,Coord coord)
    {
        return Math.abs(end.x - coord.x)
                + Math.abs(end.y - coord.y);
    }


    private boolean isEndNode(Coord end,Coord coord)
    {
        return coord != null && end.equals(coord);
    }

    private boolean canAddNodeToOpen(MapInformation mapInfo,int x, int y)
    {
        // 是否在地图中
        if (x < 0 || x >= mapInfo.width || y < 0 || y >= mapInfo.hight) return false;
        // 判断是否是不可通过的结点
        if (mapInfo.maps[y][x] == BAR) return false;
        // 判断结点是否存在close表
        if (isCoordInClose(x, y)) return false;

        return true;
}


    private boolean isCoordInClose(Coord coord)
    {
        return coord!=null&&isCoordInClose(coord.x, coord.y);
    }


    private boolean isCoordInClose(int x, int y)
    {
        if (closeList.isEmpty()) return false;
        for (Node node : closeList)
        {
            if (node.coord.x == x && node.coord.y == y)
            {
                return true;
            }
        }
        return false;
    }

}
