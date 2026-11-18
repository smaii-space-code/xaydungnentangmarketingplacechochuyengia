package vn.biz04;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.*;
import org.springframework.security.access.AccessDeniedException;
import vn.biz04.data.Store;
import vn.biz04.domain.*;
import vn.biz04.service.*;
import java.math.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:electronic;MODE=MSSQLServer;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false")
@TestPropertySource(properties="biz.contract.operator-phone=")
@AutoConfigureMockMvc(print=MockMvcPrint.NONE) @ActiveProfiles("test")
class ElectronicContractTest {
    static final String CUSTOMER_SIGNATURE = "40,96 84,39 111,82 163,49 198,90";
    static final String EXPERT_SIGNATURE = "48,73 108,51 79,102 155,39 209,79";
    @Autowired Store db;@Autowired TransactionTemplate tx;@Autowired MilestonePlanService plans;@Autowired HireOfferService hires;@Autowired CatalogService catalog;@Autowired ProposalService proposals;@Autowired ContractService contracts;@Autowired RoleObligations obligations;@Autowired MockMvc mvc;
    @MockitoBean Clock clock;
    NguoiDung customer,expert,outsider,admin;Long hire,proposal,request;String tag;MockHttpSession session;
    @BeforeEach void setup(){when(clock.instant()).thenReturn(Instant.parse("2026-10-28T04:00:00Z"));when(clock.getZone()).thenReturn(ZoneOffset.UTC);tag=UUID.randomUUID().toString();session=new MockHttpSession();tx.executeWithoutResult(s->{customer=TestAccounts.create(db,"Khách hàng kiểm thử mốc","kh"+tag+"@example.test",UUID.randomUUID().toString(),true,false);expert=TestAccounts.create(db,"Chuyên gia kiểm thử mốc","cg"+tag+"@example.test",UUID.randomUUID().toString(),false,true);outsider=TestAccounts.create(db,"Người ngoài kiểm thử","ng"+tag+"@example.test",UUID.randomUUID().toString(),true,false);admin=TestAccounts.create(db,"Quản trị kiểm thử","ad"+tag+"@example.test",UUID.randomUUID().toString(),false,false);admin.setLaQuanTriVien(true);});var form=new HireOfferForm();form.setTitle("Xây dựng website kiểm thử mốc");form.setScope("Thiết kế và xây dựng website theo nội dung thống nhất.");form.setDeliverable("Website hoàn chỉnh cùng mã nguồn và tài liệu.");form.setPrice("5000000");form.setDays("40");hire=hires.create(customer.getMaNguoiDung(),expert.getMaNguoiDung(),null,UUID.randomUUID().toString(),form);hires.accept(expert.getMaNguoiDung(),hire,1);var y=catalog.request(customer.getMaNguoiDung(),null,"Dự án kiểm thử xác lập mốc","Xây dựng sản phẩm có mốc bàn giao rõ ràng.",new BigDecimal("5000000"),null);request=y.getMaYeuCau();var d=catalog.proposal(expert.getMaNguoiDung(),request,"Triển khai website với sản phẩm bàn giao kiểm thử được.",new BigDecimal("5000000"),40);proposal=d.getMaDeXuat();proposals.accept(customer.getMaNguoiDung(),proposal);}
    Long id(String kind){return kind.equals("thue")?hire:proposal;}String path(String kind){return "/ke-hoach-moc/"+kind+"/"+id(kind);}
    MilestonePlanService.View view(String kind){return plans.details(customer.getMaNguoiDung(),kind,id(kind),1);}
    MilestonePlanForm form(){var f=new MilestonePlanForm();for(int i=0;i<2;i++){var row=new MilestonePlanForm.Item();row.setTitle(i==0?"Thiết kế và bản dùng thử":"Bàn giao website hoàn chỉnh");row.setWork("Thực hiện công việc theo nội dung mốc đã phân chia.");row.setDeliverable("Mã nguồn, bản chạy và tài liệu của từng mốc.");row.setCriteria("Các chức năng chạy đúng theo danh sách nghiệm thu.");row.setDue(i==0?"15":"40");row.setAmount(i==0?"2000000":"3000000");f.getItems().add(row);}return f;}
    MilestonePlanRules.Terms terms(String kind,Long actor,MilestonePlanForm f){var v=plans.details(actor,kind,id(kind),1);return plans.preview(actor,kind,id(kind),v.source().fingerprint(),v.current(),f);}
    void send(String kind,Long actor,MilestonePlanForm f){var v=plans.details(actor,kind,id(kind),1);plans.send(actor,kind,id(kind),v.source().fingerprint(),v.current(),UUID.randomUUID().toString(),terms(kind,actor,f));}
    long count(String entity){return tx.execute(s->db.count("select count(x) from "+entity+" x"));}
    MockHttpServletRequestBuilder browser(MockHttpServletRequestBuilder request){return request.header("User-Agent","MilestoneFormBrowser").header("Accept-Language","vi-VN").header("Accept-Encoding","gzip, deflate, br").header("Content-Type","application/x-www-form-urlencoded").header("Sec-Fetch-Site","same-origin");}
    String html(MvcResult result)throws Exception{return result.getResponse().getContentAsString();}
    MultiValueMap<String,String> fields(MvcResult result,String action)throws Exception{return HireOfferFormBindingTest.fields(html(result).replaceAll("(?s)<template\\b[^>]*>.*?</template>",""),action);}
    MvcResult getAs(String path,NguoiDung user)throws Exception{return mvc.perform(get(path).session(session).with(user(user.getEmail()))).andReturn();}
    MvcResult postAs(String path,NguoiDung user,MultiValueMap<String,String> data)throws Exception{return mvc.perform(browser(post(path).session(session).with(user(user.getEmail())).params(data).with(csrf()))).andReturn();}
    MultiValueMap<String,String> initial(String kind)throws Exception{var page=getAs(path(kind)+"/thiet-lap",expert);assertEquals(200,page.getResponse().getStatus());assertEquals("milestone-editor",page.getModelAndView().getViewName());capture("editor-"+kind,page);var data=fields(page,path(kind)+"/ra-soat");data.set("mode","MANUAL");var f=form();for(int i=0;i<f.getItems().size();i++){var row=f.getItems().get(i);String prefix="items["+i+"].";data.set(prefix+"stableKey",row.getStableKey());data.set(prefix+"title",row.getTitle());data.set(prefix+"work",row.getWork());data.set(prefix+"deliverable",row.getDeliverable());data.set(prefix+"criteria",row.getCriteria());data.set(prefix+"due",row.getDue());data.set(prefix+"amount",row.getAmount());data.set(prefix+"revisions",row.getRevisions());}return data;}
    MvcResult review(String kind,MultiValueMap<String,String> data,NguoiDung actor)throws Exception{return postAs(path(kind)+"/ra-soat",actor,data);}
    MultiValueMap<String,String> reviewed(String kind)throws Exception{var r=review(kind,initial(kind),expert);assertEquals("milestone-review",r.getModelAndView().getViewName());assertNull(view(kind).planId());capture("review-"+kind,r);return fields(r,path(kind)+"/xac-nhan");}
    void capture(String name,MvcResult result)throws Exception{Path folder=Path.of(".run","econtract","rendered");Files.createDirectories(folder);Files.writeString(folder.resolve(name+".html"),html(result));}


