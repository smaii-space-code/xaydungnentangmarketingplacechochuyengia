package vn.biz04;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.support.TransactionTemplate;
import vn.biz04.data.Store;
import vn.biz04.domain.AnhDaiDienChuyenGia;
import vn.biz04.domain.NguoiDung;
import vn.biz04.service.*;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:expertavatars;MODE=MSSQLServer;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false")
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@ActiveProfiles("test")
class ExpertAvatarTest {
    static final String BASE = "/quan-tri/tai-khoan/";
    static final String PASSWORD = "Avatar-fixture-only-password";
    static final Path TEMP = makeTemp();
    static final Path UPLOADS = TEMP.resolve("uploads");
    @Autowired MockMvc mvc;
    @Autowired TransactionTemplate tx;
    @Autowired Store db;
    @Autowired UserDetailsService users;
    @Autowired ExpertAvatars avatars;
    @Autowired CatalogService catalog;
    @Autowired ServiceApproval approval;
    NguoiDung admin, expert, customer;

    private static Path makeTemp() {
        try {
            return Files.createTempDirectory("biz04-expert-avatar-test-");
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    @DynamicPropertySource
    static void uploads(DynamicPropertyRegistry p) {
        p.add("biz.upload-dir", UPLOADS::toString);
    }

    @BeforeEach
    void fixtures() {
        tx.executeWithoutResult(s -> {
            admin = TestAccounts.create(db, "Quản trị ảnh thử", email(), PASSWORD, false, false);
            admin.setLaQuanTriVien(true);
            expert = TestAccounts.create(db, "Chuyên gia thử " + UUID.randomUUID(), email(), PASSWORD, false, true);
            customer = TestAccounts.create(db, "Khách thử", email(), PASSWORD, true, false);
        });
    }

    static String email() {
        return "expert-avatar-" + UUID.randomUUID() + "@example.test";
    }

    static MockMultipartFile file(byte[] bytes) {
        // The original name and supplied MIME deliberately disagree with every allowed image type.
        return new MockMultipartFile("avatar", "form.txt", "text/plain", bytes);
    }

    static byte[] image(String kind, Color color) throws IOException {
        if ("webp".equals(kind)) {
            // Lossless 2×2 WebP fixture generated from a solid color.
            return Base64.getDecoder().decode("UklGRhwAAABXRUJQVlA4TA8AAAAvAUAAAAcQ0f/+ByKi/wEA");
        }
        var image = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, color.getRGB());
        var out = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(image, kind, out));
        return out.toByteArray();
    }

    Long expertId() { return expert.getMaNguoiDung(); }
    Long adminId() { return admin.getMaNguoiDung(); }
    String adminUrl(Long id) { return BASE + id + "/anh-dai-dien"; }
    String publicUrl(Long id) { return "/anh-chuyen-gia/" + id; }

    MockHttpServletRequestBuilder asAdmin(MockHttpServletRequestBuilder request) {
        return request.with(user(users.loadUserByUsername(admin.getEmail())));
    }

    Long publishService() {
        var service = catalog.service(expertId(), null, "Dịch vụ ảnh thử", "Thiết kế", "Mô tả dịch vụ kiểm thử ảnh đại diện.", BigDecimal.TEN);
        approval.submit(expertId(), service.getMaDichVu(), true);
        approval.review(adminId(), service.getMaDichVu(), service.getPhienBanNoiDung(), true, null);
        return service.getMaDichVu();
    }

    String ref(Long id) {
        return tx.execute(s -> db.first("select a.fileRef from AnhDaiDienChuyenGia a where a.userId=:id",
                String.class, "id", id).orElse(null));
    }

    MockHttpServletRequestBuilder create(String kind, String email, MockMultipartFile photo) {
        var request = multipart(BASE + "tao");
        if (photo != null) request.file(photo);
        return asAdmin(request.with(csrf()).param("kind", kind).param("email", email)
                .param("hoTen", "Chuyên gia có hồ sơ thử").param("password", PASSWORD).param("passwordAgain", PASSWORD)
                .param("soDienThoai", "0962481678").param("linhVuc", "Thiết kế")
                .param("gioiThieu", "Giới thiệu chuyên môn đầy đủ để xét hồ sơ."));
    }

