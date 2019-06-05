package com.boswinner.service;

import com.boswinner.entity.*;


import java.util.*;


public class GridService {

    public final static int BAR = 0; // 障碍值
    public final static int PATH = 2; // 路径
    public final static int DIRECT_VALUE = 10; // 横竖移动代价
    public final static int OBLIQUE_VALUE = 14; // 斜移动代价
    private static int COLUMN_NUM;
    private Map<Double, BosGrids> gridsMap = new HashMap<>();
    Queue<Node> openList = new PriorityQueue<Node>(); // 优先队列(升序)
    List<Node> closeList = new ArrayList<Node>();
    List<Entrance> pathList = new ArrayList<>();




    /**
    * @Description: 开始算法
    * @Param: [mapInfo]
    * @return: java.util.List<com.boswinner.bos.entity.Entrance>
    * @Author: Wang
    * @Date: 2019/3/27
    */

    public List<Entrance> start(MapInfo mapInfo)
    {
        /**
         * getgrids
         * downlod fastdfs
         * */

        if(mapInfo==null) return null;
        // clean
        openList.clear();
        closeList.clear();
        // 开始搜索
        openList.add(mapInfo.start);
        moveNodes(mapInfo);
        Collections.reverse(pathList);
//        System.out.print(pathList+"\n");
        return pathList;
    }


    /**
    * @Description: 根据用户的输入点返回查询到的合适栅格数据
    * @Param: [poi]
    * @return: java.lang.String
    * @Author: Wang
    * @Date: 2019/3/27
    */
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
    * @Description: 通过filekey查询并将信息存储到gridmap中
    * @Param: [filekey]
    * @return: void
    * @Author: Wang
    * @Date: 2019/3/27
    */
    private  void getGrids(Iterable<BosGrids> grids){


        for(BosGrids bosGrid:grids)
        {
            gridsMap.put(Double.valueOf(bosGrid.getGridSettings().getHeight()),bosGrid);
        }
    }

//    private  List<BosGrids> bosGrids(String filekey){
//        Iterable<BosGrids> grids =  bosGridsRepository.findByModelAndPathNotNull(filekey);
//        List<BosGrids> bosGrids = Lists.newArrayList(grids);
//        return bosGrids;
//    }


    /**
    * @Description: 移动当前结点
    * @Param: [mapInfo]
    * @return: void
    * @Author: Wang
    * @Date: 2019/3/27
    */
    private void moveNodes(MapInfo mapInfo)
    {
        while (!openList.isEmpty())
        {
            if (isCoordInClose(mapInfo.end.coord))
            {
                drawPath(mapInfo.maps, mapInfo.end);
                break;
            }
            Node current = openList.poll();
            closeList.add(current);
            addNeighborNodeInOpen(mapInfo,current);
        }
    }




    /**
    * @Description: 在二维数组中绘制路径
    * @Param: [maps, end]
    * @return: void
    * @Author: Wang
    * @Date: 2019/3/27
    */
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

    /**
    * @Description: 添加所有邻结点到open表
    * @Param: [mapInfo, current]
    * @return: void
    * @Author: Wang
    * @Date: 2019/3/27
    */
    private void addNeighborNodeInOpen(MapInfo mapInfo,Node current)
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

    /**
    * @Description: 添加一个邻结点到open表
    * @Param: [mapInfo, current, x, y, value]
    * @return: void
    * @Author: Wang
    * @Date: 2019/3/27
    */
    private void addNeighborNodeInOpen(MapInfo mapInfo,Node current, int x, int y, int value)
    {
        if (canAddNodeToOpen(mapInfo,x, y))
        {
            Node end=mapInfo.end;
            Coord coord = new Coord(x, y);
            int G = current.G + value; // 计算邻结点的G值
            Node child = findNodeInOpen(coord);
            if (child == null)
            {
                int H=calcH(end.coord,coord); // 计算H值
                if(isEndNode(end.coord,coord))
                {
                    child=end;
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

    /**
    * @Description: 从Open列表中查找结点
    * @Param: [coord]
    * @return: com.boswinner.bos.entity.Node
    * @Author: Wang
    * @Date: 2019/3/27
    */
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


    /**
    * @Description: 计算H的估值：“曼哈顿”法，坐标分别取差值相加
    * @Param: [end, coord]
    * @return: int
    * @Author: Wang
    * @Date: 2019/3/27
    */
    private int calcH(Coord end,Coord coord)
    {
        return Math.abs(end.x - coord.x)
                + Math.abs(end.y - coord.y);
    }

    /**
    * @Description: 判断结点是否是最终结点
    * @Param: [end, coord]
    * @return: boolean
    * @Author: Wang
    * @Date: 2019/3/27
    */
    private boolean isEndNode(Coord end,Coord coord)
    {
        return coord != null && end.equals(coord);
    }

    /**
    * @Description: 判断结点能否放入Open列表
    * @Param: [mapInfo, x, y]
    * @return: boolean
    * @Author: Wang
    * @Date: 2019/3/27
    */
    private boolean canAddNodeToOpen(MapInfo mapInfo,int x, int y)
    {
        // 是否在地图中
        if (x < 0 || x >= mapInfo.width || y < 0 || y >= mapInfo.hight) return false;
        // 判断是否是不可通过的结点
        if (mapInfo.maps[y][x] == BAR) return false;
        // 判断结点是否存在close表
        if (isCoordInClose(x, y)) return false;

        return true;
    }

    /**
    * @Description: 判断坐标是否在close表中
    * @Param: [coord]
    * @return: boolean
    * @Author: Wang
    * @Date: 2019/3/27
    */
    private boolean isCoordInClose(Coord coord)
    {
        return coord!=null&&isCoordInClose(coord.x, coord.y);
    }

    /**
    * @Description: 判断坐标是否在close表中
    * @Param: [x, y]
    * @return: boolean
    * @Author: Wang
    * @Date: 2019/3/27
    */
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
