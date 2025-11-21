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
            login(); // Yeu cau nhap ten
            String msg;
            while ((msg = in.readLine()) != null) {
                if (msg.isBlank()) continue;
                if (msg.startsWith("/")) handleCmd(msg); // Xu ly lenh
                else server.send(user, msg);             // Gui tin binh thuong
            }
        } catch (IOException e) {
            server.remove(user);
        } finally {
            close();
        }
    }

    // Dang nhap nguoi dung
    private void login() throws IOException {
        out.println("[SERVER] Nhap ten:");
        while (true) {
            String name = in.readLine().trim();
            if (name.isEmpty() || name.equalsIgnoreCase("SERVER")) {
                out.println("[SERVER] Ten khong hop le.");
                continue;
            }
            if (server.add(name, this)) {
                this.user = server.getUser(name);
                out.println("[SERVER] Chao " + name + "! Dung /help de xem lenh.");
                server.broadcast(name + " da tham gia.", null);
                return;
            }
            out.println("[SERVER] Ten '" + name + "' da ton tai.");
        }
    }

    // Xu ly cac lenh bat dau bang /
    private void handleCmd(String cmd) {
        if (cmd.equals("/list")) server.list(out);
        else if (cmd.equals("/help"))
            out.println("[SERVER] /to <ten>, /send <ten> <tin>, /accept, /deny, /back, /list, /exit");
        else if (cmd.startsWith("/to ")) requestPM(cmd.split(" ", 2)[1]);
        else if (cmd.startsWith("/send ")) sendOneWay(cmd);
        else if (cmd.equals("/accept")) accept();
        else if (cmd.equals("/deny")) deny();
        else if (cmd.equals("/back")) back();
        else out.println("[SERVER] Lenh khong hop le. Go /help.");
    }

    // Xin chat rieng voi nguoi khac
    private void requestPM(String target) {
        if (user.getChatWith() != null) {
            out.println("[SERVER] Dang chat rieng.");
            return;
        }
        User t = server.getUser(target);
        if (t == null || t.getName().equals(user.getName())) {
            out.println("[SERVER] Khong tim thay.");
            return;
        }
        t.setPending(user.getName());
        server.send(t.getName(), "[YEU CAU] " + user.getName() + " muon chat rieng. Go /accept hoac /deny");
        out.println("[SERVER] Da gui yeu cau.");
    }

    // Gui tin 1 chieu
    private void sendOneWay(String cmd) {
        String[] p = cmd.split(" ", 3);
        if (p.length < 3) {
            out.println("[SERVER] Sai: /send <ten> <tin>");
            return;
        }
        server.sendOneWay(user.getName(), p[1], p[2]);
    }

    // Chap nhan yeu cau chat rieng
    private void accept() {
        String req = user.getPending();
        if (req == null) {
            out.println("[SERVER] Khong co yeu cau.");
            return;
        }
        User r = server.getUser(req);
        user.setChatWith(req);
        r.setChatWith(user.getName());
        user.clearPending();
        server.send(req, "[SERVER] " + user.getName() + " da chap nhan.");
        out.println("[SERVER] Dang chat voi " + req + ". Go /back de thoat.");
    }

    // Tu choi yeu cau
    private void deny() {
        String req = user.getPending();
        if (req != null) {
            server.send(req, "[SERVER] " + user.getName() + " da tu choi.");
            user.clearPending();
        }
    }

    // Tro ve chat chung
    private void back() {
        String p = user.getChatWith();
        if (p != null) {
            User pu = server.getUser(p);
            if (pu != null) {
                pu.setChatWith(null);
                server.send(p, "[SERVER] " + user.getName() + " da thoat.");
            }
            user.setChatWith(null);
            out.println("[SERVER] Da ve chat chung.");
        }
    }

    public void send(String msg) {
        out.println(msg);
    }

    private void close() {
        try { socket.close(); } catch (IOException ignored) {}
    }
}