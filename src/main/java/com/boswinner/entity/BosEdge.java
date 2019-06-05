package com.boswinner.entity;




public class BosEdge {


	
	private String from;      // point对应id
	private String to;        // point对应id 
	private String distance;  // 两个点的距离
    private String route;     // 对应routes的标识(id)
	public String getFrom() {
		return from;
	}
	public void setFrom(String from) {
		this.from = from;
	}
	public String getTo() {
		return to;
	}
	public void setTo(String to) {
		this.to = to;
	}
	public String getDistance() {
		return distance;
	}
	public void setDistance(String distance) {
		this.distance = distance;
	}
	public String getRoute() {
		return route;
	}
	public void setRoute(String route) {
		this.route = route;
	}
	public BosEdge( String from, String to, String distance, String route) {
		super();
		this.from = from;
		this.to = to;
		this.distance = distance;
		this.route = route;
	}
	public BosEdge() {
		super();
	}
	
    
}
