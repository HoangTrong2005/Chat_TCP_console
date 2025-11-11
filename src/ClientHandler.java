package chat_TCP;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket socket;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)
        ) {
            writer.println("Chào! Bạn đã kết nối thành công tới server!");
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("Client gửi: " + line);
                writer.println("Server nhận được: " + line);
            }
        } catch (IOException e) {
            System.out.println("⚠️ Client ngắt kết nối.");
        }
    }
}
