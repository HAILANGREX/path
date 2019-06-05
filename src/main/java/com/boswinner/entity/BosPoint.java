package com.boswinner.entity;




public class BosPoint {


	private String id;
	

	private String key;
	
	// x, y, z为点的三维坐标
	private double x; 
	private double y;
	private double z;
	private String route;  // 对应routes的标识(id)
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
	public double getX() {
		return x;
	}
	public void setX(double x) {
		this.x = x;
	}
	public double getY() {
		return y;
	}
	public void setY(double y) {
		this.y = y;
	}
	public double getZ() {
		return z;
	}
	public void setZ(double z) {
		this.z = z;
	}
	public String getRoute() {
		return route;
	}
	public void setRoute(String route) {
		this.route = route;
	}
	public BosPoint(String id, String key, double x, double y, double z, String route) {
		super();
		this.id = id;
		this.key = key;
		this.x = x;
		this.y = y;
		this.z = z;
		this.route = route;
	}
	public BosPoint() {
		super();
	}
	
}
