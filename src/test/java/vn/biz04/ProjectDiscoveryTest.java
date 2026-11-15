package vn.biz04;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.security.access.AccessDeniedException;
import vn.biz04.data.Store;
import vn.biz04.domain.*;
import vn.biz04.service.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.nio.file.*;
import java.util.regex.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.hamcrest.Matchers.containsString;

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:discovery;MODE=MSSQLServer;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false")
@AutoConfigureMockMvc @ActiveProfiles("test")
class ProjectDiscoveryTest {
    @Autowired TestServices testServices;@Autowired Store db;@Autowired TransactionTemplate tx;@Autowired CatalogService catalog;@Autowired ProjectDiscovery projects;@Autowired MockMvc mvc;
    NguoiDung customer,other,expert,anotherExpert,admin;String prefix;
    @BeforeEach void setup(){prefix="P"+UUID.randomUUID();tx.executeWithoutResult(s->{customer=TestAccounts.create(db,"Khách dự án",prefix+"c@test.local","Demo@12345",true,false);other=TestAccounts.create(db,"Khách khác",prefix+"o@test.local","Demo@12345",true,false);expert=TestAccounts.create(db,"Chuyên gia dự án",prefix+"e@test.local","Demo@12345",false,true);anotherExpert=TestAccounts.create(db,"Chuyên gia khác",prefix+"a@test.local","Demo@12345",false,true);admin=TestAccounts.create(db,"Quản trị",prefix+"admin@test.local","Demo@12345",false,false);admin.setLaQuanTriVien(true);});}
    YeuCau request(NguoiDung owner,String suffix,int amount){return catalog.request(owner.getMaNguoiDung(),null,prefix+" "+suffix,"Mô tả công việc đầy đủ cho "+suffix,BigDecimal.valueOf(amount),LocalDate.of(2026,12,15));}
    ProjectDiscovery.Filter filter(){return ProjectDiscovery.Filter.parse(prefix,"","","","1");}
    void state(YeuCau y,String state){tx.executeWithoutResult(s->db.get(YeuCau.class,y.getMaYeuCau()).setTrangThai(state));}
    List<String> menu(String path,NguoiDung u)throws Exception{
        var req=get(path);if(u!=null)req.with(user(u.getEmail()));String html=mvc.perform(req).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();String nav=html.substring(html.indexOf("<nav"),html.indexOf("</nav>"));Matcher matcher=Pattern.compile("<a[^>]*>([^<]*)</a>").matcher(nav);List<String> names=new ArrayList<>();while(matcher.find())names.add(matcher.group(1));return names;
    }
    @Test void menusMatchPublicCustomerExpertAndAdmin()throws Exception{
        assertEquals(List.of("Trang chủ","Tìm chuyên gia","Về Sang Connect"),menu("/",null));
        assertEquals(List.of("Trang chủ","Tìm chuyên gia","Về Sang Connect","Yêu cầu của tôi","Không gian làm việc"),menu("/",customer));
        assertEquals(List.of("Trang chủ","Tìm dự án","Về Sang Connect","Không gian làm việc"),menu("/",expert));
        assertEquals(List.of("Trang chủ","Tìm chuyên gia","Về Sang Connect","Yêu cầu công việc","Không gian làm việc","Quản trị"),menu("/",admin));
    }
    @Test void anonymousAndCustomerCannotAccessExpertDiscovery()throws Exception{
        mvc.perform(get("/tim-du-an")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrlPattern("**/dang-nhap"));
        mvc.perform(get("/tim-du-an").with(user(customer.getEmail()))).andExpect(status().isForbidden());
        mvc.perform(get("/tim-du-an").with(user(expert.getEmail()))).andExpect(status().isOk()).andExpect(view().name("projects"));
        assertThrows(AccessDeniedException.class,()->projects.find(customer.getMaNguoiDung(),filter()));
    }
    @Test void onlyOpenEligibleRequestsFromMultipleCustomersAppear(){
        var a=request(customer,"mở một",100);var b=request(other,"mở hai",200);var c=request(customer,"đóng",300);state(c,"DA_DONG");var draft=request(customer,"nháp thử nghiệm",400);state(draft,"NHAP");var hidden=request(customer,"ẩn thử nghiệm",500);state(hidden,"DA_AN");var negotiating=request(customer,"thương lượng",600);state(negotiating,"DANG_THUONG_LUONG");
        var result=projects.find(expert.getMaNguoiDung(),filter());assertEquals(Set.of(a.getMaYeuCau(),b.getMaYeuCau()),new HashSet<>(result.items().stream().map(ProjectDiscovery.Item::id).toList()));
        tx.executeWithoutResult(s->db.get(NguoiDung.class,other.getMaNguoiDung()).setTrangThai("KHOA"));assertEquals(1,projects.find(expert.getMaNguoiDung(),filter()).total());
    }
    @Test void targetedServiceRequestsRespectExistingProviderRule(){
        var service=testServices.create(expert.getMaNguoiDung(),null,"Dịch vụ kiểm tra "+prefix,"Thiết kế "+prefix,"Dịch vụ mô tả đủ dài",BigDecimal.valueOf(300));
        var target=catalog.request(customer.getMaNguoiDung(),service.getMaDichVu(),prefix+" yêu cầu dịch vụ","Mô tả yêu cầu đủ dài",BigDecimal.valueOf(300),null);
        assertEquals(1,projects.find(expert.getMaNguoiDung(),filter()).total());assertEquals(0,projects.find(anotherExpert.getMaNguoiDung(),filter()).total());
        assertEquals(1,projects.find(expert.getMaNguoiDung(),ProjectDiscovery.Filter.parse(prefix,service.getLinhVuc(),"","","1")).total());
        assertEquals(0,projects.find(expert.getMaNguoiDung(),ProjectDiscovery.Filter.parse(prefix,"_NONE","","","1")).total());
        assertThrows(BusinessException.class,()->catalog.proposal(anotherExpert.getMaNguoiDung(),target.getMaYeuCau(),"Phương án của người không được chọn",BigDecimal.TEN,2));
    }
    @Test void filtersBudgetsLiteralKeywordAndPaginationAreStable(){
        for(int i=0;i<12;i++)request(customer,"nội dung "+i,100+i);
        var first=projects.find(expert.getMaNguoiDung(),ProjectDiscovery.Filter.parse(prefix,"","100","200","1"));var second=projects.find(expert.getMaNguoiDung(),ProjectDiscovery.Filter.parse(prefix,"","100","200","2"));
        assertEquals(12,first.total());assertEquals(9,first.items().size());assertEquals(3,second.items().size());assertNotNull(first.next());assertTrue(first.next().contains("tu=100"));assertTrue(first.next().contains("den=200"));assertTrue(first.next().contains("q="+prefix));assertTrue(first.next().contains("trang=2"));
        Set<Long> ids=new HashSet<>();first.items().forEach(i->ids.add(i.id()));second.items().forEach(i->assertTrue(ids.add(i.id())));
        assertEquals(3,projects.find(expert.getMaNguoiDung(),ProjectDiscovery.Filter.parse(prefix,"","102","104","1")).total());
        request(customer,"100% nguyên bản",500);assertEquals(1,projects.find(expert.getMaNguoiDung(),ProjectDiscovery.Filter.parse(prefix+" 100%","","","","1")).total());
    }
    @Test void missingFieldsAreNotInventedAndAuditDateOrdersNewestFirst(){
        var older=request(customer,"đăng cũ",100);var newer=request(other,"đăng mới",200);
        tx.executeWithoutResult(s->{for(NhatKyXuLy n:db.list("from NhatKyXuLy n where n.loaiDoiTuong='YeuCau' and n.hanhDong='DANG_YEU_CAU' and n.maDoiTuong in :ids",NhatKyXuLy.class,"ids",List.of(older.getMaYeuCau(),newer.getMaYeuCau())))n.setThoiDiem(LocalDateTime.of(2026,10,27,Objects.equals(n.getMaDoiTuong(),older.getMaYeuCau())?1:2,0));});
        var result=projects.find(expert.getMaNguoiDung(),filter());assertEquals(newer.getMaYeuCau(),result.items().get(0).id());assertEquals(LocalDateTime.of(2026,10,27,2,0),result.items().get(0).posted());assertNull(result.items().get(0).field());
        tx.executeWithoutResult(s->{for(NhatKyXuLy n:db.list("from NhatKyXuLy n where n.loaiDoiTuong='YeuCau' and n.maDoiTuong=:id",NhatKyXuLy.class,"id",older.getMaYeuCau()))db.remove(n);});assertNull(projects.details(older.getMaYeuCau(),expert.getMaNguoiDung()).posted());
    }
    @Test void organizationDisplayAndPrivateFieldsDoNotLeak()throws Exception{
        var y=request(customer,"riêng tư",100);tx.executeWithoutResult(s->{HoSoKhachHang h=new HoSoKhachHang();h.maNguoiDung=customer.getMaNguoiDung();h.loaiKhachHang="TO_CHUC";h.tenToChuc="Công ty Công Khai";h.soDienThoai="0912345678";h.soCccdMaHoa="PRIVATE-CIPHER";h.cccdBonSoCuoi="1234";db.save(h);});
        String body=mvc.perform(get("/tim-du-an").param("q",prefix).with(user(expert.getEmail()))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();assertTrue(body.contains("Công ty Công Khai"));for(String value:List.of(customer.getEmail(),"0912345678","PRIVATE-CIPHER"))assertFalse(body.contains(value));
        String detail=mvc.perform(get("/yeu-cau/"+y.getMaYeuCau()).with(user(expert.getEmail()))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();assertTrue(detail.contains("Công ty Công Khai"));assertFalse(detail.contains(customer.getEmail()));
    }
    @Test void ownRequestsListContainsNoOtherOwnersRequests()throws Exception{
        var mine=request(customer,"của tôi",100);var notMine=request(other,"của người khác",200);
        String body=mvc.perform(get("/yeu-cau").with(user(customer.getEmail()))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();assertTrue(body.contains(mine.getTieuDe()));assertFalse(body.contains(notMine.getTieuDe()));assertTrue(body.contains("Yêu cầu của tôi"));
        assertThrows(BusinessException.class,()->catalog.closeRequest(customer.getMaNguoiDung(),notMine.getMaYeuCau()));assertThrows(BusinessException.class,()->catalog.editRequest(customer.getMaNguoiDung(),notMine.getMaYeuCau(),"Nội dung giả mạo","Không được sửa yêu cầu của người khác",BigDecimal.TEN,null));
    }
    @Test void proposalUpdatesOneRecordAndWithdrawnCanResubmit(){
        var y=request(customer,"đề xuất",100);var a=catalog.proposal(expert.getMaNguoiDung(),y.getMaYeuCau(),"Phương án đề xuất lần đầu",BigDecimal.TEN,2);var b=catalog.proposal(expert.getMaNguoiDung(),y.getMaYeuCau(),"Phương án cập nhật hiện có",BigDecimal.valueOf(20),3);assertEquals(a.getMaDeXuat(),b.getMaDeXuat());assertTrue(projects.find(expert.getMaNguoiDung(),filter()).items().get(0).proposed());catalog.withdraw(expert.getMaNguoiDung(),a.getMaDeXuat());assertFalse(projects.find(expert.getMaNguoiDung(),filter()).items().get(0).proposed());assertEquals(a.getMaDeXuat(),catalog.proposal(expert.getMaNguoiDung(),y.getMaYeuCau(),"Gửi lại theo quy tắc hiện có",BigDecimal.TEN,2).getMaDeXuat());
        state(y,"DA_DONG");assertThrows(BusinessException.class,()->catalog.proposal(expert.getMaNguoiDung(),y.getMaYeuCau(),"Đề xuất vào yêu cầu đã đóng",BigDecimal.TEN,2));assertThrows(BusinessException.class,()->catalog.proposal(customer.getMaNguoiDung(),y.getMaYeuCau(),"Khách hàng không được đề xuất",BigDecimal.TEN,2));
    }
    @Test void concurrentProposalsNeverCreateDuplicates()throws Exception{
        var y=request(customer,"đồng thời",100);ExecutorService pool=Executors.newFixedThreadPool(2);CountDownLatch gate=new CountDownLatch(1);
        try{Callable<Long> submit=()->{gate.await();return catalog.proposal(expert.getMaNguoiDung(),y.getMaYeuCau(),"Phương án gửi đồng thời kiểm thử",BigDecimal.TEN,3).getMaDeXuat();};var a=pool.submit(submit);var b=pool.submit(submit);gate.countDown();assertEquals(a.get(20,TimeUnit.SECONDS),b.get(20,TimeUnit.SECONDS));assertEquals(1L,tx.execute(s->db.count("select count(d) from DeXuat d where d.maYeuCau=:id","id",y.getMaYeuCau())).longValue());}finally{pool.shutdownNow();}
    }
    @Test void requestDetailAndFormsRespectProviderAndRole()throws Exception{
        var y=request(customer,"quyền chi tiết",100);mvc.perform(get("/yeu-cau/"+y.getMaYeuCau()).with(user(other.getEmail()))).andExpect(status().isForbidden());
        mvc.perform(get("/yeu-cau/"+y.getMaYeuCau()).with(user(expert.getEmail()))).andExpect(status().isOk()).andExpect(model().attribute("canPropose",true));
        state(y,"DA_DONG");mvc.perform(get("/yeu-cau/"+y.getMaYeuCau()).with(user(expert.getEmail()))).andExpect(status().isForbidden());mvc.perform(get("/yeu-cau/"+y.getMaYeuCau()).with(user(customer.getEmail()))).andExpect(status().isOk());
    }
    @Test void validationAndEmptyResultsAreClear()throws Exception{
        for(String[] invalid:new String[][]{{"-1","","1"},{"1.5","","1"},{"200","100","1"},{"0","","0"},{"0","","x"},{"1000000000001","","1"}})assertThrows(BusinessException.class,()->ProjectDiscovery.Filter.parse("","",invalid[0],invalid[1],invalid[2]));
        assertThrows(BusinessException.class,()->projects.find(expert.getMaNguoiDung(),ProjectDiscovery.Filter.parse(prefix,"không tồn tại","","","1")));
        mvc.perform(get("/tim-du-an").param("q","not-found-"+prefix).with(user(expert.getEmail()))).andExpect(status().isOk()).andExpect(content().string(containsString("Chưa có dự án phù hợp"))).andExpect(content().string(containsString("Xóa bộ lọc")));
        mvc.perform(get("/tim-du-an").param("tu","100").param("den","10").with(user(expert.getEmail()))).andExpect(status().isOk()).andExpect(content().string(containsString("Ngân sách từ không được lớn hơn")));
    }
    @Test void expertHomeSearchAndActiveMenuPointToProjects()throws Exception{
        String html=mvc.perform(get("/").with(user(expert.getEmail()))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();assertTrue(html.contains("action=\"/tim-du-an\""));assertTrue(html.contains("id=\"connect-film\""));String nav=html.substring(html.indexOf("<nav"),html.indexOf("</nav>"));assertTrue(nav.contains("href=\"/\" aria-current=\"page\""));assertFalse(nav.contains("href=\"/tim-du-an\" aria-current=\"page\""));
    }
    @Test void captureRenderedProjectStatesForVisualQa()throws Exception{
        for(int i=0;i<11;i++)request(customer,"Dự án thiết kế nội dung "+i,1000000+i*100000);Path folder=Path.of(".run","project-qa","rendered");Files.createDirectories(folder);
        for(String role:List.of("customer","expert","admin")){
            NguoiDung u=role.equals("customer")?customer:role.equals("expert")?expert:admin;var html=mvc.perform(get("/ve-sang-connect").with(user(u.getEmail()))).andExpect(status().isOk()).andReturn();Files.write(folder.resolve(role+"-about.html"),html.getResponse().getContentAsByteArray());
        }
        var html=mvc.perform(get("/tim-du-an").param("q",prefix).with(user(expert.getEmail()))).andExpect(status().isOk()).andReturn();Files.write(folder.resolve("projects.html"),html.getResponse().getContentAsByteArray());
    }
}

