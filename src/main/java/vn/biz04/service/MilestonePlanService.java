package vn.biz04.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import vn.biz04.data.Store;
import vn.biz04.domain.*;
import java.math.*;
import java.time.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import static vn.biz04.service.MilestonePlanRules.*;

@Service @Transactional
public class MilestonePlanService {
    private final Store db;private final Access access;private final Clock clock;
    public MilestonePlanService(Store db,Access access,Clock clock){this.db=db;this.access=access;this.clock=clock;}
    public record Source(String kind,Long id,Long customer,Long expert,String customerName,String expertName,String title,String scope,String deliverable,BigDecimal total,int days,Integer version,String fingerprint,boolean eligible){
        public String url(){return (kind.equals("thue")?"/de-nghi-thue/":"/de-xuat/")+id;}
        public String planUrl(){return "/ke-hoach-moc/"+kind+"/"+id;}
    }
    public record Version(int number,Long author,String authorName,OffsetDateTime sent,Terms terms){}
    public record View(Source source,Long planId,String state,String label,int current,Long waiting,boolean canEdit,boolean canRespond,boolean canCancel,boolean stale,Version latest,List<Version> history,int historyPage,int historyPages,Long decider,String deciderName,OffsetDateTime decided,String cancellation,Long contractId){}
    public record Workspace(List<View> rows,int page,int pages,int total){}
    private OffsetDateTime now(){return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);}
    private static void permit(boolean value){if(!value)throw new AccessDeniedException("Bạn không được tham gia kế hoạch mốc này.");}
    private static String fingerprint(Object... fields){try{var digest=MessageDigest.getInstance("SHA-256");for(Object field:fields){String value=field==null?"<null>":field.toString();byte[] bytes=value.getBytes(StandardCharsets.UTF_8);digest.update(Integer.toString(bytes.length).getBytes(StandardCharsets.US_ASCII));digest.update((byte)':');digest.update(bytes);}return HexFormat.of().formatHex(digest.digest());}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
    private Source source(Long actor,String kind,Long id,boolean lock){
        Checks.require(Set.of("thue","de-xuat").contains(kind),"Nguồn kế hoạch mốc không hợp lệ.");NguoiDung viewer=access.user(actor);Long customer,expert;DeNghiThue h=null;DeXuat d=null;YeuCau y=null;
        if(kind.equals("thue")){h=db.get(DeNghiThue.class,id);customer=h.customer;expert=h.expert;}else{d=db.get(DeXuat.class,id);y=db.get(YeuCau.class,d.getMaYeuCau());customer=y.getMaKhachHang();expert=d.getMaChuyenGia();}
        permit(viewer.getLaQuanTriVien()||actor.equals(customer)||actor.equals(expert));
        if(lock){permit(!viewer.getLaQuanTriVien());for(Long uid:new TreeSet<>(List.of(customer,expert)))db.lockEntity(NguoiDung.class,uid);if(h!=null)h=db.lockEntity(DeNghiThue.class,id);else{y=db.lockEntity(YeuCau.class,y.getMaYeuCau());d=db.lockEntity(DeXuat.class,id);}}
        if(lock)Checks.require(h!=null?customer.equals(h.customer)&&expert.equals(h.expert):customer.equals(y.getMaKhachHang())&&expert.equals(d.getMaChuyenGia()),"Các bên của nguồn đã thay đổi. Hãy mở lại nguồn.");
        NguoiDung c=db.get(NguoiDung.class,customer),e=db.get(NguoiDung.class,expert);boolean roles="HOAT_DONG".equals(c.getTrangThai())&&"HOAT_DONG".equals(e.getTrangThai())&&c.getLaKhachHang()&&e.getLaChuyenGia()&&!customer.equals(expert);
        String title,scope,deliverable;BigDecimal total;int days;Integer version;boolean eligible;
        if(h!=null){version=h.agreed;var v=db.first("from PhienBanDeNghiThue v where v.offer=:h and v.number=:v",PhienBanDeNghiThue.class,"h",id,"v",h.agreed==null?h.current:h.agreed).orElseThrow(()->new BusinessException("Chưa có nội dung thuê để xác lập mốc."));title=h.title;scope=v.scope;deliverable=v.deliverable;total=v.price;days=v.days;eligible=roles&&"DA_THONG_NHAT".equals(h.state)&&Objects.equals(h.agreed,h.current);}
        else{version=null;title=y.getTieuDe();scope=d.getPhuongAn();deliverable=d.getPhuongAn();total=d.getGiaDeXuat();days=d.getSoNgayThucHien()==null?0:d.getSoNgayThucHien();eligible=roles&&"DA_CHON".equals(d.getTrangThai())&&Set.of("DANG_MO","DANG_THUONG_LUONG").contains(y.getTrangThai())&&db.count("select count(h) from HopDong h where h.maYeuCau=:y","y",y.getMaYeuCau())==0&&db.count("select count(d) from DeXuat d where d.maYeuCau=:y and d.trangThai='DA_CHON'","y",y.getMaYeuCau())==1;}
        eligible=eligible&&days>0&&total!=null&&total.signum()>0;
        return new Source(kind,id,customer,expert,c.getHoTen(),e.getHoTen(),title,scope,deliverable,total,days,version,fingerprint(kind,id,customer,expert,title,scope,deliverable,total==null?null:total.toPlainString(),days,version),eligible);
    }
    private KeHoachMoc find(Source source){return db.first("from KeHoachMoc p where p."+(source.kind().equals("thue")?"hire":"proposal")+"=:id",KeHoachMoc.class,"id",source.id()).orElse(null);}
    private Source snapshot(Source origin,KeHoachMoc plan){return new Source(origin.kind(),origin.id(),plan.customer,plan.expert,plan.customerName,plan.expertName,plan.title,plan.scope,plan.deliverable,plan.total,plan.days,plan.sourceVersion,plan.fingerprint,origin.eligible());}
    private PhienBanKeHoachMoc version(KeHoachMoc plan,int number){return db.first("from PhienBanKeHoachMoc v where v.plan=:p and v.number=:n",PhienBanKeHoachMoc.class,"p",plan.id,"n",number).orElseThrow(()->new BusinessException("Không tìm thấy phiên bản kế hoạch mốc."));}
    private Terms terms(PhienBanKeHoachMoc version){var items=db.list("from ChiTietKeHoachMoc i where i.version=:v order by i.position",ChiTietKeHoachMoc.class,"v",version.id).stream().map(i->new Item(i.stableKey,i.position,i.title,i.work,i.deliverable,i.criteria,i.unit,i.due,i.amount,i.fee,i.net,i.revisions)).toList();BigDecimal total=items.stream().map(Item::amount).reduce(BigDecimal.ZERO,BigDecimal::add),fee=items.stream().map(Item::fee).reduce(BigDecimal.ZERO,BigDecimal::add);return new Terms(version.mode,version.reason,items,total,fee,total.subtract(fee),version.rate,version.feeRule,version.deadlineRule==null?DAY_RULE:version.deadlineRule);}
    private Version view(PhienBanKeHoachMoc version){return new Version(version.number,version.author,db.get(NguoiDung.class,version.author).getHoTen(),version.sent,terms(version));}
    @Transactional(readOnly=true) public View details(Long actor,String kind,Long id,int page){
        Source origin=source(actor,kind,id,false);KeHoachMoc plan=find(origin);boolean admin=access.user(actor).getLaQuanTriVien();
        if(plan==null){Checks.require(origin.eligible(),"Nguồn chưa đủ điều kiện xác lập mốc, thiếu thời lượng hoặc đã có hợp đồng. Hãy mở lại chi tiết nguồn.");return new View(origin,null,"INITIAL","Chờ chuyên gia thiết lập mốc",0,origin.expert(),!admin&&actor.equals(origin.expert()),false,!admin,false,null,List.of(),1,1,null,null,null,null,null);}
        Long contractId=db.first("select h.maHopDong from HopDong h where h.maKeHoach=:p",Long.class,"p",plan.id).orElse(null);boolean stale=!origin.fingerprint().equals(plan.fingerprint)||(!origin.eligible()&&contractId==null);boolean pending=contractId==null&&"CHO_PHAN_HOI".equals(plan.state),turn=pending&&actor.equals(plan.waiting)&&!admin&&!stale;int pages=Math.max(1,(plan.current+9)/10),number=Math.min(Math.max(1,page),pages);
        var history=db.list("from PhienBanKeHoachMoc v where v.plan=:p order by v.number desc",PhienBanKeHoachMoc.class,"p",plan.id).stream().skip((long)(number-1)*10).limit(10).map(this::view).toList();
        String label="DA_THONG_NHAT".equals(plan.state)?"Đã thống nhất mốc — Chờ lập hợp đồng":"DA_HUY".equals(plan.state)?"Đã hủy thương lượng":Objects.equals(plan.waiting,plan.customer)?"Chờ khách hàng phản hồi":"Chờ chuyên gia phản hồi";
        if(contractId!=null)label="Đã lập hợp đồng";
        return new View(snapshot(origin,plan),plan.id,contractId==null?plan.state:"DA_LAP_HOP_DONG",label,plan.current,plan.waiting,turn,turn,pending&&!admin&&!stale,stale,plan.current==0?null:view(version(plan,plan.current)),history,number,pages,plan.decider,plan.decider==null?null:db.get(NguoiDung.class,plan.decider).getHoTen(),plan.decided,plan.cancellation,contractId);
    }
    @Transactional(readOnly=true) public Terms preview(Long actor,String kind,Long id,String expectedSource,int expected,MilestonePlanForm form){
        var current=details(actor,kind,id,1);permit(current.canEdit());Checks.require(expected==current.current()&&expectedSource.equals(current.source().fingerprint()),"Nội dung hoặc phiên bản đã thay đổi. Hãy tải lại và rà soát bản mới.");var result=validate(form,current.source().total(),current.source().days(),expected>0);validateKeys(current.planId(),result);
        if(current.latest()!=null)Checks.require(!current.latest().terms().items().equals(result.items())||!current.latest().terms().mode().equals(result.mode()),"Nội dung mốc chưa thay đổi.");return result;
    }
    private void validateKeys(Long plan,Terms terms){for(var item:terms.items()){long elsewhere=plan==null?db.count("select count(i) from ChiTietKeHoachMoc i where i.stableKey=:k","k",item.stableKey()):db.count("select count(i) from ChiTietKeHoachMoc i,PhienBanKeHoachMoc v where i.version=v.id and i.stableKey=:k and v.plan<>:p","k",item.stableKey(),"p",plan);Checks.require(elsewhere==0,"Định danh mốc không thuộc kế hoạch này.");}}
    private KeHoachMoc locked(Source source){Checks.require(db.count("select count(h) from HopDong h,KeHoachMoc p where h.maKeHoach=p.id and p."+(source.kind().equals("thue")?"hire":"proposal")+"=:id","id",source.id())==0,"Kế hoạch đã lập hợp đồng, không thể thương lượng lại.");Checks.require(source.eligible(),"Nguồn không còn đủ điều kiện xác lập mốc hoặc đã có hợp đồng.");var plan=find(source);if(plan!=null){plan=db.lockEntity(KeHoachMoc.class,plan.id);Checks.require(source.fingerprint().equals(plan.fingerprint),"Nội dung thuê đã thay đổi. Không được sử dụng kế hoạch của nội dung cũ.");}return plan;}
    private KeHoachMoc create(Source s){var plan=new KeHoachMoc();if(s.kind().equals("thue"))plan.hire=s.id();else plan.proposal=s.id();plan.fingerprint=s.fingerprint();plan.sourceVersion=s.version();plan.customer=s.customer();plan.expert=s.expert();plan.customerName=s.customerName();plan.expertName=s.expertName();plan.title=s.title();plan.scope=s.scope();plan.deliverable=s.deliverable();plan.total=s.total();plan.days=s.days();plan.current=0;plan.created=now();return plan;}
    private void expected(KeHoachMoc plan,int version){Checks.require(plan!=null&&plan.current==version&&"CHO_PHAN_HOI".equals(plan.state),"Kế hoạch đã thay đổi hoặc kết thúc. Hãy tải lại bản mới nhất.");}
    public Long send(Long actor,String kind,Long id,String expectedSource,int expected,String operation,Terms reviewed){
        Checks.require(operation!=null&&operation.matches("[0-9a-f-]{36}"),"Phiên gửi không hợp lệ.");Source source=source(actor,kind,id,true);Checks.require(source.fingerprint().equals(expectedSource),"Nội dung thuê đã thay đổi. Hãy mở lại kế hoạch.");var plan=locked(source);var checked=validate(form(reviewed),source.total(),source.days(),expected>0);Checks.require(checked.equals(reviewed),"Nội dung rà soát không hợp lệ.");
        if(plan!=null){var replay=db.first("from PhienBanKeHoachMoc v where v.plan=:p and v.operation=:o",PhienBanKeHoachMoc.class,"p",plan.id,"o",operation);if(replay.isPresent()){var v=replay.get();permit(v.author.equals(actor));Checks.require(v.number==expected+1&&terms(v).equals(reviewed),"Phiên gửi đã được dùng cho nội dung khác.");return plan.id;}}
        if(plan==null){Checks.require(expected==0,"Kế hoạch đã thay đổi.");permit(actor.equals(source.expert()));plan=create(source);}else{expected(plan,expected);permit(actor.equals(plan.waiting));Checks.require(!terms(version(plan,expected)).items().equals(checked.items())||!version(plan,expected).mode.equals(checked.mode()),"Nội dung mốc chưa thay đổi.");}
        validateKeys(plan.id,checked);plan.current++;plan.state="CHO_PHAN_HOI";plan.waiting=actor.equals(source.expert())?source.customer():source.expert();if(plan.id==null)db.save(plan);
        var version=new PhienBanKeHoachMoc();version.plan=plan.id;version.number=plan.current;version.author=actor;version.operation=operation;version.mode=checked.mode();version.reason=checked.reason();version.rate=checked.rate();version.feeRule=checked.feeRule();version.deadlineRule=checked.deadlineRule();version.sent=now();db.save(version);
        for(var item:checked.items()){var row=new ChiTietKeHoachMoc();row.version=version.id;row.stableKey=item.stableKey();row.position=item.position();row.title=item.title();row.work=item.work();row.deliverable=item.deliverable();row.criteria=item.criteria();row.unit=item.unit();row.due=item.due();row.amount=item.amount();row.fee=item.fee();row.net=item.net();row.revisions=item.revisions();db.save(row);}access.audit(actor,"KeHoachMoc",plan.id,"GUI_PHIEN_BAN_MOC","Phiên bản "+plan.current);return plan.id;
    }
    public void accept(Long actor,String kind,Long id,String fingerprint,int expected){var source=source(actor,kind,id,true);Checks.require(source.fingerprint().equals(fingerprint),"Nội dung thuê đã thay đổi.");var plan=locked(source);if(plan!=null&&"DA_THONG_NHAT".equals(plan.state)&&plan.current==expected&&actor.equals(plan.decider))return;expected(plan,expected);permit(actor.equals(plan.waiting));plan.state="DA_THONG_NHAT";plan.agreed=expected;decide(plan,actor);access.audit(actor,"KeHoachMoc",plan.id,"THONG_NHAT_MOC","Phiên bản "+expected);}
    public void cancel(Long actor,String kind,Long id,String fingerprint,int expected,String explanation){String reason=Checks.text(explanation,5,2000,"Lý do hủy");var source=source(actor,kind,id,true);Checks.require(source.fingerprint().equals(fingerprint),"Nội dung thuê đã thay đổi.");var plan=locked(source);if(plan!=null&&"DA_HUY".equals(plan.state)&&plan.current==expected&&actor.equals(plan.decider)&&reason.equals(plan.cancellation))return;if(plan==null){Checks.require(expected==0,"Kế hoạch đã thay đổi.");plan=create(source);}else expected(plan,expected);plan.state="DA_HUY";plan.cancellation=reason;decide(plan,actor);if(plan.id==null)db.save(plan);access.audit(actor,"KeHoachMoc",plan.id,"HUY_THUONG_LUONG_MOC",reason);}
    private void decide(KeHoachMoc plan,Long actor){plan.waiting=null;plan.decider=actor;plan.decided=now();}
    @Transactional(readOnly=true) public Workspace workspace(Long actor,int requested){
        var viewer=access.user(actor);boolean admin=viewer.getLaQuanTriVien();List<SourceKey> keys=new ArrayList<>();
        for(var p:db.list(admin?"from KeHoachMoc p":"from KeHoachMoc p where p.customer=:u or p.expert=:u",KeHoachMoc.class,admin?new Object[]{}:new Object[]{"u",actor}))keys.add(new SourceKey(p.hire!=null?"thue":"de-xuat",p.hire!=null?p.hire:p.proposal));
        for(var h:db.list(admin?"from DeNghiThue h where h.state='DA_THONG_NHAT'":"from DeNghiThue h where h.state='DA_THONG_NHAT' and (h.customer=:u or h.expert=:u)",DeNghiThue.class,admin?new Object[]{}:new Object[]{"u",actor}))keys.add(new SourceKey("thue",h.id));
        for(var d:db.list(admin?"select d from DeXuat d,YeuCau y where d.maYeuCau=y.maYeuCau and d.trangThai='DA_CHON'":"select d from DeXuat d,YeuCau y where d.maYeuCau=y.maYeuCau and d.trangThai='DA_CHON' and (y.maKhachHang=:u or d.maChuyenGia=:u)",DeXuat.class,admin?new Object[]{}:new Object[]{"u",actor}))keys.add(new SourceKey("de-xuat",d.getMaDeXuat()));
        List<View> rows=new ArrayList<>();for(var key:new LinkedHashSet<>(keys))try{rows.add(details(actor,key.kind(),key.id(),1));}catch(BusinessException ineligible){/* Existing contracts/unready sources remain in their original workspace. */}
        int pages=Math.max(1,(rows.size()+19)/20),page=Math.min(Math.max(1,requested),pages);return new Workspace(rows.stream().skip((long)(page-1)*20).limit(20).toList(),page,pages,rows.size());
    }
    public View forContract(Long actor,String kind,Long id,boolean lock){var origin=source(actor,kind,id,lock);var plan=find(origin);Checks.require(plan!=null,"Cần xác lập và thống nhất kế hoạch mốc trước khi lập hợp đồng.");if(lock)plan=db.lockEntity(KeHoachMoc.class,plan.id);permit(actor.equals(plan.customer)&&!access.user(actor).getLaQuanTriVien());var view=details(actor,kind,id,1);if(view.contractId()!=null)return view;Checks.require(!view.stale()&&"DA_THONG_NHAT".equals(plan.state)&&Objects.equals(plan.agreed,plan.current),"Hai bên cần thống nhất đúng phiên bản kế hoạch mốc trước khi lập hợp đồng.");return view;}
    private record SourceKey(String kind,Long id){}
}
