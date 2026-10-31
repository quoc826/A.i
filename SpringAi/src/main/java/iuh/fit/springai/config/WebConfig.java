package iuh.fit.springai.config;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;
import java.nio.file.Path;
@Configuration public class WebConfig implements WebMvcConfigurer {
 @Override public void addResourceHandlers(ResourceHandlerRegistry registry){registry.addResourceHandler("/uploads/**").addResourceLocations(Path.of("uploads").toAbsolutePath().toUri().toString()+"/");}
}
