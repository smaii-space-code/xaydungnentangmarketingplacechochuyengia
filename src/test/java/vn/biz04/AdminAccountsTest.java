package vn.biz04;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.support.TransactionTemplate;
import vn.biz04.data.Store;
import vn.biz04.domain.*;
import vn.biz04.service.*;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:adminaccounts;MODE=MSSQLServer;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
        "biz.otp.hmac-key=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="})
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@ActiveProfiles("test")
@Import(ExpertRegistrationTest.Config.class)
class AdminAccountsTest {
    static final String BASE = "/quan-tri/tai-khoan/";
    static final String TEST_PASSWORD = "Fixture-only-password";
    static final String BIO = "Tư vấn và phát triển phần mềm cho doanh nghiệp.";
    @Autowired MockMvc mvc;
    @Autowired Store db;
    @Autowired TransactionTemplate tx;
    @Autowired AdminAccounts accounts;
    @Autowired AccountService registrations;
    @Autowired OtpService otp;
    @Autowired ExpertApplications expertApps;
    @Autowired CatalogService catalog;
    @Autowired UserDetailsService users;
    @Autowired ExpertRegistrationTest.MutableClock clock;
    @MockitoBean OtpMailGateway mail;
    NguoiDung admin, customer;
    String ip, receivedCode;

    @BeforeEach
    void prepare() {
        clock.advance(3601);
        ip = UUID.randomUUID().toString();
        admin = fixture(true);
        customer = fixture(false);
        when(mail.send(anyString(), anyString())).thenAnswer(call -> {
            receivedCode = call.getArgument(1);
            return OtpMailGateway.Delivery.ACCEPTED;
        });
    }

    NguoiDung fixture(boolean isAdmin) {
        return tx.execute(s -> {
            var u = TestAccounts.create(db, isAdmin ? "Admin thử" : "Khách thử", address(), TEST_PASSWORD, !isAdmin, false);
            u.setLaQuanTriVien(isAdmin);
            if (!isAdmin) u.setNguonDangKy("KHACH_HANG");
            return u;
        });
    }

    static String address() { return "admin-account-" + UUID.randomUUID() + "@example.test"; }
    MockHttpServletRequestBuilder asAdmin(MockHttpServletRequestBuilder req) {
        return req.with(user(users.loadUserByUsername(admin.getEmail()))).with(r -> { r.setRemoteAddr(ip); return r; });
    }
    MockHttpServletRequestBuilder create(String kind, String email) {
        return asAdmin(post(BASE + "tao").with(csrf()).param("kind", kind)
                .param("hoTen", "Tài khoản kiểm thử").param("email", email).param("soDienThoai", "0962481678")
                .param("password", TEST_PASSWORD).param("passwordAgain", TEST_PASSWORD)
                .param("loaiKhachHang", "CA_NHAN").param("linhVuc", "Phần mềm").param("gioiThieu", BIO));
    }
    NguoiDung find(String email) {
        return tx.execute(s -> db.first("from NguoiDung u where u.email=:e", NguoiDung.class, "e", email).orElse(null));
    }

    @Test
    void listingShowsRoleDetailMasksIdentityAndExistingLockWorks() throws Exception {
        Long id = customer.getMaNguoiDung();
        tx.executeWithoutResult(s -> {
            var h = new HoSoKhachHang();
            h.maNguoiDung = id;
            h.cccdBonSoCuoi = "6789";
            h.soCccdMaHoa = "fixture-encrypted-identity";
            db.save(h);
        });
        mvc.perform(asAdmin(get("/quan-tri"))).andExpect(status().isOk())
                .andExpect(content().string(containsString(BASE + id)))
                .andExpect(content().string(containsString("Vai trò")));
        mvc.perform(asAdmin(get(BASE + id))).andExpect(status().isOk())
                .andExpect(content().string(containsString("********6789")))
                .andExpect(content().string(not(containsString("fixture-encrypted-identity"))))
                .andExpect(content().string(not(containsString("MatKhauBam"))));
        mvc.perform(asAdmin(post(BASE + id).with(csrf()).param("khoa", "true")))
                .andExpect(redirectedUrl("/quan-tri"));
        assertEquals("KHOA", find(customer.getEmail()).getTrangThai());
    }

