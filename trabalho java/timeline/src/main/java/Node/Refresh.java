package Node;

import Common.*;
import io.atomix.cluster.messaging.ManagedMessagingService;
import io.atomix.cluster.messaging.impl.NettyMessagingService;
import io.atomix.utils.net.Address;
import io.atomix.utils.serializer.Serializer;
import io.atomix.utils.serializer.SerializerBuilder;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
        Serializer s = new SerializerBuilder().build();
        ManagedMessagingService ms = NettyMessagingService.builder().withAddress(Address.from(host, localport)).build();
        ExecutorService es = Executors.newSingleThreadExecutor();

        try {
            ms.start().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        ms.registerHandler("network", (o, m) -> {
            NeighborsReply nr = s.decode(m);
            this.node.addNeighbors(nr.getNeighbors());
            this.node.storeState(fileName);
            this.node.writeInTextFile(fileName + "_TextVersion");
        }, es);

        ms.registerHandler("suggestionsRequest", (o, m) -> {
            SuggestionsRequest request = s.decode(m);

            if (request.getTo().equals(this.node.getClient().getKey())) { //to me
                SuggestionsReply reply = new SuggestionsReply(this.node.listPublishersKeys(), this.node.getClient(), request.getFrom().getKey());
                ms.sendAsync(request.getFrom().getAddress(), "suggestionsReply", s.encode(reply));
            } else {
                //todo (sofia): COMO NO TIMELINE questões do timeout / ttl
                //if -> ver se o tenho nos subscritos
                //if -> ver se o tenho nos vizinhos
                //else if -> mandar para os meus vizinhos
            }
        }, es);

        ms.registerHandler("suggestionsReply", (o, m) -> {
            SuggestionsReply reply = s.decode(m);

            if (reply.getTo().equals(this.node.getClient().getKey())) { //to me
                Client sub = reply.getFrom();
                this.node.updatePublisher(sub);
                this.node.updateSuggestedPubsByPub(sub.getKey(), reply.getSuggestedKeys());

                this.node.storeState(fileName);
                this.node.writeInTextFile(fileName + "_TextVersion");
            } else {
                //todo (sofia): COMO NO TIMELINE questões do timeout / ttl
                //if -> ver se o tenho nos subscritos
                //if -> ver se o tenho nos vizinhos
                //else if -> mandar para os meus vizinhos
            }
        }, es);

        //Initial communication with bootstrap
        NodeMsg msg = new NodeMsg(this.node.getClient());
        if (this.node.getNeighbors().size() == 0) {
            ms.sendAsync(bootstrapIP, "network", s.encode(msg));
        } else {
            ms.sendAsync(bootstrapIP, "update", s.encode(msg));

            /*todo (geral): deviamos enviar aqui (aka antes de tudo) msg a pedir publicações novas e sugestões de subscritores OU só qd o utilizador faz viewTimeline/pede sugestões?
            Assim já tinhamos coisas novas para mostrar nessas funcionalidades
             */
        }
    }
}
