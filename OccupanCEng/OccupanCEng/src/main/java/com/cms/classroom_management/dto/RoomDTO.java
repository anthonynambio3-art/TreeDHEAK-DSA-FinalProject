package com.cms.classroom_management.dto;

public class RoomDTO {

    private String name;
    private String status;
    private String occupant;
    private String schedule;
    private String location;
    private String faculty;
    private String reason;

    public RoomDTO(String name, String status, String occupant, String schedule, String location) {
        this.name = name;
        this.status = status;
        this.occupant = occupant;
        this.schedule = schedule;
        this.location = location;
    }

    public RoomDTO(String name, String status, String occupant, String schedule) {
        this.name = name;
        this.status = status;
        this.occupant = occupant;
        this.schedule = schedule;
    }

    public RoomDTO(String name, String status) {
        this.name = name;
        this.status = status;
    }

    public RoomDTO(
        String name,
        String status,
        String occupant,
        String faculty,
        String reason,
        String schedule){

    this.name = name;
    this.status = status;
    this.occupant = occupant;
    this.faculty = faculty;
    this.reason = reason;
    this.schedule = schedule;
}

    public String getName() {
        return name;
    }

    public String getStatus() {
        return status;
    }

    public String getOccupant() {
        return occupant;
    }

    public String getSchedule() {
        return schedule;
    }

    public String getLocation() {
        return location;
    }

    public String getFaculty() {
        return faculty;
    }

    public String getReason() {
        return reason;
    }

    public void setFaculty(String faculty) {
        this.faculty = faculty;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}