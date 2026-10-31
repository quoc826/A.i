package iuh.fit.springai.services;

import iuh.fit.springai.entities.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.*;

@Service @RequiredArgsConstructor
public class ProductSearchService {
 private final ProductService products;
 private final ProductEmbeddingService embeddings;
 public record ProductCard(Long id,String name,String brand,String description,String specifications,String category,BigDecimal price,int quantity,String image,String url){}
 public record Result(String mode,String notice,List<ProductCard> products){}
 public static ProductCard card(Product p){return new ProductCard(p.getId(),p.getName(),p.getBrand(),p.getDescription(),p.getSpecifications(),p.getCategory().getName(),p.getPrice(),p.getQuantity(),p.getImage(),"/product/"+p.getId());}
 public static String normalize(String text){return Normalizer.normalize(text==null?"":text,Normalizer.Form.NFD).replaceAll("\\p{M}","").replace('đ','d').replace('Đ','D').toLowerCase(Locale.ROOT);}
 public Result search(String query,BigDecimal maxPrice,boolean inStock){
  var live=products.findAll().stream().filter(p->maxPrice==null || p.getPrice().compareTo(maxPrice)<=0).filter(p->!inStock || p.isInStock()).toList();
  if(query==null || query.isBlank())return new Result("CATALOG","Danh sách sản phẩm hiện tại",live.stream().map(ProductSearchService::card).toList());
  if(embeddings.enabled()){
   try{
    var byId=new HashMap<String,Product>();live.forEach(p->byId.put(p.getId().toString(),p));
    var matches=new LinkedHashMap<Long,ProductCard>();
    for(var doc:embeddings.search(query)){
     var p=byId.get(String.valueOf(doc.getMetadata().get("productId")));
     // Recheck live SQL data, including the semantic hash, so stale or locked records cannot be recommended.
     if(p!=null && ProductEmbeddingService.hash(ProductEmbeddingService.text(p)).equals(doc.getMetadata().get("contentHash")))matches.put(p.getId(),card(p));
     if(matches.size()==6)break;
    }
    if(embeddings.status().failed()>0 && embeddings.status().indexed()==0)throw new IllegalStateException("Index unavailable");
    return new Result("SEMANTIC","Tìm kiếm ngữ nghĩa",List.copyOf(matches.values()));
   }catch(RuntimeException e){return keyword(query,live,"Tìm kiếm AI đang gián đoạn; kết quả dưới đây dùng từ khóa.");}
  }
  return keyword(query,live,"Đang tìm theo từ khóa. Tìm kiếm ngữ nghĩa chưa được bật.");
 }
 private Result keyword(String query,List<Product> live,String notice){
  var stop=Set.of("toi","can","muon","tim","cho","voi","cua","san","pham","co","khong","va","la","de","mua","hay","giup","minh","mot","nhung","phu","hop","nao","ban","duoi");
  var tokens=Arrays.stream(normalize(query).split("[^a-z0-9]+")).filter(t->t.length()>1 && !stop.contains(t)).distinct().toList();
  return new Result("KEYWORD",notice,live.stream().map(p->Map.entry(p,score(p,tokens))).filter(e->e.getValue()>0)
   .sorted(Map.Entry.<Product,Integer>comparingByValue().reversed().thenComparing(e->e.getKey().getId())).limit(6).map(e->card(e.getKey())).toList());
 }
 private int score(Product p,List<String> tokens){
  String content=" "+normalize(ProductEmbeddingService.text(p)).replaceAll("[^a-z0-9]+"," ")+" ";
  String name=" "+normalize(p.getName()).replaceAll("[^a-z0-9]+"," ")+" ";
  int matched=0,weight=0;
  for(String token:tokens){if(content.contains(" "+token+" ")){matched++;weight+=name.contains(" "+token+" ")?3:1;}}
  return matched>=Math.min(2,tokens.size())?weight:0;
 }
}
