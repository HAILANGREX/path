package entity;

import com.arangodb.springframework.annotation.Document;
import com.arangodb.springframework.annotation.Key;
import org.springframework.data.annotation.Id;

import java.util.HashMap;
import java.util.Map;

@Document("grids")
public class BosGrids {

    @Id
    private String id;

    @Key
    private String key;

    private String model; // 网格地图对应的模型

    private String path;  //文件存储路径

    private GridSettings gridSettings; // 存储网格地图生成参数

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

    public BosGrids(String id, String key, String model, String path, GridSettings settings) {
        this.id = id;
        this.key = key;
        this.model = model;
        this.path = path;
        this.gridSettings = settings;
    }
}