    NguoiDung account(String email) {
        return tx.execute(s -> db.first("from NguoiDung u where u.email=:e", NguoiDung.class,
                "e", email).orElse(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"jpeg", "png", "webp"})
    void detectsContentAndServesCorrectMime(String format) throws Exception {
        byte[] bytes = image(format, Color.RED);
        mvc.perform(asAdmin(multipart(adminUrl(expertId())).file(file(bytes)).with(csrf())))
                .andExpect(redirectedUrl(BASE + expertId())).andExpect(flash().attribute("success", containsString("Đã lưu")));
        mvc.perform(asAdmin(get(adminUrl(expertId())))).andExpect(status().isOk())
                .andExpect(content().contentType("image/" + format))
                .andExpect(content().bytes(bytes)).andExpect(header().string("Cache-Control", "no-store"));
        assertTrue(ref(expertId()).endsWith("jpeg".equals(format) ? ".jpg" : "." + format));
        assertEquals(1L, tx.<Long>execute(s -> db.count("select count(n) from NhatKyXuLy n where n.maDoiTuong=:id and n.maNguoiThucHien=:admin and n.hanhDong='CAP_NHAT_ANH_DAI_DIEN'",
                "id", expertId(), "admin", adminId())));
    }

    @Test
    void adminDefaultAndPublicImagesRespectTheExistingPublicationRules() throws Exception {
        mvc.perform(asAdmin(get(BASE + expertId()))).andExpect(content().string(containsString("Ảnh đại diện mặc định")))
                .andExpect(content().string(containsString("Đổi ảnh đại diện")));
        byte[] old = image("png", Color.RED), replacement = image("png", Color.BLUE);
        avatars.replace(adminId(), expertId(), file(old));
        String oldRef = ref(expertId());
        mvc.perform(get(publicUrl(expertId()))).andExpect(status().isNotFound());
        Long serviceId = publishService();
        avatars.replace(adminId(), expertId(), file(replacement));
        assertFalse(Files.exists(UPLOADS.resolve(oldRef.substring(5))));
        mvc.perform(get(publicUrl(expertId()))).andExpect(status().isOk()).andExpect(content().bytes(replacement));
        mvc.perform(asAdmin(get(BASE + expertId()))).andExpect(content().string(containsString(adminUrl(expertId()))));
        mvc.perform(get("/trang/trangchu").param("q", expert.getHoTen()))
                .andExpect(content().string(containsString(publicUrl(expertId()))));
        mvc.perform(get("/chuyen-gia/" + expertId()).with(user(customer.getEmail())))
                .andExpect(content().string(containsString(publicUrl(expertId()))));
        mvc.perform(get("/dich-vu/xem/" + serviceId).with(user(customer.getEmail())))
                .andExpect(content().string(containsString(publicUrl(expertId()))));
        catalog.visibility(expertId(), serviceId, false);
        mvc.perform(get(publicUrl(expertId()))).andExpect(status().isNotFound());
        mvc.perform(asAdmin(get(adminUrl(expertId())))).andExpect(content().bytes(replacement));
    }

    @Test
    void createsPendingExpertWithOptionalImageAndRejectsBadCreationWithoutPartialAccount() throws Exception {
        String withImage = email(), withoutImage = email(), invalid = email(), wrongRole = email();
        byte[] photo = image("png", Color.BLUE);
        mvc.perform(create("CHUYEN_GIA", withImage, file(photo))).andExpect(status().is3xxRedirection());
        var account = account(withImage);
        assertEquals("CHO_DUYET_CHUYEN_GIA", account.getTrangThai());
        assertEquals("DA_XAC_THUC", account.getTrangThaiEmail());
        assertFalse(account.getLaChuyenGia());
        assertEquals("CHO_DUYET", tx.execute(s -> db.get(vn.biz04.domain.HoSoChuyenGia.class, account.getMaNguoiDung()).state));
        mvc.perform(asAdmin(get(adminUrl(account.getMaNguoiDung())))).andExpect(content().bytes(photo));
        mvc.perform(get(publicUrl(account.getMaNguoiDung()))).andExpect(status().isNotFound());
        mvc.perform(create("CHUYEN_GIA", withoutImage, null)).andExpect(status().is3xxRedirection());
        assertNull(ref(account(withoutImage).getMaNguoiDung()));
        mvc.perform(create("CHUYEN_GIA", invalid, file(new byte[] {1, 2}))).andExpect(status().isOk())
                .andExpect(content().string(containsString("ảnh JPEG, PNG hoặc WebP")));
        assertNull(account(invalid));
        mvc.perform(create("KHACH_HANG", wrongRole, file(photo))).andExpect(status().isOk())
                .andExpect(content().string(containsString("Chỉ đính kèm ảnh khi tạo chuyên gia")));
        assertNull(account(wrongRole));
    }

    @Test
    void invalidOversizedAndUnauthorizedRequestsLeaveOriginalAvatar() throws Exception {
        byte[] old = image("png", Color.RED);
        avatars.replace(adminId(), expertId(), file(old));
        mvc.perform(asAdmin(multipart(adminUrl(expertId())).file(file(new byte[] {1, 2})).with(csrf())))
                .andExpect(flash().attribute("error", containsString("ảnh JPEG, PNG hoặc WebP")));
        mvc.perform(asAdmin(multipart(adminUrl(expertId())).file(file(new byte[ExpertAvatars.MAX_BYTES + 1])).with(csrf())))
                .andExpect(flash().attribute("error", containsString("2 MB")));
        mvc.perform(asAdmin(multipart(adminUrl(expertId())).file(file(old)))).andExpect(status().isForbidden());
        mvc.perform(multipart(adminUrl(expertId())).file(file(old)).with(csrf())
                .with(user(customer.getEmail()))).andExpect(status().isForbidden());
        mvc.perform(get(adminUrl(expertId())).with(user(customer.getEmail()))).andExpect(status().isForbidden());
        mvc.perform(asAdmin(multipart(adminUrl(customer.getMaNguoiDung())).file(file(old)).with(csrf())))
                .andExpect(flash().attribute("error", containsString("tài khoản chuyên gia")));
        mvc.perform(asAdmin(get(adminUrl(expertId())))).andExpect(content().bytes(old));
    }

    @Test
    void fileWriteFailureAndDatabaseRollbackKeepPreviousImage() throws Exception {
        byte[] old = image("png", Color.RED), next = image("png", Color.BLUE);
        avatars.replace(adminId(), expertId(), file(old));
        String oldRef = ref(expertId());
        Set<String> before = uploadedNames();
        tx.executeWithoutResult(s -> {
            avatars.replace(adminId(), expertId(), file(next));
            db.flush();
            s.setRollbackOnly();
        });
        assertEquals(oldRef, ref(expertId()));
        assertEquals(before, uploadedNames());

        Path backup = TEMP.resolve("uploads-backup");
        Files.move(UPLOADS, backup);
        try {
            Files.createFile(UPLOADS);
            mvc.perform(asAdmin(multipart(adminUrl(expertId())).file(file(next)).with(csrf())))
                    .andExpect(flash().attribute("error", containsString("Không lưu được tệp ảnh")));
            assertEquals(oldRef, ref(expertId()));
        } finally {
            if (Files.isRegularFile(UPLOADS)) Files.delete(UPLOADS);
            Files.move(backup, UPLOADS);
        }
        mvc.perform(asAdmin(get(adminUrl(expertId())))).andExpect(content().bytes(old));
    }

    private Set<String> uploadedNames() throws IOException {
        try (var files = Files.list(UPLOADS)) {
            return files.map(p -> p.getFileName().toString()).collect(Collectors.toSet());
        }
    }
}
