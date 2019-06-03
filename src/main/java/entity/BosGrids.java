package entity;


import java.util.HashMap;
import java.util.Map;


public class BosGrids {



    private String model; // 网格地图对应的模型

    private String path;  //文件存储路径

    private GridSettings gridSettings; // 存储网格地图生成参数


    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public GridSettings getGridSettings() {
        return gridSettings;
    }

    public void setGridSettings(GridSettings gridSettings) {
        this.gridSettings = gridSettings;
    }

    public BosGrids(){
        super();
    }

    public BosGrids( String model, String path, GridSettings settings) {

        this.model = model;
        this.path = path;
        this.gridSettings = settings;
    }
}
