package udp.src;

import java.io.*;
import java.security.NoSuchAlgorithmException;

public class Main {
    public static void main(String[] args) {
        try {
            UDPTx tx = new UDPTx();
            tx.sendFile("path_to_file"); // Pfad zur Datei, die gesendet werden soll
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }
}