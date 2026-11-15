package vn.biz04.service;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import vn.biz04.data.Store;
import vn.biz04.domain.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

/** Customer-initiated hiring negotiation. Deliberately has no contract/payment dependency. */
@Service @Transactional
public class HireOfferService {
    private final Store db;private final Access access;private final EntityManager em;private final Clock clock;
    public HireOfferService(Store db,Access access,EntityManager em,Clock clock){this.db=db;this.access=access;this.em=em;this.clock=clock;}
    public record Terms(String scope,String deliverable,BigDecimal price,int days,String reason){}
    public record Origin(Long expert,String expertName,Long source,String sourceTitle,HireOfferForm form){}
    public record Version(int number,Long author,String authorName,String scope,String deliverable,BigDecimal price,int days,String reason,OffsetDateTime sent){}
    public record Row(Long id,String title,Long customer,Long expert,String customerName,String expertName,Long source,String sourceTitle,String state,int current,Long waiting,String label,String tone,BigDecimal price,int days,OffsetDateTime sent,OffsetDateTime updated){}
    public record Page<T>(List<T> items,long total,int number,int pages,String previous,String next){}
    public record Details(Row row,Version current,Page<Version> history,boolean canRespond,Integer agreed,Long decider,String deciderName,OffsetDateTime decided,String rejection){}
    private OffsetDateTime now(){return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);}
    private static void forbidden(boolean allowed){if(!allowed)throw new AccessDeniedException("Bạn không được xem hoặc xử lý đề nghị thuê này.");}
    private DeNghiThue get(Long id){DeNghiThue value=em.find(DeNghiThue.class,id);if(value==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy đề nghị thuê.");return value;}
    private NguoiDung read(Long actor,DeNghiThue offer){NguoiDung u=access.user(actor);forbidden(u.getLaQuanTriVien()||Objects.equals(actor,offer.customer)||Objects.equals(actor,offer.expert));return u;}
    private void parties(Long customer,Long expert){
        forbidden(!Objects.equals(customer,expert));
        for(Long id:new TreeSet<>(List.of(customer,expert)))db.lockEntity(NguoiDung.class,id);
        forbidden(Boolean.TRUE.equals(access.user(customer).getLaKhachHang())&&Boolean.TRUE.equals(access.user(expert).getLaChuyenGia()));
    }
    private DeNghiThue lock(Long actor,Long id){
        DeNghiThue offer=get(id);read(actor,offer);forbidden(Objects.equals(actor,offer.customer)||Objects.equals(actor,offer.expert));
        parties(offer.customer,offer.expert);return db.lockEntity(DeNghiThue.class,id);
    }
    private void turn(Long actor,DeNghiThue offer,int expected){
        Checks.require(expected==offer.current,"Phiên bản đã thay đổi. Hãy tải lại trang và xem nội dung mới trước khi phản hồi.");
        Checks.require("CHO_PHAN_HOI".equals(offer.state),"Đề nghị đã được xử lý, không thể tiếp tục thương lượng.");
        forbidden(Objects.equals(actor,offer.waiting));
        PhienBanDeNghiThue latest=version(offer.id,expected);forbidden(!Objects.equals(actor,latest.author));
    }
    private void key(String key){Checks.require(key!=null&&key.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"),"Phiên gửi không hợp lệ. Hãy mở lại biểu mẫu.");}
    public Terms validate(HireOfferForm form,boolean revised){
        String scope=Checks.text(form.getScope(),10,10000,"Phạm vi công việc"),deliverable=Checks.text(form.getDeliverable(),5,5000,"Sản phẩm mong muốn");
        Checks.require(form.getPrice()!=null&&form.getPrice().matches("[0-9]{1,13}"),"Giá đề nghị phải là số nguyên dương, tối đa 1.000 tỷ đồng.");BigDecimal price=Checks.money(new BigDecimal(form.getPrice()),false);
        Checks.require(form.getDays()!=null&&form.getDays().matches("[0-9]{1,3}"),"Thời gian thực hiện từ 1 đến 730 ngày.");int days=Integer.parseInt(form.getDays());Checks.require(days>0&&days<=730,"Thời gian thực hiện từ 1 đến 730 ngày.");
        return new Terms(scope,deliverable,price,days,revised?Checks.text(form.getReason(),5,2000,"Giải thích điều chỉnh"):null);
    }
    @Transactional(readOnly=true) public Origin origin(Long actor,Long expert,Long source){
        NguoiDung customer=access.user(actor),provider=access.user(expert);forbidden(customer.getLaKhachHang()&&provider.getLaChuyenGia()&&!Objects.equals(actor,expert));
        HireOfferForm form=new HireOfferForm();String title=null;
        if(source!=null){DichVu d=db.get(DichVu.class,source);Checks.require(Objects.equals(d.getMaChuyenGia(),expert)&&"DANG_HIEN".equals(d.getTrangThai())&&PublicCatalogPolicy.approved(d)&&PublicCatalogPolicy.official(provider),"Dịch vụ nguồn không thuộc chuyên gia hoặc không còn công khai.");title=d.getTenDichVu();form.setTitle(title);form.setScope(d.getMoTa());form.setPrice(d.getGiaThamKhao().toPlainString());}
        return new Origin(expert,provider.getHoTen(),source,title,form);
    }
    public Long create(Long actor,Long expert,Long source,String requestKey,HireOfferForm form){
        key(requestKey);String title=Checks.text(form.getTitle(),5,255,"Tiêu đề công việc");Terms terms=validate(form,false);parties(actor,expert);
        var existing=db.first("from DeNghiThue h where h.customer=:u and h.key=:key",DeNghiThue.class,"u",actor,"key",requestKey);
        if(existing.isPresent()){DeNghiThue h=existing.get();Checks.require(h.expert.equals(expert)&&Objects.equals(h.source,source)&&h.title.equals(title)&&matches(version(h.id,1),terms),"Phiên gửi đã được dùng cho nội dung khác. Hãy mở biểu mẫu mới.");return h.id;}
        if(source!=null)db.lockEntity(DichVu.class,source);
        origin(actor,expert,source);DeNghiThue h=new DeNghiThue();h.customer=actor;h.expert=expert;h.source=source;h.key=requestKey;h.title=title;h.state="CHO_PHAN_HOI";h.current=1;h.waiting=expert;h.sent=now();h.updated=h.sent;db.save(h);append(h,actor,requestKey,terms);access.audit(actor,"DeNghiThue",h.id,"GUI_DE_NGHI_THUE",null);return h.id;
    }
    private PhienBanDeNghiThue version(Long id,int number){return db.first("from PhienBanDeNghiThue v where v.offer=:id and v.number=:n",PhienBanDeNghiThue.class,"id",id,"n",number).orElseThrow(()->new BusinessException("Không tìm thấy phiên bản đề nghị."));}
    private boolean matches(PhienBanDeNghiThue v,Terms t){return v.scope.equals(t.scope())&&v.deliverable.equals(t.deliverable())&&v.price.compareTo(t.price())==0&&v.days==t.days()&&Objects.equals(v.reason,t.reason());}
    private void append(DeNghiThue h,Long author,String key,Terms terms){PhienBanDeNghiThue v=new PhienBanDeNghiThue();v.offer=h.id;v.number=h.current;v.author=author;v.key=key;v.scope=terms.scope();v.deliverable=terms.deliverable();v.price=terms.price();v.days=terms.days();v.reason=terms.reason();v.sent=now();db.save(v);}
    @Transactional(readOnly=true) public Details responseView(Long actor,Long id,int expected){DeNghiThue h=get(id);read(actor,h);turn(actor,h,expected);return details(actor,id,1);}
    @Transactional(readOnly=true) public Terms previewRevision(Long actor,Long id,int expected,HireOfferForm form){
        responseView(actor,id,expected);Terms terms=validate(form,true);PhienBanDeNghiThue v=version(id,expected);
        Checks.require(!v.scope.equals(terms.scope())||!v.deliverable.equals(terms.deliverable())||v.price.compareTo(terms.price())!=0||v.days!=terms.days(),"Nội dung chưa thay đổi. Hãy điều chỉnh ít nhất một trường.");return terms;
    }
    public Long revise(Long actor,Long id,int expected,String operation,Terms terms){
        key(operation);DeNghiThue h=lock(actor,id);
        // A repeated confirmed POST returns the same snapshot even after later negotiation.
        var repeat=db.first("from PhienBanDeNghiThue v where v.offer=:id and v.key=:key",PhienBanDeNghiThue.class,"id",id,"key",operation);
        if(repeat.isPresent()){PhienBanDeNghiThue v=repeat.get();forbidden(v.author.equals(actor));Checks.require(v.number==expected+1&&matches(v,terms),"Phiên gửi điều chỉnh đã được sử dụng.");return id;}
        turn(actor,h,expected);HireOfferForm check=form(terms);previewRevision(actor,id,expected,check);
        h.current++;h.waiting=actor.equals(h.customer)?h.expert:h.customer;h.updated=now();append(h,actor,operation,terms);access.audit(actor,"DeNghiThue",id,"DIEU_CHINH_DE_NGHI",terms.reason());return id;
    }
    public void accept(Long actor,Long id,int expected){
        DeNghiThue h=lock(actor,id);
        if("DA_THONG_NHAT".equals(h.state)&&h.current==expected&&actor.equals(h.decider))return;
        turn(actor,h,expected);h.state="DA_THONG_NHAT";h.agreed=expected;decide(h,actor);access.audit(actor,"DeNghiThue",id,"THONG_NHAT_DE_NGHI","Phiên bản "+expected);
    }
    public void reject(Long actor,Long id,int expected,String reason){
        String explanation=Checks.text(reason,5,2000,"Lý do từ chối");DeNghiThue h=lock(actor,id);
        if("BI_TU_CHOI".equals(h.state)&&h.current==expected&&actor.equals(h.decider)&&explanation.equals(h.rejection))return;
        turn(actor,h,expected);h.state="BI_TU_CHOI";h.rejection=explanation;decide(h,actor);access.audit(actor,"DeNghiThue",id,"TU_CHOI_DE_NGHI",explanation);
    }
    private void decide(DeNghiThue h,Long actor){h.waiting=null;h.decider=actor;h.decided=now();h.updated=h.decided;}
    public static HireOfferForm form(Terms terms){HireOfferForm f=new HireOfferForm();f.setScope(terms.scope());f.setDeliverable(terms.deliverable());f.setPrice(terms.price().toPlainString());f.setDays(Integer.toString(terms.days()));f.setReason(terms.reason());return f;}
    public static HireOfferForm form(Version v){return form(new Terms(v.scope(),v.deliverable(),v.price(),v.days(),null));}
    private Row row(DeNghiThue h,PhienBanDeNghiThue v){
        String label,tone="waiting";if("DA_THONG_NHAT".equals(h.state)){label="Đã thống nhất — Chờ chốt mốc nghiệm thu";tone="agreed";}else if("BI_TU_CHOI".equals(h.state)){label="Bị từ chối";tone="rejected";}else if(h.current==1)label="Chờ chuyên gia phản hồi";else label=Objects.equals(h.waiting,h.customer)?"Chuyên gia đề nghị điều chỉnh — Chờ khách hàng phản hồi":"Chờ chuyên gia phản hồi điều chỉnh";
        String source=h.source==null?null:db.get(DichVu.class,h.source).getTenDichVu();return new Row(h.id,h.title,h.customer,h.expert,db.get(NguoiDung.class,h.customer).getHoTen(),db.get(NguoiDung.class,h.expert).getHoTen(),h.source,source,h.state,h.current,h.waiting,label,tone,v.price,v.days,h.sent,h.updated);
    }
    private Version view(PhienBanDeNghiThue v){return new Version(v.number,v.author,db.get(NguoiDung.class,v.author).getHoTen(),v.scope,v.deliverable,v.price,v.days,v.reason,v.sent);}
    @Transactional(readOnly=true) public Details details(Long actor,Long id,int historyPage){
        DeNghiThue h=get(id);NguoiDung user=read(actor,h);PhienBanDeNghiThue v=version(id,h.current);int pages=Math.max(1,(h.current+9)/10),page=Math.min(Math.max(1,historyPage),pages);
        var history=em.createQuery("from PhienBanDeNghiThue v where v.offer=:id order by v.number desc",PhienBanDeNghiThue.class).setParameter("id",id).setFirstResult((page-1)*10).setMaxResults(10).getResultList().stream().map(this::view).toList();
        boolean can="CHO_PHAN_HOI".equals(h.state)&&Objects.equals(actor,h.waiting)&&(Objects.equals(actor,h.customer)?user.getLaKhachHang():user.getLaChuyenGia());
        return new Details(row(h,v),view(v),new Page<>(history,h.current,page,pages,page>1?"/de-nghi-thue/"+id+"?trangLichSu="+(page-1)+"#lich-su":null,page<pages?"/de-nghi-thue/"+id+"?trangLichSu="+(page+1)+"#lich-su":null),can,h.agreed,h.decider,h.decider==null?null:db.get(NguoiDung.class,h.decider).getHoTen(),h.decided,h.rejection);
    }
    @Transactional(readOnly=true) public Page<Row> list(Long actor,String side,int requested){
        NguoiDung u=access.user(actor);boolean all="tat-ca".equals(side);if(all)forbidden(u.getLaQuanTriVien());
        String where=all?"":" where h."+("da-gui".equals(side)?"customer":"expert")+"=:u";var count=em.createQuery("select count(h) from DeNghiThue h"+where,Long.class);if(!all)count.setParameter("u",actor);long total=count.getSingleResult();int pages=(int)Math.max(1,Math.min(100000,(total+11)/12)),number=Math.min(Math.max(1,requested),pages);
        var q=em.createQuery("from DeNghiThue h"+where+" order by h.updated desc,h.id desc",DeNghiThue.class);if(!all)q.setParameter("u",actor);
        var rows=q.setFirstResult((number-1)*12).setMaxResults(12).getResultList().stream().map(h->row(h,version(h.id,h.current))).toList();String base="/de-nghi-thue/"+side+"?trang=";
        return new Page<>(rows,total,number,pages,number>1?base+(number-1):null,number<pages?base+(number+1):null);
    }
}
