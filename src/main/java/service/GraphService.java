package service;


import entity.Vertex;
import entity.vertexpoi;


import java.util.*;



public class GraphService {

    private List<vertexpoi> poiInMap = new ArrayList<>();
    private Map<Integer, List<Integer>> relation = new HashMap<>();
    private List<Integer> rela = new ArrayList<>();
    private int statu = 0;
    private void setDijkStraGraph(Map<Integer,Map<Integer,Double>> relDis)
    {
        for(Integer key:relDis.keySet())
        {
            this.addVertex(key,setVertexList(relDis.get(key)));
        }
    }
    public GraphService(String relationAndDistance)
    {
        this.inPutRelAndDis(relationAndDistance);
        this.vertices = new HashMap<>();
        setDijkStraGraph(relAndDis);
    }

    private Map<Integer,Map<Integer,Double>> relAndDis = new HashMap<>();
    /**
    * @Description:  关系与距离数据传入
    * @Param: [data]
    * @return: void
    * @Author: Mr.Wang
    * @Date: 2019/3/25
    */
    private void inPutRelAndDis(String data)
    {
        String[] sett = data.replace("[","").replace("]","").replace("\"","").split(",");


        for(int i =0;i<sett.length;i+=3)
        {
            if(relAndDis.containsKey(Integer.parseInt(sett[i])))
            {
                relAndDis.get(Integer.parseInt(sett[i])).put(Integer.parseInt(sett[i+1]),Double.parseDouble(sett[i+2]));
            }
            else
            {
                Map<Integer,Double> distance = new HashMap<>();
                relAndDis.put(Integer.parseInt(sett[i]),distance);
                relAndDis.get(Integer.parseInt(sett[i])).put(Integer.parseInt(sett[i+1]),Double.parseDouble(sett[i+2]));
            }
            if(relAndDis.containsKey(Integer.parseInt(sett[i+1])))
            {
                relAndDis.get(Integer.parseInt(sett[i+1])).put(Integer.parseInt(sett[i]),Double.parseDouble(sett[i+2]));
            }
            else
            {
                Map<Integer,Double> distance = new HashMap<>();
                relAndDis.put(Integer.parseInt(sett[i+1]),distance);
                relAndDis.get(Integer.parseInt(sett[i+1])).put(Integer.parseInt(sett[i]),Double.parseDouble(sett[i+2]));
            }
        }

    }
    private void setDijkStraMap(Map<Integer, List<Integer>> relMap){
        for (Integer key:relMap.keySet())
        {

            this.addVertex(key,setVertexList(key));
        }
    }

    private GraphService(List<Integer> relationList,List<Double> poinList)
    {
        poiInMap = setPoiInMap(poinList);
        relation = getRelationList(relationList);
        this.vertices = new HashMap<>();
        setDijkStraMap(relation);

    }




    /**
    * @Description:  将点的坐标存在list中
    * @Param: [pointList]
    * @return: java.util.List<com.boswinner.bos.entity.vertexpoi>
    * @Author: Wang
    * @Date: 2019/3/25
    */

    private List<vertexpoi> setPoiInMap(List<Double> pointList){

        for (int i = 0;i <pointList.size();i+=3)
        {
            vertexpoi tem = new vertexpoi();
            tem.setX(pointList.get(i));
            tem.setY(pointList.get(i+1));
            tem.setZ(pointList.get(i+2));
            poiInMap.add(tem);
        }

        return poiInMap;
    }

    private Map<Integer,Double> getDisByPoint(Integer point){
        Map<Integer,Double> disByPoint = new HashMap<>();
        List<Integer> points = relation.get(point);
        if (points!=null){
            for (Integer poi:points){
                Double dis = getDistance(point,poi);
                disByPoint.put(poi,dis);
            }}
        return disByPoint;
    }

    private Map<Integer, List<Integer>> getRelationList(List<Integer> relationList)
    {
        for(int i=0;i<relationList.size();i+=2)
        {
            setRel(relationList.get(i),relationList.get(i+1));
            setRel(relationList.get(i+1),relationList.get(i));
        }
        return relation;
    }

    private void setRel(Integer a,Integer b){
        if(relation.get(a)!=null)
            relation.get(a).add(b);
        else {
            List<Integer> point = new ArrayList<>();
            point.add(b);
            relation.put(a,point);
        }
    }

    private Double getDistance(Integer point1,Integer point2)
    {
        return getDistance(poiInMap.get(point1), poiInMap.get(point2));
    }

