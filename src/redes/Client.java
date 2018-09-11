package redes;

import java.io.*;
import java.net.*;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.TimerTask;
import java.util.regex.Pattern;

import static redes.LogMessage.setAndGetMessage;
import static redes.Message.checkMd5;

public class Client {

    private InetAddress addr;
    private  int portNumber;
    private int tout;
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
        this.tout = Integer.parseInt(args[3]);
        this.perror = Double.parseDouble(args[4]);
        this.socket = new DatagramSocket();
        this.logs = new ArrayList<>();
        this.totalAcks = 0;
        this.totalLogs = -1;
        this.sent = 0;
        this.corrupted = 0;

    }

    public static void main(String[] args) throws IOException, InterruptedException {

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
            } catch (IOException | NoSuchAlgorithmException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        } );

        t1.start();
        Thread.sleep(1);
        t2.start();

    }

    /*** RECEBE ACKs ***/
    @SuppressWarnings("unchecked")
    private static synchronized void recvAcks(Client client)
            throws IOException, ClassNotFoundException, NoSuchAlgorithmException {

        byte[] recvData = new byte[16384];

        // Condicao de parada: numero de acks recebidos for igual ao numero de logs lidos
        while(client.totalLogs != client.totalAcks) {
            DatagramPacket rPack = new DatagramPacket(recvData, recvData.length);

            client.socket.receive(rPack);
            ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(recvData));

            HashMap<Integer, Object> l = (HashMap<Integer, Object>)in.readObject();
            Ack ack = new Ack((long)l.get(0), new Timestamp((long)l.get(1), (int)l.get(2)), (String) l.get(3));

            // Verificacao de erro
            if(checkMd5(String.valueOf(ack.getSeq_num()) + ack.getTime().toString(), ack.getMd5())) {
                client.window.update(ack.getSeq_num());
                client.totalAcks++;
                System.out.println("Recebido pacote "+ack.getSeq_num()+" no cliente com sucesso!");
            }
            else {
                System.out.println("Pacote "+ack.getSeq_num()+" chegou com erro. Reenviando...");
                LogMessage msg = client.logs.get((int)ack.getSeq_num());
                /*** DOCUMENTAR QUE PACOTE É REENVIADO APÓS RETORNAR UM ACK CORROMPIDO ***/
                sendMessage(client, msg);
            }
        }
        System.out.printf("\n%d %d %d %.3f", client.totalLogs, client.sent, client.corrupted, (double)(System.currentTimeMillis()-client.timer)/1000);
        client.socket.close();
        System.exit(0);
    }

    private static void start(Client client, String[] args)
            throws IOException, NoSuchAlgorithmException, InterruptedException {

        long seq_num = 0;
        Scanner scanner = new Scanner(new File(args[0]));
        while (scanner.hasNextLine()) {

            // Segundos desde Epoch (1970-01-01 00:00:00 +0000 (UTC).)
            Instant inst = Instant.now();

            // Espera ate que o pacote esteja dentro da janela deslizante ("enviável")
            while(!client.window.insideWindow(seq_num)){}

            Timestamp time = new Timestamp(inst.getEpochSecond(), inst.getNano());
            LogMessage msg = setAndGetMessage(scanner.nextLine(), seq_num, client.perror, time);
            client.logs.add(msg);

            /*** Insere na janela deslizante ***/
            client.window.insert(seq_num);


            /*** ENVIA MENSAGEM ***/
            sendMessage(client, msg);

            seq_num++;

        }
        client.totalLogs = (int)seq_num;

    }

    /*** ENVIA MENSAGENS***/
    private static void sendMessage(Client client, LogMessage msg) throws IOException, NoSuchAlgorithmException {

        // Segundos desde Epoch (1970-01-01 00:00:00 +0000 (UTC).)
        long secs = Instant.now().getEpochSecond();
        long start = System.nanoTime();
        Timestamp time = new Timestamp(secs, Math.toIntExact(System.nanoTime()-start));
        msg = setAndGetMessage(msg.getMsg(), msg.getSeq_num(), client.perror, time);

        if(client.window.getPacks().get(msg.getSeq_num())) return;

        System.out.print("Enviando pacote "+msg.getSeq_num()+" para o servidor com mensagem "+msg.getMsg());
        client.sent++;
        if (msg.isErr()) {
            System.out.println(" (com erro).");
            client.corrupted++;
        }
        else System.out.println(".");

        ByteArrayOutputStream bStream = new ByteArrayOutputStream();
        ObjectOutput out = new ObjectOutputStream(bStream);

        HashMap<Integer, Object> list = new HashMap<>();
        list.put(0, msg.getSeq_num());
        list.put(1, msg.getTime().getSecs());
        list.put(2, msg.getTime().getNanos());
        list.put(3, msg.getSize());
        list.put(4, msg.getMsg());
        list.put(5, msg.getMd5());

        out.writeObject(list);
        byte[] send = bStream.toByteArray();
        DatagramPacket p = new DatagramPacket(send, send.length, client.addr, client.portNumber);
        client.socket.send(p);

        LogMessage finalMsg = msg;
        // Temporizador que dispara após "Tout" segundos -> caso não tenha recebido ACK do pacote, reenvia
        client.logs.get((int)finalMsg.getSeq_num()).timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if(!client.window.getPacks().get(finalMsg.getSeq_num())) {
                    System.out.println("Pacote "+ finalMsg.getSeq_num()+" nao chegou. Reenviando...");
                    try {
                        sendMessage(client, client.logs.get((int) finalMsg.getSeq_num()));
                    } catch (IOException | NoSuchAlgorithmException e) {
                        e.printStackTrace();
                    }
                }
            }
        }, client.tout*1000);
    }
}

