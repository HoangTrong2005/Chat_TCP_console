
import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Client {
    private final String SERVER_IP = "localhost";
    private final int SERVER_PORT = 1433;
    private Socket socket;
    //Dung BufferedReader/PrintWriter de doc/ghi theo tung dong
    private BufferedReader reader; // Doc tu server
    private PrintWriter writer; // Gui len server

    public Client() {
        try {
            socket = new Socket(SERVER_IP, SERVER_PORT);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);
            Scanner sc = new Scanner(System.in);

            System.out.println("Da ket noi den server: " + SERVER_IP + ":" + SERVER_PORT);


            Thread readThread = new Thread(() -> {
                try {
                    String serverMsg;
                    while ((serverMsg = reader.readLine()) != null) {
                        if (serverMsg.equals("[SERVER_KICK]")) {
                            System.out.println("Bạn đã bị kick khỏi server!");
                            closeClient();
                            break;
                        }
                        if (serverMsg.equals("[SERVER] Server đóng. Đang thoát...")) {
                            System.out.println("Server đã đóng, mọi người thoát khỏi phòng chat");
                            closeClient();
                            break;
                        }
                        System.out.println(serverMsg);
                    }
                } catch (IOException e) {
                    System.out.println("Server đã ngắt kết nối.");
                    closeClient();
                }
            });readThread.start();

            // Thread gui tin (luong main doc tu console)
            while (true) {
                String line = sc.nextLine();
                writer.println(line);
                if (line.equalsIgnoreCase("/exit")) {
                    closeClient();
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("Loi ket noi toi server: " + e.getMessage());
        }
    }

    private void closeClient() {
        try {
            if (reader != null) reader.close();
            if (writer != null) writer.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
        System.out.println("Đã thoát chương trình.");
        System.exit(0);
    }

    public static void main(String[] args) {
        new Client();
    }
}