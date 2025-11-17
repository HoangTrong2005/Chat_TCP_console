
import java.io.*;
import java.net.Socket;
import java.util.Scanner;

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
            ){
                
            writer.println("Chào! Bạn đã kết nối thành công tới server!");
            // Nhận tin từ client
            new Thread(() -> {
                try {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println("Client gửi: " + line);
                    }
                } catch (IOException e) {
                    System.out.println(" Client đã ngắt kết nối.");
                }
            }).start();

            //Server nhập tin gửi xuống client
            Scanner sc = new Scanner(System.in);
            while (true) {
                String msg = sc.nextLine();
                writer.println("Server: " + msg);

                if (msg.equalsIgnoreCase("/exit"))
                    break;
            }

        } catch (IOException e) {
            System.out.println(" Lỗi trong ClientHandler.");
        }
    }
}
