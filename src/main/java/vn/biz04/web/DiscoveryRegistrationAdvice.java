package vn.biz04.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import vn.biz04.security.DiscoveryLoginFlow;

@ControllerAdvice(assignableTypes={AccountWeb.class,ExpertRegistrationWeb.class})
public class DiscoveryRegistrationAdvice {
    @ModelAttribute("detailRegistration") public boolean detailRegistration(HttpServletRequest request){return DiscoveryLoginFlow.pending(request.getSession(false));}
}
