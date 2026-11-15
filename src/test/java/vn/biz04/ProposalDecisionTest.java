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
import java.util.*;
import java.util.concurrent.*;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.hamcrest.Matchers.containsString;

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:decisions;MODE=MSSQLServer;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false")
@AutoConfigureMockMvc @ActiveProfiles("test")
class ProposalDecisionTest {
    @Autowired Store db;@Autowired TransactionTemplate tx;@Autowired CatalogService catalog;@Autowired ProposalService proposals;@Autowired ContractService contracts;@Autowired MockMvc mvc;
    NguoiDung a,b,c,d,admin;YeuCau request;DeXuat offer,other;String prefix;
    @BeforeEach void setup(){prefix=UUID.randomUUID().toString();tx.executeWithoutResult(s->{a=TestAccounts.create(db,"Khách hàng A "+prefix,prefix+"a@test.local","Demo@12345",true,false);b=TestAccounts.create(db,"Chuyên gia B "+prefix,prefix+"b@test.local","Demo@12345",false,true);c=TestAccounts.create(db,"Khách hàng C "+prefix,prefix+"c@test.local","Demo@12345",true,false);d=TestAccounts.create(db,"Chuyên gia D "+prefix,prefix+"d@test.local","Demo@12345",false,true);admin=TestAccounts.create(db,"Quản trị",prefix+"admin@test.local","Demo@12345",false,false);admin.setLaQuanTriVien(true);});request=catalog.request(a.getMaNguoiDung(),null,"Thiết kế nội dung dự án "+prefix,"Nhu cầu triển khai nội dung được mô tả đầy đủ.",BigDecimal.valueOf(3000000),null);offer=catalog.proposal(b.getMaNguoiDung(),request.getMaYeuCau(),"Phương án thực hiện của chuyên gia B theo yêu cầu.",BigDecimal.valueOf(2500000),14);other=catalog.proposal(d.getMaNguoiDung(),request.getMaYeuCau(),"Phương án thực hiện của chuyên gia D theo yêu cầu.",BigDecimal.valueOf(2800000),20);}
    String state(Long id){return tx.execute(s->db.get(DeXuat.class,id).getTrangThai());}
    long contractsCount(){return tx.execute(s->db.count("select count(h) from HopDong h where h.maYeuCau=:y","y",request.getMaYeuCau()));}
    @Test void existingOffersAreReceivedThroughRequestOwnerWithoutResubmission()throws Exception{
        assertEquals(2,proposals.received(a.getMaNguoiDung()).size());assertEquals(0,proposals.received(b.getMaNguoiDung()).size());assertEquals(1,proposals.sent(b.getMaNguoiDung()).size());assertNotNull(proposals.sent(b.getMaNguoiDung()).get(0).submitted());
        var html=mvc.perform(get("/yeu-cau").with(user(a.getEmail()))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();assertTrue(html.contains("Đề xuất nhận được: 2"));assertTrue(html.contains("Xem đề xuất"));
        mvc.perform(get("/de-xuat/nhan").with(user(a.getEmail()))).andExpect(status().isOk()).andExpect(content().string(containsString(b.getHoTen())));
        mvc.perform(get("/de-xuat/da-gui").with(user(b.getEmail()))).andExpect(content().string(containsString("Đã đề xuất"))).andExpect(content().string(containsString("proposal-state--sent")));
    }
    @Test void rejectionPersistsAndHistoryCountsRemain()throws Exception{
        assertTrue(proposals.reject(a.getMaNguoiDung(),offer.getMaDeXuat()));assertFalse(proposals.reject(a.getMaNguoiDung(),offer.getMaDeXuat()));assertEquals("BI_TU_CHOI",state(offer.getMaDeXuat()));assertEquals(2,proposals.received(a.getMaNguoiDung()).size());assertEquals(1,proposals.sent(b.getMaNguoiDung()).size());assertEquals(0,contractsCount());
        mvc.perform(get("/de-xuat/"+offer.getMaDeXuat()).with(user(b.getEmail()))).andExpect(status().isOk()).andExpect(content().string(containsString("Đề xuất bị từ chối"))).andExpect(content().string(containsString("proposal-state--rejected")));
        assertThrows(BusinessException.class,()->proposals.accept(a.getMaNguoiDung(),offer.getMaDeXuat()));assertThrows(BusinessException.class,()->contracts.select(a.getMaNguoiDung(),offer.getMaDeXuat()));
        assertEquals(1L,tx.execute(s->db.count("select count(n) from NhatKyXuLy n where n.loaiDoiTuong='DeXuat' and n.maDoiTuong=:d and n.hanhDong='TU_CHOI_DE_XUAT'","d",offer.getMaDeXuat())).longValue());
    }
    @Test void approvalIsDurableBeforeContractAndCanResume()throws Exception{
        assertTrue(proposals.accept(a.getMaNguoiDung(),offer.getMaDeXuat()).changed());assertFalse(proposals.accept(a.getMaNguoiDung(),offer.getMaDeXuat()).changed());assertEquals(0,contractsCount());assertEquals("DA_CHON",state(offer.getMaDeXuat()));assertEquals("DANG_GUI",state(other.getMaDeXuat()));
        mvc.perform(get("/de-xuat/"+offer.getMaDeXuat()).with(user(b.getEmail()))).andExpect(content().string(containsString("Chờ chuyên gia thiết lập mốc")));
        mvc.perform(get("/de-xuat/"+offer.getMaDeXuat()).with(user(a.getEmail()))).andExpect(content().string(containsString("Chờ chuyên gia thiết lập mốc")));
        mvc.perform(get("/de-xuat/"+offer.getMaDeXuat()+"/lap-hop-dong").with(user(a.getEmail()))).andExpect(redirectedUrl("/ke-hoach-moc/de-xuat/"+offer.getMaDeXuat()));assertEquals(0,contractsCount());
        assertThrows(BusinessException.class,()->catalog.closeRequest(a.getMaNguoiDung(),request.getMaYeuCau()));
    }
    @Test void oldCreationPostCannotBypassPlanOrCreateFinances()throws Exception{
        proposals.accept(a.getMaNguoiDung(),offer.getMaDeXuat());
        mvc.perform(post("/de-xuat/"+offer.getMaDeXuat()+"/lap-hop-dong").with(user(a.getEmail())).with(csrf()).param("gia","1").param("khachHang",c.getMaNguoiDung().toString())).andExpect(status().is3xxRedirection());
        assertEquals(0,contractsCount());assertThrows(BusinessException.class,()->contracts.select(a.getMaNguoiDung(),offer.getMaDeXuat()));
    }
    @Test void remainingPendingDeclinedOnlyAtCreationAndWithdrawnHistoryPreserved(){
        catalog.withdraw(d.getMaNguoiDung(),other.getMaDeXuat());proposals.accept(a.getMaNguoiDung(),offer.getMaDeXuat());assertEquals("DA_RUT",state(other.getMaDeXuat()));tx.execute(s->TestContracts.legacy(db,offer.getMaDeXuat()));assertEquals("DA_RUT",state(other.getMaDeXuat()));
        assertEquals("Đã lập hợp đồng",proposals.details(b.getMaNguoiDung(),offer.getMaDeXuat()).label());
    }
    @Test void oldContractRelationTakesPriorityOverProposalStatus(){
        proposals.accept(a.getMaNguoiDung(),offer.getMaDeXuat());var h=tx.execute(s->TestContracts.legacy(db,offer.getMaDeXuat()));contracts.cancel(a.getMaNguoiDung(),h.getMaHopDong());assertEquals("BI_TU_CHOI",state(offer.getMaDeXuat()));var row=proposals.details(b.getMaNguoiDung(),offer.getMaDeXuat());assertEquals("Đã lập hợp đồng",row.label());assertEquals("DA_HUY",row.contractStatus());assertEquals(h.getMaHopDong(),row.contractId());assertFalse(row.canContinue());
    }
    @Test void missingOldSubmissionDateDoesNotRequireSendingAgain(){
        tx.executeWithoutResult(s->{for(NhatKyXuLy n:db.list("from NhatKyXuLy n where n.loaiDoiTuong='DeXuat' and n.maDoiTuong=:d",NhatKyXuLy.class,"d",offer.getMaDeXuat()))db.remove(n);});assertNull(proposals.details(a.getMaNguoiDung(),offer.getMaDeXuat()).submitted());assertTrue(proposals.accept(a.getMaNguoiDung(),offer.getMaDeXuat()).changed());
    }
    @Test void getConfirmationAndReviewNeverChangeDataAndPostsRequireCsrf()throws Exception{
        for(String action:List.of("chap-thuan","tu-choi"))mvc.perform(get("/de-xuat/"+offer.getMaDeXuat()+"/xac-nhan").param("hanhDong",action).with(user(a.getEmail()))).andExpect(status().isOk());
        mvc.perform(get("/de-xuat/"+offer.getMaDeXuat()+"/lap-hop-dong").with(user(a.getEmail()))).andExpect(redirectedUrl("/ke-hoach-moc/de-xuat/"+offer.getMaDeXuat()));assertEquals("DANG_GUI",state(offer.getMaDeXuat()));assertEquals(0,contractsCount());
        for(String action:List.of("chap-thuan","tu-choi","lap-hop-dong"))mvc.perform(post("/de-xuat/"+offer.getMaDeXuat()+"/"+action).with(user(a.getEmail()))).andExpect(status().isForbidden());assertEquals("DANG_GUI",state(offer.getMaDeXuat()));
    }
    @Test void ownershipPrivacyAndAdminReadAccessAreEnforced()throws Exception{
        for(String suffix:List.of("","/lap-hop-dong","/xac-nhan?hanhDong=chap-thuan"))mvc.perform(get("/de-xuat/"+offer.getMaDeXuat()+suffix).with(user(c.getEmail()))).andExpect(status().isForbidden());
        mvc.perform(post("/de-xuat/"+offer.getMaDeXuat()+"/chap-thuan").with(user(c.getEmail())).with(csrf())).andExpect(status().isForbidden());mvc.perform(post("/de-xuat/"+offer.getMaDeXuat()+"/tu-choi").with(user(b.getEmail())).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(get("/de-xuat/"+offer.getMaDeXuat()).with(user(admin.getEmail()))).andExpect(status().isOk());assertThrows(AccessDeniedException.class,()->proposals.accept(admin.getMaNguoiDung(),offer.getMaDeXuat()));
        String expertHtml=mvc.perform(get("/yeu-cau/"+request.getMaYeuCau()).with(user(b.getEmail()))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();assertTrue(expertHtml.contains(offer.getPhuongAn()));assertFalse(expertHtml.contains(other.getPhuongAn()));assertFalse(expertHtml.contains(a.getEmail()));
    }
    @Test void withdrawnAndRejectedOffersCannotBeAcceptedAndResubmitPolicyRemains(){
        catalog.withdraw(b.getMaNguoiDung(),offer.getMaDeXuat());assertThrows(BusinessException.class,()->proposals.accept(a.getMaNguoiDung(),offer.getMaDeXuat()));catalog.proposal(b.getMaNguoiDung(),request.getMaYeuCau(),"Gửi lại theo chính sách hiện có",BigDecimal.valueOf(2500000),14);proposals.reject(a.getMaNguoiDung(),offer.getMaDeXuat());assertThrows(BusinessException.class,()->proposals.accept(a.getMaNguoiDung(),offer.getMaDeXuat()));assertEquals(offer.getMaDeXuat(),catalog.proposal(b.getMaNguoiDung(),request.getMaYeuCau(),"Gửi lại theo chính sách hiện có",BigDecimal.valueOf(2500000),14).getMaDeXuat());
    }
    @Test void twoRoleStatisticsKeepSentAndReceivedSeparate()throws Exception{
        tx.executeWithoutResult(s->db.get(NguoiDung.class,a.getMaNguoiDung()).setLaChuyenGia(true));var theirs=catalog.request(c.getMaNguoiDung(),null,"Yêu cầu khác cho thống kê","Nội dung kiểm tra thống kê gửi nhận độc lập",BigDecimal.valueOf(3000000),null);catalog.proposal(a.getMaNguoiDung(),theirs.getMaYeuCau(),"Đề xuất do A gửi sang C",BigDecimal.valueOf(1000000),5);
        assertEquals(2,proposals.received(a.getMaNguoiDung()).size());assertEquals(1,proposals.sent(a.getMaNguoiDung()).size());mvc.perform(get("/bang-dieu-khien").with(user(a.getEmail()))).andExpect(status().isOk()).andExpect(model().attributeExists("receivedProposals","proposals"));
    }
    private List<Boolean> race(Callable<Boolean> first,Callable<Boolean> second)throws Exception{ExecutorService pool=Executors.newFixedThreadPool(2);CountDownLatch gate=new CountDownLatch(1);try{var one=pool.submit(()->{gate.await();return first.call();});var two=pool.submit(()->{gate.await();return second.call();});gate.countDown();return List.of(one.get(30,TimeUnit.SECONDS),two.get(30,TimeUnit.SECONDS));}finally{pool.shutdownNow();}}
    @Test void acceptAndRejectRaceHasOneDecision()throws Exception{
        var outcomes=race(()->{try{return proposals.accept(a.getMaNguoiDung(),offer.getMaDeXuat()).changed();}catch(BusinessException ex){return false;}},()->{try{return proposals.reject(a.getMaNguoiDung(),offer.getMaDeXuat());}catch(BusinessException ex){return false;}});assertNotEquals(outcomes.get(0),outcomes.get(1));assertTrue(Set.of("DA_CHON","BI_TU_CHOI").contains(state(offer.getMaDeXuat())));assertEquals(0,contractsCount());
    }
    @Test void acceptAndWithdrawRaceHasOneTransition()throws Exception{
        var outcomes=race(()->{try{return proposals.accept(a.getMaNguoiDung(),offer.getMaDeXuat()).changed();}catch(BusinessException ex){return false;}},()->{try{catalog.withdraw(b.getMaNguoiDung(),offer.getMaDeXuat());return true;}catch(BusinessException ex){return false;}});assertNotEquals(outcomes.get(0),outcomes.get(1));assertTrue(Set.of("DA_CHON","DA_RUT").contains(state(offer.getMaDeXuat())));
    }
    @Test void twoAcceptancesAndRepeatedContractCreationCannotDuplicate()throws Exception{
        var outcomes=race(()->{try{return proposals.accept(a.getMaNguoiDung(),offer.getMaDeXuat()).changed();}catch(BusinessException ex){return false;}},()->{try{return proposals.accept(a.getMaNguoiDung(),other.getMaDeXuat()).changed();}catch(BusinessException ex){return false;}});assertNotEquals(outcomes.get(0),outcomes.get(1));Long chosen="DA_CHON".equals(state(offer.getMaDeXuat()))?offer.getMaDeXuat():other.getMaDeXuat();tx.execute(s->TestContracts.legacy(db,chosen));Set<Long> ids=ConcurrentHashMap.newKeySet();race(()->{ids.add(contracts.select(a.getMaNguoiDung(),chosen).getMaHopDong());return true;},()->{ids.add(contracts.select(a.getMaNguoiDung(),chosen).getMaHopDong());return true;});assertEquals(1,ids.size());assertEquals(1,contractsCount());
    }
    @Test void emptyReceivedAndUiStatesRenderForReview()throws Exception{
        var empty=catalog.request(c.getMaNguoiDung(),null,"Yêu cầu chưa có ai đề xuất","Nội dung đủ dài cho trạng thái chưa có đề xuất",BigDecimal.valueOf(1000000),null);mvc.perform(get("/yeu-cau/"+empty.getMaYeuCau()).with(user(c.getEmail()))).andExpect(content().string(containsString("Yêu cầu này chưa nhận được đề xuất.")));
        Path folder=Path.of(".run","proposal-qa","rendered");Files.createDirectories(folder);
        save(folder,"received",get("/de-xuat/nhan").with(user(a.getEmail())));save(folder,"dashboard-customer",get("/bang-dieu-khien").with(user(a.getEmail())));save(folder,"requests",get("/yeu-cau").with(user(a.getEmail())));save(folder,"confirm-reject",get("/de-xuat/"+offer.getMaDeXuat()+"/xac-nhan").param("hanhDong","tu-choi").with(user(a.getEmail())));save(folder,"confirm-accept",get("/de-xuat/"+offer.getMaDeXuat()+"/xac-nhan").param("hanhDong","chap-thuan").with(user(a.getEmail())));
        proposals.reject(a.getMaNguoiDung(),other.getMaDeXuat());save(folder,"rejected",get("/de-xuat/da-gui").with(user(d.getEmail())));proposals.accept(a.getMaNguoiDung(),offer.getMaDeXuat());save(folder,"accepted",get("/de-xuat/da-gui").with(user(b.getEmail())));save(folder,"review",get("/ke-hoach-moc/de-xuat/"+offer.getMaDeXuat()).with(user(a.getEmail())));tx.execute(s->TestContracts.legacy(db,offer.getMaDeXuat()));save(folder,"contracted",get("/de-xuat/da-gui").with(user(b.getEmail())));save(folder,"dashboard-expert",get("/bang-dieu-khien").with(user(b.getEmail())));
    }
    private void save(Path folder,String name,org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder req)throws Exception{var result=mvc.perform(req).andExpect(status().isOk()).andReturn();Files.write(folder.resolve(name+".html"),result.getResponse().getContentAsByteArray());}
}
