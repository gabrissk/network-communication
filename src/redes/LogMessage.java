package redes;

import java.io.Serializable;

public class LogMessage extends Message implements Serializable {

    private String msg;
    private short size;
    private String md5;

    public LogMessage(short size, String msg, String md5) {
        //super(md5);
        this.msg = msg; this.size = size; this.md5 = md5;
    }

    public String getMsg() {
        return msg;
    }

    public short getSize() {
        return size;
    }

    public String getMd5() {
        return this.md5;
    }
}
