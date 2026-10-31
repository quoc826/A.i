package iuh.fit.springai.services;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.*;
import java.io.IOException;
import java.util.UUID;
import javax.imageio.ImageIO;
@Service public class ImageStorage {
 public String store(MultipartFile file) throws IOException {
  if(file.isEmpty())return null;
  if(file.getSize()>2*1024*1024)throw new IllegalArgumentException("Ảnh tối đa 2MB");
  var decoded=ImageIO.read(file.getInputStream());
  if(decoded==null || decoded.getWidth()>6000 || decoded.getHeight()>6000)throw new IllegalArgumentException("Ảnh không hợp lệ");
  Path dir=Path.of("uploads").toAbsolutePath();Files.createDirectories(dir);
  String name=UUID.randomUUID()+".png";ImageIO.write(decoded,"png",dir.resolve(name).toFile());return name;
 }
}
