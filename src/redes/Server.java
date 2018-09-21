package redes;

import javafx.util.Pair;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.*;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.TreeMap;

import static java.lang.System.exit;
import static redes.Message.checkMd5;
import static redes.Message.hash;

public class Server {

    private int portNumber;
    private double perror;
    private DatagramSocket socket;
    private HashMap<SocketAddress,SlidingWindow> windows;
    private HashMap<SocketAddress, TreeMap<Long, Pair<String, Boolean>>> logs;

    private Server(String[] args) throws SocketException {
        this.portNumber = Integer.parseInt(args[1]);
        this.perror = Double.parseDouble(args[3]);
        this.socket = new DatagramSocket(this.portNumber);
        this.windows = new HashMap<>();
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

            LogMessage msg = new LogMessage(seqNum,  time, size, m);//, md5);

            if(!server.windows.containsKey(pack.getSocketAddress())) {
                server.windows.put(pack.getSocketAddress(), new SlidingWindow(winSize));
                server.logs.put(pack.getSocketAddress(), new TreeMap<>());
            }


            SlidingWindow window = server.windows.get(pack.getSocketAddress());
            TreeMap<Long, Pair<String, Boolean>> t = server.logs.get(pack.getSocketAddress());
            updateLogs(t, msg.getSeq_num());

            if(!window.getPacks().containsKey(msg.getSeq_num())) {
                window.insert(msg.getSeq_num());
            }

            // Verifica se o pacote pode ser confirmado; caso contrario, o ignora
            if(!window.insideWindow(msg.getSeq_num())){
                System.out.println("Pacote "+msg.getSeq_num()+
                        " chegou no servidor fora do intervalo da janela. Descartando...");
                continue;
            }

            // Faz a verificacao de erro
            if (!checkMd5(aux, msg.getnMd5())) {
                System.out.println("Falha na verificacao da mensagem " + msg.getSeq_num() +
                        ". Descartando mensagem...");
                continue;
            }

            System.out.println("Pacote " + msg.getSeq_num() + " recebido no servidor com mensagem "+msg.getMsg());
            t.put(msg.getSeq_num(), new Pair(msg.getMsg(), false));

            md5 = hash(String.valueOf(msg.getSeq_num() + msg.getTime().toString()));
            Ack ack = new Ack(msg.getSeq_num(), msg.getTime());//, md5);
            double rdm = Math.random();
            if (rdm < server.perror) {
                ack.setMd5(hash(md5));
                ack.nMd5[2] +=1;
                ack.setErr(true);
            }
            else {
                try {
                    window.update(msg.getSeq_num());
                } catch (NullPointerException e) {
                    System.out.println("Erro ao atualizar janela.");
                    window.print();
                    e.printStackTrace();
                    exit(1);

                }
                // Escreve mensagens pendentes no arquivo de saida
                tryToWrite(t, window,outFile);

            }
            send(server, ack, pack);
        }
    }

    @SuppressWarnings("unchecked")
    private synchronized static void tryToWrite(TreeMap<Long, Pair<String, Boolean>> t, SlidingWindow window, PrintWriter out) {
        for(Long i:t.keySet()) {
            if (!window.getPacks().containsKey(i) || !window.getPacks().get(i)) break;
            if (!t.get(i).getValue()) {
                out.write(t.get(i).getKey()+"\n");
                out.flush();
                t.replace(i, new Pair(t.get(i).getKey(), true));
            }
        }
    }

    private static void send(Server server, Ack ack, DatagramPacket pack) throws IOException {

        InetAddress addr = pack.getAddress();
        int portNum = pack.getPort();

        /*** ENVIA ACK ***/

        ByteBuffer buf = ByteBuffer.allocate(20000);
        buf.putLong(ack.getSeq_num());
        buf.putLong(ack.getTime().getSecs());
        buf.putInt(ack.getTime().getNanos());
        buf.put(ack.getnMd5());

        byte[] sendData = buf.array();

        DatagramPacket sPack = new DatagramPacket(sendData, sendData.length, addr, portNum);
        server.socket.send(sPack);

        System.out.print("Enviando ack do pacote "+ack.getSeq_num());
        if(ack.isErr()) System.out.println(" (com erro)");
        else System.out.println();
    }

    @SuppressWarnings("unchecked")
    private static void updateLogs(TreeMap<Long, Pair<String, Boolean>> t, long seqNum) {
        for(int i =0; i<= (int) seqNum; i++) {
            if(!t.containsKey((long)i))
                t.put((long)i, new Pair("", false));
        }
    }
}
