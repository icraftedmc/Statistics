package eu.icrafted.statistics;

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
}
