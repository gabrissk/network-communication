package redes;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.security.NoSuchAlgorithmException;

import static java.lang.System.exit;
import static redes.Message.hash;

public class Server {

    private int portNumber;
    private double perror;
    private DatagramSocket socket;

    public Server(String[] args) throws SocketException {
        this.portNumber = Integer.parseInt(args[1]);
        this.perror = Double.parseDouble(args[2]);
        this.socket = new DatagramSocket(portNumber);
    }

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException, ClassNotFoundException {

        if (args.length < 3) {
            System.err.println("Usage: <file> <port number> <Perror>");
            exit(1);
        }


        PrintWriter outFile = null;
        try {
            outFile = new PrintWriter(new File(args[0]));
        }
        catch (IOException e)
        {
            System.err.println("Error: " + e.getMessage());
        }

        Server server = new Server(args);

        receive(server, outFile);

    }

    public static void receive(Server server, PrintWriter outFile) throws IOException, ClassNotFoundException, NoSuchAlgorithmException {
        byte[] recvData = new byte[16384];
        //byte[] sendData = new byte[1024];

        long seq_num;
        Timestamp time;
        String m;
        short size;
        String md5;

        DatagramPacket pack = new DatagramPacket(recvData,
                recvData.length);
        System.out.println("Esperando por datagrama UDP na porta " + server.portNumber);

        while(true) {

            /*** RECEBE MENSAGEM ***/

            server.socket.receive(pack);
            ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(recvData));
            seq_num = (Long) in.readObject();

            server.socket.receive(pack);
            time = (Timestamp) in.readObject();

            server.socket.receive(pack);
            m = (String) in.readObject();

            server.socket.receive(pack);
            size = (short) in.readObject();

            server.socket.receive(pack);
            md5 = (String) in.readObject();

            LogMessage msg = new LogMessage(seq_num, time, size, m, md5);
            //System.out.println(msg.toString()+"\n");


            // Faz a verificacao de erro
            if (!(Message.checkMd5(String.valueOf(msg.getSeq_num()) + time.toString()
                    + String.valueOf((msg.getSize()) + msg.getMsg()), msg.getMd5()))) {
                System.out.println("Falha na verificacao da mensagem " + msg.getSeq_num() +
                        ". Descartando mensagem...");
                continue;
            }

            System.out.println("Pacote " + msg.getSeq_num() + " recebido no servidor com mensagem "+msg.getMsg());
            // Escreve mensagem no arquivo de saida
            outFile.write(msg.getMsg() + "\n");
            outFile.flush();


            /*** ENVIA ACK ***/

            md5 = hash(String.valueOf(seq_num) + time.toString());
            Ack ack = new Ack(seq_num, time, md5);
            double rdm = Math.random();
            if (rdm < server.perror) {
                ack.setMd5(hash(md5));
                ack.setErr(true);
            }
            send(server, ack, pack);
        }

    }

    public static void send(Server server, Ack ack, DatagramPacket pack) throws IOException {

        InetAddress addr = pack.getAddress();
        int portNum = pack.getPort();
        byte[] sendData;

        ByteArrayOutputStream bStream = new ByteArrayOutputStream();
        ObjectOutput out = new ObjectOutputStream(bStream);

        out.writeObject(ack.getSeq_num());
        sendData = bStream.toByteArray();
        DatagramPacket sPack = new DatagramPacket(sendData, sendData.length, addr, portNum);
        server.socket.send(sPack);


        out.writeObject(ack.getTime());
        sendData = bStream.toByteArray();
        sPack = new DatagramPacket(sendData, sendData.length, addr, portNum);
        server.socket.send(sPack);


        out.writeObject(ack.getMd5());
        sendData = bStream.toByteArray();
        sPack = new DatagramPacket(sendData, sendData.length, addr, portNum);
        server.socket.send(sPack);
        System.out.print("Enviando ack do pacote "+ack.getSeq_num());
        if(ack.isErr()) System.out.println(" (com erro)");
        else System.out.println();
    }
}
