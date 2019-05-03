package Common;

public class NodeMsg {
    private Client client;

    public NodeMsg(Client c){
        this.client = c;
    }

    public Client getClient() {
        return client;
    }

    public String toString(){
        return "New NodeMsg from client " + this.client.getUsername();
    }
}
