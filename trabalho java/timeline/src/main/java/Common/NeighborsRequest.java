package Common;

public class NeighborsRequest {
    private Client client;

    public NeighborsRequest(Client c){
        this.client = c;
    }

    public Client getClient() {
        return client;
    }

    public String toString(){
        return "New NeighborRequest from client " + this.client.getUsername();
    }
}
