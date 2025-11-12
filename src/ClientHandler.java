
import java.io.*;
import java.net.*;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final Server server;
    private final BufferedReader in;
    private final PrintWriter out;
    protected User user;

    public ClientHandler(Socket socket, Server server) throws IOException {
        this.socket = socket;
        this.server = server;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);
    }

    @Override
    public void run() {
        try {
            login();
            String msg;
            while ((msg = in.readLine()) != null) {
                if (msg.isBlank()) continue;
                if (msg.startsWith("/")) handleCmd(msg);
                else server.send(user, msg);
            }
        } catch (IOException e) {
            server.remove(user);
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
            if (server.add(name, this)) {
                this.user = server.getUser(name);
                out.println("[SERVER] Chào " + name + "! Dùng /help để xem lệnh.");
                server.broadcast(name + " đã tham gia.", null);
                return;
            }
            out.println("[SERVER] Tên '" + name + "' đã tồn tại.");
        }
    }

    private void handleCmd(String cmd) {
        if (cmd.equals("/list")) server.list(out);
        else if (cmd.equals("/help")) out.println("[SERVER] /to <tên>, /send <tên> <tin>, /accept, /deny, /back, /list, /exit");
        else if (cmd.startsWith("/to ")) requestPM(cmd.split(" ", 2)[1]);
        else if (cmd.startsWith("/send ")) sendOneWay(cmd);
        else if (cmd.equals("/accept")) accept();
        else if (cmd.equals("/deny")) deny();
        else if (cmd.equals("/back")) back();
        else out.println("[SERVER] Lệnh không hợp lệ. Gõ /help.");
    }

    private void requestPM(String target) {
        if (user.getChatWith() != null) { out.println("[SERVER] Đang chat riêng."); return; }
        User t = server.getUser(target);
        if (t == null || t.getName().equals(user.getName())) { out.println("[SERVER] Không tìm thấy."); return; }
        t.setPending(user.getName());
        server.send(t.getName(), "[YÊU CẦU] " + user.getName() + " muốn chat riêng. Gõ /accept hoặc /deny");
        out.println("[SERVER] Đã gửi yêu cầu.");
    }

    private void sendOneWay(String cmd) {
        String[] p = cmd.split(" ", 3);
        if (p.length < 3) { out.println("[SERVER] Sai: /send <tên> <tin>"); return; }
        server.sendOneWay(user.getName(), p[1], p[2]);
    }

    private void accept() {
        String req = user.getPending();
        if (req == null) { out.println("[SERVER] Không có yêu cầu."); return; }
        User r = server.getUser(req);
        user.setChatWith(req);
        r.setChatWith(user.getName());
        user.clearPending();
        server.send(req, "[SERVER] " + user.getName() + " đã chấp nhận.");
        out.println("[SERVER] Đang chat với " + req + ". Gõ /back để thoát.");
    }

    private void deny() {
        String req = user.getPending();
        if (req != null) {
            server.send(req, "[SERVER] " + user.getName() + " đã từ chối.");
            user.clearPending();
        }
    }

    private void back() {
        String p = user.getChatWith();
        if (p != null) {
            User pu = server.getUser(p);
            if (pu != null) {
                pu.setChatWith(null);
                server.send(p, "[SERVER] " + user.getName() + " đã thoát.");
            }
            user.setChatWith(null);
            out.println("[SERVER] Đã về chat chung.");
        }
    }

    public void send(String msg) {
        out.println(msg);
//        out.print("\033[1;32m> \033[0m");
    }
    private void close() { try { socket.close(); } catch (IOException ignored) {} }
}