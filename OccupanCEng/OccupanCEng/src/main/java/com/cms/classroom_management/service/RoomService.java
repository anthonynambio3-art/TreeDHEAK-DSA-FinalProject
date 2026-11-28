package com.cms.classroom_management.service;

import com.cms.classroom_management.dto.OccupiedRoomDTO;
import com.cms.classroom_management.dto.RoomDTO;
import com.cms.classroom_management.model.Node;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class RoomService {

    private static final String DATA_DIR = "data";
    private static final String SER_FILE_PATH = DATA_DIR + "/rooms.ser";
    private static final String EXCEL_FILE_PATH = DATA_DIR + "/ClassroomData.xlsx";

    private Node root;

    public RoomService() {
        loadData();
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

        Node rm101 = new Node("SMART 101", Node.Status.VACANT);
        Node rm201 = new Node("SMART 201", Node.Status.VACANT);
        Node rm202 = new Node("Rm. 202", Node.Status.VACANT);
        Node rm203 = new Node("Rm. 203", Node.Status.OCCUPIED, "BSCpE 2A", LocalDateTime.of(2026, 11, 30, 9, 0), LocalDateTime.of(2026, 11, 30, 12, 0));
        Node rm204 = new Node("SMART 204", Node.Status.OCCUPIED, "BSCpE 2B", LocalDateTime.of(2026, 11, 30, 9, 0), LocalDateTime.of(2026, 11, 30, 12, 0));
        Node dlab = new Node("DES LAB", Node.Status.VACANT);
        Node rm301 = new Node("ELECTRONICS LAB", Node.Status.VACANT);
        Node rm302 = new Node("Rm. 302", Node.Status.MAINTENANCE);
        Node rm303 = new Node("Rm. 303", Node.Status.MAINTENANCE);
        Node rm304 = new Node("Rm. 304", Node.Status.VACANT);
        Node rm305 = new Node("Rm. 305", Node.Status.VACANT);
        Node rm306 = new Node("Rm. 306", Node.Status.VACANT);

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
        old3.addChild(rm306);

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

        Node elb11 = new Node("ELB 11", Node.Status.VACANT);
        Node elb12 = new Node("ELB 12", Node.Status.VACANT);
        Node elb13 = new Node("ELB 13", Node.Status.VACANT);
        Node elb21 = new Node("ELB 21", Node.Status.VACANT);
        Node elb22 = new Node("ELB 22", Node.Status.VACANT);
        Node elb23 = new Node("ELB 23", Node.Status.MAINTENANCE);
        Node elb31 = new Node("ELB 31", Node.Status.VACANT);
        Node elb32 = new Node("ELB 32", Node.Status.VACANT);
        Node elb33 = new Node("ELB 33", Node.Status.VACANT);
        Node elb41 = new Node("ELB 41", Node.Status.VACANT);
        Node elb42 = new Node("ELB 42", Node.Status.VACANT);
        Node elb43 = new Node("ELB 43", Node.Status.MAINTENANCE);
        Node elb51 = new Node("ELB 51", Node.Status.VACANT);
        Node elb52 = new Node("ELB 52", Node.Status.VACANT);
        Node elb53 = new Node("ELB 53", Node.Status.VACANT);

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

    private void syncTimeAndPersistIfChanged() {
        if (root != null) {
            root.updateStatusByTime();
            saveData();
        }
    }

    public Node findRoom(String roomName) {
        syncTimeAndPersistIfChanged();
        return root.findRoom(roomName);
    }

    public int availableRooms() {
        syncTimeAndPersistIfChanged();
        return root.vacantCount();
    }

    public Node getRoot() {
        return root;
    }

    public List<RoomDTO> getAllRooms() {
        syncTimeAndPersistIfChanged();
        List<RoomDTO> rooms = new ArrayList<>();
        collectRooms(root, rooms);
        return rooms;
    }

    private void collectRooms(Node current, List<RoomDTO> rooms) {
        if (current.getStatus() != null) {
            rooms.add(new RoomDTO(
                    current.getData(),
                    current.getStatus().toString(),
                    current.getOccupator(),
                    current.getFaculty(),
                    current.getReason(),
                    current.getSched()
            ));
        }

        for (Node child : current.getChildren()) {
            collectRooms(child, rooms);
        }
    }

    public List<OccupiedRoomDTO> getOccupiedRooms() {
        syncTimeAndPersistIfChanged();
        List<OccupiedRoomDTO> rooms = new ArrayList<>();
        collectOccupiedRooms(root, rooms);
        return rooms;
    }

    private void collectOccupiedRooms(Node current, List<OccupiedRoomDTO> rooms) {
        if (current.getStatus() == Node.Status.OCCUPIED) {
            rooms.add(new OccupiedRoomDTO(
                    current.getData(),
                    current.getOccupator(),
                    current.getSched()
            ));
        }

        for (Node child : current.getChildren()) {
            collectOccupiedRooms(child, rooms);
        }
    }

    public synchronized void saveData() {
        try {
            File dir = new File(DATA_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(SER_FILE_PATH))) {
                oos.writeObject(root);
            }

            exportToExcel();
            System.out.println("SAVE SUCCESS -> " + SER_FILE_PATH + " & " + EXCEL_FILE_PATH);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadData() {
        File serFile = new File(SER_FILE_PATH);
        if (serFile.exists() && serFile.length() > 0) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(serFile))) {
                root = (Node) ois.readObject();
                syncTimeAndPersistIfChanged();
                exportToExcel();
                System.out.println("LOAD SUCCESS <- " + SER_FILE_PATH);
                return;
            } catch (Exception e) {
                System.err.println("Failed to read rooms.ser, reinitializing default tree.");
                e.printStackTrace();
            }
        }

        System.out.println("NO rooms.ser FOUND -> Initializing default tree");
        initializeTree();
        saveData();
    }

    public void exportToExcel() {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Rooms");

            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Room");
            header.createCell(1).setCellValue("Status");
            header.createCell(2).setCellValue("Occupant");
            header.createCell(3).setCellValue("Faculty");
            header.createCell(4).setCellValue("Reason");
            header.createCell(5).setCellValue("Schedule");

            List<RoomDTO> rooms = new ArrayList<>();
            collectRooms(root, rooms);

            int rowNum = 1;
            for (RoomDTO room : rooms) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(room.getName());
                row.createCell(1).setCellValue(room.getStatus());
                row.createCell(2).setCellValue(room.getOccupant() == null ? "" : room.getOccupant());
                row.createCell(3).setCellValue(room.getFaculty() == null ? "" : room.getFaculty());
                row.createCell(4).setCellValue(room.getReason() == null ? "" : room.getReason());
                row.createCell(5).setCellValue(room.getSchedule() == null ? "" : room.getSchedule());
            }

            File dir = new File(DATA_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            try (FileOutputStream fos = new FileOutputStream(EXCEL_FILE_PATH)) {
                workbook.write(fos);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void manualSave() {
        saveData();
        System.out.println("MANUAL SAVE SUCCESS");
    }
}