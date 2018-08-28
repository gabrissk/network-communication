package redes;

import java.io.*;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Client {
    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {

        if (args.length < 2) {
            System.err.println(
                    "Usage: java EchoClient <host name> <port number>");
            System.exit(1);
        }


        InetAddress addr = InetAddress.getByName(args[0]);
        int portNumber = Integer.parseInt(args[1]);
        String str = args[2];
        String md5 = args[0] + args[1] + args[2];
        md5 = hash(md5);

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
            Message m = new Message(str.length(), str, md5);
            hash(md5);

            out.writeObject(m);

            /*while ((fromServer = in.readLine()) != null) {
                System.out.println("Server: " + fromServer);
                if (fromServer.equals("Bye."))
                    break;

                fromUser = stdIn.readLine();
                if (fromUser != null) {
                    System.out.println("Client: " + fromUser);
                    out.println(fromUser);
                }
            }*/
        } catch (UnknownHostException e) {
            System.err.println("Don't know about host " + args[0]);
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to " +
                    args[0]);
            System.exit(1);
        }
    }


    // Retorna o hash
    public static String hash(String a) throws NoSuchAlgorithmException {
        if(a == null || "".equals(a)) {
            return a;
        }
        MessageDigest message = MessageDigest.getInstance("MD5");
        message.update(a.getBytes(),0,a.length());
        return new BigInteger(1,message.digest()).toString(16);
    }


}

