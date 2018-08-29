package redes;

import java.io.Serializable;

public class LogMessage extends Message implements Serializable {

    private long seq_num;
    private Timestamp time;
    private String msg;
    private short size;
    private String md5;

    public LogMessage(long seq_num, Timestamp time, short size, String msg, String md5) {
        //super(md5);
        this.seq_num = seq_num; this.time = time; this.msg = msg; this.size = size; this.md5 = md5;
    }

    public long getSeq_num() { return seq_num; }

    public Timestamp getTime() { return time; }

    public String getMsg() {
        return msg;
    }

    public short getSize() {
        return size;
    }

    public String getMd5() { return this.md5; }

    @Override
    public String toString() {
        return "seqNum: " +seq_num+ "\tTime: " +time.getSecs()+ ":" +time.getNanos()+ "\tMensagem: " +msg+ "\tsize: " +size+ "\tmd5: " +md5+ "\tseqNum: " +seq_num;
    }
}
