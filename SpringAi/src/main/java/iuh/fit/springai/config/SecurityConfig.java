package iuh.fit.springai.config;
import iuh.fit.springai.repositories.UserRepository;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
@Configuration @EnableMethodSecurity
public class SecurityConfig {
 @Bean BCryptPasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder();}
 @Bean UserDetailsService userDetailsService(UserRepository users){
  return name -> {
   var u=users.findByUsername(name).orElseThrow(()->new UsernameNotFoundException(name));
   return org.springframework.security.core.userdetails.User.withUsername(u.getUsername()).password(u.getPassword())
    .roles(u.getRole()).disabled(!u.isActive() || (u.getCustomer()!=null && !u.getCustomer().isActive())).build();
  };
 }
 @Bean SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
  return http.authorizeHttpRequests(a->a
   .requestMatchers("/","/home","/login","/css/**","/js/**","/uploads/**","/error","/assistant","/api/v1/chat","/api/v1/search","/api/v1/tracking/events").permitAll()
   .requestMatchers("/admin/**","/product/add","/product/edit/**","/product/save","/product/update","/product/lock/**").hasRole("ADMIN")
   .requestMatchers(org.springframework.http.HttpMethod.GET,"/product","/product/**").permitAll()
   .anyRequest().authenticated())
   .formLogin(f->f.loginPage("/login").defaultSuccessUrl("/product",true).permitAll())
   .logout(l->l.logoutSuccessUrl("/login?logout").invalidateHttpSession(true).deleteCookies("JSESSIONID"))
   .build();
 }
}
