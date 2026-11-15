package vn.biz04.service;
import vn.biz04.data.Store;
import vn.biz04.domain.NguoiDung;

/** Legacy entitlement is distinct from a recorded decision. Does not grant any role. */
public final class ExpertEligibility {
    private ExpertEligibility(){}
    public static String hql(String u){return "(("+u+".nguonDangKy is null and not exists(select h.userId from HoSoChuyenGia h where h.userId="+u+".maNguoiDung)) or ("+u+".nguonDangKy='CHUYEN_GIA' and "+u+".trangThaiEmail='DA_XAC_THUC' and "+u+".thoiDiemXacThucEmail is not null and exists(select h.userId from HoSoChuyenGia h, QuyetDinhChuyenGia q where h.userId="+u+".maNguoiDung and h.state='DA_DUYET' and q.userId=h.userId and q.version=h.version and q.accepted=true)))";}
    public static boolean eligible(Store db,NguoiDung u){return db.count("select count(u) from NguoiDung u where u.maNguoiDung=:id and "+hql("u"),"id",u.getMaNguoiDung())==1;}
}
