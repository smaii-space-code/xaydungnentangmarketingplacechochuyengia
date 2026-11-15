package vn.biz04.service;
import org.springframework.stereotype.Component;
import vn.biz04.data.Store;
import vn.biz04.domain.*;
import java.time.*;
import java.util.Objects;
@Component
public class Access {
    private final Store db; private final Clock clock;
    public Access(Store db,Clock clock) { this.db=db; this.clock=clock; }
    public LocalDateTime now() { return LocalDateTime.now(clock); }
    public NguoiDung user(Long id) { NguoiDung u=db.get(NguoiDung.class,id); Checks.require("HOAT_DONG".equals(u.getTrangThai()),"Tài khoản đã bị khóa."); return u; }
    public NguoiDung role(Long id,boolean customer){NguoiDung u=db.lockEntity(NguoiDung.class,id);user(id);Checks.require(customer?u.getLaKhachHang():u.getLaChuyenGia(),customer?"Bạn cần vai trò khách hàng.":"Bạn cần vai trò chuyên gia.");if(!customer)Checks.require(ExpertEligibility.eligible(db,u),"Hồ sơ chuyên gia chưa được duyệt.");return u;}
    public Long customer(HopDong h) { return h.getMaKeHoach()!=null?h.getMaKhachHang():db.get(YeuCau.class,h.getMaYeuCau()).getMaKhachHang(); }
    public Long expert(HopDong h) { return h.getMaKeHoach()!=null?h.getMaChuyenGia():db.get(DeXuat.class,h.getMaDeXuat()).getMaChuyenGia(); }
    public boolean party(HopDong h,Long id) { return Objects.equals(customer(h),id)||Objects.equals(expert(h),id); }
    public void member(HopDong h,Long id) { user(id); Checks.require(party(h,id),"Bạn không phải một bên của hợp đồng."); }
    public void read(HopDong h,Long id) { NguoiDung u=user(id); Checks.require(u.getLaQuanTriVien()||party(h,id),"Bạn không được xem hợp đồng này."); }
    public void admin(Long id) { Checks.require(user(id).getLaQuanTriVien(),"Chỉ quản trị viên được thực hiện."); }
    public void audit(Long who,String type,Long id,String action,String reason) { NhatKyXuLy n=new NhatKyXuLy();n.setMaNguoiThucHien(who);n.setLoaiDoiTuong(type);n.setMaDoiTuong(id);n.setHanhDong(action);n.setNoiDungLyDo(reason);n.setThoiDiem(now());db.save(n); }
    public boolean openDispute(Long mid) { return db.count("select count(t) from TranhChap t where t.maMocNghiemThu=:m and t.ngayDong is null","m",mid)>0; }
}