    @Autowired PaymentService payments;@Autowired DeliveryService delivery;@Autowired TerminationService term;@Autowired DisputeService disputes;@Autowired ContractSnapshots snapshots;@Autowired ReviewService reviews;
    void agreed(String kind){send(kind,expert.getMaNguoiDung(),form());var v=view(kind);plans.accept(customer.getMaNguoiDung(),kind,id(kind),v.source().fingerprint(),v.current());}
    HopDong create(String kind){agreed(kind);var p=contracts.previewElectronic(customer.getMaNguoiDung(),kind,id(kind),new ElectronicContractForm());return contracts.createElectronic(customer.getMaNguoiDung(),kind,id(kind),p.plan().current(),p.body());}
    HopDong fresh(Long id){return tx.execute(s->db.get(HopDong.class,id));}
    void confirm(NguoiDung user,HopDong h){contracts.confirmElectronic(user.getMaNguoiDung(),h.getMaHopDong(),h.getPhienBan(),h.getDauKiemNoiDung(),CUSTOMER_SIGNATURE);}
    HopDong active(String kind){var h=create(kind);confirm(customer,h);confirm(expert,h);return fresh(h.getMaHopDong());}
    List<MocNghiemThu> ms(HopDong h){return contracts.milestones(h.getMaHopDong());}
    void fund(MocNghiemThu m){payments.fund(customer.getMaNguoiDung(),m.getMaMocNghiemThu(),"SUCCESS");}
    void deliver(MocNghiemThu m){delivery.deliver(expert.getMaNguoiDung(),m.getMaMocNghiemThu(),"Sản phẩm đúng phạm vi bàn giao.","https://example.test/product");}
    void money(BigDecimal expected,BigDecimal actual){assertEquals(0,expected.compareTo(actual));}

