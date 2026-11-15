package vn.biz04.service;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.biz04.data.Store;
import vn.biz04.domain.*;
import java.math.*;
import java.util.*;
@Service
@Transactional
public class TerminationService {
    private final Store db;private final Access access;private final ContractService contracts;private final PaymentService payments;private final DisputeService disputes;
    public TerminationService(Store db,Access access,ContractService contracts,PaymentService payments,DisputeService disputes){this.db=db;this.access=access;this.contracts=contracts;this.payments=payments;this.disputes=disputes;}
    public YeuCauChamDut propose(Long actor,Long id,String reason,int expertPercent){HopDong h=db.lock(id);access.member(h,actor);Checks.require("DANG_THUC_HIEN".equals(h.getTrangThai()),"Hợp đồng chưa cho phép đề nghị chấm dứt.");Checks.require(expertPercent>=0&&expertPercent<=100,"Tỷ lệ chuyên gia từ 0 đến 100%.");Checks.require(db.count("select count(y) from YeuCauChamDut y where y.maHopDong=:h and y.trangThai='CHO_PHAN_HOI'","h",id)==0,"Đang có một đề nghị chờ phản hồi.");
        MocNghiemThu current=null;BigDecimal held=BigDecimal.ZERO;for(MocNghiemThu m:contracts.milestones(id)){BigDecimal q=payments.balance(m.getMaMocNghiemThu()).dangGiu();if(q.signum()>0){Checks.require(current==null,"Có nhiều mốc đang giữ tiền, cần kiểm tra dữ liệu.");current=m;held=q;}}
        BigDecimal expert=held.multiply(BigDecimal.valueOf(expertPercent)).divide(BigDecimal.valueOf(100),0,RoundingMode.HALF_UP);YeuCauChamDut y=new YeuCauChamDut();y.setMaHopDong(id);y.setMaMocNghiemThu(current==null?null:current.getMaMocNghiemThu());y.setMaNguoiDeNghi(actor);y.setLyDo(Checks.text(reason,5,5000,"Lý do chấm dứt"));y.setTrangThai("CHO_PHAN_HOI");y.setTienChuyenGia(expert);y.setTienHoanKhach(held.subtract(expert));y.setNgayDeNghi(access.now());y.setHanPhanHoi(access.now().plusDays(3));db.save(y);h.setTrangThai("CHO_CHAM_DUT");access.audit(actor,"YeuCauChamDut",y.getMaYeuCauChamDut(),"DE_NGHI_CHAM_DUT",reason);return y;
    }
    public boolean respond(Long actor,Long id,boolean accept,String mode){YeuCauChamDut y=db.get(YeuCauChamDut.class,id);HopDong h=db.lock(y.getMaHopDong());db.refresh(y);access.member(h,actor);Checks.require(!Objects.equals(actor,y.getMaNguoiDeNghi()),"Bên còn lại mới được chấp nhận đề nghị.");if("DA_HOAN_TAT".equals(y.getTrangThai()))return true;
        Checks.require("CHO_CHAM_DUT".equals(h.getTrangThai())&&"CHO_PHAN_HOI".equals(y.getTrangThai()),"Đề nghị không còn chờ xử lý.");
        if(!accept){Checks.require(y.getMaNguoiDongY()==null,"Hai bên đã đồng ý phân bổ; chỉ được thử lại giao dịch.");y.setNgayPhanHoi(access.now());y.setTrangThai("BI_TU_CHOI");if(y.getMaMocNghiemThu()!=null){TranhChap t=disputes.open(actor,y.getMaMocNghiemThu(),"Không thống nhất chấm dứt: "+y.getLyDo());y.setTrangThai("CHO_GIAI_QUYET");access.audit(actor,"YeuCauChamDut",id,"CHUYEN_TRANH_CHAP",String.valueOf(t.getMaTranhChap()));}else h.setTrangThai("DANG_THUC_HIEN");return false;}
        y.setMaNguoiDongY(actor);y.setNgayPhanHoi(access.now());boolean ok=y.getMaMocNghiemThu()==null||payments.allocate(h,db.get(MocNghiemThu.class,y.getMaMocNghiemThu()),null,actor,y.getTienChuyenGia(),y.getTienHoanKhach(),"CD:"+id,mode);
        if(ok){y.setTrangThai("DA_HOAN_TAT");contracts.terminate(h);}access.audit(actor,"YeuCauChamDut",id,ok?"CHAM_DUT_DONG_THUAN":"CHO_THU_LAI_GIAO_DICH",null);return ok;
    }
}
