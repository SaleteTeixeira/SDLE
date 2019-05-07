package Node;

import Common.*;
import io.atomix.cluster.messaging.ManagedMessagingService;
import io.atomix.cluster.messaging.impl.NettyMessagingService;
import io.atomix.utils.net.Address;
import io.atomix.utils.serializer.Serializer;
import io.atomix.utils.serializer.SerializerBuilder;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

//todo (geral): qd aplicarmos a thread (store com CF dava, não?), talvez tenhamos de sincronizar/lockar isto tudo :(
public class Node implements Serializable {
    private Client client;
    private List<Post> myPosts; //added by causal order
    private Integer causalID;

    private List<Client> neighbors;

    private Map<String, Client> publishers;
    private Map<String, List<Post>> pubsPosts; //added by causal order
    private Map<String, Integer> causalIdPubs;
    private Map<String, List<Post>> waitingListPubsPost;

    public void setClient(Client client) {
        this.client = client;
    }

    public List<Post> getMyPosts() {
        return this.myPosts;
    }

    public void setMyPosts(List<Post> myPosts) {
        this.myPosts = myPosts;
    }

    public Integer getCausalID() {
        return this.causalID;
    }

    public void setCausalID(Integer causalID) {
        this.causalID = causalID;
    }

    public void setNeighbors(List<Client> neighbors) {
        this.neighbors = neighbors;
    }

    public Map<String, Client> getPublishers() {
        return this.publishers;
    }

    public void setPublishers(Map<String, Client> publishers) {
        this.publishers = publishers;
    }

    public Map<String, List<Post>> getPubsPosts() {
        return this.pubsPosts;
    }

    public void setPubsPosts(Map<String, List<Post>> pubsPosts) {
        this.pubsPosts = pubsPosts;
    }

    public Map<String, Integer> getCausalIdPubs() {
        return this.causalIdPubs;
    }

    public void setCausalIdPubs(Map<String, Integer> causalIdPubs) {
        this.causalIdPubs = causalIdPubs;
    }

    public Map<String, List<Post>> getWaitingListPubsPost() {
        return this.waitingListPubsPost;
    }

    public void setWaitingListPubsPost(Map<String, List<Post>> waitingListPubsPost) {
        this.waitingListPubsPost = waitingListPubsPost;
    }

    public Map<String, List<String>> getSuggestedPubsByPub() {
        return this.suggestedPubsByPub;
    }

    public void setSuggestedPubsByPub(Map<String, List<String>> suggestedPubsByPub) {
        this.suggestedPubsByPub = suggestedPubsByPub;
    }

    private Map<String, List<String>> suggestedPubsByPub;

    private Node(String username, String RSA, String host, int port) {
        this.client = new Client(username, RSA, Address.from(host, port));
        this.myPosts = new ArrayList<Post>();
        this.causalID = 1;
        this.neighbors = new ArrayList<>();
        this.publishers = new HashMap<>();
        this.pubsPosts = new HashMap<>();
        this.causalIdPubs = new HashMap<>();
        this.waitingListPubsPost = new HashMap<>();
        this.suggestedPubsByPub = new HashMap<>();
    }

    Client getClient() {
        return this.client;
    }

    List<Client> getNeighbors() {
        return this.neighbors;
    }

    List<Post> getCloneMyPosts() {
        List<Post> result = new ArrayList<>();

        for (Post p : this.myPosts) {
            result.add(p.clone());
        }

        return result;
    }

    void addNeighbors(List<Client> neighbors) {
        for (Client c : neighbors) {
            if (!this.neighbors.contains(c)) this.neighbors.add(c);
        }
    }

    public String toString() {
        StringBuilder ss = new StringBuilder();

        ss.append("----- Node -----").append("\n\n");

        ss.append(this.client.toString()).append("\n");

        ss.append("----- My Posts -----").append("\n");
        ss.append(this.myPosts.toString()).append("\n\n");
        ss.append("Next causal ID: ").append(this.causalID).append("\n");

        ss.append("----- Neighbors -----").append("\n");
        ss.append(this.neighbors.toString()).append("\n\n");

        ss.append("----- Pubs -----").append("\n\n");
        for (String s : this.publishers.keySet()) {
            ss.append(this.publishers.get(s).toString()).append("\n");
            ss.append("----- Posts -----").append("\n");
            ss.append(this.pubsPosts.get(s).toString()).append("\n\n");
            ss.append("----- Waiting Posts -----").append("\n");
            ss.append("Next causal ID: ").append(this.causalIdPubs.get(s)).append("\n");
            ss.append(this.waitingListPubsPost.get(s).toString()).append("\n\n");
            ss.append("----- Suggested Pubs -----").append("\n");
            ss.append(this.suggestedPubsByPub.get(s).toString()).append("\n\n");
        }

        return ss.toString();
    }

