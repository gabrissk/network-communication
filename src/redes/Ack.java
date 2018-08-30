package redes;

import java.io.Serializable;

public class Ack extends Message implements Serializable {

    private long seq_num;
    private Timestamp time;
    private String md5;

    public Ack(long seq_num, Timestamp time, String md5) {
        //super(md5);
        this.seq_num = seq_num; this.time = time; this.md5 = md5;
    }

    public long getSeq_num() { return seq_num; }

    public Timestamp getTime() { return time; }

    public String getMd5() { return this.md5; }

    @Override
    public String toString() {
        return "seqNum: " +seq_num+ "\tTime: " +time.getSecs()+ ":" +time.getNanos()+
                "\tmd5: " +md5;
    }
}
