package udp.src;

import java.io.*;
import java.security.NoSuchAlgorithmException;

public class Main {
    public static void main(String[] args) {
        try {
            UDPTx tx = new UDPTx();
            tx.sendFile("D:/test.txt"); // Pfad zur Datei, die gesendet werden soll
            ////tx.sendFile("C:/Users/Leon/Pictures/image.webp");
            System.out.println("File sent successfully");
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }
}