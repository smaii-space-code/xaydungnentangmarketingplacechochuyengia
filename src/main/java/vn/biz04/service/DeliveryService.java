package vn.biz04.service;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.biz04.data.Store;
import vn.biz04.domain.*;
import java.util.*;
@Service
@Transactional
public class DeliveryService {
    private final Store db;private final Access access;private final PaymentService payments;
    public DeliveryService(Store db,Access access,PaymentService payments){this.db=db;this.access=access;this.payments=payments;}
    public BanGiao deliver(Long actor,Long mid,String description,String product){MocNghiemThu m=db.get(MocNghiemThu.class,mid);HopDong h=db.lock(m.getMaHopDong());db.refresh(m);access.member(h,actor);Checks.require(Objects.equals(access.expert(h),actor),"Chỉ chuyên gia của hợp đồng được bàn giao.");Checks.require("DANG_THUC_HIEN".equals(h.getTrangThai())&&Set.of("DANG_THUC_HIEN","YEU_CAU_CHINH_SUA").contains(m.getTrangThai())&&!access.openDispute(mid),"Mốc chưa được phép bàn giao.");Checks.require(payments.balance(mid).dangGiu().compareTo(m.getSoTien())==0,"Mốc chưa có đủ ký quỹ.");
        Checks.require(product!=null&&(product.matches("file:[a-zA-Z0-9.\\-]+")||product.startsWith("https://")||product.startsWith("http://")),"Cần tệp hoặc đường dẫn HTTP/HTTPS hợp lệ.");
        BanGiao b=new BanGiao();b.setMaMocNghiemThu(mid);b.setLanBanGiao((int)db.count("select count(b) from BanGiao b where b.maMocNghiemThu=:m","m",mid)+1);b.setMoTa(Checks.text(description,5,10000,"Mô tả bàn giao"));b.setDuongDanSanPham(Checks.text(product,5,2000,"Sản phẩm"));b.setNgayGui(access.now());b.setKetQuaXuLy("CHO_XU_LY");db.save(b);m.setTrangThai("CHO_NGHIEM_THU");m.setHanNghiemThu(access.now().plusDays(14));access.audit(actor,"BanGiao",b.getMaBanGiao(),"BAN_GIAO",null);return b;
    }
    public BanGiao latest(Long mid){return db.first("from BanGiao b where b.maMocNghiemThu=:m order by b.lanBanGiao desc",BanGiao.class,"m",mid).orElseThrow(()->new BusinessException("Chưa có bàn giao."));}
    public boolean accept(Long actor,Long mid,String mode){MocNghiemThu m=db.get(MocNghiemThu.class,mid);HopDong h=db.lock(m.getMaHopDong());db.refresh(m);if(actor!=null){access.member(h,actor);Checks.require(Objects.equals(actor,access.customer(h)),"Chỉ khách hàng nghiệm thu.");}
        Checks.require("DANG_THUC_HIEN".equals(h.getTrangThai())&&"CHO_NGHIEM_THU".equals(m.getTrangThai())&&!access.openDispute(mid),"Mốc không chờ nghiệm thu hoặc đang bị khóa.");BanGiao b=latest(mid);Checks.require("CHO_XU_LY".equals(b.getKetQuaXuLy()),"Bản bàn giao đã được xử lý.");b.setKetQuaXuLy("DA_CHAP_NHAN");b.setNgayXuLy(access.now());m.setTrangThai("DA_NGHIEM_THU");m.setNgayNghiemThu(access.now());access.audit(actor,"MocNghiemThu",mid,"NGHIEM_THU",null);return payments.release(actor,mid,mode);
    }
    public void revise(Long actor,Long mid,String reason){MocNghiemThu m=db.get(MocNghiemThu.class,mid);HopDong h=db.lock(m.getMaHopDong());db.refresh(m);access.member(h,actor);Checks.require(Objects.equals(actor,access.customer(h)),"Chỉ khách hàng yêu cầu sửa.");Checks.require("DANG_THUC_HIEN".equals(h.getTrangThai())&&"CHO_NGHIEM_THU".equals(m.getTrangThai())&&!access.openDispute(mid),"Mốc không còn chờ nghiệm thu.");Checks.require(m.getHanNghiemThu()!=null&&access.now().isBefore(m.getHanNghiemThu()),"Đã hết thời hạn phản hồi nghiệm thu.");Checks.require(m.getSoLanChinhSuaToiDa()==null||db.count("select count(b) from BanGiao b where b.maMocNghiemThu=:m and b.ketQuaXuLy='YEU_CAU_SUA'","m",mid)<m.getSoLanChinhSuaToiDa(),"Đã hết số lần chỉnh sửa trong phạm vi. Hãy rà soát nghiệm thu hoặc xử lý bất đồng qua luồng hiện có.");BanGiao b=latest(mid);b.setKetQuaXuLy("YEU_CAU_SUA");b.setNoiDungYeuCauSua(Checks.text(reason,5,5000,"Yêu cầu sửa"));b.setNgayXuLy(access.now());m.setTrangThai("YEU_CAU_CHINH_SUA");m.setHanNghiemThu(null);access.audit(actor,"BanGiao",b.getMaBanGiao(),"YEU_CAU_SUA",reason);}
    public void autoAccept(Long mid){MocNghiemThu m=db.get(MocNghiemThu.class,mid);HopDong h=db.lock(m.getMaHopDong());db.refresh(m);if("DANG_THUC_HIEN".equals(h.getTrangThai())&&"CHO_NGHIEM_THU".equals(m.getTrangThai())&&m.getHanNghiemThu()!=null&&!access.now().isBefore(m.getHanNghiemThu())&&!access.openDispute(mid)&&"CHO_XU_LY".equals(latest(mid).getKetQuaXuLy()))accept(null,mid,"SUCCESS");}
}
