package vn.biz04.service;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.biz04.data.Store;
import vn.biz04.domain.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;

@Service @Transactional
public class AccountService {
    private final Store db; private final Access access; private final PasswordEncoder encoder;
    private final AccountCrypto crypto; private final RoleObligations roles; private final Clock clock;
    public AccountService(Store db,Access access,PasswordEncoder encoder,AccountCrypto crypto,RoleObligations roles,Clock clock){this.db=db;this.access=access;this.encoder=encoder;this.crypto=crypto;this.roles=roles;this.clock=clock;}
    public static String email(String value){
        String mail=clean(value).toLowerCase(Locale.ROOT);
        Checks.require(mail.length()<=254&&mail.matches("[a-z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-z0-9](?:[a-z0-9-]*[a-z0-9])?(?:\\.[a-z0-9](?:[a-z0-9-]*[a-z0-9])?)+")&&!mail.startsWith(".")&&!mail.contains("..")&&mail.charAt(mail.indexOf('@')-1)!='.'&&mail.indexOf('@')<=64,"Địa chỉ email không đúng định dạng. Vui lòng kiểm tra lại.");
        return mail;
    }
    public void unique(String mail,Long except){Checks.require(db.list("from NguoiDung u where lower(trim(u.email))=:e",NguoiDung.class,"e",mail).stream().allMatch(u->Objects.equals(u.getMaNguoiDung(),except)),"Email đã được sử dụng.");}
    private void password(String value){Checks.require(value!=null&&value.length()>=8&&value.getBytes(StandardCharsets.UTF_8).length<=72,"Mật khẩu từ 8 ký tự, tối đa 72 byte UTF-8.");}
    private static String clean(String value){return value==null?"":value.replaceAll("(?U)^\\s+|\\s+$","");}
    private String optional(String text,int max,String label){String value=clean(text);return value.isEmpty()?null:Checks.text(value,1,max,label);}
    private void validate(AccountForm f,boolean fresh){
        Checks.text(clean(f.getHoTen()),1,100,"Họ tên");
        String phone=optional(f.getSoDienThoai(),10,"Số điện thoại di động");
        Checks.require((!fresh&&phone==null)||(phone!=null&&phone.matches("^0[35789][0-9]{8}$")),"Số điện thoại di động phải là số Việt Nam gồm 10 chữ số, bắt đầu bằng 03, 05, 07, 08 hoặc 09.");
        Checks.require(Set.of("CA_NHAN","TO_CHUC").contains(Objects.toString(f.getLoaiKhachHang(),"")),"Loại khách hàng không hợp lệ.");
        if("TO_CHUC".equals(f.getLoaiKhachHang()))Checks.require(optional(f.getTenToChuc(),200,"Tên tổ chức")!=null,"Vui lòng nhập tên công ty, doanh nghiệp.");
        optional(f.getDiaChi(),250,"Địa chỉ");optional(f.getGioiThieu(),2000,"Giới thiệu");
        String cccd=clean(f.getSoCccdMoi());
        Checks.require(cccd.isEmpty()||cccd.matches("[0-9]{12}"),"Số CCCD mới phải gồm đúng 12 chữ số từ 0 đến 9.");
    }
    public NguoiDung register(AccountForm f){return register(f,false);}
    public NguoiDung registerExpert(AccountForm f){return register(f,true);}
    private NguoiDung register(AccountForm f,boolean expert){
        validate(f,true);String mail=email(f.getEmail());unique(mail,null);password(f.getPassword());Checks.require(Objects.equals(f.getPassword(),f.getPasswordAgain()),"Mật khẩu nhập lại không khớp.");
        NguoiDung u=new NguoiDung();u.setHoTen(clean(f.getHoTen()));u.setEmail(mail);u.setMatKhauBam(encoder.encode(f.getPassword()));u.setNguonDangKy(expert?"CHUYEN_GIA":"KHACH_HANG");u.setLaKhachHang(!expert);u.setLaChuyenGia(false);u.setLaQuanTriVien(false);u.setTrangThai("CHO_XAC_THUC");u.setTrangThaiEmail("CHO_XAC_THUC");u.setNgayTao(now());u.setNgayCapNhat(now());db.save(u);
        HoSoKhachHang h=new HoSoKhachHang();h.maNguoiDung=u.getMaNguoiDung();apply(h,f);db.save(h);access.audit(u.getMaNguoiDung(),"NguoiDung",u.getMaNguoiDung(),"DANG_KY_CHO_OTP",null);return u;
    }
    private OffsetDateTime now(){return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);}
    private void apply(HoSoKhachHang h,AccountForm f){h.soDienThoai=optional(f.getSoDienThoai(),10,"Số điện thoại");h.loaiKhachHang=f.getLoaiKhachHang();h.tenToChuc="TO_CHUC".equals(h.loaiKhachHang)?optional(f.getTenToChuc(),200,"Tên tổ chức"):null;h.diaChi=optional(f.getDiaChi(),250,"Địa chỉ");h.gioiThieu=optional(f.getGioiThieu(),2000,"Giới thiệu");}
    public AccountForm form(Long id){NguoiDung u=access.user(id);AccountForm f=new AccountForm();f.setHoTen(u.getHoTen());f.setEmail(u.getEmail());f.setKhach(u.getLaKhachHang());f.setChuyenGia(u.getLaChuyenGia());db.first("from HoSoKhachHang h where h.maNguoiDung=:u",HoSoKhachHang.class,"u",id).ifPresent(h->{f.setSoDienThoai(h.soDienThoai);f.setLoaiKhachHang(h.loaiKhachHang);f.setTenToChuc(h.tenToChuc);f.setDiaChi(h.diaChi);f.setGioiThieu(h.gioiThieu);});return f;}
    public String maskedCccd(Long id){return db.first("from HoSoKhachHang h where h.maNguoiDung=:u",HoSoKhachHang.class,"u",id).map(h->h.cccdBonSoCuoi==null?"":"********"+h.cccdBonSoCuoi).orElse("");}
    public void profile(Long id,AccountForm f){
        NguoiDung u=db.lockEntity(NguoiDung.class,id);access.user(id);validate(f,false);
        Checks.require((!f.isKhach()||u.getLaKhachHang())&&(!f.isChuyenGia()||u.getLaChuyenGia()),"Không được tự cấp thêm vai trò qua hồ sơ.");
        Checks.require(f.isKhach()||f.isChuyenGia()||u.getLaQuanTriVien(),"Tài khoản phải có ít nhất một vai trò: Khách hàng hoặc Chuyên gia.");
        if(u.getLaKhachHang()&&!f.isKhach())Checks.require(!roles.outstanding(id,true),"Bạn chưa thể bỏ vai trò Khách hàng vì còn công việc hoặc giao dịch chưa hoàn tất.");
        if(u.getLaChuyenGia()&&!f.isChuyenGia())Checks.require(!roles.outstanding(id,false),"Bạn chưa thể bỏ vai trò Chuyên gia vì còn đề xuất hoặc giao dịch chưa hoàn tất.");
        String cccd=optional(f.getSoCccdMoi(),12,"Số CCCD mới");String encrypted=cccd==null?null:crypto.encryptCccd(id,cccd);
        HoSoKhachHang h=db.first("from HoSoKhachHang h where h.maNguoiDung=:u",HoSoKhachHang.class,"u",id).orElse(null);
        if(h==null){h=new HoSoKhachHang();h.maNguoiDung=id;db.save(h);}apply(h,f);
        if(encrypted!=null){h.soCccdMaHoa=encrypted;h.cccdBonSoCuoi=cccd.substring(8);}
        u.setHoTen(clean(f.getHoTen()));u.setLaKhachHang(f.isKhach());u.setLaChuyenGia(f.isChuyenGia());u.setNgayCapNhat(now());access.audit(id,"NguoiDung",id,"CAP_NHAT_HO_SO",null);
    }
    public boolean passwordMatches(NguoiDung u,String password){return password!=null&&password.getBytes(StandardCharsets.UTF_8).length<=72&&encoder.matches(password,u.getMatKhauBam());}
    public void checkPassword(NguoiDung u,String password){Checks.require(passwordMatches(u,password),"Mật khẩu hiện tại không đúng.");}
    public void block(Long admin,Long target,boolean blocked){access.admin(admin);Checks.require(!Objects.equals(admin,target),"Không tự khóa tài khoản đang sử dụng.");NguoiDung u=db.lockEntity(NguoiDung.class,target);Checks.require(!u.getLaQuanTriVien(),"Không khóa quản trị viên trong bản đồ án.");Checks.require(Set.of("HOAT_DONG","KHOA").contains(u.getTrangThai()),"Không mở khóa hồ sơ đang chờ xác thực hoặc xét duyệt.");Checks.require(!"CHO_TIEP_QUAN".equals(u.getTrangThai()),"Tài khoản đang tiếp quản, không mở khóa bằng thao tác thông thường.");Checks.require(!"CHO_XAC_THUC".equals(u.getTrangThaiEmail()),"Tài khoản chưa hoàn tất xác thực email.");u.setTrangThai(blocked?"KHOA":"HOAT_DONG");access.audit(admin,"NguoiDung",target,blocked?"KHOA":"MO_KHOA",null);}
}
