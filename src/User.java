package chattcp;

public class User {
  private final String username;
  private String chatWith = null;
  private String pendingRequest = null;

  public User(String username) {
    this.username = username;
  }

  public String getUsername() { return username; }
  public String getChatWith() { return chatWith; }
  public void setChatWith(String target) { this.chatWith = target; }
  public String getPendingRequest() { return pendingRequest; }
  public void setPendingRequest(String requester) { this.pendingRequest = requester; }
  public void clearPending() { this.pendingRequest = null; }
}