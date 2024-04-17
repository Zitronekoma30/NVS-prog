package udp.src;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class UDPRx {
    public static void main(String[] args) {
        DatagramSocket socket = null;
        try {
            // Create a UDP socket
            socket = new DatagramSocket(4445);
            socket.setReceiveBufferSize(1024 * 1024); // Increase receive buffer size to 1MB

            byte[] buffer = new byte[4096];

            // Initialize variables
            int transmissionId = -1;
            int maxSeqNumber = -1;
            String fileName = null;
            byte[] fileData = new byte[0];
            byte[] receivedMd5 = null;

            System.out.println("Listening on localhost:4445");

            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                byte[] data = packet.getData();

                System.out.println("Received " + packet.getLength() + " bytes");

                if (transmissionId == -1) {
                    // This is the first packet, get transmissionId, maxSeqNumber, and file name
                    transmissionId = ((data[0] & 0xFF) << 24) | ((data[1] & 0xFF) << 16) | ((data[2] & 0xFF) << 8) | (data[3] & 0xFF);
                    int seqNumber = ((data[4] & 0xFF) << 24) | ((data[5] & 0xFF) << 16) | ((data[6] & 0xFF) << 8) | (data[7] & 0xFF);
                    maxSeqNumber = ((data[8] & 0xFF) << 24) | ((data[9] & 0xFF) << 16) | ((data[10] & 0xFF) << 8) | (data[11] & 0xFF);
                    fileName = new String(data, 12, packet.getLength() - 12, "UTF-8");
                    System.out.println("Receiving file: " + fileName);
                    System.out.println("seq_number: " + seqNumber);
                    System.out.println("max_seq_number: " + maxSeqNumber);
                } else {
                    // Regular packet, get data
                    int seqNumber = ((data[4] & 0xFF) << 24) | ((data[5] & 0xFF) << 16) | ((data[6] & 0xFF) << 8) | (data[7] & 0xFF);
                    if (seqNumber == maxSeqNumber) {
                        System.out.println("final: " + seqNumber);
                        // get md5
                        receivedMd5 = new byte[packet.getLength() - 8];
                        System.arraycopy(data, 8, receivedMd5, 0, receivedMd5.length);
                        break;
                    }
                    byte[] newData = new byte[fileData.length + (packet.getLength() - 8)];
                    System.arraycopy(fileData, 0, newData, 0, fileData.length);
                    System.arraycopy(data, 8, newData, fileData.length, packet.getLength() - 8);
                    fileData = newData;
                    System.out.println("seq_number: " + seqNumber);
                }
            }

            // Check MD5 hash
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(fileData);
            byte[] calculatedMd5 = md5.digest();
            if (MessageDigest.isEqual(calculatedMd5, receivedMd5)) {
                System.out.println("File received successfully");
                try (FileOutputStream fos = new FileOutputStream(fileName)) {
                    fos.write(fileData);
                }
            } else {
                System.out.println("File was not received correctly");
            }

        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        } finally {
            if (socket != null) {
                socket.close();
            }
        }
    }
}
