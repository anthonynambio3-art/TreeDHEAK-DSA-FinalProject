package com.cms.classroom_management.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.cms.classroom_management.dto.MaintenanceRequest;
import com.cms.classroom_management.dto.OccupiedRoomDTO;
import com.cms.classroom_management.dto.RoomDTO;
import com.cms.classroom_management.dto.ScheduleRequest;
import com.cms.classroom_management.service.RoomService;
import com.cms.classroom_management.model.Node;

@RestController
@RequestMapping("/api")
public class RoomController {

    private final RoomService service;
    private static final String ADMIN_PASSWORD = "Tree_DHEAK4";

    public RoomController(RoomService service) {
        this.service = service;
    }

    @GetMapping("/count")
    public int countAvailable() {
        return service.availableRooms();
    }

    @GetMapping("/available")
    public List<String> availableRooms(){

        List<String> rooms = new ArrayList<>();
        service.getRoot().getAvailableRooms(rooms);

        return rooms;
    }

    @PostMapping("/schedule")
    public String scheduleRoom(
            @RequestBody ScheduleRequest request){

        Node room =
            service.findRoom(request.getRoom());

        if(room == null){
            return "Room not found";
        }

        room.occupy(request.getOccupant(), request.getFaculty(),
                request.getStart(),
                request.getEnd(),
                request.getReason());
        return "Success";
    }

    @GetMapping("/search/{name}")
    public RoomDTO searchRoom(@PathVariable String name){

        Node room = service.findRoom(name);

        if(room == null){
            return null;
        }

        String location = room.getParent().getData() + ", " +
                room.getParent().getParent()
                    .getData();

        return new RoomDTO(
                room.getData(),
                room.getStatus().toString(),
                room.getOccupator(),
                room.getSched(),
                location
        );
    }
    @GetMapping("/rooms")
    public List<RoomDTO> getAllRooms(){
        return service.getAllRooms();
    }

    @PostMapping("/maintenance")
    public String setMaintenance(
            @RequestBody MaintenanceRequest req){

        if(!ADMIN_PASSWORD.equals(
                req.getPassword())){

            return "Invalid admin password.";
        }

        Node room = service.findRoom(req.getRoom());

        if(room == null){
            return "Room not found.";
        }

        room.maintenance();

        return "Room marked as maintenance.";
    }

    @PostMapping("/maintenance/remove")
    public String removeMaintenance(
            @RequestBody MaintenanceRequest req){

        if(!ADMIN_PASSWORD.equals(
                req.getPassword())){

            return "Invalid admin password.";
        }

        Node room =
                service.findRoom(req.getRoom());

        if(room == null){
            return "Room not found.";
        }

        room.vacant();

        return "Maintenance Removed.";
    }

    @GetMapping("/occupied")
    public List<OccupiedRoomDTO> getOccupiedRooms(){

        return service.getOccupiedRooms();
    }

    @PostMapping("/leave/{roomName}")
    public String leaveRoom(
            @PathVariable String roomName){

        Node room =
                service.findRoom(roomName);

        if(room == null){
            return "Room not found.";
        }

        room.leaving();

        return "Room Vacated.";
    }
}