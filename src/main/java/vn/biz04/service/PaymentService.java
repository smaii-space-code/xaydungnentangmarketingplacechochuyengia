package vn.biz04.service;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.biz04.data.Store;
import vn.biz04.domain.*;
import java.math.*;
import java.util.*;
@Service
@Transactional
public class PaymentService {
    private final Store db;private final Access access;private final ContractService contracts;private final PaymentGateway gateway;
    public PaymentService(Store db,Access access,ContractService contracts,PaymentGateway gateway){this.db=db;this.access=access;this.contracts=contracts;this.gateway=gateway;}
    public record Balance(BigDecimal nap,BigDecimal traChuyenGia,BigDecimal hoanKhach,BigDecimal phi,BigDecimal thucNhan,BigDecimal dangGiu){}
    public Balance balance(Long mid){BigDecimal in=BigDecimal.ZERO,out=BigDecimal.ZERO,refund=BigDecimal.ZERO,fee=BigDecimal.ZERO,net=BigDecimal.ZERO;for(GiaoDich g:db.list("from GiaoDich g where g.maMocNghiemThu=:m and g.trangThai='THANH_CONG'",GiaoDich.class,"m",mid)){switch(g.getLoaiGiaoDich()){case "KY_QUY"->in=in.add(g.getSoTien());case "GIAI_NGAN"->{out=out.add(g.getSoTien());fee=fee.add(g.getPhiNenTang());net=net.add(g.getThucNhan());}case "HOAN_TRA"->refund=refund.add(g.getSoTien());default->throw new BusinessException("Loại giao dịch không hợp lệ.");}}return new Balance(in,out,refund,fee,net,in.subtract(out).subtract(refund));}
    private GiaoDich operation(HopDong h,MocNghiemThu m,Long tc,Long actor,String kind,BigDecimal amount,String key,String outcome){
        Checks.money(amount,false);GiaoDich g=db.first("from GiaoDich g where g.khoaChongLap=:k",GiaoDich.class,"k",key).orElse(null);
        if(g!=null){Checks.require(Objects.equals(g.getMaMocNghiemThu(),m.getMaMocNghiemThu())&&g.getLoaiGiaoDich().equals(kind)&&g.getSoTien().compareTo(amount)==0&&Objects.equals(g.getMaTranhChap(),tc),"Khóa giao dịch không khớp nội dung.");if("THANH_CONG".equals(g.getTrangThai()))return g;}
        if(!"KY_QUY".equals(kind))Checks.require(balance(m.getMaMocNghiemThu()).dangGiu().compareTo(amount)>=0,"Không đủ ký quỹ để xử lý.");
        if(tc!=null)Checks.require(Objects.equals(db.get(TranhChap.class,tc).getMaMocNghiemThu(),m.getMaMocNghiemThu()),"Tranh chấp không thuộc mốc.");
        if(g==null){g=new GiaoDich();g.setMaMocNghiemThu(m.getMaMocNghiemThu());g.setMaTranhChap(tc);g.setMaNguoiYeuCau(actor);g.setLoaiGiaoDich(kind);g.setSoTien(amount);g.setKhoaChongLap(key);g.setTrangThai("DANG_XU_LY");g.setPhiNenTang(BigDecimal.ZERO);g.setThucNhan(BigDecimal.ZERO);db.save(g);}
        boolean ok=gateway.execute(key,outcome);g.setTrangThai(ok?"THANH_CONG":"THAT_BAI");g.setNgayHoanTat(access.now());
        if(ok){BigDecimal fee="GIAI_NGAN".equals(kind)?amount.multiply(h.getTyLePhi()).setScale(0,RoundingMode.HALF_UP):BigDecimal.ZERO;g.setPhiNenTang(fee);g.setThucNhan("KY_QUY".equals(kind)?BigDecimal.ZERO:amount.subtract(fee));g.setMaThamChieuCong("MO-PHONG-"+g.getMaGiaoDich());}
        access.audit(actor,"GiaoDich",g.getMaGiaoDich(),ok?"GIAO_DICH_THANH_CONG":"GIAO_DICH_THAT_BAI",kind+"; "+key);return g;
    }
    public GiaoDich fund(Long actor,Long mid,String outcome){MocNghiemThu m=db.get(MocNghiemThu.class,mid);HopDong h=db.lock(m.getMaHopDong());db.refresh(m);access.member(h,actor);Checks.require(Objects.equals(actor,access.customer(h)),"Chỉ khách hàng nạp ký quỹ.");
        Optional<GiaoDich> old=db.first("from GiaoDich g where g.khoaChongLap=:k and g.trangThai='THANH_CONG'",GiaoDich.class,"k","KQ:"+mid);if(old.isPresent()){contracts.recordFirstFunding(h,m,old.get());return old.get();}
        contracts.requireFundingConsent(h);
        Checks.require("DANG_THUC_HIEN".equals(h.getTrangThai())&&"CHO_KY_QUY".equals(m.getTrangThai())&&!access.openDispute(mid),"Mốc chưa được phép ký quỹ.");
        for(MocNghiemThu p:contracts.milestones(h.getMaHopDong()))if(p.getThuTu()<m.getThuTu())Checks.require("DA_TAT_TOAN".equals(p.getTrangThai()),"Mốc trước phải tất toán trước khi ký quỹ mốc này.");
        GiaoDich g=operation(h,m,null,actor,"KY_QUY",m.getSoTien(),"KQ:"+mid,outcome);if("THANH_CONG".equals(g.getTrangThai())){m.setTrangThai("DANG_THUC_HIEN");contracts.recordFirstFunding(h,m,g);}return g;
    }
    public boolean release(Long actor,Long mid,String outcome){MocNghiemThu m=db.get(MocNghiemThu.class,mid);HopDong h=db.lock(m.getMaHopDong());db.refresh(m);if(actor!=null){access.member(h,actor);Checks.require(Objects.equals(actor,access.customer(h)),"Chỉ khách hàng yêu cầu tất toán sau nghiệm thu.");}
        if("DA_TAT_TOAN".equals(m.getTrangThai()))return true;
        Checks.require("DANG_THUC_HIEN".equals(h.getTrangThai())&&"DA_NGHIEM_THU".equals(m.getTrangThai())&&!access.openDispute(mid),"Mốc chưa đủ điều kiện tất toán.");
        GiaoDich g=operation(h,m,null,actor,"GIAI_NGAN",m.getSoTien(),"GN:"+mid,outcome);if("THANH_CONG".equals(g.getTrangThai())){m.setTrangThai("DA_TAT_TOAN");contracts.completeIfSettled(h);return true;}return false;
    }
    public boolean allocate(HopDong h,MocNghiemThu m,Long tc,Long actor,BigDecimal expert,BigDecimal refund,String prefix,String mode){
        Checks.require(Set.of("SUCCESS","FAIL","FAIL_REFUND").contains(mode),"Kết quả mô phỏng không hợp lệ.");
        if(expert.signum()>0&&!"THANH_CONG".equals(operation(h,m,tc,actor,"GIAI_NGAN",expert,prefix+":PAY","FAIL".equals(mode)?"FAIL":"SUCCESS").getTrangThai()))return false;
        if(refund.signum()>0&&!"THANH_CONG".equals(operation(h,m,tc,actor,"HOAN_TRA",refund,prefix+":REFUND","FAIL".equals(mode)||"FAIL_REFUND".equals(mode)?"FAIL":"SUCCESS").getTrangThai()))return false;
        Checks.require(balance(m.getMaMocNghiemThu()).dangGiu().signum()==0,"Còn tiền chưa được tất toán.");m.setTrangThai("DA_TAT_TOAN");return true;
    }
}
