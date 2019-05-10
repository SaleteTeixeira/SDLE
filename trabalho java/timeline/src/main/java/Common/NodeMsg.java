package Common;

public class NodeMsg {
    private Client client;
    private int id;

    public NodeMsg(Client c, int id) {
        this.client = c;
        this.id = id;
    }

    public Client getClient() {
        return this.client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String toString() {
        return "----- Node Msg " + this.id + " -----\n" + this.client.toString();
    }
}