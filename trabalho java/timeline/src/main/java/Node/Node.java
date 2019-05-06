package Node;

import Common.*;
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
import java.util.stream.Collectors;

//todo (geral): NODE + BOOTSTRAP -> o serializable é um bocado mau pq ...
//em FSD escrevia-se no LOG e dps é que se fazia as alterações (put/get) -> o que faz sentido,
//mas com o serializable não dá para fazer isso pq ele guarda o objeto (logo é preciso alterar e só dps guardar)

//todo (geral): qd aplicarmos a thread (store com CF dava, não?), talvez tenhamos de sincronizar/lockar isto tudo :(
public class Node implements Serializable {
    private Client client;
    private List<Post> myPosts; //added by causal order
    private Integer causalID;

    private List<Client> neighbors;
    
    private Map<String, Client> publishers;
    private Map<String, List<Post>> pubsPosts; //added by causal order
    private Map<String, Integer> causalIdPubs;
    private Map<String, List<Post>> waitingListPubsPost;
    
    private Map<String, List<String>> suggestedPubsByPub;

    private Node(String username, String RSA, String host, int port) {
        this.client = new Client(username, RSA, Address.from(host, port));
        this.myPosts = new ArrayList<Post>();
        this.causalID = 1;
        this.neighbors = new ArrayList<>();
        this.publishers = new HashMap<>();
        this.pubsPosts = new HashMap<>();
        this.causalIdPubs = new HashMap<>();
        this.waitingListPubsPost = new HashMap<>();
        this.suggestedPubsByPub = new HashMap<>();
    }

    public Client getClient() {
        return this.client;
    }

    public List<Client> getNeighbors() {
        return this.neighbors;
    }

    public List<Post> getCloneMyPosts(){
        List<Post> result = new ArrayList<>();

        for(Post p: this.myPosts){
            result.add(p.clone());
        }

        return result;
    }

    public void addNeighbors(List<Client> neighbors) {
        for(Client c: neighbors){
            if(!this.neighbors.contains(c)) this.neighbors.add(c);
        }
    }

    public String toString() {
        StringBuilder ss = new StringBuilder();

        ss.append("----- Node -----").append("\n\n");

        ss.append(this.client.toString()).append("\n");

        ss.append("----- My Posts -----").append("\n");
        ss.append(this.myPosts.toString()).append("\n\n");
        ss.append("Next causal ID: ").append(this.causalID).append("\n");

        ss.append("----- Neighbors -----").append("\n");
        ss.append(this.neighbors.toString()).append("\n\n");

        ss.append("----- Pubs -----").append("\n\n");
        for (String s : this.publishers.keySet()) {
            ss.append(this.publishers.get(s).toString()).append("\n");
            ss.append("----- Posts -----").append("\n");
            ss.append(this.pubsPosts.get(s).toString()).append("\n\n");
            ss.append("----- Waiting Posts -----").append("\n");
            ss.append("Next causal ID: ").append(this.causalIdPubs.get(s)).append("\n");
            ss.append(this.waitingListPubsPost.get(s).toString()).append("\n\n");
            ss.append("----- Suggested Pubss -----").append("\n");
            ss.append(this.suggestedPubsByPub.get(s).toString()).append("\n\n");
        }

        return ss.toString();
    }

    public void addPost(String p){
        Post post = new Post(p, this.causalID);
        this.myPosts.add(post);
        this.causalID++;
    }

    public void removeOneWeekOldPosts(){
        Map<String, List<Post>> newPubsPosts = new HashMap<>();

        for(Map.Entry<String, List<Post>> e : this.pubsPosts.entrySet()){
            List<Post> newPosts = e.getValue().stream()
                                              .filter(post -> !post.oneWeekOld())
                                              .collect(Collectors.toList());

            newPubsPosts.put(e.getKey(), newPosts);
        }

        this.pubsPosts = newPubsPosts;
        newPubsPosts = null;
    }

