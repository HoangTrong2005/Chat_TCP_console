<<<<<<< Updated upstream
package chattcp;
=======
package ;
>>>>>>> Stashed changes

import java.io.*;
import java.net.Socket;
import java.net.SocketException;

<<<<<<< Updated upstream
public class ClientHandler implements Runnable {
    private Socket socket;
    private Server server;
    // Dung BufferedReader/PrintWriter de doc/ghi theo tung dong
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
=======
public class ClientHandler<user> implements Runnable {
    private final Socket socket;
    private final Server server;
    private final BufferedReader in;
    private final PrintWriter out;
    public User user;

    public ClientHandler(Socket socket, Server server) throws IOException {
        this.socket = socket;
        this.server = server;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);
>>>>>>> Stashed changes
    }

    public Socket getSocket() { return socket; }

    @Override
    public void run() {
        try {
<<<<<<< Updated upstream
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
=======
            login();
            showMenu();

            String msg;
            while ((msg = in.readLine()) != null) {
                if (msg.isBlank()) continue;

                if (msg.startsWith("/")) {
                    handleCmd(msg);
                } else {
                    // Chat riêng
                    if (user.getChatWith() != null) {
                        if (user.getChatWith().equalsIgnoreCase("SERVER")) {
                            // Chat client -> server
                            server.receiveFromClient(user.getName(), msg);
                        } else {
                            // Chat client -> client
                            server.sendPrivateMessage(user.getName(), user.getChatWith(), msg);
                        }
                    } else {
                        // Chat chung
                        String formatted = user.getName() + ": " + msg;
                        server.broadcastMessage(formatted, user.getName());
                    }
>>>>>>> Stashed changes
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
<<<<<<< Updated upstream
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
=======
            disconnect();
        } finally {
            close();
        }
    }

    private void login() throws IOException {
        out.println("[SERVER] Nhập tên:");
        while (true) {
            String name = in.readLine().trim();
            if (name.isEmpty() || name.equalsIgnoreCase("SERVER")) {
                out.println("[SERVER] Tên không hợp lệ.");
                continue;
            }
            if (server.registerClient(name, this)) {
                this.user = new User(name);
                out.println("[SERVER] Chào " + name + "! Dùng /help để xem menu.");
                return;
            }
            out.println("[SERVER] Tên '" + name + "' đã tồn tại.");
        }
    }

    private void showMenu() {
        out.println("\n========== MENU CLIENT ==========");
        out.println("/list       ➜ Xem danh sách client online");
        out.println("/to <tên>   ➜ Gửi yêu cầu chat riêng với client khác");
        out.println("/accept     ➜ Chấp nhận yêu cầu chat riêng");
        out.println("/deny       ➜ Từ chối yêu cầu chat riêng");
        out.println("/back       ➜ Thoát chat riêng về chat chung");
        out.println("/toserver   ➜ Chat riêng với server");
        out.println("/history    ➜ Xem lịch sử chat");
        out.println("/help       ➜ Xem lại menu");
        out.println("/exit       ➜ Thoát client");
        out.println("================================\n");
    }

    private void handleCmd(String cmd) throws IOException {
        if (cmd.equals("/list")) server.listClientNames(out);
        else if (cmd.equals("/help")) showMenu();
        else if (cmd.startsWith("/to ")) requestPM(cmd.split(" ", 2)[1]);
        else if (cmd.equals("/accept")) accept();
        else if (cmd.equals("/deny")) deny();
        else if (cmd.equals("/back")) back();
        else if (cmd.equals("/history")) sendChatHistory();
        else if (cmd.equals("/toserver")) {
            user.setChatWith("SERVER");
            out.println("[SERVER] Bạn đang chat riêng với server. /back để thoát.");
        }
        else if (cmd.equals("/exit")) exit();
        else out.println("[SERVER] Lệnh không hợp lệ. /help");
    }

    private void requestPM(String target) {
        if (user.getChatWith() != null) {
            out.println("[SERVER] Bạn đang chat riêng với " + user.getChatWith());
            return;
        }
        User t = server.getUser(target);
        if (t == null || t.getName().equals(user.getName())) {
            out.println("[SERVER] Không tìm thấy người nhận.");
            return;
        }
        t.setPending(user.getName());
        server.sendPrivateMessage(user.getName(), target,
                "[YÊU CẦU] " + user.getName() + " muốn chat riêng. /accept hoặc /deny");
        out.println("[SERVER] Đã gửi yêu cầu.");
    }

    private void accept() {
        String req = user.getPending();
        if (req == null) { out.println("[SERVER] Không có yêu cầu."); return; }
        User r = server.getUser(req);
        user.setChatWith(req);
        r.setChatWith(user.getName());
        user.clearPending();
        server.sendPrivateMessage("SERVER", req, user.getName() + " đã chấp nhận. Bắt đầu chat 2 chiều!");
        out.println("[SERVER] Chat 2 chiều với " + req + ". /back để thoát.");
    }

    private void deny() {
        String req = user.getPending();
        if (req != null) {
            server.sendPrivateMessage("SERVER", req, user.getName() + " đã từ chối.");
            user.clearPending();
        }
    }

    private void back() {
        String chatWith = user.getChatWith();
        if (chatWith != null) {
            if (!chatWith.equalsIgnoreCase("SERVER")) {
                User u = server.getUser(chatWith);
                if (u != null) u.setChatWith(null);
                server.sendPrivateMessage("SERVER", chatWith, user.getName() + " đã thoát chat riêng.");
            }
            user.setChatWith(null);
            out.println("[SERVER] Đã về chat chung.");
        }
    }

    private void sendChatHistory() {
        out.println("[SERVER] --- Lịch sử chat trước đó ---");
        for (String msg : server.getChatHistory()) {
            out.println(msg);
        }
        out.println("[SERVER] --- Kết thúc lịch sử chat ---");
    }

    public void exit() { disconnect(); }

    private void disconnect() {
        if (user != null) {
            server.removeClient(user.getName());
        }
    }

    public void sendMessage(String msg) { out.println(msg); }

    public String getUsername() { return user != null ? user.getName() : null; }

    public void kick() { sendMessage("[SERVER] Bạn đã bị kick!"); close(); }

    private void close() { try { socket.close(); } catch (IOException ignored) {} }
}
>>>>>>> Stashed changes
