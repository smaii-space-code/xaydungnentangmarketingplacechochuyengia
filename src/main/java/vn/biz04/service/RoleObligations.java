package vn.biz04.service;
import org.springframework.stereotype.Component;
import vn.biz04.data.Store;
import vn.biz04.domain.*;
import java.math.BigDecimal;
import java.util.*;

@Component
public class RoleObligations {
    private final Store db;
    public RoleObligations(Store db){this.db=db;}
    private static final Set<String> CLOSED=Set.of("HOAN_THANH","DA_HUY","DA_CHAM_DUT");
    public boolean outstanding(Long id,boolean customer){
        return blocker(id,customer).isPresent();
    }
    public Optional<String> blocker(Long id,boolean customer){
        if(db.count("select count(h) from DeNghiThue h where h."+(customer?"customer":"expert")+"=:u and h.state in ('CHO_PHAN_HOI','DA_THONG_NHAT') and not exists(select p.id from KeHoachMoc p where p.hire=h.id and (p.state='DA_HUY' or exists(select c.maHopDong from HopDong c where c.maKeHoach=p.id)))","u",id)>0)return Optional.of("Còn đề nghị thuê đang chờ phản hồi hoặc chờ hoàn tất kế hoạch mốc.");
        if(customer&&db.count("select count(y) from YeuCau y where y.maKhachHang=:u and y.trangThai in ('DANG_MO','DANG_THUONG_LUONG')","u",id)>0)return Optional.of("Còn yêu cầu công việc đang mở hoặc đang thương lượng.");
        if(!customer&&db.count("select count(d) from DeXuat d,YeuCau y where d.maYeuCau=y.maYeuCau and d.maChuyenGia=:u and d.trangThai='DANG_GUI' and y.trangThai='DANG_MO'","u",id)>0)return Optional.of("Còn đề xuất đang gửi.");
        if(db.count("select count(p) from KeHoachMoc p where p."+(customer?"customer":"expert")+"=:u and p.state in ('CHO_PHAN_HOI','DA_THONG_NHAT') and not exists(select c.maHopDong from HopDong c where c.maKeHoach=p.id)","u",id)>0)return Optional.of("Còn kế hoạch mốc đang thương lượng hoặc chờ lập hợp đồng.");
        if(!customer&&db.count("select count(d) from DeXuat d,YeuCau y where d.maYeuCau=y.maYeuCau and d.maChuyenGia=:u and d.trangThai='DA_CHON' and y.trangThai in ('DANG_MO','DANG_THUONG_LUONG') and not exists(select h.maHopDong from HopDong h where h.maYeuCau=y.maYeuCau) and not exists(select p.id from KeHoachMoc p where p.proposal=d.maDeXuat and p.state='DA_HUY')","u",id)>0)return Optional.of("Còn đề xuất đã được chọn chưa hoàn tất.");
        String relation=customer?"y.maKhachHang=:u":"d.maChuyenGia=:u";
        List<HopDong> linked=new ArrayList<>(db.list("select h from HopDong h,YeuCau y,DeXuat d where h.maKeHoach is null and h.maYeuCau=y.maYeuCau and h.maDeXuat=d.maDeXuat and "+relation,HopDong.class,"u",id));linked.addAll(db.list("from HopDong h where h.maKeHoach is not null and h."+(customer?"maKhachHang":"maChuyenGia")+"=:u",HopDong.class,"u",id));
        for(HopDong h:linked){
            if(!CLOSED.contains(h.getTrangThai()))return Optional.of("Hợp đồng #"+h.getMaHopDong()+" chưa kết thúc.");
            for(MocNghiemThu m:db.list("from MocNghiemThu m where m.maHopDong=:h",MocNghiemThu.class,"h",h.getMaHopDong())){
                if(db.count("select count(t) from TranhChap t where t.maMocNghiemThu=:m and t.ngayDong is null","m",m.getMaMocNghiemThu())>0)return Optional.of("Mốc #"+m.getMaMocNghiemThu()+" còn tranh chấp mở.");
                BigDecimal held=BigDecimal.ZERO;
                for(GiaoDich g:db.list("from GiaoDich g where g.maMocNghiemThu=:m",GiaoDich.class,"m",m.getMaMocNghiemThu())){
                    if("DANG_XU_LY".equals(g.getTrangThai()))return Optional.of("Mốc #"+m.getMaMocNghiemThu()+" còn giao dịch đang xử lý.");
                    if("THANH_CONG".equals(g.getTrangThai()))held="KY_QUY".equals(g.getLoaiGiaoDich())?held.add(g.getSoTien()):held.subtract(g.getSoTien());
                }
                if(held.signum()!=0)return Optional.of("Mốc #"+m.getMaMocNghiemThu()+" còn số dư chưa tất toán.");
            }
        }
        return Optional.empty();
    }
}
