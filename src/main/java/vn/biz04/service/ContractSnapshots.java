package vn.biz04.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.stereotype.Component;
import vn.biz04.data.Store;
import vn.biz04.domain.*;
import java.math.*;
import java.time.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

/** Versioned documents for the existing HopDong aggregate, never a second contract system. */
@Component
public class ContractSnapshots {
    public static final String START_RULE="FIRST_ESCROW_SUCCESS";
    private final Store db;private final ObjectMapper mapper;private final ContractOperator operator;
    public ContractSnapshots(Store db,ObjectMapper mapper,ContractOperator operator){this.db=db;this.mapper=mapper;this.operator=operator;}
    public record Party(Long id,String name,String type,String organization,String email,String phone,String address,String expertise,String maskedIdentity){}
    public record Clauses(String customerDuties,String expertDuties,String additional,String representativeName,String representativeTitle){}
    public record Body(MilestonePlanService.Source source,Long planId,int planVersion,Party customer,Party expert,ContractOperator.Info intermediary,MilestonePlanRules.Terms milestones,Clauses clauses,String startRule,String operatingPolicy,
                       @JsonInclude(JsonInclude.Include.NON_NULL) ServiceContractTerms serviceTerms){}
    public record Document(Long id,int version,OffsetDateTime created,Body body){}
    public record Signed(Long actor,int version,String digest,OffsetDateTime at,byte[] signature) {
        public String image() {
            return signature == null ? null : "data:image/png;base64," + Base64.getEncoder().encodeToString(signature);
        }
    }
    public record Read(Document document,String digest,List<Signed> confirmations,List<Integer> versions) {
        public Signed signedBy(Long actor) {
            return confirmations.stream().filter(proof -> proof.actor().equals(actor)).findFirst().orElse(null);
        }
    }
    public String json(Object value){try{return mapper.writeValueAsString(value);}catch(Exception e){throw new BusinessException("Không thể tạo bản chụp hợp đồng.");}}
    public static String digest(String text){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
    public String fingerprint(Body body){return digest(json(body));}
    public Clauses clauses(ElectronicContractForm form,String customerType){
        String additional=Checks.text(form.getAdditional(),0,10000,"Thỏa thuận bổ sung"),representative=Checks.text(form.getRepresentativeName(),0,150,"Người đại diện khai báo"),title=Checks.text(form.getRepresentativeTitle(),0,150,"Chức vụ khai báo");
        Checks.require(representative.isEmpty()==title.isEmpty(),"Nếu khai báo đại diện, cần điền cả họ tên và chức vụ để hai bên rà soát.");
        Checks.require("TO_CHUC".equals(customerType)||representative.isEmpty(),"Chỉ khai báo đại diện khi hồ sơ khách hàng là tổ chức.");
        return new Clauses(Checks.text(form.getCustomerDuties(),5,10000,"Trách nhiệm khách hàng"),Checks.text(form.getExpertDuties(),5,10000,"Trách nhiệm chuyên gia"),additional,representative,title);
    }
    private Party party(Long id,boolean customer){NguoiDung user=db.get(NguoiDung.class,id);var h=db.first("from HoSoKhachHang p where p.maNguoiDung=:u",HoSoKhachHang.class,"u",id).orElse(null);String type=h==null?"CA_NHAN":h.loaiKhachHang,organization=h==null||h.tenToChuc==null?"":h.tenToChuc;Checks.require(user.getHoTen()!=null&&!user.getHoTen().isBlank()&&user.getEmail()!=null&&!user.getEmail().isBlank(),(customer?"Khách hàng":"Chuyên gia")+" cần hoàn thiện tên/email trong hồ sơ.");Checks.require(!customer||!"TO_CHUC".equals(type)||!organization.isBlank(),"Khách hàng tổ chức cần bổ sung tên tổ chức trong hồ sơ.");return new Party(id,user.getHoTen(),type,organization,user.getEmail(),h==null||h.soDienThoai==null?"":h.soDienThoai,h==null||h.diaChi==null?"":h.diaChi,h==null||h.gioiThieu==null?"":h.gioiThieu,h==null||h.cccdBonSoCuoi==null?"":"********"+h.cccdBonSoCuoi);}
    public Body draft(MilestonePlanService.View plan,ElectronicContractForm form){
        var customer=party(plan.source().customer(),true);
        var expert=party(plan.source().expert(),false);
        return new Body(plan.source(),plan.planId(),plan.current(),customer,expert,operator.snapshot(),
                plan.latest().terms(),clauses(form,customer.type()),START_RULE,
                "Mọi khoản tiền ghi nhận trên Sang Connect là mô phỏng bằng VND. Ký quỹ theo thứ tự từng mốc. "
                + "Phí được trừ từ phần chuyên gia theo tỷ lệ đã chốt, không cộng vào tiền ký quỹ hoặc trừ từ phần hoàn cho khách hàng. "
                + "Bên A rà soát sản phẩm theo tiêu chí của mốc và yêu cầu chỉnh sửa trong giới hạn đã chốt. "
                + "Hệ thống tự nghiệm thu sau 14 ngày từ lần bàn giao đủ điều kiện nếu mốc còn chờ phản hồi, hợp đồng đang thực hiện và không có tranh chấp mở. "
                + "Khi chấm dứt hoặc có tranh chấp, hệ thống giữ khoản tiền mô phỏng đang ký quỹ cho đến khi có phương án xử lý theo đúng quyền và ghi nhận giao dịch thành công.",
                ServiceContractTerms.draft(form));
    }
    public Body amended(Body old,ElectronicContractForm form){
        var terms=old.serviceTerms()==null?null:old.serviceTerms().amended(form);
        return new Body(old.source(),old.planId(),old.planVersion(),old.customer(),old.expert(),old.intermediary(),
                old.milestones(),clauses(form,old.customer().type()),old.startRule(),old.operatingPolicy(),terms);
    }
    public ElectronicContractForm form(Body body){
        var form=new ElectronicContractForm();
        form.setCustomerDuties(body.clauses().customerDuties());
        form.setExpertDuties(body.clauses().expertDuties());
        form.setAdditional(body.clauses().additional());
        form.setRepresentativeName(body.clauses().representativeName());
        form.setRepresentativeTitle(body.clauses().representativeTitle());
        if(body.serviceTerms()!=null){
            form.setUsageRights(body.serviceTerms().usageRights());
            form.setConfidentiality(body.serviceTerms().confidentiality());
            form.setLiability(body.serviceTerms().liability());
        }
        return form;
    }
    public Document document(BanChupHopDong snapshot){Checks.require(snapshot.digest.equals(digest(snapshot.json)),"Bản chụp hợp đồng không khớp dấu kiểm tra. Cần đối chiếu trước khi xử lý.");try{return mapper.readValue(snapshot.json,Document.class);}catch(Exception e){throw new BusinessException("Không đọc được bản chụp hợp đồng.");}}
    public BanChupHopDong snapshot(HopDong h,int version){return db.first("from BanChupHopDong s where s.contract=:h and s.version=:v",BanChupHopDong.class,"h",h.getMaHopDong(),"v",version).orElseThrow(()->new BusinessException("Không tìm thấy phiên bản hợp đồng điện tử."));}
    public void save(HopDong h,Long actor,Body body,OffsetDateTime at){var record=new BanChupHopDong();record.contract=h.getMaHopDong();record.version=h.getPhienBan();record.author=actor;record.created=at;record.json=json(new Document(h.getMaHopDong(),h.getPhienBan(),h.getNgayLap(),body));record.digest=digest(record.json);db.save(record);h.setDauKiemNoiDung(record.digest);}
    public Read read(HopDong h,int version){var record=snapshot(h,version);return new Read(document(record),record.digest,db.list("from XacNhanHopDong x where x.contract=:h and x.version=:v order by x.at,x.id",XacNhanHopDong.class,"h",h.getMaHopDong(),"v",version).stream().map(x->new Signed(x.actor,x.version,x.digest,x.at,x.signature)).toList(),db.list("select s.version from BanChupHopDong s where s.contract=:h order by s.version desc",Integer.class,"h",h.getMaHopDong()));}
}
