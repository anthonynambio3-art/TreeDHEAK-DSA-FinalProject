import java.util.ArrayList;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Node {
    private String data;
    private ArrayList<Node> children;
    private Status status;
    private Node parent;
    private String occupator;
    private LocalDateTime inOccupy;
    private LocalDateTime outOccupy;
    
    public enum Status{
        VACANT,
        OCCUPIED,
        RESERVED,
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

    public void occupy(){
        this.status = Status.OCCUPIED;
    }

    public void occupy(String occ, LocalDateTime iO, LocalDateTime oO){
        this.status = Status.OCCUPIED;
        this.occupator = occ;
        this.inOccupy = iO;
        this.outOccupy = oO;
    }

    public void leaving(){
        this.status = Status.OCCUPIED;
        this.occupator = null;
        this.inOccupy = null;
        this.outOccupy = null;
    }

    public void vacant(){
        this.status = Status.VACANT;
    }

    public void reserve(){
        this.status = Status.RESERVED;
    }

    public void maintenance(){
        this.status = Status.MAINTENANCE;
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
        return "Occupied by: " + occupator + " | " + inOccupy.format(fmt) + " - " + outOccupy.format(fmt);
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
