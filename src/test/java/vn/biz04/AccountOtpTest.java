package vn.biz04;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.eclipse.angus.mail.smtp.SMTPAddressFailedException;
import jakarta.mail.internet.InternetAddress;
import vn.biz04.data.Store;
import vn.biz04.domain.*;
import vn.biz04.service.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.math.BigDecimal;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:accountotp;MODE=MSSQLServer;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
    "biz.otp.hmac-key=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=","biz.cccd.key=AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQE="})
@AutoConfigureMockMvc(print=org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint.NONE) @ActiveProfiles("test") @Import(AccountOtpTest.Config.class)
class AccountOtpTest {
    static class MutableClock extends Clock {
        Instant value=Instant.parse("2026-10-27T00:00:00Z");
        public ZoneId getZone(){return ZoneOffset.UTC;}public Clock withZone(ZoneId z){return this;}public Instant instant(){return value;}
        void seconds(int seconds){value=value.plusSeconds(seconds);}
    }
    @TestConfiguration static class Config {@Bean @Primary MutableClock otpClock(){return new MutableClock();}}
    @Autowired ProposalService proposalDecisions;
    @Autowired Store db;@Autowired TransactionTemplate tx;@Autowired AccountService accounts;@Autowired OtpService otp;@Autowired CatalogService catalog;
    @Autowired ContractService contracts;@Autowired PaymentService payments;@Autowired DeliveryService delivery;@Autowired MockMvc mvc;@Autowired MutableClock clock;
    @MockBean OtpMailGateway mail;
    Map<String,String> codes=new ConcurrentHashMap<>();String ip;OtpMailGateway.Delivery deliveryResult;
    @BeforeEach void setup(){ip=UUID.randomUUID().toString();deliveryResult=OtpMailGateway.Delivery.ACCEPTED;clock.seconds(3601);when(mail.send(anyString(),anyString())).thenAnswer(a->{codes.put(a.getArgument(0),a.getArgument(1));return deliveryResult;});}
    AccountForm form(){AccountForm f=new AccountForm();f.setHoTen("  Nguyễn Ánh  ");f.setEmail("u"+UUID.randomUUID()+"@example.com");f.setPassword("Demo@12345");f.setPasswordAgain("Demo@12345");f.setSoDienThoai("0912345678");return f;}
    Long id(String email){return tx.execute(s->db.first("from NguoiDung u where u.email=:e",NguoiDung.class,"e",email.toLowerCase(Locale.ROOT).strip()).orElseThrow().getMaNguoiDung());}
    NguoiDung recordUser(Long id){return tx.execute(s->db.get(NguoiDung.class,id));}
    MaXacThucOTP latest(Long id){return tx.execute(s->db.first("from MaXacThucOTP o where o.maNguoiDung=:u order by o.maOTP desc",MaXacThucOTP.class,"u",id).orElseThrow());}
    Long active(){AccountForm f=form();var result=otp.register(f,ip);assertTrue(otp.verify(result.token(),null,codes.get(f.getEmail()),ip).success());return id(f.getEmail());}
    void both(Long id){tx.executeWithoutResult(s->{var u=db.get(NguoiDung.class,id);u.setNguonDangKy(null);u.setLaChuyenGia(true);});}
    String cipher(Long id){return tx.execute(s->db.get(HoSoKhachHang.class,id).soCccdMaHoa);}

