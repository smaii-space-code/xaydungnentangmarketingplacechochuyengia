package vn.biz04.service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.*;
import java.io.*;
import java.util.*;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
@Service
public class FileStore {
    private final Path root;
    public FileStore(@Value("${biz.upload-dir:./du-lieu/tep}")String root){this.root=Path.of(root).toAbsolutePath().normalize();}
    public String save(MultipartFile file)throws IOException {Checks.require(file!=null&&!file.isEmpty()&&file.getSize()<=20*1024*1024,"Tệp cần có nội dung và không quá 20 MB.");String name=file.getOriginalFilename()==null?"":file.getOriginalFilename();String ext=name.contains(".")?name.substring(name.lastIndexOf('.')+1).toLowerCase(Locale.ROOT):"";Checks.require(Set.of("pdf","zip","png","jpg","jpeg","webp","txt","docx","xlsx","pptx").contains(ext),"Loại tệp chưa được hỗ trợ.");Files.createDirectories(root);String key=UUID.randomUUID()+"."+ext;try(InputStream in=file.getInputStream()){Files.copy(in,root.resolve(key));}if(TransactionSynchronizationManager.isSynchronizationActive()){Path saved=root.resolve(key);TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){@Override public void afterCompletion(int status){if(status!=STATUS_COMMITTED)try{Files.deleteIfExists(saved);}catch(IOException ignored){}}});}return "file:"+key;}
    public Path path(String ref){Checks.require(ref!=null&&ref.matches("file:[a-f0-9\\-]{36}\\.[a-z0-9]+"),"Tệp không hợp lệ.");Path p=root.resolve(ref.substring(5)).normalize();Checks.require(p.startsWith(root)&&Files.isRegularFile(p),"Tệp không tồn tại.");return p;}
}
