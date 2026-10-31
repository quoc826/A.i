package iuh.fit.springai.config;
import iuh.fit.springai.entities.*;
import iuh.fit.springai.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
@Component @RequiredArgsConstructor @ConditionalOnProperty(name="app.seed-demo",havingValue="true")
public class DemoData implements CommandLineRunner {
 private final CategoryRepository categories; private final ProductRepository products; private final CustomerRepository customers; private final UserRepository users; private final PasswordEncoder encoder;
 @Override @Transactional public void run(String...args){
  if(users.count()>0)return;
  var c=new Category();c.setName("Electronics");categories.save(c);
  for(String name:new String[]{"Laptop","Headphones","Keyboard"}){var p=new Product();p.setName(name);p.setPrice(new BigDecimal(name.equals("Laptop")?"999.00":"49.00"));p.setQuantity(20);p.setCategory(c);products.save(p);}
  var sport=new Product();sport.setName("Tai nghe thể thao Demo");sport.setBrand("Demo");sport.setDescription("Dữ liệu minh họa: tai nghe không dây dành cho chạy bộ, tập thể thao ngoài trời.");sport.setSpecifications("Mẫu giả lập phục vụ nghiên cứu: Bluetooth, chống nước, móc tai.");sport.setPrice(new BigDecimal("39.00"));sport.setQuantity(15);sport.setCategory(c);products.save(sport);
  var customer=new Customer();customer.setName("Nguyen An");customer.setEmail("customer@gmail.com");customer.setCustomerSince(LocalDate.now().minusDays(1));customers.save(customer);
  var admin=new User();admin.setUsername("admin");admin.setPassword(encoder.encode("123"));admin.setRole("ADMIN");users.save(admin);
  var account=new User();account.setUsername("customer");account.setPassword(encoder.encode("111"));account.setCustomer(customer);users.save(account);
 }
}
