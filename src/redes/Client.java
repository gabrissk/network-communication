package redes;

import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Scanner;
import java.util.regex.Pattern;

public class Client {
    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {

        if (args.length < 4) {
            System.err.println(
                    "Usage:  <file> <IP:port number> <Perror>");
            System.exit(1);
        }


        final String[] ip_port = args[1].split(Pattern.quote(":"));
        InetAddress addr = InetAddress.getByName(ip_port[0]);
        int portNumber = Integer.parseInt(ip_port[1]);
        int tout = Integer.parseInt(args[2]);
        final double perror = Double.parseDouble(args[3]);


        // UDP

        DatagramSocket socket = new DatagramSocket();

        /*try (
                Socket socket = new Socket(addr, portNumber);
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
        ) {*/

        long seq_num = 0;
        Scanner scanner = new Scanner(new File(args[0]));
        while (scanner.hasNextLine()) {
            long secs = Instant.now().getEpochSecond();
            long start = System.nanoTime();
            ByteArrayOutputStream bStream = new ByteArrayOutputStream();
            ObjectOutput out = new ObjectOutputStream(bStream);
            Timestamp time = new Timestamp(secs, Math.toIntExact(System.nanoTime()-start));
            LogMessage msg = setAndGetMessage(scanner.nextLine(), seq_num, perror, time);
            out.writeObject(msg);
            byte[] send_serial = bStream.toByteArray();
            DatagramPacket sendPacket = new DatagramPacket(send_serial,
                    send_serial.length, addr, portNumber);
            socket.send(sendPacket);
            seq_num++;
        }
        socket.close();
        /*} catch (UnknownHostException e) {
            System.err.println("Host desconhecido " + args[0]);
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Nao conseguiu conectar a " +
                    args[0]);
            System.exit(1);
        }*/
    }

    private static LogMessage setAndGetMessage(String nextLine, long seq_num, double perror, Timestamp time) throws NoSuchAlgorithmException {
        String md5 = hash(String.valueOf(seq_num) + String.valueOf(time.getSecs()) +
                        String.valueOf(time.getNanos()) + String.valueOf((short)nextLine.length()) + nextLine);
        double rdm = Math.random();
        if(rdm < perror) {
            System.out.println("erro "+nextLine);
            md5 = hash(md5);
        }
        return new LogMessage(seq_num, time, (short)nextLine.length(), nextLine, md5);
    }


    // Retorna o hash com algoritmo MD5
    public static String hash(String str) throws NoSuchAlgorithmException {
        if(str == null || "".equals(str)) {
            return str;
        }
        MessageDigest message = MessageDigest.getInstance("MD5");
        message.update(str.getBytes(),0,str.length());
        return new BigInteger(1,message.digest()).toString(16);
    }


}

