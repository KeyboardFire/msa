package com.keyboardfire.msa;

import java.util.HashMap;

public class ClassData {
    public int sectionid;
    public String sectionidentifier;
    public String room;
    public String coursedescription;
    public String schoollevel;
    public String groupownername;
    public int OwnerId;
    public String groupowneremail;
    public String groupownerphoto;
    public String currentterm;
    public int assignmentactivetoday;
    public int assignmentduetoday;
    public int assignmentassignedtoday;
    public String mostrecentgroupphoto;
    public int AlbumId;
    public int DurationId;
    public int gradebookcumgpa; // maybe
    public boolean publishgrouptouser;
    public int markingperiodid;
    public int leadsectionid;
    public int cumgrade; // maybe
    public String FilePath;
    public boolean canviewassignments;

    public static String serialize(HashMap<Integer, String> data) {
        String s = "";
        for (int key : data.keySet()) {
            s += key + "\u0001" + data.get(key) + "\u0002";
        }
        return s.substring(0, s.length() - 1);
    }

    public static HashMap<Integer, String> deserialize(String data) {
        HashMap<Integer, String> map = new HashMap<>();
        if (data.isEmpty()) return map;
        for (String entry : data.split("\u0002")) {
            String[] parts = entry.split("\u0001");
            map.put(Integer.parseInt(parts[0]), parts[1]);
        }
        return map;
    }
}
