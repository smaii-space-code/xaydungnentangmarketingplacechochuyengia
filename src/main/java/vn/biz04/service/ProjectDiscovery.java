package vn.biz04.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.util.UriComponentsBuilder;
import vn.biz04.data.Store;
import vn.biz04.domain.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/** Danh sách cơ hội đọc cùng YeuCau; gửi đề xuất vẫn qua CatalogService. */
@Service @Transactional(readOnly=true)
public class ProjectDiscovery {
    public static final int PAGE_SIZE=9;
    private final Store db;private final Access access;private final EntityManager em;
    public ProjectDiscovery(Store db,Access access,EntityManager em){this.db=db;this.access=access;this.em=em;}
    public record Filter(String q,String field,String min,String max,int page){
        public static Filter parse(String q,String field,String min,String max,String page){
            q=q==null?"":q.strip();field=field==null?"":field.strip();min=min==null?"":min.strip();max=max==null?"":max.strip();
            Checks.require(q.length()<=200,"Từ khóa tìm kiếm tối đa 200 ký tự.");Checks.require(field.length()<=100,"Lĩnh vực không hợp lệ.");
            BigDecimal low=amount(min),high=amount(max);Checks.require(low==null||high==null||low.compareTo(high)<=0,"Ngân sách từ không được lớn hơn ngân sách đến.");
            Checks.require(page!=null&&page.matches("[0-9]{1,6}"),"Số trang không hợp lệ.");int number=Integer.parseInt(page);Checks.require(number>=1&&number<=100000,"Số trang từ 1 đến 100000.");
            return new Filter(q,field,min,max,number);
        }
        private static BigDecimal amount(String value){if(value.isEmpty())return null;Checks.require(value.matches("[0-9]{1,13}"),"Ngân sách phải là số nguyên không âm, tối đa 1.000 tỷ đồng.");BigDecimal n=new BigDecimal(value);Checks.require(n.compareTo(new BigDecimal("1000000000000"))<=0,"Ngân sách tối đa 1.000 tỷ đồng.");return n;}
        public String url(int number){return UriComponentsBuilder.fromPath("/tim-du-an").queryParam("q",q).queryParam("linhVuc",field).queryParam("tu",min).queryParam("den",max).queryParam("trang",number).build().encode().toUriString();}
    }
    public record Item(Long id,String title,String description,String field,BigDecimal budget,LocalDateTime due,LocalDateTime posted,String customer,boolean proposed){}
    public record Page(List<Item> items,long total,int number,int pages,String previous,String next,List<String> fields){}
    private static final String FROM=" from YeuCau y left join y.maDichVuLienKet d join y.maKhachHangLienKet u left join HoSoKhachHang h on h.maNguoiDung=u.maNguoiDung ";
    private static final String ELIGIBLE=" y.trangThai='DANG_MO' and u.trangThai='HOAT_DONG' and u.laKhachHang=true and y.maKhachHang<>:viewer and (y.maDichVu is null or d.maChuyenGia=:viewer) ";
    private static final String POSTED="(select min(n.thoiDiem) from NhatKyXuLy n where n.loaiDoiTuong='YeuCau' and n.maDoiTuong=y.maYeuCau and n.hanhDong='DANG_YEU_CAU')";
    public NguoiDung requireExpert(Long id){NguoiDung u=access.user(id);if(!u.getLaChuyenGia()||!ExpertEligibility.eligible(db,u))throw new AccessDeniedException("Chức năng này dành cho chuyên gia.");return u;}
    public boolean eligible(YeuCau y,Long viewer){
        if(!"DANG_MO".equals(y.getTrangThai())||Objects.equals(y.getMaKhachHang(),viewer))return false;
        NguoiDung owner=db.get(NguoiDung.class,y.getMaKhachHang());
        return "HOAT_DONG".equals(owner.getTrangThai())&&owner.getLaKhachHang()&&(y.getMaDichVu()==null||Objects.equals(db.get(DichVu.class,y.getMaDichVu()).getMaChuyenGia(),viewer));
    }
    private <T> TypedQuery<T> query(String hql,Class<T> type,Map<String,Object> params){var q=em.createQuery(hql,type);params.forEach(q::setParameter);return q;}
    public Page find(Long viewer,Filter filter){
        requireExpert(viewer);Map<String,Object> params=new LinkedHashMap<>();params.put("viewer",viewer);
        List<String> fields=query("select distinct d.linhVuc"+FROM+"where "+ELIGIBLE+" and d.linhVuc is not null order by d.linhVuc",String.class,params).getResultList();
        Checks.require(filter.field().isEmpty()||"_NONE".equals(filter.field())||fields.contains(filter.field()),"Lĩnh vực không có trong danh sách lọc.");
        StringBuilder where=new StringBuilder("where "+ELIGIBLE);
        if(!filter.q().isEmpty()){where.append(" and (lower(y.tieuDe) like :term escape '!' or lower(y.moTa) like :term escape '!')");String term=filter.q().toLowerCase(Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_").replace("[","![");params.put("term","%"+term+"%");}
        if("_NONE".equals(filter.field()))where.append(" and d.linhVuc is null");else if(!filter.field().isEmpty()){where.append(" and d.linhVuc=:field");params.put("field",filter.field());}
        if(!filter.min().isEmpty()){where.append(" and y.nganSach>=:minimum");params.put("minimum",Filter.amount(filter.min()));}
        if(!filter.max().isEmpty()){where.append(" and y.nganSach<=:maximum");params.put("maximum",Filter.amount(filter.max()));}
        long total=query("select count(y)"+FROM+where,Long.class,params).getSingleResult();int pages=(int)Math.max(1,Math.min(Integer.MAX_VALUE,(total+PAGE_SIZE-1)/PAGE_SIZE));int number=Math.min(filter.page(),pages);
        String select="select y,d.linhVuc,u.hoTen,h.loaiKhachHang,h.tenToChuc,"+POSTED+FROM+where+" order by case when "+POSTED+" is null then 1 else 0 end,"+POSTED+" desc,y.maYeuCau desc";
        var rows=query(select,Object[].class,params).setFirstResult((number-1)*PAGE_SIZE).setMaxResults(PAGE_SIZE).getResultList();
        Set<Long> proposed=new HashSet<>();if(!rows.isEmpty()){List<Long> ids=rows.stream().map(r->((YeuCau)r[0]).getMaYeuCau()).toList();proposed.addAll(db.list("select d.maYeuCau from DeXuat d where d.maChuyenGia=:u and d.trangThai='DANG_GUI' and d.maYeuCau in :ids",Long.class,"u",viewer,"ids",ids));}
        List<Item> items=rows.stream().map(r->item(r,proposed)).toList();return new Page(items,total,number,pages,number>1?filter.url(number-1):null,number<pages?filter.url(number+1):null,fields);
    }
    private Item item(Object[] r,Set<Long> proposed){YeuCau y=(YeuCau)r[0];String customer="TO_CHUC".equals(r[3])&&r[4]!=null&&!((String)r[4]).isBlank()?(String)r[4]:(String)r[2];return new Item(y.getMaYeuCau(),y.getTieuDe(),y.getMoTa(),(String)r[1],y.getNganSach(),y.getHanDuKien(),(LocalDateTime)r[5],customer,proposed.contains(y.getMaYeuCau()));}
    public Item details(Long request,Long viewer){
        var rows=query("select y,d.linhVuc,u.hoTen,h.loaiKhachHang,h.tenToChuc,"+POSTED+FROM+" where y.maYeuCau=:id",Object[].class,Map.of("id",request)).getResultList();
        Checks.require(!rows.isEmpty(),"Không tìm thấy yêu cầu.");boolean sent=db.count("select count(d) from DeXuat d where d.maYeuCau=:y and d.maChuyenGia=:u and d.trangThai='DANG_GUI'","y",request,"u",viewer)>0;return item(rows.get(0),sent?Set.of(request):Set.of());
    }
}
