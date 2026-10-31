package iuh.fit.springai.services;
import iuh.fit.springai.entities.*;
import iuh.fit.springai.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.LocalDate;
import java.util.*;
@Service @RequiredArgsConstructor @Transactional
public class OrderService {
 private final OrderRepository orders;
 private final ProductRepository products;
 public List<Order> visible(User user){return "ADMIN".equals(user.getRole())?orders.findByActiveTrueOrderByIdDesc():orders.findByCustomerIdAndActiveTrueOrderByIdDesc(user.getCustomer().getId());}
 public Order detail(Long id,User user){
  var o=orders.findByIdAndActiveTrue(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
  if(!"ADMIN".equals(user.getRole()) && (user.getCustomer()==null || !o.getCustomer().getId().equals(user.getCustomer().getId())))throw new AccessDeniedException("Không được truy cập đơn hàng này");
  return o;
 }
 public Order checkout(User user,Map<Long,Integer> items,LocalDate date){
  if(user.getCustomer()==null || !user.getCustomer().isActive())throw new IllegalArgumentException("Cần hồ sơ khách hàng hoạt động");
  if(date==null || date.isBefore(LocalDate.now()))throw new IllegalArgumentException("Ngày đặt hàng phải từ hôm nay trở đi");
  if(items.isEmpty())throw new IllegalArgumentException("Giỏ hàng đang trống");
  var order=new Order();order.setDate(date);order.setCustomer(user.getCustomer());
  for(var entry:new TreeMap<>(items).entrySet()){
   var p=products.lockById(entry.getKey()).orElseThrow(()->new IllegalArgumentException("Sản phẩm không tồn tại"));
   int count=entry.getValue();
   if(!p.isActive() || !p.getCategory().isActive() || count<1 || count>p.getQuantity())throw new IllegalArgumentException("Không đủ tồn kho: "+p.getName());
   p.setQuantity(p.getQuantity()-count);
   var line=new OrderLine();line.setOrder(order);line.setProduct(p);line.setAmount(count);line.setPurchasePrice(p.getPrice());order.getOrderLines().add(line);
  }
  return orders.save(order);
 }
 public void update(Long id,LocalDate date,String status,User user){
  if(!"ADMIN".equals(user.getRole()))throw new AccessDeniedException("Admin only");
  if(date==null || date.isBefore(LocalDate.now()))throw new IllegalArgumentException("Ngày phải từ hôm nay trở đi");
  if(!Set.of("PLACED","PROCESSING","COMPLETED").contains(status))throw new IllegalArgumentException("Trạng thái không hợp lệ");
  var o=detail(id,user);o.setDate(date);o.setStatus(status);
 }
 public void lock(Long id,User user){if(!"ADMIN".equals(user.getRole()))throw new AccessDeniedException("Admin only");detail(id,user).setActive(false);}
}
