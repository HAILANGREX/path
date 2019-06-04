package service;


import entity.*;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;


public class GridPathService {

    private static int COLUMN_NUM;
    private Map<Double, BosGrids> gridsMap = new HashMap<>();   //楼层及其对应栅格数据map
    private Map<Integer,List<Integer>> stairsConnect = new HashMap<>();  //楼层连接关系map
    private Map<Integer,List<Entrance>> entrancemap = new HashMap<>();   //每层楼的楼梯口
    private Map<Integer,Map<String,List<Entrance>>> entranceInMap = new HashMap<>();   //每层楼的楼梯口
    private List<vertexpoi> gridPathOverFloor = new ArrayList<>();  //最终的输出路径

    private List<BosStairGrid> bosStairGrids = new ArrayList<>(); //初始化解析获取的stairGrids

    private BosStairGridRepository bosStairGridRepo;
    private BosGridsRepository bosGridsRepo;

    /**
    * @Description: 获取所有楼层之间的联系
    * @Param: [filekey]
    * @return: void
    * @Author: Wang
    * @Date: 2019/3/25
    */
    private void getStair(String filekey){
        for(BosStairGrid stair:bosStairGrids)
        {
            this.putStairsconnect(stair.getStartheight(),stair.getEndheight());
            this.putStairsconnect(stair.getEndheight(),stair.getStartheight());
            this.putEntranceUpFloor(stair.getStartheight(),stair.getStartgrid());
            this.putEntranceDownFloor(stair.getEndheight(),stair.getEndgrid());
        }
    }

//    public List<String> getgrids(String model)
//    {
//        List<String> fileList = new ArrayList<String>();
//        for (BosGrids bosGrids1 : bosGrids) {
//            fileList.add(bosGrids1.getPath());
//        }
//       return fileList;
//    }

    public void setGridPath(String modelkey,vertexpoi poi1,vertexpoi poi2,String unit) throws IOException,PathExceptions {
        this.getGrids(modelkey);
        this.getStair(modelkey);
        this.setDijkStraMap(stairsConnect);
        Double h1 = this.getShortestFloor(poi1.getZ(),unit);
        Double h2 = this.getShortestFloor(poi2.getZ(),unit);
        Entrance e1 =this.getPosition(poi1,h1);
        Entrance e2 =this.getPosition(poi2,h2);
        if(null == e1 && null != e2){
            throw new PathExceptions("起始点超出范围");
        }
        if(null == e2 && null != e1){
            throw new PathExceptions("终点超出范围");
        }
        if(null == e2 && null == e1){
            throw new PathExceptions("起始点,终点均超出范围");
        }


        if(h1.equals(h2))
        {
            if(e1!=null&&e2!=null)
            {
                gridPathOverFloor.addAll(this.getPathInFloor(e1,e2,h1)) ;
            }
        }
        else {
            floorPath=this.getShortestPath((int)Math.ceil(h1),(int)Math.ceil(h2) );

            if(floorPath.isEmpty()||floorPath.size()==1){
                throw new PathExceptions("无楼层连接关系");
            }
            this.getPathOverFloor(e1,e2,modelkey);
        }
    }

    private void putEntranceUpFloor(Integer floor,Entrance entrance)
    {
        if(!entranceInMap.containsKey(floor))
        {
            Map<String,List<Entrance>> map = new HashMap<String,List<Entrance>>();
            List<Entrance> entrances = new ArrayList<>();
            map.put("up",entrances);
            map.get("up").add(entrance);
            entranceInMap.put(floor,map);
        }
        else {
            if (entranceInMap.get(floor).get("up") != null)
            {
                entranceInMap.get(floor).get("up").add(entrance);
            }
            else
            {
                List<Entrance> entrances = new ArrayList<>();
                entranceInMap.get(floor).put("up",entrances);
                entranceInMap.get(floor).get("up").add(entrance);
            }

        }
    }

