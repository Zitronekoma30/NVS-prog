package udp.src;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class UDPReceiverStopWait {

    private static final int BUFFER_SIZE = 64 * 1024; // 64KB

    public static long receiveFile() throws IOException, NoSuchAlgorithmException {
        DatagramSocket socket = new DatagramSocket(4444);
        DatagramSocket ackSocket = new DatagramSocket();
        socket.setReceiveBufferSize(1024 * 1024); // Set receive buffer to 1MB
        InetAddress address = InetAddress.getByName("localhost");
        int ackPort = 4447;

        System.out.println("starting up on localhost port 4445");

        MessageDigest md5 = MessageDigest.getInstance("MD5");

        // Initialize variables
        Integer transmissionId = null;
        Integer maxSeqNumber = null;
        String fileName = null;
        ByteArrayOutputStream fileData = new ByteArrayOutputStream();
        byte[] receivedMd5 = null;

        System.out.println("listening on localhost:4444");

        long startTime = 0;

        while (true) {
            byte[] buffer = new byte[BUFFER_SIZE];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);

            int packetLength = packet.getLength();
            //System.out.println("received " + packetLength + " bytes");

            ByteBuffer wrapped = ByteBuffer.wrap(packet.getData(), 0, packetLength);

            if (transmissionId == null) {
                // This is the first packet, get transmissionId, maxSeqNumber, and file name
                startTime = System.currentTimeMillis();
                transmissionId = wrapped.getInt();
                int seqNumber = wrapped.getInt();
                maxSeqNumber = wrapped.getInt();
                byte[] fileNameBytes = new byte[packetLength - 12];
                wrapped.get(fileNameBytes);
                fileName = new String(fileNameBytes).trim();
                System.out.println("Receiving file: " + fileName);
                System.out.println("seq_number: " + seqNumber);
                System.out.println("max_seq_number: " + maxSeqNumber);
            } else {
                // Regular packet, get data
                wrapped.position(4); // skip transmissionId
                int seqNumber = wrapped.getInt();

                if (seqNumber == maxSeqNumber) {
                    System.out.println("final: " + seqNumber);
                    // get md5
                    receivedMd5 = new byte[packetLength - 8];
                    wrapped.get(receivedMd5);
                    break;
                }

                byte[] data = new byte[packetLength - 8];
                wrapped.get(data);
                fileData.write(data);
                md5.update(data);
                System.out.print("\rseq_number: " + seqNumber);
                System.out.flush();
            }

            // Send ACK with seq_number
            ByteBuffer ackBuffer = ByteBuffer.allocate(8);
            ackBuffer.putInt(2);
            ackBuffer.putInt(wrapped.getInt(4));
            byte[] ack = ackBuffer.array();
            DatagramPacket ackPacket = new DatagramPacket(ack, ack.length, address, ackPort);
            ackSocket.send(ackPacket);
        }

        // Check MD5 hash
        byte[] calculatedMd5 = md5.digest();

        long receiveTime = System.currentTimeMillis() - startTime;
        System.out.println("Time taken: " + receiveTime + " ms");
        System.out.println("Speed: " + (fileData.size() / 1e6) / (receiveTime / 1000.0) + " MBps");

        if (MessageDigest.isEqual(calculatedMd5, receivedMd5)) {
            System.out.println("File received successfully");
            try (FileOutputStream fos = new FileOutputStream(fileName)) {
                fileData.writeTo(fos);
            }
        } else {
            System.out.println("File was not received correctly");
        }

        socket.close();
        return receiveTime;
    }

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        List<Long> times = new ArrayList<>();
        int filesSent = 0;

        while (true) {
            filesSent++;
            long rcvTime = receiveFile();
            times.add(rcvTime);
            System.out.println("Average time: " + times.stream().mapToLong(Long::longValue).average().orElse(0.0) + " ms over " + filesSent + " files");
        }
    }
}
