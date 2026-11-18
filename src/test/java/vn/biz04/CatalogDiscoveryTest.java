package vn.biz04;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import vn.biz04.data.Store;
import vn.biz04.domain.*;
import vn.biz04.service.*;
import vn.biz04.security.DiscoveryLoginFlow;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:catalog;MODE=MSSQLServer;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false","biz.otp.hmac-key=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=","biz.cccd.key=AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQE="})
@AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class CatalogDiscoveryTest {
    @Autowired TestServices testServices;@Autowired Store db;@Autowired CatalogDiscovery discovery;@Autowired CatalogService catalog;@Autowired MockMvc mvc;
    @Autowired ProposalService proposals;@Autowired ContractService contracts;
    @MockBean OtpMailGateway mail;
    NguoiDung a,b,customer;String prefix,field;DichVu a1,a2,b1;
    @BeforeEach void setup(){
        prefix="C"+UUID.randomUUID();field="Thiết kế "+prefix;
        a=TestAccounts.create(db,"Nguyễn Minh An "+prefix,prefix+"a@test.local","Demo@62481",false,true);
        b=TestAccounts.create(db,"Trần Quỳnh Bình "+prefix,prefix+"b@test.local","Demo@62481",false,true);
        customer=TestAccounts.create(db,"Khách kiểm tra "+prefix,prefix+"c@test.local","Demo@62481",true,false);
        a1=service(a,"Nhận diện thương hiệu",field);a2=service(a,"Thiết kế giao diện",field);b1=service(b,"Website doanh nghiệp","Phát triển "+prefix);
        HoSoKhachHang profile=new HoSoKhachHang();profile.maNguoiDung=a.getMaNguoiDung();profile.gioiThieu="Tôi thiết kế trải nghiệm sản phẩm và nhận diện thương hiệu. "+"Giới thiệu chuyên môn. ".repeat(16)+"BIO_DETAIL_ONLY";profile.soDienThoai="0962481678";profile.soCccdMaHoa="PRIVATE-CIPHER";profile.cccdBonSoCuoi="1234";profile.diaChi="PRIVATE-ADDRESS";db.save(profile);db.flush();
    }
    DichVu service(NguoiDung owner,String name,String category){return testServices.create(owner.getMaNguoiDung(),null,name+" "+prefix,category,"Dịch vụ có phạm vi rõ ràng. "+"Mô tả chi tiết chuyên môn. ".repeat(16)+"SERVICE_DETAIL_ONLY",new BigDecimal("2500000"));}
    CatalogDiscovery.Filter filter(String keyword){return CatalogDiscovery.Filter.parse(keyword,"","1","1");}
    String html(String path,NguoiDung viewer)throws Exception{var req=get(path);if(viewer!=null)req.with(user(viewer.getEmail()));return mvc.perform(req).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();}
    @Test void expertsAreUniqueAndRequireAnApprovedPublicService(){
        NguoiDung independent=TestAccounts.create(db,"Chuyên gia độc lập "+prefix,prefix+"i@test.local","Demo@62481",false,true);db.flush();
        var listing=discovery.listing(filter(prefix));assertEquals(2,listing.experts().total());assertEquals(3,listing.services().total());assertEquals(1,listing.experts().items().stream().filter(e->e.id().equals(a.getMaNguoiDung())).count());assertFalse(listing.experts().items().stream().anyMatch(e->e.id().equals(independent.getMaNguoiDung())));
    }
    @Test void onlyActiveExpertsAndTheirPublishedServicesAppear(){
        a2.setTrangThai("DA_AN");b1.setTrangThai("NHAP");b.setTrangThai("KHOA");db.flush();var result=discovery.listing(filter(prefix));assertEquals(1,result.experts().total());assertEquals(List.of(a1.getMaDichVu()),result.services().items().stream().map(CatalogDiscovery.ServiceItem::id).toList());
        a.setLaChuyenGia(false);db.flush();assertEquals(0,discovery.listing(filter(prefix)).services().total());
    }
    @Test void categoriesComeFromPublicServicesAndBothSectionsShareThem(){
        var result=discovery.listing(CatalogDiscovery.Filter.parse(prefix,field,"1","1"));assertEquals(1,result.experts().total());assertEquals(2,result.services().total());assertEquals(List.of(field),result.experts().items().get(0).fields());
        a1.setTrangThai("DA_AN");a2.setTrangThai("DA_AN");db.flush();assertFalse(discovery.fields().contains(field));assertThrows(ResponseStatusException.class,()->discovery.listing(CatalogDiscovery.Filter.parse("",field,"1","1")));
    }
    @Test void searchUsesActualNamesTitlesBiosAndDescriptionsLiterally(){
        assertEquals(1,discovery.listing(filter("Nguyễn Minh An "+prefix)).experts().total());assertEquals(2,discovery.listing(filter("Nguyễn Minh An "+prefix)).services().total());
        assertEquals(1,discovery.listing(filter("nhận diện thương hiệu "+prefix)).services().total());assertEquals(1,discovery.listing(filter("BIO_DETAIL_ONLY")).experts().total());
        a1.setTenDichVu("100%_[!] "+prefix);db.flush();assertEquals(1,discovery.listing(filter("100%_[!] "+prefix)).services().total());assertEquals(0,discovery.listing(filter("%all%"+prefix)).services().total());
    }
    @Test void independentDatabasePagesKeepBothPageNumbersAndFilters(){
        for(int i=0;i<8;i++){NguoiDung u=TestAccounts.create(db,"Chuyên gia "+i+" "+prefix,prefix+i+"@test.local","Demo@62481",false,true);service(u,"Gói dịch vụ "+i,field);}db.flush();
        var first=discovery.listing(CatalogDiscovery.Filter.parse(prefix,field,"1","2"));var second=discovery.listing(CatalogDiscovery.Filter.parse(prefix,field,"2","2"));assertEquals(9,first.experts().total());assertEquals(10,first.services().total());assertEquals(6,first.experts().items().size());assertEquals(3,second.experts().items().size());assertEquals(first.services().items(),second.services().items());assertTrue(first.experts().next().contains("trangDichVu=2"));assertTrue(first.experts().next().contains("trangChuyenGia=2"));assertTrue(first.experts().next().contains("linhVuc="));assertTrue(first.services().previous().contains("trangChuyenGia=1"));
        Set<Long> seen=new HashSet<>();first.experts().items().forEach(e->seen.add(e.id()));second.experts().items().forEach(e->assertTrue(seen.add(e.id())));assertTrue(first.experts().items().get(0).id()>first.experts().items().get(1).id());
    }
    @Test void parametersAreBoundedAndInvalidValuesGet400()throws Exception{
        for(String page:List.of("0","-1","x","100001","1.5","9999999999"))mvc.perform(get("/trang/trangchu").param("trangChuyenGia",page)).andExpect(status().isBadRequest());
        mvc.perform(get("/trang/trangchu").param("q","a".repeat(201))).andExpect(status().isBadRequest());mvc.perform(get("/trang/trangchu").param("linhVuc","unknown")).andExpect(status().isBadRequest());
        var result=discovery.listing(CatalogDiscovery.Filter.parse(prefix,"","99999","99999"));assertEquals(1,result.experts().number());assertTrue(result.services().items().size()<=CatalogDiscovery.PAGE_SIZE);
    }
    @Test void publicCardsCarrySummariesAndNoPrivateFields()throws Exception{
        String body=html("/trang/trangchu?q="+prefix,null);for(String secret:List.of(a.getEmail(),b.getEmail(),a.getMatKhauBam(),"0962481678","PRIVATE-CIPHER","PRIVATE-ADDRESS","SERVICE_DETAIL_ONLY","BIO_DETAIL_ONLY"))assertFalse(body.contains(secret),secret);
        assertTrue(discovery.listing(filter(prefix)).experts().items().stream().allMatch(e->e.introduction()==null||e.introduction().length()<=240));assertTrue(discovery.listing(filter(prefix)).services().items().stream().allMatch(d->d.summary().length()<=240));
        assertFalse(body.contains("mai-phuoc-sang.jpg"));assertTrue(body.contains("Chưa có đánh giá"));
    }
    @Test void profileUsesViewedOwnerNotViewerAndHasPagedServices()throws Exception{
        var profile=discovery.profile(a.getMaNguoiDung(),1,1);assertEquals(Set.of(a1.getMaDichVu(),a2.getMaDichVu()),new HashSet<>(profile.services().items().stream().map(CatalogDiscovery.ServiceItem::id).toList()));
        String body=html("/chuyen-gia/"+a.getMaNguoiDung(),b);assertTrue(body.contains(a1.getTenDichVu()));assertTrue(body.contains(a2.getTenDichVu()));assertFalse(body.contains(b1.getTenDichVu()));assertTrue(body.contains("BIO_DETAIL_ONLY"));assertFalse(body.contains("PRIVATE-CIPHER"));assertFalse(body.contains(a.getEmail()));
        for(int i=0;i<6;i++)service(a,"Thêm dịch vụ "+i,field);db.flush();assertEquals(6,discovery.profile(a.getMaNguoiDung(),1,1).services().items().size());assertEquals(2,discovery.profile(a.getMaNguoiDung(),2,1).services().items().size());
    }
    @Test void serviceDetailDerivesOwnerIgnoresForgedClientOwnerAndLimitsOtherServices()throws Exception{
        for(int i=0;i<5;i++)service(a,"Gói bổ sung "+i,field);db.flush();var detail=discovery.detail(a1.getMaDichVu());assertEquals(a.getMaNguoiDung(),detail.expert().id());assertEquals(4,detail.others().size());assertEquals(6,detail.otherCount());assertTrue(detail.others().stream().allMatch(d->d.ownerId().equals(a.getMaNguoiDung())&&!d.id().equals(a1.getMaDichVu())));
        String body=html("/dich-vu/xem/"+a1.getMaDichVu()+"?chuyenGia="+b.getMaNguoiDung(),b);assertFalse(body.contains(b1.getTenDichVu()));assertTrue(body.contains("SERVICE_DETAIL_ONLY"));assertTrue(body.contains("/chuyen-gia/"+a.getMaNguoiDung()+"#dich-vu"));assertTrue(body.contains("Xem hồ sơ"));
        String customerBody=html("/dich-vu/xem/"+a1.getMaDichVu(),customer);assertTrue(customerBody.contains("Tạo yêu cầu từ dịch vụ"));assertFalse(body.contains("Tạo yêu cầu từ dịch vụ"));
    }
    @Test void missingDataAndEmptyOwnerServicesHaveHonestStates()throws Exception{
        String body=html("/chuyen-gia/"+b.getMaNguoiDung(),customer);assertTrue(body.contains("Chuyên gia chưa bổ sung giới thiệu."));assertTrue(body.contains("Chưa có đánh giá"));assertFalse(body.contains("theo giờ"));
        catalog.visibility(b.getMaNguoiDung(),b1.getMaDichVu(),false);mvc.perform(get("/chuyen-gia/"+b.getMaNguoiDung()).with(user(customer.getEmail()))).andExpect(status().isNotFound());assertTrue(html("/dich-vu/xem/"+a1.getMaDichVu(),customer).contains("Dịch vụ khác của chuyên gia"));catalog.visibility(a.getMaNguoiDung(),a2.getMaDichVu(),false);assertTrue(html("/dich-vu/xem/"+a1.getMaDichVu(),customer).contains("Chuyên gia chưa có dịch vụ công khai khác."));
        assertTrue(html("/trang/trangchu?q=missing-"+prefix,null).contains("Chưa có chuyên gia phù hợp."));
    }
    @Test void missingHiddenInactiveProfilesAndServicesReturn404()throws Exception{
        mvc.perform(get("/chuyen-gia/999999999").with(user(customer.getEmail()))).andExpect(status().isNotFound());mvc.perform(get("/dich-vu/xem/999999999").with(user(customer.getEmail()))).andExpect(status().isNotFound());
        a1.setTrangThai("DA_AN");db.flush();mvc.perform(get("/dich-vu/xem/"+a1.getMaDichVu()).with(user(customer.getEmail()))).andExpect(status().isNotFound());a.setTrangThai("KHOA");db.flush();mvc.perform(get("/chuyen-gia/"+a.getMaNguoiDung()).with(user(customer.getEmail()))).andExpect(status().isNotFound());mvc.perform(get("/dich-vu/xem/"+a2.getMaDichVu()).with(user(customer.getEmail()))).andExpect(status().isNotFound());
    }
    @Test void guestCanListFilterPageAndAllDetailEntrypointsRequireRegistration()throws Exception{
        for(String path:List.of("/","/trang/trangchu?q="+prefix,"/trang/trangchu?trangDichVu=2","/ve-sang-connect"))html(path,null);
        for(String path:List.of("/chuyen-gia/"+a.getMaNguoiDung(),"/dich-vu/xem/"+a1.getMaDichVu())){
            var result=mvc.perform(get(path)).andExpect(status().isFound()).andExpect(redirectedUrl("/dang-ky?chiTiet=1")).andReturn();MockHttpSession session=(MockHttpSession)result.getRequest().getSession(false);assertEquals(path,session.getAttribute(DiscoveryLoginFlow.TARGET));mvc.perform(get("/dang-ky").session(session)).andExpect(status().isOk()).andExpect(content().string(containsString("Đăng ký tài khoản để xem chi tiết chuyên gia và dịch vụ."))).andExpect(content().string(containsString("Đã có tài khoản?")));
            mvc.perform(get(path).accept("application/json")).andExpect(status().isUnauthorized()).andExpect(content().string(""));
        }
    }
    @Test void loginReturnsToAllowedDetailAndConsumesTheTarget()throws Exception{
        MockHttpSession session=new MockHttpSession();String target="/dich-vu/xem/"+a1.getMaDichVu();mvc.perform(get(target).session(session)).andExpect(redirectedUrl("/dang-ky?chiTiet=1"));mvc.perform(post("/dang-nhap").session(session).with(csrf()).param("email",customer.getEmail()).param("password","Demo@62481")).andExpect(redirectedUrl(target));assertNull(session.getAttribute(DiscoveryLoginFlow.TARGET));
    }
    @Test void unsafeRemovedAndUnlistedReturnTargetsFallBackWithoutLoops()throws Exception{
        for(String target:List.of("https://evil.test/","//evil.test/","/quan-tri","/chuyen-gia/1?next=//evil.test","/chuyen-gia/../quan-tri","/dich-vu/xem/999999999","/chuyen-gia/999999999")){
            MockHttpSession session=new MockHttpSession();session.setAttribute(DiscoveryLoginFlow.TARGET,target);mvc.perform(post("/dang-nhap").session(session).with(csrf()).param("email",customer.getEmail()).param("password","Demo@62481")).andExpect(redirectedUrl("/bang-dieu-khien"));assertNull(session.getAttribute(DiscoveryLoginFlow.TARGET));
        }
        assertFalse(DiscoveryLoginFlow.allowed("/%2f%2fevil.test"));
    }
    @Test void lockedSessionCannotReadDetailOrGainOwnerEditRights()throws Exception{
        customer.setTrangThai("KHOA");db.flush();mvc.perform(get("/chuyen-gia/"+a.getMaNguoiDung()).with(user(customer.getEmail()))).andExpect(redirectedUrl("/dang-nhap?session=expired"));mvc.perform(get("/dich-vu/xem/"+a1.getMaDichVu()).with(user(customer.getEmail()))).andExpect(redirectedUrl("/dang-nhap?session=expired"));
        mvc.perform(get("/dich-vu/soan").param("id",a1.getMaDichVu().toString()).with(user(b.getEmail()))).andExpect(redirectedUrl("/bang-dieu-khien"));mvc.perform(post("/dich-vu/"+a1.getMaDichVu()+"/hien-thi").with(user(b.getEmail())).param("hien","false")).andExpect(status().isForbidden());
    }
    HopDong contract(){var request=catalog.request(customer.getMaNguoiDung(),null,"Yêu cầu đánh giá "+prefix,"Công việc kiểm tra công khai đánh giá",BigDecimal.TEN,null);var proposal=catalog.proposal(a.getMaNguoiDung(),request.getMaYeuCau(),"Phương án kiểm tra đánh giá đúng dữ liệu",BigDecimal.TEN,2);proposals.accept(customer.getMaNguoiDung(),proposal.getMaDeXuat());return TestContracts.legacy(db,proposal.getMaDeXuat());}
    void review(HopDong contract,int score,String state,String text){DanhGia r=new DanhGia();r.setMaHopDong(contract.getMaHopDong());r.setMaNguoiDanhGia(customer.getMaNguoiDung());r.setMaNguoiDuocDanhGia(a.getMaNguoiDung());r.setDiem(score);r.setNoiDung(text);r.setTrangThai(state);r.setNgayGui(LocalDateTime.now());if(state.equals("CONG_KHAI"))r.setNgayCongKhai(LocalDateTime.now());db.save(r);}
    @Test void onlyPublicReviewsAffectRatingsAndCompletedMeansCompletedContract()throws Exception{
        HopDong completed=contract();completed.setTrangThai("HOAN_THANH");completed.setNgayKetThuc(LocalDateTime.now());review(completed,4,"CONG_KHAI","PUBLIC_REVIEW");review(contract(),1,"DANG_AN","BLIND_PRIVATE_REVIEW");review(contract(),1,"BI_AN","MODERATED_PRIVATE_REVIEW");db.flush();
        var expert=discovery.expert(a.getMaNguoiDung());assertEquals(1,expert.completed());assertEquals(1,expert.reviewCount());assertEquals(4.0,expert.average());String body=html("/chuyen-gia/"+a.getMaNguoiDung(),b);assertTrue(body.contains("PUBLIC_REVIEW"));assertFalse(body.contains("BLIND_PRIVATE_REVIEW"));assertFalse(body.contains("MODERATED_PRIVATE_REVIEW"));assertFalse(html("/trang/trangchu?q="+prefix,null).contains("PRIVATE_REVIEW"));
    }
    @Test void registrationOtpAndLoginKeepTheSelectedDetail()throws Exception{
        AtomicReference<String> code=new AtomicReference<>();when(mail.send(anyString(),anyString())).thenAnswer(call->{code.set(call.getArgument(1));return OtpMailGateway.Delivery.ACCEPTED;});String email=prefix+"new@test.local",target="/chuyen-gia/"+a.getMaNguoiDung();MockHttpSession session=new MockHttpSession();mvc.perform(get(target).session(session)).andExpect(redirectedUrl("/dang-ky?chiTiet=1"));
        mvc.perform(post("/dang-ky").session(session).with(csrf()).param("hoTen","Người xem mới").param("email",email).param("password","Demo@62481").param("passwordAgain","Demo@62481").param("soDienThoai","0962481678").param("loaiKhachHang","CA_NHAN")).andExpect(redirectedUrl("/xac-thuc-email"));assertNotNull(code.get());
        mvc.perform(post("/dang-nhap").session(session).with(csrf()).param("email",email).param("password","Demo@62481")).andExpect(redirectedUrl("/xac-thuc-email/tu-dang-nhap"));assertEquals(target,session.getAttribute(DiscoveryLoginFlow.TARGET));
        mvc.perform(post("/xac-thuc-email").session(session).with(csrf()).param("code",code.get())).andExpect(redirectedUrl("/dang-nhap"));mvc.perform(post("/dang-nhap").session(session).with(csrf()).param("email",email).param("password","Demo@62481")).andExpect(redirectedUrl(target));
    }
    @Test void homeKeepsHeroAndUsesBoundedRealDataWithConsistentDestinations()throws Exception{
        var home=discovery.home(filter(""));assertTrue(home.experts().items().size()<=3);assertTrue(home.services().items().size()<=3);
        String body=mvc.perform(get("/").param("linhVuc",field)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();assertTrue(body.contains("id=\"connect-film\""));assertTrue(body.contains("/videos/sang-connect-720p.mp4"));assertTrue(body.contains("Xem tất cả chuyên gia"));assertTrue(body.contains("linhVuc="));assertTrue(body.contains("#chuyen-gia"));assertTrue(body.contains("/dich-vu/xem/"+a1.getMaDichVu()));
    }
    @Test void captureRenderedStatesForRealBrowserQa()throws Exception{
        for(int i=0;i<6;i++)service(a,"Gói thiết kế sản phẩm "+i,field);
        // Human-readable visual fixtures stay in the rolled-back test transaction only.
        a.setHoTen("Nguyễn Minh An");b.setHoTen("Trần Quỳnh Bình");customer.setHoTen("Lê Thanh Hà");
        TestAccounts.create(db,"Phạm Gia Linh",prefix+"visual@test.local","Demo@62481",false,true);
        db.get(HoSoKhachHang.class,a.getMaNguoiDung()).gioiThieu="Tôi thiết kế trải nghiệm sản phẩm và nhận diện thương hiệu. Từ nghiên cứu nhu cầu đến hoàn thiện giao diện, tôi cùng khách hàng xác định phạm vi và theo dõi từng mốc công việc.";
        for(DichVu d:db.list("from DichVu d where d.maChuyenGia in :ids",DichVu.class,"ids",List.of(a.getMaNguoiDung(),b.getMaNguoiDung()))){d.setTenDichVu(d.getTenDichVu().replace(" "+prefix,""));d.setLinhVuc(d.getMaChuyenGia().equals(a.getMaNguoiDung())?"Thiết kế":"Phát triển website");d.setMoTa("Trao đổi nhu cầu, thống nhất phạm vi và xây dựng sản phẩm phù hợp với mục tiêu của bạn. Bản bàn giao được đối chiếu theo các tiêu chí đã thống nhất.\n\nPhạm vi công việc được làm rõ trong quá trình trao đổi đề xuất và hợp đồng.");}
        db.flush();Path folder=Path.of(".run","catalog-qa","rendered");Files.createDirectories(folder);
        Map<String,String> paths=new LinkedHashMap<>();paths.put("listing","/trang/trangchu");paths.put("profile","/chuyen-gia/"+a.getMaNguoiDung());paths.put("service","/dich-vu/xem/"+a1.getMaDichVu());paths.put("empty","/trang/trangchu?q=missing-"+prefix);paths.put("home","/");
        for(var entry:paths.entrySet())Files.writeString(folder.resolve(entry.getKey()+".html"),html(entry.getValue(),entry.getKey().equals("profile")||entry.getKey().equals("service")?customer:null));
    }
}
