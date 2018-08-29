package redes;

import java.io.Serializable;

public class Message implements Serializable {
    public static long seq_num ;
    private short size;
    private String msg;
    private String md5;

    public Message(short size, String msg, String md5) {
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

    public static long getSeq_num() {
        return seq_num++;
    }
}