    void addPost(String p) {
        Post post = new Post(p, this.causalID);
        this.myPosts.add(post);
        this.causalID++;
    }

    void removeOneWeekOldPosts() {
        Map<String, List<Post>> newPubsPosts = new HashMap<>();

        for (Map.Entry<String, List<Post>> e : this.pubsPosts.entrySet()) {
            List<Post> newPosts = e.getValue().stream()
                    .filter(post -> !post.oneWeekOld())
                    .collect(Collectors.toList());

            newPubsPosts.put(e.getKey(), newPosts);
        }

        this.pubsPosts = newPubsPosts;
        newPubsPosts = null; //todo wat
    }

    List<Client> listNeighbors_NotFollowing() {
        List<Client> result = new ArrayList<>();

        for (Client c : this.neighbors) {
            if (!this.publishers.containsKey(c.getKey())) result.add(c);
        }

        return result;
    }

    List<String> listPublishersKeys() {
        return new ArrayList<>(this.publishers.keySet());
    }

    List<Client> listPublishersValues() {
        return new ArrayList<>(this.publishers.values());
    }

    void addPublisher(String tempUsername, String key) {
        this.publishers.put(key, new Client(tempUsername, key));
        this.pubsPosts.put(key, new ArrayList<>());
        this.causalIdPubs.put(key, 1);
        this.waitingListPubsPost.put(key, new ArrayList<>());
    }

    void addPublisher(Client client) {
        String key = client.getKey();

        this.publishers.put(key, client);
        this.pubsPosts.put(key, new ArrayList<>());
        this.causalIdPubs.put(key, 1);
        this.waitingListPubsPost.put(key, new ArrayList<>());
    }

    void updatePublisher(Client client) {
        this.publishers.put(client.getKey(), client);
    }

    void removePublisher(String key) {
        this.publishers.remove(key);
        this.pubsPosts.remove(key);
        this.causalIdPubs.remove(key);
        this.waitingListPubsPost.remove(key);
        this.suggestedPubsByPub.remove(key);
    }

    void updateSuggestedPubsByPub(String pubKey, List<String> suggestedPubs) {
        this.suggestedPubsByPub.put(pubKey, suggestedPubs);
    }

    void writeInTextFile(String fileName) {
        try {
            PrintWriter fich = new PrintWriter(fileName);
            fich.println(this.toString());
            fich.flush();
            fich.close();
        } catch (IOException e) {
            System.out.println("Error saving state in text file.");
        }
    }

    void storeState(String fileName) {
        try {
            FileOutputStream fos = new FileOutputStream(fileName);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(this);
            oos.flush();
            oos.close();
            fos.close();
        } catch (IOException e) {
            System.out.println("Error saving state.");
        }
    }

    private static Node loadState(String username, String RSA, String host, int port, String fileName) {
        Node node = new Node(username, RSA, host, port);

        try {
            FileInputStream fis = new FileInputStream(fileName);
            ObjectInputStream ois = new ObjectInputStream(fis);
            node = (Node) ois.readObject();
            ois.close();
            fis.close();
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Could not find previous state.");
        }

        return node;
    }

    public static void main(String[] args) {
        String username = args[0];
        int localport = Integer.parseInt(args[1]);
        Address bootstrapIP = Address.from(args[2]);
        String RSAFile = args[3];

        //Get public key and IP
        String host = Util.getPublicIp();
        String RSA = Util.LoadRSAKey(RSAFile);

        //Initialize new node or with previous state
        String fileName = "nodeDB";
        Node node = loadState(username, RSA, host, localport, fileName);
        node.removeOneWeekOldPosts();
        node.storeState(fileName);
        node.writeInTextFile(fileName + "_TextVersion");

        new Thread(new Refresh(node, host, localport, bootstrapIP, fileName)).start();
        new Thread(new Terminal(node, fileName)).start();
    }
}