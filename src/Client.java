import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
    private static final String IP = "localhost";  // IP chay tren 1 may.
//    private static final String IP = "<192.168.x.x>";  // Sua thanh IP cua may chay Server khi chay 2 may tro len ket noi cung mang.
    private static final int PORT = 1433;

    public static void main(String[] args) {
        new Client().run();
    }

    // Ham chinh cua client: ket noi va giao tiep voi server
    private void run() {
        try (Socket socket = new Socket(IP, PORT);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            Scanner sc = new Scanner(System.in)) {

            System.out.println("\033[1;36mKet noi thanh cong!\033[0m");

            // Thread rieng de nhan tin tu server
            new Thread(() -> {
                try {
                    String msg;
                    while ((msg = in.readLine()) != null) {
                        print(msg);                    // In tin nhan co mau
                        System.out.print("\033[1;32m> \033[0m"); // Dau nhac lenh
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
            System.err.println("Loi ket noi: " + e.getMessage());
        }
    }

    // In tin nhan voi mau phu hop
    private void print(String msg) {
        String color = msg.startsWith("[PM") ? "\033[1;35m" :
            msg.startsWith("[TIN") ? "\033[1;31m" :
                msg.startsWith("[YEU CAU]") || msg.startsWith("[SERVER]") ? "\033[1;36m" :
                    msg.contains("tham gia") || msg.contains("roi") ? "\033[1;33m" : "\033[1;37m";
        System.out.println(color + msg + "\033[0m");
    }
}