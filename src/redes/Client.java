package redes;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.ToDoubleBiFunction;
import java.util.regex.Pattern;

import static redes.LogMessage.setAndGetMessage;

public class Client {
    public static void main(String[] args) throws IOException, NoSuchAlgorithmException, ClassNotFoundException {

        if (args.length < 4) {
            System.err.println(
                    "Usage:  <file> <IP:port number> <tout> <Perror>");
            System.exit(1);
        }

        final String[] ip_port = args[1].split(Pattern.quote(":"));
        InetAddress addr = InetAddress.getByName(ip_port[0]);
        int portNumber = Integer.parseInt(ip_port[1]);
        int tout = Integer.parseInt(args[2]);
        final double perror = Double.parseDouble(args[3]);


        DatagramSocket socket = new DatagramSocket();

        Timer timer = new Timer();
        long seq_num = 0;
        Scanner scanner = new Scanner(new File(args[0]));
        while (scanner.hasNextLine()) {
            // Segundos desde Epoch (1970-01-01 00:00:00 +0000 (UTC).)
            long secs = Instant.now().getEpochSecond();
            long start = System.nanoTime();

            ByteArrayOutputStream bStream = new ByteArrayOutputStream();
            ObjectOutput out = new ObjectOutputStream(bStream);
            Timestamp time = new Timestamp(secs, Math.toIntExact(System.nanoTime()-start));
            LogMessage msg = setAndGetMessage(scanner.nextLine(), seq_num, perror, time);

            /*** ENVIA MENSAGEM ***/

            // Envia numero de sequencia
            out.writeObject(msg.getSeq_num());
            byte[] send_serial = bStream.toByteArray();
            DatagramPacket pack = new DatagramPacket(send_serial,
                    send_serial.length, addr, portNumber);
            socket.send(pack);

            // Envia timestamp
            out.writeObject(msg.getTime());
            send_serial = bStream.toByteArray();
            pack = new DatagramPacket(send_serial,
                    send_serial.length, addr, portNumber);
            socket.send(pack);

            // Envia tamanho mensagem
            out.writeObject(msg.getMsg());
            send_serial = bStream.toByteArray();
            pack = new DatagramPacket(send_serial,
                    send_serial.length, addr, portNumber);
            socket.send(pack);

            //Envia mensagem
            out.writeObject(msg.getSize());
            send_serial = bStream.toByteArray();
            pack = new DatagramPacket(send_serial,
                    send_serial.length, addr, portNumber);
            socket.send(pack);

            // Envia codigo de verificacao de erro
            out.writeObject(msg.getMd5());
            send_serial = bStream.toByteArray();
            pack = new DatagramPacket(send_serial,
                    send_serial.length, addr, portNumber);
            socket.send(pack);

            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    // TODO: 30/08/18 IMPLEMENTAR A LOGICA DE REENVIO DE MENSAGEM
                }
            }, tout);

            /*** RECEBE ACK ***/

            byte[] recvData = new byte[1024];
            DatagramPacket rPack = new DatagramPacket(recvData, recvData.length);
            socket.receive(rPack);
            ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(recvData));
            long seqNum = (long) in.readObject();


            socket.receive(rPack);
            Timestamp timeS = (Timestamp) in.readObject();


            socket.receive(rPack);
            String md5 = (String) in.readObject();

            Ack ack = new Ack(seqNum, timeS, md5);
            System.out.println(ack+"\n");

            seq_num++;

        }

        timer.cancel();
        socket.close();
        /*} catch (UnknownHostException e) {
            System.err.println("Host desconhecido " + args[0]);
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Nao conseguiu conectar a " +
                    args[0]);
            System.exit(1);
        }*/

        // TODO: 29/08/18
        // Ao final da execução, o cliente deve imprimir uma linha com o número de mensagens de log distintas,
        // o número de mensagens de log transmitidas (incluindo retransmissões), o número de mensagens transmitidas com MD5 incorreto,
        // e o tempo total de execução. Utilize um formato de impressão equivalente ao formato [%d %d %d %.3fs]
        // da função [printf] da biblioteca padrão do C.

    }


}

