package redes;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.security.NoSuchAlgorithmException;

import static redes.Message.hash;

public class Server {
    public static void main(String[] args) throws IOException, NoSuchAlgorithmException, ClassNotFoundException {

        if (args.length < 3) {
            System.err.println("Usage: <file> <port number> <Perror>");
            System.exit(1);
        }


        BufferedWriter outFile = null;
        try {
            outFile = new BufferedWriter(new FileWriter(args[0], false));
        }
        catch (IOException e)
        {
            System.err.println("Error: " + e.getMessage());
        }

        int portNumber = Integer.parseInt(args[1]);
        final double perror = Double.parseDouble(args[2]);

        // UDP
        DatagramSocket socket = new DatagramSocket(portNumber);
        byte[] recvData = new byte[1024];
        byte[] sendData = new byte[1024];

        long seq_num;
        Timestamp time;
        String m;
        short size;
        String md5;

        DatagramPacket pack = new DatagramPacket(recvData,
                recvData.length);
        System.out.println("Esperando por datagrama UDP na porta " + portNumber);

        while(true) {

            /*** RECEBE MENSAGEM ***/

            socket.receive(pack);
            ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(recvData));
            seq_num = (Long) in.readObject();

            socket.receive(pack);
            time = (Timestamp) in.readObject();

            socket.receive(pack);
            m = (String) in.readObject();

            socket.receive(pack);
            size = (short)in.readObject();

            socket.receive(pack);
            md5 = (String) in.readObject();

            LogMessage msg = new LogMessage(seq_num, time, size, m, md5);
            System.out.println(msg.toString()+"\n");


            // Faz a verificacao de erro
            if(!(Message.hash(String.valueOf(msg.getSeq_num()) + time.toString()
                    + String.valueOf((msg.getSize())) + msg.getMsg()).equals(msg.getMd5()))) {
                System.out.println("Falha na verificacao! Descartar mensagem\n");
            }

            // Escreve mensagem no arquivo de saida
            outFile.write(msg.getMsg()+"\n");
            outFile.flush();



            /*** ENVIA ACK ***/

            md5 = hash(String.valueOf(seq_num) + time.toString());
            double rdm = Math.random();
            if(rdm < perror) md5 = hash(md5);
            Ack ack = new Ack(seq_num, time, md5);

            InetAddress addr = pack.getAddress();
            int portNum = pack.getPort();

            ByteArrayOutputStream bStream = new ByteArrayOutputStream();
            ObjectOutput out = new ObjectOutputStream(bStream);

            out.writeObject(ack.getSeq_num());
            sendData = bStream.toByteArray();
            DatagramPacket sPack = new DatagramPacket(sendData, sendData.length, addr, portNum);
            socket.send(sPack);


            out.writeObject(ack.getTime());
            sendData = bStream.toByteArray();
            sPack = new DatagramPacket(sendData, sendData.length, addr, portNum);
            socket.send(sPack);


            out.writeObject(ack.getMd5());
            sendData = bStream.toByteArray();
            sPack = new DatagramPacket(sendData, sendData.length, addr, portNum);
            socket.send(sPack);
        }

    }
}
