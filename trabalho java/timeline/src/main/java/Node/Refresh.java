package Node;

import Common.*;
import io.atomix.cluster.messaging.ManagedMessagingService;
import io.atomix.cluster.messaging.impl.NettyMessagingService;
import io.atomix.utils.net.Address;
import io.atomix.utils.serializer.Serializer;
import io.atomix.utils.serializer.SerializerBuilder;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

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

    public List<Post> postsAfterCausalID(List<Post> list, int causalID){
        List<Post> subList = new ArrayList<Post>();
        for(Post p : list){
            if(p.getCausalID() >= causalID){
                subList.add(p);
            }
        }
        return subList;
    }

    public boolean mandarVizinhos(Map<String, Boolean> neighborResponse){
        for(Boolean b: neighborResponse.values()){
            if(b) return true;
        }
        return false;
    }

    @Override
    public void run() {
        Serializer s = Util.buildSerializer();
        ManagedMessagingService ms = NettyMessagingService.builder().withAddress(Address.from(this.host, this.localport)).build();
        ScheduledExecutorService es = Executors.newSingleThreadScheduledExecutor();

        //todo e se estas variaveis mudarem no node ????
        Map<String, Boolean> pubsResponse = new HashMap<>();
        for(Client c: this.node.getPublishers().values()){
            pubsResponse.put(c.getKey(), true);
        }

        //todo e se estas variaveis mudarem no node ????
        Map<String, Boolean> neighborResponse = new HashMap<>();
        for(Client c: this.node.getNeighbors()){
            neighborResponse.put(c.getKey(), true);
        }

        try {
            ms.start().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        ms.registerHandler("network", (o, m) -> {
            NeighborsReply nr = s.decode(m);
            this.node.addNeighbors(nr.getNeighbors());

            System.out.println("I received new neighbors!\n"+this.node.getNeighbors());
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
            PostsRequest request = s.decode(m);
            int causalID = request.getCausalID();
            Client from = request.getFrom();
            String to = request.getTo();
            int ttl = request.getTTL();

            //todo pensar se se pode fazer estes updates do client
            //Update neighbor client info
            boolean found = false;
            List<Client> neighbors = this.node.getNeighbors();
            for(int i = 0; i<neighbors.size() && !found; i++){
                if(neighbors.get(i).getKey().equals(from.getKey())){
                    found=true;
                    this.node.updateNeighborClientInfo(from);
                }
            }

            //Update publisher client info
            if(this.node.listPublishersKeys().contains(from.getKey())){
                this.node.updatePublisherClientInfo(from);
            }

            this.node.storeState(this.fileName);
            this.node.writeInTextFile(this.fileName + "_TextVersion");

            //Message
            if(to.equals(this.node.getClient().getKey())){
                List<Post> subList = this.postsAfterCausalID(this.node.getMyPosts(), causalID);
                PostsReply reply = new PostsReply(subList.stream().collect(Collectors.toSet()), this.node.getClient(), this.node.getClient(), from.getKey());
                ms.sendAsync(from.getAddress(),"postReply", s.encode(reply));
            }
            else {
                request.setCausalID(ttl-1);

                if (this.node.listPublishersKeys().contains(to)){
                    List<Post> waitingList = this.postsAfterCausalID(this.node.getWaitingListPubsPost().get(to), causalID);
                    List<Post> subList = this.postsAfterCausalID(this.node.getPublisherPosts(to), causalID);
                    subList.addAll(waitingList);

                    Set<Post> orderedList = new TreeSet<>(new OrderedPostsByID());
                    orderedList.addAll(subList);

                    if(orderedList.size() > 0){
                        PostsReply reply = new PostsReply(orderedList, this.node.getPublishers().get(to), this.node.getClient(), from.getKey());
                        ms.sendAsync(from.getAddress(), "postReply", s.encode(reply));
                    }

                    ms.sendAsync(this.node.getPublishers().get(to).getAddress(), "postsRequest", s.encode(request));
                }
                else if (request.getTTL()>0){
                    for(Client neighbor : this.node.getNeighbors()){
                        ms.sendAsync(neighbor.getAddress(), "postsRequest", s.encode(request));
                    }
                }
            }
        }, es);

        ms.registerHandler("postsReply", (o, m) -> {
            PostsReply reply = s.decode(m);
            Set<Post> posts = reply.getPosts();
            Client from = reply.getFrom();
            Client sender = reply.getSender();
            String to = reply.getTo();

            //Message to me
            if(to.equals(this.node.getClient().getKey())){

                //Update neighbors who replied
                boolean found = false;
                List<Client> neighbors = this.node.getNeighbors();
                for(int i = 0; i<neighbors.size() && !found; i++){
                    if(neighbors.get(i).getKey().equals(sender.getKey())){
                        found = true;
                        neighborResponse.put(sender.getKey(), true);
                    }
                }

                //Update publishers who replied
                if(pubsResponse.containsKey(sender.getKey())){
                    pubsResponse.put(sender.getKey(), true);
                }

                //Update sender (who is my neighbor) client info
                found = false;
                for(int i = 0; i<neighbors.size() && !found; i++){
                    if(neighbors.get(i).getKey().equals(sender.getKey())){
                        found=true;
                        this.node.updateNeighborClientInfo(sender);
                    }
                }

                //Update sender (who is my publisher) client info
                if(this.node.listPublishersKeys().contains(sender.getKey())){
                    this.node.updatePublisherClientInfo(sender);
                }

                //todo ver qual o maior id na waiting list

                for(Post p: posts){
                    if(p.getCausalID() == this.node.getCausalIdPubs().get(from.getKey())){
                        this.node.addPubPost(p, from.getKey());
                    }
                    else this.node.addPubWaitingList(p, from.getKey());
                }

                //todo ver qual o maior id na waiting list
                //todo se mudar, fazer os updates

                //Update publisher/from (who is my neighbor) client info
                found = false;
                for(int i = 0; i<neighbors.size() && !found; i++){
                    if(neighbors.get(i).getKey().equals(from.getKey())){
                        found=true;
                        this.node.updateNeighborClientInfo(from);
                    }
                }

                //Update publisher/from (who is my publisher) client info
                this.node.updatePublisherClientInfo(from);

                this.node.storeState(this.fileName);
                this.node.writeInTextFile(this.fileName + "_TextVersion");
            }
        }, es);

        //Initial communication with bootstrap
        System.out.println("THREAD: Contacting bootstrap.");
        NodeMsg msg = new NodeMsg(this.node.getClient(), this.node.getNodeMsgID());
        int id = this.node.getNodeMsgID()+1;
        this.node.setNodeMsgID(id);
        this.node.storeState(this.fileName);
        this.node.writeInTextFile(this.fileName + "_TextVersion");
        if (this.node.getNeighbors().size() == 0) {
            ms.sendAsync(this.bootstrapIP, "network", s.encode(msg));
        } else {
            ms.sendAsync(this.bootstrapIP, "update", s.encode(msg));
        }

        //Refresh posts
        /*es.schedule(() -> {
            int ttl = 5;

            for(Client p : this.node.listPublishersValues()){
                //todo acho que se devia atualizar aqui a variavel pubsResponse
                //todo OS ADDRESS DOS PUBLISHERS PODEM SER NULLS, CUIDADO!!!!!!!!!!!!!, se forem null, mandar logo para os vizinhos

                if(pubsResponse.get(p.getKey())){
                    PostsRequest request = new PostsRequest(this.node.getCausalIdPubs().get(p.getKey()), this.node.getClient(), p.getKey(), ttl);
                    ms.sendAsync(p.getAddress(), "postsRequest", s.encode(request));
                    pubsResponse.put(p.getKey(), false);
                }
                else if(mandarVizinhos(neighborResponse)){
                        PostsRequest request = new PostsRequest(this.node.getCausalIdPubs().get(p.getKey()), this.node.getClient(), p.getKey(), ttl);
                        for(Client c: this.node.getNeighbors()){
                            ms.sendAsync(c.getAddress(), "postsRequest", s.encode(request));
                            neighborResponse.put(c.getKey(), false);
                        }
                }
                else{
                    NodeMsg msgB = new NodeMsg(this.node.getClient(), this.node.getNodeMsgID());
                    this.node.setNodeMsgID(this.node.getNodeMsgID() + 1);
                    this.node.storeState(this.fileName);
                    this.node.writeInTextFile(this.fileName + "_TextVersion");
                    ms.sendAsync(this.bootstrapIP, "network", s.encode(msgB));
                }
            }

            es.schedule(this, 5, TimeUnit.SECONDS);
        }, 5, TimeUnit.SECONDS);

        //Refresh suggestions
        es.schedule(() -> {
            List<Client> network = this.node.listPublishersValues();
            Collections.shuffle(network);
            network = network.subList(0, network.size() / 2);
            for (Client p : network) {
                if(p.getAddress()!=null){
                    ms.sendAsync(p.getAddress(), "suggestionsRequest", s.encode(""));
                }
            }
            es.schedule(this, 10, TimeUnit.SECONDS);
        }, 10, TimeUnit.SECONDS);*/
    }
}
