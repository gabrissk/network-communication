package redes;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;

public class Ack extends Message implements Serializable {

    private long seq_num;
    private Timestamp time;
    private String md5;
    private  boolean err;
    byte[] nMd5;

    public Ack(long seq_num, Timestamp time/*, String md5*/) throws NoSuchAlgorithmException {
        this.seq_num = seq_num; this.time = time; //this.md5 = md5;
        this.err = false;
        this.nMd5 = generateHash();
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

    private byte[] generateHash() throws NoSuchAlgorithmException {
        ByteBuffer buf = ByteBuffer.allocate(20);
        buf.putLong(seq_num);
        buf.putLong(time.getSecs());
        buf.putInt(time.getNanos());

        return hash(buf.array());
    }

    @Override
    public String toString() {
        return "seqNum: " +seq_num+ "\tTime: " +time.getSecs()+ ":" +time.getNanos()+
                "\tmd5: " +md5;
    }

    public void setErr(boolean err) {
        this.err = err;
    }

    public byte[] getnMd5() {
        return this.nMd5;
    }
}
