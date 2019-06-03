package service;


import entity.*;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;




public class HybridPathService {



    private static int COLUMN_NUM;



    private Map<Double, BosGrids> gridsMap = new HashMap<>();

    private int statu;

    /**
     * @Description: 根据用户的输入点返回查询到的合适栅格数据
     * @Param: [poi, unit]
     * @return: java.lang.Double
     * @Author: Wang
     * @Date: 2019/3/25
     */
    public Double getShortestFloor(double poi, String unit) {
        Double min = Double.POSITIVE_INFINITY;
        Double high = Double.POSITIVE_INFINITY;
        for (Double h : gridsMap.keySet()) {
            if (Math.abs((poi - h)) < min) {
                min = Math.abs((poi - h));
                high = h;
            }
        }
        this.setDistic(unit, min, 200.00, poi);
        return high;
    }

    /**
     * @Description: 根据用户的输入点返回查询到的第二临近的栅格数据
     * @Param: [poi, unit]
     * @return: java.lang.Double
     * @Author: Wang
     * @Date: 2019/3/25
     */
    public Double getSecondShortestFloor(double poi, String unit, Double pointH) {

        Map<Double, BosGrids> gridsTestMap = gridsMap;
        gridsTestMap.remove(pointH);

        Double min = Double.POSITIVE_INFINITY;
        Double high = Double.POSITIVE_INFINITY;
        for (Double h : gridsTestMap.keySet()) {
            if (Math.abs((poi - h)) < min) {
                min = Math.abs((poi - h));
                high = h;
            }
        }
        this.setDistic(unit, min, 200.00, poi);
        return high;
    }

    private void setDistic(String cUnit, Double distic, Double limitCM, Double pointHigh) {
        switch (cUnit) { // 设置离地距离限制
            case "0.1mm":
                if (null != distic && (distic < 0 || distic > limitCM * 100.0)) {
                    throw new PathExceptions("单位0.1mm时，gridWidth取值范围是 500-5000");
                }
                break;
            case "1mm":
                if (null != distic && (distic < 0 || distic > limitCM * 10)) {
                    throw new PathExceptions(pointHigh + "高度处的点超出地面2m范围，请重新打点");
                }
                break;
            case "1cm":
                if (null != distic && (distic < 0 || distic > limitCM)) {
                    throw new PathExceptions(pointHigh + "高度处的点超出地面2m范围，请重新打点");
                }
                break;
            case "1dm":
                if (null != distic && (distic < 0 || distic > limitCM / 10.0)) {
                    throw new PathExceptions(pointHigh + "高度处的点超出地面2m范围，请重新打点");
                }
                break;
            case "1m":
                if (null != distic && (distic < 0 || distic > limitCM / 100.0)) {
                    throw new PathExceptions(pointHigh + "高度处的点超出地面2m范围，请重新打点");
                }
                break;
            case "1ft": // 1英尺(ft)=30.48厘米(cm)
                if (null != distic && (distic < 0 || distic > limitCM / 30.48)) {
                    throw new PathExceptions(pointHigh + "高度处的点超出地面2m范围，请重新打点");
                }
                break;
            case "1inch": // 1英寸(in)=2.54厘米(cm)
                if (null != distic && (distic < 0 || distic > limitCM / 2.54)) {
                    throw new PathExceptions(pointHigh + "高度处的点超出地面2m范围，请重新打点");
                }
                break;
            default:
                throw new PathExceptions("单位输入有误");
        }
    }

