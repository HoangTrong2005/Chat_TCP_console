
import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    private final String SERVER_IP = "localhost";
    private final int SERVER_PORT = 1433;

    public Client() {
        try (Socket socket = new Socket(SERVER_IP, SERVER_PORT)) {
            System.out.println(" Đã kết nối tới server: " + SERVER_IP + ":" + SERVER_PORT);

            // Nhận tin nhắn từ server ở luồng riêng
            BufferedReader serverReader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream())
            );

            new Thread(() -> {
                try {
                    String msg;
                    while ((msg = serverReader.readLine()) != null) {
                        System.out.println(msg);
                    }
                } catch (IOException e) {
                    System.out.println(" Mất kết nối với server.");
                }
            }).start();

            // Gửi tin nhắn từ client lên server
            Scanner sc = new Scanner(System.in);
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);

            while (true) {
                String line = sc.nextLine();
                writer.println(line);
                if (line.equalsIgnoreCase("/exit")) break;
            }

        } catch (IOException e) {
            System.out.println(" Lỗi kết nối tới server: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        new Client();
    }
}
