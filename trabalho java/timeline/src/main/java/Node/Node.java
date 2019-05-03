package Node;

import Common.Client;
import Common.NeighborsReply;
import Common.NodeMsg;
import Common.Util;
import io.atomix.cluster.messaging.ManagedMessagingService;
import io.atomix.cluster.messaging.impl.NettyMessagingService;
import io.atomix.utils.net.Address;
import io.atomix.utils.serializer.Serializer;
import io.atomix.utils.serializer.SerializerBuilder;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Node implements Serializable {
    private Client client;
    private List<Post> myPosts; //added by causal order
    private Integer causalID;

    private List<Client> neighbors;

    private Map<String, Client> subs;
    private Map<String, List<Post>> subsPosts; //added by causal order
    private Map<String, Integer> causalIdSubs;
    private Map<String, List<Post>> waitingListSubsPost;

    private Node(String username, String RSA, String host, int port) {
        //todo (diogo): ir buscar a key e o IP
        this.client = new Client(username, RSA, port, Address.from(host, port));
        this.myPosts = new ArrayList<Post>();
        this.causalID = 1;
        this.neighbors = new ArrayList<>();
        this.subs = new HashMap<>();
        this.subsPosts = new HashMap<>();
        this.causalIdSubs = new HashMap<>();
        this.waitingListSubsPost = new HashMap<>();
    }

    public Client getClient() {
        return this.client;
    }

    public List<Client> getNeighbors() {
        return this.neighbors;
    }

    public void setNeighbors(List<Client> neighbors) {
        this.neighbors = neighbors;
    }

    public void setIP(String ip) {
        this.client.setAddress(Address.from(ip, this.client.getPort()));
    }

    public String toString() {
        StringBuilder ss = new StringBuilder();

        ss.append("----- Node Info -----").append("\n\n");

        ss.append(this.client.toString()).append("\n");

        ss.append("----- My Posts -----").append("\n");
        ss.append(this.myPosts.toString()).append("\n\n");
        ss.append("Next causal ID: ").append(this.causalID).append("\n");

        ss.append("----- Neighbors -----").append("\n");
        ss.append(this.neighbors.toString()).append("\n\n");

        ss.append("----- Subs -----").append("\n\n");
        for (String s : this.subs.keySet()) {
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

    private static void storeState(Node node, String fileName) {
        try {
            FileOutputStream fos = new FileOutputStream(fileName);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(node);
            oos.flush();
            oos.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Node loadState(String username, String RSA, int port, String fileName) {
        String host = Util.getPublicIp();


        Node node = new Node(username, RSA, host, port);

        try {
            FileInputStream fis = new FileInputStream(fileName);
            ObjectInputStream ois = new ObjectInputStream(fis);
            node = (Node) ois.readObject();
            ois.close();
            fis.close();
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Could not find previous state");
        }

        return node;
    }

    public static void main(String[] args) {
        String fileName = "nodeDB";
        String username = args[0];
        int localport = Integer.parseInt(args[1]);
        Address bootstrapIP = Address.from(args[2]);
        String RSAFile = args[3];
        String RSA = Util.LoadRSAKey(RSAFile);
        Node node = loadState(username, RSA, localport, fileName);

        //todo (sofia): apagar posts antigos

        Serializer s = new SerializerBuilder().build();
        ManagedMessagingService ms = NettyMessagingService.builder().withAddress(Address.from(args[0])).build(); //todo (diogo): mudar IP
        ExecutorService es = Executors.newSingleThreadExecutor();

        try {
            ms.registerHandler("network", (o, m) -> {
                NeighborsReply nr = s.decode(m);
                node.setNeighbors(nr.getNeighbors());
                storeState(node, fileName);
            }, es);

            ms.start().get();

            NodeMsg msg = new NodeMsg(node.getClient());

            if (node.getNeighbors().size() == 0) ms.sendAsync(bootstrapIP, "network", s.encode(msg));
            else ms.sendAsync(bootstrapIP, "update", s.encode(msg));

        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        //todo (salete): menu
    }
}
