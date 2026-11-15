package vn.biz04;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.*;

@SpringBootTest(properties={"spring.datasource.url=${BIZ04_TEST_DB_URL}","biz.otp.hmac-key=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=","biz.cccd.key=AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQE="})
@ActiveProfiles(value="sqlserver-test",inheritProfiles=false)
@ContextConfiguration(initializers=SqlServerIntegrationTest.SafeTestDatabase.class)
@EnabledIfEnvironmentVariable(named="BIZ04_TEST_SQLSERVER",matches="true")
class CatalogDiscoverySqlServerTest extends CatalogDiscoveryTest {}
