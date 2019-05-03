package Common;

import io.atomix.utils.net.Address;

public class Client {
    private String username;
    private String key;
    private int port; //todo (duvida): porquê separar a porta do address ??? porquê não usar o IP mesmo (InetAddress)?
    private Address address;

    public Client(String u, String k, int p, Address a){
        this.username = u;
        this.key = k;
        this.port = p;
        this.address = a;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getKey() {
        return key;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public String toString(){
        StringBuilder ss = new StringBuilder();

        ss.append("----- Node -----").append("\n");
        ss.append("Username: ").append(this.username).append("\n");
        ss.append("Public key: ").append(this.key).append("\n");
        ss.append("Port: ").append(this.port).append("\n");
        ss.append("Address: ").append(this.address).append("\n");

        return ss.toString();
    }
}
