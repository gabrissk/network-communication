package redes;

import java.io.Serializable;

public class LogMessage extends Message implements Serializable {

    private String msg;
    private short size;
    private String md5;
    private long seq_num;

    public LogMessage(short size, String msg, String md5, long seq_num) {
        //super(md5);
        this.msg = msg; this.size = size; this.md5 = md5; this.seq_num = seq_num;
    }

    public String getMsg() {
        return msg;
    }

    public short getSize() {
        return size;
    }

    public String getMd5() { return this.md5; }

    public long getSeq_num() { return seq_num; }

    @Override
    public String toString() {
        return "Mensagem: " +msg+ "\tsize: " +size+ "\tmd5: " +md5+ "\tseqNum: " +seq_num;
    }
}
