package Node;

import Common.*;
import io.atomix.cluster.messaging.ManagedMessagingService;
import io.atomix.cluster.messaging.impl.NettyMessagingService;
import io.atomix.utils.concurrent.Scheduled;
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

    @Override
    public void run() {
        Serializer s = new SerializerBuilder().build();
        ManagedMessagingService ms = NettyMessagingService.builder().withAddress(Address.from(this.host, this.localport)).build();
        ScheduledExecutorService es = Executors.newSingleThreadScheduledExecutor();

        try {
            ms.start().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        ms.registerHandler("network", (o, m) -> {
            NeighborsReply nr = s.decode(m);
            this.node.addNeighbors(nr.getNeighbors());
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

            //Message
            if(to.equals(this.node.getClient().getKey())){
                List<Post> subList = this.postsAfterCausalID(this.node.getMyPosts(), causalID);
                PostsReply reply = new PostsReply(subList.stream().collect(Collectors.toSet()), this.node.getClient(), from.getKey());
            }
            else {
                request.setCausalID(ttl-1);

                if (this.node.listPublishersKeys().contains(to)){
                    List<Post> waitingList = this.postsAfterCausalID(this.node.getWaitingListPubsPost().get(to), causalID);
                    List<Post> subList = this.postsAfterCausalID(this.node.getPublisherPosts(to), causalID);
                    subList.addAll(waitingList);

                    Set<Post> orderedList = new TreeSet<>(new OrderedPostsByID());
                    orderedList.addAll(subList);

                    PostsReply reply = new PostsReply(orderedList, this.node.getPublishers().get(to), from.getKey());

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
            String to = reply.getTo();

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

            //Message
            if(to.equals(this.node.getClient().getKey())){
                //todo aceitar por ordem
                //todo meter na BD
            }
        }, es);

        //Initial communication with bootstrap
        NodeMsg msg = new NodeMsg(this.node.getClient(), this.node.getNodeMsgID());
        this.node.setNodeMsgID(this.node.getNodeMsgID() + 1);
        if (this.node.getNeighbors().size() == 0) {
            ms.sendAsync(this.bootstrapIP, "network", s.encode(msg));
        } else {
            ms.sendAsync(this.bootstrapIP, "update", s.encode(msg));
        }

        //Refresh posts
        es.schedule(() -> {
            //todo (geral): msg a pedir publicações novas de X em X tempo
            //- pede nova informação aos subscritores apartir de um certo causalID
            //- se não conseguir resposta ao fim de X tempo (timeout), pede aos vizinhos indicando o TTL da msg (adiciona à BD essa nova info)
            //- ao receber a msg do vizinho tenta atualizar o IP do subscritor através do id da msg mais recente (aka causalID -> tem de vir com a info do client)
            //- se os vizinhos não responderem ao fim de X tempo (timeout), vai ao bootstrap pedir mais vizinhos (manda a sua chave e ip again) (adiciona à BD essa nova info)
            int ttl = 5;

            for(Client p : this.node.listPublishersValues()){
                PostsRequest request = new PostsRequest(this.node.getCausalIdPubs().get(p.getKey()), this.node.getClient(), p.getKey(),ttl);
                ms.sendAsync(p.getAddress(), "postsRequest", s.encode(request));
            }

            es.schedule(this, 5, TimeUnit.SECONDS);
        }, 5, TimeUnit.SECONDS);

        //Refresh suggestions
        es.schedule(() -> {
            List<Client> network = this.node.listPublishersValues();
            Collections.shuffle(network);
            network = network.subList(0, network.size() / 2);
            for (Client p : network) {
                ms.sendAsync(p.getAddress(), "suggestionsRequest", s.encode(""));
            }
            es.schedule(this, 10, TimeUnit.SECONDS);
        }, 10, TimeUnit.SECONDS);
    }
}
