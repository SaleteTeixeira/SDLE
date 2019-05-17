package Common.Messages;

import Common.Client;

public class NodeMsg {
    private Client client;
    private int msgID;

    public NodeMsg(Client c, int msgID) {
        this.client = c;
        this.msgID = msgID;
    }

    public Client getClient() {
        return this.client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public int getMsgID() {
        return this.msgID;
    }

    public void setMsgID(int msgID) {
        this.msgID = msgID;
    }

    public String toString() {
        return "----- Node Msg " + this.msgID + " -----\n" + this.client.toString();
    }
}