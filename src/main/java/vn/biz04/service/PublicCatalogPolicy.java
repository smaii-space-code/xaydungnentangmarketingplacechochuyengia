package vn.biz04.service;
import vn.biz04.domain.*;

/** One publication policy shared by every catalogue projection. Email proves mailbox control only. */
public final class PublicCatalogPolicy {
    private PublicCatalogPolicy(){}
    public static String approved(String alias){return alias+".trangThai='DANG_HIEN' and "+alias+".trangThaiDuyet='DA_DUYET' and "+alias+".phienBanDuyet="+alias+".phienBanNoiDung and "+alias+".xacNhanNoiDung=true and "+alias+".canRaSoatLai=false";}
    public static final String ACCOUNT="u.laChuyenGia=true and u.trangThai='HOAT_DONG' and u.loaiTaiKhoan='CHINH_THUC' and u.trangThaiEmail='DA_XAC_THUC' and u.thoiDiemXacThucEmail is not null and trim(u.hoTen)<>'' and "+ExpertEligibility.hql("u");
    public static final String EXPERT=ACCOUNT+" and exists(select ps.maDichVu from DichVu ps where ps.maChuyenGia=u.maNguoiDung and "+approved("ps")+")";
    public static final String SERVICE=ACCOUNT+" and "+approved("d");
    public static boolean official(NguoiDung u){return "HOAT_DONG".equals(u.getTrangThai())&&"CHINH_THUC".equals(u.getLoaiTaiKhoan())&&"DA_XAC_THUC".equals(u.getTrangThaiEmail())&&u.getThoiDiemXacThucEmail()!=null;}
    public static boolean approved(DichVu d){return "DA_DUYET".equals(d.getTrangThaiDuyet())&&Integer.valueOf(d.getPhienBanNoiDung()).equals(d.getPhienBanDuyet())&&d.isXacNhanNoiDung()&&!d.isCanRaSoatLai();}
}
