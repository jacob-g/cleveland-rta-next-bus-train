package org.futuresight.clevelandrtanextbustrain;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by jacob on 5/15/16.
 */
public class Station {
    private String stationName, dirName, lineName, name; //name represents the name given by the user
    private int stationId, dirId, lineId;
    private LatLng loc;
    public char type = 'b';
    private boolean transfer = false;
    private int chainId;

    public Station(String stationName, int stationId, String dirName, int dirId, String lineName, int lineId, String name) {
        this.stationName = stationName;
        this.stationId = stationId;
        this.dirName = dirName;
        this.dirId = dirId;
        this.lineName = lineName;
        this.lineId = lineId;
        this.name = name;
    }

    public Station(String stationName, int stationId, String dirName, int dirId, String lineName, int lineId, String name, double lat, double lng) {
        this.stationName = stationName;
        this.stationId = stationId;
        this.dirName = dirName;
        this.dirId = dirId;
        this.lineName = lineName;
        this.lineId = lineId;
        this.name = name;
        loc = new LatLng(lat, lng);
    }

    public Station(String stationName, int stationId, String dirName, int dirId, String lineName, int lineId, String name, double lat, double lng, char type) {
        this.stationName = stationName;
        this.stationId = stationId;
        this.dirName = dirName;
        this.dirId = dirId;
        this.lineName = lineName;
        this.lineId = lineId;
        this.name = name;
        this.type = type;
        loc = new LatLng(lat, lng);
    }

    public Station(String stationName, int stationId, String dirName, int dirId, String lineName, int lineId, String name, double lat, double lng, char type, boolean transfer) {
        this.stationName = stationName;
        this.stationId = stationId;
        this.dirName = dirName;
        this.dirId = dirId;
        this.lineName = lineName;
        this.lineId = lineId;
        this.name = name;
        this.type = type;
        loc = new LatLng(lat, lng);
        this.transfer = transfer;
    }

    public String getStationName() {
        return this.stationName;
    }

    public int getStationId() {
        return this.stationId;
    }

    public String getDirName() {
        return this.dirName;
    }

    public int getDirId() {
        return this.dirId;
    }

    public String getLineName() {
        return this.lineName;
    }

    public int getLineId() {
        return this.lineId;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String n) {
        this.name = n;
    }

    public LatLng getLatLng() {
        return loc;
    }

    public void setType(char t) {
        type = t;
    }

    public char getType() {
        return type;
    }

    public boolean isTransfer() {
        return transfer;
    }

    public String toString() {
        return "[Name: " + name + ", Station: " + stationName + " (" + stationId + "), Direction: " + dirName + " (" + dirId + "), Line: " + lineName + " (" + lineId + "), LatLng: " + loc + "]";
    }

    public boolean equals(Object other) {
        if (other instanceof Station) {
            Station otherStation = (Station)other;
            return this.lineId == otherStation.lineId && this.dirId == otherStation.dirId && this.stationId == otherStation.stationId;
        } else {
            return false;
        }
    }

    public void chain(int id) {
        chainId = id;
    }

    public int getChainId() {
        return chainId;
    }

    public void setLatLng(LatLng l) {
        loc = l;
    }

    public int hashCode() {
        return this.lineId + this.stationId + this.dirId;
    }
}
