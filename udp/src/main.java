package udp.src;

import java.io.*;
import java.security.NoSuchAlgorithmException;

public class Main {
    public static void main(String[] args) {
        try {
            UDPTx tx = new UDPTx();
            var time = System.currentTimeMillis();
            System.out.println("Time: " + time);
            //tx.sendFile("D:/test.txt"); // Pfad zur Datei, die gesendet werden soll
            tx.sendFile("C:/Users/Leon/Pictures/image2.webp");
            System.out.println("File sent successfully");
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }
}