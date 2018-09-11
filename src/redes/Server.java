package redes;

import java.io.*;
import java.net.*;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

import static java.lang.System.exit;
import static redes.Message.hash;

public class Server {

    private int portNumber;
    private double perror;
    private DatagramSocket socket;
    private HashMap<SocketAddress,SlidingWindow> windows;

    private Server(String[] args) throws SocketException {
        this.portNumber = Integer.parseInt(args[1]);
        this.perror = Double.parseDouble(args[3]);
        this.socket = new DatagramSocket(this.portNumber);
        this.windows = new HashMap<>();
    }

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException, ClassNotFoundException, InterruptedException {

        if (args.length < 4) {
            System.err.println("Uso: <arquivo> <port> <Wrx> <Perror>");
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

        receive(server, outFile, Integer.parseInt(args[2]));

    }

    private static void receive(Server server, PrintWriter outFile, int winSize) throws IOException, ClassNotFoundException,
            NoSuchAlgorithmException, InterruptedException {
        byte[] recvData = new byte[16384];

        long seq_num;
        Timestamp time;
        String m;
        short size;
        String md5;

        System.out.println("Esperando por datagrama UDP na porta " + server.portNumber);

        DatagramPacket pack = new DatagramPacket(recvData,
                recvData.length);

        while(true) {

            /*** RECEBE MENSAGEM ***/
            Thread.sleep(10);
            server.socket.receive(pack);
            ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(recvData));
            seq_num = (Long) in.readObject();

            Thread.sleep(10);
            server.socket.receive(pack);
            time = (Timestamp) in.readObject();

            Thread.sleep(10);
            server.socket.receive(pack);
            m = (String) in.readObject();

            Thread.sleep(10);
            server.socket.receive(pack);
            size = (short) in.readObject();

            Thread.sleep(10);
            server.socket.receive(pack);
            md5 = (String) in.readObject();

            LogMessage msg = new LogMessage(seq_num, time, size, m, md5);

            if(!server.windows.containsKey(pack.getSocketAddress())) {
                server.windows.put(pack.getSocketAddress(), new SlidingWindow(winSize));
            }

            SlidingWindow window = server.windows.get(pack.getSocketAddress());

            if(window.getPacks().get(seq_num) == null) {
                window.insert(seq_num);
                window.print();
            }

            // Verifica se o pacote pode ser confirmado; caso contrario, o ignora
            if(!window.insideWindow(seq_num)){
                System.out.println(seq_num);
                window.print();
                continue;
            }

            // Faz a verificacao de erro
            if (!(Message.checkMd5(String.valueOf(msg.getSeq_num()) + time.toString()
                    + String.valueOf((msg.getSize()) + msg.getMsg()), msg.getMd5()))) {
                System.out.println("Falha na verificacao da mensagem " + msg.getSeq_num() +
                        ". Descartando mensagem...");
                continue;
            }

            System.out.println("Pacote " + msg.getSeq_num() + " recebido no servidor com mensagem "+msg.getMsg());

            md5 = hash(String.valueOf(seq_num) + time.toString());
            Ack ack = new Ack(seq_num, time, md5);
            double rdm = Math.random();
            if (rdm < server.perror) {
                ack.setMd5(hash(md5));
                ack.setErr(true);
            }
            else {
                // Escreve mensagem no arquivo de saida
                outFile.write(msg.getMsg() + "\n");
                outFile.flush();
                try {
                    window.update(seq_num);
                } catch (NullPointerException e) {
                    window.print();
                }
            }
            send(server, ack, pack);
        }

    }

    private static void send(Server server, Ack ack, DatagramPacket pack) throws IOException {

        InetAddress addr = pack.getAddress();
        int portNum = pack.getPort();
        byte[] sendData;

        /*** ENVIA ACK ***/
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
