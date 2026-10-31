package iuh.fit.springai.controllers;
import iuh.fit.springai.services.*;
import iuh.fit.springai.entities.User;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import java.math.BigDecimal;
import java.util.*;
@RestController @RequiredArgsConstructor @RequestMapping("/api/v1/chat")
public class AssistantApiController {
 private final AssistantService assistant;private final AccountService accounts;private final ChatConversation conversation;private final RequestBudget budget;
 public record ChatInput(@NotBlank @Size(max=2000) String message,@DecimalMin("0.01") @Digits(integer=12,fraction=2) BigDecimal maxPrice,Boolean inStock){}
 private User bind(Authentication auth,HttpSession session){
  if(auth==null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken){conversation.bind("anonymous:"+TrackingService.anonymousId(session));return null;}
  var user=accounts.current(auth);conversation.bind("user:"+user.getId());return user;
 }
 @GetMapping public Map<String,Object> history(Authentication auth,HttpSession session){bind(auth,session);return Map.of("messages",conversation.history(),"aiEnabled",assistant.enabled());}
 @PostMapping public AssistantService.Reply chat(@Valid @RequestBody ChatInput input,Authentication auth,HttpSession session){
  budget.check(session,"chat",15);User user=bind(auth,session);
  synchronized(session){
   var reply=assistant.reply(input.message(),user,conversation.history(),input.maxPrice(),Boolean.TRUE.equals(input.inStock()));
   conversation.add(new ChatConversation.Message("user",input.message(),List.of(),List.of(),""));
   conversation.add(new ChatConversation.Message("assistant",reply.text(),reply.products(),reply.orders(),reply.mode()));
   return reply;
  }
 }
 @DeleteMapping @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
 public void clear(Authentication auth,HttpSession session){bind(auth,session);conversation.clear();}
}
