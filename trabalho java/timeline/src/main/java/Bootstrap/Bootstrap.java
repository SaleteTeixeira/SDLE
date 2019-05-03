package Bootstrap;

import Common.Client;

import java.io.*;
import java.util.*;

import Common.NeighborsReply;
import Common.NodeMsg;
import io.atomix.cluster.messaging.ManagedMessagingService;
import io.atomix.cluster.messaging.impl.NettyMessagingService;
import io.atomix.utils.net.Address;
import io.atomix.utils.serializer.Serializer;
import io.atomix.utils.serializer.SerializerBuilder;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Bootstrap {
    public static void main(String[] args){
        String file = "bootstrapDB";

        try {
            Map<String, Client> clients = loadState(file);

            Serializer s = new SerializerBuilder().build();
            ManagedMessagingService ms = NettyMessagingService.builder().withAddress(Address.from(args[0])).build();
            ExecutorService es = Executors.newSingleThreadExecutor();

            ms.registerHandler("network", (o, m) -> {
                NodeMsg msg = s.decode(m);
                clients.put(msg.getClient().getKey(), msg.getClient());

                List<Client> network = neighbors(clients);
                NeighborsReply send = new NeighborsReply(network);
                ms.sendAsync(o,"network", s.encode(send));

                storeState(clients, file);
            }, es);

            ms.registerHandler("update", (o,m) -> {
                NodeMsg msg = s.decode(m);
                clients.put(msg.getClient().getKey(), msg.getClient());
                storeState(clients, file);
            }, es);

            ms.start().get();

        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    private static List<Client> neighbors(Map<String, Client> clients) {
        List<Client> network = new ArrayList<>(clients.values());
        Collections.shuffle(network);
        return network.subList(0, 5);
    }

    private static Map<String, Client> loadState(String file){
        Map<String, Client> clients;

        try{
            FileInputStream fileIn = new FileInputStream(file);
            ObjectInputStream in = new ObjectInputStream(fileIn);
            clients = (Map<String, Client>) in.readObject();
            in.close();
            fileIn.close();

            if(clients == null){
                clients = new HashMap<>();
            }
        } catch (IOException | ClassNotFoundException e) {
            clients = new HashMap<>();
        }

        return clients;
    }

    private static void storeState(Map<String, Client> clients, String file){
        try {
            FileOutputStream fileOut = new FileOutputStream(file);
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(clients);
            out.flush();
            out.close();
            fileOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