    //混合路网获取最短路径
    public List<vertexpoi> getHybridShortest(String point1, String point2, String route, String filekey) throws IOException {
        String[] split1 = point1.split(",");
        String[] split2 = point2.split(",");
        if (split1.length != 3 || split2.length != 3) {
            throw new PathExceptions("请输入规范的始终点坐标");
        }

        vertexpoi vertexpoi1 = new vertexpoi();
        vertexpoi vertexpoi2 = new vertexpoi();

        vertexpoi1.setX(Double.parseDouble(split1[0]));
        vertexpoi1.setY(Double.parseDouble(split1[1]));
        vertexpoi1.setZ(Double.parseDouble(split1[2]));

        vertexpoi2.setX(Double.parseDouble(split2[0]));
        vertexpoi2.setY(Double.parseDouble(split2[1]));
        vertexpoi2.setZ(Double.parseDouble(split2[2]));

        this.getGrids(filekey);                                             //存储不同高度的栅格
        Optional<BosUnit> bosUnit = bosUnitRepository.findByKey(filekey);
        if (!bosUnit.isPresent() || bosUnit.get().getUnit() == null) {
            throw new PathExceptions("单位未提取，请提取单位");
        }
        String unit = bosUnit.get().getUnit();
        Double h1 = this.getShortestFloor(vertexpoi1.getZ(), unit);
        Double h2 = this.getShortestFloor(vertexpoi2.getZ(), unit);

        Map<Double, List<String>> pointsbyhigh = new HashMap<>();          //按高度存储拓扑点
        Map<String, vertexpoi> pointsbykey = new HashMap<>();

        Iterable<BosPoint> allByRoute = pointRepo.findAllByRoute(route);
        for (BosPoint bosPoint : allByRoute) {
            vertexpoi vertexpoi = new vertexpoi();
            vertexpoi.setX(bosPoint.getX());
            vertexpoi.setY(bosPoint.getY());
            vertexpoi.setZ(bosPoint.getZ());
            Double high = bosPoint.getZ();
            if (pointsbyhigh.containsKey(high)) {
                pointsbyhigh.get(high).add(bosPoint.getKey());
                pointsbykey.put(bosPoint.getKey(), vertexpoi);
            } else {
                List<String> vertexpoiList = new ArrayList<>();
                vertexpoiList.add(bosPoint.getKey());
                pointsbyhigh.put(high, vertexpoiList);
                pointsbykey.put(bosPoint.getKey(), vertexpoi);
            }
        }

        List<String> h1key = pointsbyhigh.get(h1);
        if (h1key == null) {
            h1 = this.getSecondShortestFloor(vertexpoi1.getZ(), unit, h1);
            h1key = pointsbyhigh.get(h1);
        }
        List<String> h2key = pointsbyhigh.get(h2);
        if (h2key == null) {
            h2 = this.getSecondShortestFloor(vertexpoi2.getZ(), unit, h2);
            h2key = pointsbyhigh.get(h2);
        }
        if (h1key != null && h2key != null) {
            Entrance e1star = this.getPosition(vertexpoi1, h1);
            Entrance e2star = this.getPosition(vertexpoi2, h2);
            if (e1star == null || e2star == null) {
                throw new PathExceptions("超出打点范围");
            }
            if (h1 == h2) {
                if (e1star.equals(e2star)) {
                    throw new PathExceptions("始终点距离过近，请重新打点");
                }

                if(gridsMap.get(h1).getGridSettings().getRow()*gridsMap.get(h1).getGridSettings().getCol()<=30000)
                {
                    List<vertexpoi> pathInOneFloor = this.getPathInFloor(e1star, e2star, h1);

                    pathInOneFloor.add(0,vertexpoi1);
                    pathInOneFloor.add(vertexpoi2);
                    gridsMap.clear();
                    return pathInOneFloor;
                }


            }


            final double startTime = System.nanoTime();

            /**
             * 通过栅格位置反向查询拓扑点的key值
             * **/
            Map<Entrance, String> keybyentranceh1 = this.saveEntrance(h1, h1key, pointsbykey);

            //TODO 计算h1key中的point到起点的最短距离的三个点

            List<Entrance> path1 = this.hybridpathlist(keybyentranceh1, h1, e1star);

            //TODO 判断这三个点是否存在总的路径当中，若存在，取最邻近的值，计算起点到该点的栅格路径并续接到之前的路径上

            String starkey = keybyentranceh1.get(path1.get(path1.size() - 1));

            path1.remove(path1.size() - 1);
            List<vertexpoi> uppath = this.toVertexpoi(h1, path1);                    //起点路径

            final double path1Time = System.nanoTime();
            double slicingTime = (path1Time - startTime) / 1.E9;

            Map<Entrance, String> keybyentranceh2 = this.saveEntrance(h2, h2key, pointsbykey);
            List<Entrance> path2 = this.hybridpathlist(keybyentranceh2, h2, e2star);
            Collections.reverse(path2);
            String endkey = keybyentranceh2.get(path2.get(0));

            path2.remove(0);
            List<vertexpoi> downpath = this.toVertexpoi(h2, path2);                 //终点路径


            final double path2Time = System.nanoTime();
            double slicingTime2 = (path2Time - path1Time) / 1.E9;

            Iterable<BosEdge> edgesByRouteId = edgeRepo.findAllByRoute(route);
            List<String> list = new ArrayList<String>();
            for (BosEdge bosEdge : edgesByRouteId) {
                list.add(bosEdge.getFrom());
                list.add(bosEdge.getTo());
                list.add(bosEdge.getDistance());
            }
            GraphService graphService = new GraphService(String.join(",", list));
            List<Integer> shortestPath = graphService.getShortestPath(Integer.parseInt(starkey), Integer.parseInt(endkey));
            if (shortestPath.size() == 0) {
                throw new PathExceptions("未能查询到拓扑可通路径");
            }

            List<vertexpoi> medpath = new ArrayList<>();                              //中间路径
            for (Integer integer : shortestPath) {
                Optional<BosPoint> byId = pointRepo.findByKey(integer.toString());
                if (byId.isPresent()) {
                    vertexpoi vertexpoi = new vertexpoi();
                    vertexpoi.setX(byId.get().getX());
                    vertexpoi.setY(byId.get().getY());
                    vertexpoi.setZ(byId.get().getZ());
                    medpath.add(vertexpoi);
                }
            }


            if(medpath.size()>6)
            {
                Entrance e1end = new Entrance();
                Entrance e2end = new Entrance();
                for (int i =0 ;i<2 ;i++)
                {
                    medpath.remove(0);

                }

                if(medpath.get(0).getZ() == h1&&medpath.get(medpath.size()-1).getZ() == h2) {
                    e1end = this.getPosition(medpath.get(0), h1);

                    for (int i = 0; i < 2; i++) {
                        medpath.remove(medpath.size() - 1);
                    }
                    e2end = this.getPosition(medpath.get(medpath.size() - 1), h1);
                    List<vertexpoi> pathA1 = this.getPathInFloor(e1star, e1end, h1);
                    List<vertexpoi> pathA2 = this.getPathInFloor(e2end, e2star, h2);

                    final double midTime = System.nanoTime();
                    double slicingTime3 = (midTime - path2Time) / 1.E9;
                    System.out.println("混合路径搜寻总时间" + (slicingTime3 + slicingTime2 + slicingTime));

                    pathA1.addAll(medpath);
                    pathA1.addAll(pathA2);
                    pathA1.add(0, vertexpoi1);
                    pathA1.add(vertexpoi2);
                    gridsMap.clear();
                    return pathA1;
                }

            }

            final double midTime = System.nanoTime();
            double slicingTime3 = (midTime - path2Time) / 1.E9;


            uppath.addAll(medpath);
            uppath.addAll(downpath);
            uppath.add(0, vertexpoi1);
            uppath.add(vertexpoi2);
            gridsMap.clear();
            return uppath;

        } else {
            gridsMap.clear();
            System.out.println(filekey+" : 栅格与拓扑路网高度不统一");
            throw new PathExceptions("未能查询到拓扑可通路径");
        }
    }