    public List<Client> listNeighbors_NotFollowing() {
        List<Client> result = new ArrayList<>();

        for(Client c: this.neighbors){
            if(!this.publishers.containsKey(c.getKey())) result.add(c);
        }

        return result;
    }

    public List<String> listPublishersKeys(){
        return new ArrayList<>(this.publishers.keySet());
    }

    public List<Client> listPublishersValues(){
        return new ArrayList<>(this.publishers.values());
    }

    public void addPublisher(String tempUsername, String key) {
        this.publishers.put(key, new Client(tempUsername, key));
        this.pubsPosts.put(key, new ArrayList<>());
        this.causalIdPubs.put(key, 1);
        this.waitingListPubsPost.put(key, new ArrayList<>());
    }

    public void addPublisher(Client client) {
        String key = client.getKey();

        this.publishers.put(key, client);
        this.pubsPosts.put(key, new ArrayList<>());
        this.causalIdPubs.put(key, 1);
        this.waitingListPubsPost.put(key, new ArrayList<>());
    }

    public void updatePublisher(Client client){
        this.publishers.put(client.getKey(), client);
    }

    public void removePublisher(String key){
        this.publishers.remove(key);
        this.pubsPosts.remove(key);
        this.causalIdPubs.remove(key);
        this.waitingListPubsPost.remove(key);
        this.suggestedPubsByPub.remove(key);
    }

    public void updateSuggestedPubsByPub(String pubKey, List<String> suggestedPubs) {
        this.suggestedPubsByPub.put(pubKey, suggestedPubs);
    }

    private static void writeInTextFile(Node node, String fileName) {
        try {
            PrintWriter fich = new PrintWriter(fileName);
            fich.println(node.toString());
            fich.flush();
            fich.close();
        }
        catch (IOException e){
            System.out.println("Error saving state in text file.");
        }
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
            System.out.println("Error saving state.");
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
            System.out.println("Could not find previous state.");
        }

