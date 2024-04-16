package udp.src;

import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class UDPTx {
    private DatagramSocket socket;
    private InetAddress address;
    private byte[] buf;

    public UDPTx() throws SocketException, UnknownHostException {
        socket = new DatagramSocket();
        address = InetAddress.getByName("localhost"); // oder die Ziel-IP-Adresse
    }

    public void sendFile(String file) throws IOException, NoSuchAlgorithmException {
        FileInputStream fis = new FileInputStream(file);
        MessageDigest md = MessageDigest.getInstance("MD5");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        
        int transmissionId = 1; // Beispiel-Wert
        byte[] data = new byte[1024];
        int bytesRead;

        int seqNumber = 0;
        File fileObj = new File(file);
        long fileSize = fileObj.length();
        String fileName = fileObj.getName();
        int maxSeqNumber = (int) Math.ceil((double) fileSize / 1024);

        System.out.println("maxSeqNumber: " + maxSeqNumber);
        System.out.println("fileSize: " + fileSize);

        dos.writeInt(transmissionId);
        dos.writeInt(seqNumber++);
        dos.writeInt(maxSeqNumber);
        dos.writeBytes(fileName);

        // Send initial packet
        byte[] initialPacket = baos.toByteArray();
        DatagramPacket packet = new DatagramPacket(initialPacket, initialPacket.length, address, 4445);
        socket.send(packet);

        // Reset ByteArrayOutputStream
        baos.reset();

        // Now start sending file data
        

        while ((bytesRead = fis.read(data)) != -1) {
            dos.writeInt(transmissionId);
            dos.writeInt(seqNumber++);

            dos.write(data, 0, bytesRead);
            md.update(data, 0, bytesRead); // Update MD5 hash with the chunk of data
            
            buf = baos.toByteArray();
            packet = new DatagramPacket(buf, buf.length, address, 4445);
            socket.send(packet);
            
            baos.reset(); // Reset the ByteArrayOutputStream for the next packet
        }
        
        // Send the final packet with MD5 hash
        byte[] md5Hash = md.digest();
        dos.writeInt(transmissionId);
        dos.writeInt(seqNumber);
        dos.write(md5Hash);
        buf = baos.toByteArray();
        packet = new DatagramPacket(buf, buf.length, address, 4445);
        socket.send(packet);

        fis.close();
        socket.close();
    }
}
