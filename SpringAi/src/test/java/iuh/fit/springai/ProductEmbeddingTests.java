package iuh.fit.springai;
import iuh.fit.springai.entities.*;
import iuh.fit.springai.repositories.*;
import iuh.fit.springai.services.*;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.*;
import org.springframework.ai.vectorstore.*;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class ProductEmbeddingTests {
 @TempDir Path directory;
 static class FakeEmbedding implements EmbeddingModel {
  final AtomicInteger count=new AtomicInteger();boolean fail=false;
  public float[] embed(Document document){if(fail)throw new IllegalStateException("Simulated API outage");count.incrementAndGet();return vector(document.getText());}
  private float[] vector(String text){return text.toLowerCase().matches(".*(headphones|tai nghe|am thanh).*")?new float[]{1,0,0}:new float[]{0,1,0};}
  public EmbeddingResponse call(EmbeddingRequest request){var list=new ArrayList<Embedding>();for(int i=0;i<request.getInstructions().size();i++)list.add(new Embedding(vector(request.getInstructions().get(i)),i));return new EmbeddingResponse(list);}
  public int dimensions(){return 3;}
 }
 @Test void vectorsPersistSyncOnlySemanticChangesRetryAndHideStaleData(){
  var model=new FakeEmbedding();var store=SimpleVectorStore.builder(model).build();
  var factory=new StaticListableBeanFactory();factory.addBean("store",store);
  var catalog=mock(ProductService.class);var repository=mock(ProductEmbeddingRepository.class);
  Map<String,ProductEmbedding> states=new HashMap<>();
  when(repository.findByStoreKey(anyString())).thenAnswer(i->new ArrayList<>(states.values()));
  when(repository.save(any())).thenAnswer(i->{ProductEmbedding s=i.getArgument(0);states.put(s.getId(),s);return s;});
  doAnswer(i->{states.remove(i.getArgument(0));return null;}).when(repository).deleteById(anyString());
  var category=new Category();category.setId(1L);category.setName("Audio");
  var headphones=new Product();headphones.setId(1L);headphones.setName("Headphones");headphones.setCategory(category);headphones.setQuantity(2);headphones.setPrice(new java.math.BigDecimal("79.00"));headphones.setDescription("<b>Tai nghe</b> không dây");
  var live=new ArrayList<Product>();live.add(headphones);when(catalog.findAll()).thenAnswer(i->List.copyOf(live));
  var index=new ProductEmbeddingService(factory.getBeanProvider(VectorStore.class),catalog,repository,true,directory.toString(),"simple","test");
  assertEquals(1,index.sync().indexed());assertEquals(1,model.count.get());assertEquals(1,states.size());
  index.sync();assertEquals(1,model.count.get());
  headphones.setPrice(new java.math.BigDecimal("59.00"));headphones.setQuantity(1);index.sync();assertEquals(1,model.count.get());
  var search=new ProductSearchService(catalog,index);
  var result=search.search("am thanh",null,true);assertEquals("SEMANTIC",result.mode());assertEquals(1,result.products().size());assertEquals("59.00",result.products().get(0).price().toString());
  headphones.setDescription("Tai nghe chống nước");model.fail=true;assertEquals(1,index.sync().failed());
  assertTrue(search.search("am thanh",null,true).products().isEmpty()); // Old vector cannot recommend edited content.
  model.fail=false;index.sync();assertEquals(2,model.count.get());assertEquals(1,states.size());
  var restored=SimpleVectorStore.builder(model).build();restored.load(directory.resolve("products-text3small-1536.json").toFile());
  assertEquals(1,restored.similaritySearch("am thanh").size());
  live.clear();index.sync();assertTrue(states.isEmpty());assertTrue(store.similaritySearch("am thanh").isEmpty());
 }
}