    private List<vertexpoi> getPathInFloor (Entrance e1,Entrance e2,Double floorHigh) throws IOException {
        List<vertexpoi> path = new ArrayList<>();
        InputStream is =  FastDFS.downloadFileAsStream(gridsMap.get(floorHigh).getPath());
        int[][] gridmap =  this.getCsvDataNew(is);


        MapInfo info=new MapInfo(gridmap,gridmap[0].length, gridmap.length,new Node(e1.getX(), e1.getY()), new Node(e2.getX(), e2.getY()));

        List<Entrance> entrancepath =new ArrayList<>();
        entrancepath = new GridService().start(info);
        for (Entrance entrance:entrancepath)
        {
            vertexpoi poi = new vertexpoi();
            poi.setX(((entrance.getY()+0.5)*gridsMap.get(floorHigh).getGridSettings().getGridHeight())+gridsMap.get(floorHigh).getGridSettings().getX());
            poi.setY(((entrance.getX()+0.5)*gridsMap.get(floorHigh).getGridSettings().getGridWidth())+gridsMap.get(floorHigh).getGridSettings().getY());
            poi.setZ(floorHigh);
            path.add(poi);
        }
        if(path.isEmpty())
        {
            throw new PathExceptions("无可达路径");
        }
        else
        {
            return path;
        }
    }



    /**
     * 通过filekey查询并将信息存储到gridmap中
     */
    private void getGrids(String filekey) {
        Iterable<BosGrids> grids = bosGridsRepository.findByModelAndPathNotNull(filekey);

        for (BosGrids bosGrid : grids) {
            gridsMap.put(Double.valueOf(bosGrid.getGridSettings().getHeight()), bosGrid);
        }
    }

