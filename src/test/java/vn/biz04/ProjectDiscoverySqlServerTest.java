package vn.biz04;

import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.*;

/** Cùng các ca quyền/lọc/khóa, kiểm chứng riêng trên SQL Server BIZ04_Test. */
@SpringBootTest(properties="spring.datasource.url=${BIZ04_TEST_DB_URL}")
@ActiveProfiles(value="sqlserver-test",inheritProfiles=false)
@ContextConfiguration(initializers=SqlServerIntegrationTest.SafeTestDatabase.class)
@EnabledIfEnvironmentVariable(named="BIZ04_TEST_SQLSERVER",matches="true")
class ProjectDiscoverySqlServerTest extends ProjectDiscoveryTest {}
