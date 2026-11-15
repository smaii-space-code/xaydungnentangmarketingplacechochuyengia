package vn.biz04;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.*;
import org.springframework.validation.BindingResult;
import org.springframework.web.util.HtmlUtils;
import vn.biz04.data.Store;
import vn.biz04.domain.*;
import vn.biz04.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.util.regex.*;
import java.math.BigDecimal;
import java.nio.file.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:hirebinding;MODE=MSSQLServer;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false")
@AutoConfigureMockMvc(print=MockMvcPrint.NONE) @ActiveProfiles("test")
class HireOfferFormBindingTest {
    @Autowired MockMvc mvc;@Autowired Store db;@Autowired TransactionTemplate tx;@Autowired HireOfferService offers;@Autowired CatalogService catalog;@Autowired TestServices testServices;
    NguoiDung customer,expert,other;DichVu source;String tag;MockHttpSession session;
    @BeforeEach void setup(){tag=UUID.randomUUID().toString();session=new MockHttpSession();tx.executeWithoutResult(s->{customer=TestAccounts.create(db,"Khách thử "+tag,"c"+tag+"@example.test",UUID.randomUUID().toString(),true,false);expert=TestAccounts.create(db,"Chuyên gia thử "+tag,"e"+tag+"@example.test",UUID.randomUUID().toString(),false,true);other=TestAccounts.create(db,"Người ngoài thử "+tag,"o"+tag+"@example.test",UUID.randomUUID().toString(),true,false);});source=testServices.create(expert.getMaNguoiDung(),null,"Dịch vụ thử "+tag,"Thiết kế","Phạm vi dịch vụ đủ dài để kiểm thử",BigDecimal.valueOf(2500000));}
    MockHttpServletRequestBuilder browser(MockHttpServletRequestBuilder request){return request.header("User-Agent","HireFormRegressionBrowser").header("Content-Type","application/x-www-form-urlencoded").header("Accept-Language","vi-VN").header("Accept-Encoding","gzip, deflate, br").header("Sec-Fetch-Site","same-origin");}
    static Map<String,String> attributes(String source){return ProfileFormBindingTest.attrs(source);}
    static MultiValueMap<String,String> fields(String html,String action){
        var form=Pattern.compile("(?s)<form\\b[^>]*action=\""+Pattern.quote(action)+"\"[^>]*>(.*?)</form>").matcher(html);assertTrue(form.find(),"Expected rendered form action");var result=new LinkedMultiValueMap<String,String>();String body=form.group(1);
        var inputs=Pattern.compile("<input\\b([^>]*)>").matcher(body);while(inputs.find()){var a=attributes(inputs.group(1));if(!a.containsKey("name")||a.containsKey("disabled")||("checkbox".equals(a.get("type"))&&!a.containsKey("checked")))continue;result.add(a.get("name"),a.getOrDefault("value",""));}
        var areas=Pattern.compile("(?s)<textarea\\b([^>]*)>(.*?)</textarea>").matcher(body);while(areas.find())result.add(attributes(areas.group(1)).get("name"),HtmlUtils.htmlUnescape(areas.group(2)));
        var buttons=Pattern.compile("<button\\b([^>]*)>").matcher(body);while(buttons.find()){var a=attributes(buttons.group(1));if(!"button".equals(a.get("type"))&&a.containsKey("name")){result.add(a.get("name"),a.getOrDefault("value",""));break;}}
        return result;
    }
    String html(MvcResult r)throws Exception{return r.getResponse().getContentAsString();}
    void capture(String name,MvcResult result)throws Exception{Path folder=Path.of(".run","hire-binding","rendered");Files.createDirectories(folder);Files.writeString(folder.resolve(name+".html"),html(result));}
    long sent(){return offers.list(customer.getMaNguoiDung(),"da-gui",1).total();}
    MultiValueMap<String,String> initial(boolean fromService)throws Exception{
        String page=fromService?"/dich-vu/xem/"+source.getMaDichVu():"/chuyen-gia/"+expert.getMaNguoiDung();String body=html(mvc.perform(get(page).with(user(customer.getEmail()))).andReturn());String entry="/de-nghi-thue/gui/chuyen-gia/"+expert.getMaNguoiDung()+(fromService?"?dichVu="+source.getMaDichVu():"");assertTrue(body.contains("href=\""+entry+"\""),"Entry link must come from the real profile/service template");
        var request=get("/de-nghi-thue/gui/chuyen-gia/"+expert.getMaNguoiDung()).session(session).with(user(customer.getEmail()));if(fromService)request.param("dichVu",source.getMaDichVu().toString());var result=mvc.perform(request).andReturn();assertEquals(200,result.getResponse().getStatus());var data=fields(html(result),"/de-nghi-thue/gui");assertEquals(Set.of("token","_csrf","title","scope","deliverable","price","days"),data.keySet());data.set("title","Công việc kiểm thử "+tag);data.set("scope","Phạm vi công việc kiểm thử đầy đủ");data.set("deliverable","Sản phẩm kiểm thử đúng yêu cầu");data.set("price","2500000");data.set("days","14");return data;
    }
    MvcResult preview(MultiValueMap<String,String> data)throws Exception{
        var r=mvc.perform(browser(post("/de-nghi-thue/gui").session(session).with(user(customer.getEmail())).params(data).with(csrf()))).andReturn();Object binding=r.getModelAndView()==null?null:r.getModelAndView().getModel().get(BindingResult.MODEL_KEY_PREFIX+"form");List<String> suppressed=binding instanceof BindingResult b?List.of(b.getSuppressedFields()):List.of();
        var report=Map.of("submittedFields",data.keySet(),"suppressedFields",suppressed,"rejectedFields",suppressed.stream().filter(s->!Set.of("token","_csrf").contains(s)).toList(),"httpStatus",r.getResponse().getStatus());Path folder=Path.of(".run","hire-binding");Files.createDirectories(folder);new ObjectMapper().writeValue(folder.resolve("latest-field-names.json").toFile(),report);return r;
    }
    MvcResult confirm(MultiValueMap<String,String> data)throws Exception{return mvc.perform(browser(post("/de-nghi-thue/gui/xac-nhan").session(session).with(user(customer.getEmail())).params(data).with(csrf()))).andReturn();}
    MultiValueMap<String,String> reviewed(boolean fromService)throws Exception{var r=preview(initial(fromService));assertEquals("hire-create-review",r.getModelAndView().getViewName());assertEquals(0,sent());return fields(html(r),"/de-nghi-thue/gui/xac-nhan");}
    @ParameterizedTest @ValueSource(booleans={false,true}) void realProfileAndServiceFormsReviewThenConfirmOnce(boolean fromService)throws Exception{
        var data=initial(fromService);var reviewed=preview(data);assertEquals("hire-create-review",reviewed.getModelAndView().getViewName(),"Expected review; see names-only diagnostics");assertEquals(0,sent());var confirmation=fields(html(reviewed),"/de-nghi-thue/gui/xac-nhan");assertEquals(Set.of("token","_csrf"),confirmation.keySet());
        capture(fromService?"review-service":"review-profile",reviewed);Files.copy(Path.of(".run","hire-binding","latest-field-names.json"),Path.of(".run","hire-binding","accepted-fields-after-fix.json"),StandardCopyOption.REPLACE_EXISTING);
        var saved=confirm(confirmation);assertEquals(302,saved.getResponse().getStatus());String path=saved.getResponse().getRedirectedUrl();assertTrue(path.matches("/de-nghi-thue/[0-9]+"));assertEquals(path,confirm(confirmation).getResponse().getRedirectedUrl());assertEquals(1,sent());var row=offers.list(customer.getMaNguoiDung(),"da-gui",1).items().get(0);assertEquals(expert.getMaNguoiDung(),row.expert());assertEquals(customer.getMaNguoiDung(),row.customer());assertEquals(fromService?source.getMaDichVu():null,row.source());assertEquals("CHO_PHAN_HOI",row.state());assertEquals(1,offers.list(expert.getMaNguoiDung(),"nhan",1).total());assertEquals(0,offers.list(other.getMaNguoiDung(),"nhan",1).total());
        for(var view:Map.of("/de-nghi-thue/da-gui",customer,"/de-nghi-thue/nhan",expert).entrySet()){var page=mvc.perform(get(view.getKey()).with(user(view.getValue().getEmail()))).andReturn();assertEquals(200,page.getResponse().getStatus());assertTrue(html(page).contains(row.title()));}
    }
    @ParameterizedTest @ValueSource(strings={"customer","expert","source","actor","id","state","current","version","phienBan","waiting","decider","agreed","laQuanTriVien"})
    void forgedCreateFieldsRejectWithoutCreatingAnyOffer(String field)throws Exception{var data=initial(true);data.set(field,"999999");var r=preview(data);assertEquals("hire-form",r.getModelAndView().getViewName());assertTrue(html(r).contains("Biểu mẫu chứa trường không được phép cập nhật."));assertEquals(0,sent());}
    @ParameterizedTest @ValueSource(strings={"expert","customer","source","state","phienBan","price","title"})
    void finalPostAcceptsOnlyServerTicketAndCsrf(String field)throws Exception{var confirmation=reviewed(true);confirmation.set(field,"1");var blocked=confirm(confirmation);assertEquals("hire-create-review",blocked.getModelAndView().getViewName());assertTrue(html(blocked).contains("Biểu mẫu chứa trường không được phép cập nhật."));assertEquals(0,sent());confirmation.remove(field);assertEquals(302,confirm(confirmation).getResponse().getStatus());var row=offers.list(customer.getMaNguoiDung(),"da-gui",1).items().get(0);assertEquals(0,BigDecimal.valueOf(2500000).compareTo(row.price()));assertEquals("Công việc kiểm thử "+tag,row.title());}
    @Test void invalidInputKeepsAllowedFieldsAndReportsValidationError()throws Exception{var data=initial(false);data.set("price","1.5");var r=preview(data);assertEquals("hire-form",r.getModelAndView().getViewName());assertTrue(html(r).contains("Giá đề nghị phải là số nguyên dương"));var restored=fields(html(r),"/de-nghi-thue/gui");for(String key:List.of("title","scope","deliverable","price","days"))assertTrue(Objects.equals(data.getFirst(key),restored.getFirst(key)),"Allowed input should be retained");assertEquals(0,sent());capture("invalid-form",r);}
    @Test void headerCannotSupplyMissingBusinessFields()throws Exception{var data=initial(false);data.remove("title");var r=mvc.perform(browser(post("/de-nghi-thue/gui").session(session).with(user(customer.getEmail())).params(data).with(csrf())).header("Title","Nội dung chèn từ header")).andReturn();assertEquals("hire-form",r.getModelAndView().getViewName());assertTrue(html(r).contains("Tiêu đề công việc cần từ 5 đến 255 ký tự."));assertEquals(0,sent());}
    @Test void expiredIntentCannotReviewOrSendToAnyRecipient()throws Exception{var data=initial(true);session.removeAttribute("SANG_HIRE_INTENTS");var r=preview(data);assertEquals("/bang-dieu-khien",r.getResponse().getRedirectedUrl());assertTrue(r.getFlashMap().get("error").toString().contains("Phiên gửi đã hết hạn"));assertEquals(0,sent());}
    @ParameterizedTest @ValueSource(strings={"SANG_HIRE_INTENTS","SANG_HIRE_CREATE_CONFIRM"})
    void expiryAfterReviewDoesNotPersistOffer(String group)throws Exception{var data=reviewed(true);session.removeAttribute(group);var r=confirm(data);assertEquals(0,sent());assertTrue((r.getModelAndView()!=null&&"hire-create-review".equals(r.getModelAndView().getViewName()))||"/bang-dieu-khien".equals(r.getResponse().getRedirectedUrl()));}
    @Test void intentAndConfirmationBelongToTheAuthenticatedActor()throws Exception{var data=initial(true);var denied=mvc.perform(browser(post("/de-nghi-thue/gui").session(session).with(user(other.getEmail())).params(data).with(csrf()))).andReturn();assertEquals(403,denied.getResponse().getStatus());var confirmation=fields(html(preview(data)),"/de-nghi-thue/gui/xac-nhan");assertEquals(403,mvc.perform(browser(post("/de-nghi-thue/gui/xac-nhan").session(session).with(user(other.getEmail())).params(confirmation).with(csrf()))).andReturn().getResponse().getStatus());assertEquals(0,sent());}
    @Test void csrfRemainsMandatoryAtBothStages()throws Exception{var data=initial(true);data.set("_csrf","invalid");assertEquals(403,mvc.perform(browser(post("/de-nghi-thue/gui").session(session).with(user(customer.getEmail())).params(data))).andReturn().getResponse().getStatus());var confirmation=fields(html(preview(data)),"/de-nghi-thue/gui/xac-nhan");confirmation.set("_csrf","invalid");assertEquals(403,mvc.perform(browser(post("/de-nghi-thue/gui/xac-nhan").session(session).with(user(customer.getEmail())).params(confirmation))).andReturn().getResponse().getStatus());assertEquals(0,sent());}
    @ParameterizedTest @ValueSource(strings={"HIDDEN","REJECTED","OWNER","ROLE","LOCKED","EMAIL"})
    void sourceAndPartiesAreRecheckedAtFinalPost(String change)throws Exception{var confirmation=reviewed(true);tx.executeWithoutResult(s->{var d=db.get(DichVu.class,source.getMaDichVu());var e=db.get(NguoiDung.class,expert.getMaNguoiDung());switch(change){case "HIDDEN"->d.setTrangThai("DA_AN");case "REJECTED"->d.setTrangThaiDuyet("TU_CHOI");case "OWNER"->d.setMaChuyenGia(other.getMaNguoiDung());case "ROLE"->e.setLaChuyenGia(false);case "LOCKED"->e.setTrangThai("KHOA");case "EMAIL"->{e.setTrangThaiEmail("CHUYEN_TIEP");e.setThoiDiemXacThucEmail(null);}}});var r=confirm(confirmation);assertTrue(Set.of(200,403).contains(r.getResponse().getStatus()));assertEquals(0,sent());}
    @Test void sourceIsAlsoRecheckedBeforeReview()throws Exception{var data=initial(true);catalog.visibility(expert.getMaNguoiDung(),source.getMaDichVu(),false);var r=preview(data);assertEquals("hire-form",r.getModelAndView().getViewName());assertTrue(html(r).contains("không còn công khai"));assertEquals(0,sent());}
    @Test void reviewAndBackToEditDoNotWriteAndPreserveReviewedFields()throws Exception{var confirmation=reviewed(true);var r=mvc.perform(get("/de-nghi-thue/gui/sua").session(session).with(user(customer.getEmail())).param("token",confirmation.getFirst("token"))).andReturn();assertEquals("hire-form",r.getModelAndView().getViewName());var data=fields(html(r),"/de-nghi-thue/gui");assertEquals("Công việc kiểm thử "+tag,data.getFirst("title"));assertEquals("2500000",data.getFirst("price"));assertEquals(0,sent());}
    @Test void simultaneousConfirmedRetriesCreateOneOfferOnly()throws Exception{var confirmation=reviewed(true);ExecutorService pool=Executors.newFixedThreadPool(2);CountDownLatch gate=new CountDownLatch(1);try{Callable<String> post=()->{gate.await();var r=confirm(confirmation);assertEquals(302,r.getResponse().getStatus());return r.getResponse().getRedirectedUrl();};var a=pool.submit(post);var b=pool.submit(post);gate.countDown();assertEquals(a.get(30,TimeUnit.SECONDS),b.get(30,TimeUnit.SECONDS));}finally{pool.shutdownNow();}assertEquals(1,sent());}
    @Test void creationDoesNotCreateRequestsProposalsContractsMilestonesOrPayments()throws Exception{var before=unrelatedCounts();var confirmation=reviewed(true);assertEquals(before,unrelatedCounts());assertEquals(302,confirm(confirmation).getResponse().getStatus());assertEquals(before,unrelatedCounts());}
    List<Long> unrelatedCounts(){return tx.execute(s->List.of("YeuCau","DeXuat","HopDong","MocNghiemThu","GiaoDich").stream().map(entity->db.count("select count(x) from "+entity+" x")).toList());}
}
