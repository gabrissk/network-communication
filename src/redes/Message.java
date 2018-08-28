package redes;

import java.io.Serializable;

public class Message implements Serializable {
    private int size;
    private String msg;
    private String md5;

    public Message(int size, String msg, String md5) {
        this.msg = msg; this.size = size;   this.md5 = md5;
    }

    public int getSize() {
        return size;
    }

    public String getMsg() {
        return msg;
    }

    public String getMd5() {
        return md5;
    }
}
