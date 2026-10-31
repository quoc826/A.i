package iuh.fit.springai.services;
import org.springframework.stereotype.Component;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
@Component public class RequestBudget {
 public void check(HttpSession session,String bucket,int maximum){
  synchronized(session){
   String key="budget:"+bucket;long now=System.currentTimeMillis();long[] state=(long[])session.getAttribute(key);
   if(state==null || now-state[0]>=60000)state=new long[]{now,0};
   if(state[1]>=maximum)throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Bạn thao tác quá nhanh. Vui lòng thử lại sau một phút.");
   state[1]++;session.setAttribute(key,state);
  }
 }
}
