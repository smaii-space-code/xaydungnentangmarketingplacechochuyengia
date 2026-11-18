package vn.biz04;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import vn.biz04.service.AccountForm;
import vn.biz04.service.AdminAccounts;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = {"spring.datasource.url=${BIZ04_TEST_DB_URL}",
        "biz.otp.hmac-key=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="})
@ActiveProfiles(value = "sqlserver-test", inheritProfiles = false)
@ContextConfiguration(initializers = SqlServerIntegrationTest.SafeTestDatabase.class)
@EnabledIfEnvironmentVariable(named = "BIZ04_TEST_SQLSERVER", matches = "true")
class AdminAccountsSqlServerTest extends AdminAccountsTest {
    @Test
    void freshApplicationReadsCommittedAccountAndContact() {
        var f = new AccountForm();
        f.setHoTen("Kiểm thử lưu bền");
        f.setEmail(address());
        f.setPassword(TEST_PASSWORD);
        f.setPasswordAgain(TEST_PASSWORD);
        f.setSoDienThoai("0962481678");
        Long adminId = admin.getMaNguoiDung();
        Long id = accounts.create(adminId, "KHACH_HANG", f, null);
        try (var restarted = new SpringApplicationBuilder(MarketplaceApplication.class)
                .profiles("sqlserver-test")
                .initializers(new SqlServerIntegrationTest.SafeTestDatabase())
                .run("--server.port=0", "--spring.datasource.url=" + System.getenv("BIZ04_TEST_DB_URL"),
                        "--biz.demo=false", "--biz.scheduler.enabled=false")) {
            var account = restarted.getBean(AdminAccounts.class).detail(adminId, id);
            assertEquals(f.getEmail(), account.email());
            assertEquals("HOAT_DONG", account.status());
            assertEquals("DA_XAC_THUC", account.emailStatus());
            assertEquals("0962481678", account.contact().phone());
        }
    }
}
