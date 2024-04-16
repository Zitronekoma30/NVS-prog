package udp.src;

import java.io.*;
import java.security.NoSuchAlgorithmException;

public class Main {
    public static void main(String[] args) {
        try {
            UDPTx tx = new UDPTx();
            tx.sendFile("D:/Programming/Uni/NVS-prog/udp/src/test.txt"); // Pfad zur Datei, die gesendet werden soll
            System.out.println("File sent successfully");
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }
}