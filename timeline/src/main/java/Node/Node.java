package Node;

import Common.*;
import io.atomix.utils.net.Address;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class Node implements Serializable {
    /**
     * Class Attributes
     **/

    //Client basic information
    private Client client;

    //Client posts related
    private List<Post> myPosts; //added by causal order
    private Integer causalID;

    //Direct nodes with whom Client communicates
    private List<String> neighbors;
    private List<String> publishers;

    //Publishers posts related
    private Map<String, List<Post>> pubsPosts; //added by causal order
    private Map<String, Integer> causalIdPubs;
    private Map<String, List<Post>> waitingListPubsPost;

    //Publishers suggestions
    private Map<String, List<String>> suggestedPubsByPub;

    //Communication with direct nodes related
    private Map<String, Boolean> neighborsResponse;
    private Map<String, Boolean> pubsResponse;

    //Communication with Bootstrap related
    private int nodeMsgID;
    private Map<String, Client> network;

    /**
     * Constructors
     **/

    private Node(String username, String RSA, String host, int port) {
        this.client = new Client(username, RSA, Address.from(host, port));
        this.myPosts = new ArrayList<Post>();
        this.causalID = 1;
        this.neighbors = new ArrayList<>();
        this.publishers = new ArrayList<>();
        this.pubsPosts = new HashMap<>();
        this.causalIdPubs = new HashMap<>();
        this.waitingListPubsPost = new HashMap<>();
        this.suggestedPubsByPub = new HashMap<>();
        this.neighborsResponse = new HashMap<>();
        this.pubsResponse = new HashMap<>();
        this.nodeMsgID = 1;
        this.network = new HashMap<>();
    }

    /**
     * Gets and Sets
     **/

    synchronized Client getClient() {
        return this.client.clone();
    }

    synchronized void setClient(Client client) {
        this.client = client.clone();
    }

    synchronized List<Post> getMyPosts() {
        List<Post> result = new ArrayList<>();

        for (Post p : this.myPosts) {
            result.add(p.clone());
        }

        return result;
    }

    synchronized void setMyPosts(List<Post> myPosts) {
        List<Post> result = new ArrayList<>();

        for (Post p : myPosts) {
            result.add(p.clone());
        }

        this.myPosts = result;
    }

    synchronized Integer getCausalID() {
        return this.causalID;
    }

    synchronized void setCausalID(Integer causalID) {
        this.causalID = causalID;
    }

    synchronized List<String> getNeighbors() {
        return new ArrayList<>(this.neighbors);
    }

    synchronized void setNeighbors(List<String> neighbors) {
        this.neighbors = new ArrayList<>(neighbors);

        for (String s : neighbors) {
            this.neighborsResponse.put(s, true);
        }
    }

    synchronized List<String> getPublishers() {
        return new ArrayList<>(this.publishers);
    }

    synchronized void setPublishers(List<String> publishers) {
        this.publishers = new ArrayList<>(publishers);

        for (String s : publishers) {
            this.pubsResponse.put(s, true);
        }
    }

    synchronized Map<String, List<Post>> getPubsPosts() {
        Map<String, List<Post>> result = new HashMap<>();

        for (Map.Entry<String, List<Post>> entry : this.pubsPosts.entrySet()) {
            List<Post> nestedResult = new ArrayList<>();
            for (Post p : entry.getValue()) {
                nestedResult.add(p.clone());
            }
            result.put(entry.getKey(), nestedResult);
        }

        return result;
    }

    @SuppressWarnings("Duplicates")
    synchronized void setPubsPosts(Map<String, List<Post>> pubsPosts) {
        Map<String, List<Post>> result = new HashMap<>();

        for (Map.Entry<String, List<Post>> entry : pubsPosts.entrySet()) {
            List<Post> nestedResult = new ArrayList<>();
            for (Post p : entry.getValue()) {
                nestedResult.add(p.clone());
            }
            result.put(entry.getKey(), nestedResult);
        }

        this.pubsPosts = result;
    }

    synchronized Map<String, Integer> getCausalIdPubs() {
        return new HashMap<>(this.causalIdPubs);
    }

    synchronized void setCausalIdPubs(Map<String, Integer> causalIdPubs) {
        this.causalIdPubs = new HashMap<>(causalIdPubs);
    }

    synchronized Map<String, List<Post>> getWaitingListPubsPost() {
        Map<String, List<Post>> result = new HashMap<>();

        for (Map.Entry<String, List<Post>> entry : this.waitingListPubsPost.entrySet()) {
            List<Post> nestedResult = new ArrayList<>();
            for (Post p : entry.getValue()) {
                nestedResult.add(p.clone());
            }
            result.put(entry.getKey(), nestedResult);
        }

        return result;
    }

    @SuppressWarnings("Duplicates")
    synchronized void setWaitingListPubsPost(Map<String, List<Post>> waitingListPubsPost) {
        Map<String, List<Post>> result = new HashMap<>();

        for (Map.Entry<String, List<Post>> entry : waitingListPubsPost.entrySet()) {
            List<Post> nestedResult = new ArrayList<>();
            for (Post p : entry.getValue()) {
                nestedResult.add(p.clone());
            }
            result.put(entry.getKey(), nestedResult);
        }

        this.waitingListPubsPost = result;
    }

    synchronized Map<String, List<String>> getSuggestedPubsByPub() {
        Map<String, List<String>> result = new HashMap<>();

        for (Map.Entry<String, List<String>> entry : this.suggestedPubsByPub.entrySet()) {
            List<String> nestedResult = new ArrayList<>(entry.getValue());
            result.put(entry.getKey(), nestedResult);
        }

        return result;
    }

    synchronized void setSuggestedPubsByPub(Map<String, List<String>> suggestedPubsByPub) {
        Map<String, List<String>> result = new HashMap<>();

        for (Map.Entry<String, List<String>> entry : suggestedPubsByPub.entrySet()) {
            List<String> nestedResult = new ArrayList<>(entry.getValue());
            result.put(entry.getKey(), nestedResult);
        }

        this.suggestedPubsByPub = result;
    }

    synchronized Map<String, Boolean> getNeighborsResponse() {
        Map<String, Boolean> result = new HashMap<>();

        for (Map.Entry<String, Boolean> entry : this.neighborsResponse.entrySet()) {
            result.put(entry.getKey(), entry.getValue());
        }

        return result;
    }

    synchronized Map<String, Boolean> getPubsResponse() {
        Map<String, Boolean> result = new HashMap<>();

        for (Map.Entry<String, Boolean> entry : this.pubsResponse.entrySet()) {
            result.put(entry.getKey(), entry.getValue());
        }

        return result;
    }

    synchronized int getNodeMsgID() {
        return this.nodeMsgID;
    }

    synchronized void setNodeMsgID(int nodeMsgID) {
        this.nodeMsgID = nodeMsgID;
    }

    synchronized Map<String, Client> getNetwork() {
        Map<String, Client> result = new HashMap<>();

        for (Map.Entry<String, Client> entry : this.network.entrySet()) {
            result.put(entry.getKey(), entry.getValue().clone());
        }

        return result;
    }

    synchronized void setNetwork(Map<String, Client> network) {
        Map<String, Client> result = new HashMap<>();

        for (Map.Entry<String, Client> entry : network.entrySet()) {
            result.put(entry.getKey(), entry.getValue().clone());
        }

        this.network = result;
    }

    /**
     * toString
     **/

    synchronized public String toString() {
        StringBuilder ss = new StringBuilder();

        ss.append("----- Node -----").append("\n\n");

        ss.append(this.client.toString()).append("\n");

        ss.append("----- My Posts -----").append("\n");
        ss.append("Next causal ID: ").append(this.causalID).append("\n");
        ss.append(this.myPosts.toString()).append("\n\n");

        ss.append("----- My Neighbors -----").append("\n");
        for (String s : this.neighbors) {
            ss.append(this.network.get(s).toString());
        }
        ss.append("\n");

        ss.append("----- My Publishers -----").append("\n");
        int i = 0;
        for (String s : this.publishers) {
            i++;
            ss.append("Publisher nr ").append(i).append(".\n");
            ss.append(this.network.get(s).toString()).append("\n");
            ss.append("----- Cached Posts -----").append("\n");
            ss.append(this.pubsPosts.get(s).toString()).append("\n\n");
            ss.append("----- Waiting Posts -----").append("\n");
            ss.append("Next causal ID: ").append(this.causalIdPubs.get(s)).append("\n");
            ss.append(this.waitingListPubsPost.get(s).toString()).append("\n\n");
            ss.append("----- Suggested Publishers -----").append("\n");
            ss.append(this.suggestedPubsByPub.get(s).toString()).append("\n\n");
        }

        ss.append("----- Bootstrap Information -----").append("\n");
        ss.append("NodeMsg ID (to talk with bootstrap): ").append(this.nodeMsgID).append("\n\n");

        ss.append("----- Network -----").append("\n");
        for (Client c : this.network.values()) {
            ss.append(c.toString());
        }

        return ss.toString();
    }

    /**
     * Methods related to stored state
     **/

    synchronized void writeInTextFile(String fileName) {
        try {
            PrintWriter fich = new PrintWriter(fileName);
            fich.println(this.toString());
            fich.flush();
            fich.close();
        } catch (IOException e) {
            System.out.println("Error saving state in text file.");
        }
    }

    synchronized void storeState(String fileName) {
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

    synchronized private static Node loadState(String username, String RSA, String host, int port, String fileName) {
        Node node = new Node(username, RSA, host, port);

        try {
            FileInputStream fis = new FileInputStream(fileName);
            ObjectInputStream ois = new ObjectInputStream(fis);
            node = (Node) ois.readObject();
            node.setClientAddress(Address.from(host, port));
            ois.close();
            fis.close();
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Could not find previous state.");
        }

        return node;
    }

    /**
     * Methods
     **/

    synchronized void setClientAddress(Address address) {
        this.client.setAddress(address);
    }

    synchronized void addPost(String p) {
        Post post = new Post(this.client.getUsername(), p, this.causalID);
        this.myPosts.add(post);
        this.causalID++;
    }

    synchronized private void removeOneWeekOldPosts() {
        Map<String, List<Post>> newPubsPosts = new HashMap<>();

        for (Map.Entry<String, List<Post>> e : this.pubsPosts.entrySet()) {
            List<Post> newPosts = e.getValue().stream()
                    .filter(post -> !post.oneWeekOld())
                    .collect(Collectors.toList());

            newPubsPosts.put(e.getKey(), newPosts);
        }

        this.pubsPosts = newPubsPosts;
        newPubsPosts = null;
    }

    synchronized List<Client> listNeighborsClients() {
        List<Client> result = new ArrayList<>();
        for (String n : this.neighbors) {
            result.add(this.network.get(n).clone());
        }
        return result;
    }

    synchronized void addNeighbors(List<Client> neighbors) {
        for (Client c : neighbors) {
            String key = c.getKey();

            if (!this.neighbors.contains(key) && !key.equals(this.client.getKey())) {
                this.neighbors.add(key);
                this.neighborsResponse.put(key, true);
            }

            updateNetwork(c.clone());
        }
    }

    synchronized List<Client> listNeighbors_NotFollowing() {
        List<Client> result = new ArrayList<>();

        for (String key : this.neighbors) {
            if (!this.publishers.contains(key)){
                result.add(this.network.get(key).clone());
            }
        }

        return result;
    }

    synchronized List<Client> listPublishersClients() {
        List<Client> result = new ArrayList<>();
        for (String p : this.publishers) {
            result.add(this.network.get(p).clone());
        }
        return result;
    }

    //Verified if network has that key before calling this function
    synchronized void addPublisher(String key) {
        if(!this.publishers.contains(key)){
            this.publishers.add(key);
            this.pubsPosts.put(key, new ArrayList<>());
            this.causalIdPubs.put(key, 1);
            this.waitingListPubsPost.put(key, new ArrayList<>());
            this.suggestedPubsByPub.put(key, new ArrayList<>());
            this.pubsResponse.put(key, true);
        }
    }

    synchronized void removePublisher(String key) {
        if(this.publishers.contains(key)){
            boolean found = false;
            int i;

            for (i=0; i<this.publishers.size() && !found; i++) {
                if (key.equals(this.publishers.get(i))) {
                    found = true;
                }
            }

            this.publishers.remove(i-1);
            this.pubsPosts.remove(key);
            this.causalIdPubs.remove(key);
            this.waitingListPubsPost.remove(key);
            this.suggestedPubsByPub.remove(key);
            this.pubsResponse.remove(key);
        }
    }

    synchronized List<Post> getPublisherPosts(String key) {
        List<Post> aux = new ArrayList<Post>();

        for (Post p : this.pubsPosts.get(key)) {
            aux.add(p.clone());
        }

        return aux;
    }

    synchronized void addPubPost(Post p, String k) {
        if (p.getCausalID().equals(this.causalIdPubs.get(k))) {
            this.pubsPosts.get(k).add(p.clone());
            this.causalIdPubs.put(k, p.getCausalID() + 1);

            List<Post> removeWaitingPosts = new ArrayList<Post>();
            List<Post> postsList = this.waitingListPubsPost.get(k);

            for (int i = 0; i < postsList.size(); i++) {
                Post post = postsList.get(i);
                if (this.causalIdPubs.get(k).equals(post.getCausalID())) {
                    this.pubsPosts.get(k).add(post.clone());
                    this.causalIdPubs.put(k, post.getCausalID() + 1);
                    removeWaitingPosts.add(post);
                    i = -1;
                }
            }

            for (Post tmp : removeWaitingPosts) {
                this.waitingListPubsPost.get(k).remove(tmp);
            }
        }

        else if (p.getCausalID() > this.causalIdPubs.get(k)){
            addPubWaitingList(p.clone(), k);
        }
    }

    synchronized void addPubWaitingList(Post p, String k) {
        boolean add = true;

        for (Post tmp : this.waitingListPubsPost.get(k)) {
            if (tmp.getCausalID().equals(p.getCausalID())) {
                add = false;
                break;
            }
        }

        if (add) this.waitingListPubsPost.get(k).add(p.clone());
    }

    synchronized void updateNeighborResponse(String key, boolean bool) {
        if(this.neighborsResponse.containsKey(key)){
            this.neighborsResponse.put(key, bool);
        }
    }

    synchronized void updatePubResponse(String key, boolean bool) {
        if(this.pubsResponse.containsKey(key)){
            this.pubsResponse.put(key, bool);
        }
    }

    synchronized boolean biggestPost(String key, Post p) {
        for (Post tmp : this.waitingListPubsPost.get(key)) {
            if (tmp.getCausalID() > p.getCausalID()) return false;
        }

        return true;
    }

    //add and update
    synchronized void updateSuggestedPubsByPub(String pubKey, List<String> suggestedPubs) {
        this.suggestedPubsByPub.put(pubKey, new ArrayList<>(suggestedPubs));
    }

    //add and update
    synchronized void updateNetwork(Client c) {
        this.network.put(c.getKey(), c.clone());
    }

    public static void main(String[] args) {
        String username = args[0];
        int localport = Integer.parseInt(args[1]);
        Address bootstrapIP = Address.from(args[2]);
        //String RSAFile = args[3];

        //Get public key and IP
        //String host = Util.getPublicIp();
        //String RSA = Util.LoadRSAKey(RSAFile);
        String host = "localhost";
        String RSA = "key" + username;

        //Initialize new node or with previous state
        String fileName = "nodeDB_" + username;
        Node node = loadState(username, RSA, host, localport, fileName);
        node.removeOneWeekOldPosts();
        node.storeState(fileName);
        node.writeInTextFile(fileName + "_TextVersion");

        new Thread(new Refresh(node, host, localport, bootstrapIP, fileName)).start();
        new Thread(new Terminal(node, fileName)).start();
    }
}