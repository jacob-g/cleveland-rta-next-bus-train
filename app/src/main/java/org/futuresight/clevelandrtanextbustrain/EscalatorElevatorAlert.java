package org.futuresight.clevelandrtanextbustrain;

/**
 * Created by jacob on 6/25/16.
 */
public class EscalatorElevatorAlert {
    public final String name;
    public final boolean working;
    public EscalatorElevatorAlert(String name, boolean working) {
        this.name = name;
        this.working = working;
    }

    public String toString() {
        return "[Name: " + name + ", Working: " + working + "]";
    }
}
