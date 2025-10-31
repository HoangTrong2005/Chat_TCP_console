import java.io.*;
import java.net.*;

/**
 * ChatClientLevel6 - Improved Version
 * - Handshake loop đọc đầy đủ response
 * - In "You: " khi gửi
 * - Prompt ">" cho input
 * - Timestamp từ server sẽ hiển thị
 */
public class Client {

  private static final String SERVER_HOST = "localhost";
//  private static final String SERVER_HOST = "192.168.1.9";
  private static final int SERVER_PORT = 5000;

  public static void main(String[] args) {
    System.out.println("=== ChatClientLevel6 connecting to " + SERVER_HOST + ":" + SERVER_PORT + " ===");

    try (
        Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
        BufferedReader serverIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter serverOut = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader console = new BufferedReader(new InputStreamReader(System.in))
    ) {
      // --- Handshake ---
      String serverMsg = serverIn.readLine();
      System.out.println(serverMsg);
      boolean registered = false;

      while (!registered) {
        System.out.print("Enter username: ");
        String username = console.readLine();
        if (username == null) return;
        serverOut.println(username);

        // Đọc tất cả response từ server cho đến khi thấy registered hoặc error
        while (serverIn.ready() || !registered) {
          String response = serverIn.readLine();
          if (response == null) {
            System.out.println("[CLIENT] Server closed.");
            return;
          }
          System.out.println(response);
          if (response.contains("registered") || response.contains("Username")) {
            if (response.contains("registered")) registered = true;
            // Nếu invalid, loop lại không break
          }
        }
      }

      // --- Listener thread ---
      Thread listener = new Thread(() -> {
        try {
          String msg;
          while ((msg = serverIn.readLine()) != null) {
            System.out.println("\r" + msg); // Override prompt nếu cần
          }
        } catch (IOException e) {
          System.out.println("[CLIENT] Connection closed.");
        }
      });
      listener.setDaemon(true);
      listener.start();

      // --- Main send loop ---
      String input;
      System.out.print("> ");
      while ((input = console.readLine()) != null) {
        if (input.trim().isEmpty()) continue;
        serverOut.println(input);

        // In ngay cho user thấy (trừ quit để tránh double)
        if (!input.startsWith("/quit")) {
          System.out.println("You: " + input);
        }

        if (input.equalsIgnoreCase("/quit")) {
          break;
        }
        System.out.print("> "); // Prompt
      }

      System.out.println("[CLIENT] Exiting. Goodbye.");
      listener.join(1000); // Chờ listener die

    } catch (IOException e) {
      System.err.println("[CLIENT] Error: " + e.getMessage());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}