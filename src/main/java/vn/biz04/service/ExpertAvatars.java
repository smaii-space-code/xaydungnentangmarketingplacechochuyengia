package vn.biz04.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Locale;
import java.util.TreeSet;
import java.util.List;
import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import vn.biz04.data.Store;
import vn.biz04.domain.AnhDaiDienChuyenGia;
import vn.biz04.domain.NguoiDung;

@Service
@Transactional
public class ExpertAvatars {
    public static final int MAX_BYTES = 2 * 1024 * 1024;
    private static final int MAX_EDGE = 4096;
    private static final String INVALID = "Tệp phải là ảnh JPEG, PNG hoặc WebP đọc được.";
    private final Store db;
    private final Access access;
    private final FileStore files;

    public ExpertAvatars(Store db, Access access, FileStore files) {
        this.db = db;
        this.access = access;
        this.files = files;
    }

    public record Image(byte[] bytes, String contentType) {}

    public static boolean selected(MultipartFile file) {
        return file != null && (!file.isEmpty()
                || (file.getOriginalFilename() != null && !file.getOriginalFilename().isBlank()));
    }

    public void replace(Long admin, Long id, MultipartFile file) {
        for (Long user : new TreeSet<>(List.of(admin, id))) db.lockEntity(NguoiDung.class, user);
        access.admin(admin);
        NguoiDung expert = db.get(NguoiDung.class, id);
        Checks.require(!expert.getLaQuanTriVien() && (expert.getLaChuyenGia()
                || db.count("select count(h) from HoSoChuyenGia h where h.userId=:id", "id", id) > 0),
                "Chỉ cập nhật ảnh đại diện cho tài khoản chuyên gia.");
        Checks.require(file != null && !file.isEmpty(), "Vui lòng chọn ảnh đại diện.");
        Checks.require(file.getSize() <= MAX_BYTES, "Ảnh đại diện không được vượt quá 2 MB.");

        byte[] bytes;
        try (var input = file.getInputStream()) {
            bytes = input.readNBytes(MAX_BYTES + 1);
        } catch (IOException ex) {
            throw new BusinessException("Không đọc được tệp ảnh. Vui lòng chọn lại.");
        }
        Checks.require(bytes.length <= MAX_BYTES, "Ảnh đại diện không được vượt quá 2 MB.");
        String extension = extension(bytes);
        var previous = db.first("from AnhDaiDienChuyenGia a where a.userId=:id", AnhDaiDienChuyenGia.class, "id", id);
        String ref;
        try {
            ref = files.saveAvatar(bytes, extension);
        } catch (IOException | SecurityException ex) {
            throw new BusinessException("Không lưu được tệp ảnh. Vui lòng kiểm tra thư mục tải lên rồi thử lại.");
        }

        if (previous.isPresent()) {
            var avatar = previous.get();
            files.deleteAvatarAfterCommit(avatar.fileRef);
            avatar.fileRef = ref;
        } else {
            var avatar = new AnhDaiDienChuyenGia();
            avatar.userId = id;
            avatar.fileRef = ref;
            db.save(avatar);
        }
        access.audit(admin, "AnhDaiDienChuyenGia", id, "CAP_NHAT_ANH_DAI_DIEN", null);
    }

    private static String extension(byte[] bytes) {
        try (var input = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
            var readers = ImageIO.getImageReaders(input);
            Checks.require(readers.hasNext(), INVALID);
            var reader = readers.next();
            try {
                String extension = switch (reader.getFormatName().toLowerCase(Locale.ROOT)) {
                    case "jpeg", "jpg" -> "jpg";
                    case "png" -> "png";
                    case "webp" -> "webp";
                    default -> throw new BusinessException(INVALID);
                };
                reader.setInput(input, false, true);
                int width = reader.getWidth(0), height = reader.getHeight(0);
                Checks.require(width > 0 && height > 0 && width <= MAX_EDGE && height <= MAX_EDGE,
                        "Mỗi chiều ảnh phải từ 1 đến 4.096 pixel.");
                reader.read(0);
                return extension;
            } finally {
                reader.dispose();
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (IOException | RuntimeException ex) {
            throw new BusinessException(INVALID);
        }
    }

    @Transactional(readOnly = true)
    public Image forAdmin(Long admin, Long id) {
        access.admin(admin);
        return load(db.first("select a.fileRef from AnhDaiDienChuyenGia a where a.userId=:id", String.class, "id", id)
                .orElseThrow(ExpertAvatars::missing));
    }

    @Transactional(readOnly = true)
    public Image forPublic(Long id) {
        return load(db.first("select a.fileRef from AnhDaiDienChuyenGia a, NguoiDung u where a.userId=u.maNguoiDung"
                + " and a.userId=:id and " + PublicCatalogPolicy.EXPERT, String.class, "id", id)
                .orElseThrow(ExpertAvatars::missing));
    }

    private Image load(String ref) {
        String contentType = switch (ref.substring(ref.lastIndexOf('.') + 1)) {
            case "jpg" -> "image/jpeg";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            default -> throw missing();
        };
        try (var input = Files.newInputStream(files.path(ref))) {
            byte[] bytes = input.readNBytes(MAX_BYTES + 1);
            if (bytes.length == 0 || bytes.length > MAX_BYTES) throw missing();
            return new Image(bytes, contentType);
        } catch (IOException | BusinessException ex) {
            throw missing();
        }
    }

    private static ResponseStatusException missing() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy ảnh đại diện.");
    }
}
