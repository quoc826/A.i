package iuh.fit.springai;
import iuh.fit.springai.entities.*;
import iuh.fit.springai.repositories.*;
import iuh.fit.springai.services.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
@SpringBootTest @ActiveProfiles("demo") @AutoConfigureMockMvc
class ShoppingFlowTests {
 @Autowired MockMvc mvc;@Autowired UserRepository users;@Autowired ProductRepository products;@Autowired OrderRepository repository;@Autowired OrderService orders;@Autowired jakarta.validation.Validator validator;
 @Test void guestCanBrowseButCannotManage() throws Exception {
  mvc.perform(get("/product")).andExpect(status().isOk());
  mvc.perform(get("/product/"+products.findAll().get(0).getId())).andExpect(status().isOk());
  mvc.perform(get("/admin/customer")).andExpect(status().is3xxRedirection());
  mvc.perform(get("/admin/customer").with(user("customer").roles("CUSTOMER"))).andExpect(status().isForbidden());
  mvc.perform(post("/cart/add").with(user("customer").roles("CUSTOMER")).param("productId","1").param("quantity","1")).andExpect(status().isForbidden());
 }
 @Test @Transactional void checkoutSnapshotsPricesAndAllowsSellingLastUnit(){
  var u=users.findByUsername("customer").orElseThrow();var p=products.findAll().get(0);var price=p.getPrice();int stock=p.getQuantity();
  var order=orders.checkout(u,Map.of(p.getId(),stock),LocalDate.now());products.flush();
  assertEquals(0,p.getQuantity());assertEquals(price,order.getOrderLines().get(0).getPurchasePrice());
  p.setPrice(price.add(java.math.BigDecimal.TEN));assertEquals(price,order.getOrderLines().get(0).getPurchasePrice());
 }
 @Test @Transactional void rejectsOversellingAndForeignOrders(){
  var u=users.findByUsername("customer").orElseThrow();var p=products.findAll().get(0);
  assertThrows(IllegalArgumentException.class,()->orders.checkout(u,Map.of(p.getId(),p.getQuantity()+1),LocalDate.now()));
  assertThrows(IllegalArgumentException.class,()->orders.checkout(u,Map.of(p.getId(),1),LocalDate.now().minusDays(1)));
  var o=orders.checkout(u,Map.of(p.getId(),1),LocalDate.now());
  var stranger=new User();var c=new Customer();c.setId(999L);stranger.setCustomer(c);
  assertThrows(org.springframework.security.access.AccessDeniedException.class,()->orders.detail(o.getId(),stranger));
 }
 @Test void customerAndAdminPagesRender() throws Exception {
  for(String path:new String[]{"/home","/cart","/order","/assistant","/admin/customer","/admin/customer/add","/admin/category","/admin/order/add","/product/add","/product/edit/"+products.findAll().get(0).getId()})
   mvc.perform(get(path).with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
  var u=users.findByUsername("customer").orElseThrow();var p=products.findAll().get(0);var o=orders.checkout(u,Map.of(p.getId(),1),LocalDate.now());
  mvc.perform(get("/order/"+o.getId()).with(user("customer").roles("CUSTOMER"))).andExpect(status().isOk());
  mvc.perform(get("/order/"+o.getId()).with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
 }
 @Test void inputValidationMatchesAssignment(){
  var c=new Customer();c.setName("one");c.setEmail("a@example.com");c.setCustomerSince(LocalDate.now());assertEquals(3,validator.validate(c).size());
  var p=new Product();p.setName("a");p.setPrice(java.math.BigDecimal.ZERO);p.setQuantity(0);
  assertTrue(validator.validate(p,jakarta.validation.groups.Default.class,Product.Input.class).size()>=4);
 }
 @Test void checkoutViaWebClearsCartAndOrderIsVisible() throws Exception {
  var p=products.findAll().get(0);
  var result=mvc.perform(post("/cart/add").with(user("customer").roles("CUSTOMER")).with(csrf()).param("productId",p.getId().toString()).param("quantity","2")).andExpect(redirectedUrl("/cart")).andReturn();
  var session=(org.springframework.mock.web.MockHttpSession)result.getRequest().getSession();
  mvc.perform(get("/cart").session(session).with(user("customer").roles("CUSTOMER"))).andExpect(status().isOk());
  mvc.perform(post("/cart/checkout").session(session).with(user("customer").roles("CUSTOMER")).with(csrf()).param("date",LocalDate.now().toString())).andExpect(status().is3xxRedirection());
  mvc.perform(get("/cart").session(session).with(user("customer").roles("CUSTOMER"))).andExpect(status().isOk()).andExpect(model().attribute("total",java.math.BigDecimal.ZERO));
 }
 @Test void productAndCustomerFormsPersistAndValidate() throws Exception {
  var p=products.findAll().get(0);
  mvc.perform(post("/product/save").with(user("admin").roles("ADMIN")).with(csrf()).param("name","Monitor").param("price","149.00").param("quantity","3").param("category",p.getCategory().getId().toString())).andExpect(redirectedUrl("/product"));
  mvc.perform(post("/product/save").with(user("admin").roles("ADMIN")).with(csrf()).param("name","a").param("price","0").param("quantity","0").param("category",p.getCategory().getId().toString())).andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("product","name","price","quantity"));
  mvc.perform(post("/admin/customer/save").with(user("admin").roles("ADMIN")).with(csrf()).param("customer.name","Tran Binh").param("customer.email","binh@gmail.com").param("customer.customerSince",LocalDate.now().minusDays(2).toString()).param("username","binh").param("password","secret123")).andExpect(redirectedUrl("/admin/customer"));
  mvc.perform(post("/product/"+p.getId()+"/comment").with(user("customer").roles("CUSTOMER")).with(csrf()).param("text","Sản phẩm tốt")).andExpect(status().is3xxRedirection());
  mvc.perform(get("/product/"+p.getId())).andExpect(status().isOk());
 }
 @Test void databaseLoginWorks() throws Exception {
  mvc.perform(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin().user("customer").password("111"))
   .andExpect(org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated().withRoles("CUSTOMER"));
  mvc.perform(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin().user("customer").password("wrong"))
   .andExpect(org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated());
 }
 @Test void failedMultiItemCheckoutRollsBackEarlierStockChange(){
  var list=products.findAll().stream().sorted(java.util.Comparator.comparing(Product::getId)).toList();
  var first=list.get(0);var second=list.get(1);int before=first.getQuantity();
  assertThrows(IllegalArgumentException.class,()->orders.checkout(users.findByUsername("customer").orElseThrow(),Map.of(first.getId(),1,second.getId(),second.getQuantity()+1),LocalDate.now()));
  assertEquals(before,products.findById(first.getId()).orElseThrow().getQuantity());
 }
}
