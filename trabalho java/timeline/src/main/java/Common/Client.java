package Common;

import io.atomix.utils.net.Address;

public class Client {
    private String username;
    private String key;
    private int port;
    private Address address;

    public Client(String u, String k, int p, Address a){
        this.username = u;
        this.key = k;
        this.port = p;
        this.address = a;
    }

    public String getUsername() {
        return this.username;
    }

    public String getKey() {
        return this.key;
    }

    public int getPort() {
        return this.port;
    }

    public Address getAddress(){
        return this.address;
    }

    public String toString(){
        StringBuilder ss = new StringBuilder();

        ss.append("----- Neighbor -----").append("\n");
        ss.append("Username: ").append(this.username).append("\n");
        ss.append("Public key: ").append(this.key).append("\n");
        ss.append("Port: ").append(this.port).append("\n");
        ss.append("Address: ").append(this.address).append("\n");

        return ss.toString();
    }
}
