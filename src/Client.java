
import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
    private static final String IP = "localhost";
    private static final int PORT = 1433;

    public static void main(String[] args) {
        new Client().run();
    }

    private void run() {
        try (Socket socket = new Socket(IP, PORT);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            Scanner sc = new Scanner(System.in)) {

            System.out.println("\033[1;36mKết nối thành công!\033[0m");

            // luong rieng nhan thông tin rieng
            new Thread(() -> {
                try {
                    String msg;
                    while ((msg = in.readLine()) != null) {
//                        if (!msg.startsWith(">")) {
//                            System.out.print("\033[1;32m> \033[0m");
//                        }
                        print(msg);
                        System.out.print("\033[1;32m> \033[0m");
                    }
                } catch (IOException ignored) {}
            }).start();

            System.out.print("\033[1;32m> \033[0m");
            while (sc.hasNextLine()) {
                String line = sc.nextLine();
                out.println(line);
                if (line.equalsIgnoreCase("/exit")) break;
                System.out.print("\033[1;32m> \033[0m");
            }
        } catch (IOException e) {
            System.err.println("Lỗi: " + e.getMessage());
        }
    }

    private void print(String msg) {
        String color = msg.startsWith("[PM") ? "\033[1;35m" :
            msg.startsWith("[TIN") ? "\033[1;31m" :
                msg.startsWith("[YÊU CẦU]") || msg.startsWith("[SERVER]") ? "\033[1;36m" :
                    msg.contains("tham gia") || msg.contains("rời") ? "\033[1;33m" : "\033[1;37m";
        System.out.println(color + msg + "\033[0m");
    }
}