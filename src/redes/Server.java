package redes;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    public static void main(String[] args) throws IOException {

        if (args.length < 1) {
            System.err.println("Usage: java  <port number>");
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

            // Initiate conversation with client

            /*while ((inputLine = in.readLine()) != null) {
                out.println("eviado de volta:" +inputLine);
                System.out.println("recebido:"+inputLine);
                if (inputLine.equals("Bye."))
                    break;
            }*/

            Message m = (Message)inputStream.readObject();
            System.out.println("Size: " +m.getSize()+"\nMessage: "+m.getMsg() +"\nHash:"+m.getMd5());
        } catch (IOException e) {
            System.out.println("Exception caught when trying to listen on port "
                    + portNumber + " or listening for a connection");
            System.out.println(e.getMessage());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
