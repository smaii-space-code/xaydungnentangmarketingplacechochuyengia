package vn.biz04.security;

import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import vn.biz04.service.*;
import java.io.IOException;

@Component
public class ExpertLoginFailure implements AuthenticationFailureHandler {
    public static final String PROOF="SANG_EXPERT_PROOF",EDIT="SANG_EXPERT_EDIT",OTP="SANG_EXPERT_RETRY_OTP";
    public static class ReviewRequired extends AccountStatusException {
        public final Long id;
        public ReviewRequired(Long id){super("Expert application requires review");this.id=id;}
    }
    private final ExpertApplications experts;
    public ExpertLoginFailure(ExpertApplications experts){this.experts=experts;}
    @Override public void onAuthenticationFailure(HttpServletRequest req,HttpServletResponse res,AuthenticationException exception)throws IOException {
        HttpSession session=req.getSession();clear(session);
        if(exception instanceof ReviewRequired review){
            try{var proof=experts.proof(review.id);req.changeSessionId();session.setAttribute(PROOF,proof);res.sendRedirect(req.getContextPath()+"/dang-ky/chuyen-gia/trang-thai");return;}catch(BusinessException ignored){}
        }
        res.sendRedirect(req.getContextPath()+"/dang-nhap?error");
    }
    public static void clear(HttpSession session){session.removeAttribute(PROOF);session.removeAttribute(EDIT);session.removeAttribute(OTP);}
}
