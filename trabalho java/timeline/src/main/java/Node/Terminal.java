package Node;

import Common.Client;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Terminal implements Runnable {

    private final Node node;
    private final String fileName;

    Terminal(Node node, String fileName) {
        this.node = node;
        this.fileName = fileName;
    }

    @Override
    public void run() {
        System.out.println("Welcome, " + this.node.getClient().getUsername() + ".");
        int op;

        do{
            op = this.showMenu();

            switch(op){
                case 1:
                    this.post();
                    break;
                case 2:
                    this.profile();
                    break;
                case 3:
                    this.viewTimeline();
                    //todo (geral): aula 6 de Maio (overleaf detalhes)
                    //todo (geral): aqui dentro podia ser 1. view timeline de um publishers em especifico 2. ver timeline de todos os publishers
                    break;
                case 4:
                    this.subscribe();
                    break;
                case 5:
                    this.unsubscribe();
                    break;
                case 6:
                    break;
                default:
                    System.out.println("Error: invalid option. Try again.");
                    break;
            }
        }
        while(op != 6);

        System.out.println("\nSee you later, " + this.node.getClient().getUsername() + ".");
    }

    private int showMenu() {
        Scanner scan = new Scanner(System.in);

        System.out.println("Choose one of the following options:");
        System.out.println("1. Publish.");
        System.out.println("2. View profile.");
        System.out.println("3. View timeline.");
        System.out.println("4. Subscribe.");
        System.out.println("5. Unsubscribe.");
        System.out.println("6. Logout.");

        if (scan.hasNextInt()) {
            return scan.nextInt();
        } else {
            return -1;
        }
    }

    // -------- INTERFACE --------

    //Menu option 1
    private void post(){
        Scanner scan = new Scanner(System.in);

        System.out.println("Writte in one line what you want to post");

        String post = scan.nextLine();
        this.node.addPost(post);

        System.out.println("Post published with success");
        this.node.storeState(this.fileName);
        this.node.writeInTextFile(this.fileName+"_TextVersion");
    }

    //Menu option 2
    private void profile(){
        System.out.println("Ola " + this.node.getClient().getUsername() + ", this is your profile.");

        List<Post> aux = this.node.getCloneMyPosts();
        Collections.reverse(aux);

        for(Post p: aux){
            System.out.println(p.toString());
        }
    }

    //Menu option 3
    private void viewTimeline() {
    }

    //Menu option 4
    private void subscribe(){
        Scanner scan = new Scanner(System.in);
        int op;

        do{
            System.out.println("Chose one of the following options: ");
            System.out.println("1. Subscribe a non yet subscribed neighbor.");
            System.out.println("2. Subscribe giving an RSA key.");
            System.out.println("3. Suggestions of people you follow.");
            System.out.println("4. Back to the main menu.");

            if (scan.hasNextInt()) {
                op = scan.nextInt();
            } else {
                op = -1;
            }

            switch(op){
                case 1:
                    System.out.println("Specify the number of the neighbor you want to subscribe.");
                    int i=1;

                    List<Client> aux = this.node.listNeighbors_NotFollowing();
                    for(Client c: aux){
                        System.out.println(i + ". " + c.getUsername() + ": " + c.getKey());
                        i++;
                    }

                    if(i!=1){
                        if (scan.hasNextInt()) {
                            try{
                                int v = scan.nextInt();
                                this.node.addPublisher(aux.get(v-1));
                                System.out.println("Subscription done with success.");
                                this.node.storeState(this.fileName);
                                this.node.writeInTextFile(this.fileName +"_TextVersion");
                            }
                            catch(IndexOutOfBoundsException e){
                                System.out.println("Error: chosen number is invalid.");
                            }
                        }
                        else {
                            System.out.println("Error: invalid option.");
                        }
                    }
                    else{
                        System.out.println("You are already subscribed to all your neighbors.");
                    }

                    break;
                case 2:
                    System.out.println("Specify the RSA key of the node you want to subscribe.");
                    String key = scan.nextLine();

                    System.out.println("Specify the username to partner temporarily to this node.");
                    String tempUsername = scan.nextLine();

                    this.node.addPublisher(tempUsername, key);
                    System.out.println("Subscription done with sucess");

                    this.node.storeState(this.fileName);
                    this.node.writeInTextFile(this.fileName +"_TextVersion");

                    break;
                case 3:
                    for(Map.Entry<String, List<String>> e : this.node.getSuggestedPubsByPub().entrySet()){
                        System.out.println("Sugested by "+ this.node.getPublishers().get(e.getKey()).getUsername()+": "+e.getKey()+"\n");
                        int j=1;

                        for(String str : e.getValue()){
                            System.out.println(j+". "+str);
                            j++;
                        }

                        System.out.println();
                    }

                    break;
                case 4:
                    break;
                default:
                    System.out.println("Error: invalid option. Try again.");
                    break;
            }
        }
        while (op!=4);
    }

    //Menu option 5
    private void unsubscribe(){
        Scanner scan = new Scanner(System.in);

        System.out.println("Specify the number of the node you want to unsubscribe.");
        int i=1;

        List<Client> aux = this.node.listPublishersValues();
        for(Client c: aux){
            System.out.println(i + ". " + c.getUsername() + ": "+ c.getKey());
            i++;
        }

        if(i!=1){
            if (scan.hasNextInt()) {
                try{
                    int v = scan.nextInt();
                    this.node.removePublisher(aux.get(v-1).getKey());
                    System.out.println("You are now unsubscribed.");
                    this.node.storeState(this.fileName);
                    this.node.writeInTextFile(this.fileName +"_TextVersion");
                }
                catch(IndexOutOfBoundsException e){
                    System.out.println("Error: chosen number is invalid.");
                }
            }
            else {
                System.out.println("Error: invalid option.");
            }
        }
        else{
            System.out.println("You are not subscribed to any node!");
        }
    }
}
