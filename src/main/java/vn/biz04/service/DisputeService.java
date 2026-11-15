package vn.biz04.service;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.biz04.data.Store;
import vn.biz04.domain.*;
import java.math.BigDecimal;
import java.util.*;
@Service
@Transactional
public class DisputeService {
    private final Store db;private final Access access;private final PaymentService payments;private final ContractService contracts;
    public DisputeService(Store db,Access access,PaymentService payments,ContractService contracts){this.db=db;this.access=access;this.payments=payments;this.contracts=contracts;}
    public TranhChap open(Long actor,Long mid,String reason){MocNghiemThu m=db.get(MocNghiemThu.class,mid);HopDong h=db.lock(m.getMaHopDong());db.refresh(m);access.member(h,actor);
        Checks.require(db.count("select count(y) from YeuCauChamDut y where y.maHopDong=:h and y.trangThai='CHO_PHAN_HOI' and y.maNguoiDongY is not null","h",h.getMaHopDong())==0,"Hai bên đã chốt phân bổ; cần hoàn tất hoặc thử lại giao dịch.");Checks.require(Set.of("DANG_THUC_HIEN","CHO_CHAM_DUT").contains(h.getTrangThai()),"Hợp đồng chưa cho phép mở tranh chấp.");Checks.require(!access.openDispute(mid)&&payments.balance(mid).dangGiu().signum()>0&&!Set.of("DA_HUY","DA_TAT_TOAN").contains(m.getTrangThai()),"Mốc không còn ký quỹ hoặc đã có tranh chấp chưa đóng.");
        TranhChap t=new TranhChap();t.setMaMocNghiemThu(mid);t.setMaNguoiMo(actor);t.setLyDo(Checks.text(reason,5,5000,"Lý do tranh chấp"));t.setTrangThai("DANG_XU_LY");t.setHanNopBangChung(access.now().plusDays(5));db.save(t);m.setTrangThai("DANG_TRANH_CHAP");h.setTrangThai("TAM_DUNG");for(YeuCauChamDut y:db.list("from YeuCauChamDut y where y.maHopDong=:h and y.trangThai='CHO_PHAN_HOI'",YeuCauChamDut.class,"h",h.getMaHopDong()))y.setTrangThai("CHO_GIAI_QUYET");access.audit(actor,"TranhChap",t.getMaTranhChap(),"MO_TRANH_CHAP",reason);return t;
    }
    public void evidence(Long actor,Long id,String content,String file){TranhChap t=db.get(TranhChap.class,id);MocNghiemThu m=db.get(MocNghiemThu.class,t.getMaMocNghiemThu());HopDong h=db.lock(m.getMaHopDong());db.refresh(m);db.refresh(t);access.member(h,actor);Checks.require(t.getNgayDong()==null&&t.getNgayKetLuan()==null,"Hồ sơ đã có kết luận hoặc đã đóng.");Checks.require(t.getHanNopBangChung()==null||access.now().isBefore(t.getHanNopBangChung()),"Đã hết hạn bằng chứng. Có thể đề nghị quản trị viên mở lại hạn bổ sung.");
        BangChungTranhChap b=new BangChungTranhChap();b.setMaTranhChap(id);b.setMaNguoiGui(actor);b.setLoaiNoiDung("BANG_CHUNG");b.setNoiDung(Checks.text(content,5,10000,"Bằng chứng"));b.setDuongDanTep(file);b.setNgayGui(access.now());db.save(b);access.audit(actor,"TranhChap",id,"NOP_BANG_CHUNG",null);
    }
    public void supplement(Long admin,Long id,String reason){access.admin(admin);TranhChap t=db.get(TranhChap.class,id);MocNghiemThu m=db.get(MocNghiemThu.class,t.getMaMocNghiemThu());HopDong h=db.lock(m.getMaHopDong());db.refresh(m);db.refresh(t);Checks.require(!access.party(h,admin)&&t.getNgayKetLuan()==null&&t.getNgayDong()==null,"Không thể bổ sung hồ sơ này.");t.setMaQuanTriVienXuLy(admin);t.setTrangThai("CHO_BO_SUNG");t.setHanNopBangChung(access.now().plusDays(5));BangChungTranhChap b=new BangChungTranhChap();b.setMaTranhChap(id);b.setMaNguoiGui(admin);b.setLoaiNoiDung("YEU_CAU_BO_SUNG");b.setNoiDung(Checks.text(reason,5,5000,"Nội dung bổ sung"));b.setNgayGui(access.now());db.save(b);access.audit(admin,"TranhChap",id,"YEU_CAU_BO_SUNG",reason);}
    public boolean decide(Long admin,Long id,String conclusion,BigDecimal expert,boolean resume,String mode){access.admin(admin);TranhChap t=db.get(TranhChap.class,id);MocNghiemThu m=db.get(MocNghiemThu.class,t.getMaMocNghiemThu());HopDong h=db.lock(m.getMaHopDong());db.refresh(m);db.refresh(t);Checks.require(!access.party(h,admin),"Quản trị viên không được xử lý tranh chấp của chính mình.");
        if(t.getNgayDong()!=null)return true;
        if(t.getNgayKetLuan()==null){Checks.money(expert,true);BigDecimal held=payments.balance(m.getMaMocNghiemThu()).dangGiu();Checks.require(expert.compareTo(held)<=0,"Phân bổ vượt ký quỹ còn giữ.");t.setKetLuan(Checks.text(conclusion,10,10000,"Kết luận"));t.setTienGiaiNgan(expert);t.setTienHoanTra(held.subtract(expert));t.setHuongXuLyHopDong(resume?"TIEP_TUC":"CHAM_DUT");t.setNgayKetLuan(access.now());t.setMaQuanTriVienXuLy(admin);}else Checks.require(Objects.equals(t.getMaQuanTriVienXuLy(),admin),"Quản trị viên đã kết luận cần thực hiện lại giao dịch này.");
        t.setTrangThai("DANG_THUC_THI");boolean ok=payments.allocate(h,m,id,admin,t.getTienGiaiNgan(),t.getTienHoanTra(),"TC:"+id,mode);
        if(ok){t.setTrangThai("DA_DONG");t.setNgayDong(access.now());for(YeuCauChamDut y:db.list("from YeuCauChamDut y where y.maHopDong=:h and y.trangThai='CHO_GIAI_QUYET'",YeuCauChamDut.class,"h",h.getMaHopDong()))y.setTrangThai("DA_HOAN_TAT");
            if("TIEP_TUC".equals(t.getHuongXuLyHopDong())){h.setTrangThai("CHO_TIEP_TUC");contracts.resumeVersion(h,admin);}else contracts.terminate(h);
        }access.audit(admin,"TranhChap",id,ok?"DONG_TRANH_CHAP":"CHO_THU_LAI_GIAO_DICH",t.getKetLuan());return ok;
    }
}