    public int[][] getCsvDataNew(InputStream inputStream) throws IOException {

        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        String line = "";
        ArrayList<int[]> lineList = new ArrayList<int[]>();
        // Read a single line from the file until there are no more lines to read
        while ((line = br.readLine()) != null) {
            StringTokenizer st = new StringTokenizer(line, ",");
            COLUMN_NUM = st.countTokens();
            int[] currCol = new int[COLUMN_NUM];
            for (int i = 0; i < COLUMN_NUM; i++) {
                if (st.hasMoreTokens()) {
                    currCol[i] = Integer.parseInt(st.nextToken());
                }
            }
            lineList.add(currCol);
        }

        int[][] str = new int[lineList.size()][COLUMN_NUM];
        for (int i = 0; i < lineList.size(); i++) {
            for (int j = 0; j < COLUMN_NUM; j++) {
                str[i][j] = lineList.get(i)[j];
                //System.out.println(str[i][x]);
            }
        }

        br.close();

        return str;
    }

    private Entrance getPosition(vertexpoi poi, Double high) {
        Entrance entrance = new Entrance();

        entrance.setX((int) Math.floor((poi.getY() - gridsMap.get(high).getGridSettings().getY()) / gridsMap.get(high).getGridSettings().getGridWidth()));
        entrance.setY((int) Math.floor((poi.getX() - gridsMap.get(high).getGridSettings().getX()) / gridsMap.get(high).getGridSettings().getGridHeight()));
        if (entrance.getX() >= 0 && entrance.getX() <= gridsMap.get(high).getGridSettings().getCol() && entrance.getY() >= 0 && entrance.getY() <= gridsMap.get(high).getGridSettings().getRow()) {
            return entrance;
        } else return null;

    }

    private Map<Entrance, String> saveEntrance(Double high, List<String> keys, Map<String, vertexpoi> pointsbykey) { //将entrance与key的关系相关联
        Map<Entrance, String> keybyentrance = new HashMap<>();
        for (String key : keys) {
            vertexpoi vertexpoi = pointsbykey.get(key);
            Entrance entrance = this.getPosition(vertexpoi, high);
            if (entrance != null) {
                keybyentrance.put(entrance, key);
            }
        }
        return keybyentrance;
    }

    private List<Entrance> hybridpathlist(Map<Entrance, String> keybyentranceh, Double high, Entrance star) throws IOException {
        List<Entrance> path = new ArrayList<>();
        InputStream is = FastDFS.downloadFileAsStream(gridsMap.get(high).getPath());
        int[][] gridmap = this.getCsvDataNew(is);
        int a = gridmap[star.getY()][star.getX()];
        if (a == 0) {
            throw new PathExceptions("高度为：" + high + "处的打点为障碍物点");
        }
        List<Node> entrances = new ArrayList<>();
        for (Entrance entrance : keybyentranceh.keySet()) {
            entrances.add(new Node(entrance.getX(), entrance.getY()));
        }
        MapInformation info = new MapInformation(gridmap, gridmap[0].length, gridmap.length, new Node(star.getX(), star.getY()), entrances);
        path = new HybridRoteService().start(info);

        if (path.size() == 0) {
            System.out.println("高度为：" + high + "未能搜寻到拓扑叶子节点");
            throw new PathExceptions("未能查询到拓扑可通路径");
        }

        return path;
    }

    private List<vertexpoi> toVertexpoi(Double floorHigh, List<Entrance> entrancepath) {
        List<vertexpoi> path = new ArrayList<>();
        for (Entrance entrance : entrancepath) {
            vertexpoi poi = new vertexpoi();
            poi.setX(((entrance.getY() + 0.5) * gridsMap.get(floorHigh).getGridSettings().getGridHeight()) + gridsMap.get(floorHigh).getGridSettings().getX());
            poi.setY(((entrance.getX() + 0.5) * gridsMap.get(floorHigh).getGridSettings().getGridWidth()) + gridsMap.get(floorHigh).getGridSettings().getY());
            poi.setZ(floorHigh);
            path.add(poi);
        }
        return path;
    }


    public List<List<Double>> getShortest(String point1, String point2, List<String> relationlist,Map<String,List<Double>> points,String route) {
        final double startTime = System.nanoTime();
        String proximalPoint = getProximalPoint(point1, point2, route);
        if ("".equals(proximalPoint)) {
            statu = 1;
            return null;
        }
        if ("1".equals(proximalPoint)) {
            statu = 2;
            return null;
        }
        String[] split = proximalPoint.split(",");


        GraphService graphService = new GraphService(String.join(",", relationlist));
        List<Integer> shortestPath = graphService.getShortestPath(Integer.parseInt(split[0]), Integer.parseInt(split[1]));
        if (shortestPath.size() == 1) {
            statu = 3;
            return null;
        }
        List<List<Double>> list1 = null;
        for (int i = 0; i < shortestPath.size() - 1; i++) {
            list1 = new ArrayList<>();
            list1.add(points.get(shortestPath.get(i).toString()));
        }
        final double getOutlineEndTime = System.nanoTime();
        System.out.println(String.format("拓扑最短路径提取完成！共耗时 %.2f 秒。",
                (getOutlineEndTime - startTime) / 1.E9));
        return list1;
    }


