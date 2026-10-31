package iuh.fit.springai.services;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;
import java.util.*;
@Component @SessionScope
public class ChatConversation {
 public record Message(String role,String text,List<ProductSearchService.ProductCard> products,List<AssistantService.OrderCard> orders,String mode){}
 private String owner="";
 private final List<Message> messages=new ArrayList<>();
 public synchronized void bind(String owner){if(!this.owner.equals(owner)){messages.clear();this.owner=owner;}}
 public synchronized List<Message> history(){return List.copyOf(messages);}
 public synchronized void add(Message message){messages.add(message);while(messages.size()>16)messages.remove(0);}
 public synchronized void clear(){messages.clear();}
}
