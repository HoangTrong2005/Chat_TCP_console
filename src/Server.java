import java.io.*;
import java.net.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.*;

/**
 * ChatServerLevel6 - Improved Version
 * - Thêm timestamp cho message
 * - Sanitize username
 * - Cải thiện sender thread (handle interrupt)
 * - Giới hạn queue size (nếu cần, nhưng giữ unbounded cho đơn giản)
 */
public class Server {
  private static final int PORT = 5000;
  private static final ConcurrentMap<String, ClientHandler> users = new ConcurrentHashMap<>();
  private static final ExecutorService pool = Executors.newCachedThreadPool();
  private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

  public static void main(String[] args) {
    System.out.println("=== ChatServer running on port " + PORT + " ===");
    // Shutdown hook for Ctrl+C
    Runtime.getRuntime().addShutdownHook(new Thread(Server::shutdown));

    try (ServerSocket serverSocket = new ServerSocket(PORT)) {
      while (!Thread.interrupted()) {
        Socket socket = serverSocket.accept();
        pool.execute(() -> handleNewConnection(socket));
      }
    } catch (IOException e) {
      System.err.println("[SERVER] Error: " + e.getMessage());
    } finally {
      shutdown();
    }
  }

  private static void handleNewConnection(Socket socket) {
    System.out.println("[SERVER] Connection from " + socket.getRemoteSocketAddress());
    try {
      BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

      out.println("[SERVER] Welcome! Please enter your username (alphanumeric, 3-20 chars):");
      String username;

      while (true) {
        username = in.readLine();
        if (username == null) {
          socket.close();
          return;
        }
        username = username.trim();
        if (username.isEmpty() || !username.matches("^[a-zA-Z0-9]{3,20}$")) {
          out.println("[SERVER] Invalid username. Alphanumeric, 3-20 chars. Try again:");
          continue;
        }
        if (users.containsKey(username)) {
          out.println("[SERVER] Username already taken. Try another:");
          continue;
        }

        ClientHandler handler = new ClientHandler(username, socket);
        users.put(username, handler);
        pool.execute(handler);

        broadcastServerMessage("→ " + username + " has joined the chat.");
        out.println("[SERVER] You are registered as '" + username + "'. Use /quit to exit.");
        out.println("[SERVER] Users online: " + String.join(", ", users.keySet()));
        System.out.println("[SERVER] Registered user: " + username + " from " + socket.getRemoteSocketAddress());
        break;
      }

    } catch (IOException e) {
      System.out.println("[SERVER] Error during handshake: " + e.getMessage());
    }
  }

  private static void broadcastServerMessage(String message) {
    String timedMsg = getTimestamp() + " [SERVER] " + message;
    users.forEach((u, h) -> h.send(timedMsg));
    System.out.println(timedMsg);
  }

  private static void broadcastUserMessage(String from, String message) {
    String timedMsg = getTimestamp() + " " + from + ": " + message;
    users.forEach((u, h) -> { if (!u.equals(from)) h.send(timedMsg); }); // Không gửi lại cho sender
    System.out.println("[BROADCAST] " + timedMsg);
  }

  private static boolean sendPrivate(String from, String toUser, String message) {
    ClientHandler target = users.get(toUser);
    if (target == null) return false;

    String timedMsg = getTimestamp() + " [PRIVATE] " + from + " -> you: " + message;
    target.send(timedMsg);
    // gửi private
    users.get(from).send(getTimestamp() + " [PRIVATE] you -> " + toUser + ": " + message);
    System.out.println("[PRIVATE] " + from + " -> " + toUser + ": " + message);
    return true;
  }

  private static String getTimestamp() {
    return "[" + LocalTime.now().format(TIME_FMT) + "]";
  }

  private static void removeUser(String username) {
    users.remove(username);
    broadcastServerMessage("← " + username + " has left the chat.");
    System.out.println("[SERVER] Removed user: " + username);
  }

  private static void shutdown() {
    pool.shutdownNow();
    users.forEach((u, h) -> h.close());
    users.clear();
    System.out.println("[SERVER] Shutdown complete.");
  }

  // ===== INNER CLASS =====
  private static class ClientHandler implements Runnable {
    private final String username;
    private final Socket socket;
    private BufferedReader in;
    private final BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();
    private volatile boolean running = true;

    ClientHandler(String username, Socket socket) {
      this.username = username;
      this.socket = socket;
    }

    @Override
    public void run() {
      try {
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        Thread senderThread = new Thread(this::processOutgoingMessages);
        senderThread.setDaemon(true);
        senderThread.start();

        String line;
        while (running && (line = in.readLine()) != null) {
          line = line.trim();
          if (line.isEmpty()) continue;

          if (line.startsWith("/")) {
            handleCommand(line);
          } else {
            broadcastUserMessage(username, line);
          }
        }
      } catch (IOException e) {
        System.out.println("[SERVER] Connection lost with " + username + ": " + e.getMessage());
      } finally {
        cleanup();
      }
    }

    private void handleCommand(String line) {
      try {
        if (line.equalsIgnoreCase("/quit")) {
          send(getTimestamp() + " [SERVER] Bye!");
          cleanup();
        } else if (line.equalsIgnoreCase("/users")) {
          send(getTimestamp() + " [SERVER] Users online: " + String.join(", ", users.keySet()));
        } else if (line.toLowerCase().startsWith("/msg ")) {
          String[] parts = line.split(" ", 3);
          if (parts.length < 3) {
            send(getTimestamp() + " [SERVER] Usage: /msg <user> <message>");
          } else {
            String target = parts[1];
            String msg = parts[2];
            if (!sendPrivate(username, target, msg)) {
              send(getTimestamp() + " [SERVER] User '" + target + "' not found.");
            }
          }
        } else {
          send(getTimestamp() + " [SERVER] Unknown command: " + line);
        }
      } catch (Exception e) {
        send(getTimestamp() + " [SERVER] Command error: " + e.getMessage());
      }
    }

    void send(String message) {
      if (!messageQueue.offer(message)) {
        System.out.println("[SERVER] Queue full for " + username + ", dropping message.");
      }
    }

    private void processOutgoingMessages() {
      try (PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
        while (running) {
          String message = messageQueue.take();
          if (!running) break;
          out.println(message);
          out.flush(); // Đảm bảo gửi ngay
        }
      } catch (IOException e) {
        System.out.println("[SERVER] IO error in sender for " + username);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        System.out.println("[SERVER] Sender interrupted for " + username);
      }
    }

    void cleanup() {
      running = false;
      try { if (in != null) in.close(); } catch (IOException ignored) {}
      try { socket.close(); } catch (IOException ignored) {}
      removeUser(username);
    }

    void close() {
      cleanup();
    }
  }
}