        return node;
    }

    // -------- INTERFACE --------

    //Menu option 1
    private static void post(Node node, String fileName){
        Scanner scan = new Scanner(System.in);

        System.out.println("Escreva, em uma linha, o que pretende publicar.");

        String post = scan.nextLine();
        node.addPost(post);

        System.out.println("Publicação publicada com sucesso.");
        storeState(node, fileName);
        writeInTextFile(node, fileName+"_TextVersion");
    }

    //Menu option 2
    private static void perfil(Node node){
        System.out.println("Ola " + node.getClient().getUsername() + ", está a ver o seu perfil.");
        
        List<Post> aux = node.getCloneMyPosts();
        Collections.reverse(aux);

        for(Post p: aux){
            System.out.println(p.toString());
        }
    }

    //Menu option 3
    private static void viewTimeline(Node node) {
    }

    //Menu option 4
    private static void subscribe(Node node, String fileName, ManagedMessagingService ms, Serializer s){
        Scanner scan = new Scanner(System.in);
        int op;

        do{
            System.out.println("Escolha uma das seguintes opções: ");
            System.out.println("1. Subscrever um vizinho que ainda não segue.");
            System.out.println("2. Subscrever dando uma chave publica.");
            System.out.println("3. Sugestões das pessoas que segues.");
            System.out.println("4. Voltar para o menu principal.");

            if (scan.hasNextInt()) {
                op = scan.nextInt();
            } else {
                op = -1;
            }

            switch(op){
                case 1:
                    System.out.println("Indique o número do vizinho que pretende subscrever.");
                    int i=1;

                    List<Client> aux = node.listNeighbors_NotFollowing();
                    for(Client c: aux){
                        System.out.println(i + ". " + c.getUsername() + ": " + c.getKey());
                        i++;
                    }

                    if(i!=1){
                        if (scan.hasNextInt()) {
                            try{
                                int v = scan.nextInt();
                                node.addPublisher(aux.get(v-1));
                                System.out.println("Subscrição realizada com sucesso.");
                                storeState(node, fileName);
                                writeInTextFile(node, fileName+"_TextVersion");

                                //todo (diogo): msg a pedir publicações -> atualizar publicações em cache
                            }
                            catch(IndexOutOfBoundsException e){
                                System.out.println("Erro: número escolhido inválido.");
                            }
                        }
                        else {
                            System.out.println("Erro: opção inválida.");
                        }
                    }
                    else{
                        System.out.println("Já está subscrito a todos os seus vizinhos!");
                    }

                    break;
                case 2:
                    System.out.println("Indique a chave RSA do nodo que pretende subscrever.");
                    String key = scan.nextLine();

                    System.out.println("Indique o username a ser associado a esse nó, temporariamente.");
                    String tempUsername = scan.nextLine();

                    node.addPublisher(tempUsername, key);
                    System.out.println("Subscrição realizada com sucesso.");

                    storeState(node, fileName);
                    writeInTextFile(node, fileName+"_TextVersion");

                    //todo (diogo): msg a pedir publicações -> atualizar publicações em cache
                    break;
                case 3:
                    for(Map.Entry<String, List<String>> e : node.suggestedPubsByPub.entrySet()){
                        System.out.println("Sugerido por "+node.publishers.get(e.getKey()).getUsername()+": "+e.getKey()+"\n");
                        int j=1;

                        for(String str : e.getValue()){
                            System.out.println(j+". "+str);
                            j++;
                        }

                        System.out.println("");

                        Address pubAddress = node.publishers.get(e.getKey()).getAddress();
                        if(pubAddress==null){
                            //todo (sofia): COMO NO TIMELINE pedir aos vizinhos (TTL + timeout) -> bootstrap
                        }
                        else{
                            SuggestionsRequest request = new SuggestionsRequest(node.getClient(), e.getKey());
                            ms.sendAsync(pubAddress, "suggestionsRequest", s.encode(request));
                            //todo (sofia): COMO NO TIMELINE timeout para dps mandar aos vizinhos (TTL e timeout again) -> bootstrap
                        }
                    }

                    System.out.println("Faça refresh para atualizar.");
                    break;
                case 4:
                    break;
                default:
                    System.out.println("Error: opção inválida. Tente de novo.");
                    break;
            }
        }
        while (op!=4);
    }

    //Menu option 5
    private static void unsubscribe(Node node, String fileName){
        Scanner scan = new Scanner(System.in);

        System.out.println("Indique o número do nodo que pretende des-subscrever.");
        int i=1;

        List<Client> aux = node.listPublishersValues();
        for(Client c: aux){
            System.out.println(i + ". " + c.getUsername() + ": "+ c.getKey());
            i++;
        }

        if(i!=1){
            if (scan.hasNextInt()) {
                try{
                    int v = scan.nextInt();
                    node.removePublisher(aux.get(v-1).getKey());
                    System.out.println("Des-subscrição realizada com sucesso.");
                    storeState(node, fileName);
                    writeInTextFile(node, fileName+"_TextVersion");
                }
                catch(IndexOutOfBoundsException e){
                    System.out.println("Erro: número escolhido inválido.");
                }
            }
            else {
                System.out.println("Erro: opção inválida.");
            }
        }
        else{
            System.out.println("Não está subscrito a nenhum nodo!");
        }
    }

    private static int showMenu() {
        Scanner scan = new Scanner(System.in);

        System.out.println("Escolha uma das seguintes opções:");
        System.out.println("1. Publicar.");
        System.out.println("2. Ver perfil.");
        System.out.println("3. Ver timeline.");
        System.out.println("4. Subscrever.");
        System.out.println("5. Des-subscrever.");
        System.out.println("6. Logout.");

        if (scan.hasNextInt()) {
            return scan.nextInt();
        } else {
            return -1;
        }
    }

    public static void main(String[] args) {
        //todo (geral): podiamos fazer -> se ao carregar o client der null, pedimos username e avisamos q não dá para mudar mais e tirava-se do args
        String username = args[0];
        int localport = Integer.parseInt(args[1]);
        Address bootstrapIP = Address.from(args[2]);
        String RSAFile = args[3];

        //Get public key and IP
        String host = Util.getPublicIp();
        String RSA = Util.LoadRSAKey(RSAFile);

        //Initialize new node or with previous state
        String fileName = "nodeDB";
        Node node = loadState(username, RSA, host, localport, fileName);
        node.removeOneWeekOldPosts();
        storeState(node, fileName);
        writeInTextFile(node, fileName+"_TextVersion");

        //Set possible communication messages
        //todo (geral): não poderiamos ter um store com CF para isto ??? seria muito melhor para tratar da comunicação toda (questão do timeout, mandar para os vizinhos, ...)
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
            node.addNeighbors(nr.getNeighbors());
            storeState(node, fileName);
            writeInTextFile(node, fileName+"_TextVersion");
        }, es);

        ms.registerHandler("suggestionsRequest", (o, m) -> {
            SuggestionsRequest request = s.decode(m);

            if(request.getTo().equals(node.getClient().getKey())) { //to me
                SuggestionsReply reply = new SuggestionsReply(node.listPublishersKeys(), node.getClient(), request.getFrom().getKey());
                ms.sendAsync(request.getFrom().getAddress(),"suggestionsReply", s.encode(reply));
            }
            else{
                //todo (sofia): COMO NO TIMELINE questões do timeout / ttl
                //if -> ver se o tenho nos subscritos
                //if -> ver se o tenho nos vizinhos
                //else if -> mandar para os meus vizinhos
            }
        }, es);

        ms.registerHandler("suggestionsReply", (o, m) -> {
            SuggestionsReply reply = s.decode(m);

            if(reply.getTo().equals(node.getClient().getKey())){ //to me
                Client pub = reply.getFrom();
                node.updatePublisher(pub);
                node.updateSuggestedPubsByPub(pub.getKey(), reply.getSuggestedKeys());

                storeState(node, fileName);
                writeInTextFile(node, fileName+"_TextVersion");
            }
            else{
                //todo (sofia): COMO NO TIMELINE questões do timeout / ttl
                //if -> ver se o tenho nos subscritos
                //if -> ver se o tenho nos vizinhos
                //else if -> mandar para os meus vizinhos
            }
        }, es);

        //Initial communication with bootstrap
        NodeMsg msg = new NodeMsg(node.getClient());
        if (node.getNeighbors().size() == 0) ms.sendAsync(bootstrapIP, "network", s.encode(msg));
        else {
            ms.sendAsync(bootstrapIP, "update", s.encode(msg));

            /*todo (geral): deviamos enviar aqui (aka antes de tudo) msg a pedir publicações novas e sugestões de subscritores OU só qd o utilizador faz viewTimeline/pede sugestões?
            Assim já tinhamos coisas novas para mostrar nessas funcionalidades
             */
        }

        //Terminal interface
        //todo (geral): está tudo em inglês menos o menu ??? xD
        System.out.println("Bem vindo, " + node.getClient().getUsername() + ".");
        int op;

        do{
            op = showMenu();

            switch(op){
                case 1:
                    post(node, fileName);
                    break;
                case 2:
                    perfil(node);
                    break;
                case 3:
                    viewTimeline(node);
                    //todo (geral): aula 6 de Maio (overleaf detalhes)
                    //todo (geral): aqui dentro podia ser 1. view timeline de um publishers em especifico 2. ver timeline de todos os publishers
                    break;
                case 4:
                    subscribe(node, fileName, ms, s);
                    break;
                case 5:
                    unsubscribe(node, fileName);
                    break;
                case 6:
                    break;
                default:
                    System.out.println("Erro: opção inválida. Tente de novo.");
                    break;
            }
        }
        while(op != 6);

        System.out.println("\nAté já, " + node.getClient().getUsername() + ".");
    }
}


/* todo (geral): dúvidas no overleaf */