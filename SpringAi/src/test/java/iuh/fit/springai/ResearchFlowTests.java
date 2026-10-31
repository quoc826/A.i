package iuh.fit.springai;
import iuh.fit.springai.repositories.*;
import iuh.fit.springai.entities.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:researchflows;MODE=MariaDB;DB_CLOSE_DELAY=-1")
@ActiveProfiles("demo") @AutoConfigureMockMvc
class ResearchFlowTests {
 @Autowired MockMvc mvc;@Autowired ProductRepository products;@Autowired TrackingEventRepository events;@Autowired UserRepository users;@Autowired OrderRepository orders;@Autowired CustomerRepository customers;
 @Test void guestChatWorksWithoutKeyAndHistoryIsSessionBound() throws Exception {
  var session=new MockHttpSession();
  mvc.perform(get("/assistant").session(session)).andExpect(status().isOk());
  mvc.perform(post("/api/v1/chat").session(session).with(csrf()).contentType("application/json").content("{\"message\":\"Laptop\"}"))
   .andExpect(status().isOk()).andExpect(jsonPath("$.mode").value("CATALOG")).andExpect(jsonPath("$.products[0].name").value("Laptop"));
  mvc.perform(get("/api/v1/chat").session(session)).andExpect(jsonPath("$.messages.length()").value(2));
  mvc.perform(get("/api/v1/chat").session(new MockHttpSession())).andExpect(jsonPath("$.messages.length()").value(0));
  mvc.perform(get("/api/v1/chat").session(session).with(user("customer").roles("CUSTOMER"))).andExpect(jsonPath("$.messages.length()").value(0));
  mvc.perform(delete("/api/v1/chat").session(session).with(csrf())).andExpect(status().isNoContent());
 }
 @Test void chatValidatesInputCsrfAndThrottle() throws Exception {
  mvc.perform(post("/api/v1/chat").contentType("application/json").content("{\"message\":\"Laptop\"}")).andExpect(status().isForbidden());
  mvc.perform(post("/api/v1/chat").with(csrf()).contentType("application/json").content("{\"message\":\"\"}")).andExpect(status().isBadRequest());
  mvc.perform(post("/api/v1/chat").with(csrf()).contentType("application/json").content("{\"message\":\"Laptop\",\"maxPrice\":-1}")).andExpect(status().isBadRequest());
  var session=new MockHttpSession();
  for(int i=0;i<15;i++)mvc.perform(post("/api/v1/chat").session(session).with(csrf()).contentType("application/json").content("{\"message\":\"Laptop\"}")).andExpect(status().isOk());
  mvc.perform(post("/api/v1/chat").session(session).with(csrf()).contentType("application/json").content("{\"message\":\"Laptop\"}")).andExpect(status().isTooManyRequests());
 }
 @Test void searchUsesRealPricesAndValidatesFilters() throws Exception {
  mvc.perform(get("/api/v1/search").param("query","Laptop")).andExpect(status().isOk()).andExpect(jsonPath("$.mode").value("KEYWORD")).andExpect(jsonPath("$.products[0].price").value(999.0));
  mvc.perform(get("/api/v1/search").param("query","Laptop").param("maxPrice","100")).andExpect(jsonPath("$.products.length()").value(0));
  mvc.perform(get("/api/v1/search").param("query","Laptop").param("maxPrice","-1")).andExpect(status().isBadRequest());
  mvc.perform(get("/product").param("query","Laptop")).andExpect(status().isOk());
 }
 @Test void trackingBindsIdentityDeduplicatesAndStoresServerPrice() throws Exception {
  var p=products.findAll().get(0);String eventId=UUID.randomUUID().toString();
  String json="{\"eventId\":\""+eventId+"\",\"eventType\":\"PRODUCT_VIEW\",\"productId\":"+p.getId()+",\"path\":\"/product/"+p.getId()+"?secret=ignored\"}";
  var session=new MockHttpSession();
  for(int i=0;i<2;i++)mvc.perform(post("/api/v1/tracking/events").session(session).with(csrf()).contentType("application/json").content(json)).andExpect(status().isNoContent());
  var saved=events.findById(eventId).orElseThrow();assertNull(saved.getUserId());assertNotNull(saved.getAnonymousId());assertFalse(saved.getPath().contains("?"));
  String cartId=UUID.randomUUID().toString();
  mvc.perform(post("/api/v1/tracking/events").session(session).with(user("customer").roles("CUSTOMER")).with(csrf()).contentType("application/json")
    .content("{\"eventId\":\""+cartId+"\",\"eventType\":\"ADD_TO_CART\",\"productId\":"+p.getId()+",\"quantity\":2}")).andExpect(status().isNoContent());
  var cartEvent=events.findById(cartId).orElseThrow();assertEquals(users.findByUsername("customer").orElseThrow().getId(),cartEvent.getUserId());assertEquals(0,p.getPrice().compareTo(cartEvent.getPrice()));
  mvc.perform(post("/api/v1/tracking/events").with(csrf()).contentType("application/json").content("{\"eventId\":\""+UUID.randomUUID()+"\",\"eventType\":\"SEARCH\"}")).andExpect(status().isBadRequest());
 }
 @Test void orderPrivacyAndAdminDashboard() throws Exception {
  var customer=users.findByUsername("customer").orElseThrow().getCustomer();
  var stranger=new Customer();stranger.setName("Other User");stranger.setEmail("other@gmail.com");stranger.setCustomerSince(java.time.LocalDate.now().minusDays(1));
  customers.save(stranger);var foreign=new iuh.fit.springai.entities.Order();foreign.setDate(java.time.LocalDate.now());foreign.setCustomer(stranger);orders.save(foreign);
  // An unrelated customer's order must never enter the chat response.
  var own=new iuh.fit.springai.entities.Order();own.setDate(java.time.LocalDate.now());own.setCustomer(customer);orders.save(own);
  mvc.perform(post("/api/v1/chat").with(csrf()).contentType("application/json").content("{\"message\":\"Đơn hàng của tôi\"}")).andExpect(jsonPath("$.orders.length()").value(0));
  mvc.perform(post("/api/v1/chat").with(user("customer").roles("CUSTOMER")).with(csrf()).contentType("application/json").content("{\"message\":\"Đơn hàng của tôi\"}")).andExpect(jsonPath("$.orders.length()").value(1)).andExpect(jsonPath("$.orders[0].id").value(own.getId().intValue()));
  mvc.perform(get("/admin/research").with(user("customer").roles("CUSTOMER"))).andExpect(status().isForbidden());
  mvc.perform(get("/admin/research").with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
 }
}
