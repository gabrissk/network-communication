package redes;

import java.io.Serializable;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public abstract class Message {

    //protected static long seq_num = 0;
   // protected String md5;

    public Message(){}

    /*public Message(String md5) {
       this.md5 = md5;
    }*/

    /*public String getMd5() {
        return this.md5;
    }*/

    /*public static long getSeq_num() {
        return seq_num;
    }*/

    // Retorna o hash com algoritmo MD5
    protected static String hash(String str) throws NoSuchAlgorithmException {
        if(str == null || "".equals(str)) {
            return str;
        }
        MessageDigest message = MessageDigest.getInstance("MD5");
        message.update(str.getBytes(),0,str.length());
        return new BigInteger(1,message.digest()).toString(16);
    }
}
