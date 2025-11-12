
import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private static final int PORT = 1433;
    private final ServerSocket serverSocket;
    private final Map<String, ClientHandler> clients = new HashMap<>();
    private final Map<String, User> users = new HashMap<>();

    public Server() throws IOException {
        serverSocket = new ServerSocket(PORT);
        System.out.println("\033[1;36m=== SERVER CHẠY TẠI CỔNG " + PORT + " ===\033[0m");
        accept();
        menu(); // Menu điều khiển
    }

    private void accept() {
        new Thread(() -> {
            while (true) {
                try {
                    Socket s = serverSocket.accept();
                    System.out.println("\nNew connection from " + s.getRemoteSocketAddress());
                    new Thread(new ClientHandler(s, this)).start();

                } catch (IOException e) {
                    if (!serverSocket.isClosed()) e.printStackTrace();
                }
            }
        }).start();
    }

    // === MENU SERVER ===
    private void menu() {
        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.println("\n\033[1;33m--- SERVER MENU ---\033[0m");
            System.out.println("1. Gửi tin chung");
            System.out.println("2. Gửi tin riêng (1 chiều)");
            System.out.println("3. Xem danh sách online");
            System.out.println("4. Thoát");
            System.out.print("\033[1;32mChọn: \033[0m");
            String choice = sc.nextLine();

            switch (choice) {
                case "1" -> sendBroadcast(sc);
                case "2" -> sendOneWayFromServer(sc);
                case "3" -> listOnline();
                case "4" -> { System.out.println("Đóng server..."); System.exit(0); }
                default -> System.out.println("\033[1;31mLựa chọn không hợp lệ!\033[0m");
            }
        }
    }

    private void sendBroadcast(Scanner sc) {
        System.out.print("Tin nhắn: ");
        String msg = sc.nextLine();
        log("\033[1;34m[BROADCAST] " + msg + "\033[0m");
        broadcast("[SERVER] " + msg, null);
    }

    private void sendOneWayFromServer(Scanner sc) {
        System.out.print("Người nhận: ");
        String to = sc.nextLine();
        System.out.print("Tin nhắn: ");
        String msg = sc.nextLine();
        log("\033[1;35m[ONE-WAY → " + to + "] " + msg + "\033[0m");
        sendOneWay("SERVER", to, msg);
    }

    private void listOnline() {
        if (users.isEmpty()) {
            System.out.println("\033[1;33mChưa có ai online.\033[0m");
        } else {
            System.out.println("\033[1;32mOnline (" + users.size() + "): " + String.join(", ", users.keySet()) + "\033[0m");
        }
    }

    // === QUẢN LÝ CLIENT ===
    public synchronized boolean add(String name, ClientHandler handler) {
        if (users.containsKey(name)) return false;
        User user = new User(name);
        users.put(name, user);
        clients.put(name, handler);
        log("\033[1;32m[+] " + name + " tham gia\033[0m");
        return true;
    }

    public synchronized void remove(User user) {
        if (user == null) return;
        String name = user.getName();
        clients.remove(name);
        users.remove(name);
        if (user.getChatWith() != null) {
            User p = getUser(user.getChatWith());
            if (p != null) p.setChatWith(null);
        }
        log("\033[1;31m[-] " + name + " đã rời\033[0m");
        broadcast(name + " đã rời phòng.", null);
    }

    // === GỬI TIN ===
    public void broadcast(String msg, String exclude) {
        log("\033[1;37m" + msg + "\033[0m");
        clients.values().stream()
            .filter(h -> exclude == null || !exclude.equals(h.user.getName()))
            .forEach(h -> h.send(msg));
    }

    public void send(User from, String msg) {
        String sender = from.getName();
        if (from.getChatWith() != null) {
            sendPM(sender, from.getChatWith(), msg);
        } else {
            broadcast(sender + ": " + msg, sender);
        }
    }

    private void sendPM(String from, String to, String msg) {
        log("\033[1;35m[PM] " + from + " → " + to + ": " + msg + "\033[0m");
        ClientHandler r = clients.get(to);
        if (r != null) {
            r.send("[PM từ " + from + "]: " + msg);
            if (!from.equals("SERVER")) clients.get(from).send("[PM đến " + to + "]: " + msg);
        }
    }

    public void sendOneWay(String from, String to, String msg) {
        log("\033[1;31m[TIN 1 CHIỀU] " + from + " → " + to + ": " + msg + "\033[0m");
        ClientHandler r = clients.get(to);
        if (r != null) r.send("[TIN MỘT CHIỀU từ " + from + "]: " + msg);
        else if (!from.equals("SERVER")) clients.get(from).send("[SERVER] Không tìm thấy '" + to + "'.");
    }

    public void send(String to, String msg) {
        ClientHandler h = clients.get(to);
        if (h != null) h.send(msg);
    }

    public void list(PrintWriter out) {
        out.println("[SERVER] Online (" + users.size() + "): " + String.join(", ", users.keySet()));
    }

    public User getUser(String name) { return users.get(name); }

    // === GHI LOG ĐẸP ===
    private void log(String msg) {
        System.out.println(msg);
    }

    public static void main(String[] args) {
        try { new Server(); } catch (IOException e) { System.err.println("Lỗi: " + e.getMessage()); }
    }
}