package vn.biz04.config;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.biz04.data.Store;
import vn.biz04.domain.*;
import vn.biz04.service.*;
import java.math.BigDecimal;
import java.time.LocalDate;
@Component
@ConditionalOnProperty(name="biz.demo",havingValue="true")
public class DemoData implements CommandLineRunner {
    private final Store db;private final AccountService accounts;private final CatalogService catalog;private final String password;
    public DemoData(Store db,AccountService accounts,CatalogService catalog,@Value("${biz.demo-password:}")String password){this.db=db;this.accounts=accounts;this.catalog=catalog;this.password=password;}
    private NguoiDung demoAccount(String name,String email,String password,boolean customer,boolean expert){
        NguoiDung u=new NguoiDung();u.setHoTen(name);u.setEmail(email);u.setMatKhauBam(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode(password));u.setLaKhachHang(customer);u.setLaChuyenGia(expert);u.setLaQuanTriVien(false);u.setTrangThai("HOAT_DONG");u.setTrangThaiEmail("DEMO");u.setLoaiTaiKhoan("MAU");db.save(u);return u;
    }
    @Override @Transactional public void run(String... args){if(db.count("select count(u) from NguoiDung u")>0)return;
        Checks.require(password!=null&&password.length()>=8,"Seed yêu cầu mật khẩu được cấu hình cục bộ; không có mật khẩu mặc định.");
        NguoiDung admin=demoAccount("Quản trị viên Sang Connect","admin@biz04.local",password,true,false);admin.setLaQuanTriVien(true);admin.setLaKhachHang(false);
        NguoiDung kh=demoAccount("Khách hàng mẫu","khach@biz04.local",password,true,false);
        NguoiDung cg=demoAccount("Chuyên gia thiết kế","chuyengia@biz04.local",password,false,true);
        NguoiDung cg2=demoAccount("Chuyên gia nội dung","noidung@biz04.local",password,false,true);
        demoAccount("Tài khoản ngoài hợp đồng","ngoai@biz04.local",password,true,true);
        catalog.service(cg.getMaNguoiDung(),null,"Thiết kế bộ nhận diện thương hiệu","Thiết kế","Thiết kế logo, màu sắc và hướng dẫn nhận diện theo yêu cầu.",BigDecimal.valueOf(2_000_000));
        catalog.service(cg2.getMaNguoiDung(),null,"Viết nội dung cho website doanh nghiệp","Nội dung","Xây dựng nội dung giới thiệu, sản phẩm và dịch vụ cho website.",BigDecimal.valueOf(1_500_000));
        catalog.request(kh.getMaNguoiDung(),null,"Thiết kế nhận diện cho cửa hàng mới","Cần logo và tài liệu nhận diện, chia thành các mốc có sản phẩm và tiêu chí rõ ràng.",BigDecimal.valueOf(4_000_000),LocalDate.now().plusDays(30));
    }
}

