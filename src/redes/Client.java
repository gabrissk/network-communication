package redes;

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.TimerTask;
import java.util.regex.Pattern;

import static redes.LogMessage.setAndGetMessage;
import static redes.Message.checkMd5;

public class Client {

    private InetAddress addr;
    private  int portNumber;
    //private int tout;
    private double tout;
    private double perror;
    private SlidingWindow window;
    private DatagramSocket socket;
    private ArrayList<LogMessage> logs;
    private int totalAcks;
    private int totalLogs;
    private int sent;
    private int corrupted;
    private long timer;

    private Client(String[] ip_port, String[] args) throws UnknownHostException, SocketException {
        this.addr = InetAddress .getByName(ip_port[0]);
        this.portNumber = Integer.parseInt(ip_port[1]);
        this.window = new SlidingWindow(Integer.parseInt(args[2]));
        //this.tout = Integer.parseInt(args[3]);
        this.tout = Double.parseDouble(args[3]);
        this.perror = Double.parseDouble(args[4]);
        this.socket = new DatagramSocket();
        this.logs = new ArrayList<>();
        this.totalAcks = 0;
        this.totalLogs = -1;
        this.sent = 0;
        this.corrupted = 0;

    }

    public static void main(String[] args) throws IOException {

        if (args.length < 5) {
            System.err.println(
                    "Uso:  <arquivo> <IP:port> <Wtx> <Tout> <Perror>");
            System.exit(1);
        }

        final String[] ip_port = args[1].split(Pattern.quote(":"));

        Client client = new Client(ip_port, args);
        client.timer = System.currentTimeMillis();

        // Envia mensagens
        Thread t1 = new Thread(() -> {
            try {
                start(client, args);
            } catch (IOException | NoSuchAlgorithmException | InterruptedException e) {
                e.printStackTrace();
            }
        } );

        // Recebe ACKs
        Thread t2 = new Thread(() -> {
            try {
                recvAcks(client);
            } catch (IOException | NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        } );

        t1.start(); // Para rodar concorrentemente os metodos de envio e recebimento de pacotes
        t2.start();

    }

    /*** RECEBE ACKs ***/
    @SuppressWarnings("unchecked")
    private static synchronized void recvAcks(Client client)
            throws IOException, NoSuchAlgorithmException {

        byte[] recvData = new byte[16384];
        long seqNum;
        Timestamp time;

        // Condicao de parada: numero de acks recebidos for igual ao numero de logs lidos
        while(client.totalLogs != client.totalAcks) {
            DatagramPacket rPack = new DatagramPacket(recvData, recvData.length);

            client.socket.receive(rPack);

            ByteBuffer buf = ByteBuffer.wrap(rPack.getData());
            seqNum = buf.getLong();
            time = new Timestamp(buf.getLong(), buf.getInt());
            byte[] aux = new byte[16];
            buf.get(aux, 0, 16);

            Ack ack = new Ack(seqNum, time);//, md5);

            // Verificacao de erro
            if(checkMd5(aux, ack.getnMd5())) {
                System.out.println("Recebido pacote "+ack.getSeq_num()+" no cliente com sucesso!");
                if(!client.window.getPacks().get(ack.getSeq_num())) {
                    client.window.update(ack.getSeq_num());
                    client.totalAcks++;
                }
            }
            else {
                System.out.println("Pacote "+ack.getSeq_num()+" chegou com erro. Reenviando...");
                LogMessage msg = client.logs.get((int)ack.getSeq_num()); // MUDAR PRA -1 CASO SEQNUM COMECE POR 1
                /*** DOCUMENTAR QUE PACOTE É REENVIADO APÓS RETORNAR UM ACK CORROMPIDO ***/
                sendMessage(client, msg);
            }
        }
        System.out.printf("\n%d %d %d %.3f", client.totalLogs, client.sent, client.corrupted, (double)(System.currentTimeMillis()-client.timer)/1000);
        System.exit(0);
    }

    private static void start(Client client, String[] args)
            throws IOException, NoSuchAlgorithmException, InterruptedException {

        long seq_num = 0; // MUDAR PRA 1 CASO SEQNUM COMECE POR 1
        Scanner scanner = new Scanner(new File(args[0]));
        while (scanner.hasNextLine()) {
            String nextLog = scanner.nextLine();
            if(nextLog == null || nextLog.equals("")) continue;

            // Segundos desde Epoch (1970-01-01 00:00:00 +0000 (UTC).)
            Instant inst = Instant.now();

            // Espera ate que o pacote esteja dentro da janela deslizante ("enviável")
            while(!client.window.insideWindow(seq_num)){}

            Timestamp time = new Timestamp(inst.getEpochSecond(), inst.getNano());
            LogMessage msg = setAndGetMessage(nextLog, seq_num, client.perror, time);
            client.logs.add(msg);

            /*** Insere na janela deslizante ***/
            client.window.insert(seq_num);


            /*** ENVIA MENSAGEM ***/
            sendMessage(client, msg);

            seq_num++;

        }
        client.totalLogs = (int)seq_num; // MUDAR PRA -1 CASO SEQNUM COMECE POR 1
        Thread.currentThread().interrupt();

    }

    /*** ENVIA MENSAGENS***/
    private static void sendMessage(Client client, LogMessage msg) throws IOException, NoSuchAlgorithmException {

        msg = setAndGetMessage(msg.getMsg(), msg.getSeq_num(), client.perror, msg.getTime());

        if(client.window.getPacks().get(msg.getSeq_num())) return;

        System.out.print("Enviando pacote "+msg.getSeq_num()+" para o servidor com mensagem "+msg.getMsg());
        client.sent++;
        if (msg.isErr()) {
            System.out.println(" (com erro).");
            client.corrupted++;
        }
        else System.out.println(".");

        ByteBuffer buf = ByteBuffer.allocate(60000);
        buf.putLong(msg.getSeq_num());
        buf.putLong(msg.getTime().getSecs());
        buf.putInt(msg.getTime().getNanos());
        buf.putShort(msg.getSize());
        buf.put(msg.getMsg().getBytes());
        buf.put(msg.getnMd5());

        byte[] send = buf.array();

        DatagramPacket p = new DatagramPacket(send, send.length, client.addr, client.portNumber);
        client.socket.send(p);

        LogMessage finalMsg = msg;
        // Temporizador que dispara após "Tout" segundos -> caso não tenha recebido ACK do pacote, reenvia
        client.logs.get((int)finalMsg.getSeq_num()).timer.schedule(new TimerTask() { // MUDAR PRA -1 CASO SEQNUM COMECE POR 1
            @Override
            public void run() {
                if(!client.window.getPacks().get(finalMsg.getSeq_num())) {
                    System.out.println("Pacote "+ finalMsg.getSeq_num()+" nao chegou. Reenviando...");
                    try {
                        sendMessage(client, client.logs.get((int) finalMsg.getSeq_num())); // MUDAR PRA -1 CASO SEQNUM COMECE POR 1
                    } catch (IOException | NoSuchAlgorithmException e) {
                        e.printStackTrace();
                    }
                }
            }
        }, new Double(client.tout*1000).longValue());
    }
}

