package redes;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.*;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import static java.lang.System.exit;
import static redes.Message.hash;

public class Server {

    private int portNumber;
    private double perror;
    private DatagramSocket socket;
    private HashMap<SocketAddress,SlidingWindow> windows;
    private HashMap<SocketAddress, TreeMap<Long, Map.Entry<String, Boolean>>> logs;
    //private TreeMap<Long, Map.Entry<String, Boolean>> logs;

    private Server(String[] args) throws SocketException {
        this.portNumber = Integer.parseInt(args[1]);
        this.perror = Double.parseDouble(args[3]);
        this.socket = new DatagramSocket(this.portNumber);
        this.windows = new HashMap<>();
        //this.logs = new TreeMap<>();
        this.logs = new HashMap<>();
    }

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException, InterruptedException {

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
    private static void receive(Server server, PrintWriter outFile, int winSize) throws IOException,
            NoSuchAlgorithmException, InterruptedException {

        byte[] recvData = new byte[16384];
        long seqNum;
        Timestamp time;
        short size;
        String m;
        String md5;

        System.out.println("Esperando por datagrama UDP na porta " + server.portNumber);

        DatagramPacket pack = new DatagramPacket(recvData,
                recvData.length);

        while(true) {

            /*** RECEBE MENSAGEM ***/

            server.socket.receive(pack);

            ByteBuffer buf = ByteBuffer.wrap(pack.getData());
            seqNum = buf.getLong();
            time = new Timestamp(buf.getLong(), buf.getInt());
            size = buf.getShort();
            byte[] aux = new byte[size];
            buf.get(aux, 0, size);
            m = new String(aux);
            aux = new byte[16];
            buf.get(aux, 0, 16);
            md5 = new String(aux);

            LogMessage msg = new LogMessage(seqNum,  time, size, m, md5);
            //System.out.println(pack.getSocketAddress());
            if(!server.windows.containsKey(pack.getSocketAddress())) {
                server.windows.put(pack.getSocketAddress(), new SlidingWindow(winSize));
                server.logs.put(pack.getSocketAddress(), new TreeMap<>());
            }


            SlidingWindow window = server.windows.get(pack.getSocketAddress());
            TreeMap<Long, Map.Entry<String, Boolean>> t = server.logs.get(pack.getSocketAddress());
            updateLogs(t, msg.getSeq_num(), msg.getMsg());

            if(window.getPacks().get(msg.getSeq_num()) == null) {
                window.insert(msg.getSeq_num());
                //window.print();
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
            t.put(msg.getSeq_num(), Map.entry(msg.getMsg(), false));

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
                /*outFile.write(msg.getMsg() + "\n");
                outFile.flush();*/
                try {
                    window.update(msg.getSeq_num());
                } catch (NullPointerException e) {
                    window.print();
                }
                tryToWrite(pack.getSocketAddress(), t, window,outFile);

            }
            send(server, ack, pack);
        }
    }

    private static void tryToWrite(SocketAddress s, TreeMap<Long, Map.Entry<String, Boolean>> t, SlidingWindow window, PrintWriter out) {
        System.out.println(t.values());
        System.out.println(t.size());
        for(Long i:t.keySet()) {
            if (!window.getPacks().get(i)) break;
            if (!t.get(i).getValue()) {
                out.write(t.get(i).getKey()+" "+s+ "\n");
                out.flush();
                t.replace(i, Map.entry(t.get(i).getKey(), true));
            }
        }
        //System.out.println(t.values());
    }

    private static void send(Server server, Ack ack, DatagramPacket pack) throws IOException {

        InetAddress addr = pack.getAddress();
        int portNum = pack.getPort();

        /*** ENVIA ACK ***/

        ByteBuffer buf = ByteBuffer.allocate(20000);
        buf.putLong(ack.getSeq_num());
        buf.putLong(ack.getTime().getSecs());
        buf.putInt(ack.getTime().getNanos());
        buf.put(ack.getMd5().getBytes());

        byte[] sendData = buf.array();

        DatagramPacket sPack = new DatagramPacket(sendData, sendData.length, addr, portNum);
        server.socket.send(sPack);

        System.out.print("Enviando ack do pacote "+ack.getSeq_num());
        if(ack.isErr()) System.out.println(" (com erro)");
        else System.out.println();
    }

    static void updateLogs(TreeMap<Long, Map.Entry<String, Boolean>> t, long seqNum, String msg) {
        for(int i =0; i< (int) seqNum; i++) {
            if(!t.containsKey((long)i))
                t.put((long)i, Map.entry("", false));
        }
        t.put(seqNum, Map.entry(msg, false));
    }
}
