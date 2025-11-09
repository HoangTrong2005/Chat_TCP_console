// Client kết nối vào server

import java.io.IOException;
import java.net.Socket;

public class Client {
    private final String SERVER_IP = "localhost";
    private final int SERVER_PORT = 1433;

    public Client() {
        try (Socket socket = new Socket(SERVER_IP, SERVER_PORT)) {
            System.out.println(" Đã kết nối tới server: " + SERVER_IP + ":" + SERVER_PORT);
        } catch (IOException e) {
            System.out.println(" Lỗi kết nối tới server: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        new Client();
    }
}