    @ParameterizedTest
    @ValueSource(strings = {"thue", "de-xuat"})
    void serviceContractAndTwoMilestonesWorkThroughHtmlForms(String kind) throws Exception {
        var plan = form();
        plan.getItems().getFirst().setRevisions("1");
        send(kind, expert.getMaNguoiDung(), plan);
        var waiting = view(kind);
        plans.accept(customer.getMaNguoiDung(), kind, id(kind), waiting.source().fingerprint(), waiting.current());

        String draftUrl = path(kind) + "/hop-dong", finalAction = "/hop-dong-dien-tu/xac-nhan";
        String hireUrl = "/de-nghi-thue/" + hire;
        if ("thue".equals(kind)) {
            assertTrue(html(getAs(hireUrl, customer)).contains("href=\"" + draftUrl + "\""));
            assertFalse(html(getAs(hireUrl, expert)).contains("href=\"" + draftUrl + "\""));
        }
        var draft = getAs(draftUrl, customer);
        assertTrue(html(draft).contains(customer.getEmail()));
        assertTrue(html(draft).contains(expert.getEmail()));
        assertTrue(html(draft).contains("Đơn vị vận hành chưa công bố số điện thoại."));
        var contractForm = fields(draft, draftUrl + "/ra-soat");
        String rights = "Khách hàng được dùng bản bàn giao để vận hành website của mình.";
        contractForm.set("usageRights", rights);
        contractForm.set("reviewed", "true");
        var preview = postAs(draftUrl + "/ra-soat", customer, contractForm);
        assertTrue(html(preview).contains("Hợp đồng cung ứng dịch vụ"));
        assertTrue(html(preview).contains(rights));
        String url = postAs(finalAction, customer, fields(preview, finalAction)).getResponse().getRedirectedUrl();
        assertNotNull(url);
        assertTrue(url.startsWith("/hop-dong/"));
        Long hid = Long.valueOf(url.substring(url.lastIndexOf('/') + 1));
        var contract = fresh(hid);
        var storedBody = contracts.electronic(expert.getMaNguoiDung(), hid, 1).document().body();
        assertEquals(rights, storedBody.serviceTerms().usageRights());
        assertEquals("", storedBody.intermediary().phone());
        Long first = ms(contract).get(0).getMaMocNghiemThu(), second = ms(contract).get(1).getMaMocNghiemThu();
        if ("thue".equals(kind)) {
            for (var actor : List.of(customer, expert)) {
                assertTrue(html(getAs(hireUrl, actor)).contains("href=\"" + url + "\""));
            }
        }
        var signingSessions = Map.of(customer, new MockHttpSession(), expert, new MockHttpSession());
        var signingForms = new HashMap<NguoiDung, MultiValueMap<String, String>>();
        for (var actor : List.of(customer, expert)) {
            var confirmation = mvc.perform(get(url + "/xac-nhan-dien-tu?phienBan=1")
                    .session(signingSessions.get(actor)).with(user(actor.getEmail()))).andReturn();
            assertTrue(html(confirmation).contains(rights));
            assertEquals(1, html(confirmation).split("<canvas", -1).length - 1);
            assertTrue(html(confirmation).contains("Ô ký của " + actor.getHoTen()));
            var signatureForm = fields(confirmation, finalAction);
            var missingSignature = submitSignature(finalAction, actor, signingSessions.get(actor), signatureForm);
            assertTrue(html(missingSignature).contains("Hãy ký tên trong ô chữ ký"));
            signatureForm.set("signature", "500,20");
            assertTrue(html(submitSignature(finalAction, actor, signingSessions.get(actor), signatureForm)).contains("Chữ ký không hợp lệ"));
            assertEquals("CHO_XAC_NHAN", fresh(hid).getTrangThai());
            assertNull(contracts.electronic(actor.getMaNguoiDung(), hid, 1).signedBy(actor.getMaNguoiDung()));
            signatureForm.set("signature", actor == customer ? CUSTOMER_SIGNATURE : EXPERT_SIGNATURE);
            signingForms.put(actor, signatureForm);
        }
        assertEquals(url, submitSignature(finalAction, expert, signingSessions.get(expert), signingForms.get(expert)).getResponse().getRedirectedUrl());
        assertEquals("CHO_XAC_NHAN", fresh(hid).getTrangThai());
        assertEquals(url, submitSignature(finalAction, customer, signingSessions.get(customer), signingForms.get(customer)).getResponse().getRedirectedUrl());
        assertEquals("DANG_THUC_HIEN", fresh(hid).getTrangThai());
        var signed = getAs(url, customer);
        assertEquals(2, html(signed).split("data:image/png;base64,", -1).length - 1);
        var history = getAs(url + "/phien-ban/1", expert);
        assertEquals(2, html(history).split("data:image/png;base64,", -1).length - 1);
        capture("signed-paper-" + kind, signed);
        assertNull(fresh(hid).getThoiDiemBatDau());
        assertNotNull(milestonePost(second, "ky-quy", "ketQua", "SUCCESS").getFlashMap().get("error"));
        money(BigDecimal.ZERO, payments.balance(second).nap());
        assertEquals(url, milestonePost(first, "ky-quy", "ketQua", "FAIL").getResponse().getRedirectedUrl());
        assertNull(fresh(hid).getThoiDiemBatDau());
        assertEquals(url, milestonePost(first, "ky-quy", "ketQua", "SUCCESS").getResponse().getRedirectedUrl());
        assertNotNull(fresh(hid).getThoiDiemBatDau());

        deliverThroughForm(first, url);
        assertEquals(1, delivery.latest(first).getLanBanGiao());
        var revise = fields(getAs(url, customer), "/moc/" + first + "/sua");
        revise.set("lyDo", "Bổ sung tài liệu cho bản bàn giao của mốc đầu.");
        assertEquals(url, postAs("/moc/" + first + "/sua", customer, revise).getResponse().getRedirectedUrl());
        assertEquals("YEU_CAU_CHINH_SUA", ms(contract).getFirst().getTrangThai());
        deliverThroughForm(first, url);
        assertEquals(2, delivery.latest(first).getLanBanGiao());
        assertEquals(url, milestonePost(first, "nghiem-thu", "ketQua", "FAIL").getResponse().getRedirectedUrl());
        assertEquals("DA_NGHIEM_THU", ms(contract).getFirst().getTrangThai());
        money(new BigDecimal("2000000"), payments.balance(first).dangGiu());
        assertTrue(html(getAs(url, customer)).contains("Thử giải ngân lại"));
        assertEquals(url, milestonePost(first, "giai-ngan", "ketQua", "SUCCESS").getResponse().getRedirectedUrl());
        assertEquals("DA_TAT_TOAN", ms(contract).getFirst().getTrangThai());
        money(new BigDecimal("1800000"), payments.balance(first).thucNhan());

        assertEquals(url, milestonePost(second, "ky-quy", "ketQua", "SUCCESS").getResponse().getRedirectedUrl());
        deliverThroughForm(second, url);
        assertEquals(url, milestonePost(second, "nghiem-thu", "ketQua", "SUCCESS").getResponse().getRedirectedUrl());
        assertEquals("HOAN_THANH", fresh(hid).getTrangThai());
        money(BigDecimal.ZERO, payments.balance(first).dangGiu());
        money(BigDecimal.ZERO, payments.balance(second).dangGiu());
    }

