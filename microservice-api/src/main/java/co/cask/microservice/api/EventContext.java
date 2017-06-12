package co.cask.microservice.api;

/**
 * Class description here.
 */
public class EventContext {
  private String namespace;
  private String queue;
  private String messageId;

  public EventContext(String namespace, String queue, String messageId) {
    this.namespace = namespace;
    this.queue = queue;
    this.messageId = messageId;
  }

  public String getNamespace() {
    return namespace;
  }

  public String getQueue() {
    return queue;
  }

  public String getMessageId() {
    return messageId;
  }
}
