public class User {
  private final String name; // ten nguoi dung
  private String chatWith; // dang chat rieng voi ai(ten)
  private String pending; // ai dang xin chat rieng voi minh

  public User(String name) { this.name = name; }
  public String getName() { return name; }
  public String getChatWith() { return chatWith; }
  public void setChatWith(String chatWith) { this.chatWith = chatWith; }
  public String getPending() { return pending; }
  public void setPending(String pending) { this.pending = pending; }
  public void clearPending() { this.pending = null; }
}