    private void putEntranceDownFloor(Integer floor,Entrance entrance)
    {
        if(!entranceInMap.containsKey(floor))
        {
            Map<String,List<Entrance>> map = new HashMap<String,List<Entrance>>();
            List<Entrance> entrances = new ArrayList<>();
            map.put("down",entrances);
            map.get("down").add(entrance);
            entranceInMap.put(floor,map);
        }
        else {
            if (entranceInMap.get(floor).get("down") != null)
            {
                entranceInMap.get(floor).get("down").add(entrance);
            }
            else
            {
                List<Entrance> entrances = new ArrayList<>();
                entranceInMap.get(floor).put("down",entrances);
                entranceInMap.get(floor).get("down").add(entrance);
            }

        }
    }

    private void putStairsconnect(Integer h1,Integer h2){
        if (stairsConnect.containsKey(h1))
        {
            stairsConnect.get(h1).add(h2);
        }
        else {
            List<Integer> high = new ArrayList<>();
            stairsConnect.put(h1,high);
            stairsConnect.get(h1).add(h2);
        }
    }

    /**
    * @Description: 获取其中某一层楼里两点间的路径
    * @Param: [e1, e2, floorHigh]
    * @return: java.util.List<com.boswinner.bos.entity.vertexpoi>
    * @Author: Wang
    * @Date: 2019/3/25
    */
    private List<vertexpoi> getPathInFloor (Entrance e1,Entrance e2,Double floorHigh) throws IOException,PathExceptions {
        List<vertexpoi> path = new ArrayList<>();
        InputStream is =  FastDFS.downloadFileAsStream(gridsMap.get(floorHigh).getPath());
        int[][] gridmap =  this.getCsvDataNew(is);

        int a=gridmap[e1.getY()][e1.getX()];
        int b=gridmap[e2.getY()][e2.getX()];
        if(a == 0 && b != 0){
            throw new PathExceptions("高度为："+floorHigh+"处的起始点为障碍物");
        }
        if(b == 0 && a != 0){
            throw new PathExceptions("高度为："+floorHigh+"处的终点为障碍物");
        }
        if(a == 0 && b == 0){
            throw new PathExceptions("高度为："+floorHigh+"处的起始点，终点均为障碍物");
        }

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
    * @Description: 输入点获取该点所在地图的栅格位置
    * @Param: [poi, high]
    * @return: com.boswinner.bos.entity.Entrance
    * @Author: Wang
    * @Date: 2019/3/25
    */

    private Entrance getPosition(vertexpoi poi ,Double high){
        Entrance entrance = new Entrance();

        entrance.setX((int)Math.floor((poi.getY()-gridsMap.get(high).getGridSettings().getY())/gridsMap.get(high).getGridSettings().getGridWidth()));
        entrance.setY((int)Math.floor((poi.getX()-gridsMap.get(high).getGridSettings().getX())/gridsMap.get(high).getGridSettings().getGridHeight()));
        if(entrance.getX()>=0&&entrance.getX()<=gridsMap.get(high).getGridSettings().getCol()&&entrance.getY()>=0&&entrance.getY()<=gridsMap.get(high).getGridSettings().getRow())
        {
            return entrance;
        }
        else return null;

    }

    public List<vertexpoi> getGridPathOverFloor() {
        return gridPathOverFloor;
    }

    public void setGridPathOverFloor(List<vertexpoi> gridPathOverFloor) {
        this.gridPathOverFloor = gridPathOverFloor;
    }

    /**
    * @Description: 跨楼层路径
    * @Param: [star, end, filekey]
    * @return: void
    * @Author: Mr.Wang
    * @Date: 2019/3/25
    */
    private void getPathOverFloor(Entrance star,Entrance end,String filekey) throws IOException {
        Entrance inter =new Entrance();
        Entrance inter2 = new Entrance();

        for(Integer i=0;i<floorPath.size()-1;i++)
        {
            if(i ==0)
            {
                if(floorPath.get(0)<floorPath.get(1)) {
                    inter = this.shortestUpEntrance(star, floorPath.get(0));
                    gridPathOverFloor.addAll(this.getPathInFloor(star, inter, Double.valueOf(floorPath.get(0))));
                }
                else
                {
                    inter = this.shortestDownEntrance(star, floorPath.get(0));
                    gridPathOverFloor.addAll(this.getPathInFloor(star, inter, Double.valueOf(floorPath.get(0))));
                }

            }else {
                gridPathOverFloor.addAll(this.getPathInFloor(inter2,inter,Double.valueOf(floorPath.get(i))));
            }
            Integer minfloor ;
            Integer maxfloor ;
            if(floorPath.get(i)<floorPath.get(i+1))
            {
                minfloor = floorPath.get(i);
                maxfloor =floorPath.get(i+1);

                BosStairGrid bosStairGrids = this.getBosStairGrid(filekey,minfloor,maxfloor,inter.getX(),inter.getY(),"start");
                if(null != bosStairGrids){
                    gridPathOverFloor.addAll(bosStairGrids.getXyzlist());
                    inter2 =bosStairGrids.getEndgrid();
                }
                inter = this.shortestUpEntrance(inter2,floorPath.get(i+1));
            }
            else {
                maxfloor = floorPath.get(i);
                minfloor =floorPath.get(i+1);
                BosStairGrid bosStairGrids = this.getBosStairGrid(filekey,minfloor,maxfloor,inter.getX(),inter.getY(),"end");
                if(null != bosStairGrids){
                    List<vertexpoi> path1 = bosStairGrids.getXyzlist();
                    Collections.reverse(path1);
                    gridPathOverFloor.addAll(path1);
                    inter2 = bosStairGrids.getStartgrid();
                }
                inter = this.shortestDownEntrance(inter2,floorPath.get(i+1));
            }
        }
        gridPathOverFloor.addAll(this.getPathInFloor(inter2,end,Double.valueOf(floorPath.get(floorPath.size()-1))));
    }

    /**
    * @Description: 获取最邻近路口
    * @Param: [entrance, floor]
    * @return: com.boswinner.bos.entity.Entrance
    * @Author: Wang
    * @Date: 2019/3/25
    */

    private Entrance shortestDownEntrance(Entrance entrance,Integer floor){
        Entrance shortest = new Entrance();
        Integer min = Integer.MAX_VALUE;
        List<Entrance> entrances = entranceInMap.get(floor).get("down");
        if(entrances!=null){
            for(Entrance entr:entrances){
                Integer dis = (entr.getX()-entrance.getX())*(entr.getX()-entrance.getX())+(entr.getY()-entrance.getY())*(entr.getY()-entrance.getY());
                if(dis<min)
                {
                    shortest = entr;
                    min =dis;
                }
            }
            return shortest;}
        else
            return null;
    }

    private Entrance shortestUpEntrance(Entrance entrance,Integer floor){
        Entrance shortest = new Entrance();
        Integer min = Integer.MAX_VALUE;
        List<Entrance> entrances = entranceInMap.get(floor).get("up");
        if(entrances!=null) {
            for (Entrance entr : entrances) {
                Integer dis = (entr.getX() - entrance.getX()) * (entr.getX() - entrance.getX()) + (entr.getY() - entrance.getY()) * (entr.getY() - entrance.getY());
                if (dis < min) {
                    shortest = entr;
                    min = dis;
                }
            }
            return shortest;
        }
        else
            return null;
    }
    private Entrance shortestEntrance(Entrance entrance,Integer floor){
        Entrance shortest = new Entrance();
        Integer min = Integer.MAX_VALUE;
        List<Entrance> entrances = entranceInMap.get(floor).get("up");
        for(Entrance entr:entrances){
            Integer dis = (entr.getX()-entrance.getX())*(entr.getX()-entrance.getX())+(entr.getY()-entrance.getY())*(entr.getY()-entrance.getY());
            if(dis<min)
            {
                shortest = entr;
                min =dis;
            }

        }
        List<Entrance> entrancess = entranceInMap.get(floor).get("down");
        for(Entrance entr:entrances){
            Integer dis = (entr.getX()-entrance.getX())*(entr.getX()-entrance.getX())+(entr.getY()-entrance.getY())*(entr.getY()-entrance.getY());
            if(dis<min)
            {
                shortest = entr;
                min =dis;
            }
        }
        return shortest;
    }
    /**
    * @Description: 多个楼层之间的关系
    * @Param:
    * @return:
    * @Author: Wang
    * @Date: 2019/3/25
    */
    private final Map<Integer, List<Vertex>> vertices;
    private List<Integer> floorPath = new ArrayList<>();

    public GridPathService(BosStairGridRepository bosStairGridRepository,BosGridsRepository bosGridsRepository){
        this.vertices = new HashMap<Integer, List<Vertex>>();
        this.bosStairGridRepo = bosStairGridRepository;
        this.bosGridsRepo = bosGridsRepository;
    }

    private void setDijkStraMap(Map<Integer, List<Integer>> stairsconnect){
        for (Integer key:stairsconnect.keySet())
        {

            this.addVertex(key,setVertexList(stairsconnect.get(key)));
        }

    }

    public List<Vertex> setVertexList(List<Integer> connect){
        List<Vertex> vertec = new ArrayList<>();
        for (Integer ve:connect){
            Vertex vertex =  new Vertex(ve,1);
            if(!vertec.contains(vertex))
                vertec.add(vertex);
        }
        return vertec;
    }

    public void addVertex(Integer character, List<Vertex> vertex) {
        this.vertices.put(character, vertex);
    }

    public List<Integer> getShortestPath(Integer start, Integer finish) {
        final Map<Integer, Integer> distances = new HashMap<Integer, Integer>();
        final Map<Integer, Vertex> previous = new HashMap<Integer, Vertex>();
        PriorityQueue<Vertex> nodes = new PriorityQueue<Vertex>();
        for(Integer vertex : vertices.keySet()) {
            if (vertex.intValue() ==start.intValue()) {
                distances.put(vertex, 0);
                nodes.add(new Vertex(vertex, 0));
            } else {
                distances.put(vertex, Integer.MAX_VALUE);
                nodes.add(new Vertex(vertex, Integer.MAX_VALUE));
            }
            previous.put(vertex, null);
        }
        while (!nodes.isEmpty()) {
            Vertex smallest = nodes.poll();
            if (smallest.getId().intValue() == finish.intValue()) {
                final List<Integer> path = new ArrayList<Integer>();
                while (previous.get(smallest.getId()) != null) {
                    path.add(smallest.getId());
                    smallest = previous.get(smallest.getId());
                }
                path.add(start);
                Collections.reverse(path);
                return path;
            }
            if (distances.get(smallest.getId()) == Integer.MAX_VALUE) {
                break;
            }
            for (Vertex neighbor : vertices.get(smallest.getId())) {
                double alt = distances.get(smallest.getId()) + neighbor.getDistance();
                if (alt < distances.get(neighbor.getId())) {
                    distances.put(neighbor.getId(), (int)alt);
                    previous.put(neighbor.getId(), smallest);
                    forloop:
                    for(Vertex n : nodes) {
                        if (n.getId().intValue() == neighbor.getId().intValue()) {
                            nodes.remove(n);
                            n.setDistance(alt);
                            nodes.add(n);
                            break forloop;
                        }
                    }
                }
            }
        }

        return new ArrayList<Integer>();
    }

    /**
    * @Description: 根据用户的输入点返回查询到的合适栅格数据
    * @Param: [poi, unit]
    * @return: java.lang.Double
    * @Author: Wang
    * @Date: 2019/3/25
    */
    public Double getShortestFloor(double poi,String unit){
        Double min=Double.POSITIVE_INFINITY ;
        Double high = Double.POSITIVE_INFINITY;
        for(Double h:gridsMap.keySet())
        {
            if( Math.abs((poi-h)) <min )
            {
                min = Math.abs((poi-h));
                high=h;
            }
        }
        this.setDistic(unit,min,200.00,poi);
        return high;
    }

    private void setDistic(String cUnit, Double distic,Double limitCM,Double pointHigh) throws PathExceptions {
        switch (cUnit) { // 设置离地距离限制
            case "0.1mm":
                if (null != distic && (distic < 0||distic > limitCM*100.0)) {
                    throw new PathExceptions("单位0.1mm时，gridWidth取值范围是 500-5000");
                }
                break;
            case "1mm":
                if (null != distic && (distic < 0||distic > limitCM*10)) {
                    throw new PathExceptions(pointHigh+"高度处的点超出地面2m范围，请重新打点");
                }
                break;
            case "1cm":
                if (null != distic && (distic < 0||distic > limitCM)) {
                    throw new PathExceptions(pointHigh+"高度处的点超出地面2m范围，请重新打点");
                }
                break;
            case "1dm":
                if (null != distic && (distic < 0||distic > limitCM/10.0)) {
                    throw new PathExceptions( pointHigh+"高度处的点超出地面2m范围，请重新打点");
                }
                break;
            case "1m":
                if (null != distic && (distic < 0||distic > limitCM/100.0)) {
                    throw new PathExceptions(pointHigh+"高度处的点超出地面2m范围，请重新打点");
                }
                break;
            case "1ft": // 1英尺(ft)=30.48厘米(cm)
                if (null != distic && (distic < 0||distic > limitCM/30.48)) {
                    throw new PathExceptions( pointHigh+"高度处的点超出地面2m范围，请重新打点");
                }
                break;
            case "1inch": // 1英寸(in)=2.54厘米(cm)
                if (null != distic && (distic < 0||distic > limitCM/2.54)) {
                    throw new PathExceptions(pointHigh+"高度处的点超出地面2m范围，请重新打点");
                }
                break;
            default:
                throw new PathExceptions("单位输入有误");
        }
    }

    /**
    * @Description: 根据用户的输入点返回查询到的合适栅格数据
    * @Param: [filekey]
    * @return: void
    * @Author: Wang
    * @Date: 2019/3/25
    */
    private  void getGrids(String filekey){
        for(BosGrids bosGrid:bosGrids)
        {
            gridsMap.put(bosGrid.getGridSettings().getHeight(),bosGrid);
        }
    }

    /**
    * @Description: 将FASTDFS中的返回的栅格文件流读入生成二维数组图
    * @Param: [inputStream]
    * @return: int[][]
    * @Author: Wang
    * @Date: 2019/3/25
    */
    public  int[][] getCsvDataNew(InputStream inputStream) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        String line = "";
        ArrayList<int[]> lineList = new ArrayList<int[]>();
        // Read a single line from the file until there are no more lines to read
        while((line = br.readLine()) != null) {
            StringTokenizer st = new StringTokenizer(line, ",");
            COLUMN_NUM =st.countTokens();
            int[] currCol = new int[COLUMN_NUM];
            for(int i = 0; i < COLUMN_NUM; i++) {
                if(st.hasMoreTokens()){
                    currCol[i]  = Integer.parseInt(st.nextToken());
                }

            }
            lineList.add(currCol);
        }
        int[][] str = new int[lineList.size()][COLUMN_NUM];
        for(int i = 0; i < lineList.size(); i++) {
            for(int j = 0; j < COLUMN_NUM; j++) {
                str[i][j] = lineList.get(i)[j];
                //System.out.println(str[i][x]);
            }
        }
        br.close();
        return str;
    }

