package iuh.fit.springai.services;
import iuh.fit.springai.entities.User;
import iuh.fit.springai.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.AccessDeniedException;
@Service @RequiredArgsConstructor
public class AccountService {
 private final UserRepository users;
 public User current(Authentication auth){
  var u=users.findByUsername(auth.getName()).orElseThrow(()->new AccessDeniedException("Tài khoản không tồn tại"));
  if(!u.isActive() || (u.getCustomer()!=null && !u.getCustomer().isActive())) throw new AccessDeniedException("Tài khoản đã khóa");
  return u;
 }
 public boolean admin(User u){return "ADMIN".equals(u.getRole());}
}
