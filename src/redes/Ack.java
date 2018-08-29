package redes;

import java.io.Serializable;

public class Ack extends Message implements Serializable {

    private String md5;

    public Ack (String md5) {
        //super(md5);
        this.md5 = md5;
    }
}
