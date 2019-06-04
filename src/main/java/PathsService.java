import entity.*;
//import service.GridPathService;
import service.GridService;
import service.HybridPathService;

import java.io.IOException;
import java.io.InputStream;
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

    public List<vertexpoi> getHybridShortest(String point1, String point2, List<String> relationlist,Map<String,List<Object>> points,Map<String,InputStream> geoPaths, List<Map<String,Object>> grids, String unit) throws IOException {

        List<vertexpoi> vertexpoiList = new ArrayList<>();
        HybridPathService hybridPathService = new HybridPathService();
        vertexpoiList=hybridPathService.getHybridShortest(point1,point2,relationlist,points,geoPaths,grids,unit);
        return vertexpoiList;
    }
    /**
    * @Description: 获取栅格路网的最短路基方法
    * @Param: []
    * @return: java.util.List<entity.vertexpoi>
    * @Author: Wang
    * @Date: 2019/6/3
    */

    public  List<vertexpoi> getGridShortest(String point1, String point2, List<Map<String,Object>> stairGrids,
                                            Map<String,InputStream> geoPaths, List<Map<String,Object>> grids, String unit) throws IOException {
        List<vertexpoi> vertexpoiList = new ArrayList<>();
        HybridPathService hybridPathService = new HybridPathService();
        vertexpoiList=hybridPathService.gridShortest(point1, point2, stairGrids, geoPaths, grids, unit);
        return vertexpoiList;
    }

    /** 
    * @Description: 获取拓扑路网的最短路径方法 
    * @Param: [] 
    * @return: java.util.List<entity.vertexpoi> 
    * @Author: Wang 
    * @Date: 2019/6/3 
    */ 
    
    public  List<List<Object>> getTopologyShortest(String point1, String point2, List<String> relationlist,Map<String,List<Object>> points){
        List<List<Object>> vertexpoiList = new ArrayList<>();
        HybridPathService hybridPathService = new HybridPathService();
        vertexpoiList=hybridPathService.getShortest(point1, point2, relationlist,points);
        return vertexpoiList;
    }

}
