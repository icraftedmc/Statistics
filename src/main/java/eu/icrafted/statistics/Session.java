package eu.icrafted.statistics;

import java.util.HashMap;

public class Session {
    private long SessionID;

    public long getSessionID() {
        return SessionID;
    }

    public void setSessionID(long sessionID) {
        SessionID = sessionID;
    }

    private String UUID;

    public String getUUID() {
        return UUID;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
    }

    private String Name;

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }

    private long StartTime;

    public long getStartTime() {
        return StartTime;
    }

    public void setStartTime(long startTime) {
        StartTime = startTime;
    }

    private int deads;

    public int getDeads() {
        return deads;
    }

    public void setDeads(int deads) {
        this.deads = deads;
    }

    private double damagetaken;

    public double getDamagetaken() {
        return damagetaken;
    }

    public void setDamagetaken(double damagetaken) {
        this.damagetaken = damagetaken;
    }

    private double damagedealt;

    public double getDamagedealt() {
        return damagedealt;
    }

    public void setDamagedealt(double damagedealt) {
        this.damagedealt = damagedealt;
    }

    public HashMap<String, Double> AttackedEntities = new HashMap<>();
}
