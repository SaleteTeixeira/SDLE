package Node;

import Common.*;
import io.atomix.cluster.messaging.ManagedMessagingService;
import io.atomix.cluster.messaging.impl.NettyMessagingService;
import io.atomix.utils.net.Address;
import io.atomix.utils.serializer.Serializer;

import java.util.*;
import java.util.concurrent.*;

public class Refresh implements Runnable {

    private final Node node;
    private final String host;
    private final int localport;
    private final Address bootstrapIP;
    private final String fileName;

    Refresh(Node node, String host, int localport, Address bootstrapIP, String fileName) {
        this.node = node;
        this.host = host;
        this.localport = localport;
        this.bootstrapIP = bootstrapIP;
        this.fileName = fileName;
    }

    public List<Post> postsAfterCausalID(List<Post> list, int causalID) {
        List<Post> subList = new ArrayList<Post>();
        for (Post p : list) {
            if (p.getCausalID() >= causalID) {
                subList.add(p);
            }
        }
        return subList;
    }

    public boolean mandarVizinhos(Map<String, Boolean> neighborResponse) {
        for (Boolean b : neighborResponse.values()) {
            if (b) return true;
        }
        return false;
    }

    @Override
    public void run() {
        Serializer s = Util.buildSerializer();
        ManagedMessagingService ms = NettyMessagingService.builder().withAddress(Address.from(this.host, this.localport)).build();
        ScheduledExecutorService es = Executors.newSingleThreadScheduledExecutor();

        try {
            ms.start().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        ms.registerHandler("hello", (o, m) -> {
            NodeMsg msg = s.decode(m);
            Client c = msg.getClient();
            List<Client> t = new ArrayList<>();
            t.add(c);
            this.node.addNeighbors(t);
            System.out.println("Received hello poke\n" + this.node.getNeighbors().toString());
        }, es);

        ms.registerHandler("network", (o, m) -> {
            NeighborsReply nr = s.decode(m);

            this.node.addNeighbors(nr.getNeighbors());
            this.node.setNeighbors(this.node.getNeighbors()); //refresh neighborsResponse to true

            NodeMsg msg = new NodeMsg(this.node.getClient(), 0);
            for (Client c : this.node.getNeighbors()) {
                this.node.updatePublisherClientInfo(c.clone());
                ms.sendAsync(c.getAddress(), "hello", s.encode(msg));
            }

            System.out.println("I received new neighbors!\n" + this.node.getNeighbors());
            this.node.storeState(this.fileName);
            this.node.writeInTextFile(this.fileName + "_TextVersion");
        }, es);

        ms.registerHandler("suggestionsRequest", (o, m) -> {
            SuggestionsReply reply = new SuggestionsReply(this.node.getClient().getKey(), this.node.listPublishersKeys());
            ms.sendAsync(o, "suggestionsReply", s.encode(reply));
        }, es);

        ms.registerHandler("suggestionsReply", (o, m) -> {
            SuggestionsReply reply = s.decode(m);
            this.node.updateSuggestedPubsByPub(reply.getPublisherKey(), reply.getSuggestedKeys());
            this.node.storeState(this.fileName);
            this.node.writeInTextFile(this.fileName + "_TextVersion");
        }, es);

        ms.registerHandler("postsRequest", (o, m) -> {
            System.out.println("Received request for posts.");
            PostsRequest request = s.decode(m);
            int causalID = request.getCausalID();
            Client from = request.getFrom();
            String to = request.getTo();
            int ttl = request.getTTL();

            //Update neighbor client info
            boolean found = false;
            List<Client> neighbors = this.node.getNeighbors();
            for (int i = 0; i < neighbors.size() && !found; i++) {
                if (neighbors.get(i).getKey().equals(from.getKey())) {
                    found = true;
                    this.node.updateNeighborClientInfo(from);
                    this.node.updateNeighborResponse(from.getKey(), true);
                }
            }

            //Update publisher client info
            if (this.node.listPublishersKeys().contains(from.getKey())) {
                this.node.updatePublisherClientInfo(from);
                this.node.updatePubResponse(from.getKey(), true);
            }

            this.node.storeState(this.fileName);
            this.node.writeInTextFile(this.fileName + "_TextVersion");

            //Message
            if (to.equals(this.node.getClient().getKey())) {
                System.out.println("Direct request for me, replying!");
                List<Post> subList = this.postsAfterCausalID(this.node.getMyPosts(), causalID);
                System.out.println(new HashSet<>(subList));
                PostsReply reply = new PostsReply(new HashSet<>(subList), this.node.getClient(), this.node.getClient(), from.getKey());
                System.out.println("Replying to: " + from.getAddress().toString());
                ms.sendAsync(from.getAddress(), "postsReply", s.encode(reply));
                System.out.println("Sent message with my posts.");

            } else {
                request.setTTL(ttl - 1);

                if (this.node.listPublishersKeys().contains(to)) {
                    System.out.println("Request for content from a user I subscribe to, sending cached content.");
                    List<Post> waitingList = this.postsAfterCausalID(this.node.getWaitingListPubsPost().get(to), causalID);
                    List<Post> subList = this.postsAfterCausalID(this.node.getPublisherPosts(to), causalID);
                    subList.addAll(waitingList);

                    Set<Post> orderedList = new TreeSet<>(new OrderedPostsByID());
                    orderedList.addAll(subList);

                    System.out.println("SENDING: " + orderedList.toString());

                    if (orderedList.size() > 0) {
                        PostsReply reply = new PostsReply(orderedList, this.node.getPublishers().get(to), this.node.getClient(), from.getKey());
                        ms.sendAsync(from.getAddress(), "postsReply", s.encode(reply));
                    }

                    ms.sendAsync(this.node.getPublishers().get(to).getAddress(), "postsRequest", s.encode(request));
                } else if (request.getTTL() > 0) {
                    for (Client neighbor : this.node.getNeighbors()) {
                        System.out.println("Request for content for unknown user, redirecting to neighbors.");
                        ms.sendAsync(neighbor.getAddress(), "postsRequest", s.encode(request));
                    }
                }
            }
        }, es);

        ms.registerHandler("postsReply", (o, m) -> {
            System.out.println("Received posts after request.");
            PostsReply reply = s.decode(m);
            Set<Post> posts = reply.getPosts();
            Client from = reply.getFrom();
            Client sender = reply.getSender();
            String to = reply.getTo();

            //Message to me
            if (to.equals(this.node.getClient().getKey())) {

                //Update neighbors who replied
                boolean found = false;
                List<Client> neighbors = this.node.getNeighbors();
                for (int i = 0; i < neighbors.size() && !found; i++) {
                    if (neighbors.get(i).getKey().equals(sender.getKey())) {
                        found = true;
                        this.node.updateNeighborResponse(sender.getKey(), true);
                    }
                }

                //Update publishers who replied
                this.node.updatePubResponse(sender.getKey(), true);

                //Update sender (who is my neighbor) client info
                found = false;
                for (int i = 0; i < neighbors.size() && !found; i++) {
                    if (neighbors.get(i).getKey().equals(sender.getKey())) {
                        found = true;
                        this.node.updateNeighborClientInfo(sender);
                    }
                }

                //Update sender (who is my publisher) client info
                if (this.node.listPublishersKeys().contains(sender.getKey())) {
                    this.node.updatePublisherClientInfo(sender);
                }

                for (Post p : posts) {
                    if (p.getCausalID().equals(this.node.getCausalIdPubs().get(from.getKey()))) {
                        this.node.addPubPost(p, from.getKey());
                    } else if (p.getCausalID() > this.node.getCausalIdPubs().get(from.getKey()))
                        this.node.addPubWaitingList(p, from.getKey());

                    //Update publisher/from (who is my publisher) client info
                    if (this.node.getPublishers().containsKey(from.getKey()) && this.node.biggestPost(from.getKey(), p)) {
                        this.node.updatePublisherClientInfo(from);

                        //Update publisher/from (who is my neighbor) client info
                        found = false;
                        for (int i = 0; i < neighbors.size() && !found; i++) {
                            if (neighbors.get(i).getKey().equals(from.getKey())) {
                                found = true;
                                this.node.updateNeighborClientInfo(from);
                            }
                        }
                    }

                }

                this.node.storeState(this.fileName);
                this.node.writeInTextFile(this.fileName + "_TextVersion");
            }
        }, es);

        //Initial communication with bootstrap
        System.out.println("THREAD: Contacting bootstrap.");
        NodeMsg msg = new NodeMsg(this.node.getClient(), this.node.getNodeMsgID());
        int id = this.node.getNodeMsgID() + 1;
        this.node.setNodeMsgID(id);
        this.node.storeState(this.fileName);
        this.node.writeInTextFile(this.fileName + "_TextVersion");
        if (this.node.getNeighbors().size() == 0) {
            ms.sendAsync(this.bootstrapIP, "network", s.encode(msg));
        } else {
            ms.sendAsync(this.bootstrapIP, "update", s.encode(msg));
        }

        //Refresh posts
        es.scheduleWithFixedDelay(() -> {
            System.out.println("Scheduled task |  Posts");
            int ttl = 5;

            if (this.node.getNeighbors().size() == 0) {
                System.out.println("No neighbors, requesting to bootstrap.");
                requestNeighbors(s, ms);
            } else {

                for (Client p : this.node.listPublishersValues()) {
                    if (p.getAddress() != null && this.node.getPubsResponse().get(p.getKey())) {
                        System.out.println("Sending request for posts.");
                        PostsRequest request = new PostsRequest(this.node.getCausalIdPubs().get(p.getKey()), this.node.getClient(), p.getKey(), ttl);
                        ms.sendAsync(p.getAddress(), "postsRequest", s.encode(request));
                        this.node.updatePubResponse(p.getKey(), false);

                    } else if (mandarVizinhos(this.node.getNeighborsResponse())) {
                        PostsRequest request = new PostsRequest(this.node.getCausalIdPubs().get(p.getKey()), this.node.getClient(), p.getKey(), ttl);

                        if (p.getAddress() != null) {
                            System.out.println("Sending request for posts.");
                            ms.sendAsync(p.getAddress(), "postsRequest", s.encode(request));
                        }

                        System.out.println("Client did not reply last request for posts.");
                        for (Client c : this.node.getNeighbors()) {
                            ms.sendAsync(c.getAddress(), "postsRequest", s.encode(request));
                            this.node.updateNeighborResponse(c.getKey(), false);
                        }

                    } else {
                        System.out.println("Requesting neighbors.");
                        requestNeighbors(s, ms);
                    }
                }
            }
        }, 5, 5, TimeUnit.SECONDS);


        //Refresh suggestions
        es.scheduleWithFixedDelay(() -> {
            System.out.println("Scheduled task |  Suggestions");
            List<Client> network = this.node.listPublishersValues();
            Collections.shuffle(network);

            int total;
            if (network.size() == 1) total = 1;
            else total = network.size() / 2;
            network = network.subList(0, total);

            for (Client p : network) {
                if (p.getAddress() != null) {
                    ms.sendAsync(p.getAddress(), "suggestionsRequest", s.encode(""));
                }
            }
        }, 10, 10, TimeUnit.SECONDS);
    }

    private void requestNeighbors(Serializer s, ManagedMessagingService ms) {
        NodeMsg msgB = new NodeMsg(this.node.getClient(), this.node.getNodeMsgID());
        this.node.setNodeMsgID(this.node.getNodeMsgID() + 1);
        this.node.storeState(this.fileName);
        this.node.writeInTextFile(this.fileName + "_TextVersion");
        ms.sendAsync(this.bootstrapIP, "network", s.encode(msgB));
    }
}
