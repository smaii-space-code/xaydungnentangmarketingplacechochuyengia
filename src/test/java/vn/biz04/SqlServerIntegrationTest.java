package vn.biz04;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.*;
import org.springframework.test.context.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import vn.biz04.domain.*;
import vn.biz04.data.Store;
import vn.biz04.service.*;
import java.math.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
@ActiveProfiles("sqlserver-test")
@ContextConfiguration(initializers=SqlServerIntegrationTest.SafeTestDatabase.class)
@EnabledIfEnvironmentVariable(named="BIZ04_TEST_SQLSERVER",matches="true")
class SqlServerIntegrationTest {
    public static class SafeTestDatabase implements ApplicationContextInitializer<ConfigurableApplicationContext>{public void initialize(ConfigurableApplicationContext ctx){String u=System.getenv("BIZ04_TEST_DB_URL");if(u==null||!u.toLowerCase(Locale.ROOT).matches(".*databasename=biz04_test(?:;.*)?"))throw new IllegalStateException("Chỉ chạy trên database BIZ04_Test được chỉ định rõ bằng BIZ04_TEST_DB_URL.");}}
    @Autowired ProposalService proposalDecisions;
    @Autowired TransactionTemplate tx;@Autowired Store db;@Autowired AccountService accounts;@Autowired CatalogService catalog;@Autowired ContractService contracts;@Autowired PaymentService payments;@Autowired JdbcTemplate jdbc;
    record Setup(Long kh,Long mid){}
    Setup fixture(){return tx.execute(s->{String x=UUID.randomUUID().toString();NguoiDung kh=TestAccounts.create(db,"Khách kiểm tra SQL","kh"+x+"@sql.test","Demo@12345",true,false);NguoiDung cg=TestAccounts.create(db,"Chuyên gia kiểm tra SQL","cg"+x+"@sql.test","Demo@12345",false,true);YeuCau y=catalog.request(kh.getMaNguoiDung(),null,"Yêu cầu kiểm tra SQL Server","Kiểm tra giao dịch và khóa thật của SQL Server.",BigDecimal.valueOf(2000000),null);DeXuat d=catalog.proposal(cg.getMaNguoiDung(),y.getMaYeuCau(),"Thực hiện kiểm tra có dữ liệu độc lập",BigDecimal.valueOf(2000000),10);proposalDecisions.accept(kh.getMaNguoiDung(),d.getMaDeXuat());HopDong h=TestContracts.legacy(db,d.getMaDeXuat());MocNghiemThu m=contracts.milestone(kh.getMaNguoiDung(),h.getMaHopDong(),null,1,"Mốc kiểm tra SQL","Nội dung công việc kiểm thử","Có kết quả kiểm chứng SQL Server",BigDecimal.valueOf(2000000),LocalDate.now().plusDays(10));int v=h.getPhienBan();contracts.confirm(kh.getMaNguoiDung(),h.getMaHopDong(),v);contracts.confirm(cg.getMaNguoiDung(),h.getMaHopDong(),v);return new Setup(kh.getMaNguoiDung(),m.getMaMocNghiemThu());});}
    @Test void filteredIndexesAndMultipleNullReferences(){Setup a=fixture(),b=fixture();payments.fund(a.kh,a.mid,"FAIL");payments.fund(b.kh,b.mid,"FAIL");assertTrue(jdbc.queryForObject("select count(*) from GiaoDich where MaThamChieuCong is null",Integer.class)>=2);assertNotNull(jdbc.queryForObject("select filter_definition from sys.indexes where name='UX_GiaoDich_ThamChieu'",String.class));assertNotNull(jdbc.queryForObject("select filter_definition from sys.indexes where name='UX_TranhChap_DangMo'",String.class));}
    @Test void concurrentFundingHasOnlyOneFinancialEffect()throws Exception{Setup s=fixture();ExecutorService pool=Executors.newFixedThreadPool(2);CountDownLatch gate=new CountDownLatch(1);try{Callable<Long> call=()->{gate.await();return payments.fund(s.kh,s.mid,"SUCCESS").getMaGiaoDich();};Future<Long> a=pool.submit(call),b=pool.submit(call);gate.countDown();assertEquals(a.get(30,TimeUnit.SECONDS),b.get(30,TimeUnit.SECONDS));tx.execute(t->{assertEquals(0,BigDecimal.valueOf(2000000).compareTo(payments.balance(s.mid).dangGiu()));return null;});}finally{pool.shutdownNow();}}
}
