package redes;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.Timer;

public class LogMessage extends Message implements Serializable {

    private long seq_num;
    private Timestamp time;
    private short size;
    private String msg;
    private String md5;
    transient Timer timer; // transiente para poder serializar LogMessage
    private boolean err;
    private byte[] nMd5;

    public LogMessage(long seq_num, Timestamp time, short size, String msg/*, String md5*/) throws NoSuchAlgorithmException {
        this.seq_num = seq_num; this.time = time; this.msg = msg; this.size = size;// this.md5 = md5;
        this.timer = new Timer(); this.err = false;
        this.nMd5 = generateHash();
    }

    public long getSeq_num() { return seq_num; }

    public Timestamp getTime() { return time; }

    public short getSize() {
        return size;
    }

    public String getMsg() {
        return msg;
    }

    public boolean isErr() {
        return err;
    }

    @Override
    public String toString() {
        return "seqNum: " +seq_num+ "\tTime: " +time+
                "\tsize: " +size+ "\tmessage: " +msg+ "\tmd5: " +md5;
    }

    static LogMessage setAndGetMessage(String nextLine, long seq_num, double perror, Timestamp time) throws NoSuchAlgorithmException {
        String md5 = hash(String.valueOf(seq_num) + time.toString()
                + String.valueOf((short)nextLine.length()) + nextLine);
        LogMessage msg = new LogMessage(seq_num, time, (short)nextLine.length(), nextLine);
        double rdm = Math.random();
        if(rdm < perror) {
            msg.setMd5(hash(md5));
            msg.nMd5[2] += 1;
            msg.setErr(true);
        }
        return msg;
    }

    private byte[] generateHash() throws NoSuchAlgorithmException {
        ByteBuffer buf = ByteBuffer.allocate(22 + msg.getBytes().length);
        buf.putLong(seq_num);
        buf.putLong(time.getSecs());
        buf.putInt(time.getNanos());
        buf.putShort(size);
        buf.put(msg.getBytes());

        return hash(buf.array());
    }

    public byte[] getnMd5() {
        return nMd5;
    }

    private void setMd5(String hash) {
        this.md5 = hash;
    }

    private void setErr(boolean err) {
        this.err = err;
    }
}
