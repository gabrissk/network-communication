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

    @SuppressWarnings("unchecked")
    private static void receive(Server server, PrintWriter outFile, int winSize) throws IOException, ClassNotFoundException,
            NoSuchAlgorithmException, InterruptedException {

        byte[] recvData = new byte[16384];
        String md5;

        System.out.println("Esperando por datagrama UDP na porta " + server.portNumber);

        DatagramPacket pack = new DatagramPacket(recvData,
                recvData.length);

        while(true) {

            /*** RECEBE MENSAGEM ***/

            server.socket.receive(pack);
            ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(recvData));
            /*HashMap<Integer, Object> l = (HashMap<Integer, Object>)in.readObject();

            LogMessage msg = new LogMessage((long)l.get(0), new Timestamp((long)l.get(1),(int)l.get(2)),
                    (short)l.get(3), (String)l.get(4), (String)l.get(5));*/
            LogMessage msg = (LogMessage) in.readObject();

            if(!server.windows.containsKey(pack.getSocketAddress())) {
                server.windows.put(pack.getSocketAddress(), new SlidingWindow(winSize));
            }

            SlidingWindow window = server.windows.get(pack.getSocketAddress());

            if(window.getPacks().get(msg.getSeq_num()) == null) {
                window.insert(msg.getSeq_num());
                window.print();
            }

            // Verifica se o pacote pode ser confirmado; caso contrario, o ignora
            if(!window.insideWindow(msg.getSeq_num())){
                continue;
            }

            // Faz a verificacao de erro
            if (!(Message.checkMd5(String.valueOf(msg.getSeq_num()) + msg.getTime().toString()
                    + String.valueOf((msg.getSize()) + msg.getMsg()), msg.getMd5()))) {
                System.out.println("Falha na verificacao da mensagem " + msg.getSeq_num() +
                        ". Descartando mensagem...");
                continue;
            }

            System.out.println("Pacote " + msg.getSeq_num() + " recebido no servidor com mensagem "+msg.getMsg());

            //String md5;
            md5 = hash(String.valueOf(msg.getSeq_num() + msg.getTime().toString()));
            Ack ack = new Ack(msg.getSeq_num(), msg.getTime(), md5);
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
                    window.update(msg.getSeq_num());
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

        /*HashMap<Integer, Object> list = new HashMap<>();
        list.put(0, ack.getSeq_num());
        list.put(1, ack.getTime().getSecs());
        list.put(2, ack.getTime().getNanos());
        list.put(3, ack.getMd5());
        out.writeObject(list);*/
        out.writeObject(ack);
        sendData = bStream.toByteArray();
        DatagramPacket sPack = new DatagramPacket(sendData, sendData.length, addr, portNum);
        server.socket.send(sPack);

        System.out.print("Enviando ack do pacote "+ack.getSeq_num());
        if(ack.isErr()) System.out.println(" (com erro)");
        else System.out.println();
    }
}