    @Test
    void createCustomerNormalizesEmailConfirmsAndSignsInWithoutMail() throws Exception {
        String email = address();
        when(mail.send(anyString(), anyString())).thenReturn(OtpMailGateway.Delivery.FAILED);
        var response = mvc.perform(create("KHACH_HANG", " " + email.toUpperCase(java.util.Locale.ROOT) + " "))
                .andExpect(flash().attribute("success", containsString("có thể đăng nhập ngay"))).andReturn();
        NguoiDung u = find(email);
        assertNotNull(u);
        assertEquals(BASE + u.getMaNguoiDung(), response.getResponse().getRedirectedUrl());
        assertEquals("HOAT_DONG", u.getTrangThai());
        assertEquals("DA_XAC_THUC", u.getTrangThaiEmail());
        assertNotNull(u.getThoiDiemXacThucEmail());
        assertFalse(u.getLaQuanTriVien());
        assertEquals("0962481678", accounts.detail(admin.getMaNguoiDung(), u.getMaNguoiDung()).contact().phone());
        assertEquals(1L, tx.<Long>execute(s -> db.count("select count(n) from NhatKyXuLy n where n.maDoiTuong=:u and n.maNguoiThucHien=:a and n.hanhDong='TAO_TAI_KHOAN_QUAN_TRI'", "u", u.getMaNguoiDung(), "a", admin.getMaNguoiDung())));
        mvc.perform(create("KHACH_HANG", email)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Email đã được sử dụng")))
                .andExpect(content().string(not(containsString(TEST_PASSWORD))));
        mvc.perform(post("/dang-nhap").with(csrf()).param("email", email).param("password", TEST_PASSWORD))
                .andExpect(redirectedUrl("/bang-dieu-khien"));
        org.mockito.Mockito.verifyNoInteractions(mail);
    }

    @Test
    void createExpertSkipsOtpAndWaitsForExistingExpertReview() throws Exception {
        String email = address();
        mvc.perform(asAdmin(get(BASE + "tao"))).andExpect(status().isOk());
        mvc.perform(create("CHUYEN_GIA", email)).andExpect(flash().attribute("success", containsString("đang chờ duyệt")));
        var u = find(email);
        var id = u.getMaNguoiDung();
        assertFalse(u.getLaChuyenGia());
        assertEquals("DA_XAC_THUC", u.getTrangThaiEmail());
        assertEquals("CHO_DUYET", accounts.detail(admin.getMaNguoiDung(), id).expert().state());
        assertEquals("CHO_DUYET_CHUYEN_GIA", find(email).getTrangThai());
        assertFalse(find(email).getLaChuyenGia());
        mvc.perform(asAdmin(get(BASE + id))).andExpect(content().string(containsString("Chờ admin duyệt")));
        expertApps.decide(admin.getMaNguoiDung(), id, 1, true, null);
        assertTrue(find(email).getLaChuyenGia());
        mvc.perform(post("/dang-nhap").with(csrf()).param("email", email).param("password", TEST_PASSWORD))
                .andExpect(redirectedUrl("/bang-dieu-khien"));
        org.mockito.Mockito.verifyNoInteractions(mail);
    }

    @Test
    void publicCustomerCanContinueFromLoginThroughOtpAndSignIn() throws Exception {
        String email = address(), otpStart = "/xac-thuc-email/tu-dang-nhap";
        registrations.register(publicForm(email));
        mvc.perform(post(otpStart).with(csrf())).andExpect(redirectedUrl("/dang-nhap"));
        mvc.perform(post("/dang-nhap").with(csrf()).param("email", email).param("password", "Wrong-fixture-password"))
                .andExpect(redirectedUrl("/dang-nhap?error"));
        var login = mvc.perform(post("/dang-nhap").with(csrf()).param("email", email).param("password", TEST_PASSWORD))
                .andExpect(redirectedUrl(otpStart)).andReturn();
        var session = (MockHttpSession) login.getRequest().getSession(false);
        assertNull(session.getAttribute("SPRING_SECURITY_CONTEXT"));
        assertNull(receivedCode);
        mvc.perform(get(otpStart).session(session)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Gửi mã xác thực")))
                .andExpect(content().string(not(containsString(email))));
        mvc.perform(post(otpStart).session(session)).andExpect(status().isForbidden());
        when(mail.send(anyString(), anyString())).thenReturn(OtpMailGateway.Delivery.FAILED);
        mvc.perform(post(otpStart).session(session).with(csrf())).andExpect(redirectedUrl("/xac-thuc-email"))
                .andExpect(flash().attribute("error", containsString("chưa gửi được mã")));
        assertEquals("CHO_XAC_THUC", find(email).getTrangThai());
        clock.advance(61);
        when(mail.send(anyString(), anyString())).thenAnswer(call -> {
            receivedCode = call.getArgument(1);
            return OtpMailGateway.Delivery.ACCEPTED;
        });
        mvc.perform(post("/xac-thuc-email/gui-lai").session(session).with(csrf()))
                .andExpect(redirectedUrl("/xac-thuc-email"));
        assertNotNull(receivedCode);
        mvc.perform(post("/xac-thuc-email").session(session).with(csrf()).param("code", receivedCode))
                .andExpect(redirectedUrl("/dang-nhap"));
        mvc.perform(post("/dang-nhap").session(session).with(csrf()).param("email", email).param("password", TEST_PASSWORD))
                .andExpect(redirectedUrl("/bang-dieu-khien"));
        assertEquals("DA_XAC_THUC", find(email).getTrangThaiEmail());
    }

    @Test
    void otpLoginProofExpiresAndCannotSendAfterSecurityVersionChanges() throws Exception {
        String email = address(), otpStart = "/xac-thuc-email/tu-dang-nhap";
        registrations.register(publicForm(email));
        var session = new MockHttpSession();
        mvc.perform(post("/dang-nhap").session(session).with(csrf()).param("email", email).param("password", TEST_PASSWORD))
                .andExpect(redirectedUrl(otpStart));
        clock.advance(601);
        mvc.perform(post(otpStart).session(session).with(csrf())).andExpect(redirectedUrl("/dang-nhap"));
        mvc.perform(post("/dang-nhap").session(session).with(csrf()).param("email", email).param("password", TEST_PASSWORD))
                .andExpect(redirectedUrl(otpStart));
        tx.executeWithoutResult(s -> {
            var u = db.get(NguoiDung.class, find(email).getMaNguoiDung());
            u.setPhienBaoMat(u.getPhienBaoMat() + 1);
        });
        mvc.perform(post(otpStart).session(session).with(csrf())).andExpect(redirectedUrl("/dang-nhap"));
        assertNull(receivedCode);
    }

    private static AccountForm publicForm(String email) {
        var f = new AccountForm();
        f.setHoTen("Khách đăng ký");
        f.setEmail(email);
        f.setSoDienThoai("0962481678");
        f.setPassword(TEST_PASSWORD);
        f.setPasswordAgain(TEST_PASSWORD);
        return f;
    }

    @Test
    void onlyAdminCanConfirmAnOldAccountWithAdminCreationAudit() throws Exception {
        var ordinary = registrations.register(publicForm(address()));
        var old = registrations.register(publicForm(address()));
        tx.executeWithoutResult(s -> {
            var log = new NhatKyXuLy();
            log.setMaNguoiThucHien(admin.getMaNguoiDung());
            log.setLoaiDoiTuong("NguoiDung");
            log.setMaDoiTuong(old.getMaNguoiDung());
            log.setHanhDong("TAO_TAI_KHOAN_QUAN_TRI");
            log.setNoiDungLyDo("KHACH_HANG");
            log.setThoiDiem(java.time.LocalDateTime.now(clock));
            db.save(log);
        });
        String target = BASE + old.getMaNguoiDung() + "/xac-nhan-email";
        mvc.perform(asAdmin(get(BASE + old.getMaNguoiDung()))).andExpect(content().string(containsString(target)));
        mvc.perform(post(target).with(user(customer.getEmail())).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(asAdmin(post(target))).andExpect(status().isForbidden());
        mvc.perform(asAdmin(post(BASE + ordinary.getMaNguoiDung() + "/xac-nhan-email").with(csrf())))
                .andExpect(flash().attributeExists("error"));
        assertEquals("CHO_XAC_THUC", find(ordinary.getEmail()).getTrangThaiEmail());
        mvc.perform(asAdmin(post(target).with(csrf()))).andExpect(flash().attributeExists("success"));
        mvc.perform(asAdmin(post(target).with(csrf()))).andExpect(flash().attributeExists("error"));
        mvc.perform(post("/dang-nhap").with(csrf()).param("email", old.getEmail()).param("password", TEST_PASSWORD))
                .andExpect(redirectedUrl("/bang-dieu-khien"));
        assertEquals("DA_XAC_THUC", find(old.getEmail()).getTrangThaiEmail());
        org.mockito.Mockito.verifyNoInteractions(mail);
    }

    @Test
    void forbiddenCsrfAndTamperedRoleAndInvalidProfileDoNotCreateUsers() throws Exception {
        String email = address();
        mvc.perform(get(BASE + customer.getMaNguoiDung()).with(user(customer.getEmail()))).andExpect(status().isForbidden());
        mvc.perform(post(BASE + "tao").with(user(customer.getEmail())).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(post(BASE + customer.getMaNguoiDung() + "/chuyen-chuyen-gia")
                .with(user(customer.getEmail())).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(asAdmin(post(BASE + "tao"))).andExpect(status().isForbidden());
        mvc.perform(create("ADMIN", email)).andExpect(content().string(containsString("Chỉ được tạo")));
        mvc.perform(create("KHACH_HANG", email).param("laQuanTriVien", "true"))
                .andExpect(content().string(containsString("không được phép")));
        mvc.perform(asAdmin(post(BASE + "tao").with(csrf()).param("kind", "CHUYEN_GIA")
                        .param("email", email).param("hoTen", "Chuyên gia thiếu hồ sơ")))
                .andExpect(content().string(containsString("Lĩnh vực chuyên môn")));
        assertNull(find(email));
    }

    @Test
    void rollbackKeepsAccountAndProfileFromBeingPartiallyCreated() {
        String email = address();
        var form = new AccountForm();
        form.setEmail(email);
        form.setHoTen("Hồ sơ kiểm thử hoàn tác");
        form.setSoDienThoai("0962481678");
        form.setPassword(TEST_PASSWORD);
        form.setPasswordAgain(TEST_PASSWORD);
        form.setGioiThieu(BIO);
        Long id = tx.execute(s -> {
            Long created = accounts.create(admin.getMaNguoiDung(), "CHUYEN_GIA", form, "Phần mềm");
            db.flush();
            s.setRollbackOnly();
            return created;
        });
        assertNull(find(email));
        assertEquals(0L, tx.<Long>execute(s -> db.count("select count(h) from HoSoChuyenGia h where h.userId=:id", "id", id)));
        assertEquals(0L, tx.<Long>execute(s -> db.count("select count(h) from HoSoKhachHang h where h.maNguoiDung=:id", "id", id)));
    }

    @Test
    void reviewConfirmAndReplayKeepCustomerHistoryAndRefreshSession() throws Exception {
        Long id = customer.getMaNguoiDung();
        Long request = catalog.request(id, null, "Yêu cầu cũ của khách", BIO, BigDecimal.TEN, null).getMaYeuCau();
        catalog.closeRequest(id, request);
        var login = mvc.perform(post("/dang-nhap").with(csrf()).param("email", customer.getEmail())
                .param("password", TEST_PASSWORD)).andExpect(redirectedUrl("/bang-dieu-khien")).andReturn();
        var oldSession = (MockHttpSession) login.getRequest().getSession(false);
        String route = BASE + id + "/chuyen-chuyen-gia";
        mvc.perform(asAdmin(get(route))).andExpect(status().isOk()).andExpect(content().string(containsString("Lĩnh vực chuyên môn")));
        var session = new MockHttpSession();
        var result = mvc.perform(asAdmin(post(route + "/ra-soat").session(session).with(csrf())
                        .param("phone", "0962481678").param("field", "Phần mềm").param("bio", BIO).param("reason", "Đã kiểm tra hồ sơ.")))
                .andExpect(content().string(containsString("Duyệt và chuyển vai trò"))).andReturn();
        String ticket = (String) result.getModelAndView().getModel().get("ticket");
        assertTrue(find(customer.getEmail()).getLaKhachHang());
        mvc.perform(asAdmin(post(route).session(session).with(csrf()).param("ticket", ticket))).andExpect(redirectedUrl(BASE + id));
        mvc.perform(asAdmin(post(route).session(session).with(csrf()).param("ticket", ticket)))
                .andExpect(flash().attribute("error", containsString("đã được dùng")));
        var updated = find(customer.getEmail());
        assertEquals(id, updated.getMaNguoiDung());
        assertTrue(updated.getLaChuyenGia());
        assertFalse(updated.getLaKhachHang());
        assertEquals("KHACH_HANG", updated.getNguonDangKy());
        var decisions = expertApps.adminDetail(admin.getMaNguoiDung(), id).decisions();
        assertEquals(1, decisions.size());
        assertEquals("Đã kiểm tra hồ sơ.", decisions.getFirst().reason);
        assertEquals(admin.getMaNguoiDung(), decisions.getFirst().admin);
        assertEquals(1, decisions.getFirst().version);
        mvc.perform(get("/bang-dieu-khien").session(oldSession)).andExpect(redirectedUrl("/dang-nhap?session=expired"));
        mvc.perform(post("/dang-nhap").with(csrf()).param("email", customer.getEmail())
                .param("password", TEST_PASSWORD)).andExpect(redirectedUrl("/bang-dieu-khien"));
        var owner = users.loadUserByUsername(customer.getEmail());
        mvc.perform(get("/bang-dieu-khien").with(user(owner))).andExpect(content().string(containsString("Lịch sử yêu cầu của tôi")));
        mvc.perform(get("/yeu-cau").with(user(owner))).andExpect(content().string(containsString("Yêu cầu cũ của khách")));
        mvc.perform(get("/yeu-cau/" + request).with(user(owner))).andExpect(status().isOk());
        mvc.perform(get("/yeu-cau/" + request).with(user(fixture(false).getEmail()))).andExpect(status().isForbidden());
    }

    @Test
    void rechecksNewObligationAndConcurrentAdminsCanOnlyApproveOneVersion() throws Exception {
        Long id = customer.getMaNguoiDung(), adminId = admin.getMaNguoiDung();
        var draft = accounts.review(adminId, id, "0962481678", "Phần mềm", BIO, "Đã soát hồ sơ.");
        var request = catalog.request(id, null, "Yêu cầu mở mới", BIO, BigDecimal.TEN, null);
        var blocked = assertThrows(BusinessException.class, () -> accounts.convert(adminId, draft));
        assertTrue(blocked.getMessage().contains("yêu cầu công việc đang mở"));
        assertTrue(find(customer.getEmail()).getLaKhachHang());
        assertNull(accounts.detail(adminId, id).expert());
        catalog.closeRequest(id, request.getMaYeuCau());
        Long otherAdmin = fixture(true).getMaNguoiDung();
        try (var pool = Executors.newFixedThreadPool(2)) {
            var start = new CountDownLatch(1);
            var first = pool.submit(() -> attempt(start, adminId, draft));
            var second = pool.submit(() -> attempt(start, otherAdmin, draft));
            start.countDown();
            assertNotEquals(first.get(30, TimeUnit.SECONDS), second.get(30, TimeUnit.SECONDS));
        }
        assertEquals(1, expertApps.adminDetail(adminId, id).versions().size());
        assertEquals(1, expertApps.adminDetail(adminId, id).decisions().size());
    }

    private boolean attempt(CountDownLatch start, Long admin, AdminAccounts.Conversion draft) throws InterruptedException {
        start.await();
        try {
            accounts.convert(admin, draft);
            return true;
        } catch (BusinessException ex) {
            return false;
        }
    }
}
