package com.cms.classroom_management.dto;

public class OccupiedRoomDTO {

    private String name;
    private String occupant;
    private String schedule;

    public OccupiedRoomDTO(
            String name,
            String occupant,
            String schedule) {

        this.name = name;
        this.occupant = occupant;
        this.schedule = schedule;
    }

    public String getName() {
        return name;
    }

    public String getOccupant() {
        return occupant;
    }

    public String getSchedule() {
        return schedule;
    }
}