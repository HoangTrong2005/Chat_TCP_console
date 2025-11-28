package chattcp;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private final int SERVER_PORT = 1436;
    private ServerSocket serverSocket;

    private final Map<String, ClientHandler> clients = new ConcurrentHashMap<>();
    private final Map<String, String> onlineInfo = new ConcurrentHashMap<>();
    private final List<String> chatHistory = Collections.synchronizedList(new ArrayList<>());

    public Server() {
        try {
            serverSocket = new ServerSocket(SERVER_PORT);
            System.out.println("Server đang chạy tại cổng " + SERVER_PORT);
            printMenu();

            // Luồng nhận client mới
            new Thread(() -> {
                while (!serverSocket.isClosed()) {
                    try {
                        Socket socket = serverSocket.accept();
                        ClientHandler handler = new ClientHandler(socket, this);
                        new Thread(handler).start();
                    } catch (IOException e) {
                        if (!serverSocket.isClosed()) e.printStackTrace();
                    }
                }
            }).start();

            handleServerCommands();

        } catch (IOException e) { e.printStackTrace(); }
    }

    // ===== SERVER CONSOLE =====
    private void handleServerCommands() {
        Scanner sc = new Scanner(System.in);
        String mode = null;
        String targetUser = null;

        while (true) {
            if (mode == null) System.out.print("SERVER> ");
            String line = sc.nextLine().trim();
            if (line.isEmpty()) continue;

            if (mode != null) {
                if (line.equalsIgnoreCase("/close")) {
                    System.out.println("Thoát chế độ " + mode);
                    mode = null;
                    targetUser = null;
                    continue;
                }
                switch (mode) {
                    case "/broadcast":
                        broadcastMessage("[SERVER_BROADCAST] " + line, null);
                        break;
                    case "/msg":
                        if (targetUser != null)
                            sendPrivateMessage("SERVER", targetUser, line);
                        break;
                }
                continue;
            }

            String[] parts = line.split(" ", 2);
            String command = parts[0].toLowerCase();
            String args = parts.length > 1 ? parts[1] : "";

            switch (command) {
                case "/menu": printMenu(); break;
                case "/online": listClients(System.out); break;
                case "/exit":
                    System.out.println("Đóng server...");
                    shutdownServer();
                    return;
                case "/broadcast":
                    mode = "/broadcast";
                    System.out.println("Bắt đầu broadcast (gõ /close để thoát)");
                    break;
                case "/msg":
                    if (!args.isEmpty()) {
                        targetUser = args;
                        if (!clients.containsKey(targetUser)) {
                            System.out.println("Không tìm thấy client: " + targetUser);
                            targetUser = null;
                        } else {
                            mode = "/msg";
                            System.out.println("Bắt đầu chat liên tục với " + targetUser + " (gõ /close để thoát)");
                        }
                    } else System.out.println("Cú pháp: /msg <tên_user>");
                    break;
                case "/kick":
                    if (!args.isEmpty()) kickUser(args);
                    else System.out.println("Cú pháp: /kick <tên_user>");
                    break;
                default:
                    System.out.println("Lệnh không hợp lệ! Gõ /menu để xem các lệnh.");
                    break;
            }
        }
    }

    private void printMenu() {
        System.out.println("\n================== SERVER MENU ==================");
        System.out.println("/online      ➜ Xem danh sách clients online (tên + IP)");
        System.out.println("/msg <user>  ➜ Chat riêng liên tục với client (gõ /close để thoát)");
        System.out.println("/broadcast   ➜ Gửi broadcast tới tất cả client");
        System.out.println("/kick <user> ➜ Kick client khỏi server");
        System.out.println("/exit        ➜ Thoát server");
        System.out.println("/menu        ➜ Mở menu");
        System.out.println("=================================================");
    }

    // ===== QUẢN LÝ CLIENT =====
    public boolean registerClient(String username, ClientHandler handler) {
        Socket sock = handler.getSocket();
        String ip = (sock != null && sock.getRemoteSocketAddress() != null)
                ? sock.getRemoteSocketAddress().toString()
                : "Unknown";

        synchronized (clients) {
            if (clients.containsKey(username)) return false;
            clients.put(username, handler);
            onlineInfo.put(username, ip);
        }

        broadcastMessage("📢 " + username + " đã tham gia phòng.", null);
        return true;
    }

    public void removeClient(String username) {
        if (username == null) return;
        clients.remove(username);
        onlineInfo.remove(username);
        broadcastMessage("📢 " + username + " đã rời phòng.", null);
    }

    public void kickUser(String username) {
        ClientHandler client = clients.get(username);
        if (client != null) {
            client.sendMessage("[SERVER_KICK]");
            client.kick();
            System.out.println("Client " + username + " đã bị kick!");
        } else {
            System.out.println("Không tìm thấy client: " + username);
        }
    }

    // ===== CHAT =====
    public void broadcastMessage(String message, String senderUsername) {
        saveMessageToHistory(message);
        System.out.println("[BROADCAST] " + message);
        for (ClientHandler client : clients.values()) {
            String currentUsername = client.getUsername();
            if (currentUsername == null) continue;
            if (senderUsername == null || !currentUsername.equals(senderUsername))
                client.sendMessage(message);
        }
    }

    public void sendPrivateMessage(String fromUser, String toUser, String message) {
        ClientHandler receiver = clients.get(toUser);
        if (receiver != null) {
            receiver.sendMessage("[" + fromUser + "]: " + message);
            System.out.println("[PRIVATE][" + fromUser + " -> " + toUser + "]: " + message);
        }
    }

    public User getUser(String username) {
        ClientHandler handler = clients.get(username);
        return handler != null ? handler.user : null;
    }

    // Dành cho **client**: chỉ in tên
    public void listClientNames(PrintWriter out) {
        if (clients.isEmpty()) {
            out.println("[SERVER] Chưa có client nào online.");
            return;
        }
        out.println("=== Clients online ===");
        for (String username : clients.keySet()) {
            out.println(" - " + username);
        }
    }

    // Dành cho **server console**: tên + IP
    public void listClients(Object outObj) {
        if (clients.isEmpty()) {
            if (outObj instanceof PrintWriter) ((PrintWriter) outObj).println("[SERVER] Chưa có client nào online.");
            else System.out.println("Chưa có client nào online.");
            return;
        }

        if (outObj instanceof PrintWriter) {
            PrintWriter out = (PrintWriter) outObj;
            out.println("=== Clients online ===");
            for (String username : clients.keySet()) {
                String ip = onlineInfo.getOrDefault(username, "Unknown");
                out.println(" - " + username + " | IP: " + ip);
            }
        } else {
            System.out.println("=== Clients online ===");
            for (String username : clients.keySet()) {
                String ip = onlineInfo.getOrDefault(username, "Unknown");
                System.out.println(" - " + username + " | IP: " + ip);
            }
        }
    }

    private void saveMessageToHistory(String message) {
        chatHistory.add(message);
        try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("chat_history.txt", true)))) {
            out.println(message);
        } catch (IOException e) { e.printStackTrace(); }
    }

    public List<String> getChatHistory() { return chatHistory; }

    public void receiveFromClient(String fromUser, String message) {
        String formatted = "[CLIENT->SERVER][" + fromUser + "]: " + message;
        System.out.println(formatted);
        saveMessageToHistory(formatted);

        ClientHandler client = clients.get(fromUser);
        if (client != null) {
            client.sendMessage("[SERVER] Đã nhận: " + message);
        }
    }

    // ===== TẮT SERVER =====
    private void shutdownServer() {
        for (ClientHandler client : clients.values()) {
            client.sendMessage("[SERVER] Server đóng. Đang thoát...");
            client.kick();
        }
        clients.clear();
        onlineInfo.clear();

        try {
            serverSocket.close();
        } catch (IOException e) { e.printStackTrace(); }
        System.out.println("Server đã tắt.");
    }

    public static void main(String[] args) { new Server(); }
}
