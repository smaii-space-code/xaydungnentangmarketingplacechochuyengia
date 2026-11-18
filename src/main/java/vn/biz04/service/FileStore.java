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
    public String save(MultipartFile file) throws IOException {
        Checks.require(file != null && !file.isEmpty() && file.getSize() <= 20*1024*1024, "Tệp cần có nội dung và không quá 20 MB.");
        String name = Objects.toString(file.getOriginalFilename(), "");
        String ext = name.contains(".") ? name.substring(name.lastIndexOf('.')+1).toLowerCase(Locale.ROOT) : "";
        Checks.require(Set.of("pdf","zip","png","jpg","jpeg","webp","txt","docx","xlsx","pptx").contains(ext), "Loại tệp chưa được hỗ trợ.");
        return store(file.getInputStream(), ext);
    }

    String saveAvatar(byte[] bytes, String extension) throws IOException {
        Checks.require(Set.of("jpg", "png", "webp").contains(extension), "Định dạng ảnh không được hỗ trợ.");
        return store(new ByteArrayInputStream(bytes), extension);
    }

    private String store(InputStream source, String extension) throws IOException {
        String key = UUID.randomUUID() + "." + extension;
        Path saved = root.resolve(key);
        boolean created = false;
        try (source) {
            Files.createDirectories(root);
            try (var out = Files.newOutputStream(saved, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                created = true;
                source.transferTo(out);
            }
        } catch (IOException | RuntimeException ex) {
            if (created) delete(saved);
            throw ex;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCompletion(int status) {
                    if (status != STATUS_COMMITTED) delete(saved);
                }
            });
        }
        return "file:" + key;
    }

    void deleteAvatarAfterCommit(String ref) {
        if (ref == null) return;
        Path previous = resolve(ref);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { delete(previous); }
        });
    }

    private void delete(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException | SecurityException ex) {
            org.slf4j.LoggerFactory.getLogger(FileStore.class).warn("Không dọn được tệp đã bỏ tham chiếu.");
        }
    }

    public Path path(String ref) {
        Path file = resolve(ref);
        Checks.require(Files.isRegularFile(file), "Tệp không tồn tại.");
        return file;
    }

    private Path resolve(String ref) {
        Checks.require(ref != null && ref.matches("file:[a-f0-9\\-]{36}\\.[a-z0-9]+"), "Tệp không hợp lệ.");
        Path file = root.resolve(ref.substring(5)).normalize();
        Checks.require(file.startsWith(root), "Tệp không hợp lệ.");
        return file;
    }
}