    /**
     * 初始化楼梯连接参数
     */
    public void getBosStairGrid(List<Map<String,Object>> stairGrids){
        for(Map<String,Object> map : stairGrids){
            BosStairGrid bosStairGrid = new BosStairGrid();
            bosStairGrid.setModel(map.get("model").toString());
            //startgrid
            Map<String,Object> startgrid = (Map<String, Object>) map.get("startgrid");
            Entrance entrance = new Entrance();
            entrance.setX(Integer.parseInt(startgrid.get("x").toString()));
            entrance.setY(Integer.parseInt(startgrid.get("y").toString()));
            bosStairGrid.setStartgrid(entrance);
            //xyzlist
            List<Map<String,Object>> xyzlist = (List<Map<String, Object>>) map.get("xyzlist");
            List<vertexpoi> vertexpois = new ArrayList<>();
            for(Map<String,Object> xyzMap : xyzlist){
                vertexpoi vertexpoi = new vertexpoi();
                vertexpoi.setX(Double.parseDouble(xyzMap.get("x").toString()));
                vertexpoi.setY(Double.parseDouble(xyzMap.get("y").toString()));
                vertexpoi.setZ(Double.parseDouble(xyzMap.get("z").toString()));
                vertexpois.add(vertexpoi);
            }
            bosStairGrid.setXyzlist(vertexpois);
            bosStairGrid.setStartheight(Integer.parseInt(map.get("startheight").toString()));
            bosStairGrid.setEndheight(Integer.parseInt(map.get("endheight").toString()));
            //endgrid
            Map<String,Object> endgrid = (Map<String, Object>) map.get("endgrid");
            Entrance entranc = new Entrance();
            entranc.setX(Integer.parseInt(endgrid.get("x").toString()));
            entranc.setY(Integer.parseInt(endgrid.get("y").toString()));
            bosStairGrid.setEndgrid(entranc);
            bosStairGrids.add(bosStairGrid);
        }
    }

