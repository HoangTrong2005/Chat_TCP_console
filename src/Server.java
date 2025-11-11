

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    private final int SERVER_PORT = 1433;

    public Server() {
        try (ServerSocket serverSocket = new ServerSocket(SERVER_PORT)) {
            System.out.println(" Server đang chạy tại cổng " + SERVER_PORT);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println(" Client mới kết nối: " + socket.getRemoteSocketAddress());

                // Tạo luồng riêng cho mỗi client
                new Thread(new ClientHandler(socket)).start();
            }

        } catch (IOException e) {
            System.out.println(" Lỗi server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new Server();
    }
}
