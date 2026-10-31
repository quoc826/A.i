package iuh.fit.springai.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.*;
import org.springframework.ai.chroma.vectorstore.ChromaVectorStore;
import org.springframework.ai.chroma.vectorstore.ChromaApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.EnableAsync;
import java.nio.file.*;

@Configuration @EnableScheduling @EnableAsync
public class AiVectorConfig {
 @Bean @Lazy @ConditionalOnProperty(name="app.ai.enabled",havingValue="true")
 VectorStore productVectorStore(EmbeddingModel model,
   @Value("${app.ai.vector-store:simple}") String type,
   @Value("${app.ai.index-directory:./data/vector-index}") String directory,
   @Value("${app.ai.chroma-url:http://localhost:8000}") String url,
   @Value("${app.ai.chroma-collection:shopping-products-text3small-v1}") String collection,tools.jackson.databind.json.JsonMapper mapper) throws Exception {
  if("chroma".equals(type)){
   var factory=new SimpleClientHttpRequestFactory();factory.setConnectTimeout(5000);factory.setReadTimeout(15000);
   return ChromaVectorStore.builder(new ChromaApi(url,RestClient.builder().requestFactory(factory),mapper),model)
    .collectionName(collection).initializeSchema(true).build();
  }
  if(!"simple".equals(type))throw new IllegalArgumentException("Unknown vector store: "+type);
  var store=SimpleVectorStore.builder(model).build();
  Path file=Path.of(directory,"products-text3small-1536.json");
  if(Files.exists(file))store.load(file.toFile());
  return store;
 }
}
