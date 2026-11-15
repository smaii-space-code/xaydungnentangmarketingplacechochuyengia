package vn.biz04;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.transaction.support.TransactionTemplate;
import vn.biz04.data.Store;
import vn.biz04.domain.*;
import vn.biz04.service.*;
import vn.biz04.config.DemoData;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:takeover;MODE=MSSQLServer;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false","biz.otp.hmac-key=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=","biz.cccd.key=AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQE="})
@AutoConfigureMockMvc @ActiveProfiles("test") @Import(AccountOtpTest.Config.class)
class AccountTakeoverTest {
    @Autowired Store db;@Autowired TransactionTemplate tx;@Autowired AccountTakeoverService takeover;@Autowired OtpService otp;
    @Autowired CatalogService catalog;@Autowired ServiceApproval approvals;@Autowired CatalogDiscovery discovery;@Autowired AccountService accounts;
    @Autowired ProposalService proposals;@Autowired ContractService contracts;@Autowired MockMvc mvc;@Autowired PasswordEncoder encoder;@Autowired UserDetailsService users;
    @Autowired AccountOtpTest.MutableClock clock;
    @MockBean OtpMailGateway mail;
    Map<String,String> codes=new ConcurrentHashMap<>();Long expert,customer,admin,source;String email,oldEmail,tag;
    static final String OLD="FixtureOldOnly!",NEW="FixtureNewOnly!";
    @BeforeEach void setup(){
        var latest=tx.execute(s->db.list("select max(b.batDau) from OtpGioiHan b",OffsetDateTime.class).get(0));if(latest!=null&&clock.instant().isBefore(latest.toInstant().plusSeconds(3601)))clock.value=latest.toInstant().plusSeconds(3601);else clock.seconds(3601);tag=UUID.randomUUID().toString();email="takeover"+tag+"@example.test";oldEmail="old"+tag+"@example.test";
        when(mail.send(anyString(),anyString())).thenAnswer(a->{codes.put(a.getArgument(0),a.getArgument(1));return OtpMailGateway.Delivery.ACCEPTED;});
        tx.executeWithoutResult(s->{expert=TestAccounts.create(db,"Nguồn kiểm thử "+tag,oldEmail,OLD,false,true).getMaNguoiDung();customer=TestAccounts.create(db,"Khách kiểm thử", "customer"+tag+"@example.test",OLD,true,false).getMaNguoiDung();var a=TestAccounts.create(db,"Admin kiểm thử","admin"+tag+"@example.test",OLD,false,false);a.setLaQuanTriVien(true);admin=a.getMaNguoiDung();});
        source=catalog.service(expert,null,"Dịch vụ cũ "+tag,"Lĩnh vực "+tag,"Nội dung cũ cần chủ mới kiểm tra lại.",BigDecimal.TEN).getMaDichVu();
        tx.executeWithoutResult(s->{var u=db.get(NguoiDung.class,expert);u.setLoaiTaiKhoan("MAU");u.setTrangThai("CHO_TIEP_QUAN");u.setTrangThaiEmail("CHUYEN_TIEP");u.setThoiDiemXacThucEmail(null);u.setPhienBaoMat(1);var p=new TiepQuanTaiKhoan();p.user=expert;p.originalName=u.getHoTen();p.originalEmail=u.getEmail();p.origin="ISOLATED_TEST";p.started=OffsetDateTime.now(clock);db.save(p);var d=db.get(DichVu.class,source);d.setCanRaSoatLai(true);d.setNguonNoiDung("MAU");});
    }
    NguoiDung account(){return tx.execute(s->db.get(NguoiDung.class,expert));}
    DichVu service(){return tx.execute(s->db.get(DichVu.class,source));}
    OtpService.Result start(){return takeover.start(expert,"Tên chính thức "+tag,email);}
    void complete(){var r=start();assertTrue(r.success());assertTrue(takeover.complete(expert,r.token(),codes.get(email),NEW,NEW).success());}
    void revise(){var d=service();catalog.service(expert,source,d.getTenDichVu(),d.getLinhVuc(),"Nội dung thật đã được chủ mới rà soát "+tag,BigDecimal.TEN);}
    void publish(){revise();approvals.submit(expert,source,true);approvals.review(admin,source,service().getPhienBanNoiDung(),true,"");}
    CatalogDiscovery.Filter filter(){return CatalogDiscovery.Filter.parse(tag,"","1","1");}
    String address(Long id){return tx.execute(s->db.get(NguoiDung.class,id).getEmail());}
    @Test void passwordLengthMatchesRegistrationPolicyWithoutBypassingOtp(){
        var r=start();String eight=UUID.randomUUID().toString().substring(0,8);assertThrows(BusinessException.class,()->takeover.complete(expert,r.token(),codes.get(email),eight.substring(0,7),eight.substring(0,7)));assertTrue(takeover.complete(expert,r.token(),codes.get(email),eight,eight).success());assertTrue(encoder.matches(eight,account().getMatKhauBam()));
    }
    @Test void pendingBlocksOldPasswordAndSessionWithoutChangingOriginalIdentity()throws Exception{
        var before=account();assertFalse(users.loadUserByUsername(oldEmail).isEnabled());mvc.perform(post("/dang-nhap").with(csrf()).param("email",oldEmail).param("password",OLD)).andExpect(redirectedUrl("/dang-nhap?error"));
        mvc.perform(get("/ho-so").with(user(oldEmail))).andExpect(redirectedUrl("/dang-nhap?session=expired"));
        assertEquals(oldEmail,account().getEmail());assertEquals(before.getMaNguoiDung(),account().getMaNguoiDung());assertThrows(BusinessException.class,()->accounts.block(admin,expert,false));
    }
    @Test void actualRecipientRequiredAndStartDoesNotMakeAccountOfficial(){
        assertThrows(BusinessException.class,()->takeover.start(expert,"Tên mới","[Điền email thứ nhất]"));assertThrows(BusinessException.class,()->takeover.start(expert,"Tên mới",address(customer)));
        var r=start();assertTrue(r.success());assertNotNull(codes.get(email));verify(mail).send(eq(email),anyString());assertEquals(oldEmail,account().getEmail());assertEquals("MAU",account().getLoaiTaiKhoan());assertNull(account().getThoiDiemXacThucEmail());assertEquals(0,discovery.listing(filter()).experts().total());
        assertThrows(BusinessException.class,()->otp.verify(r.token(),expert,codes.get(email),"wrong-purpose"));
    }
    @Test void failureAndExpiryNeverActivateAndRetryInvalidatesPreviousCode(){
        when(mail.send(anyString(),anyString())).thenReturn(OtpMailGateway.Delivery.FAILED);assertFalse(start().success());assertEquals("CHO_TIEP_QUAN",account().getTrangThai());clock.seconds(61);
        when(mail.send(anyString(),anyString())).thenAnswer(a->{codes.put(a.getArgument(0),a.getArgument(1));return OtpMailGateway.Delivery.ACCEPTED;});var r=start();String code=codes.get(email);clock.seconds(300);assertFalse(takeover.complete(expert,r.token(),code,NEW,NEW).success());var next=start();assertFalse(takeover.complete(expert,r.token(),code,NEW,NEW).success());assertTrue(takeover.complete(expert,next.token(),codes.get(email),NEW,NEW).success());
    }
    @Test void wrongCodeFiveTimesPersistsAndCannotActivate(){
        var r=start();for(int n=0;n<5;n++)assertFalse(takeover.complete(expert,r.token(),"invalid",NEW,NEW).success());assertFalse(takeover.complete(expert,r.token(),codes.get(email),NEW,NEW).success());assertEquals("MAU",account().getLoaiTaiKhoan());
        var o=tx.execute(s->db.first("from MaXacThucOTP o where o.maNguoiDung=:u order by o.maOTP desc",MaXacThucOTP.class,"u",expert).orElseThrow());assertEquals(5,o.soLanThuSai);assertEquals("BI_HUY",o.trangThai);
    }
    @Test void completionIsAtomicRetainsProvenanceAndRotatesPasswordAndSessions()throws Exception{
        var r=start();assertThrows(BusinessException.class,()->takeover.complete(expert,r.token(),codes.get(email),OLD,OLD));assertThrows(BusinessException.class,()->takeover.complete(expert,r.token(),codes.get(email),NEW,"mismatch"));
        assertTrue(takeover.complete(expert,r.token(),codes.get(email),NEW,NEW).success());var u=account();assertEquals(email,u.getEmail());assertEquals("Tên chính thức "+tag,u.getHoTen());assertEquals("CHINH_THUC",u.getLoaiTaiKhoan());assertEquals("HOAT_DONG",u.getTrangThai());assertEquals("DA_XAC_THUC",u.getTrangThaiEmail());assertNotNull(u.getThoiDiemXacThucEmail());assertEquals(2,u.getPhienBaoMat());assertTrue(encoder.matches(NEW,u.getMatKhauBam()));assertFalse(encoder.matches(OLD,u.getMatKhauBam()));assertEquals(expert,service().getMaChuyenGia());assertTrue(service().isCanRaSoatLai());
        var p=tx.execute(s->db.get(TiepQuanTaiKhoan.class,expert));assertEquals(oldEmail,p.originalEmail);assertEquals("ISOLATED_TEST",p.origin);assertNotNull(p.completed);assertFalse(takeover.complete(expert,r.token(),codes.get(email),NEW,NEW).success());
        mvc.perform(get("/ho-so").with(user(email))).andExpect(redirectedUrl("/dang-nhap?session=expired"));u.setPhienBaoMat(1);mvc.perform(get("/ho-so").with(user(new vn.biz04.security.AccountPrincipal(u,List.of("CHUYEN_GIA"))))).andExpect(redirectedUrl("/dang-nhap?session=expired"));mvc.perform(post("/dang-nhap").with(csrf()).param("email",email).param("password",OLD)).andExpect(redirectedUrl("/dang-nhap?error"));
        var logged=mvc.perform(post("/dang-nhap").with(csrf()).param("email",email).param("password",NEW)).andExpect(redirectedUrl("/bang-dieu-khien")).andReturn();mvc.perform(get("/ho-so").session((org.springframework.mock.web.MockHttpSession)logged.getRequest().getSession(false))).andExpect(status().isOk());
        tx.executeWithoutResult(s->new DemoData(db,accounts,catalog,"").run());assertEquals(email,account().getEmail());
    }
    @Test void legacyDualRoleBecomesExpertOnlyAndClosedCustomerHistoryStaysReadable()throws Exception{
        Long request=tx.execute(s->{var u=db.get(NguoiDung.class,expert);u.setTrangThai("HOAT_DONG");u.setLaKhachHang(true);var y=catalog.request(expert,null,"Lịch sử khách hàng "+tag,"Nội dung công việc đã kết thúc cần giữ lại",BigDecimal.TEN,null);catalog.closeRequest(expert,y.getMaYeuCau());u.setTrangThai("CHO_TIEP_QUAN");return y.getMaYeuCau();});
        complete();assertFalse(account().getLaKhachHang());assertTrue(account().getLaChuyenGia());assertFalse(account().getLaQuanTriVien());assertEquals(expert,tx.execute(s->db.get(YeuCau.class,request).getMaKhachHang()));
        mvc.perform(get("/yeu-cau/"+request).with(user(users.loadUserByUsername(email)))).andExpect(status().isOk());
    }
    @Test void openCustomerObligationRollsBackCompletionIncludingOtpAndRoles(){
        Long request=tx.execute(s->{var u=db.get(NguoiDung.class,expert);u.setTrangThai("HOAT_DONG");u.setLaKhachHang(true);var y=catalog.request(expert,null,"Công việc đang mở "+tag,"Nghĩa vụ khách hàng cần được xử lý trước",BigDecimal.TEN,null);u.setTrangThai("CHO_TIEP_QUAN");return y.getMaYeuCau();});
        var r=start();assertThrows(BusinessException.class,()->takeover.complete(expert,r.token(),codes.get(email),NEW,NEW));
        assertEquals("MAU",account().getLoaiTaiKhoan());assertEquals("CHO_TIEP_QUAN",account().getTrangThai());assertEquals("CHUYEN_TIEP",account().getTrangThaiEmail());assertEquals(oldEmail,account().getEmail());assertTrue(account().getLaKhachHang());assertTrue(account().getLaChuyenGia());assertNull(tx.execute(s->db.get(TiepQuanTaiKhoan.class,expert).completed));
        assertEquals("CHO_XAC_THUC",tx.execute(s->db.first("from MaXacThucOTP o where o.maNguoiDung=:u order by o.maOTP desc",MaXacThucOTP.class,"u",expert).orElseThrow().trangThai));
        tx.executeWithoutResult(s->{var u=db.get(NguoiDung.class,expert);u.setTrangThai("HOAT_DONG");catalog.closeRequest(expert,request);u.setTrangThai("CHO_TIEP_QUAN");});
        assertTrue(takeover.complete(expert,r.token(),codes.get(email),NEW,NEW).success());assertFalse(account().getLaKhachHang());
    }
    @Test void mailboxClaimedBetweenSendAndVerifyDoesNotOverwriteOtherAccount(){
        var r=start();tx.executeWithoutResult(s->TestAccounts.create(db,"Khác",email,OLD,true,false));assertFalse(takeover.complete(expert,r.token(),codes.get(email),NEW,NEW).success());assertEquals(oldEmail,account().getEmail());assertEquals("MAU",account().getLoaiTaiKhoan());
    }
    @Test void concurrentCompletionConsumesOtpOnce()throws Exception{
        var r=start();String code=codes.get(email);ExecutorService pool=Executors.newFixedThreadPool(2);CountDownLatch gate=new CountDownLatch(1);try{Callable<Boolean> call=()->{gate.await();return takeover.complete(expert,r.token(),code,NEW,NEW).success();};var a=pool.submit(call);var b=pool.submit(call);gate.countDown();assertNotEquals(a.get(30,TimeUnit.SECONDS),b.get(30,TimeUnit.SECONDS));}finally{pool.shutdownNow();}assertEquals(2,account().getPhienBaoMat());
    }
    @Test void oldArticlesNeedActualEditOwnerAcknowledgementAndFreshSeparateAdminApproval(){
        complete();assertThrows(BusinessException.class,()->approvals.submit(expert,source,true));var d=service();assertThrows(BusinessException.class,()->catalog.service(expert,source,d.getTenDichVu()+" ",d.getLinhVuc(),d.getMoTa(),d.getGiaThamKhao()));
        revise();assertThrows(BusinessException.class,()->approvals.submit(expert,source,false));assertThrows(BusinessException.class,()->approvals.submit(customer,source,true));approvals.submit(expert,source,true);assertEquals(0,discovery.listing(filter()).services().total());assertThrows(BusinessException.class,()->approvals.review(customer,source,service().getPhienBanNoiDung(),true,""));
        assertThrows(BusinessException.class,()->approvals.review(admin,source,1,true,""));approvals.review(admin,source,service().getPhienBanNoiDung(),true,"");assertEquals(1,discovery.listing(filter()).experts().total());assertEquals(1,discovery.listing(filter()).services().total());
        assertEquals(expert,discovery.detail(source).expert().id());assertEquals(List.of(source),discovery.profile(expert,1,1).services().items().stream().map(CatalogDiscovery.ServiceItem::id).toList());assertTrue(discovery.fields().contains(d.getLinhVuc()));
    }
    @Test void newServicesStartDraftAndEditingResetsApprovedVersion(){
        complete();var d=catalog.service(expert,null,"Dịch vụ mới "+tag,"Mới "+tag,"Nội dung dịch vụ thật cần xét duyệt",BigDecimal.TEN);assertEquals("NHAP",d.getTrangThai());assertEquals("CHUA_GUI",d.getTrangThaiDuyet());assertThrows(BusinessException.class,()->catalog.visibility(expert,d.getMaDichVu(),true));publish();
        catalog.service(expert,source,"Đã chỉnh sửa "+tag,service().getLinhVuc(),"Nội dung thay đổi sau khi đã được duyệt",BigDecimal.TEN);assertEquals(0,discovery.listing(filter()).experts().total());assertNull(service().getPhienBanDuyet());assertThrows(BusinessException.class,()->catalog.visibility(admin,source,true));
    }
    @ParameterizedTest @ValueSource(strings={"MAU","EMAIL","EMAIL_TIME","KHOA","ROLE","HIDDEN","PENDING","REJECTED","VERSION"})
    void publicationPolicyIsIdenticalInAllProjections(String condition){
        complete();publish();assertEquals(1,discovery.home(filter()).experts().total());tx.executeWithoutResult(s->{var u=db.get(NguoiDung.class,expert);var d=db.get(DichVu.class,source);switch(condition){case "MAU"->u.setLoaiTaiKhoan("MAU");case "EMAIL"->u.setTrangThaiEmail("CHUYEN_TIEP");case "EMAIL_TIME"->u.setThoiDiemXacThucEmail(null);case "KHOA"->u.setTrangThai("KHOA");case "ROLE"->u.setLaChuyenGia(false);case "HIDDEN"->d.setTrangThai("DA_AN");case "PENDING"->d.setTrangThaiDuyet("CHO_DUYET");case "REJECTED"->d.setTrangThaiDuyet("TU_CHOI");case "VERSION"->d.setPhienBanNoiDung(d.getPhienBanNoiDung()+1);}});
        assertEquals(0,discovery.home(filter()).experts().total());assertEquals(0,discovery.listing(filter()).experts().total());assertEquals(0,discovery.listing(filter()).services().total());assertFalse(discovery.fields().contains(service().getLinhVuc()));assertThrows(org.springframework.web.server.ResponseStatusException.class,()->discovery.profile(expert,1,1));assertThrows(org.springframework.web.server.ResponseStatusException.class,()->discovery.detail(source));
    }
    @Test void rejectionRequiresReasonAndSelfApprovalCannotPublish(){
        complete();revise();approvals.submit(expert,source,true);int v=service().getPhienBanNoiDung();assertThrows(BusinessException.class,()->approvals.review(admin,source,v,false,""));approvals.review(admin,source,v,false,"Nội dung cần mô tả cụ thể hơn");assertEquals("TU_CHOI",service().getTrangThaiDuyet());assertEquals(0,discovery.listing(filter()).services().total());
        approvals.submit(expert,source,true);tx.executeWithoutResult(s->db.get(NguoiDung.class,expert).setLaQuanTriVien(true));assertThrows(BusinessException.class,()->approvals.review(expert,source,v,true,""));
    }
    @Test void existingBusinessOwnershipAndHistoryRemainReadableAfterTakeover()throws Exception{
        tx.executeWithoutResult(s->db.get(NguoiDung.class,expert).setTrangThai("HOAT_DONG"));var y=catalog.request(customer,null,"Yêu cầu lịch sử "+tag,"Lịch sử giao dịch phải giữ nguyên sau khi tiếp quản",BigDecimal.TEN,null);var p=catalog.proposal(expert,y.getMaYeuCau(),"Phương án cũ có quan hệ phải được giữ lại",BigDecimal.TEN,2);proposals.accept(customer,p.getMaDeXuat());var h=tx.execute(s->TestContracts.legacy(db,p.getMaDeXuat()));tx.executeWithoutResult(s->db.get(NguoiDung.class,expert).setTrangThai("CHO_TIEP_QUAN"));
        mvc.perform(get("/hop-dong/"+h.getMaHopDong()).with(user(address(customer)))).andExpect(status().isOk());complete();assertEquals(expert,tx.execute(s->db.get(DeXuat.class,p.getMaDeXuat()).getMaChuyenGia()));assertEquals(p.getMaDeXuat(),tx.execute(s->db.get(HopDong.class,h.getMaHopDong()).getMaDeXuat()));mvc.perform(get("/hop-dong/"+h.getMaHopDong()).with(user(address(customer)))).andExpect(status().isOk());
    }
    @Test void approvalEndpointsRequireAdminAndCsrfAndRenderReviewAndWorkspace()throws Exception{
        complete();revise();var principal=users.loadUserByUsername(email);Path draftFolder=Path.of(".run","takeover-qa","rendered");Files.createDirectories(draftFolder);var draft=mvc.perform(get("/bang-dieu-khien").with(user(principal))).andExpect(status().isOk()).andReturn().getResponse();Files.write(draftFolder.resolve("workspace-draft.html"),draft.getContentAsByteArray());approvals.submit(expert,source,true);
        mvc.perform(post("/dich-vu/"+source+"/gui-duyet").with(user(principal)).param("xacNhan","true")).andExpect(status().isForbidden());
        mvc.perform(get("/quan-tri/dich-vu/"+source+"/duyet").with(user(address(customer)))).andExpect(status().is3xxRedirection());
        Path folder=Path.of(".run","takeover-qa","rendered");Files.createDirectories(folder);for(var page:Map.of("review","/quan-tri/dich-vu/"+source+"/duyet","workspace","/bang-dieu-khien").entrySet()){var request=get(page.getValue()).with(user(page.getKey().equals("review")?users.loadUserByUsername(address(admin)):principal));var response=mvc.perform(request).andExpect(status().isOk()).andReturn().getResponse();Files.write(folder.resolve(page.getKey()+".html"),response.getContentAsByteArray());}
        var adminPage=mvc.perform(get("/quan-tri").with(user(users.loadUserByUsername(address(admin))))).andExpect(status().isOk()).andReturn().getResponse();Files.write(folder.resolve("admin.html"),adminPage.getContentAsByteArray());
        mvc.perform(post("/quan-tri/dich-vu/"+source+"/duyet").with(user(users.loadUserByUsername(address(admin)))).with(csrf()).param("phienBan",String.valueOf(service().getPhienBanNoiDung())).param("chapThuan","true")).andExpect(redirectedUrl("/quan-tri"));
    }
}
