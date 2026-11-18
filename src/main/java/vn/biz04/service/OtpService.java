package vn.biz04.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.support.TransactionTemplate;
import vn.biz04.domain.*;
import vn.biz04.data.Store;
import java.time.*;
import java.util.*;
import org.slf4j.*;

@Service
public class OtpService {
    public static final String SENT="Mã xác thực đã được gửi. Vui lòng kiểm tra hộp thư và thư rác.";
    public static final String FAILED="Hiện chưa gửi được mã xác thực. Vui lòng thử lại sau.";
    private final Store db;private final AccountService accounts;private final AccountCrypto crypto;private final OtpMailGateway mail;private final TransactionTemplate tx;private final Clock clock;
    private final ExpertApplications experts;
    private final int sendAccount,sendIp,tryAccount,tryIp,cooldown;
    public record Result(String token,boolean success,String message){}
    public record View(String email,OffsetDateTime expires,OffsetDateTime resendAfter,String purpose,long remainingSeconds){}
    public record PendingLogin(Long id,String email,int epoch,Instant expires){}
    public OtpService(Store db,AccountService accounts,AccountCrypto crypto,OtpMailGateway mail,TransactionTemplate tx,Clock clock,ExpertApplications experts,
        @Value("${biz.otp.send-account-hour:5}")int sendAccount,@Value("${biz.otp.send-ip-hour:20}")int sendIp,@Value("${biz.otp.try-account-hour:20}")int tryAccount,@Value("${biz.otp.try-ip-hour:100}")int tryIp,@Value("${biz.otp.cooldown-seconds:60}")int cooldown){
        this.experts=experts;this.db=db;this.accounts=accounts;this.crypto=crypto;this.mail=mail;this.tx=tx;this.clock=clock;this.sendAccount=sendAccount;this.sendIp=sendIp;this.tryAccount=tryAccount;this.tryIp=tryIp;this.cooldown=Math.max(60,cooldown);
    }
    @Bean @org.springframework.core.annotation.Order(-100) ApplicationRunner initializeOtpGate(){return args->tx.executeWithoutResult(s->{if(db.count("select count(g) from OtpDieuPhoi g")==0){OtpDieuPhoi g=new OtpDieuPhoi();g.id=1L;db.save(g);}});}
    private OffsetDateTime now(){return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);}
    private void gate(){db.lockEntity(OtpDieuPhoi.class,1L);}
    private String binding(MaXacThucOTP o){return o.maNguoiDung+":"+o.email+":"+o.loaiOTP+":"+o.maYeuCau;}
    private MaXacThucOTP challenge(String token,Long actor){return challenge(token,actor,false);}
    private MaXacThucOTP challenge(String token,Long actor,boolean takeover){
        return challengePurpose(token,actor,takeover?"TIEP_QUAN":null);
    }
    private MaXacThucOTP challengePurpose(String token,Long actor,String purpose){
        Checks.require(token!=null&&token.length()==43,"Phiên xác thực không hợp lệ. Vui lòng bắt đầu lại.");
        MaXacThucOTP o=db.first("from MaXacThucOTP o where o.maYeuCau=:k",MaXacThucOTP.class,"k",crypto.digest(token)).orElseThrow(()->new BusinessException("Phiên xác thực không hợp lệ."));
        Checks.require(purpose!=null?Objects.equals(actor,o.maNguoiDung)&&purpose.equals(o.loaiOTP):(actor==null?"DANG_KY".equals(o.loaiOTP):Objects.equals(actor,o.maNguoiDung)&&"CAP_NHAT_THONG_TIN".equals(o.loaiOTP)),"Phiên xác thực không thuộc tài khoản hoặc mục đích này.");return o;
    }
    private boolean quota(String operation,Long user,String email,String ip){
        int accountLimit="SEND".equals(operation)?sendAccount:tryAccount,ipLimit="SEND".equals(operation)?sendIp:tryIp;
        List<OtpGioiHan> buckets=new ArrayList<>();
        String[] dimensions={"USER:"+(Objects.equals(user,0L)?email:user),"EMAIL:"+email,"IP:"+ip};
        for(int i=0;i<dimensions.length;i++){
            String key=crypto.digest(operation+":"+dimensions[i]);
            OtpGioiHan b=db.first("from OtpGioiHan b where b.khoa=:k",OtpGioiHan.class,"k",key).orElse(null);
            if(b==null){b=new OtpGioiHan();b.khoa=key;b.batDau=now();db.save(b);}
            if(!now().isBefore(b.batDau.plusHours(1))){b.batDau=now();b.soLuong=0;}
            if(b.soLuong>=(i==2?ipLimit:accountLimit))return false;buckets.add(b);
        }
        buckets.forEach(b->b.soLuong++);return true;
    }
    public Result register(AccountForm form,String ip){return tx.execute(s->{
        gate();String email=AccountService.email(form.getEmail());
        // IP/email trước khi tạo tài khoản; không tái dùng đăng ký để sửa tài khoản có sẵn.
        Checks.require(quota("SEND",0L,email,ip),"Bạn đã gửi quá nhiều yêu cầu. Vui lòng thử lại sau một giờ.");
        NguoiDung u=accounts.register(form);
        OtpGioiHan initial=new OtpGioiHan();initial.khoa=crypto.digest("SEND:USER:"+u.getMaNguoiDung());initial.batDau=now();initial.soLuong=1;db.save(initial);
        return issue(u,email,"DANG_KY");
    });}
    public Result registerExpert(ExpertRegistrationForm form,String operation,String ip){return tx.execute(s->{
        gate();String email=AccountService.email(form.getEmail());Checks.require(quota("SEND",0L,email,ip),"Bạn đã gửi quá nhiều yêu cầu. Vui lòng thử lại sau một giờ.");
        NguoiDung u=experts.register(form,operation);OtpGioiHan initial=new OtpGioiHan();initial.khoa=crypto.digest("SEND:USER:"+u.getMaNguoiDung());initial.batDau=now();initial.soLuong=1;db.save(initial);return issue(u,email,"DANG_KY");
    });}
    public Result startExpertResubmission(ExpertApplications.Proof proof,String ip){return tx.execute(s->{
        gate();NguoiDung u=db.lockEntity(NguoiDung.class,proof.userId());experts.validateProof(proof,true);
        Result limited=sendLimit(u,"DANG_KY_LAI_CG",u.getEmail(),ip,null);return limited==null?issue(u,u.getEmail(),"DANG_KY_LAI_CG"):limited;
    });}
    public View expertResubmissionView(String token,ExpertApplications.Proof proof){return tx.execute(s->{experts.validateProof(proof,true);return viewOf(challengePurpose(token,proof.userId(),"DANG_KY_LAI_CG"));});}
    public Result verifyExpertResubmission(String token,ExpertApplications.Proof proof,String code,String ip){
        return tx.execute(s->{gate();db.lockEntity(NguoiDung.class,proof.userId());experts.validateProof(proof,true);return verifyPurpose(token,proof.userId(),code,ip,"DANG_KY_LAI_CG");});
    }
    public Result resumeRegistration(String email,String password,String ip){return tx.execute(s->{
        gate();String address=AccountService.email(email);
        if(!quota("TRY",0L,address,ip))return new Result(null,false,"Bạn đã thử quá nhiều lần. Vui lòng thử lại sau một giờ.");
        NguoiDung u=db.first("from NguoiDung u where u.email=:e",NguoiDung.class,"e",address).orElse(null);
        if(u==null||!"CHO_XAC_THUC".equals(u.getTrangThai())||!accounts.passwordMatches(u,password))return new Result(null,false,"Email hoặc mật khẩu của tài khoản chờ xác thực không đúng.");
        db.lockEntity(NguoiDung.class,u.getMaNguoiDung());
        Result limit=sendLimit(u,"DANG_KY",address,ip,null);return limit==null?issue(u,address,"DANG_KY"):limit;
    });}
    public boolean awaitingRegistrationEmail(NguoiDung u){
        return "CHO_XAC_THUC".equals(u.getTrangThai()) && "CHO_XAC_THUC".equals(u.getTrangThaiEmail());
    }
    public PendingLogin pendingLogin(NguoiDung u){
        return new PendingLogin(u.getMaNguoiDung(),u.getEmail(),u.getPhienBaoMat(),clock.instant().plusSeconds(600));
    }
    public String pendingLoginEmail(PendingLogin proof){return tx.execute(s->{
        requireLiveProof(proof);
        return maskEmail(checkPendingLogin(db.get(NguoiDung.class,proof.id()),proof).getEmail());
    });}
    public Result sendPendingLoginCode(PendingLogin proof,String ip){return tx.execute(s->{
        requireLiveProof(proof);
        gate();
        NguoiDung u=checkPendingLogin(db.lockEntity(NguoiDung.class,proof.id()),proof);
        Result limit=sendLimit(u,"DANG_KY",proof.email(),ip,null);
        return limit==null?issue(u,proof.email(),"DANG_KY"):limit;
    });}
    private void requireLiveProof(PendingLogin proof){
        Checks.require(proof!=null&&clock.instant().isBefore(proof.expires()),"Phiên xác thực đã hết hạn. Vui lòng đăng nhập lại.");
    }
    private NguoiDung checkPendingLogin(NguoiDung u,PendingLogin proof){
        requireLiveProof(proof);
        Checks.require(awaitingRegistrationEmail(u)&&u.getPhienBaoMat()==proof.epoch()&&u.getEmail().equals(proof.email()),
                "Tài khoản đã thay đổi hoặc không còn chờ xác thực. Vui lòng đăng nhập lại.");
        return u;
    }
    public Result startEmailChange(Long actor,String email,String password,String ip){return tx.execute(s->{
        gate();NguoiDung u=db.lockEntity(NguoiDung.class,actor);Checks.require("HOAT_DONG".equals(u.getTrangThai()),"Tài khoản chưa hoạt động.");accounts.checkPassword(u,password);
        String next=AccountService.email(email);Checks.require(!next.equals(u.getEmail()),"Email mới phải khác email hiện tại.");accounts.unique(next,actor);
        Result limited=sendLimit(u,"CAP_NHAT_THONG_TIN",next,ip,null);return limited==null?issue(u,next,"CAP_NHAT_THONG_TIN"):limited;
    });}
    private Result sendLimit(NguoiDung u,String purpose,String email,String ip,String token){
        Optional<MaXacThucOTP> last=db.first("from MaXacThucOTP o where o.maNguoiDung=:u and o.loaiOTP=:p order by o.thoiDiemTao desc,o.maOTP desc",MaXacThucOTP.class,"u",u.getMaNguoiDung(),"p",purpose);
        if(last.isPresent()&&now().isBefore(last.get().thoiDiemTao.plusSeconds(cooldown)))return new Result(token,false,"Vui lòng đợi ít nhất 60 giây giữa các lần gửi mã.");
        if(!quota("SEND",u.getMaNguoiDung(),email,ip))return new Result(token,false,"Bạn đã gửi quá nhiều yêu cầu. Vui lòng thử lại sau một giờ.");return null;
    }
    public Result resend(String token,Long actor,String newPendingEmail,String ip){return tx.execute(s->{
        gate();MaXacThucOTP old=challenge(token,actor);NguoiDung u=db.lockEntity(NguoiDung.class,old.maNguoiDung);
        Checks.require("DANG_KY".equals(old.loaiOTP)?"CHO_XAC_THUC".equals(u.getTrangThai()):"HOAT_DONG".equals(u.getTrangThai()),"Tài khoản không còn ở trạng thái phù hợp.");
        Checks.require(!"DA_SU_DUNG".equals(old.trangThai),"Yêu cầu đã được xác thực.");
        MaXacThucOTP latest=db.first("from MaXacThucOTP o where o.maNguoiDung=:u and o.loaiOTP=:p order by o.maOTP desc",MaXacThucOTP.class,"u",old.maNguoiDung,"p",old.loaiOTP).orElseThrow();
        Checks.require(Objects.equals(latest.maOTP,old.maOTP),"Yêu cầu đã được thay thế. Vui lòng mở phiên xác thực mới nhất.");
        String next=old.email;
        if(newPendingEmail!=null){Checks.require("DANG_KY".equals(old.loaiOTP),"Không được sửa email qua luồng này.");next=AccountService.email(newPendingEmail);accounts.unique(next,u.getMaNguoiDung());}
        Result limit=sendLimit(u,old.loaiOTP,next,ip,token);if(limit!=null)return limit;
        if(newPendingEmail!=null){u.setEmail(next);u.setNgayCapNhat(now());}return issue(u,next,old.loaiOTP);
    });}
    private Result issue(NguoiDung u,String email,String purpose){
        for(MaXacThucOTP old:db.list("from MaXacThucOTP o where o.maNguoiDung=:u and o.loaiOTP=:p and o.trangThai='CHO_XAC_THUC'",MaXacThucOTP.class,"u",u.getMaNguoiDung(),"p",purpose))old.trangThai="BI_HUY";
        db.flush();String token=crypto.token(),code=crypto.code();MaXacThucOTP o=new MaXacThucOTP();o.maNguoiDung=u.getMaNguoiDung();o.email=email;o.loaiOTP=purpose;o.maYeuCau=crypto.digest(token);o.thoiDiemTao=now();o.thoiDiemHetHan=now().plusMinutes(5);o.trangThai="BI_HUY";o.maCodeHash="DISABLED";
        OtpMailGateway.Delivery delivery=OtpMailGateway.Delivery.FAILED;
        try{o.maCodeHash=crypto.otp(binding(o),code);delivery=mail.send(email,code);}catch(BusinessException e){LoggerFactory.getLogger(OtpService.class).warn("Thiếu cấu hình BIZ04_OTP_HMAC_KEY hợp lệ; không gửi mã.");}catch(RuntimeException e){LoggerFactory.getLogger(OtpService.class).warn("Gửi OTP có kết quả không xác định; vô hiệu mã.");}
        boolean sent=delivery==OtpMailGateway.Delivery.ACCEPTED;if(sent)o.trangThai="CHO_XAC_THUC";db.save(o);
        return new Result(token,sent,sent?SENT:delivery==OtpMailGateway.Delivery.RECIPIENT_MISSING?"Địa chỉ email nhận không tồn tại. Vui lòng kiểm tra lại.":FAILED);
    }
    public View view(String token,Long actor){return tx.execute(s->viewOf(challenge(token,actor)));}
    private static String maskEmail(String email){return email.substring(0,1)+"***"+email.substring(email.indexOf('@'));}
    private View viewOf(MaXacThucOTP o){long seconds="CHO_XAC_THUC".equals(o.trangThai)?Math.max(0,Math.min(300,Duration.between(now(),o.thoiDiemHetHan).getSeconds())):0;return new View(maskEmail(o.email),o.thoiDiemHetHan,o.thoiDiemTao.plusSeconds(cooldown),o.loaiOTP,seconds);}
    public Result startTakeover(Long actor,String email,String name,String ip){return tx.execute(s->{
        gate();NguoiDung u=db.lockEntity(NguoiDung.class,actor);
        Checks.require("CHO_TIEP_QUAN".equals(u.getTrangThai())&&"MAU".equals(u.getLoaiTaiKhoan()),"Tài khoản không còn chờ tiếp quản.");
        TiepQuanTaiKhoan t=db.get(TiepQuanTaiKhoan.class,actor);Checks.require(t.completed==null,"Tài khoản đã hoàn tất tiếp quản.");
        String address=AccountService.email(email),display=Checks.text(name,1,100,"Tên hiển thị");accounts.unique(address,actor);
        Result limited=sendLimit(u,"TIEP_QUAN",address,ip,null);if(limited!=null)return limited;
        t.pendingName=display;return issue(u,address,"TIEP_QUAN");
    });}
    public Result verifyTakeover(String token,Long actor,String code,String ip){return verify(token,actor,code,ip,true);}
    public Result verify(String token,Long actor,String code,String ip){return verify(token,actor,code,ip,false);}
    private Result verify(String token,Long actor,String code,String ip,boolean takeover){return verifyPurpose(token,actor,code,ip,takeover?"TIEP_QUAN":null);}
    private Result verifyPurpose(String token,Long actor,String code,String ip,String purpose){return tx.execute(s->{
        boolean takeover="TIEP_QUAN".equals(purpose);gate();MaXacThucOTP o=challengePurpose(token,actor,purpose);NguoiDung u=db.lockEntity(NguoiDung.class,o.maNguoiDung);
        if(!"CHO_XAC_THUC".equals(o.trangThai))return new Result(token,false,"Mã không còn hiệu lực hoặc đã được sử dụng. Vui lòng gửi lại mã.");
        if(!now().isBefore(o.thoiDiemHetHan)){o.trangThai="HET_HAN";return new Result(token,false,"Mã đã hết hạn. Vui lòng gửi lại mã.");}
        if(!quota("TRY",u.getMaNguoiDung(),o.email,ip))return new Result(token,false,"Bạn đã thử quá nhiều lần. Vui lòng thử lại sau một giờ.");
        boolean match=false;try{match=code!=null&&code.matches("[0-9]{6}")&&crypto.matches(binding(o),code,o.maCodeHash);}catch(BusinessException ex){o.trangThai="BI_HUY";return new Result(token,false,FAILED);}
        if(!match){o.soLanThuSai++;if(o.soLanThuSai>=5)o.trangThai="BI_HUY";return new Result(token,false,o.soLanThuSai>=5?"Mã đã bị hủy sau 5 lần nhập sai. Vui lòng gửi lại mã.":"Mã xác thực không đúng. Vui lòng kiểm tra lại.");}
        if("DANG_KY_LAI_CG".equals(o.loaiOTP)){
            if(!"TU_CHOI_CHUYEN_GIA".equals(u.getTrangThai())||!u.getEmail().equals(o.email))return new Result(token,false,"Hồ sơ đã thay đổi. Vui lòng đăng nhập lại.");
            o.trangThai="DA_SU_DUNG";o.thoiDiemSuDung=now();return new Result(null,true,"Đã xác thực. Bạn có 15 phút để chỉnh sửa và gửi lại hồ sơ.");
        }
        if("DANG_KY".equals(o.loaiOTP)){
            if(!"CHO_XAC_THUC".equals(u.getTrangThai())||!u.getEmail().equals(o.email))return new Result(token,false,"Yêu cầu đăng ký không còn phù hợp.");
        }else{
            if(!(takeover?"CHO_TIEP_QUAN".equals(u.getTrangThai())&&"MAU".equals(u.getLoaiTaiKhoan()):"HOAT_DONG".equals(u.getTrangThai())))return new Result(token,false,"Tài khoản chưa ở trạng thái phù hợp.");
            // Kiểm tra lại tính duy nhất khi dùng mã, vẫn giữ email cũ nếu xung đột.
            if(db.count("select count(v) from NguoiDung v where lower(v.email)=:e and v.maNguoiDung<>:u","e",o.email,"u",u.getMaNguoiDung())>0){o.trangThai="BI_HUY";return new Result(token,false,"Email đã được sử dụng. Vui lòng chọn email khác trong hồ sơ.");}
            u.setEmail(o.email);
        }
        o.trangThai="DA_SU_DUNG";o.thoiDiemSuDung=now();u.setTrangThaiEmail("DA_XAC_THUC");u.setThoiDiemXacThucEmail(now());u.setNgayCapNhat(now());if("DANG_KY".equals(o.loaiOTP)){if("CHUYEN_GIA".equals(u.getNguonDangKy()))experts.emailVerified(u);else u.setTrangThai("HOAT_DONG");}db.flush();
        return new Result(null,true,"CHUYEN_GIA".equals(u.getNguonDangKy())&&"CHO_DUYET_CHUYEN_GIA".equals(u.getTrangThai())?ExpertApplications.PENDING:"Xác thực email thành công. Hãy đăng nhập.");
    });}
}
