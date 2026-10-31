package iuh.fit.springai.services;

import iuh.fit.springai.entities.*;
import iuh.fit.springai.repositories.ProductEmbeddingRepository;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.*;
import org.springframework.transaction.event.*;
import org.slf4j.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

@Service
public class ProductEmbeddingService {
 private static final Logger log=LoggerFactory.getLogger(ProductEmbeddingService.class);
 private final ObjectProvider<VectorStore> stores;
 private final ProductService products;
 private final ProductEmbeddingRepository states;
 private final Path file;
 private final String key;
 private final boolean enabled;
 private volatile SyncStatus status=new SyncStatus(false,0,0,"Chưa bật tìm kiếm ngữ nghĩa",null);
 public record SyncStatus(boolean enabled,int indexed,int failed,String message,Instant lastRun){}
 public ProductEmbeddingService(ObjectProvider<VectorStore> stores,ProductService products,ProductEmbeddingRepository states,
   @Value("${app.ai.enabled:false}") boolean enabled,@Value("${app.ai.index-directory:./data/vector-index}") String directory,
   @Value("${app.ai.vector-store:simple}") String type,@Value("${app.ai.chroma-collection:shopping-products-text3small-v1}") String collection){
  this.stores=stores;this.products=products;this.states=states;this.enabled=enabled;
  this.file=Path.of(directory,"products-text3small-1536.json");
  this.key=type+":text-embedding-3-small:1536:"+("chroma".equals(type)?collection:file.toAbsolutePath().normalize());
 }
 public SyncStatus status(){return status;}
 public static String clean(String input){return input==null?"":input.replaceAll("<[^>]*>"," ").replaceAll("\\s+"," ").trim();}
 public static String text(Product p){
  return "Tên: "+clean(p.getName())+". Danh mục: "+clean(p.getCategory().getName())+
   ". Thương hiệu: "+clean(p.getBrand())+". Mô tả: "+clean(p.getDescription())+". Đặc điểm: "+clean(p.getSpecifications());
 }
 public static String hash(String text){
  try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));}
  catch(Exception e){throw new IllegalStateException(e);}
 }
 private String id(Long productId){return UUID.nameUUIDFromBytes((key+":"+productId).getBytes(StandardCharsets.UTF_8)).toString();}
 private void persist(VectorStore store) throws Exception {
  if(store instanceof SimpleVectorStore simple){
   Files.createDirectories(file.toAbsolutePath().getParent());
   Path temporary=file.resolveSibling(file.getFileName()+".tmp");
   simple.save(temporary.toFile());Files.move(temporary,file,StandardCopyOption.REPLACE_EXISTING);
  }
 }
 // Database commits happen before indexing. Failed items keep their old fingerprint and retry next run.
 public synchronized SyncStatus sync(){
  if(!enabled)return status;
  int failed=0,indexed=0;
  try{
   VectorStore store=stores.getObject();
   var live=products.findAll();var old=states.findByStoreKey(key);
   Map<Long,ProductEmbedding> byProduct=new HashMap<>();old.forEach(s->byProduct.put(s.getProductId(),s));
   boolean rebuild=store instanceof SimpleVectorStore && !Files.exists(file);
   for(var p:live){
    var previous=byProduct.remove(p.getId());String content=text(p),digest=hash(content);
    if(!rebuild && previous!=null && digest.equals(previous.getContentHash())){indexed++;continue;}
    try{
     var document=Document.builder().id(id(p.getId())).text(content)
      .metadata(Map.of("productId",p.getId().toString(),"contentHash",digest)).build();
     store.add(List.of(document));persist(store); // VectorStore creates each embedding exactly once.
     var state=new ProductEmbedding();state.setId(document.getId());state.setProductId(p.getId());state.setStoreKey(key);
     state.setContentHash(digest);state.setSyncedAt(Instant.now());states.save(state);indexed++;
    }catch(Exception e){failed++;log.warn("Product vector sync failed for product {} ({})",p.getId(),e.getClass().getSimpleName());}
   }
   for(var stale:byProduct.values()){
    try{store.delete(List.of(stale.getId()));persist(store);states.deleteById(stale.getId());}
    catch(Exception e){failed++;log.warn("Vector removal failed for product {}",stale.getProductId());}
   }
   status=new SyncStatus(true,indexed,failed,failed==0?"Đồng bộ hoàn tất":"Có lỗi đồng bộ; hệ thống sẽ thử lại",Instant.now());
  }catch(Exception e){
   log.warn("Vector store unavailable ({})",e.getClass().getSimpleName());
   status=new SyncStatus(true,indexed,failed+1,"Chưa kết nối được mô hình embedding hoặc vector store",Instant.now());
  }
  return status;
 }
 @Async @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT)
 public void changed(ProductChanged event){sync();}
 @Scheduled(initialDelay=10000,fixedDelayString="${app.ai.sync-delay-ms:60000}")
 public void scheduled(){if(enabled)sync();}
 public synchronized List<Document> search(String query){
  if(!enabled)return List.of();
  if(status.lastRun()==null)sync();
  return stores.getObject().similaritySearch(SearchRequest.builder().query(query).topK(30).similarityThreshold(0.25).build());
 }
 public boolean enabled(){return enabled;}
}