    /**
     * 获取Grids数据
     */
    public void getGrids(List<Map<String,Object>> grids){
        for(Map<String,Object> map : grids){
            BosGrids bosGrid = new BosGrids();
            GridSettings gridSettings = new GridSettings();
            Map<String,Object> gridSettingMap = (Map<String, Object>) map.get("gridSettings");
            gridSettings.setGridHeight(Double.parseDouble(gridSettingMap.get("gridHeight").toString()));
            gridSettings.setCol(Integer.parseInt(gridSettingMap.get("col").toString()));
            gridSettings.setUnit(gridSettingMap.get("unit").toString());
            gridSettings.setX(Double.parseDouble(gridSettingMap.get("x").toString()));
            gridSettings.setY(Double.parseDouble(gridSettingMap.get("y").toString()));
            gridSettings.setRow(Integer.parseInt(gridSettingMap.get("row").toString()));
            gridSettings.setGridWidth(Double.parseDouble(gridSettingMap.get("gridWidth").toString()));
            gridSettings.setStatus(gridSettingMap.get("status").toString());
            gridSettings.setHeight(Double.parseDouble(gridSettingMap.get("height").toString()));
            bosGrid.setGridSettings(gridSettings);
            bosGrid.setPath(map.get("path").toString());
            bosGrid.setModel(map.get("model").toString());
            gridsMap.put(Double.parseDouble(gridSettingMap.get("height").toString()),bosGrid);
        }
    }

    /**
     * 查询起点楼梯或终点楼梯
     * @param filekey
     * @param minfloor
     * @param maxfloor
     * @param x
     * @param y
     * @param str
     * @return
     */
    public BosStairGrid getBosStairGrid(String filekey,Integer minfloor ,Integer maxfloor,Integer x,Integer y,String str){
        for(BosStairGrid bosStairGrid : bosStairGrids){
            switch (str){
                case "start":
                    return filekey.equals(bosStairGrid.getModel()) && minfloor == bosStairGrid.getStartheight() && maxfloor == bosStairGrid.getEndheight() && x == bosStairGrid.getStartgrid().getX() && y == bosStairGrid.getStartgrid().getY()?bosStairGrid:null;
                case "end":
                    return filekey.equals(bosStairGrid.getModel()) && minfloor == bosStairGrid.getStartheight() && maxfloor == bosStairGrid.getEndheight() && x == bosStairGrid.getEndgrid().getX() && y == bosStairGrid.getEndgrid().getY()?bosStairGrid:null;
                default:break;
            }
        }
        return null;
    }
}
