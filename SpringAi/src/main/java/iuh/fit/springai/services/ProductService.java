package iuh.fit.springai.services;
import iuh.fit.springai.entities.*;
import iuh.fit.springai.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.List;
@Service @RequiredArgsConstructor @Transactional
public class ProductService {
 private final ProductRepository products;
 private final CategoryRepository categories;
 private final CommentRepository comments;
 private final org.springframework.context.ApplicationEventPublisher events;
 public List<Product> findAll(){return products.findByActiveTrueOrderByNameAsc().stream().filter(p->p.getCategory().isActive()).toList();}
 public Product findById(Long id){return products.findByIdAndActiveTrue(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy sản phẩm"));}
 public Product save(Product input){
  var category=categories.findById(input.getCategory().getId()).filter(Category::isActive).orElseThrow(()->new IllegalArgumentException("Danh mục không hợp lệ"));
  Product p=input.getId()==null?new Product():findById(input.getId());
  p.setName(input.getName());p.setPrice(input.getPrice());p.setQuantity(input.getQuantity());p.setCategory(category);
  p.setBrand(input.getBrand());p.setDescription(input.getDescription());p.setSpecifications(input.getSpecifications());
  if(input.getImage()!=null)p.setImage(input.getImage());
  Product saved=products.save(p);events.publishEvent(new ProductChanged(saved.getId()));return saved;
 }
 public void lock(Long id){findById(id).setActive(false);events.publishEvent(new ProductChanged(id));}
 public void comment(Long id,String text,Customer customer){
  if(customer==null || !customer.isActive())throw new IllegalArgumentException("Cần tài khoản khách hàng");
  var c=new Comment();c.setProduct(findById(id));c.setCustomer(customer);c.setText(text);comments.save(c);
 }
}
