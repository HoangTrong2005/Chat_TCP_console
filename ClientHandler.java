package chattcp;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;

public class ClientHandler implements Runnable {
    private Socket socket;
    private Server server;
    // CAI TIEN: Dung BufferedReader/PrintWriter de doc/ghi theo tung dong
    private BufferedReader reader;
    private PrintWriter writer;
    private String username;

    public ClientHandler(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
        try {
            // Tao luong doc (tu client)
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            // Tao luong ghi (den client), true = autoFlush (tu dong gui ngay khi goi println)
            this.writer = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getUsername() {
        return this.username;
    }

    @Override
    public void run() {
        try {
            // Buoc 1: Yeu cau va xu ly Login
            writer.println("[SERVER] Chao mung! Vui long nhap ten dang nhap:");
            while (true) {
                String name = reader.readLine();
                if (name == null || name.trim().isEmpty() || name.trim().equalsIgnoreCase("SERVER")) {
                    writer.println("[SERVER] Ten khong hop le. Vui long chon ten khac:");
                    continue;
                }

                name = name.trim();

                // Goi ham dang ky cua Server
                if (server.registerClient(name, this)) {
                    this.username = name; // Gan ten khi dang ky thanh cong
                    writer.println("[SERVER] Dang nhap thanh cong voi ten " + username);
                    writer.println("[SERVER] --- HUONG DAN ---");
                    writer.println("[SERVER] Go /list de xem ai dang online.");
                    writer.println("[SERVER] Go /w <ten_user> <tin_nhan> de gui tin rieng.");
                    writer.println("[SERVER] Go /exit de thoat.");

                    // Thong bao cho cac client khac
                    server.broadcastMessage("📢 " + username + " da tham gia phong chat!", this.username);
                    System.out.println("👤 " + username + " da dang nhap.");
                    break; // Thoat vong lap login
                } else {
                    writer.println("[SERVER] Ten '" + name + "' da duoc su dung. Vui long chon ten khac:");
                }
            }

            // Buoc 2: Lang nghe tin nhan (chat hoac lenh) tu client
            String clientMessage;
            while ((clientMessage = reader.readLine()) != null) {

                if (clientMessage.trim().isEmpty()) continue;

                System.out.println("💬 [" + username + "]: " + clientMessage);

                // *** XU LY LENH ***
                if (clientMessage.startsWith("/list")) {
                    server.sendClientList(this.username);
                }
                else if (clientMessage.startsWith("/w ")) {
                    // Cu phap: /w ten_nguoi_nhan Noi dung tin nhan
                    try {
                        String[] parts = clientMessage.split(" ", 3); // Tach lam 3 phan
                        String receiver = parts[1];
                        String privateMessage = parts[2];

                        server.sendPrivateMessage(this.username, receiver, privateMessage);

                    } catch (Exception e) {
                        writer.println("[SERVER] Cu phap sai. Dung: /w <ten_user> <tin_nhan>");
                    }
                }
                else if (clientMessage.equalsIgnoreCase("/exit")) {
                    break; // Thoat vong lap while
                }
                else {
                    // Khong phai lenh thi broadcast (tin nhan cong khai)
                    server.broadcastMessage(username + ": " + clientMessage, this.username);
                }
            }

        } catch (SocketException e) {
            System.out.println("⚠️ Client " + (username != null ? username : "[CHUA DANG NHAP]") + " da ngat ket noi dot ngot.");
        } catch (IOException e) {
            System.out.println("Loi IO voi client " + username + ": " + e.getMessage());
        } finally {
            // Don dep khi client thoat
            server.removeClient(username);
            try {
                if (socket != null) socket.close();
            } catch (IOException ignored) {}
        }
    }

    // Ham gui tin cho client nay (an toan vi dung PrintWriter)
    public void sendMessage(String message) {
        writer.println(message);
    }
}