package iuh.fit.springai.services;
import iuh.fit.springai.entities.TrackingEvent;
import iuh.fit.springai.repositories.TrackingEventRepository;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import java.time.Instant;
import java.util.UUID;
@Service @RequiredArgsConstructor
public class TrackingService {
 private final TrackingEventRepository events;private final AccountService accounts;private final ProductService products;
 public record EventInput(@NotNull UUID eventId,@NotNull TrackingEvent.Type eventType,
   @Positive Long productId,@Size(max=2000) String keyword,@Min(0) @Max(1000000) Integer resultCount,
   @Min(1) @Max(10000) Integer quantity,@Size(max=500) String path,@Size(max=200) String title,Instant timestamp){}
 public static String anonymousId(HttpSession session){
  synchronized(session){String id=(String)session.getAttribute("trackingAnonymousId");if(id==null){id=UUID.randomUUID().toString();session.setAttribute("trackingAnonymousId",id);}return id;}
 }
 public void record(EventInput input,Authentication auth,HttpSession session){
  if(events.existsById(input.eventId().toString()))return;
  var event=new TrackingEvent();event.setId(input.eventId().toString());event.setEventType(input.eventType());
  event.setAnonymousId(anonymousId(session));event.setReceivedAt(Instant.now());
  if(auth!=null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken))event.setUserId(accounts.current(auth).getId());
  if(input.timestamp()!=null && input.timestamp().isAfter(Instant.now().plusSeconds(300)))throw new IllegalArgumentException("Thời gian sự kiện không hợp lệ");
  event.setClientTimestamp(input.timestamp());
  if(input.path()!=null){if(!input.path().startsWith("/") || input.path().startsWith("//"))throw new IllegalArgumentException("Đường dẫn không hợp lệ");event.setPath(input.path().split("[?#]",2)[0]);}
  event.setTitle(input.title());
  if(input.eventType()==TrackingEvent.Type.PRODUCT_VIEW || input.eventType()==TrackingEvent.Type.ADD_TO_CART){
   if(input.productId()==null)throw new IllegalArgumentException("Thiếu mã sản phẩm");
   var p=products.findById(input.productId());event.setProductId(p.getId());
   if(input.eventType()==TrackingEvent.Type.ADD_TO_CART){
    if(event.getUserId()==null)throw new IllegalArgumentException("Cần đăng nhập để thêm giỏ hàng");
    if(input.quantity()==null)throw new IllegalArgumentException("Thiếu số lượng");
    event.setQuantity(input.quantity());event.setPrice(p.getPrice());
   }
  }
  if(input.eventType()==TrackingEvent.Type.SEARCH){
   if(input.keyword()==null || input.keyword().isBlank())throw new IllegalArgumentException("Thiếu từ khóa");
   event.setKeyword(input.keyword());event.setResultCount(input.resultCount());
  }
  events.save(event);
 }
}
