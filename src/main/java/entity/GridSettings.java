package entity;

public class GridSettings {

    private String status;  //提取状态
    private String unit;  //模型单位
    private Double x;//起始点x坐标
    private Double y;//起始点y坐标
    private Double height; //楼层高度
    private Double gridHeight; //栅格高度
    private Double gridWidth; //栅格宽度
    private Integer row;  //行
    private Integer col;  //列

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public Double getX() {
        return x;
    }

    public void setX(Double x) {
        this.x = x;
    }

    public Double getY() {
        return y;
    }

    public void setY(Double y) {
        this.y = y;
    }

    public Double getHeight() {
        return height;
    }

    public void setHeight(Double height) {
        this.height = height;
    }

    public Double getGridHeight() {
        return gridHeight;
    }

    public void setGridHeight(Double gridHeight) {
        this.gridHeight = gridHeight;
    }

    public Double getGridWidth() {
        return gridWidth;
    }

    public void setGridWidth(Double gridWidth) {
        this.gridWidth = gridWidth;
    }

    public Integer getRow() {
        return row;
    }

    public void setRow(Integer row) {
        this.row = row;
    }

    public Integer getCol() {
        return col;
    }

    public void setCol(Integer col) {
        this.col = col;
    }

    public GridSettings(){
        super();
    }

    public GridSettings(String status, String unit, Double x, Double y, Double height, Double gridHeight, Double gridWidth, Integer row, Integer col) {
        this.status = status;
        this.unit = unit;
        this.x = x;
        this.y = y;
        this.height = height;
        this.gridHeight = gridHeight;
        this.gridWidth = gridWidth;
        this.row = row;
        this.col = col;
    }
}
