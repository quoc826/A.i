package iuh.fit.springai.controllers;
import iuh.fit.springai.services.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import java.time.LocalDate;
@Controller @RequiredArgsConstructor
public class ShopController {
 private final ProductService products; private final OrderService orders; private final AccountService accounts; private final Cart cart;
 private final ProductSearchService search;
 @GetMapping({"/","/home"}) String home(Model m){m.addAttribute("message","Welcome Thymeleaf");m.addAttribute("date",LocalDate.now());return "home";}
 @GetMapping("/login") String login(){return "login";}
 @GetMapping("/product") String products(Model m,@RequestParam(defaultValue="") String query,@RequestParam(required=false) java.math.BigDecimal maxPrice,@RequestParam(defaultValue="false") boolean inStock){
  if(query.length()>2000 || (maxPrice!=null && maxPrice.signum()<=0))throw new IllegalArgumentException("Từ khóa tối đa 2000 ký tự, giá phải lớn hơn 0");
  var result=search.search(query,maxPrice,inStock);m.addAttribute("products",result.products());m.addAttribute("query",query);
  m.addAttribute("searchNotice",query.isBlank()?"":result.notice());m.addAttribute("maxPrice",maxPrice);m.addAttribute("inStock",inStock);
  return "product/list";
 }
 @GetMapping({"/product/{id}","/product/detail/{id}"}) String product(@PathVariable Long id,Model m){m.addAttribute("product",products.findById(id));m.addAttribute("commentForm",new CommentForm());return "product/productdetail";}
 @Getter @Setter public static class CommentForm {@NotBlank @Size(max=1000) private String text;}
 @PostMapping("/product/{id}/comment") String comment(@PathVariable Long id,@Valid @ModelAttribute CommentForm commentForm,BindingResult errors,Authentication auth,Model m){
  if(errors.hasErrors()){m.addAttribute("product",products.findById(id));return "product/productdetail";}
  products.comment(id,commentForm.getText(),accounts.current(auth).getCustomer());return "redirect:/product/"+id;
 }
 public record CartRow(iuh.fit.springai.entities.Product product,int quantity){public java.math.BigDecimal getSubtotal(){return product.getPrice().multiply(java.math.BigDecimal.valueOf(quantity));}}
 @GetMapping("/cart") String cart(Model m){var rows=cart.getItems().entrySet().stream().map(e->new CartRow(products.findById(e.getKey()),e.getValue())).toList();m.addAttribute("rows",rows);m.addAttribute("total",rows.stream().map(CartRow::getSubtotal).reduce(java.math.BigDecimal.ZERO,java.math.BigDecimal::add));m.addAttribute("checkoutForm",new CheckoutForm());return "cart";}
 @PostMapping("/cart/add") String add(@RequestParam Long productId,@RequestParam int quantity,Authentication auth){accounts.current(auth);var p=products.findById(productId);if(quantity<1 || quantity+cart.getItems().getOrDefault(productId,0)>p.getQuantity())throw new IllegalArgumentException("Số lượng vượt tồn kho");cart.add(productId,quantity);return "redirect:/cart";}
 @PostMapping("/cart/update") String update(@RequestParam Long productId,@RequestParam int quantity){if(quantity>products.findById(productId).getQuantity())throw new IllegalArgumentException("Số lượng vượt tồn kho");cart.set(productId,quantity);return "redirect:/cart";}
 @PostMapping("/cart/remove") String remove(@RequestParam Long productId){cart.remove(productId);return "redirect:/cart";}
 @Getter @Setter public static class CheckoutForm {@NotNull @FutureOrPresent private LocalDate date=LocalDate.now();}
 @PostMapping("/cart/checkout") String checkout(@Valid @ModelAttribute CheckoutForm form,BindingResult errors,Authentication auth){if(errors.hasErrors())throw new IllegalArgumentException("Ngày đặt hàng phải từ hôm nay trở đi");var o=orders.checkout(accounts.current(auth),cart.getItems(),form.getDate());cart.clear();return "redirect:/order/"+o.getId();}
 @GetMapping("/order") String orders(Authentication auth,Model m){m.addAttribute("orders",orders.visible(accounts.current(auth)));return "order/list";}
 @GetMapping("/order/{id}") String order(@PathVariable Long id,Authentication auth,Model m){m.addAttribute("order",orders.detail(id,accounts.current(auth)));return "order/detail";}
}
