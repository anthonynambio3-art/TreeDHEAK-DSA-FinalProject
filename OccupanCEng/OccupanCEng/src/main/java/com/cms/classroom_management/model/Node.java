package com.cms.classroom_management.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.List;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Node implements Serializable{
    private static final long serialVersionUID = 1L;
    private String data;
    @JsonIgnore
    private ArrayList<Node> children;
    private Status status;
    @JsonIgnore
    private Node parent;
    private String occupator;
    private LocalDateTime inOccupy;
    private LocalDateTime outOccupy;
    private String faculty;
    private String reason;
    
    public enum Status{
        VACANT,
        OCCUPIED,
        MAINTENANCE
    }

    public Node(){

    }

    public Node(String data) {
        this.data = data;
        children = new ArrayList<>();
        this.status = null;
        this.parent = null;
    }

    public Node(String data, Status s){
        this.data = data;
        children = new ArrayList<>();
        this.status = s;
        this.parent = null;
    }

    public Node(String data, Status s, String occ, LocalDateTime iO, LocalDateTime oO){
        this.data = data;
        children = new ArrayList<>();
        this.parent = null;
        this.status = Status.OCCUPIED;
        this.occupator = occ;
        this.inOccupy = iO;
        this.outOccupy = oO;
    }

    public void addChild(Node child) {
        child.parent = this;
        children.add(child);
    }

    public Node getParent(){
        return parent;
    }

    public String getData() {
        return data;
    }

    public ArrayList<Node> getChildren() {
        return children;
    }

    public Status getStatus() {
        return status;
    }

    public String getOccupator() {
        return occupator;
    }

    public LocalDateTime getInOccupy() {
        return inOccupy;
    }

    public LocalDateTime getOutOccupy() {
        return outOccupy;
    }

    public String getFaculty() {
        return faculty;
    }

    public String getReason() {
        return reason;
    }

    public void occupy(){
        this.status = Status.OCCUPIED;
    }

    public void occupy(
            String occ,
            String faculty,
            LocalDateTime iO,
            LocalDateTime oO,
            String reason){

        this.status = Status.OCCUPIED;
        this.occupator = occ;
        this.faculty = faculty;
        this.inOccupy = iO;
        this.outOccupy = oO;
        this.reason = reason;
    }

    public void leaving(){
        this.status = Status.VACANT;
        this.occupator = null;
        this.inOccupy = null;
        this.outOccupy = null;
    }

    public void vacant(){
        this.status = Status.VACANT;
    }

    public Node findRoom(String roomName) {
        if (this.data.equalsIgnoreCase(roomName)) {
            return this;
        }

        for (Node child : children) {
            Node found = child.findRoom(roomName);
            if (found != null) {
                return found;
            }
        }

        return null;
    }

    public void getAvailableRooms(List<String> rooms){
        if(status == Status.VACANT){
            rooms.add(data);
        }

        for(Node child : children){
            child.getAvailableRooms(rooms);
        }
    }

    public boolean isAvailable() {
        return status == Status.VACANT;
    }

    public void resetSchedule() {
        if (status == Status.OCCUPIED) {
        leaving();
        }
    }

    public void showOccupiedRooms() {
        if (status == Status.OCCUPIED) {
            System.out.println(data + " -> " + getSched());
        }

        for (Node child : children) {
            child.showOccupiedRooms();
        }
    }

    public void showMaintenanceRooms() {
        if (status == Status.MAINTENANCE) {
            System.out.println(data);
        }
    
        for (Node child : children) {
            child.showMaintenanceRooms();
        }
    }

    public void showAvailableRooms() {
        if (status == Status.VACANT) {
            System.out.println(data);
        }

        for (Node child : children) {
            child.showAvailableRooms();
        }
    }

    public int vacantCount(){
        int count = 0;
        if(this.status == Status.VACANT){
            count++;
        }
        for(Node child : this.children){
            count += child.vacantCount();
        }
        return count;
    }

    public void maintenance(){
        this.status = Status.MAINTENANCE;
    }

    public void reset(){
        this.status = null;
    }

    public void updateStatusByTime() {
        if(status == Status.OCCUPIED && outOccupy != null && LocalDateTime.now().isAfter(outOccupy)) {
            leaving();
        }
        for(Node child : children) {
            child.updateStatusByTime();
        }
    }

    public void print(Node n, int level){
        if(n == null){
            return;
        }

        if(n.data.equals("Old Building") || n.data.equals("Engineering Laboratory Building")){
            System.out.println();
        }

        for (int i = 0; i < level; i++) {
            System.out.print("\t");
        }
        System.out.print(n.data);
        if(n.status != null){
            System.out.print(" : " + n.status);
            if(n.status == Node.Status.OCCUPIED){
                System.out.println(" > " + n.getSched());
            }
        }

        System.out.println();

        for (Node child : n.children) {
            print(child, level + 1);
        }
    }

    public String getSched(){
        if(status != Status.OCCUPIED || inOccupy == null) return "";
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM dd, h:mm a");
        return inOccupy.format(fmt) + " - " + outOccupy.format(fmt);
    }

    public void search (Node n){
        System.out.println(n.data + " Informations:");
        System.out.println("Location: " + n.getParent().data + ", " + n.getParent().getParent().data);
        if(n.status == Status.OCCUPIED){
            if(n.status == Node.Status.OCCUPIED){
                System.out.println(n.getSched());
            }
        }
        else System.out.println("Status: " + n.status);
    }
}