    private MvcResult submitSignature(String action, NguoiDung actor, MockHttpSession browserSession,
                                      MultiValueMap<String, String> data) throws Exception {
        return mvc.perform(browser(post(action).session(browserSession).with(user(actor.getEmail()))
                .params(data).with(csrf()))).andReturn();
    }

    private MvcResult milestonePost(Long id, String action, String key, String value) throws Exception {
        var data = new LinkedMultiValueMap<String, String>();
        data.set(key, value);
        return postAs("/moc/" + id + "/" + action, customer, data);
    }

    private void deliverThroughForm(Long milestone, String contractUrl) throws Exception {
        String action = "/moc/" + milestone + "/ban-giao";
        var data = fields(getAs(contractUrl, expert), action);
        data.set("moTa", "Bàn giao sản phẩm và tài liệu theo thỏa thuận.");
        data.set("duongDan", "https://example.test/sang-connect-deliverable");
        var delivered = mvc.perform(multipart(action).session(session).with(user(expert.getEmail()))
                .params(data).with(csrf())).andReturn();
        assertEquals(contractUrl, delivered.getResponse().getRedirectedUrl());
    }
    @ParameterizedTest @ValueSource(strings={"thue","de-xuat"}) void htmlReviewFinalCreateConfirmAndBothPartiesSeeSameContract(String kind)throws Exception{
        agreed(kind);long headers=count("HopDong"),funds=count("GiaoDich");String path=path(kind)+"/hop-dong";
        var first=getAs(path,customer);assertEquals("electronic-form",first.getModelAndView().getViewName());capture("create-"+kind,first);var data=fields(first,path+"/ra-soat");data.set("reviewed","true");var review=postAs(path+"/ra-soat",customer,data);assertEquals("electronic-review",review.getModelAndView().getViewName());capture("review-"+kind,review);assertEquals(headers,count("HopDong"));assertEquals(funds,count("GiaoDich"));
        var finalData=fields(review,"/hop-dong-dien-tu/xac-nhan");assertEquals(Set.of("token","_csrf"),finalData.keySet());var result=postAs("/hop-dong-dien-tu/xac-nhan",customer,finalData);assertEquals(302,result.getResponse().getStatus());String url=result.getResponse().getRedirectedUrl();Long hid=Long.valueOf(url.substring(url.lastIndexOf('/')+1));assertEquals(url,postAs("/hop-dong-dien-tu/xac-nhan",customer,finalData).getResponse().getRedirectedUrl());assertEquals(headers+1,count("HopDong"));assertEquals(funds,count("GiaoDich"));var h=fresh(hid);assertNull(h.getPhienBanKhachXacNhan());assertNull(h.getPhienBanChuyenGiaXacNhan());assertNull(h.getThoiDiemBatDau());assertEquals(2,ms(h).size());assertTrue(ms(h).stream().allMatch(m->m.getHanBanGiao()==null));assertEquals(view(kind).planId(),h.getMaKeHoach());if(kind.equals("thue")){assertNull(h.getMaYeuCau());assertNull(h.getMaDeXuat());}
        for(var user:List.of(customer,expert)){var page=getAs(url,user);assertEquals(200,page.getResponse().getStatus());capture("contract-pending-"+kind+"-"+(user==customer?"customer":"expert"),page);assertFalse(html(page).contains("Sửa mốc nháp"));assertTrue(html(getAs("/bang-dieu-khien",user)).contains(url));var confirm=getAs(url+"/xac-nhan-dien-tu?phienBan=1",user);assertEquals("electronic-review",confirm.getModelAndView().getViewName());capture("confirm-"+kind,confirm);var confirmData=fields(confirm,"/hop-dong-dien-tu/xac-nhan");confirmData.set("signature",CUSTOMER_SIGNATURE);assertEquals(url,postAs("/hop-dong-dien-tu/xac-nhan",user,confirmData).getResponse().getRedirectedUrl());assertEquals(url,postAs("/hop-dong-dien-tu/xac-nhan",user,confirmData).getResponse().getRedirectedUrl());}
        h=fresh(hid);assertEquals("DANG_THUC_HIEN",h.getTrangThai());assertNull(h.getThoiDiemBatDau());assertEquals(2,contracts.electronic(customer.getMaNguoiDung(),hid,0).confirmations().size());assertEquals(hid,view(kind).contractId());assertFalse(view(kind).canEdit());capture("contract-ready-"+kind,getAs(url,customer));assertTrue(html(getAs(path(kind),customer)).contains("Mở hợp đồng"));assertThrows(BusinessException.class,()->plans.cancel(customer.getMaNguoiDung(),kind,id(kind),view(kind).source().fingerprint(),1,"Không được hủy sau khi đã lập hợp đồng."));
    }
    @Test void newContractsCannotBypassAgreedPlansOrExistingMilestones(){assertThrows(BusinessException.class,()->contracts.select(customer.getMaNguoiDung(),proposal));assertThrows(BusinessException.class,()->contracts.previewElectronic(customer.getMaNguoiDung(),"thue",hire,new ElectronicContractForm()));send("thue",expert.getMaNguoiDung(),form());assertThrows(BusinessException.class,()->contracts.previewElectronic(customer.getMaNguoiDung(),"thue",hire,new ElectronicContractForm()));var v=view("thue");plans.accept(customer.getMaNguoiDung(),"thue",hire,v.source().fingerprint(),1);var p=contracts.previewElectronic(customer.getMaNguoiDung(),"thue",hire,new ElectronicContractForm());var h=contracts.createElectronic(customer.getMaNguoiDung(),"thue",hire,1,p.body());assertThrows(BusinessException.class,()->contracts.terms(customer.getMaNguoiDung(),h.getMaHopDong(),"Nội dung giả mạo muốn đổi phạm vi.",BigDecimal.ONE));assertThrows(BusinessException.class,()->contracts.removeMilestone(customer.getMaNguoiDung(),h.getMaHopDong(),ms(h).get(0).getMaMocNghiemThu()));assertThrows(BusinessException.class,()->contracts.confirm(customer.getMaNguoiDung(),h.getMaHopDong(),1));}
    @ParameterizedTest @ValueSource(strings={"id","customer","expert","maKeHoach","version","state","rate","total","laQuanTriVien","dauKiemNoiDung"}) void strictHtmlBindingBlocksManagedFields(String field)throws Exception{agreed("thue");String path=path("thue")+"/hop-dong";var data=fields(getAs(path,customer),path+"/ra-soat");data.set("reviewed","true");data.set(field,"1");var r=postAs(path+"/ra-soat",customer,data);assertEquals("electronic-form",r.getModelAndView().getViewName());assertTrue(html(r).contains("Biểu mẫu chứa trường không được phép cập nhật."));assertNull(view("thue").contractId());}
    @Test void finalTicketIsCsrfProtectedCannotChangeContentOrCrossActorAndExpires()throws Exception{agreed("thue");String path=path("thue")+"/hop-dong";var data=fields(getAs(path,customer),path+"/ra-soat");data.set("reviewed","true");var r=postAs(path+"/ra-soat",customer,data);var finalData=fields(r,"/hop-dong-dien-tu/xac-nhan");finalData.set("_csrf","invalid");assertEquals(403,mvc.perform(post("/hop-dong-dien-tu/xac-nhan").session(session).with(user(customer.getEmail())).params(finalData)).andReturn().getResponse().getStatus());assertEquals(403,postAs("/hop-dong-dien-tu/xac-nhan",expert,finalData).getResponse().getStatus());finalData.set("state","DANG_THUC_HIEN");assertEquals("electronic-error",postAs("/hop-dong-dien-tu/xac-nhan",customer,finalData).getModelAndView().getViewName());finalData.remove("state");when(clock.instant()).thenReturn(Instant.parse("2026-10-28T05:00:00Z"));var expired=postAs("/hop-dong-dien-tu/xac-nhan",customer,finalData);assertTrue(html(expired).contains("hết hạn"));assertNull(view("thue").contractId());}
    @Test void profileChangesAfterReviewRequireNewReviewAndSnapshotsNeverDecryptIdentity(){tx.executeWithoutResult(s->{for(var user:List.of(customer,expert)){var p=new HoSoKhachHang();p.maNguoiDung=user.getMaNguoiDung();p.cccdBonSoCuoi=user==customer?"0123":"6789";p.soCccdMaHoa="SYNTHETIC_CIPHERTEXT_MUST_NOT_LEAK";p.soDienThoai="0906248167";db.save(p);}});agreed("thue");var p=contracts.previewElectronic(customer.getMaNguoiDung(),"thue",hire,new ElectronicContractForm());assertEquals("********0123",p.body().customer().maskedIdentity());assertEquals("********6789",p.body().expert().maskedIdentity());assertEquals("Mai Phước Sang",p.body().intermediary().operator());assertFalse(snapshots.json(p.body()).contains("SYNTHETIC_CIPHERTEXT"));tx.executeWithoutResult(s->db.get(NguoiDung.class,customer.getMaNguoiDung()).setHoTen("Tên hồ sơ cập nhật sau rà soát"));assertThrows(BusinessException.class,()->contracts.createElectronic(customer.getMaNguoiDung(),"thue",hire,1,p.body()));var next=contracts.previewElectronic(customer.getMaNguoiDung(),"thue",hire,new ElectronicContractForm());var h=contracts.createElectronic(customer.getMaNguoiDung(),"thue",hire,1,next.body());tx.executeWithoutResult(s->db.get(NguoiDung.class,expert.getMaNguoiDung()).setHoTen("Tên chuyên gia sau khi lập hợp đồng"));assertEquals(next.body(),contracts.electronic(customer.getMaNguoiDung(),h.getMaHopDong(),0).document().body());}
    @Test void clausesRevisionResetsConsentKeepsPreviousProofsAndFreezesPlan()throws Exception{var h=create("thue");confirm(expert,h);var old=contracts.electronic(customer.getMaNguoiDung(),h.getMaHopDong(),0);var form=snapshots.form(old.document().body());form.setAdditional("Hai bên sử dụng kênh trao đổi trên nền tảng.");String editPath="/hop-dong/"+h.getMaHopDong()+"/dieu-khoan";var edit=getAs(editPath,customer);capture("amend",edit);var data=fields(edit,editPath+"/ra-soat");data.set("additional",form.getAdditional());data.set("reviewed","true");var reviewed=postAs(editPath+"/ra-soat",customer,data);assertEquals("electronic-review",reviewed.getModelAndView().getViewName());capture("amend-review",reviewed);assertEquals(1,fresh(h.getMaHopDong()).getPhienBan());assertEquals(302,postAs("/hop-dong-dien-tu/xac-nhan",customer,fields(reviewed,"/hop-dong-dien-tu/xac-nhan")).getResponse().getStatus());var next=fresh(h.getMaHopDong());assertEquals(2,next.getPhienBan());assertNull(next.getPhienBanChuyenGiaXacNhan());assertEquals(1,contracts.electronic(expert.getMaNguoiDung(),h.getMaHopDong(),1).confirmations().size());assertThrows(BusinessException.class,()->confirm(customer,h));assertEquals(old.document().body().milestones(),contracts.electronic(expert.getMaNguoiDung(),h.getMaHopDong(),0).document().body().milestones());assertThrows(BusinessException.class,()->payments.fund(customer.getMaNguoiDung(),ms(next).get(0).getMaMocNghiemThu(),"SUCCESS"));confirm(customer,next);confirm(expert,next);assertEquals("DANG_THUC_HIEN",fresh(h.getMaHopDong()).getTrangThai());capture("history",getAs("/hop-dong/"+h.getMaHopDong()+"/phien-ban/1",expert));}
    @Test void ownConfirmationsAndAdminReadOnlyEnforced()throws Exception{var h=create("thue");assertThrows(AccessDeniedException.class,()->confirm(outsider,h));assertThrows(AccessDeniedException.class,()->confirm(admin,h));assertThrows(BusinessException.class,()->contracts.electronic(outsider.getMaNguoiDung(),h.getMaHopDong(),0));assertEquals(200,getAs("/hop-dong/"+h.getMaHopDong(),admin).getResponse().getStatus());assertEquals(403,getAs("/hop-dong/"+h.getMaHopDong()+"/xac-nhan-dien-tu?phienBan=1",admin).getResponse().getStatus());assertThrows(BusinessException.class,()->contracts.confirmElectronic(customer.getMaNguoiDung(),h.getMaHopDong(),1,"0".repeat(64),CUSTOMER_SIGNATURE));}
    @Test void failedFundingDoesNotStartAndFirstSuccessSetsExactStableDates()throws Exception{var h=active("thue");var list=ms(h);payments.fund(customer.getMaNguoiDung(),list.get(0).getMaMocNghiemThu(),"FAIL");assertNull(fresh(h.getMaHopDong()).getThoiDiemBatDau());assertTrue(ms(h).stream().allMatch(m->m.getHanBanGiao()==null));fund(list.get(0));var start=fresh(h.getMaHopDong()).getThoiDiemBatDau();assertEquals(Instant.parse("2026-10-28T04:00:00Z"),start.toInstant());var dates=ms(h).stream().map(MocNghiemThu::getHanBanGiao).toList();assertEquals(LocalDateTime.parse("2026-11-12T16:59:00"),dates.get(0));assertEquals(LocalDateTime.parse("2026-12-07T16:59:00"),dates.get(1));when(clock.instant()).thenReturn(Instant.parse("2026-12-30T04:00:00Z"));fund(list.get(0));assertEquals(start,fresh(h.getMaHopDong()).getThoiDiemBatDau());assertEquals(dates,ms(h).stream().map(MocNghiemThu::getHanBanGiao).toList());delivery.autoAccept(list.get(0).getMaMocNghiemThu());money(new BigDecimal("2000000"),payments.balance(list.get(0).getMaMocNghiemThu()).dangGiu());capture("contract-funded",getAs("/hop-dong/"+h.getMaHopDong(),expert));}
    @Test void monthlyEndOfMonthUsesCalendarAndLastExactDays(){var f=form();f.setMode("MONTH");f.getItems().get(0).setDue("1");f.getItems().get(1).setDue("2");send("thue",expert.getMaNguoiDung(),f);var v=view("thue");plans.accept(customer.getMaNguoiDung(),"thue",hire,v.source().fingerprint(),1);var p=contracts.previewElectronic(customer.getMaNguoiDung(),"thue",hire,new ElectronicContractForm());var h=contracts.createElectronic(customer.getMaNguoiDung(),"thue",hire,1,p.body());confirm(customer,h);confirm(expert,h);when(clock.instant()).thenReturn(Instant.parse("2028-01-31T04:00:00Z"));fund(ms(h).get(0));assertEquals(LocalDate.of(2028,2,29),ms(h).get(0).getHanBanGiao().toLocalDate());assertEquals(LocalDate.of(2028,3,11),ms(h).get(1).getHanBanGiao().toLocalDate());}
    @Test void repeatedConcurrentCreatesAndConfirmationsProduceOneContractAndOwnProofs()throws Exception{agreed("de-xuat");var p=contracts.previewElectronic(customer.getMaNguoiDung(),"de-xuat",proposal,new ElectronicContractForm());var pool=Executors.newFixedThreadPool(2);try{Callable<Long> create=()->contracts.createElectronic(customer.getMaNguoiDung(),"de-xuat",proposal,1,p.body()).getMaHopDong();var a=pool.submit(create);var b=pool.submit(create);assertEquals(a.get(45,TimeUnit.SECONDS),b.get(45,TimeUnit.SECONDS));var h=fresh(a.get());var one=pool.submit(()->confirm(customer,h));var two=pool.submit(()->confirm(expert,h));one.get(45,TimeUnit.SECONDS);two.get(45,TimeUnit.SECONDS);assertEquals(2,contracts.electronic(customer.getMaNguoiDung(),h.getMaHopDong(),0).confirmations().size());assertEquals("DANG_THUC_HIEN",fresh(h.getMaHopDong()).getTrangThai());Long mid=ms(h).get(0).getMaMocNghiemThu();Callable<Long> funding=()->payments.fund(customer.getMaNguoiDung(),mid,"SUCCESS").getMaGiaoDich();var fundA=pool.submit(funding);var fundB=pool.submit(funding);assertEquals(fundA.get(45,TimeUnit.SECONDS),fundB.get(45,TimeUnit.SECONDS));money(new BigDecimal("2000000"),payments.balance(mid).nap());assertNotNull(fresh(h.getMaHopDong()).getThoiDiemBatDau());}finally{pool.shutdownNow();}}
    @Test void revisionsCappedAndSuccessfulSettlementUsesExistingPaymentAndReview(){var h=active("thue");var m=ms(h).get(0);fund(m);deliver(m);assertThrows(BusinessException.class,()->delivery.revise(customer.getMaNguoiDung(),m.getMaMocNghiemThu(),"Yêu cầu ngoài số lần đã thống nhất."));assertFalse(delivery.accept(customer.getMaNguoiDung(),m.getMaMocNghiemThu(),"FAIL"));assertTrue(payments.release(customer.getMaNguoiDung(),m.getMaMocNghiemThu(),"SUCCESS"));money(new BigDecimal("200000"),payments.balance(m.getMaMocNghiemThu()).phi());var second=ms(h).get(1);fund(second);deliver(second);assertTrue(delivery.accept(customer.getMaNguoiDung(),second.getMaMocNghiemThu(),"SUCCESS"));assertEquals("HOAN_THANH",fresh(h.getMaHopDong()).getTrangThai());reviews.submit(customer.getMaNguoiDung(),h.getMaHopDong(),5,"Đánh giá kiểm thử đúng thời hạn.");assertTrue(reviews.publicReviews(h.getMaHopDong()).isEmpty());reviews.submit(expert.getMaNguoiDung(),h.getMaHopDong(),4,"Đánh giá phía chuyên gia kiểm thử.");assertEquals(2,reviews.publicReviews(h.getMaHopDong()).size());}
    @ParameterizedTest @ValueSource(ints={0,40,100}) void terminationConservesHeldMoneyAndRetriesOnlyUnpaidLeg(int percent){var h=active("thue");var m=ms(h).get(0);fund(m);var y=term.propose(customer.getMaNguoiDung(),h.getMaHopDong(),"Hai bên kết thúc sau phần việc hiện tại.",percent);term.respond(expert.getMaNguoiDung(),y.getMaYeuCauChamDut(),true,"FAIL_REFUND");term.respond(expert.getMaNguoiDung(),y.getMaYeuCauChamDut(),true,"SUCCESS");var b=payments.balance(m.getMaMocNghiemThu());money(new BigDecimal("2000000"),b.hoanKhach().add(b.thucNhan()).add(b.phi()));money(new BigDecimal("2000000").multiply(BigDecimal.valueOf(percent)).divide(BigDecimal.valueOf(100)).multiply(new BigDecimal("0.1")).setScale(0,RoundingMode.HALF_UP),b.phi());if(percent==40){money(new BigDecimal("1200000"),b.hoanKhach());money(new BigDecimal("720000"),b.thucNhan());money(new BigDecimal("80000"),b.phi());}money(BigDecimal.ZERO,b.dangGiu());assertEquals("DA_CHAM_DUT",fresh(h.getMaHopDong()).getTrangThai());assertEquals("DA_HUY",ms(h).get(1).getTrangThai());}
    @Test void disputeBlocksReleaseAndResumeRequiresNewVersionConsent(){var h=active("thue");var m=ms(h).get(0);fund(m);deliver(m);var t=disputes.open(customer.getMaNguoiDung(),m.getMaMocNghiemThu(),"Cần xử lý bất đồng theo bằng chứng.");when(clock.instant()).thenReturn(Instant.parse("2026-11-20T04:00:00Z"));delivery.autoAccept(m.getMaMocNghiemThu());money(new BigDecimal("2000000"),payments.balance(m.getMaMocNghiemThu()).dangGiu());assertTrue(disputes.decide(admin.getMaNguoiDung(),t.getMaTranhChap(),"Hoàn tiền mốc và cho hai bên xác nhận tiếp tục.",BigDecimal.ZERO,true,"SUCCESS"));var next=fresh(h.getMaHopDong());assertEquals(2,next.getPhienBan());assertEquals("CHO_TIEP_TUC",next.getTrangThai());assertThrows(BusinessException.class,()->fund(ms(next).get(1)));confirm(customer,next);confirm(expert,next);fund(ms(next).get(1));money(new BigDecimal("3000000"),payments.balance(ms(next).get(1).getMaMocNghiemThu()).dangGiu());}
    @Test void cancellationRequiresReviewedTicketAndCreatesNoMoney()throws Exception{var h=create("thue");var first=getAs("/hop-dong/"+h.getMaHopDong()+"/huy-dien-tu",expert);capture("cancel",first);String post="/hop-dong/"+h.getMaHopDong()+"/huy-dien-tu/ra-soat";var data=fields(first,post);data.set("reason","Hai bên không tiếp tục trước hiệu lực.");var review=postAs(post,expert,data);assertEquals("CHO_XAC_NHAN",fresh(h.getMaHopDong()).getTrangThai());assertEquals("electronic-review",review.getModelAndView().getViewName());capture("cancel-review",review);var finish=fields(review,"/hop-dong-dien-tu/xac-nhan");assertEquals(302,postAs("/hop-dong-dien-tu/xac-nhan",expert,finish).getResponse().getStatus());assertEquals("DA_HUY",fresh(h.getMaHopDong()).getTrangThai());}

