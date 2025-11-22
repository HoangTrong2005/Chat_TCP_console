
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
            System.out.println("Da ket noi den server: " + SERVER_IP + ":" + SERVER_PORT);

            // Tao luong doc (tu server)
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            // Tao luong ghi (den server), true = autoFlush
            this.writer = new PrintWriter(socket.getOutputStream(), true);

            // Thread nhan tin (chuyen lang nghe server)
            // Day la mot luong rieng chi de nhan tin nhan tu server va in ra console
            new Thread(() -> {
                try {
                    String serverMessage;
                    while ((serverMessage = reader.readLine()) != null) {
                        System.out.println(serverMessage); // In ra man hinh
                    }
                } catch (IOException e) {
                    System.out.println(" Da ngat ket noi voi server.");
                }
            }).start();

            // Thread gui tin (luong main doc tu console)
            Scanner sc = new Scanner(System.in);
            while (true) {
                String line = sc.nextLine();
                writer.println(line); // Gui len server (tu dong them \n va flush)

                if (line.equalsIgnoreCase("/exit")) {
                    break; // Thoat neu nguoi dung go /exit
                }
            }
        } catch (UnknownHostException e) {
            System.err.println("Khong tim thay server: " + SERVER_IP);
        } catch (IOException e) {
            System.err.println("Loi ket noi toi server: " + e.getMessage());
        } finally {
            try {
                if (socket != null) socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("Da thoat chuong trinh.");
        }
    }

    public static void main(String[] args) {
        new Client();
    }
}