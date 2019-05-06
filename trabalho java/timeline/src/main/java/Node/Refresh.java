package Node;

import Common.Client;
import Common.NeighborsReply;
import Common.SuggestionsReply;
import Common.SuggestionsRequest;
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
    private final String fileName;

    Refresh(Node node, String host, int localport, String fileName) {
        this.node = node;
        this.host = host;
        this.localport = localport;
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
            node.setNeighbors(nr.getNeighbors());
            Node.storeState(node, fileName);
            Node.writeInTextFile(node, fileName + "_TextVersion");
        }, es);

        ms.registerHandler("suggestionsRequest", (o, m) -> {
            SuggestionsRequest request = s.decode(m);

            if (request.getTo().equals(node.getClient().getKey())) { //to me
                SuggestionsReply reply = new SuggestionsReply(node.listSubscribersKeys(), node.getClient(), request.getFrom().getKey());
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

            if (reply.getTo().equals(node.getClient().getKey())) { //to me
                Client sub = reply.getFrom();
                node.updateSubscriber(sub);
                node.updateSuggestedSubsBySub(sub.getKey(), reply.getSuggestedKeys());

                Node.storeState(node, fileName);
                Node.writeInTextFile(node, fileName + "_TextVersion");
            } else {
                //todo (sofia): COMO NO TIMELINE questões do timeout / ttl
                //if -> ver se o tenho nos subscritos
                //if -> ver se o tenho nos vizinhos
                //else if -> mandar para os meus vizinhos
            }
        }, es);
    }
}
