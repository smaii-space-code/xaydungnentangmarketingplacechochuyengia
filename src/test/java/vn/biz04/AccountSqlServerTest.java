package vn.biz04;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import vn.biz04.data.Store;
import vn.biz04.domain.*;
import vn.biz04.service.*;
import java.util.*;
import java.util.concurrent.*;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(properties={"biz.otp.hmac-key=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=","biz.cccd.key=AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQE="})
@ActiveProfiles("sqlserver-test")
@ContextConfiguration(initializers=SqlServerIntegrationTest.SafeTestDatabase.class)
@EnabledIfEnvironmentVariable(named="BIZ04_TEST_SQLSERVER",matches="true")
class AccountSqlServerTest {
    @Autowired TransactionTemplate tx;@Autowired Store db;@Autowired AccountService accounts;@Autowired OtpService otp;@Autowired CatalogService catalog;@Autowired JdbcTemplate jdbc;
    @MockBean OtpMailGateway mail;
    Map<String,String> codes=new ConcurrentHashMap<>();String ip;
    @BeforeEach void setup(){ip=UUID.randomUUID().toString();when(mail.send(anyString(),anyString())).thenAnswer(a->{codes.put(a.getArgument(0),a.getArgument(1));return OtpMailGateway.Delivery.ACCEPTED;});}
    AccountForm form(){AccountForm f=new AccountForm();f.setHoTen("Kiểm thử SQL OTP");f.setEmail("otp"+UUID.randomUUID()+"@sql.test");f.setPassword("Demo@12345");f.setPasswordAgain("Demo@12345");f.setSoDienThoai("0912345678");return f;}
    Long id(String email){return tx.execute(s->db.first("from NguoiDung u where u.email=:e",NguoiDung.class,"e",email).orElseThrow().getMaNguoiDung());}
    Long active(){AccountForm f=form();var r=otp.register(f,ip);assertTrue(otp.verify(r.token(),null,codes.get(f.getEmail()),ip).success());return id(f.getEmail());}
    @Test void realMigrationHasOffsetColumnsAndFilteredPendingIndex(){
        assertEquals(1,jdbc.queryForObject("select count(*) from flyway_schema_history where version='3' and success=1",Integer.class));
        assertEquals("datetimeoffset",jdbc.queryForObject("select DATA_TYPE from INFORMATION_SCHEMA.COLUMNS where TABLE_NAME='NguoiDung' and COLUMN_NAME='NgayTao'",String.class));
        assertEquals(6,jdbc.queryForObject("select DATETIME_PRECISION from INFORMATION_SCHEMA.COLUMNS where TABLE_NAME='MaXacThucOTP' and COLUMN_NAME='ThoiDiemHetHan'",Integer.class));
        assertNotNull(jdbc.queryForObject("select filter_definition from sys.indexes where name='UX_OTP_Cho'",String.class));
    }
    @Test void realDatabaseEnforcesCaseInsensitiveEmail(){Long id=active();String email=tx.execute(s->db.get(NguoiDung.class,id).getEmail());assertThrows(DataIntegrityViolationException.class,()->tx.executeWithoutResult(s->{TestAccounts.create(db,"Trùng email",email.toUpperCase(Locale.ROOT),"Demo@12345",true,false);db.flush();}));}
    @Test void realDatabasePersistsFailedAttemptsAndCiphertext(){
        AccountForm f=form();var r=otp.register(f,ip);Long id=id(f.getEmail());assertFalse(otp.verify(r.token(),null,"invalid",ip).success());assertEquals(1,jdbc.queryForObject("select SoLanThuSai from MaXacThucOTP where MaNguoiDung=?",Integer.class,id));assertTrue(otp.verify(r.token(),null,codes.get(f.getEmail()),ip).success());
        AccountForm profile=accounts.form(id);profile.setSoCccdMoi("012345678901");accounts.profile(id,profile);String encrypted=jdbc.queryForObject("select SoCccdMaHoa from HoSoKhachHang where MaNguoiDung=?",String.class,id);profile.setSoCccdMoi(null);profile.setDiaChi("Địa chỉ Việt Nam");accounts.profile(id,profile);assertEquals(encrypted,jdbc.queryForObject("select SoCccdMaHoa from HoSoKhachHang where MaNguoiDung=?",String.class,id));assertEquals("********8901",accounts.maskedCccd(id));assertFalse(encrypted.contains("012345678901"));
    }
    @Test void realConcurrentOtpHasOneWinner()throws Exception{
        AccountForm f=form();var r=otp.register(f,ip);ExecutorService pool=Executors.newFixedThreadPool(2);CountDownLatch gate=new CountDownLatch(1);
        try{Callable<Boolean> verify=()->{gate.await();return otp.verify(r.token(),null,codes.get(f.getEmail()),ip).success();};Future<Boolean>a=pool.submit(verify),b=pool.submit(verify);gate.countDown();assertNotEquals(a.get(30,TimeUnit.SECONDS),b.get(30,TimeUnit.SECONDS));}finally{pool.shutdownNow();}
    }
    @Test void realRoleRemovalSerializesWithRequestCreation()throws Exception{
        Long id=active();tx.executeWithoutResult(s->{var u=db.get(NguoiDung.class,id);u.setNguonDangKy(null);u.setLaChuyenGia(true);});AccountForm profile=accounts.form(id);profile.setKhach(false);
        ExecutorService pool=Executors.newFixedThreadPool(2);CountDownLatch gate=new CountDownLatch(1);
        try{Future<Boolean>a=pool.submit(()->{gate.await();try{accounts.profile(id,profile);return true;}catch(BusinessException ex){return false;}});Future<Boolean>b=pool.submit(()->{gate.await();try{catalog.request(id,null,"Yêu cầu kiểm tra đồng thời","Nội dung đủ dài để kiểm tra khóa",BigDecimal.TEN,null);return true;}catch(BusinessException ex){return false;}});gate.countDown();assertNotEquals(a.get(30,TimeUnit.SECONDS),b.get(30,TimeUnit.SECONDS));}finally{pool.shutdownNow();}
    }
}
