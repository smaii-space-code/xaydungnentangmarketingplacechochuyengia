package vn.biz04.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import vn.biz04.service.CatalogDiscovery;
import java.io.IOException;

/** A narrow return target for catalogue details; no arbitrary next/redirect URL. */
@Component
public class DiscoveryLoginFlow implements AuthenticationEntryPoint,AuthenticationSuccessHandler {
    public static final String TARGET="SANG_DISCOVERY_DETAIL";
    private final CatalogDiscovery discovery;
    public DiscoveryLoginFlow(CatalogDiscovery discovery){this.discovery=discovery;}
    public static boolean allowed(String path){return path!=null&&path.matches("/(chuyen-gia|dich-vu/xem)/[1-9][0-9]{0,17}");}
    public static boolean pending(HttpSession session){return session!=null&&session.getAttribute(TARGET) instanceof String path&&allowed(path);}
    @Override public void commence(HttpServletRequest request,HttpServletResponse response,AuthenticationException exception)throws IOException {
        String accept=request.getHeader("Accept");
        if(accept!=null&&accept.toLowerCase(java.util.Locale.ROOT).contains("json")){response.setStatus(401);return;}
        String path=request.getRequestURI().substring(request.getContextPath().length());
        if(allowed(path))request.getSession().setAttribute(TARGET,path);
        response.sendRedirect(request.getContextPath()+"/dang-ky?chiTiet=1");
    }
    @Override public void onAuthenticationSuccess(HttpServletRequest request,HttpServletResponse response,Authentication authentication)throws IOException,ServletException {
        String target=null;HttpSession session=request.getSession(false);
        if(session!=null){Object stored=session.getAttribute(TARGET);session.removeAttribute(TARGET);if(stored instanceof String path&&allowed(path)&&discovery.availableTarget(path))target=path;}
        new HttpSessionRequestCache().removeRequest(request,response);
        response.sendRedirect(request.getContextPath()+(target==null?"/bang-dieu-khien":target));
    }
}
