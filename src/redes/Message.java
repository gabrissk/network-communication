package redes;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public abstract class Message {

    //protected static long seq_num = 0;
   // protected String md5;

    public Message(){}


    // Retorna o hash com algoritmo MD5
    protected static String hash(String str) throws NoSuchAlgorithmException {
        if(str == null || "".equals(str)) {
            return str;
        }
        MessageDigest message = MessageDigest.getInstance("MD5");
        message.update(str.getBytes(),0,str.length());
        return new BigInteger(1,message.digest()).toString(16).substring(0,16);
    }

    protected static byte[] hash(byte[] bytes) throws NoSuchAlgorithmException {
        return MessageDigest.getInstance("MD5").digest(bytes);
    }

    protected static boolean checkMd5(String a, String b) throws NoSuchAlgorithmException {
        return hash(a).equals(b);
    }

    protected static boolean checkMd5(byte[] a, byte[] b) throws NoSuchAlgorithmException {
        return Arrays.equals(a, b);
    }


}
