package Bootstrap;

import Common.*;

import java.io.*;
import java.util.*;

import Common.Messages.NeighborsReply;
import Common.Messages.Network;
import Common.Messages.NodeMsg;
import io.atomix.cluster.messaging.ManagedMessagingService;
import io.atomix.cluster.messaging.impl.NettyMessagingService;
import io.atomix.utils.net.Address;
import io.atomix.utils.serializer.Serializer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Bootstrap {
    public static void main(String[] args) {
        String file_clients = "bootstrapDB_clients";
        String file_ids = "bootstrapDB_ids";

        try {
            Map<String, Client> clients = loadState_clients(file_clients);
            Map<String, Integer> ids = loadState_ids(file_ids);

            Serializer s = Util.buildSerializer();
            ManagedMessagingService ms = NettyMessagingService.builder().withAddress(Address.from(args[0])).build();
            ExecutorService es = Executors.newSingleThreadExecutor();

            ms.start().get();

            ms.registerHandler("neighbors", (o, m) -> {
                NodeMsg msg = s.decode(m);
                System.out.println("\nReceived a neighbors request from "+msg.getClient().getKey()+".");

                int id = msg.getMsgID();
                if (!ids.containsKey(msg.getClient().getKey())) {
                    System.out.println("New client!");
                    ids.put(msg.getClient().getKey(), 0);
                }
                if (id > ids.get(msg.getClient().getKey())) {
                    System.out.println("Updating client "+msg.getClient().getKey()+" information.");
                    clients.put(msg.getClient().getKey(), msg.getClient());
                    ids.put(msg.getClient().getKey(), id);

                    System.out.println("Sending him his neighbors and updated network.");
                    List<Client> neighbors = neighbors(clients);
                    ms.sendAsync(o, "neighbors", s.encode(new NeighborsReply(neighbors)));
                    ms.sendAsync(o, "network", s.encode(new Network(clients)));
                }

                storeState_clients(clients, file_clients);
                storeState_ids(ids, file_ids);
                writeInTextFile(ids, clients, "bootstrapDB_TextVersion");
            }, es);

            ms.registerHandler("update", (o, m) -> {
                NodeMsg msg = s.decode(m);
                System.out.println("\nReceived a update request from "+msg.getClient().getKey()+".");

                int id = msg.getMsgID();
                if (!ids.containsKey(msg.getClient().getKey())) {
                    System.out.println("New client!");
                    ids.put(msg.getClient().getKey(), 0);
                }
                if (id > ids.get(msg.getClient().getKey())) {
                    System.out.println("Updating client "+msg.getClient().getKey()+" information.");
                    clients.put(msg.getClient().getKey(), msg.getClient());
                    ids.put(msg.getClient().getKey(), id);

                    System.out.println("Sending him updated network.");
                    ms.sendAsync(o, "network", s.encode(new Network(clients)));
                }

                storeState_clients(clients, file_clients);
                storeState_ids(ids, file_ids);
                writeInTextFile(ids, clients, "bootstrapDB_TextVersion");
            }, es);

        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    private static List<Client> neighbors(Map<String, Client> clients) {
        List<Client> neighbors = new ArrayList<>(clients.values());
        Collections.shuffle(neighbors);

        if(neighbors.size()<=5){
            return new ArrayList<>(neighbors.subList(0, neighbors.size()));
        }
        else{
            return new ArrayList<>(neighbors.subList(0, 5));
        }
    }

    private static Map<String, Client> loadState_clients(String file) {
        Map<String, Client> clients;

        try {
            FileInputStream fileIn = new FileInputStream(file);
            ObjectInputStream in = new ObjectInputStream(fileIn);
            clients = (Map<String, Client>) in.readObject();
            in.close();
            fileIn.close();

            if (clients == null) {
                clients = new HashMap<>();
            }
        } catch (IOException | ClassNotFoundException e) {
            clients = new HashMap<>();
            System.out.println("Could not find previous state for client.");
        }

        return clients;
    }

    private static void storeState_clients(Map<String, Client> clients, String file) {
        try {
            FileOutputStream fileOut = new FileOutputStream(file);
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(clients);
            out.flush();
            out.close();
            fileOut.close();
        } catch (IOException e) {
            System.out.println("Error saving clients state.");
        }
    }

    private static Map<String, Integer> loadState_ids(String file) {
        Map<String, Integer> ids;

        try {
            FileInputStream fileIn = new FileInputStream(file);
            ObjectInputStream in = new ObjectInputStream(fileIn);
            ids = (Map<String, Integer>) in.readObject();
            in.close();
            fileIn.close();

            if (ids == null) {
                ids = new HashMap<>();
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Could not find previous state for ids.");
            ids = new HashMap<>();
        }

        return ids;
    }

    private static void storeState_ids(Map<String, Integer> ids, String file) {
        try {
            FileOutputStream fileOut = new FileOutputStream(file);
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(ids);
            out.flush();
            out.close();
            fileOut.close();
        } catch (IOException e) {
            System.out.println("Error saving ids state.");
        }
    }

    private static void writeInTextFile(Map<String, Integer> ids, Map<String, Client> clients, String fileName) {
        try {
            PrintWriter fich = new PrintWriter(fileName);
            for(String s : ids.keySet()){
                fich.println("----- "+s+" -----");
                fich.println("Last received message: "+ids.get(s));
                fich.println(clients.get(s));
            }
            fich.flush();
            fich.close();
        } catch (IOException e) {
            System.out.println("Error saving state in text file.");
        }
    }
}
