
public class User {
  private final String name; // ten user
  private String chatWith; // ten user khac chat rieng
  private String pending; // cho xac nhan

  public User(String name) { this.name = name; }
  public String getName() { return name; }
  public String getChatWith() { return chatWith; }
  public void setChatWith(String chatWith) { this.chatWith = chatWith; }
  public String getPending() { return pending; }
  public void setPending(String pending) { this.pending = pending; }
  public void clearPending() { this.pending = null; }
}