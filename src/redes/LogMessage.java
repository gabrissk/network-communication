package redes;

import java.io.Serializable;
import java.security.NoSuchAlgorithmException;

public class LogMessage extends Message implements Serializable {

    private long seq_num;
    private Timestamp time;
    private short size;
    private String msg;
    private String md5;

    public LogMessage(long seq_num, Timestamp time, short size, String msg, String md5) {
        //super(md5);
        this.seq_num = seq_num; this.time = time; this.msg = msg; this.size = size; this.md5 = md5;
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

    @Override
    public String toString() {
        return "seqNum: " +seq_num+ "\tTime: " +time+
                "\tsize: " +size+ "\tmessage: " +msg+ "\tmd5: " +md5;
    }

    static LogMessage setAndGetMessage(String nextLine, long seq_num, double perror, Timestamp time) throws NoSuchAlgorithmException {
        String md5 = hash(String.valueOf(seq_num) + time.toString()
                + String.valueOf((short)nextLine.length()) + nextLine);
        double rdm = Math.random();
        if(rdm < perror) {
            System.out.println("erro "+nextLine);
            md5 = hash(md5);
        }
        return new LogMessage(seq_num, time, (short)nextLine.length(), nextLine, md5);
    }

}
