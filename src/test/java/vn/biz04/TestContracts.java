package vn.biz04;
import vn.biz04.data.Store;
import vn.biz04.domain.*;
import java.math.BigDecimal;
/** Test-only pre-V7 records. Production creation must use an agreed plan and reviewed ticket. */
final class TestContracts {
 static HopDong legacy(Store db,Long proposal){var d=db.get(DeXuat.class,proposal);var h=new HopDong();h.setMaYeuCau(d.getMaYeuCau());h.setMaDeXuat(proposal);h.setNoiDungThoaThuan(d.getPhuongAn());h.setTongGiaTri(d.getGiaDeXuat());h.setTyLePhi(new BigDecimal("0.1000"));h.setPhienBan(1);h.setTrangThai("CHO_XAC_NHAN");db.save(h);return h;}
}
