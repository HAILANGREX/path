import entity.*;
//import service.GridPathService;
import service.GridService;
import service.HybridPathService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PathsService {

    /**
    * @Description: 获取混合路网的最短路径方法
    * @Param: []
    * @return: java.util.List<entity.vertexpoi>
    * @Author: Wang
    * @Date: 2019/6/3
    */

    public List<vertexpoi> getHybridShortest(String point1, String point2, List<String> relationlist,Map<String,List<Double>> points){

        List<vertexpoi> vertexpoiList = new ArrayList<>();
        HybridPathService hybridPathService = new HybridPathService();
        vertexpoiList=hybridPathService.getHybridShortest();
        return vertexpoiList;
    }
    /**
    * @Description: 获取栅格路网的最短路基方法
    * @Param: []
    * @return: java.util.List<entity.vertexpoi>
    * @Author: Wang
    * @Date: 2019/6/3
    */

    public  List<vertexpoi> getGridShortest(){
        List<vertexpoi> vertexpoiList = new ArrayList<>();
        HybridPathService hybridPathService = new HybridPathService();
        vertexpoiList=hybridPathService.gridShortest();
        return vertexpoiList;
    }

    /** 
    * @Description: 获取拓扑路网的最短路径方法 
    * @Param: [] 
    * @return: java.util.List<entity.vertexpoi> 
    * @Author: Wang 
    * @Date: 2019/6/3 
    */ 
    
    public  List<List<Double>> getTopologyShortest(String point1, String point2, List<String> relationlist,Map<String,List<Double>> points){
        List<List<Double>> vertexpoiList = new ArrayList<>();
        HybridPathService hybridPathService = new HybridPathService();
        vertexpoiList=hybridPathService.getShortest(point1, point2, relationlist,points);
        return vertexpoiList;
    }

}
