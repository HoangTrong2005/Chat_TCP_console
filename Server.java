package chattcp;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private final int SERVER_PORT = 1433;
    private ServerSocket serverSocket;
    // Danh sach client: username -> ClientHandler (An toan cho da luong)
    private Map<String, ClientHandler> clients = new ConcurrentHashMap<>();

    public Server() {
        try {
            serverSocket = new ServerSocket(SERVER_PORT);
            System.out.println("✅ Server dang chay tai cong " + SERVER_PORT);

            // Luồng chờ kết nối client mới
            new Thread(() -> {
                while (true) {
                    try {
                        Socket socket = serverSocket.accept();
                        System.out.println("🔌 Client moi ket noi: " + socket.getRemoteSocketAddress());

                        // Tao va chay ClientHandler tren luong moi
                        ClientHandler handler = new ClientHandler(socket, this);
                        new Thread(handler).start();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

            // Menu dieu khien Server (chay tren luong main)
            handleServerCommand();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Ham xu ly lenh server tu console
    private void handleServerCommand() {
        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.println("\n========== SERVER MENU ==========");
            System.out.println("1. Danh sach clients dang online");
            System.out.println("2. Gui tin nhan den 1 client");
            System.out.println("3. Gui broadcast cho tat ca client");
            System.out.println("4. Thoat server");
            System.out.print("Chon: ");
            String choice = sc.nextLine();

            switch (choice) {
                case "1":
                    listClientsOnServerConsole();
                    break;
                case "2":
                    System.out.print("Nhap ten client: ");
                    String name = sc.nextLine();
                    System.out.print("Nhap noi dung: ");
                    String msg = sc.nextLine();
                    sendPrivateMessage("SERVER", name, msg);
                    break;
                case "3":
                    System.out.print("Nhap tin nhan broadcast: ");
                    String bmsg = sc.nextLine();
                    broadcastMessage("[SERVER_BROADCAST] " + bmsg, null); // null = gui cho tat ca
                    break;
                case "4":
                    System.out.println("Dong server...");
                    System.exit(0);
                    break;
                default:
                    System.out.println("❌ Lua chon khong hop le!");
            }
        }
    }

    /**
     * Gui tin nhan cho tat ca client (co the loai tru nguoi gui).
     * @param message Tin nhan de gui.
     * @param senderUsername Ten cua nguoi gui (de khong gui lai cho ho), null neu la server.
     */
    public void broadcastMessage(String message, String senderUsername) {
        for (ClientHandler client : clients.values()) {
            String currentUsername = client.getUsername();

            // FIX: Kiem tra null de tranh race condition khi client chua login xong
            if (currentUsername == null) {
                continue;
            }

            // Khong gui lai tin nhan cho chinh nguoi gui
            if (senderUsername == null || !currentUsername.equals(senderUsername)) {
                client.sendMessage(message);
            }
        }
    }

    /**
     * Gui tin nhan rieng tu user nay den user khac.
     */
    public void sendPrivateMessage(String fromUser, String toUser, String message) {
        ClientHandler receiver = clients.get(toUser);
        if (receiver != null) {
            // 1. Gui cho nguoi nhan
            receiver.sendMessage("[PM tu " + fromUser + "]: " + message);

            // 2. Gui phan hoi cho nguoi gui (neu nguoi gui khong phai SERVER)
            if (!fromUser.equals("SERVER")) {
                ClientHandler sender = clients.get(fromUser);
                if (sender != null) {
                    sender.sendMessage("[PM den " + toUser + "]: " + message);
                }
            }
            System.out.println("✅ [PM] " + fromUser + " -> " + toUser);
        } else {
            // 3. Bao loi neu khong tim thay user
            if (!fromUser.equals("SERVER")) {
                ClientHandler sender = clients.get(fromUser);
                if (sender != null) {
                    sender.sendMessage("[SERVER] Loi: Khong tim thay user '" + toUser + "' hoac user da offline.");
                }
            }
            System.out.println("❌ [PM] Khong tim thay user " + toUser);
        }
    }

    /**
     * Gui danh sach user hien tai cho mot client cu the.
     */
    public void sendClientList(String toUser) {
        ClientHandler receiver = clients.get(toUser);
        if (receiver != null) {
            String userList = String.join(", ", clients.keySet());
            receiver.sendMessage("[SERVER] Users online (" + clients.size() + "): " + userList);
        }
    }

    /**
     * In danh sach client ra console cua SERVER.
     */
    public void listClientsOnServerConsole() {
        if (clients.isEmpty()) {
            System.out.println("⚠️ Chua co client nao online.");
        } else {
            System.out.println("👥 Danh sach client dang ket noi (" + clients.size() + "):");
            for (String name : clients.keySet()) {
                System.out.println(" - " + name);
            }
        }
    }

    /**
     * Dang ky client moi khi ho login thanh cong.
     * @return true neu dang ky thanh cong, false neu ten trung.
     */
    public boolean registerClient(String username, ClientHandler handler) {
        // synchronized de dam bao kiem tra va them la mot hanh dong nguyen tu
        synchronized (clients) {
            if (clients.containsKey(username)) {
                return false; // Ten da ton tai
            }
            clients.put(username, handler);
            return true;
        }
    }

    /**
     * Xoa client khoi danh sach khi ho ngat ket noi.
     */
    public void removeClient(String username) {
        if (username == null) return;

        clients.remove(username);
        System.out.println("❌ Client " + username + " da ngat ket noi.");
        broadcastMessage("📢 " + username + " da roi khoi phong.", null); // Thong bao cho moi nguoi
    }

    public static void main(String[] args) {
        new Server();
    }
}