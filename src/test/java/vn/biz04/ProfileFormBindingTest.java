package vn.biz04;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.validation.BindingResult;
import org.springframework.web.util.HtmlUtils;
import org.springframework.util.*;
import vn.biz04.data.Store;
import vn.biz04.domain.*;
import vn.biz04.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.util.regex.*;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:profilebinding;MODE=MSSQLServer;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false","biz.otp.hmac-key=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=","biz.cccd.key=AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQE="})
@AutoConfigureMockMvc(print=org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint.NONE) @ActiveProfiles("test")
class ProfileFormBindingTest {
    @Autowired MockMvc mvc;@Autowired Store db;@Autowired TransactionTemplate tx;@Autowired AccountService accounts;@Autowired CatalogService catalog;
    Long id;String email;
    @BeforeEach void setup(){email="binding"+UUID.randomUUID()+"@example.test";id=tx.execute(s->TestAccounts.create(db,"Người kiểm thử",email,UUID.randomUUID().toString(),true,false).getMaNguoiDung());var f=accounts.form(id);f.setSoDienThoai("0912345678");f.setSoCccdMoi("012345678901");accounts.profile(id,f);}
    String rendered()throws Exception{return mvc.perform(get("/ho-so").with(user(email))).andReturn().getResponse().getContentAsString();}
    static Map<String,String> attrs(String source){Map<String,String> result=new LinkedHashMap<>();var m=Pattern.compile("([\\w:-]+)(?:\\s*=\\s*(\"[^\"]*\"|'[^']*'|[^\\s>]+))?").matcher(source);while(m.find()){String value=m.group(2);if(value==null)value="";else if(value.startsWith("\"")||value.startsWith("'"))value=value.substring(1,value.length()-1);result.put(m.group(1),HtmlUtils.htmlUnescape(value));}return result;}
    MultiValueMap<String,String> fields(String html){
        var form=Pattern.compile("(?s)<form\\b[^>]*action=\"/ho-so\"[^>]*>(.*?)</form>").matcher(html);assertTrue(form.find());var result=new LinkedMultiValueMap<String,String>();
        var inputs=Pattern.compile("<input\\b([^>]*)>").matcher(form.group(1));while(inputs.find()){var a=attrs(inputs.group(1));if(!a.containsKey("name")||a.containsKey("disabled")||("checkbox".equals(a.get("type"))&&!a.containsKey("checked")))continue;result.add(a.get("name"),a.getOrDefault("value",""));}
        var areas=Pattern.compile("(?s)<textarea\\b([^>]*)>(.*?)</textarea>").matcher(form.group(1));while(areas.find())result.add(attrs(areas.group(1)).get("name"),HtmlUtils.htmlUnescape(areas.group(2)));
        var selects=Pattern.compile("(?s)<select\\b([^>]*)>(.*?)</select>").matcher(form.group(1));while(selects.find()){var options=Pattern.compile("<option\\b([^>]*)>").matcher(selects.group(2));String value=null;while(options.find()){var a=attrs(options.group(1));if(value==null||a.containsKey("selected"))value=a.get("value");}result.add(attrs(selects.group(1)).get("name"),value);}
        return result;
    }
    String cipher(){return tx.execute(s->db.get(HoSoKhachHang.class,id).soCccdMaHoa);}
    NguoiDung account(){return tx.execute(s->db.get(NguoiDung.class,id));}
    MvcResult submit(MultiValueMap<String,String> fields)throws Exception{
        var result=mvc.perform(post("/ho-so").with(user(email)).params(fields).with(csrf()).header("User-Agent","ProfileRegressionBrowser").header("Content-Type","application/x-www-form-urlencoded").header("Accept-Language","vi-VN").header("Accept-Encoding","gzip, deflate, br").header("Sec-Fetch-Site","same-origin")).andReturn();
        Object binding=result.getModelAndView()==null?null:result.getModelAndView().getModel().get(BindingResult.MODEL_KEY_PREFIX+"form");
        Path folder=Path.of(".run","profile-binding");Files.createDirectories(folder);new ObjectMapper().writeValue(folder.resolve("submitted-field-names.json").toFile(),Map.of("submittedFields",fields.keySet(),"suppressedFields",binding instanceof BindingResult b?List.of(b.getSuppressedFields()):List.of(),"httpStatus",result.getResponse().getStatus()));
        return result;
    }
    @ParameterizedTest @ValueSource(booleans={true,false}) void renderedProfileSubmissionSavesAndReloadsWithoutChangingEmailOrCipher(boolean customerRole)throws Exception{
        tx.executeWithoutResult(s->{var u=db.get(NguoiDung.class,id);u.setLaKhachHang(customerRole);u.setLaChuyenGia(!customerRole);});
        String before=cipher();var data=fields(rendered());assertFalse(data.containsKey("khach"));assertFalse(data.containsKey("chuyenGia"));assertTrue(data.containsKey("_csrf"));assertFalse(data.containsKey("email"));assertEquals("",data.getFirst("soCccdMoi"));data.set("hoTen","Tên đã sửa");data.set("soDienThoai","0987654321");data.set("diaChi","Địa chỉ kiểm thử");data.set("gioiThieu","Giới thiệu kiểm thử đã cập nhật");
        var saved=submit(data);assertEquals(302,saved.getResponse().getStatus(),"Rendered form should save; see names-only diagnostic");assertEquals("/ho-so",saved.getResponse().getRedirectedUrl());assertEquals("Tên đã sửa",accounts.form(id).getHoTen());assertEquals("0987654321",accounts.form(id).getSoDienThoai());assertEquals(email,account().getEmail());assertTrue(before.equals(cipher()),"Existing ciphertext must stay unchanged");assertEquals(customerRole,account().getLaKhachHang());assertEquals(!customerRole,account().getLaChuyenGia());var loaded=fields(rendered());assertEquals("Tên đã sửa",loaded.getFirst("hoTen"));assertEquals("Địa chỉ kiểm thử",loaded.getFirst("diaChi"));assertEquals("",loaded.getFirst("soCccdMoi"));
    }
    @ParameterizedTest @ValueSource(strings={"id","maNguoiDung","laQuanTriVien","admin","matKhauBam","password","passwordAgain","trangThai","trangThaiEmail","thoiDiemXacThucEmail","loaiTaiKhoan","phienBaoMat","email","khach","chuyenGia"})
    void protectedParametersStillRejectEntireSave(String forbidden)throws Exception{
        var before=account();String encrypted=cipher();var data=fields(rendered());data.set("hoTen","Không được lưu");data.set(forbidden,forbidden.equals("email")?"unowned@example.test":"true");var result=submit(data);assertEquals(200,result.getResponse().getStatus());assertTrue(result.getResponse().getContentAsString().contains("Form chứa trường không được phép cập nhật."));var after=account();assertEquals(before.getHoTen(),after.getHoTen());assertTrue(before.getEmail().equals(after.getEmail()));assertTrue(before.getMatKhauBam().equals(after.getMatKhauBam()));assertEquals(before.getLaQuanTriVien(),after.getLaQuanTriVien());assertEquals(before.getTrangThai(),after.getTrangThai());assertEquals(before.getTrangThaiEmail(),after.getTrangThaiEmail());assertEquals(before.getPhienBaoMat(),after.getPhienBaoMat());assertTrue(encrypted.equals(cipher()));
    }
    @Test void headersCannotSupplyProfileFieldsWhenBodyOmitsThem()throws Exception{
        var data=fields(rendered());data.remove("hoTen");var result=mvc.perform(post("/ho-so").with(user(email)).params(data).with(csrf()).header("Ho-Ten","Tên chèn từ header")).andReturn();assertEquals(200,result.getResponse().getStatus());assertTrue(result.getResponse().getContentAsString().contains("Họ tên cần từ 1 đến 100 ký tự."));assertEquals("Người kiểm thử",account().getHoTen());
    }
    @Test void roleFieldsCannotBeUsedForSelfGrant()throws Exception{
        var data=fields(rendered());data.remove("khach");data.set("chuyenGia","true");var saved=submit(data);assertEquals(200,saved.getResponse().getStatus());assertTrue(account().getLaKhachHang());assertFalse(account().getLaChuyenGia());
    }
    @Test void openObligationsStillRejectRemovingCustomerRole()throws Exception{
        catalog.request(id,null,"Yêu cầu kiểm thử hồ sơ","Công việc mở để kiểm tra giữ vai trò",java.math.BigDecimal.TEN,null);var data=fields(rendered());data.remove("khach");data.set("chuyenGia","true");data.set("hoTen","Không được lưu một phần");var saved=submit(data);assertEquals(200,saved.getResponse().getStatus());assertTrue(saved.getResponse().getContentAsString().contains("Form chứa trường không được phép cập nhật."));assertTrue(account().getLaKhachHang());assertEquals("Người kiểm thử",account().getHoTen());
    }
    @Test void missingOrInvalidCsrfStillBlocksProfilePost()throws Exception{
        var data=fields(rendered());data.set("_csrf","invalid");assertEquals(403,mvc.perform(post("/ho-so").with(user(email)).params(data)).andReturn().getResponse().getStatus());assertEquals("Người kiểm thử",account().getHoTen());
    }
    @Test void organizationInformationPersistsAndNewCccdCanRemainBlank()throws Exception{
        var data=fields(rendered());String before=cipher();data.set("loaiKhachHang","TO_CHUC");data.set("tenToChuc","  Tổ chức kiểm thử  ");assertEquals(302,submit(data).getResponse().getStatus());var loaded=fields(rendered());assertEquals("TO_CHUC",loaded.getFirst("loaiKhachHang"));assertEquals("Tổ chức kiểm thử",loaded.getFirst("tenToChuc"));assertTrue(before.equals(cipher()));
    }
}
