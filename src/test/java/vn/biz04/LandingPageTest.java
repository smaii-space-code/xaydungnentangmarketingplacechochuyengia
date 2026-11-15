package vn.biz04;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.nio.file.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LandingPageTest {
    @Autowired MockMvc mvc;
    @Autowired vn.biz04.data.Store db;
    @Test void publicAboutPageHasProvidedCopyAndNoPersonalIdentifiers() throws Exception {
        String html=mvc.perform(get("/ve-sang-connect")).andExpect(status().isOk()).andExpect(view().name("about"))
            .andExpect(model().attribute("currentPath","/ve-sang-connect")).andReturn().getResponse().getContentAsString();
        for(String text:new String[]{"VỀ SANG CONNECT","Bạn mang ý tưởng.","Câu chuyện nền tảng","Người sáng lập","Mai Phước Sang","Một hành trình hợp tác.","MS","Các chức năng thanh toán đang được mô phỏng."})org.junit.jupiter.api.Assertions.assertTrue(html.contains(text));
        for(String text:new String[]{"1250080159","12_CNPM2","@gmail.com","<blockquote"})org.junit.jupiter.api.Assertions.assertFalse(html.contains(text));
        org.junit.jupiter.api.Assertions.assertTrue(html.contains("href=\"/\""));
        org.junit.jupiter.api.Assertions.assertEquals(1,html.split("<header\\b",-1).length-1);
        org.junit.jupiter.api.Assertions.assertEquals(1,html.split("<footer\\b",-1).length-1);
        org.junit.jupiter.api.Assertions.assertTrue(html.contains("aria-controls=\"about-expert-panel\""));
        org.junit.jupiter.api.Assertions.assertTrue(html.contains("href=\"#cau-chuyen\""));
        Path folder=Path.of(".run","about-20261104","rendered");Files.createDirectories(folder);Files.writeString(folder.resolve("about.html"),html);
        org.junit.jupiter.api.Assertions.assertTrue(html.contains("/images/mai-phuoc-sang.jpg"));
        org.junit.jupiter.api.Assertions.assertTrue(html.contains("Mai Phước Sang — Người sáng lập Sang Connect"));
        mvc.perform(get("/images/mai-phuoc-sang.jpg")).andExpect(status().isOk()).andExpect(content().contentType("image/jpeg"));
        org.junit.jupiter.api.Assertions.assertTrue(html.contains("href=\"/trang/trangchu\""));
        org.junit.jupiter.api.Assertions.assertEquals(1,html.split("aria-current=\"page\"",-1).length-1);
    }
    @Test void publicNavigationUsesCanonicalRoutesAndMarksCurrentPage() throws Exception {
        for(String path:new String[]{"/","/trang/trangchu","/ve-sang-connect"}){
            String html=mvc.perform(get(path)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            String nav=html.substring(html.indexOf("<nav"),html.indexOf("</nav>"));
            org.junit.jupiter.api.Assertions.assertTrue(nav.indexOf(">Trang chủ</a>")<nav.indexOf(">Tìm chuyên gia</a>"));
            org.junit.jupiter.api.Assertions.assertTrue(nav.indexOf(">Tìm chuyên gia</a>")<nav.indexOf(">Về Sang Connect</a>"));
            org.junit.jupiter.api.Assertions.assertTrue(nav.contains("href=\""+path+"\" aria-current=\"page\""));
            org.junit.jupiter.api.Assertions.assertFalse(nav.contains("Quản trị"));
            org.junit.jupiter.api.Assertions.assertTrue(html.contains("href=\"/dang-nhap\""));
        }
    }
    @Test @org.springframework.transaction.annotation.Transactional
    void authenticatedAboutKeepsAccountAndRoleNavigation() throws Exception {
        String email="nav"+java.util.UUID.randomUUID()+"@test.local";
        var u=TestAccounts.create(db,"Mai Phước Sang",email,"Demo@12345",false,false);u.setLaQuanTriVien(true);
        String html=mvc.perform(get("/ve-sang-connect").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(email).roles("QUAN_TRI")))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        for(String text:new String[]{"Xin chào: Mai Phước Sang","Đăng xuất","Yêu cầu công việc","Không gian làm việc","Quản trị"})org.junit.jupiter.api.Assertions.assertTrue(html.contains(text));
        org.junit.jupiter.api.Assertions.assertFalse(html.contains("href=\"/dang-nhap\""));
        org.junit.jupiter.api.Assertions.assertFalse(html.contains("1250080159"));
        org.junit.jupiter.api.Assertions.assertFalse(html.contains("12_CNPM2"));
    }
    @Test void homeKeepsVideoAndRenamedServicesKeepTheirOwnTemplate() throws Exception {
            mvc.perform(get("/")).andExpect(status().isOk()).andExpect(view().name("home"))
                .andExpect(header().doesNotExist("Location"))
                .andExpect(content().string(containsString("id=\"connect-film\"")))
                .andExpect(content().string(containsString("/videos/sang-connect-720p.mp4")))
                .andExpect(content().string(containsString("Bạn mang ý tưởng,")))
                .andExpect(content().string(containsString("Sang Connect kết nối chuyên gia.")));
        mvc.perform(get("/trang/trangchu")).andExpect(status().isOk()).andExpect(view().name("services"))
            .andExpect(header().doesNotExist("Location"))
            .andExpect(content().string(not(containsString("id=\"connect-film\""))));
        mvc.perform(get("/dich-vu")).andExpect(status().isFound()).andExpect(redirectedUrl("/trang/trangchu"));
        mvc.perform(get("/dich-vu").param("q","design")).andExpect(status().isFound()).andExpect(redirectedUrl("/trang/trangchu?q=design"));
        mvc.perform(get("/trang/trangchu").param("q","thiết kế")).andExpect(status().isOk()).andExpect(model().attribute("q","thiết kế"));
    }
    @Test void publicPagesUseBrandAndRealDestinations() throws Exception {
        for (String url : new String[]{"/", "/dang-nhap", "/dang-ky", "/trang/trangchu"}) {
            var result = mvc.perform(get(url)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Sang Connect")))
                .andExpect(content().string(not(containsString("BIZ-04"))))
                .andExpect(content().string(not(containsString("SANG EXPERT"))))
                .andExpect(content().string(containsString("href=\"/dang-ky\"")))
                .andExpect(content().string(containsString("href=\"/dang-nhap\""))).andReturn();
            // Optional rendered pages for visual QA, never used as a production server.
            if (Boolean.getBoolean("sang.capture")) {
                Path folder = Path.of(".taste-skill", "rendered");
                Files.createDirectories(folder);
                Files.write(folder.resolve(url.equals("/") ? "index.html" : url.substring(1).replace('/','-')+".html"), result.getResponse().getContentAsByteArray());
            }
        }
    }
    @Test void videoAndScriptArePublicAndSupportSeeking() throws Exception {
        mvc.perform(get("/js/home.js")).andExpect(status().isOk())
            .andExpect(content().string(containsString("prefers-reduced-motion")));
        mvc.perform(get("/videos/sang-connect-poster.jpg")).andExpect(status().isOk())
            .andExpect(content().contentType("image/jpeg"));
        mvc.perform(get("/videos/sang-connect-720p.mp4").header("Range","bytes=0-99"))
            .andExpect(status().isPartialContent())
            .andExpect(header().string("Content-Range", startsWith("bytes 0-99/")))
            .andExpect(content().contentType("video/mp4"));
        mvc.perform(get("/bang-dieu-khien")).andExpect(status().is3xxRedirection());
    }
}

