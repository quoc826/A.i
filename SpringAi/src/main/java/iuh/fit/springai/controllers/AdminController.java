package iuh.fit.springai.controllers;
import iuh.fit.springai.entities.*;
import iuh.fit.springai.repositories.*;
import iuh.fit.springai.services.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.time.LocalDate;
@Controller @RequiredArgsConstructor @PreAuthorize("hasRole('ADMIN')")
public class AdminController {
 private final ProductService products; private final CategoryRepository categories; private final CustomerRepository customers; private final UserRepository users; private final PasswordEncoder encoder; private final OrderService orders; private final AccountService accounts; private final ImageStorage images;
 @GetMapping("/product/add") String add(Model m){m.addAttribute("product",new Product());m.addAttribute("categories",categories.findByActiveTrueOrderByNameAsc());return "product/form";}
 @GetMapping("/product/edit/{id}") String edit(@PathVariable Long id,Model m){m.addAttribute("product",products.findById(id));m.addAttribute("categories",categories.findByActiveTrueOrderByNameAsc());return "product/form";}
 @PostMapping({"/product/save","/product/update"}) String save(@org.springframework.validation.annotation.Validated({jakarta.validation.groups.Default.class,Product.Input.class}) @ModelAttribute Product product,BindingResult errors,@RequestParam(required=false) MultipartFile fileImage,Model m) throws IOException {
  if(errors.hasErrors()){m.addAttribute("categories",categories.findByActiveTrueOrderByNameAsc());return "product/form";}
  product.setImage(fileImage==null?null:images.store(fileImage));products.save(product);return "redirect:/product";
 }
 @PostMapping("/product/lock/{id}") String lockProduct(@PathVariable Long id){products.lock(id);return "redirect:/product";}
 @GetMapping("/admin/category") String categories(Model m){m.addAttribute("categories",categories.findAll());m.addAttribute("category",new Category());return "admin/category";}
 @GetMapping("/admin/category/{id}") String category(@PathVariable Long id,Model m){m.addAttribute("categories",categories.findAll());m.addAttribute("category",categories.findById(id).orElseThrow());return "admin/category";}
 @PostMapping("/admin/category/save") @Transactional String categorySave(@Valid @ModelAttribute Category category,BindingResult errors,Model m){
  if(errors.hasErrors()){m.addAttribute("categories",categories.findAll());return "admin/category";}
  var saved=category.getId()==null?new Category():categories.findById(category.getId()).orElseThrow();saved.setName(category.getName());categories.save(saved);return "redirect:/admin/category";
 }
 @PostMapping("/admin/category/{id}/lock") @Transactional String categoryLock(@PathVariable Long id){categories.findById(id).orElseThrow().setActive(false);return "redirect:/admin/category";}
 @Getter @Setter public static class CustomerForm {
  @Valid private Customer customer=new Customer();
  @NotBlank @Size(min=3,max=50) @Pattern(regexp="[a-zA-Z0-9._-]+") private String username;
  @Size(max=100) private String password;
 }
 @GetMapping("/admin/customer") String customers(Model m){m.addAttribute("customers",customers.findAll());return "admin/customers";}
 @GetMapping("/admin/customer/add") String customerAdd(Model m){m.addAttribute("customerForm",new CustomerForm());return "admin/customer-form";}
 @GetMapping("/admin/customer/{id}") String customerEdit(@PathVariable Long id,Model m){var f=new CustomerForm();f.setCustomer(customers.findById(id).orElseThrow());f.setUsername(users.findByCustomerId(id).map(User::getUsername).orElse(""));m.addAttribute("customerForm",f);return "admin/customer-form";}
 @PostMapping("/admin/customer/save") @Transactional String customerSave(@Valid @ModelAttribute CustomerForm form,BindingResult errors){
  Long id=form.getCustomer().getId();
  var account=id==null?new User():users.findByCustomerId(id).orElseGet(User::new);
  if(users.findByUsername(form.getUsername()).filter(u->!u.getId().equals(account.getId())).isPresent())errors.rejectValue("username","duplicate","Tên đăng nhập đã tồn tại");
  if(account.getId()==null && (form.getPassword()==null || form.getPassword().length()<6))errors.rejectValue("password","short","Mật khẩu mới ít nhất 6 ký tự");
  if(errors.hasErrors())return "admin/customer-form";
  var c=id==null?new Customer():customers.findById(id).orElseThrow();c.setName(form.getCustomer().getName());c.setEmail(form.getCustomer().getEmail());c.setCustomerSince(form.getCustomer().getCustomerSince());customers.save(c);
  account.setCustomer(c);account.setUsername(form.getUsername());account.setRole("CUSTOMER");if(form.getPassword()!=null && !form.getPassword().isBlank())account.setPassword(encoder.encode(form.getPassword()));users.save(account);return "redirect:/admin/customer";
 }
 @PostMapping("/admin/customer/{id}/lock") @Transactional String customerLock(@PathVariable Long id){customers.findById(id).orElseThrow().setActive(false);users.findByCustomerId(id).ifPresent(u->u.setActive(false));return "redirect:/admin/customer";}
 @GetMapping("/admin/order/add") String orderAdd(Model m){m.addAttribute("customers",customers.findByActiveTrueOrderByNameAsc());m.addAttribute("products",products.findAll());return "admin/order-form";}
 @PostMapping("/admin/order/save") String orderSave(@RequestParam Long customerId,@RequestParam Long productId,@RequestParam int quantity,@RequestParam LocalDate date){
  var user=users.findByCustomerId(customerId).filter(User::isActive).orElseThrow(()->new IllegalArgumentException("Khách hàng không hoạt động"));
  var o=orders.checkout(user,java.util.Map.of(productId,quantity),date);return "redirect:/order/"+o.getId();
 }
 @PostMapping("/admin/order/{id}/update") String orderUpdate(@PathVariable Long id,@RequestParam LocalDate date,@RequestParam String status,Authentication auth){orders.update(id,date,status,accounts.current(auth));return "redirect:/order/"+id;}
 @PostMapping("/admin/order/{id}/lock") String orderLock(@PathVariable Long id,Authentication auth){orders.lock(id,accounts.current(auth));return "redirect:/order";}
}
