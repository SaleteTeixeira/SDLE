package Node;

import Common.Client;
import io.atomix.utils.net.Address;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Node {
    private Client client;
    private List<Post> myPosts; //added by causal order
    private Integer causalID;

    private List<Client> neighbors;

    private Map<String, Client> subs;
    private Map<String, List<Post>> subsPosts; //added by causal order
    //todo (duvida): concordam???
    private Map<String, Integer> causalIdSubs;
    private Map<String, List<Post>> waitingListSubsPost;

    private Node(){
        //todo (diogo): ir buscar a key e o IP
        this.client = new Client("", "key", 1111, Address.from("12345"));
        this.myPosts = new ArrayList<Post>();
        this.causalID = 1;
        this.neighbors = new ArrayList<Client>();
        this.subs = new HashMap<String, Client>();
        this.subsPosts = new HashMap<>();
        this.causalIdSubs = new HashMap<>();
        this.waitingListSubsPost = new HashMap<>();
    }

    public Client getClient() {
        return client;
    }

    public String toString(){
        StringBuilder ss = new StringBuilder();

        ss.append("----- Node Info -----").append("\n\n");

        ss.append(this.client.toString()).append("\n");

        ss.append("----- My Posts -----").append("\n");
        ss.append(this.myPosts.toString()).append("\n\n");
        ss.append("Next causal ID: ").append(this.causalID).append("\n");

        ss.append("----- Neighbors -----").append("\n");
        ss.append(this.neighbors.toString()).append("\n\n");

        ss.append("----- Subs -----").append("\n\n");
        for(String s : this.subs.keySet()){
            ss.append(this.subs.get(s).toString()).append("\n");
            ss.append(this.subsPosts.get(s).toString()).append("\n\n");
            ss.append("----- Waiting Posts -----").append("\n");
            ss.append("Next causal ID: ").append(this.causalIdSubs.get(s)).append("\n");
            ss.append(this.waitingListSubsPost.get(s)).append("\n");
        }

        return ss.toString();
    }

    private static void writeInTextFile(Node node, String fileName) throws IOException {
        PrintWriter fich = new PrintWriter(fileName);
        fich.println(node.toString());
        fich.flush();
        fich.close();
    }

    private static void storeState(Node node, String fileName) throws FileNotFoundException, IOException {
        FileOutputStream fos = new FileOutputStream(fileName);
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(node);
        oos.flush();
        oos.close();
    }

    private static Node loadState(String fileName) throws FileNotFoundException, IOException, ClassNotFoundException {
        FileInputStream fis = new FileInputStream(fileName);
        ObjectInputStream ois = new ObjectInputStream(fis);
        Node node = (Node) ois.readObject();
        ois.close();
        return node;
    }

    public static void main(String[] args){
        String fileName = args[0];
        Node node = new Node();
        //todo (duvida): que nomes vamos dar aos ficheiros? a key é mt grande e o username ñ é único

        try {
            node = loadState(fileName);
            //todo (diogo): atualizar ???KEY??? e o IP
            //todo (sofia): apagar posts antigos
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Node's state not found.");
        }

        //todo (diogo): menu

        try {
            writeInTextFile(node, fileName+"_TextVersion");
            storeState(node, fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
