package Node;

import Common.*;
import Common.Messages.*;
import io.atomix.cluster.messaging.ManagedMessagingService;
import io.atomix.cluster.messaging.impl.NettyMessagingService;
import io.atomix.utils.net.Address;
import io.atomix.utils.serializer.Serializer;

import java.util.*;
import java.util.concurrent.*;

//todo (geral): implementar grupo para as msg estarem por ordem e não haver duvidas que realmente é o endereço mais recente??? Ou simplemente tirar os updatesNetwork nos postsRequest/Reply
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

        /**
         * Register Handlers
         **/

        //Sent by another node that entered the network
        ms.registerHandler("hello", (o, m) -> {
            Client client = s.decode(m);
            String key = client.getKey();

            this.node.updateNetwork(client.clone());

            if(this.node.getNeighborsResponse().containsKey(key)){
                this.node.updateNeighborResponse(key, true);
            }

            if(this.node.getPubsResponse().containsKey(key)){
                this.node.updatePubResponse(key, true);
            }

            this.node.storeState(this.fileName);
            this.node.writeInTextFile(this.fileName + "_TextVersion");
            System.out.println("Received hello poke from "+ client.getKey() +". New network:\n" + this.node.getNetwork().values().toString());
        }, es);

        //Sent by bootstrap in the beginning
        ms.registerHandler("network", (o, m) -> {
            Network networkMsg = s.decode(m);
            Map<String, Client> network = networkMsg.getNetwork();

            this.node.setNetwork(network);

            //Assume, in the beginning, that everyone is online
            Map<String, Boolean> neighborsResponse = this.node.getNeighborsResponse();
            for(String key : neighborsResponse.keySet()){
                this.node.updateNeighborResponse(key, true);
            }

            //Assume, in the beginning, that everyone is online
            Map<String, Boolean> pubsResponse = this.node.getPubsResponse();
            for(String key : pubsResponse.keySet()){
                this.node.updatePubResponse(key, true);
            }

            this.node.storeState(this.fileName);
            this.node.writeInTextFile(this.fileName + "_TextVersion");
            System.out.println("Updated network!\n" + this.node.getNetwork().values().toString());

            System.out.println("Letting others know who I am.");
            for(Client c : this.node.getNetwork().values()){
                if(!c.getKey().equals(this.node.getClient().getKey()))
                    ms.sendAsync(c.getAddress(), "hello", s.encode(this.node.getClient().clone()));
            }
        }, es);

        //Sent by bootstrap
        ms.registerHandler("neighbors", (o, m) -> {
            NeighborsReply nr = s.decode(m);

            this.node.addNeighbors(nr.getNeighbors()); //network is updated as well
            this.node.setNeighbors(this.node.getNeighbors()); //refresh neighborsResponse to true

            System.out.println("I received new neighbors! My neighbors: \n" + this.node.getNeighbors().toString());
            this.node.storeState(this.fileName);
            this.node.writeInTextFile(this.fileName + "_TextVersion");
        }, es);

        //Sent by someone subscribed to me
        ms.registerHandler("suggestionsRequest", (o, m) -> {
            SuggestionsReply reply = new SuggestionsReply(this.node.getClient().getKey(), this.node.getPublishers());
            ms.sendAsync(o, "suggestionsReply", s.encode(reply));
        }, es);

        //Sent by someone I subscribe
        ms.registerHandler("suggestionsReply", (o, m) -> {
            SuggestionsReply reply = s.decode(m);
            this.node.updateSuggestedPubsByPub(reply.getPublisherKey(), reply.getSuggestedKeys());
            this.node.storeState(this.fileName);
            this.node.writeInTextFile(this.fileName + "_TextVersion");
        }, es);

        //Sent by someone subscribed to me or another node in the network
        ms.registerHandler("postsRequest", (o, m) -> {
            System.out.println("Received request for posts.");
            PostsRequest request = s.decode(m);
            int postCausalID = request.getPostCausalID();
            Client from = request.getFrom();
            String to = request.getTo();
            int ttl = request.getTTL();

            //Update possible neighbor/publisher who requested
            this.node.updateNeighborResponse(from.getKey(), true);
            this.node.updatePubResponse(from.getKey(), true);

            //Update from client info
            this.node.updateNetwork(from.clone()); //todo (geral): pode sobrepor o HELLO/network qd não há ordem nas mensagens

            this.node.storeState(this.fileName);
            this.node.writeInTextFile(this.fileName + "_TextVersion");

            //Message
            if (to.equals(this.node.getClient().getKey())) { //to me
                System.out.println("Direct request for me! Replying to "+from.getAddress().toString());

                List<Post> subList = this.postsAfterCausalID(this.node.getMyPosts(), postCausalID);
                Set<Post> orderedList = new TreeSet<>(new OrderedPostsByID());
                orderedList.addAll(subList);

                System.out.println(orderedList.toString());

                PostsReply reply = new PostsReply(orderedList, this.node.getClient(), this.node.getClient(), from.getKey());
                ms.sendAsync(from.getAddress(), "postsReply", s.encode(reply));
                System.out.println("Sent message with my posts.");
            }
            else { //to some other node
                request.setTTL(ttl - 1);

                if (this.node.getPublishers().contains(to)) {
                    System.out.println("Request for content from a user I subscribe to, sending cached content.");

                    List<Post> waitingList = this.postsAfterCausalID(this.node.getWaitingListPubsPost().get(to), postCausalID);
                    List<Post> subList = this.postsAfterCausalID(this.node.getPublisherPosts(to), postCausalID);
                    subList.addAll(waitingList);

                    Set<Post> orderedList = new TreeSet<>(new OrderedPostsByID());
                    orderedList.addAll(subList);

                    System.out.println(orderedList.toString());

                    if (orderedList.size() > 0) {
                        PostsReply reply = new PostsReply(orderedList, this.node.getNetwork().get(to), this.node.getClient(), from.getKey());
                        ms.sendAsync(from.getAddress(), "postsReply", s.encode(reply));
                    }

                    ms.sendAsync(this.node.getNetwork().get(to).getAddress(), "postsRequest", s.encode(request));
                }
                else if (request.getTTL() > 0) {
                    for (Client neighbor : this.node.listNeighborsClients()) {
                        System.out.println("Request for content for an user I don't subscribe, redirecting to neighbors.");
                        ms.sendAsync(neighbor.getAddress(), "postsRequest", s.encode(request));
                    }
                }
            }
        }, es);

        //Sent by someone I subscribe to
        ms.registerHandler("postsReply", (o, m) -> {
            System.out.println("Received posts after request.");
            PostsReply reply = s.decode(m);
            Set<Post> posts = reply.getPosts();
            Client from = reply.getFrom();
            Client sender = reply.getSender();
            String to = reply.getTo();

            //Message to me
            if (to.equals(this.node.getClient().getKey())) {

                //Update possible neighbor/publisher who replied
                //todo (geral): meti todos a true porque não é obrigatorio ser o vizinho a responder, basta apenas se receber uma resposta de alguém para ser true (pode ser um vizinho de um vizinho)
                //basicamente isto estava a ir ao bootstrap mm tendo um vizinho lhe respondido por um terceiro
                //this.node.updateNeighborResponse(sender.getKey(), true); (antigo, apagar se concordarem)
                Map<String, Boolean> neighborsResponse = this.node.getNeighborsResponse();
                for(String key : neighborsResponse.keySet()){
                    this.node.updateNeighborResponse(key, true);
                }

                this.node.updatePubResponse(sender.getKey(), true);

                //Update sender client info
                this.node.updateNetwork(sender.clone()); //todo (geral): pode sobrepor o HELLO/network qd não há ordem nas mensagens

                for (Post p : posts) {
                    //Update from (who is my publisher) client info, if it is the most recent post/address
                    //if (this.node.biggestPost(from.getKey(), p)) { //todo (geral): ainda faz sentido ver se é o que tem o maior post? acho que não
                        this.node.updateNetwork(from.clone()); //todo (geral) pode sobrepor o HELLO/network qd não há ordem nas mensagens
                    //}

                    this.node.addPubPost(p, from.getKey());
                }

                this.node.storeState(this.fileName);
                this.node.writeInTextFile(this.fileName + "_TextVersion");
            }
        }, es);

        /**
         * Refresh process
         **/

        //Initial communication with bootstrap
        System.out.println("THREAD: Contacting bootstrap.");
        NodeMsg msg = new NodeMsg(this.node.getClient(), this.node.getNodeMsgID());

        int id = this.node.getNodeMsgID() + 1;
        this.node.setNodeMsgID(id);
        this.node.storeState(this.fileName);
        this.node.writeInTextFile(this.fileName + "_TextVersion");

        if (this.node.getNeighbors().size() == 0) {
            ms.sendAsync(this.bootstrapIP, "neighbors", s.encode(msg));
        }
        else {
            ms.sendAsync(this.bootstrapIP, "update", s.encode(msg));
        }

        //Refresh posts
        es.scheduleWithFixedDelay(() -> {
            System.out.println("Scheduled task |  Posts");
            int ttl = 5;

            if (this.node.getNeighbors().size() == 0) {
                System.out.println("No neighbors, requesting to bootstrap.");
                requestNeighbors(s, ms);
            }
            else {
                for (Client p : this.node.listPublishersClients()) {
                    if (this.node.getPubsResponse().get(p.getKey())) {
                        System.out.println("Sending request for posts to my publisher.");
                        PostsRequest request = new PostsRequest(this.node.getCausalIdPubs().get(p.getKey()), this.node.getClient(), p.getKey(), ttl);
                        ms.sendAsync(p.getAddress(), "postsRequest", s.encode(request));
                        this.node.updatePubResponse(p.getKey(), false);
                    }
                    else if (contactNeighbors(this.node.getNeighborsResponse())) {
                        System.out.println("Client did not reply last request for posts.");

                        System.out.println("Sending request for posts to my publisher.");
                        PostsRequest request = new PostsRequest(this.node.getCausalIdPubs().get(p.getKey()), this.node.getClient(), p.getKey(), ttl);
                        ms.sendAsync(p.getAddress(), "postsRequest", s.encode(request));

                        System.out.println("Sending request for posts to my neighbors.");
                        for (Client c : this.node.listNeighborsClients()) {
                            ms.sendAsync(c.getAddress(), "postsRequest", s.encode(request));
                            this.node.updateNeighborResponse(c.getKey(), false);
                        }
                    }
                    else {
                        System.out.println("Requesting neighbors.");
                        requestNeighbors(s, ms);
                    }
                }
            }
        }, 5, 5, TimeUnit.SECONDS);

        //Refresh suggestions
        es.scheduleWithFixedDelay(() -> {
            System.out.println("Scheduled task |  Suggestions");
            List<Client> publishers = this.node.listPublishersClients();
            Collections.shuffle(publishers);

            int total;
            if (publishers.size() == 1) total = 1;
            else total = publishers.size() / 2;
            publishers = publishers.subList(0, total);

            for (Client p : publishers) {
                if (p.getAddress() != null) {
                    ms.sendAsync(p.getAddress(), "suggestionsRequest", s.encode(""));
                }
            }
        }, 20, 20, TimeUnit.SECONDS);
    }

    private void requestNeighbors(Serializer s, ManagedMessagingService ms) {
        NodeMsg msg = new NodeMsg(this.node.getClient(), this.node.getNodeMsgID());

        int id = this.node.getNodeMsgID() + 1;
        this.node.setNodeMsgID(id);
        this.node.storeState(this.fileName);
        this.node.writeInTextFile(this.fileName + "_TextVersion");

        ms.sendAsync(this.bootstrapIP, "neighbors", s.encode(msg));
    }

    private boolean contactNeighbors(Map<String, Boolean> neighborResponse) {
        for (Boolean b : neighborResponse.values()) {
            if (b) return true;
        }
        return false;
    }

    private List<Post> postsAfterCausalID(List<Post> list, int causalID) {
        List<Post> subList = new ArrayList<Post>();
        for (Post p : list) {
            if (p.getCausalID() >= causalID) {
                subList.add(p);
            }
        }
        return subList;
    }
}
