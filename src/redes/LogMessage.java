package redes;

import java.io.Serializable;
import java.security.NoSuchAlgorithmException;
import java.util.Timer;

public class LogMessage extends Message implements Serializable {

    private long seq_num;
    private Timestamp time;
    private short size;
    private String msg;
    private String md5;
    Timer timer;
    private boolean err;

    public LogMessage(long seq_num, Timestamp time, short size, String msg, String md5) {
        //super(md5);
        this.seq_num = seq_num; this.time = time; this.msg = msg; this.size = size; this.md5 = md5;
        this.timer = new Timer(); this.err = false;
    }

    public long getSeq_num() { return seq_num; }

    public Timestamp getTime() { return time; }

    public short getSize() {
        return size;
    }

    public String getMsg() {
        return msg;
    }

    public String getMd5() { return this.md5; }

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
        LogMessage msg = new LogMessage(seq_num, time, (short)nextLine.length(), nextLine, md5);
        double rdm = Math.random();
        if(rdm < perror) {
            msg.setMd5(hash(md5));
            msg.setErr(true);
        }
        return msg;
    }

    private void setMd5(String hash) {
        this.md5 = hash;
    }

    public void setErr(boolean err) {
        this.err = err;
    }
}
