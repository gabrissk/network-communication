package redes;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    public static void main(String[] args) throws IOException {

        if (args.length < 1) {
            System.err.println("Usage: <port number>");
            System.exit(1);
        }

        int portNumber = Integer.parseInt(args[0]);

        try (
                ServerSocket serverSocket = new ServerSocket(portNumber);
                Socket clientSocket = serverSocket.accept();
                PrintWriter out =
                        new PrintWriter(clientSocket.getOutputStream(), true);
                /*BufferedReader in = new BufferedReader(
                        new InputStreamReader(clientSocket.getInputStream()));*/
                ObjectInputStream inputStream = new ObjectInputStream(clientSocket.getInputStream());
        ) {

            String inputLine, outputLine;


            while(!clientSocket.isClosed()) {
                LogMessage m = (LogMessage) inputStream.readObject();
                System.out.println("Size: " + m.getSize() + "\tMessage: " + m.getMsg() +
                        "\tHash:" + m.getMd5() +"\tCount: " +m.getSeq_num());
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
        }
    }
}
