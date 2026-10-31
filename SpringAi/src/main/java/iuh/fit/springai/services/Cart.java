package iuh.fit.springai.services;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;
import java.util.*;
@Component @SessionScope
public class Cart {
 private final Map<Long,Integer> items = new TreeMap<>();
 public Map<Long,Integer> getItems(){return new TreeMap<>(items);}
 public void set(Long id,int quantity){if(quantity<1 || quantity>10000)throw new IllegalArgumentException("Số lượng phải từ 1 đến 10000");items.put(id,quantity);}
 public void add(Long id,int quantity){set(id,Math.addExact(items.getOrDefault(id,0),quantity));}
 public void remove(Long id){items.remove(id);}
 public void clear(){items.clear();}
}
