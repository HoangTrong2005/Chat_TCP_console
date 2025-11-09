// Server lắng nghe kết nối

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    private final int SERVER_PORT = 1433;

    public Server() {
        try (ServerSocket serverSocket = new ServerSocket(SERVER_PORT)) {
            System.out.println("✅ Server đang chạy tại cổng " + SERVER_PORT);
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("🔌 Client mới kết nối: " + socket.getRemoteSocketAddress());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new Server();
    }
}
