package entity;

import com.arangodb.springframework.annotation.Document;
import com.arangodb.springframework.annotation.Key;
import org.springframework.data.annotation.Id;

import java.util.List;

@Document("stairGrids")
public class BosStairGrid {

    @Id
    private String id;

    @Key
    private String key;

    private String model;  //网格地图对应的模型

    private Integer startheight; //楼梯开始高度

    private Integer endheight;  //楼梯终点高度

    private Entrance startgrid; //楼梯开始高度对应的网格

    private Entrance endgrid;  //楼梯终点高度对应的网格

    private List<vertexpoi> xyzlist;  //楼梯对应的xyzlist值

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Integer getStartheight() {
        return startheight;
    }

    public void setStartheight(Integer startheight) {
        this.startheight = startheight;
    }

    public Integer getEndheight() {
        return endheight;
    }

    public void setEndheight(Integer endheight) {
        this.endheight = endheight;
    }

    public Entrance getStartgrid() {
        return startgrid;
    }

    public void setStartgrid(Entrance startgrid) {
        this.startgrid = startgrid;
    }

    public Entrance getEndgrid() {
        return endgrid;
    }

    public void setEndgrid(Entrance endgrid) {
        this.endgrid = endgrid;
    }

    public List<vertexpoi> getXyzlist() {
        return xyzlist;
    }

    public void setXyzlist(List<vertexpoi> xyzlist) {
        this.xyzlist = xyzlist;
    }

    public BosStairGrid(){
        super();
    }

    public BosStairGrid(String id, String key, String model, Integer startheight, Integer endheight, Entrance startgrid, Entrance endgrid, List<vertexpoi> xyzlist) {
        this.id = id;
        this.key = key;
        this.model = model;
        this.startheight = startheight;
        this.endheight = endheight;
        this.startgrid = startgrid;
        this.endgrid = endgrid;
        this.xyzlist = xyzlist;
    }
}
