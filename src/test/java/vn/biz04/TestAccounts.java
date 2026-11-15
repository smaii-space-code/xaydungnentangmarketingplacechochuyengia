package vn.biz04;
import vn.biz04.domain.NguoiDung;
import vn.biz04.data.Store;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
final class TestAccounts {
 static NguoiDung create(Store db,String name,String email,String password,boolean customer,boolean expert){
  NguoiDung u=new NguoiDung();u.setHoTen(name);u.setEmail(email);u.setMatKhauBam(new BCryptPasswordEncoder().encode(password));u.setLaKhachHang(customer);u.setLaChuyenGia(expert);u.setLaQuanTriVien(false);u.setTrangThai("HOAT_DONG");u.setTrangThaiEmail("DA_XAC_THUC");u.setThoiDiemXacThucEmail(java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC));db.save(u);return u;
 }
}
