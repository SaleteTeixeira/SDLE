package Node;

import Common.Client;
import Common.NeighborsReply;
import Common.NodeMsg;
import Common.Util;
import io.atomix.cluster.messaging.ManagedMessagingService;
import io.atomix.cluster.messaging.impl.NettyMessagingService;
import io.atomix.utils.net.Address;
import io.atomix.utils.serializer.Serializer;
import io.atomix.utils.serializer.SerializerBuilder;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Node implements Serializable {
    private Client client;
    private List<Post> myPosts; //added by causal order
    private Integer causalID;

    private List<Client> neighbors;

    private Map<String, Client> subs;
    private Map<String, List<Post>> subsPosts; //added by causal order
    private Map<String, Integer> causalIdSubs;
    private Map<String, List<Post>> waitingListSubsPost;

    private Node(String username, String RSA, String host, int port) {
        this.client = new Client(username, RSA, port, Address.from(host, port));
        this.myPosts = new ArrayList<Post>();
        this.causalID = 1;
        this.neighbors = new ArrayList<>();
        this.subs = new HashMap<>();
        this.subsPosts = new HashMap<>();
        this.causalIdSubs = new HashMap<>();
        this.waitingListSubsPost = new HashMap<>();
    }

    public Client getClient() {
        return this.client;
    }

    public List<Client> getNeighbors() {
        return this.neighbors;
    }

    public Map<String, Client> getSubs(){
        return this.subs;
    }

    public void setNeighbors(List<Client> neighbors) {
        this.neighbors = neighbors;
    }

    public void setIP(String ip) {
        this.client.setAddress(Address.from(ip, this.client.getPort()));
    }

    public List<Client> listNeighbors() {
        List<Client> result = new ArrayList<>();

        for(Client c: this.neighbors){
            if(!this.subs.containsKey(c.getKey())) result.add(c);
        }

        return result;
    }

    public void addSubscriber(Client client) {
        this.subs.put(client.getKey(), client);
    }

    public List<Client> listSubscribers(){
        return new ArrayList<>(this.subs.values());
    }

    public void removeSubscriber(String key){
        this.subs.remove(key);
        this.subsPosts.remove(key);
        this.causalIdSubs.remove(key);
        this.waitingListSubsPost.remove(key);
    }

    public void post(String p){
        Post post = new Post(p, this.causalID);
        this.myPosts.add(post);
        this.causalID++;
    }

    public String toString() {
        StringBuilder ss = new StringBuilder();

        ss.append("----- Node Info -----").append("\n\n");

        ss.append(this.client.toString()).append("\n");

        ss.append("----- My Posts -----").append("\n");
        ss.append(this.myPosts.toString()).append("\n\n");
        ss.append("Next causal ID: ").append(this.causalID).append("\n");

        ss.append("----- Neighbors -----").append("\n");
        ss.append(this.neighbors.toString()).append("\n\n");

        ss.append("----- Subs -----").append("\n\n");
        for (String s : this.subs.keySet()) {
            ss.append(this.subs.get(s).toString()).append("\n");
            ss.append(this.subsPosts.get(s).toString()).append("\n\n");
            ss.append("----- Waiting Posts -----").append("\n");
            ss.append("Next causal ID: ").append(this.causalIdSubs.get(s)).append("\n");
            ss.append(this.waitingListSubsPost.get(s)).append("\n");
        }

        return ss.toString();
    }

    private static void writeInTextFile(Node node, String fileName) throws IOException {
        PrintWriter fich = new PrintWriter(fileName);
        fich.println(node.toString());
        fich.flush();
        fich.close();
    }

    private static void storeState(Node node, String fileName) {
        try {
            FileOutputStream fos = new FileOutputStream(fileName);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(node);
            oos.flush();
            oos.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Node loadState(String username, String RSA, String host, int port, String fileName) {
        Node node = new Node(username, RSA, host, port);

        try {
            FileInputStream fis = new FileInputStream(fileName);
            ObjectInputStream ois = new ObjectInputStream(fis);
            node = (Node) ois.readObject();
            ois.close();
            fis.close();
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Could not find previous state");
        }

        return node;
    }

    private static int showMenu() {
        Scanner scan = new Scanner(System.in);

        System.out.println("Escolha uma das seguintes opções: ");
        System.out.println("1. Publicar.");
        System.out.println("2. Ver timeline.");
        System.out.println("3. Subscrever.");
        System.out.println("4. Des-subscrever.");
        System.out.println("5. Logout.");

        return scan.nextInt();
    }

    private static void post(Node node, String fileName){
        Scanner scan = new Scanner(System.in);

        System.out.println("Escreva, em uma linha, o que pretende publicar.");

        String post = scan.nextLine();
        node.post(post);

        storeState(node, fileName);
    }

    private static void subscribe(Node node, String fileName){
        Scanner scan = new Scanner(System.in);
        int op;

        System.out.println("Escolha uma das seguintes opções: ");
        System.out.println("1. Apresentar lista de vizinhos que ainda não segue.");
        System.out.println("2. Adicionar subscritor à mão.");

        op = scan.nextInt();

        if(op == 1){
            System.out.println("Indique o número do vizinho que pretende subscrever.");
            int i=1;

            List<Client> aux = node.listNeighbors();
            for(Client c: aux){
                System.out.println(i + ". " + c.getUsername() + c.getKey());
                i++;
            }

            int v = scan.nextInt();
            node.addSubscriber(aux.get(v-1));
            System.out.println("Subscrição realizada com sucesso.");

            //todo: atualizar publicações em cache (diogo ???) -> acho que a função é a mesma que ele tem que fazer, estou em dúvida
        }
        else {
            System.out.println("Indique a chave RSA do nodo que pretende subscrever.");
            String key = scan.nextLine();

            //todo (sofia): pedir subscritores pela chave
        }

        storeState(node, fileName);
    }

    private static void unsubscribe(Node node, String fileName){
        Scanner scan = new Scanner(System.in);

        System.out.println("Indique o número do nodo que pretende des-subscrever.");
        int i=1;

        List<Client> aux = node.listSubscribers();
        for(Client c: aux){
            System.out.println(i + ". " + c.getUsername() + c.getKey());
            i++;
        }

        int v = scan.nextInt();
        node.removeSubscriber(aux.get(v-1).getKey());

        storeState(node, fileName);
    }

    public static void main(String[] args) {
        String fileName = "nodeDB";
        String username = args[0];
        String host = Util.getPublicIp();
        int localport = Integer.parseInt(args[1]);
        Address bootstrapIP = Address.from(args[2]);
        String RSAFile = args[3];
        String RSA = Util.LoadRSAKey(RSAFile);
        Node node = loadState(username, RSA, host, localport, fileName);
        boolean out = false;
        int op;

        //todo (sofia): apagar posts antigos

        Serializer s = new SerializerBuilder().build();
        ManagedMessagingService ms = NettyMessagingService.builder().withAddress(Address.from(host, localport)).build();
        ExecutorService es = Executors.newSingleThreadExecutor();

        try {
            ms.registerHandler("network", (o, m) -> {
                NeighborsReply nr = s.decode(m);
                node.setNeighbors(nr.getNeighbors());
                storeState(node, fileName);
            }, es);

            ms.start().get();

            NodeMsg msg = new NodeMsg(node.getClient());

            if (node.getNeighbors().size() == 0) ms.sendAsync(bootstrapIP, "network", s.encode(msg));
            else ms.sendAsync(bootstrapIP, "update", s.encode(msg));

        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        System.out.println("Bem vindo" + node.getClient().getUsername() + ".");

        do{
            op = showMenu();

            switch(op){
                case 1:
                    post(node, fileName);
                    break;
                case 2:
                    // todo (aula 6 de Maio): timeline
                    break;
                case 3:
                    subscribe(node, fileName);
                    break;
                case 4:
                    unsubscribe(node, fileName);
                    break;
            }
        }
        while(op != 5);

        if(op == 5){
            return;
        }
    }
}
