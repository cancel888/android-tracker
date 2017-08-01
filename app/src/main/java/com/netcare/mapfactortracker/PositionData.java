package com.netcare.mapfactortracker;

public class PositionData {
	// Data structure to be exchanged between runners and Activity with actual data
	// Coordinates - latitude(degrees),longitude(degrees),altitude(meters),speed(km/h),course(degrees)
	public boolean fix;
	public String latitude;
	public String longtitude;
	public String altitude;
	public String speed;
	public String bearing;
	// Route Status - distanctance to waypoint, time to waypoint, distance to destination, time to destination
	public boolean navigating;
	public String wpDistance;
	public String wpTime;
	public String dtDistance;
	public String dtTime;
}
