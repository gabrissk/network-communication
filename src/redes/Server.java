package redes;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;

public class Server {
    public static void main(String[] args) throws IOException, NoSuchAlgorithmException, ClassNotFoundException {

        if (args.length < 3) {
            System.err.println("Usage: <file> <port number> <Perror>");
            System.exit(1);
        }

        final double perror = Double.parseDouble(args[2]);

        BufferedWriter outFile = null;
        try {
            outFile = new BufferedWriter(new FileWriter(args[0], false));
        }
        catch (IOException e)
        {
            System.err.println("Error: " + e.getMessage());
        }

        int portNumber = Integer.parseInt(args[1]);

        // UDP
        DatagramSocket socket = new DatagramSocket(portNumber);
        byte[] data = new byte[1024];
        byte[] sendData = new byte[1024];

        DatagramPacket recvData = new DatagramPacket(data,
                data.length);
        System.out.println("Esperando por datagrama UDP na porta " + portNumber);

        while(true) {
            socket.receive(recvData);
            ObjectInputStream iStream = new ObjectInputStream(new ByteArrayInputStream(data));
            LogMessage msg = (LogMessage) iStream.readObject();
            System.out.println(msg.toString());

            // Faz a verificacao de erro
            if(!(Client.hash(String.valueOf(msg.getSeq_num()) + String.valueOf(msg.getTime().getSecs()) +
                    String.valueOf(msg.getTime().getNanos()) + String.valueOf((msg.getSize())) + msg.getMsg()).equals(msg.getMd5()))) {
                System.out.println("Falha na verificacao! Descartar mensagem");
            }
            else {
                // Escreve mensagem no arquivo de saida
                outFile.write(msg.getMsg()+"\n");
                outFile.flush();
            }
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
