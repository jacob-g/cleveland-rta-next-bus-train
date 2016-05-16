package org.futuresight.clevelandrtanextbustrain;

/**
 * Created by jacob on 5/15/16.
 */
public class Station {
    private String stationName, dirName, lineName, name; //name represents the name given by the user
    private int stationId, dirId, lineId;

    public Station(String stationName, int stationId, String dirName, int dirId, String lineName, int lineId, String name) {
        this.stationName = stationName;
        this.stationId = stationId;
        this.dirName = dirName;
        this.dirId = dirId;
        this.lineName = lineName;
        this.lineId = lineId;
        this.name = name;
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

    public String toString() {
        return "[Name: " + name + ", Station: " + stationName + " (" + stationId + "), Direction: " + dirName + " (" + dirId + "), Line: " + lineName + " (" + lineId + ")]";
    }
}
