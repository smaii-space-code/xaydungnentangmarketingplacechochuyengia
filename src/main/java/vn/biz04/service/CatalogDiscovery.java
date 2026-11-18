package vn.biz04.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.web.util.UriComponentsBuilder;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/** Read-only projections of the existing expert accounts, profiles and services. */
@Service @Transactional(readOnly=true)
public class CatalogDiscovery {
    public static final int PAGE_SIZE=6, HOME_SIZE=3, RELATED_LIMIT=4;
    private static final String EXPERT=PublicCatalogPolicy.EXPERT;
    private static final String SERVICE=PublicCatalogPolicy.SERVICE;
    private static final String PEOPLE=" from NguoiDung u left join HoSoKhachHang p on p.maNguoiDung=u.maNguoiDung ";
    private static final String SERVICES=" from DichVu d join d.maChuyenGiaLienKet u ";
    private final EntityManager em;
    public CatalogDiscovery(EntityManager em){this.em=em;}

    public record Filter(String q,String field,int expertPage,int servicePage){
        public static Filter parse(String q,String field,String experts,String services){
            q=q==null?"":q.strip();field=field==null?"":field.strip();
            if(q.length()>200||field.length()>100)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Từ khóa tối đa 200 ký tự, chuyên ngành tối đa 100 ký tự.");
            return new Filter(q,field,page(experts),page(services));
        }
        public static int page(String raw){
            if(raw==null||!raw.matches("[0-9]{1,6}"))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Số trang không hợp lệ.");
            int value=Integer.parseInt(raw);if(value<1||value>100000)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Số trang từ 1 đến 100000.");return value;
        }
        public String url(int experts,int services,String anchor){return UriComponentsBuilder.fromPath("/trang/trangchu").queryParam("q",q).queryParam("linhVuc",field).queryParam("trangChuyenGia",experts).queryParam("trangDichVu",services).fragment(anchor).build().encode().toUriString();}
        public String all(String anchor){return url(1,1,anchor);}
    }
    public record Expert(Long id,String name,String initials,String introduction,List<String> fields,long reviewCount,Double average,long completed,boolean hasAvatar){}
    public record ServiceItem(Long id,Long ownerId,String title,String field,String summary,BigDecimal price,String ownerName){}
    public record ServiceDetail(Long id,Long ownerId,String title,String field,String description,BigDecimal price){}
    public record Review(String author,int score,String text,LocalDateTime date){}
    public record Page<T>(List<T> items,long total,int number,int pages,String previous,String next){}
    public record Listing(Filter filter,List<String> fields,Page<Expert> experts,Page<ServiceItem> services){}
    public record Profile(Expert expert,Page<ServiceItem> services,Page<Review> reviews){}
    public record Detail(ServiceDetail service,Expert expert,List<ServiceItem> others,long otherCount){}
    private <T> TypedQuery<T> query(String hql,Class<T> type,Map<String,Object> params){var query=em.createQuery(hql,type);params.forEach(query::setParameter);return query;}
    private static String term(String s){return "%"+s.toLowerCase(Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_").replace("[","![")+"%";}
    public List<String> fields(){return query("select distinct d.linhVuc"+SERVICES+"where "+SERVICE+" and trim(d.linhVuc)<>'' order by d.linhVuc",String.class,Map.of()).getResultList();}

    public Listing listing(Filter f){return listing(f,false);}
    public Listing home(Filter f){return listing(f,true);}
    private Listing listing(Filter f,boolean home){
        List<String> fields=fields();
        if(!f.field().isEmpty()&&!fields.contains(f.field()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Chuyên ngành không có trong danh sách lọc.");
        Map<String,Object> ep=new LinkedHashMap<>(),sp=new LinkedHashMap<>();
        String ew="where "+EXPERT,sw="where "+SERVICE;
        if(!f.q().isEmpty()){
            String like=" like :term escape '!'";
            ew+=" and (lower(u.hoTen)"+like+" or lower(p.gioiThieu)"+like+" or exists(select d.maDichVu from DichVu d where d.maChuyenGia=u.maNguoiDung and "+PublicCatalogPolicy.approved("d")+" and (lower(d.tenDichVu)"+like+" or lower(d.moTa)"+like+")))";
            sw+=" and (lower(u.hoTen)"+like+" or lower(d.tenDichVu)"+like+" or lower(d.moTa)"+like+")";
            ep.put("term",term(f.q()));sp.put("term",term(f.q()));
        }
        if(!f.field().isEmpty()){
            ew+=" and exists(select d.maDichVu from DichVu d where d.maChuyenGia=u.maNguoiDung and "+PublicCatalogPolicy.approved("d")+" and d.linhVuc=:field)";
            sw+=" and d.linhVuc=:field";ep.put("field",f.field());sp.put("field",f.field());
        }
        long ec=query("select count(u)"+PEOPLE+ew,Long.class,ep).getSingleResult(),sc=query("select count(d)"+SERVICES+sw,Long.class,sp).getSingleResult();
        int size=home?HOME_SIZE:PAGE_SIZE,en=home?1:Math.min(f.expertPage(),pages(ec,size)),sn=home?1:Math.min(f.servicePage(),pages(sc,size));
        List<Expert> experts=experts(query("select u.maNguoiDung,u.hoTen,substring(p.gioiThieu,1,240)"+PEOPLE+ew+" order by u.maNguoiDung desc",Object[].class,ep).setFirstResult((en-1)*size).setMaxResults(size).getResultList());
        List<ServiceItem> services=serviceRows(sw,sp,(sn-1)*size,size);
        return new Listing(f,fields,new Page<>(experts,ec,en,pages(ec,size),en>1?f.url(en-1,sn,"chuyen-gia"):null,en<pages(ec,size)?f.url(en+1,sn,"chuyen-gia"):null),new Page<>(services,sc,sn,pages(sc,size),sn>1?f.url(en,sn-1,"dich-vu"):null,sn<pages(sc,size)?f.url(en,sn+1,"dich-vu"):null));
    }
    private static int pages(long count,int size){return (int)Math.max(1,Math.min(100000,(count+size-1)/size));}
    private List<ServiceItem> serviceRows(String where,Map<String,Object> params,int offset,int limit){
        return query("select d.maDichVu,u.maNguoiDung,d.tenDichVu,d.linhVuc,substring(d.moTa,1,240),d.giaThamKhao,u.hoTen"+SERVICES+where+" order by d.maDichVu desc",Object[].class,params).setFirstResult(offset).setMaxResults(limit).getResultList().stream().map(r->new ServiceItem((Long)r[0],(Long)r[1],(String)r[2],(String)r[3],(String)r[4],(BigDecimal)r[5],(String)r[6])).toList();
    }
    private List<Expert> experts(List<Object[]> rows){
        if(rows.isEmpty())return List.of();List<Long> ids=rows.stream().map(r->(Long)r[0]).toList();Map<String,Object> params=Map.of("ids",ids);
        Map<Long,List<String>> fields=new HashMap<>();
        for(Object[] r:query("select distinct d.maChuyenGia,d.linhVuc from DichVu d where d.maChuyenGia in :ids and "+PublicCatalogPolicy.approved("d")+" and trim(d.linhVuc)<>'' order by d.maChuyenGia,d.linhVuc",Object[].class,params).getResultList())fields.computeIfAbsent((Long)r[0],k->new ArrayList<>()).add((String)r[1]);
        Map<Long,Object[]> ratings=new HashMap<>();
        for(Object[] r:query("select d.maNguoiDuocDanhGia,count(d),avg(cast(d.diem as double)) from DanhGia d where d.maNguoiDuocDanhGia in :ids and d.trangThai='CONG_KHAI' group by d.maNguoiDuocDanhGia",Object[].class,params).getResultList())ratings.put((Long)r[0],r);
        Map<Long,Long> completed=new HashMap<>();
        for(Object[] r:query("select d.maChuyenGia,count(h) from HopDong h join h.maDeXuatLienKet d where d.maChuyenGia in :ids and h.maKeHoach is null and h.trangThai='HOAN_THANH' group by d.maChuyenGia",Object[].class,params).getResultList())completed.put((Long)r[0],(Long)r[1]);
        for(Object[] r:query("select h.maChuyenGia,count(h) from HopDong h where h.maKeHoach is not null and h.maChuyenGia in :ids and h.trangThai='HOAN_THANH' group by h.maChuyenGia",Object[].class,params).getResultList())completed.merge((Long)r[0],(Long)r[1],Long::sum);
        Set<Long> avatarIds = new HashSet<>(query("select a.userId from AnhDaiDienChuyenGia a where a.userId in :ids", Long.class, params).getResultList());
        return rows.stream().map(r->{Long id=(Long)r[0];Object[] rating=ratings.get(id);return new Expert(id,(String)r[1],initials((String)r[1]),(String)r[2],List.copyOf(fields.getOrDefault(id,List.of())),rating==null?0:(Long)rating[1],rating==null?null:((Number)rating[2]).doubleValue(),completed.getOrDefault(id,0L),avatarIds.contains(id));}).toList();
    }
    private static String initials(String name){String[] words=name.strip().split("\\s+");String first=words[0].substring(0,words[0].offsetByCodePoints(0,1)),last=words.length>1?words[words.length-1].substring(0,words[words.length-1].offsetByCodePoints(0,1)):"";return (first+last).toUpperCase(Locale.ROOT);}
    public Expert expert(Long id){
        var rows=query("select u.maNguoiDung,u.hoTen,p.gioiThieu"+PEOPLE+" where u.maNguoiDung=:id and "+EXPERT,Object[].class,Map.of("id",id)).getResultList();
        if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Hồ sơ chuyên gia không tồn tại hoặc không còn công khai.");return experts(rows).get(0);
    }
    public Profile profile(Long id,int servicePage,int reviewPage){
        Expert expert=expert(id);Map<String,Object> params=Map.of("owner",id);String where="where "+SERVICE+" and d.maChuyenGia=:owner";
        long count=query("select count(d)"+SERVICES+where,Long.class,params).getSingleResult();int sn=Math.min(servicePage,pages(count,PAGE_SIZE));
        int rn=Math.min(reviewPage,pages(expert.reviewCount(),PAGE_SIZE));
        List<Review> reviews=query("select u.hoTen,d.diem,d.noiDung,d.ngayGui from DanhGia d join d.maNguoiDanhGiaLienKet u where d.maNguoiDuocDanhGia=:owner and d.trangThai='CONG_KHAI' order by d.ngayGui desc,d.maDanhGia desc",Object[].class,params).setFirstResult((rn-1)*PAGE_SIZE).setMaxResults(PAGE_SIZE).getResultList().stream().map(r->new Review((String)r[0],(Integer)r[1],(String)r[2],(LocalDateTime)r[3])).toList();
        return new Profile(expert,new Page<>(serviceRows(where,params,(sn-1)*PAGE_SIZE,PAGE_SIZE),count,sn,pages(count,PAGE_SIZE),sn>1?profileUrl(id,sn-1,rn,"dich-vu"):null,sn<pages(count,PAGE_SIZE)?profileUrl(id,sn+1,rn,"dich-vu"):null),new Page<>(reviews,expert.reviewCount(),rn,pages(expert.reviewCount(),PAGE_SIZE),rn>1?profileUrl(id,sn,rn-1,"danh-gia"):null,rn<pages(expert.reviewCount(),PAGE_SIZE)?profileUrl(id,sn,rn+1,"danh-gia"):null));
    }
    private static String profileUrl(Long id,int s,int r,String anchor){return "/chuyen-gia/"+id+"?trangDichVu="+s+"&trangDanhGia="+r+"#"+anchor;}
    public Detail detail(Long id){
        var rows=query("select d.maDichVu,d.maChuyenGia,d.tenDichVu,d.linhVuc,d.moTa,d.giaThamKhao"+SERVICES+"where d.maDichVu=:id and "+SERVICE,Object[].class,Map.of("id",id)).getResultList();
        if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Dịch vụ không tồn tại hoặc không còn công khai.");
        Object[] r=rows.get(0);ServiceDetail service=new ServiceDetail((Long)r[0],(Long)r[1],(String)r[2],(String)r[3],(String)r[4],(BigDecimal)r[5]);
        Expert full=expert(service.ownerId());Expert summary=new Expert(full.id(),full.name(),full.initials(),shortText(full.introduction()),full.fields(),full.reviewCount(),full.average(),full.completed(),full.hasAvatar());
        Map<String,Object> params=Map.of("owner",service.ownerId(),"id",id);String where="where "+SERVICE+" and d.maChuyenGia=:owner and d.maDichVu<>:id";
        long count=query("select count(d)"+SERVICES+where,Long.class,params).getSingleResult();return new Detail(service,summary,serviceRows(where,params,0,RELATED_LIMIT),count);
    }
    private static String shortText(String value){return value==null?null:value.substring(0,Math.min(240,value.length()));}
    public boolean availableTarget(String path){
        if(path.startsWith("/chuyen-gia/"))return query("select count(u) from NguoiDung u where u.maNguoiDung=:id and "+EXPERT,Long.class,Map.of("id",Long.valueOf(path.substring(12)))).getSingleResult()>0;
        return query("select count(d)"+SERVICES+"where d.maDichVu=:id and "+SERVICE,Long.class,Map.of("id",Long.valueOf(path.substring(13)))).getSingleResult()>0;
    }
}
