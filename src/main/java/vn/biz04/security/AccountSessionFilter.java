package vn.biz04.security;
import org.springframework.stereotype.Component;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import vn.biz04.data.Store;
import vn.biz04.domain.NguoiDung;
import vn.biz04.service.ExpertEligibility;
import java.io.IOException;

@Component
public class AccountSessionFilter extends OncePerRequestFilter {
    private final Store db;private final TransactionTemplate tx;
    public AccountSessionFilter(Store db,TransactionTemplate tx){this.db=db;this.tx=tx;}
    @Override protected boolean shouldNotFilter(HttpServletRequest r){return r.getRequestURI().matches("^/(css|js|images|videos)/.*")||r.getRequestURI().equals("/error");}
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws ServletException,IOException{
        var auth=SecurityContextHolder.getContext().getAuthentication();
        if(auth!=null&&auth.isAuthenticated()&&!(auth instanceof AnonymousAuthenticationToken)){
            boolean valid=Boolean.TRUE.equals(tx.execute(s->db.first("from NguoiDung u where u.email=:email",NguoiDung.class,"email",auth.getName()).map(u->"HOAT_DONG".equals(u.getTrangThai())&&(!u.getLaChuyenGia()||ExpertEligibility.eligible(db,u))&&(auth.getPrincipal() instanceof AccountPrincipal p?p.id().equals(u.getMaNguoiDung())&&p.epoch()==u.getPhienBaoMat():u.getPhienBaoMat()==0)).orElse(false)));
            if(!valid){var session=request.getSession(false);if(session!=null)session.invalidate();SecurityContextHolder.clearContext();response.sendRedirect(request.getContextPath()+"/dang-nhap?session=expired");return;}
        }
        chain.doFilter(request,response);
    }
}
