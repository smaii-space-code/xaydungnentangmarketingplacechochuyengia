package vn.biz04.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.biz04.data.Store;
import vn.biz04.domain.*;
import java.time.*;
import java.util.*;

@Service @Transactional
public class ExpertApplications {
    public static final String PENDING="Tài khoản của bạn đang chờ xét duyệt. Sang Connect sẽ sớm thông báo kết quả.";
    private final Store db;private final AccountService accounts;private final Access access;private final Clock clock;
    public ExpertApplications(Store db,AccountService accounts,Access access,Clock clock){this.db=db;this.accounts=accounts;this.access=access;this.clock=clock;}
    private OffsetDateTime now(){return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);}
    private void professional(ExpertRegistrationForm f){professional(f.getLinhVuc(),f.getGioiThieu());}
    static void professional(String field,String bio){Checks.text(field,2,100,"Lĩnh vực chuyên môn");Checks.text(bio,20,2000,"Giới thiệu chuyên môn");}
    void approveConversion(NguoiDung user,HoSoChuyenGia profile,Long admin,String reason,String operation){
        snapshot(user,profile,operation);
        recordDecision(admin,profile,true,reason);
        profile.state="DA_DUYET";
        user.setLaKhachHang(false);
        user.setLaChuyenGia(true);
        user.setNgayCapNhat(now());
        user.setPhienBaoMat(user.getPhienBaoMat()+1);
        access.audit(admin,"HoSoChuyenGia",profile.userId,"CHUYEN_KHACH_HANG_SANG_CHUYEN_GIA",reason);
    }
    public NguoiDung register(ExpertRegistrationForm f,String operation){
        return register(f,operation,null);
    }
    NguoiDung registerByAdmin(Long admin,ExpertRegistrationForm f,String operation){
        NguoiDung u=register(f,operation,admin);
        db.flush();
        submitInitial(u,admin);
        return u;
    }
    private NguoiDung register(ExpertRegistrationForm f,String operation,Long admin){
        professional(f);Checks.require(operation!=null&&operation.matches("[a-f0-9-]{36}"),"Phiên đăng ký không hợp lệ.");
        Checks.require(db.count("select count(h) from HoSoChuyenGia h where h.operation=:o","o",operation)==0,"Đăng ký đã được xử lý. Vui lòng tiếp tục xác thực email.");
        NguoiDung u=admin==null?accounts.registerExpert(f.account()):accounts.registerByAdmin(admin,f.account(),true);
        HoSoChuyenGia h=new HoSoChuyenGia();h.userId=u.getMaNguoiDung();h.field=f.getLinhVuc().strip();h.bio=f.getGioiThieu().strip();h.operation=operation;h.created=now();db.save(h);return u;
    }
    public void emailVerified(NguoiDung u){
        submitInitial(u,u.getMaNguoiDung());
    }
    void submitInitial(NguoiDung u,Long submittedBy){
        HoSoChuyenGia h=db.lockEntity(HoSoChuyenGia.class,u.getMaNguoiDung());
        Checks.require("CHUYEN_GIA".equals(u.getNguonDangKy())&&"CHO_XAC_THUC".equals(h.state)&&h.version==0,"Hồ sơ không còn chờ xác thực.");
        snapshot(u,h,h.operation);pending(u,h,submittedBy);
    }
    private void snapshot(NguoiDung u,HoSoChuyenGia h,String operation){
        HoSoKhachHang contact=db.get(HoSoKhachHang.class,u.getMaNguoiDung());
        PhienBanHoSoChuyenGia v=new PhienBanHoSoChuyenGia();v.userId=h.userId;v.version=++h.version;v.name=u.getHoTen();v.email=u.getEmail();v.phone=contact.soDienThoai;v.field=h.field;v.bio=h.bio;v.operation=operation;v.submitted=now();h.submitted=v.submitted;db.save(v);
    }
    private void pending(NguoiDung u,HoSoChuyenGia h,Long submittedBy){h.state="CHO_DUYET";u.setTrangThai("CHO_DUYET_CHUYEN_GIA");u.setLaKhachHang(false);u.setLaChuyenGia(false);u.setNgayCapNhat(now());u.setPhienBaoMat(u.getPhienBaoMat()+1);access.audit(submittedBy,"HoSoChuyenGia",h.userId,"GUI_XET_DUYET_CG",null);}
    public record Proof(Long userId,int version,int epoch,String state,Instant expires){}
    public Proof proof(Long userId){NguoiDung u=db.get(NguoiDung.class,userId);HoSoChuyenGia h=db.get(HoSoChuyenGia.class,userId);checkAccount(u);Checks.require(("CHO_DUYET".equals(h.state)&&"CHO_DUYET_CHUYEN_GIA".equals(u.getTrangThai()))||("TU_CHOI".equals(h.state)&&"TU_CHOI_CHUYEN_GIA".equals(u.getTrangThai())),"Hồ sơ không ở trạng thái phù hợp.");return new Proof(userId,h.version,u.getPhienBaoMat(),h.state,clock.instant().plusSeconds(900));}
    public void validateProof(Proof p,boolean rejected){Checks.require(p!=null&&clock.instant().isBefore(p.expires()),"Phiên truy cập đã hết hạn. Vui lòng đăng nhập lại.");Proof current=proof(p.userId());Checks.require(p.version()==current.version()&&p.epoch()==current.epoch()&&p.state().equals(current.state())&&(!rejected||"TU_CHOI".equals(current.state())),"Hồ sơ đã thay đổi. Vui lòng đăng nhập lại.");}
    private void checkAccount(NguoiDung u){Checks.require("CHUYEN_GIA".equals(u.getNguonDangKy())&&"CHINH_THUC".equals(u.getLoaiTaiKhoan())&&!u.getLaQuanTriVien()&&!u.getLaKhachHang()&&!u.getLaChuyenGia()&&"DA_XAC_THUC".equals(u.getTrangThaiEmail())&&u.getThoiDiemXacThucEmail()!=null,"Tài khoản không đủ điều kiện xét duyệt.");}
    public record Detail(HoSoChuyenGia application,PhienBanHoSoChuyenGia current,List<PhienBanHoSoChuyenGia> versions,List<QuyetDinhChuyenGia> decisions){}
    private Detail detail(Long id){HoSoChuyenGia h=db.get(HoSoChuyenGia.class,id);List<PhienBanHoSoChuyenGia> versions=db.list("from PhienBanHoSoChuyenGia v where v.userId=:u order by v.version desc",PhienBanHoSoChuyenGia.class,"u",id);return new Detail(h,versions.isEmpty()?null:versions.getFirst(),versions,db.list("from QuyetDinhChuyenGia q where q.userId=:u order by q.version desc",QuyetDinhChuyenGia.class,"u",id));}
    @Transactional(readOnly=true) public Detail privateDetail(Proof p){validateProof(p,false);return detail(p.userId());}
    @Transactional(readOnly=true) public Detail adminDetail(Long admin,Long id){access.admin(admin);return detail(id);}
    @Transactional(readOnly=true) public List<PhienBanHoSoChuyenGia> queue(Long admin){access.admin(admin);return db.list("select v from PhienBanHoSoChuyenGia v, HoSoChuyenGia h where v.userId=h.userId and v.version=h.version and h.state='CHO_DUYET' order by v.submitted,v.userId",PhienBanHoSoChuyenGia.class);}
    @Transactional(readOnly=true) public List<PhienBanHoSoChuyenGia> processed(Long admin){access.admin(admin);return db.list("select v from PhienBanHoSoChuyenGia v, HoSoChuyenGia h where v.userId=h.userId and v.version=h.version and h.state in ('DA_DUYET','TU_CHOI') order by v.submitted desc,v.userId",PhienBanHoSoChuyenGia.class);}
    @Transactional(readOnly=true) public List<NguoiDung> transitional(Long admin){access.admin(admin);return db.list("from NguoiDung u where u.laChuyenGia=true and u.nguonDangKy is null and not exists(select h.userId from HoSoChuyenGia h where h.userId=u.maNguoiDung) order by u.maNguoiDung",NguoiDung.class);}
    public void decide(Long admin,Long id,int version,boolean accepted,String reason){
        for(Long user:new TreeSet<>(List.of(admin,id)))db.lockEntity(NguoiDung.class,user);access.admin(admin);Checks.require(!admin.equals(id),"Không tự duyệt hồ sơ.");
        NguoiDung u=db.get(NguoiDung.class,id);HoSoChuyenGia h=db.lockEntity(HoSoChuyenGia.class,id);
        String why=accepted?null:Checks.text(reason,1,2000,"Lý do từ chối");
        Checks.require(version==h.version,"Phiên bản hồ sơ đã thay đổi. Vui lòng tải lại.");
        var previous=db.first("from QuyetDinhChuyenGia q where q.userId=:u and q.version=:v",QuyetDinhChuyenGia.class,"u",id,"v",version);
        if(previous.isPresent()){var q=previous.get();Checks.require(q.admin.equals(admin)&&q.accepted==accepted&&Objects.equals(q.reason,why),"Phiên bản này đã có quyết định.");return;}
        checkAccount(u);Checks.require("CHO_DUYET".equals(h.state)&&"CHO_DUYET_CHUYEN_GIA".equals(u.getTrangThai()),"Hồ sơ không còn chờ xét duyệt.");
        PhienBanHoSoChuyenGia v=detail(id).current();Checks.require(v!=null&&u.getEmail().equals(v.email),"Email hồ sơ không còn phù hợp.");
        recordDecision(admin,h,accepted,why);
        h.state=accepted?"DA_DUYET":"TU_CHOI";u.setTrangThai(accepted?"HOAT_DONG":"TU_CHOI_CHUYEN_GIA");u.setLaChuyenGia(accepted);u.setLaKhachHang(false);u.setPhienBaoMat(u.getPhienBaoMat()+1);u.setNgayCapNhat(now());
        access.audit(admin,"HoSoChuyenGia",id,accepted?"DUYET_CHUYEN_GIA":"TU_CHOI_CHUYEN_GIA",why);
    }
    private void recordDecision(Long admin,HoSoChuyenGia h,boolean accepted,String reason){
        QuyetDinhChuyenGia q=new QuyetDinhChuyenGia();q.userId=h.userId;q.version=h.version;q.admin=admin;q.accepted=accepted;q.reason=reason;q.decided=now();db.save(q);
    }
    public ExpertRegistrationForm edit(Proof p){validateProof(p,true);NguoiDung u=db.get(NguoiDung.class,p.userId());HoSoChuyenGia h=db.get(HoSoChuyenGia.class,p.userId());ExpertRegistrationForm f=new ExpertRegistrationForm();f.setHoTen(u.getHoTen());f.setEmail(u.getEmail());f.setSoDienThoai(db.get(HoSoKhachHang.class,p.userId()).soDienThoai);f.setLinhVuc(h.field);f.setGioiThieu(h.bio);return f;}
    public void resubmit(Proof p,String operation,ExpertRegistrationForm f){
        Checks.require(p!=null&&clock.instant().isBefore(p.expires()),"Phiên chỉnh sửa đã hết hạn.");NguoiDung u=db.lockEntity(NguoiDung.class,p.userId());HoSoChuyenGia h=db.lockEntity(HoSoChuyenGia.class,p.userId());
        // A consumed ticket can only repeat the identical operation; it cannot write another version.
        if(db.count("select count(v) from PhienBanHoSoChuyenGia v where v.userId=:u and v.operation=:o","u",p.userId(),"o",operation)>0)return;
        validateProof(p,true);professional(f);Checks.text(f.getHoTen(),1,100,"Họ tên");Checks.require(f.getSoDienThoai()!=null&&f.getSoDienThoai().strip().matches("^0[35789][0-9]{8}$"),"Số điện thoại di động Việt Nam phải gồm 10 chữ số.");
        u.setHoTen(f.getHoTen().strip());HoSoKhachHang contact=db.get(HoSoKhachHang.class,p.userId());contact.soDienThoai=f.getSoDienThoai().strip();contact.gioiThieu=f.getGioiThieu().strip();h.field=f.getLinhVuc().strip();h.bio=f.getGioiThieu().strip();snapshot(u,h,operation);pending(u,h,p.userId());
    }
}