    @Test void registrationNormalizesAndRequiresVerification()throws Exception{
        AccountForm f=form();f.setEmail("  "+f.getEmail().toUpperCase(Locale.ROOT)+"  ");var result=otp.register(f,ip);Long id=id(f.getEmail());var u=recordUser(id);
        assertEquals("Nguyễn Ánh",u.getHoTen());assertEquals("CHO_XAC_THUC",u.getTrangThai());assertNotEquals(f.getPassword(),u.getMatKhauBam());assertNull(u.getThoiDiemXacThucEmail());assertEquals(ZoneOffset.UTC,u.getNgayTao().getOffset());
        assertThrows(BusinessException.class,()->catalog.request(id,null,"Yêu cầu kiểm tra","Nội dung kiểm tra đầy đủ",BigDecimal.TEN,null));
        mvc.perform(post("/dang-nhap").with(csrf()).param("email",u.getEmail()).param("password",f.getPassword())).andExpect(redirectedUrl("/dang-nhap?error"));
        assertTrue(otp.verify(result.token(),null,codes.get(u.getEmail()),ip).success());assertEquals("DA_XAC_THUC",recordUser(id).getTrangThaiEmail());assertNotNull(recordUser(id).getThoiDiemXacThucEmail());
        assertFalse(otp.verify(result.token(),null,codes.get(u.getEmail()),ip).success());
        mvc.perform(post("/dang-nhap").with(csrf()).param("email",u.getEmail()).param("password",f.getPassword())).andExpect(redirectedUrl("/bang-dieu-khien"));
    }
    @Test void organizationValidationAndDuplicateCannotOverwrite(){
        AccountForm f=form();f.setLoaiKhachHang("TO_CHUC");assertThrows(BusinessException.class,()->otp.register(f,ip));f.setTenToChuc("Công ty Ánh Dương");otp.register(f,ip);Long id=id(f.getEmail());
        assertEquals("Công ty Ánh Dương",tx.execute(s->db.get(HoSoKhachHang.class,id).tenToChuc));
        f.setHoTen("Tên giả mạo");f.setPassword("Other@12345");assertThrows(BusinessException.class,()->otp.register(f,ip));assertEquals("Nguyễn Ánh",recordUser(id).getHoTen());
    }
    @ParameterizedTest @ValueSource(strings={"0123456789","091234567","+84912345678","09123456a8","０９１２３４５６７８"})
    void invalidPhones(String phone){AccountForm f=form();f.setSoDienThoai(phone);assertThrows(BusinessException.class,()->otp.register(f,ip));}
    @Test void invalidEmailAndPasswordConfirmation(){AccountForm f=form();f.setEmail("x@wrong");assertThrows(BusinessException.class,()->otp.register(f,ip));AccountForm mismatch=form();mismatch.setPasswordAgain("different");assertThrows(BusinessException.class,()->otp.register(mismatch,ip));}
    @Test void fiveWrongAttemptsPersistAndResendDoesNotResetHourlyQuota(){
        AccountForm f=form();var r=otp.register(f,ip);Long id=id(f.getEmail());String wrong="000000".equals(codes.get(f.getEmail()))?"111111":"000000";
        for(int i=1;i<=5;i++){assertFalse(otp.verify(r.token(),null,wrong,ip).success());assertEquals(i,latest(id).soLanThuSai);}
        assertEquals("BI_HUY",latest(id).trangThai);assertFalse(otp.verify(r.token(),null,codes.get(f.getEmail()),ip).success());
        assertFalse(otp.resend(r.token(),null,null,ip).success());clock.seconds(61);var newer=otp.resend(r.token(),null,null,ip);assertTrue(newer.success());assertFalse(otp.verify(r.token(),null,codes.get(f.getEmail()),ip).success());
        for(int batch=0;batch<3;batch++){String bad="not-six";for(int i=0;i<5;i++)otp.verify(newer.token(),null,bad,ip);clock.seconds(61);newer=otp.resend(newer.token(),null,null,ip);}
        assertTrue(otp.verify(newer.token(),null,codes.get(f.getEmail()),ip).message().contains("quá nhiều"));
    }
    @Test void expiryAndControlledPendingEmailChange(){
        AccountForm f=form();var r=otp.register(f,ip);Long id=id(f.getEmail());String oldCode=codes.get(f.getEmail());clock.seconds(300);assertFalse(otp.verify(r.token(),null,oldCode,ip).success());assertEquals("HET_HAN",latest(id).trangThai);
        String next=form().getEmail();var newer=otp.resend(r.token(),null,next,ip);assertEquals(next,recordUser(id).getEmail());assertFalse(otp.verify(newer.token(),null,oldCode,ip).success());assertFalse(otp.verify(r.token(),null,codes.get(next),ip).success());assertTrue(otp.verify(newer.token(),null,codes.get(next),ip).success());
        assertThrows(BusinessException.class,()->otp.resend(new AccountCrypto("","").token(),null,next,ip));
    }
    @Test void mailFailureNeverActivatesAndRetryWorks(){AccountForm f=form();deliveryResult=OtpMailGateway.Delivery.FAILED;var r=otp.register(f,ip);Long id=id(f.getEmail());assertFalse(r.success());assertEquals(OtpService.FAILED,r.message());assertEquals("BI_HUY",latest(id).trangThai);assertEquals("CHO_XAC_THUC",recordUser(id).getTrangThai());clock.seconds(61);deliveryResult=OtpMailGateway.Delivery.ACCEPTED;assertTrue(otp.resend(r.token(),null,null,ip).success());}
    @Test void uncertainDeliveryInvalidatesCode(){AccountForm f=form();when(mail.send(anyString(),anyString())).thenThrow(new IllegalStateException("timeout"));var r=otp.register(f,ip);assertFalse(r.success());assertEquals("BI_HUY",latest(id(f.getEmail())).trangThai);}
    @Test void recipientMissingOnlyForExplicitStructuredSmtpReply()throws Exception{
        var address=new InternetAddress("nobody@example.com");
        assertEquals(OtpMailGateway.Delivery.RECIPIENT_MISSING,SmtpOtpMailGateway.classify(new SMTPAddressFailedException(address,"RCPT TO:<nobody@example.com>",550,"550 5.1.1 No such user")));
        for(String response:List.of("550 5.7.1 policy rejected","550 mailbox unavailable","arbitrary 550 5.1.1 text"))assertEquals(OtpMailGateway.Delivery.FAILED,SmtpOtpMailGateway.classify(new SMTPAddressFailedException(address,"RCPT TO",550,response)));
        assertEquals(OtpMailGateway.Delivery.FAILED,SmtpOtpMailGateway.classify(new IllegalStateException("550 5.1.1")));
    }
    @Test void emailChangeKeepsOldUntilSuccessAndChecksOwnership(){
        Long id=active(),other=active();String old=recordUser(id).getEmail(),next=form().getEmail();
        assertThrows(BusinessException.class,()->otp.startEmailChange(id,next,"wrong",ip));
        var r=otp.startEmailChange(id,next,"Demo@12345",ip);assertEquals(old,recordUser(id).getEmail());
        assertThrows(BusinessException.class,()->otp.verify(r.token(),other,codes.get(next),ip));assertThrows(BusinessException.class,()->otp.verify(r.token(),null,codes.get(next),ip));
        assertTrue(otp.verify(r.token(),id,codes.get(next),ip).success());assertEquals(next,recordUser(id).getEmail());
    }
    @Test void emailUniquenessRecheckedOnConfirm(){Long id=active();String old=recordUser(id).getEmail(),next=form().getEmail();var change=otp.startEmailChange(id,next,"Demo@12345",ip);String changeCode=codes.get(next);AccountForm f=form();f.setEmail(next);otp.register(f,UUID.randomUUID().toString());assertFalse(otp.verify(change.token(),id,changeCode,ip).success());assertEquals(old,recordUser(id).getEmail());}
    @Test void onlyOneConcurrentVerificationWins()throws Exception{
        AccountForm f=form();var r=otp.register(f,ip);ExecutorService pool=Executors.newFixedThreadPool(2);CountDownLatch start=new CountDownLatch(1);
        try{Callable<Boolean> verify=()->{start.await();return otp.verify(r.token(),null,codes.get(f.getEmail()),ip).success();};Future<Boolean>a=pool.submit(verify),b=pool.submit(verify);start.countDown();assertNotEquals(a.get(20,TimeUnit.SECONDS),b.get(20,TimeUnit.SECONDS));}finally{pool.shutdownNow();}
    }
    @Test void cccdEncryptedMaskedAndUnchangedWhenOmitted()throws Exception{
        Long id=active();AccountForm f=accounts.form(id);f.setSoCccdMoi("012345678901");accounts.profile(id,f);String before=cipher(id);assertNotNull(before);assertFalse(before.contains("012345678901"));assertEquals("********8901",accounts.maskedCccd(id));
        OffsetDateTime created=recordUser(id).getNgayTao();clock.seconds(10);f=accounts.form(id);f.setDiaChi("Địa chỉ mới");accounts.profile(id,f);assertEquals(before,cipher(id));assertEquals(created,recordUser(id).getNgayTao());assertTrue(recordUser(id).getNgayCapNhat().isAfter(created));
        f.setSoCccdMoi(null);accounts.profile(id,f);assertEquals(before,cipher(id));f.setSoCccdMoi("   ");accounts.profile(id,f);assertEquals(before,cipher(id));
        var html=mvc.perform(get("/ho-so").with(user(recordUser(id).getEmail()))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();assertFalse(html.contains("012345678901"));assertFalse(html.contains(before));assertTrue(html.contains("********8901"));assertFalse(html.contains("value=\"********8901\""));
        f.setSoCccdMoi("123456789012");accounts.profile(id,f);assertNotEquals(before,cipher(id));assertEquals("********9012",accounts.maskedCccd(id));
    }
    @ParameterizedTest @ValueSource(strings={"********1234","12345678901","1234567890123","123456789012345","12345678901a","12345 789012","１２３４５６７８９０１２","12345678901-"})
    void invalidCccdNeverPartiallySaves(String value){Long id=active();AccountForm f=accounts.form(id);f.setSoCccdMoi("012345678901");accounts.profile(id,f);String before=cipher(id);f.setDiaChi("Không được lưu");f.setSoCccdMoi(value);assertThrows(BusinessException.class,()->accounts.profile(id,f));assertEquals(before,cipher(id));assertNull(accounts.form(id).getDiaChi());}
    @Test void missingEncryptionKeyDoesNotFallBackToPlaintext(){assertThrows(BusinessException.class,()->new AccountCrypto("","").encryptCccd(1L,"012345678901"));}
    @Test void pendingAccountCanResumeOnlyWithOriginalPassword(){
        AccountForm f=form();var first=otp.register(f,ip);clock.seconds(61);
        assertFalse(otp.resumeRegistration(f.getEmail(),"wrong",ip).success());
        var resumed=otp.resumeRegistration(f.getEmail(),f.getPassword(),ip);assertTrue(resumed.success());assertFalse(otp.verify(first.token(),null,codes.get(f.getEmail()),ip).success());assertTrue(otp.verify(resumed.token(),null,codes.get(f.getEmail()),ip).success());
        clock.seconds(61);assertFalse(otp.resumeRegistration(f.getEmail(),f.getPassword(),ip).success());
    }
    @Test void closedSettledContractsAllowRemovalButOpenContractsBlock(){
        Long customer=active(),expert=active();both(customer);both(expert);
        var y=catalog.request(customer,null,"Yêu cầu kiểm thử hợp đồng","Nội dung công việc cần chuyên gia",BigDecimal.TEN,null);
        var d=catalog.proposal(expert,y.getMaYeuCau(),"Phương án đủ dài để kiểm thử",BigDecimal.TEN,2);
        proposalDecisions.accept(customer,d.getMaDeXuat());var h=tx.execute(s->TestContracts.legacy(db,d.getMaDeXuat()));
        var mid=contracts.milestone(customer,h.getMaHopDong(),null,1,"Mốc kiểm thử","Nội dung công việc","Tiêu chí nghiệm thu rõ ràng",BigDecimal.TEN,LocalDate.now().plusDays(30));
        AccountForm cf=accounts.form(customer),ef=accounts.form(expert);cf.setKhach(false);ef.setChuyenGia(false);
        assertThrows(BusinessException.class,()->accounts.profile(customer,cf));assertThrows(BusinessException.class,()->accounts.profile(expert,ef));
        int version=tx.execute(s->db.get(HopDong.class,h.getMaHopDong()).getPhienBan());contracts.confirm(customer,h.getMaHopDong(),version);contracts.confirm(expert,h.getMaHopDong(),version);
        payments.fund(customer,mid.getMaMocNghiemThu(),"SUCCESS");delivery.deliver(expert,mid.getMaMocNghiemThu(),"Sản phẩm hoàn thành đầy đủ","https://example.com/product");
        delivery.accept(customer,mid.getMaMocNghiemThu(),"FAIL");assertThrows(BusinessException.class,()->accounts.profile(customer,cf));assertThrows(BusinessException.class,()->accounts.profile(expert,ef));
        payments.release(customer,mid.getMaMocNghiemThu(),"SUCCESS");accounts.profile(customer,cf);accounts.profile(expert,ef);
        assertFalse(recordUser(customer).getLaKhachHang());assertFalse(recordUser(expert).getLaChuyenGia());
    }
    @Test void cccdFormErrorDoesNotEchoSubmittedSecret()throws Exception{
        Long id=active();mvc.perform(post("/ho-so").with(user(recordUser(id).getEmail())).with(csrf()).param("hoTen"," ").param("khach","true").param("loaiKhachHang","CA_NHAN").param("soCccdMoi","012345678901")).andExpect(status().isOk()).andExpect(content().string(not(containsString("012345678901"))));
    }
    @Test void captureAccountTemplatesForVisualQa()throws Exception{
        Long id=active();AccountForm f=accounts.form(id);f.setHoTen("Nguyễn Thị Tên Rất Dài Để Kiểm Tra Thanh Điều Hướng Và Nút Đăng Xuất");f.setSoCccdMoi("012345678901");f.setGioiThieu("Tôi cần thuê chuyên gia để phát triển nội dung và thương hiệu.");accounts.profile(id,f);
        Path folder=Path.of(".run","account-qa","rendered");Files.createDirectories(folder);
        var profile=mvc.perform(get("/ho-so").with(user(recordUser(id).getEmail()))).andExpect(status().isOk()).andReturn();Files.write(folder.resolve("profile.html"),profile.getResponse().getContentAsByteArray());
        AccountForm pending=form();var r=otp.register(pending,ip);MockHttpSession session=new MockHttpSession();session.setAttribute("SANG_REGISTER_CHALLENGE",r.token());
        var html=mvc.perform(get("/xac-thuc-email").session(session).flashAttr("error","Mã đã bị hủy sau 5 lần nhập sai. Vui lòng gửi lại mã và kiểm tra hộp thư cùng thư rác trước khi nhập.")).andExpect(status().isOk()).andReturn();Files.write(folder.resolve("otp.html"),html.getResponse().getContentAsByteArray());
    }
    @Test void rolesValidateOnlyOnRemovalAndKeepHistory()throws Exception{
        Long id=active();both(id);var y=catalog.request(id,null,"Yêu cầu kiểm thử","Nội dung công việc cần chuyên gia",BigDecimal.TEN,null);
        AccountForm f=accounts.form(id);f.setDiaChi("Vẫn lưu khi có nghĩa vụ");accounts.profile(id,f);f.setKhach(false);assertThrows(BusinessException.class,()->accounts.profile(id,f));
        catalog.closeRequest(id,y.getMaYeuCau());accounts.profile(id,f);assertFalse(recordUser(id).getLaKhachHang());
        assertThrows(BusinessException.class,()->catalog.request(id,null,"Yêu cầu mới","Nội dung mới đủ độ dài",BigDecimal.TEN,null));
        mvc.perform(get("/yeu-cau/"+y.getMaYeuCau()).with(user(recordUser(id).getEmail()))).andExpect(status().isOk());f.setChuyenGia(false);assertThrows(BusinessException.class,()->accounts.profile(id,f));
    }
    @Test void expertCanDropAfterWithdrawnProposal(){Long customer=active(),expert=active();both(expert);var y=catalog.request(customer,null,"Yêu cầu kiểm thử","Nội dung công việc cần chuyên gia",BigDecimal.TEN,null);var d=catalog.proposal(expert,y.getMaYeuCau(),"Phương án đủ dài để kiểm thử",BigDecimal.TEN,2);AccountForm f=accounts.form(expert);f.setChuyenGia(false);assertThrows(BusinessException.class,()->accounts.profile(expert,f));catalog.withdraw(expert,d.getMaDeXuat());accounts.profile(expert,f);assertFalse(recordUser(expert).getLaChuyenGia());}
    @Test void adminOnlyAccountCanKeepNoCustomerOrExpert(){Long id=active();tx.executeWithoutResult(s->{var u=db.get(NguoiDung.class,id);u.setNguonDangKy(null);u.setLaQuanTriVien(true);u.setLaKhachHang(false);});AccountForm f=accounts.form(id);accounts.profile(id,f);assertTrue(recordUser(id).getLaQuanTriVien());}
    @Test void roleRemovalAndNewObligationSerialize()throws Exception{
        Long id=active();both(id);AccountForm f=accounts.form(id);f.setKhach(false);ExecutorService pool=Executors.newFixedThreadPool(2);CountDownLatch start=new CountDownLatch(1);
        try{Future<Boolean>a=pool.submit(()->{start.await();try{accounts.profile(id,f);return true;}catch(BusinessException ex){return false;}});Future<Boolean>b=pool.submit(()->{start.await();try{catalog.request(id,null,"Yêu cầu đồng thời","Nội dung kiểm tra khóa vai trò",BigDecimal.TEN,null);return true;}catch(BusinessException ex){return false;}});start.countDown();assertNotEquals(a.get(20,TimeUnit.SECONDS),b.get(20,TimeUnit.SECONDS));}finally{pool.shutdownNow();}
    }
    @Test void formErrorsPreserveSafeFieldsAndRejectAdminSpoofing()throws Exception{
        var result=mvc.perform(post("/dang-ky").with(csrf()).param("hoTen","Tên được giữ").param("email","bad").param("password","DoNotEcho123!").param("passwordAgain","DoNotEcho123!").param("soDienThoai","0912345678")).andExpect(status().isOk()).andExpect(model().attribute("form",hasProperty("hoTen",is("Tên được giữ")))).andReturn();assertFalse(result.getResponse().getContentAsString().contains("DoNotEcho123!"));
        Long id=active();String email=recordUser(id).getEmail();mvc.perform(post("/ho-so").with(user(email)).with(csrf()).param("hoTen","Bị giả mạo").param("khach","true").param("loaiKhachHang","CA_NHAN").param("laQuanTriVien","true")).andExpect(status().isOk()).andExpect(content().string(containsString("không được phép")));assertFalse(recordUser(id).getLaQuanTriVien());assertEquals("Nguyễn Ánh",recordUser(id).getHoTen());
    }
    @Test void updatedNameAndRolesAppearImmediatelyInSession()throws Exception{
        Long id=active();both(id);String email=recordUser(id).getEmail();MockHttpSession session=new MockHttpSession();
        mvc.perform(post("/ho-so").session(session).with(user(email)).with(csrf()).param("hoTen","Tên mới").param("soDienThoai","0912345678").param("loaiKhachHang","CA_NHAN")).andExpect(redirectedUrl("/ho-so"));
        SecurityContext context=(SecurityContext)session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);assertTrue(context.getAuthentication().getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_CHUYEN_GIA")));
        mvc.perform(get("/ho-so").session(session)).andExpect(content().string(containsString("Xin chào: Tên mới")));
    }
    @Test void otpEndpointsNeedSessionAndCsrf()throws Exception{mvc.perform(post("/xac-thuc-email").param("code","000000")).andExpect(status().isForbidden());mvc.perform(get("/xac-thuc-email").param("MaNguoiDung","1")).andExpect(redirectedUrl("/dang-ky"));}
    @Test void missingRoleFieldsKeepsCurrentRoles()throws Exception{Long id=active();mvc.perform(post("/ho-so").with(user(recordUser(id).getEmail())).with(csrf()).param("hoTen","Tên mới").param("soDienThoai","0912345678").param("loaiKhachHang","CA_NHAN")).andExpect(redirectedUrl("/ho-so"));assertTrue(recordUser(id).getLaKhachHang());assertFalse(recordUser(id).getLaChuyenGia());}
    @Test void profileEscapesHtmlAndRendersVietnamTime()throws Exception{Long id=active();AccountForm f=accounts.form(id);f.setGioiThieu("<script>alert('x')</script>");accounts.profile(id,f);String html=mvc.perform(get("/ho-so").with(user(recordUser(id).getEmail()))).andReturn().getResponse().getContentAsString();assertFalse(html.contains("<script>alert"));assertTrue(html.contains("&lt;script&gt;"));assertTrue(html.contains(new vn.biz04.web.Format().offsetDate(recordUser(id).getNgayTao())));}
}

