package Common;

public class NodeMsg {
    private Client client;

    public NodeMsg(Client c){
        this.client = c;
    }

    public Client getClient() {
        return this.client;
    }

    public String toString(){
        return "----- Node Msg -----\n" + this.client.toString();
    }
}
