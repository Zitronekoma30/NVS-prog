package udp.src;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;

public class UDPTxStopWait {
    private DatagramSocket socket;
    private InetAddress address;
    private int port = 4444;
    private int bindPort = 4447;
    private int dataLen;

    public UDPTxStopWait(int dataLen) throws SocketException, UnknownHostException {
        this.dataLen = dataLen;
        this.socket = new DatagramSocket(bindPort);
        this.socket.setSoTimeout(10000); // Set timeout to 10 seconds
        this.address = InetAddress.getByName("localhost");
    }

    private void waitForAck(int seqNum, byte[] packet) throws IOException {
        while (true) {
            try {
                byte[] ackBuf = new byte[8];
                DatagramPacket ackPacket = new DatagramPacket(ackBuf, ackBuf.length);
                socket.receive(ackPacket);
                ByteBuffer wrapped = ByteBuffer.wrap(ackBuf);
                int packetType = wrapped.getInt();
                int receivedSeqNum = wrapped.getInt();

                if (packetType == 2) { // 2 is the ACK packet type
                    break;
                }
            } catch (SocketTimeoutException e) {
                System.out.println("Timeout waiting for ACK for seq_num " + seqNum + ". Retrying...");
                DatagramPacket retryPacket = new DatagramPacket(packet, packet.length, address, port);
                socket.send(retryPacket);
            } catch (IOException e) {
                System.out.println("Socket error: " + e.getMessage() + ". Retrying...");
            }
        }
    }

    public void sendFile(String filePath) throws IOException, NoSuchAlgorithmException {
        File file = new File(filePath);
        try (FileInputStream fis = new FileInputStream(file)) {
            int transmissionId = 1;
            int seqNumber = 0;
            long fileSize = file.length();
            String fileName = file.getName();
            int maxSeqNumber = (int) Math.ceil((double) fileSize / dataLen) + 1;

            System.out.println("maxSeqNumber: " + maxSeqNumber);
            System.out.println("fileSize: " + fileSize);

            // Send initial packet
            ByteBuffer initialBuffer = ByteBuffer.allocate(12 + fileName.length());
            initialBuffer.putInt(transmissionId);
            initialBuffer.putInt(seqNumber);
            initialBuffer.putInt(maxSeqNumber);
            initialBuffer.put(fileName.getBytes());
            byte[] initialPacket = initialBuffer.array();
            DatagramPacket datagramPacket = new DatagramPacket(initialPacket, initialPacket.length, address, port);
            socket.send(datagramPacket);

            waitForAck(seqNumber, initialPacket);

            seqNumber++;

            MessageDigest md5 = MessageDigest.getInstance("MD5");

            byte[] buffer = new byte[dataLen];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                ByteBuffer packetBuffer = ByteBuffer.allocate(8 + bytesRead);
                packetBuffer.putInt(transmissionId);
                packetBuffer.putInt(seqNumber);
                packetBuffer.put(buffer, 0, bytesRead);
                byte[] packet = packetBuffer.array();

                datagramPacket = new DatagramPacket(packet, packet.length, address, port);
                socket.send(datagramPacket);

                md5.update(buffer, 0, bytesRead);

                waitForAck(seqNumber, packet);

                seqNumber++;

                try {
                    Thread.sleep(10); // Small delay to avoid overwhelming the network
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            // Send the final packet with MD5 hash
            byte[] md5Hash = md5.digest();
            ByteBuffer finalBuffer = ByteBuffer.allocate(8 + md5Hash.length);
            finalBuffer.putInt(transmissionId);
            finalBuffer.putInt(seqNumber);
            finalBuffer.put(md5Hash);
            byte[] finalPacket = finalBuffer.array();
            datagramPacket = new DatagramPacket(finalPacket, finalPacket.length, address, port);
            socket.send(datagramPacket);
        }
    }

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Which packet size? 1K/16K/64K");
        int size = (scanner.nextInt() * 1024) - 64;

        System.out.println("Which file to send? 1MB/10MB/50MB/100MB");
        int fileSize = scanner.nextInt();
        String filePath = "./TestFiles/" + fileSize + "MB_file";

        UDPTxStopWait udp = new UDPTxStopWait(size);

        for (int i = 0; i < 1; i++) {
            udp.sendFile(filePath);
            System.out.println("Sent " + fileSize + "MB file " + (i + 1) + " times");
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        udp.socket.close();
        System.out.println("e to exit: ");
        if (!scanner.next().equals("e")) {
            main(args);
        }
    }
}
