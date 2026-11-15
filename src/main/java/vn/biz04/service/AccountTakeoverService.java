package vn.biz04.service;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.biz04.data.Store;
import vn.biz04.domain.*;
import java.nio.charset.StandardCharsets;
import java.time.*;

/** Invoked only by the local interactive operator command, never by a sample-password web session. */
@Service
public class AccountTakeoverService {
    private final OtpService otp;private final Store db;private final TransactionTemplate tx;private final PasswordEncoder encoder;private final Access access;private final Clock clock;private final RoleObligations obligations;
    public AccountTakeoverService(OtpService otp,Store db,TransactionTemplate tx,PasswordEncoder encoder,Access access,Clock clock,RoleObligations obligations){this.otp=otp;this.db=db;this.tx=tx;this.encoder=encoder;this.access=access;this.clock=clock;this.obligations=obligations;}
    public OtpService.Result start(Long id,String name,String email){return otp.startTakeover(id,email,name,"LOCAL_TAKEOVER");}
    public OtpService.Result complete(Long id,String token,String code,String password,String again){
        Checks.require(password!=null&&password.length()>=8&&password.getBytes(StandardCharsets.UTF_8).length<=72,"Mật khẩu mới từ 8 ký tự, tối đa 72 byte UTF-8.");Checks.require(password.equals(again),"Mật khẩu nhập lại không khớp.");
        return tx.execute(s->{
            var result=otp.verifyTakeover(token,id,code,"LOCAL_TAKEOVER");if(!result.success())return result;
            NguoiDung u=db.get(NguoiDung.class,id);TiepQuanTaiKhoan t=db.lockEntity(TiepQuanTaiKhoan.class,id);Checks.require(t.completed==null&&t.pendingName!=null,"Hồ sơ tiếp quản không hợp lệ.");
            // verifyTakeover holds the OTP gate and user row lock until this entire transaction commits.
            Checks.require(u.getLaChuyenGia()&&!u.getLaQuanTriVien(),"Luồng tiếp quản này chỉ dành cho tài khoản chuyên gia mẫu, không cấp quyền quản trị.");
            if(u.getLaKhachHang())Checks.require(!obligations.outstanding(id,true),"Chưa thể hoàn tất tiếp quản chuyên gia vì tài khoản còn nghĩa vụ Khách hàng. Giữ nguyên vai trò, dữ liệu và OTP; hãy xử lý nghĩa vụ trước.");
            Checks.require(!encoder.matches(password,u.getMatKhauBam()),"Mật khẩu mới phải khác mật khẩu cũ.");
            u.setHoTen(t.pendingName);u.setMatKhauBam(encoder.encode(password));u.setLaKhachHang(false);u.setLoaiTaiKhoan("CHINH_THUC");u.setTrangThai("HOAT_DONG");u.setPhienBaoMat(u.getPhienBaoMat()+1);t.completed=OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);u.setNgayCapNhat(t.completed);
            access.audit(id,"NguoiDung",id,"HOAN_TAT_TIEP_QUAN","Email OTP và mật khẩu mới đã được thiết lập; giữ nguyên ID.");return new OtpService.Result(null,true,"Tiếp quản hoàn tất. Hãy đăng nhập bằng email mới.");
        });
    }
}