    @Test void obligationsMoveToContractAndClosingOneFlowKeepsOtherOpenWork(){var h=create("thue");assertTrue(tx.<Boolean>execute(s->obligations.outstanding(customer.getMaNguoiDung(),true)));assertTrue(tx.<Boolean>execute(s->obligations.outstanding(expert.getMaNguoiDung(),false)));contracts.cancelElectronic(customer.getMaNguoiDung(),h.getMaHopDong(),1,h.getDauKiemNoiDung(),"Dừng hợp đồng trước khi phát sinh nghĩa vụ tiền.");assertTrue(tx.<Boolean>execute(s->obligations.outstanding(expert.getMaNguoiDung(),false)));var proposalPlan=view("de-xuat");plans.cancel(customer.getMaNguoiDung(),"de-xuat",proposal,proposalPlan.source().fingerprint(),0,"Không tiếp tục kế hoạch của đề xuất.");assertFalse(tx.<Boolean>execute(s->obligations.outstanding(expert.getMaNguoiDung(),false)));assertTrue(tx.<Boolean>execute(s->obligations.outstanding(customer.getMaNguoiDung(),true)));}
    @Test void finalConsentAndCancelRaceOnlyOneDecision()throws Exception{var h=create("thue");confirm(customer,h);var pool=Executors.newFixedThreadPool(2);var gate=new CountDownLatch(1);try{Callable<Boolean> accept=()->{gate.await();try{confirm(expert,h);return true;}catch(BusinessException expected){return false;}};Callable<Boolean> cancel=()->{gate.await();try{contracts.cancelElectronic(customer.getMaNguoiDung(),h.getMaHopDong(),1,h.getDauKiemNoiDung(),"Hủy trước khi hợp đồng có hiệu lực.");return true;}catch(BusinessException expected){return false;}};var a=pool.submit(accept);var b=pool.submit(cancel);gate.countDown();assertNotEquals(a.get(45,TimeUnit.SECONDS),b.get(45,TimeUnit.SECONDS));assertTrue(Set.of("DANG_THUC_HIEN","DA_HUY").contains(fresh(h.getMaHopDong()).getTrangThai()));}finally{pool.shutdownNow();}}
    @Test void boundedRevisionsRestartExistingAcceptanceWindowAndNoCalendarPayout(){var f=form();f.getItems().get(0).setRevisions("1");send("thue",expert.getMaNguoiDung(),f);var v=view("thue");plans.accept(customer.getMaNguoiDung(),"thue",hire,v.source().fingerprint(),1);var p=contracts.previewElectronic(customer.getMaNguoiDung(),"thue",hire,new ElectronicContractForm());var h=contracts.createElectronic(customer.getMaNguoiDung(),"thue",hire,1,p.body());confirm(customer,h);confirm(expert,h);var m=ms(h).get(0);fund(m);deliver(m);delivery.revise(customer.getMaNguoiDung(),m.getMaMocNghiemThu(),"Chỉnh sửa lần đầu trong phạm vi đã thống nhất.");deliver(m);assertThrows(BusinessException.class,()->delivery.revise(customer.getMaNguoiDung(),m.getMaMocNghiemThu(),"Không được yêu cầu lần hai quá giới hạn.") );when(clock.instant()).thenReturn(Instant.parse("2026-11-11T04:00:00Z"));delivery.autoAccept(m.getMaMocNghiemThu());assertEquals("DA_TAT_TOAN",ms(h).get(0).getTrangThai());money(new BigDecimal("1800000"),payments.balance(m.getMaMocNghiemThu()).thucNhan());}
}
