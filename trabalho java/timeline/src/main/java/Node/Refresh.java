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
        ManagedMessagingService ms = NettyMessagingService.builder().withAddress(Address.from(this.host, this.localport)).build();
        ExecutorService es = Executors.newSingleThreadExecutor();

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

        //todo: msg postRequest
        // quem faz o request: dar o causalID a partir do qual queremos posts

        //todo: msg postReply
        // quem faz o reply: mandar posts do causalID pedido até ao atual
        // quem faz o reply: manda o seu client para o node que pediu atualizar essa info
        // quem recebe o reply: atualizar a BD

        //Initial communication with bootstrap
        NodeMsg msg = new NodeMsg(this.node.getClient());
        if (this.node.getNeighbors().size() == 0) {
            ms.sendAsync(this.bootstrapIP, "network", s.encode(msg));
        } else {
            ms.sendAsync(this.bootstrapIP, "update", s.encode(msg));
            //todo (duvida diogo): o update aqui não faz o mesmo que o network? -> resposta: não, só atualiza o IP, não recebe resposta com vizinhos
        }

        //Refresh loop
        while (true) {
            //todo (geral): msg a pedir publicações novas de X em X tempo
            //- pede nova informação aos subscritores
            //- se não conseguir resposta ao fim de X tempo (timeout), pede aos vizinhos indicando o TTL da msg (adiciona à BD essa nova info)
            //- ao receber a msg do vizinho tenta atualizar o IP do subscritor através do id da msg mais recente (aka causalID -> tem de vir com a info do client)
            //- se os vizinhos não responderem ao fim de X tempo (timeout), vai ao bootstrap pedir mais vizinhos (manda a sua chave e ip again) (adiciona à BD essa nova info)

            //todo (geral): msg a pedir sugestões de subscritores de X em X tempo

            for (Client neighbor : this.node.getNeighbors()) {

            }
        }
    }
}
