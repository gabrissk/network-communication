package redes;

import java.io.Serializable;

public class Ack extends Message implements Serializable {

    private long seq_num;
    private Timestamp time;
    private String md5;
    private  boolean err;

    public Ack(long seq_num, Timestamp time, String md5) {
        //super(md5);
        this.seq_num = seq_num; this.time = time; this.md5 = md5; this.err = false;
    }

    public long getSeq_num() { return seq_num; }

    public Timestamp getTime() { return time; }

    public String getMd5() { return this.md5; }

    public void setMd5(String hash) {
        this.md5 = hash;
    }

    public boolean isErr() {
        return err;
    }

    @Override
    public String toString() {
        return "seqNum: " +seq_num+ "\tTime: " +time.getSecs()+ ":" +time.getNanos()+
                "\tmd5: " +md5;
    }

    public void setErr(boolean err) {
        this.err = err;
    }
}
