package Common;

import java.util.List;

public class NeighborsReply {
    private List<Client> neighbors;

    public NeighborsReply(List<Client> n){
        this.neighbors = n;
    }

    public List<Client> getNeighbors() {
        return neighbors;
    }

    public String toString(){
        StringBuilder ss = new StringBuilder();

        ss.append("----- Neighbors Reply -----").append("\n");
        for(Client c: this.neighbors){
            ss.append(c.toString());
        }

        return ss.toString();
    }
}