    public String getProximalPoint(String point1, String point2, Map<String,List<Double>> points) {
        double x1 = new Double(0), x2 = new Double(0);
        double y1 = new Double(0), y2 = new Double(0);
        double z1 = new Double(0), z2 = new Double(0);

            x1 = Double.parseDouble(point1.split(",")[0]);
            y1 = Double.parseDouble(point1.split(",")[1]);
            z1 = Double.parseDouble(point1.split(",")[2]);


            x2 = Double.parseDouble(point2.split(",")[0]);
            y2 = Double.parseDouble(point2.split(",")[1]);
            z2 = Double.parseDouble(point2.split(",")[2]);


        for (String key : points.keySet()) {
            List<Double> v2 = points.get(key)
            //求出两个点之间的距离
            double distance1 = Math.sqrt((x1 - v2[0]) * (x1 - v2.getX()) + ((y1 - v2.getY()) * (y1 - v2.getY())) + (z1 - v2.getZ()) * (z1 - v2.getZ()));
            //保留三位小数
            distance1 = (double) Math.round(distance1 * 1000) / 1000;
            treeMap1.put(distance1, v2.getKey());

            //求出两个点之间的距离
            double distance2 = Math.sqrt((x2 - v2.getX()) * (x2 - v2.getX()) + ((y2 - v2.getY()) * (y2 - v2.getY())) + (z2 - v2.getZ()) * (z2 - v2.getZ()));
            //保留三位小数
            distance2 = (double) Math.round(distance2 * 1000) / 1000;
            treeMap2.put(distance2, v2.getKey());
        }
        if (treeMap1.size() <= 0) {
            return "1";
        }
        Iterator<Double> iterator = treeMap1.keySet().iterator();
        String str = "";
        for (int i = 0; i < treeMap1.size(); i++) {
            Double key = iterator.next();
            str += treeMap1.get(key);
            break;
        }

        Iterator<Double> iterator2 = treeMap2.keySet().iterator();
        for (int i = 0; i < treeMap2.size(); i++) {
            Double key = iterator2.next();
            str += "," + treeMap2.get(key);
            break;
        }
        return str;
    }



    /**
     * 获取栅格地图给定两个点之间的最短路径
     *
     * @param point1
     * @param point2
     * @return
     */
    public List<vertexpoi> gridShortest(String model, String point1, String point2) throws IOException {
        final double startTime = System.nanoTime();

        String[] split1 = point1.split(",");
        String[] split2 = point2.split(",");
        if (split1.length == 3 && split1.length == split2.length) {
            vertexpoi vertexpoi1 = new vertexpoi();
            vertexpoi vertexpoi2 = new vertexpoi();

            vertexpoi1.setX(Double.parseDouble(split1[0]));
            vertexpoi1.setY(Double.parseDouble(split1[1]));
            vertexpoi1.setZ(Double.parseDouble(split1[2]));

            vertexpoi2.setX(Double.parseDouble(split2[0]));
            vertexpoi2.setY(Double.parseDouble(split2[1]));
            vertexpoi2.setZ(Double.parseDouble(split2[2]));

            GridPathService gridPathService = new GridPathService(bosStairGridRepo, bosGridsRepo);

            Optional<BosUnit> bosUnit = bosUnitRepository.findByKey(model);
            if (!bosUnit.isPresent() || bosUnit.get().getUnit() == null) {
                throw new PathExceptions("单位未提取，请提取单位");
            }
            String unit = bosUnit.get().getUnit();
            gridPathService.setGridPath(model, vertexpoi1, vertexpoi2, unit);
            List<vertexpoi> gridPathOverFloor = gridPathService.getGridPathOverFloor();

            System.out.println("最短路径点数=" + gridPathOverFloor.size());

            //提取计时
            final double getOutlineEndTime = System.nanoTime();
            System.out.println(String.format(model + ":获取栅格地图最短路径完成！共耗时 %.2f 秒。",
                    (getOutlineEndTime - startTime) / 1.E9));

            if (gridPathOverFloor.size() != 0)

            {
                gridPathOverFloor.add(0, vertexpoi1);
                gridPathOverFloor.add(vertexpoi2);
                return gridPathOverFloor;
            } else
                return new ArrayList<vertexpoi>();

        }
        return null;
    }


}