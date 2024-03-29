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
    private int nodeMsgID;

    private Client client;
    private List<Post> myPosts; //added by causal order
    private Integer causalID;

    private List<Client> neighbors;

    private Map<String, Client> publishers;
    private Map<String, List<Post>> pubsPosts; //added by causal order
    private Map<String, Integer> causalIdPubs;
    private Map<String, List<Post>> waitingListPubsPost;

    private Map<String, Boolean> neighborsResponse;
    private Map<String, Boolean> pubsResponse;

    private Map<String, List<String>> suggestedPubsByPub;

    /**
     * Constructors
     **/

    private Node(String username, String RSA, String host, int port) {
        this.nodeMsgID = 1;
        this.client = new Client(username, RSA, Address.from(host, port));
        this.myPosts = new ArrayList<Post>();
        this.causalID = 1;
        this.neighbors = new ArrayList<>();
        this.publishers = new HashMap<>();
        this.pubsPosts = new HashMap<>();
        this.causalIdPubs = new HashMap<>();
        this.waitingListPubsPost = new HashMap<>();
        this.suggestedPubsByPub = new HashMap<>();
        this.neighborsResponse = new HashMap<>();
        this.neighborsResponse = new HashMap<>();
        this.pubsResponse = new HashMap<>();
    }

    /**
     * Gets and Sets
     **/

    synchronized int getNodeMsgID() {
        return this.nodeMsgID;
    }

    synchronized void setNodeMsgID(int nodeMsgID) {
        this.nodeMsgID = nodeMsgID;
    }

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

    synchronized List<Client> getNeighbors() {
        List<Client> result = new ArrayList<>();

        for (Client c : this.neighbors) {
            result.add(c.clone());
        }

        return result;
    }

    synchronized void setNeighbors(List<Client> neighbors) {
        List<Client> result = new ArrayList<>();

        for (Client c : neighbors) {
            result.add(c.clone());
            this.neighborsResponse.put(c.getKey(), true);
        }

        this.neighbors = result;
    }

    synchronized Map<String, Client> getPublishers() {
        Map<String, Client> result = new HashMap<>();

        for (Map.Entry<String, Client> entry : this.publishers.entrySet()) {
            result.put(entry.getKey(), entry.getValue().clone());
        }

        return result;
    }

    synchronized void setPublishers(Map<String, Client> publishers) {
        Map<String, Client> result = new HashMap<>();

        for (Map.Entry<String, Client> entry : publishers.entrySet()) {
            result.put(entry.getKey(), entry.getValue().clone());
            this.pubsResponse.put(entry.getKey(), true);
        }

        this.publishers = result;
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

    /**
     * toString
     **/

    synchronized public String toString() {
        StringBuilder ss = new StringBuilder();

        ss.append("----- Node -----").append("\n\n");

        ss.append("NodeMsg ID (to talk with bootstrap): ").append(this.nodeMsgID).append("\n\n");

        ss.append(this.client.toString()).append("\n");

        ss.append("----- My Posts -----").append("\n");
        ss.append("Next causal ID: ").append(this.causalID).append("\n");
        ss.append(this.myPosts.toString()).append("\n\n");

        ss.append("----- My Neighbors -----").append("\n");
        ss.append(this.neighbors.toString()).append("\n\n");

        ss.append("----- My Publishers -----").append("\n");
        int i = 0;
        for (String s : this.publishers.keySet()) {
            i++;
            ss.append("Publisher nr ").append(i).append(".\n");
            ss.append(this.publishers.get(s).toString()).append("\n");
            ss.append("----- Cached Posts -----").append("\n");
            ss.append(this.pubsPosts.get(s).toString()).append("\n\n");
            ss.append("----- Waiting Posts -----").append("\n");
            ss.append("Next causal ID: ").append(this.causalIdPubs.get(s)).append("\n");
            ss.append(this.waitingListPubsPost.get(s).toString()).append("\n\n");
            ss.append("----- Suggested Publishers -----").append("\n");
            ss.append(this.suggestedPubsByPub.get(s).toString()).append("\n\n");
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

    synchronized void addNeighbors(List<Client> neighbors) {
        boolean found;

        for (Client c : neighbors) {
            found = false;
            for (Client localC : this.neighbors) {
                if (localC.getKey().equals(c.getKey())) {
                    found = true;
                    this.updateNeighborClientInfo(c);
                }
            }
            if (!found && !c.getKey().equals(this.client.getKey())) {
                this.neighbors.add(c.clone());
                this.neighborsResponse.put(c.getKey(), true);
            }
        }
    }

    synchronized void updateNeighborClientInfo(Client client) {
        for (Client c : this.neighbors) {
            if (c.getKey().equals(client.getKey())) {
                c.setUsername(client.getUsername());
                c.setAddress(client.getAddress());
            }
        }
    }

    synchronized List<Client> listNeighbors_NotFollowing() {
        List<Client> result = new ArrayList<>();

        for (Client c : this.neighbors) {
            if (!this.publishers.containsKey(c.getKey())) result.add(c.clone());
        }

        return result;
    }

    synchronized List<String> listPublishersKeys() {
        return new ArrayList<>(this.publishers.keySet());
    }

    synchronized List<Client> listPublishersValues() {
        List<Client> result = new ArrayList<>();
        for (Client c : this.publishers.values()) {
            result.add(c.clone());
        }
        return result;
    }

    synchronized void addPublisher(String tempUsername, String key) {
        this.publishers.put(key, new Client(tempUsername, key));
        this.pubsPosts.put(key, new ArrayList<>());
        this.causalIdPubs.put(key, 1);
        this.waitingListPubsPost.put(key, new ArrayList<>());
        this.suggestedPubsByPub.put(key, new ArrayList<>());
        this.pubsResponse.put(key, true);
    }

    synchronized void addPublisher(Client client) {
        Client c = client.clone();
        String key = c.getKey();

        this.publishers.put(key, c);
        this.pubsPosts.put(key, new ArrayList<>());
        this.causalIdPubs.put(key, 1);
        this.waitingListPubsPost.put(key, new ArrayList<>());
        this.suggestedPubsByPub.put(key, new ArrayList<>());
        this.pubsResponse.put(key, true);
    }

    synchronized void updatePublisherClientInfo(Client client) {
        if(this.publishers.containsKey(client.getKey())){
            this.publishers.put(client.getKey(), client.clone());
        }
    }

    synchronized void removePublisher(String key) {
        this.publishers.remove(key);
        this.pubsPosts.remove(key);
        this.causalIdPubs.remove(key);
        this.waitingListPubsPost.remove(key);
        this.suggestedPubsByPub.remove(key);
        this.pubsResponse.remove(key);
    }

    synchronized List<Post> getPublisherPosts(String key) {
        List<Post> aux = new ArrayList<Post>();

        for (Post p : this.pubsPosts.get(key)) {
            aux.add(p.clone());
        }

        return aux;
    }

    synchronized void addPubPost(Post p, String k) {
        this.pubsPosts.get(k).add(p.clone());
        this.causalIdPubs.put(k, p.getCausalID() + 1);

        List<Post> remove = new ArrayList<Post>();

        for (Post tmp : this.waitingListPubsPost.get(k)) {
            if (this.causalIdPubs.get(k) == tmp.getCausalID()) {
                this.pubsPosts.get(k).add(tmp.clone());
                this.causalIdPubs.put(k, tmp.getCausalID() + 1);
                remove.add(tmp.clone());
            } else break;
        }

        for (Post tmp : remove) {
            this.waitingListPubsPost.remove(tmp);
        }
    }

    synchronized void addPubWaitingList(Post p, String k) {
        boolean add = true;

        for (Post tmp : this.waitingListPubsPost.get(k)) {
            if (tmp.getCausalID() == p.getCausalID()) {
                add = false;
                break;
            }
        }

        if (add) this.waitingListPubsPost.get(k).add(p.clone());
    }

    synchronized void updateNeighborResponse(String key, boolean bool) {
        this.neighborsResponse.put(key, bool);
    }

    synchronized void updatePubResponse(String key, boolean bool) {
        if (this.publishers.containsKey(key)) this.pubsResponse.put(key, bool);
    }

    synchronized boolean biggestPost(String key, Post p) {
        for (Post tmp : this.waitingListPubsPost.get(key)) {
            if (tmp.getCausalID() > p.getCausalID()) return false;
        }

        return true;
    }

    synchronized void updateSuggestedPubsByPub(String pubKey, List<String> suggestedPubs) {
        this.suggestedPubsByPub.put(pubKey, new ArrayList<>(suggestedPubs));
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