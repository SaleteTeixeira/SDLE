package Common;

import java.io.Serializable;

public class Address implements Serializable {
    private String host;
    private int port;

    public Address(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public io.atomix.utils.net.Address get(){
        return io.atomix.utils.net.Address.from(host, port);
    }
}
