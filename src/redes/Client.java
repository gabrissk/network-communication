package redes;

import java.io.*;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;
import java.util.regex.Pattern;

public class Client {
    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {

        if (args.length < 2) {
            System.err.println(
                    "Usage:   <ip> <port number> <message>");
            System.exit(1);
        }


        final String[] ip_port = args[1].split(Pattern.quote(":"));
        InetAddress addr = InetAddress.getByName(ip_port[0]);
        for (String s : ip_port) {
            System.out.println(s);
        }

        int portNumber = Integer.parseInt(ip_port[1]);

        try (
                Socket socket = new Socket(addr, portNumber);
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
        ) {
            BufferedReader stdIn =
                    new BufferedReader(new InputStreamReader(System.in));
            String fromServer;
            String fromUser;

            Scanner scanner = new Scanner(new File(args[0]));
            while (scanner.hasNextLine()) {
                Message msg = setMessage(scanner.nextLine());
                //System.out.println(scanner.nextLine());
                out.writeObject(msg);
            }

        } catch (UnknownHostException e) {
            System.err.println("Don't know about host " + args[0]);
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to " +
                    args[0]);
            System.exit(1);
        }
    }

    private static Message setMessage(String nextLine) throws NoSuchAlgorithmException {
        String md5 = hash(nextLine + String.valueOf(nextLine.length()));
        return new Message((short)nextLine.length(), nextLine, md5);
    }


    // Retorna o hash
    public static String hash(String str) throws NoSuchAlgorithmException {
        if(str == null || "".equals(str)) {
            return str;
        }
        MessageDigest message = MessageDigest.getInstance("MD5");
        message.update(str.getBytes(),0,str.length());
        return new BigInteger(1,message.digest()).toString(16);
    }


}