    private Double getDistance(vertexpoi vertex1, vertexpoi vertex2)
    {

        double distance =  Math.sqrt((vertex1.getX()-vertex2.getX())*(vertex1.getX()-vertex2.getX())+((vertex1.getY()-vertex2.getY())*(vertex1.getY()-vertex2.getY()))+(vertex1.getZ()-vertex2.getZ())*(vertex1.getZ()-vertex2.getZ()));
        distance = (double)Math.round(distance*1000)/1000;
        return distance;
    }

    private  Map<Integer, List<Vertex>> vertices;

    public List<Vertex> getVertices(Integer i)
    {
        return vertices.get(i);
    }

    public Integer getMinDistance(vertexpoi vertex1){
        int point = 0;
        double z = 3250;
        double distance = Double.POSITIVE_INFINITY;
        for (int i = 0; i< poiInMap.size(); i++)
        {
            if(vertex1.getZ()<= poiInMap.get(i).getZ()+z&&vertex1.getZ()>= poiInMap.get(i).getZ()-z) {
                double dis = getDistance(vertex1, poiInMap.get(i));
                if (dis < distance) {
                    distance = dis;
                    point = i;
                }
            }
        }
        return point;
    }


    /**
    * @Description: 把连接关系添加到最短路径关系中
    * @Param: [connect]
    * @return: java.util.List<com.boswinner.bos.entity.Vertex>
    * @Author: Wang
    * @Date: 2019/3/25
    */
    private List<Vertex> setVertexList(Map<Integer,Double> connect){
        List<Vertex> vertec = new ArrayList<>();
        for (Integer ve:connect.keySet()){
            vertec.add(new Vertex(ve,connect.get(ve)));
        }
        return vertec;
    }
    private List<Vertex> setVertexList(Integer point){
        List<Vertex> vertec = new ArrayList<>();
        Map<Integer,Double> connect = getDisByPoint(point);
        vertec = setVertexList(connect);
        return vertec;
    }



    private void addVertex(Integer Integer, List<Vertex> vertex) {
        if (this.vertices.get(Integer) != null)
            this.vertices.get(Integer).addAll(vertex);
        this.vertices.put(Integer, vertex);
    }

    public  List<List<List<Double>>> getPointPath(List<Integer> path)
    {
        List<List<List<Double>>> pointPathresult = new ArrayList<>();
        for (int i= 0;i<path.size()-1;i++)
        {
            List<List<Double>> pointPath = new ArrayList<>();
            List<Double> point1 = new ArrayList<>();
            List<Double> point2 = new ArrayList<>();
            point1.add(poiInMap.get(path.get(i)).getX());
            point1.add(poiInMap.get(path.get(i)).getY());
            point1.add(poiInMap.get(path.get(i)).getZ());
            point2.add(poiInMap.get(path.get(i+1)).getX());
            point2.add(poiInMap.get(path.get(i+1)).getY());
            point2.add(poiInMap.get(path.get(i+1)).getZ());
            pointPath.add(point1);
            pointPath.add(point2);
            pointPathresult.add(pointPath);
        }
        return pointPathresult;
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

//    public List<List<List<Double>>> inPutPoint(String point1,String point2)
//    {
//        final double startTime = System.nanoTime();
//        statu = 0;
//        String[] sett = point1.replace("[","").replace("]","").split(",");
//        String[] set = point2.replace("[","").replace("]","").split(",");
//        if(sett.length != 3||set.length !=3)
//        {
//            statu = 2;
//            List<List<List<Double>>> patht = new ArrayList<>();
//            return patht;
//        }
//        vertexpoi v1 = new vertexpoi();
//        vertexpoi v2 = new vertexpoi();
//        v1.setX(Double.parseDouble(sett[0]));
//        v1.setY(Double.parseDouble(sett[1]));
//        v1.setZ(Double.parseDouble(sett[2]));
//        v2.setX(Double.parseDouble(set[0]));
//        v2.setY(Double.parseDouble(set[1]));
//        v2.setZ(Double.parseDouble(set[2]));
//        this.putInString(this.getRoad("road"));
//        int p1= this.getMinDistance(v1);
//        int p2= this.getMinDistance(v2);
//        if(p1 ==p2)
//        {
//            statu = 1;
//        }
//        final double getOutlineEndTime = System.nanoTime();
//        LOGGER.info(String.format("拓扑最短路径提取完成！共耗时 %.2f 秒。",
//                (getOutlineEndTime - startTime) / 1.E9));
//        return this.getPointPath(this.getShortestPath(p1,p2));
//    }

    public int getStatu() {
        return statu;
    }

    public void setStatu(int statu) {
        this.statu = statu;
    }

//    public String getRoad(String key) {
//        Optional<BosRoad> f = roadRepo.findByKey(key);
//        if (f.isPresent()) {
//            BosRoad file = f.get();
//            return file.getRoad();
//        }
//        else {
//            return null;
//        }
//    }
}
