package vn.biz04.web;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.support.TransactionTemplate;
import vn.biz04.data.Store;
import vn.biz04.domain.NguoiDung;
import java.security.Principal;
import java.util.Map;

@ControllerAdvice(assignableTypes=Web.class)
public class HireWorkspaceAdvice {
    private final Store db;private final TransactionTemplate tx;
    public HireWorkspaceAdvice(Store db,TransactionTemplate tx){this.db=db;this.tx=tx;}
    @ModelAttribute("hireCounts") public Map<String,Long> counts(Principal principal,HttpServletRequest request){
        if(principal==null||!request.getRequestURI().equals("/bang-dieu-khien"))return Map.of();
        return tx.execute(s->{var user=db.first("from NguoiDung u where u.email=:email and u.trangThai='HOAT_DONG'",NguoiDung.class,"email",principal.getName());if(user.isEmpty())return Map.of();Long id=user.get().getMaNguoiDung();return Map.of("sent",db.count("select count(h) from DeNghiThue h where h.customer=:u","u",id),"received",db.count("select count(h) from DeNghiThue h where h.expert=:u","u",id));});
    }
}
