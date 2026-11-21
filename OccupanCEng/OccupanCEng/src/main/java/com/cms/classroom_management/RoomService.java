package com.cms.classroom_management.service;

import com.cms.classroom_management.model.Node;
import java.time.LocalDateTime;

import com.cms.classroom_management.dto.OccupiedRoomDTO;
import com.cms.classroom_management.dto.RoomDTO;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RoomService {

    private Node root;

    public RoomService() {
        initializeTree();
    }

    private void initializeTree() {
        root = new Node("College of Engineering");
        
        Node old = new Node("Old Building");
        Node elb = new Node("Engineering Laboratory Building");

        root.addChild(old);
        root.addChild(elb);

        Node old1 = new Node("1st Floor");
        Node old2 = new Node("2nd Floor");
        Node old3 = new Node("3rd Floor");

        old.addChild(old1);
        old.addChild(old2);
        old.addChild(old3);
        
        Node rm101 = new Node ("Rm. 101", Node.Status.VACANT);
        Node rm201 = new Node ("Rm. 201", Node.Status.VACANT);
        Node rm202 = new Node ("Rm. 202", Node.Status.VACANT);
        Node rm203 = new Node ("Rm. 203", Node.Status.OCCUPIED, "BSCpE 2A",LocalDateTime.of(2026, 11, 23, 9, 0), LocalDateTime.of(2026, 11, 23, 12, 0));
        Node rm204 = new Node ("Rm. 204", Node.Status.OCCUPIED, "BSCpE 2B", LocalDateTime.of(2026, 11, 23, 9, 0), LocalDateTime.of(2026, 11, 23, 12, 0));
        Node dlab = new Node ("DES LAB", Node.Status.VACANT);
        Node rm301 = new Node ("Rm. 301", Node.Status.VACANT);
        Node rm302 = new Node ("Rm. 302", Node.Status.MAINTENANCE);
        Node rm303 = new Node ("Rm. 303", Node.Status.VACANT);
        Node rm304 = new Node ("Rm. 304", Node.Status.VACANT);
        Node rm305 = new Node ("Rm. 305", Node.Status.VACANT);

        old1.addChild(rm101);
        old2.addChild(rm201);
        old2.addChild(rm202);
        old2.addChild(rm203);
        old2.addChild(rm204);
        old2.addChild(dlab);
        old3.addChild(rm301);
        old3.addChild(rm302);
        old3.addChild(rm303);
        old3.addChild(rm304);
        old3.addChild(rm305);

        Node elb1 = new Node("1st Floor");
        Node elb2 = new Node("2nd Floor");
        Node elb3 = new Node("3rd Floor");
        Node elb4 = new Node("4th Floor");
        Node elb5 = new Node("5th Floor");

        elb.addChild(elb1);
        elb.addChild(elb2);
        elb.addChild(elb3);
        elb.addChild(elb4);
        elb.addChild(elb5);

        Node elb11 = new Node ("ELB 11", Node.Status.VACANT);
        Node elb12 = new Node ("ELB 12", Node.Status.VACANT);
        Node elb13 = new Node ("ELB 13", Node.Status.MAINTENANCE);
        Node elb21 = new Node ("ELB 21", Node.Status.VACANT);
        Node elb22 = new Node ("ELB 22", Node.Status.VACANT);
        Node elb23 = new Node ("ELB 23", Node.Status.VACANT);
        Node elb31 = new Node ("ELB 31", Node.Status.VACANT);
        Node elb32 = new Node ("ELB 32", Node.Status.VACANT);
        Node elb33 = new Node ("ELB 32", Node.Status.VACANT);
        Node elb41 = new Node ("ELB 41", Node.Status.VACANT);
        Node elb42 = new Node ("ELB 42", Node.Status.VACANT);
        Node elb43 = new Node ("ELB 43", Node.Status.VACANT);
        Node elb51 = new Node ("ELB 51", Node.Status.VACANT);
        Node elb52 = new Node ("ELB 52", Node.Status.VACANT);
        Node elb53 = new Node ("ELB 53", Node.Status.VACANT);

        elb1.addChild(elb11);
        elb1.addChild(elb12);
        elb1.addChild(elb13);
        elb2.addChild(elb21);
        elb2.addChild(elb22);
        elb2.addChild(elb23);
        elb3.addChild(elb31);
        elb3.addChild(elb32);
        elb3.addChild(elb33);
        elb4.addChild(elb41);
        elb4.addChild(elb42);
        elb4.addChild(elb43);
        elb5.addChild(elb51);
        elb5.addChild(elb52);
        elb5.addChild(elb53);
    }

    public Node findRoom(String roomName){
        root.updateStatusByTime();
        return root.findRoom(roomName);
    }

    public int availableRooms(){
        return root.vacantCount();
    }

    public Node getRoot(){
        return root;
    }

   public List<RoomDTO> getAllRooms(){
        root.updateStatusByTime();

        List<RoomDTO> rooms = new ArrayList<>();

        collectRooms(root, rooms);

        return rooms;
    }

    private void collectRooms(Node current, List<RoomDTO> rooms){
        if(current.getStatus() != null){

            rooms.add(new RoomDTO(
                    current.getData(),
                    current.getStatus().toString(),
                    current.getOccupator(),
                    current.getSched()
                )
            );
        }

        for(Node child : current.getChildren()){
            collectRooms(child, rooms);
        }
    }

    public List<OccupiedRoomDTO> getOccupiedRooms(){

        root.updateStatusByTime();

        List<OccupiedRoomDTO> rooms =
                new ArrayList<>();

        collectOccupiedRooms(root, rooms);

        return rooms;
    }

    private void collectOccupiedRooms(
            Node current,
            List<OccupiedRoomDTO> rooms){

        if(current.getStatus() ==
                Node.Status.OCCUPIED){

            rooms.add(
                new OccupiedRoomDTO(
                    current.getData(),
                    current.getOccupator(),
                    current.getSched()
                )
            );
        }

        for(Node child :
                current.getChildren()){

            collectOccupiedRooms(
                    child,
                    rooms);
        }
    }
}