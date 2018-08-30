package redes;

import javax.xml.crypto.Data;
import java.io.*;
import java.net.*;
import java.security.NoSuchAlgorithmException;

public class Server {
    public static void main(String[] args) throws IOException, NoSuchAlgorithmException, ClassNotFoundException {

        if (args.length < 3) {
            System.err.println("Usage: <file> <port number> <Perror>");
            System.exit(1);
        }


        BufferedWriter outFile = null;
        try {
            outFile = new BufferedWriter(new FileWriter(args[0], false));
        }
        catch (IOException e)
        {
            System.err.println("Error: " + e.getMessage());
        }

        int portNumber = Integer.parseInt(args[1]);
        final double perror = Double.parseDouble(args[2]);

        // UDP
        DatagramSocket socket = new DatagramSocket(portNumber);
        byte[] recvData = new byte[1024];
        byte[] sendData = new byte[1024];

        long seq_num;
        Timestamp time;
        String m;
        short size;
        String md5;

        DatagramPacket pack = new DatagramPacket(recvData,
                recvData.length);
        System.out.println("Esperando por datagrama UDP na porta " + portNumber);

        while(true) {
            socket.receive(pack);
            ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(recvData));
            seq_num = (Long) in.readObject();
            System.out.println(seq_num);

            socket.receive(pack);
            time = (Timestamp) in.readObject();
            System.out.println(time);

            socket.receive(pack);
            m = (String) in.readObject();
            System.out.println(m);

            socket.receive(pack);
            size = (short)in.readObject();
            System.out.println(size);

            socket.receive(pack);
            md5 = (String) in.readObject();
            System.out.println(md5);

            LogMessage msg = new LogMessage(seq_num, time, size, m, md5);
            System.out.println(msg.toString()+"\n\n");

            // Faz a verificacao de erro
            if(!(Client.hash(String.valueOf(msg.getSeq_num()) + String.valueOf(msg.getTime().getSecs()) +
                    String.valueOf(msg.getTime().getNanos()) + String.valueOf((msg.getSize())) + msg.getMsg()).equals(msg.getMd5()))) {
                System.out.println("Falha na verificacao! Descartar mensagem");
            }

            // Escreve mensagem no arquivo de saida
            outFile.write(msg.getMsg()+"\n");
            outFile.flush();

            InetAddress addr = pack.getAddress();
            int portNum = pack.getPort();
            //DatagramPacket sPack = new DatagramPacket(recvData, recvData.length, addr, portNum);

            ByteArrayOutputStream bStream = new ByteArrayOutputStream();
            ObjectOutput out = new ObjectOutputStream(bStream);

            out.writeObject(seq_num);
            sendData = bStream.toByteArray();
            DatagramPacket sPack = new DatagramPacket(sendData, sendData.length, addr, portNum);
            socket.send(sPack);


            out.writeObject(time);
            sendData = bStream.toByteArray();
            sPack = new DatagramPacket(sendData, sendData.length, addr, portNum);
            socket.send(sPack);


            out.writeObject(md5);
            sendData = bStream.toByteArray();
            sPack = new DatagramPacket(sendData, sendData.length, addr, portNum);
            socket.send(sPack);
        }
        /*try (
                ServerSocket serverSocket = new ServerSocket(portNumber);
                Socket clientSocket = serverSocket.accept();
                PrintWriter out =
                        new PrintWriter(clientSocket.getOutputStream(), true);
                *//*BufferedReader in = new BufferedReader(
                        new InputStreamReader(clientSocket.getInputStream()));*//*
                ObjectInputStream inputStream = new ObjectInputStream(clientSocket.getInputStream());
        ) {

            String inputLine, outputLine;

            // Pega as log messages do cliente
            while(!clientSocket.isClosed()) {
                LogMessage m = (LogMessage) inputStream.readObject();
                System.out.println("Size: " + m.getSize() + "\tMessage: " + m.getMsg() +
                        "\tHash:" + m.getMd5() +"\tCount: " +m.getSeq_num());
                // Faz a verificacao de erro
                if(!(Client.hash(m.getSize()+ m.getMsg()).equals(m.getMd5()))) {
                    System.out.println("Falha na verificacao! Descartar mensagem");
                }
                else {
                    // Escreve mensagem no arquivo de saida
                    outFile.write(m.getMsg()+"\n");
                    outFile.flush();
                }
            }
        } catch (EOFException e) {
            System.out.println("Todas as mensagens recebidas");
        }
        catch (IOException e) {
            System.out.println("Exceção ao dar listen na porta "
                    + portNumber);
            System.out.println(e.getMessage());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }*/

    }
}
