package iuh.fit.springai;
import iuh.fit.springai.services.*;
import org.springframework.ai.chat.model.*;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.prompt.*;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class AssistantModelTests {
 @Test void ragUsesRetrievedDataAndUserPreferencesThenFallsBackOnModelFailure(){
  var model=mock(ChatModel.class, CALLS_REAL_METHODS);when(model.getDefaultOptions()).thenReturn(ChatOptions.builder().build());
  when(model.call(any(Prompt.class))).thenReturn(new ChatResponse(List.of(new Generation(AssistantMessage.builder().content("Bạn có thể tham khảo tai nghe Sony trong danh sách.").build()))));
  var factory=new StaticListableBeanFactory();factory.addBean("model",model);
  var search=mock(ProductSearchService.class);var orders=mock(OrderService.class);
  var card=new ProductSearchService.ProductCard(1L,"Headphones Sony","Sony","Tai nghe không dây","Bluetooth","Audio",new java.math.BigDecimal("79.00"),3,null,"/product/1");
  when(search.search(anyString(),any(),anyBoolean())).thenReturn(new ProductSearchService.Result("SEMANTIC","",List.of(card)));
  var service=new AssistantService(factory.getBeanProvider(ChatModel.class),search,orders);
  var history=List.of(new ChatConversation.Message("user","Tôi muốn tai nghe",List.of(),List.of(),""),
   new ChatConversation.Message("assistant","Giá cũ không nên lặp lại: 10 USD",List.of(),List.of(),"AI"));
  var reply=service.reply("Có loại không dây không?",null,history,null,true);
  assertEquals("AI",reply.mode());assertEquals(1,reply.products().size());
  var prompt=ArgumentCaptor.forClass(Prompt.class);verify(model).call(prompt.capture());
  String all=prompt.getValue().getContents();assertTrue(all.contains("79.00"));assertTrue(all.contains("Tôi muốn tai nghe"));assertFalse(all.contains("10 USD"));
  when(model.call(any(Prompt.class))).thenThrow(new IllegalStateException("API unavailable"));
  assertEquals("CATALOG",service.reply("tai nghe",null,List.of(),null,false).mode());
 }
}
