package vn.biz04.security;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.transaction.support.TransactionTemplate;
import vn.biz04.data.Store;
import vn.biz04.domain.NguoiDung;
import java.util.*;
import vn.biz04.service.*;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
@Configuration
public class SecurityConfig {
    @Bean org.springframework.boot.web.servlet.FilterRegistrationBean<AccountSessionFilter> sessionFilterRegistration(AccountSessionFilter filter){var registration=new org.springframework.boot.web.servlet.FilterRegistrationBean<>(filter);registration.setEnabled(false);return registration;}
    @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder();}
    @Bean UserDetailsService userDetailsService(Store db,TransactionTemplate tx) {
        return email->tx.execute(s->{NguoiDung u=db.first("from NguoiDung u where lower(u.email)=:e",NguoiDung.class,"e",email.toLowerCase(Locale.ROOT)).orElseThrow(()->new UsernameNotFoundException("Tài khoản không tồn tại"));List<String> roles=new ArrayList<>();if(u.getLaKhachHang())roles.add("KHACH_HANG");if(u.getLaChuyenGia())roles.add("CHUYEN_GIA");if(u.getLaQuanTriVien())roles.add("QUAN_TRI");return new AccountPrincipal(u,roles);});
    }
    @Bean DaoAuthenticationProvider accountProvider(UserDetailsService users,PasswordEncoder encoder,Store db,TransactionTemplate tx){
        DaoAuthenticationProvider provider=new DaoAuthenticationProvider(users);provider.setPasswordEncoder(encoder);
        // State is private: always validate credentials before showing pending/rejection information.
        provider.setPreAuthenticationChecks(details->{});
        provider.setPostAuthenticationChecks(details->{
            if(!(details instanceof AccountPrincipal p))throw new DisabledException("Account unavailable");
            tx.executeWithoutResult(s->{NguoiDung u=db.get(NguoiDung.class,p.id());
                if(p.epoch()!=u.getPhienBaoMat())throw new DisabledException("Account changed");
                if("CHUYEN_GIA".equals(u.getNguonDangKy())&&Set.of("CHO_DUYET_CHUYEN_GIA","TU_CHOI_CHUYEN_GIA").contains(u.getTrangThai())&&"DA_XAC_THUC".equals(u.getTrangThaiEmail()))throw new ExpertLoginFailure.ReviewRequired(p.id());
                if(!"HOAT_DONG".equals(u.getTrangThai())||(u.getLaChuyenGia()&&!ExpertEligibility.eligible(db,u)))throw new DisabledException("Account unavailable");
            });
            if(!details.isAccountNonLocked())throw new LockedException("Account locked");
            if(!details.isAccountNonExpired())throw new AccountExpiredException("Account expired");
            if(!details.isCredentialsNonExpired())throw new CredentialsExpiredException("Credentials expired");
        });return provider;
    }
    @Bean @org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication SecurityFilterChain filterChain(HttpSecurity http,DiscoveryLoginFlow discoveryLogin,AccountSessionFilter accountSessions,DaoAuthenticationProvider accountProvider,ExpertLoginFailure loginFailure)throws Exception {
        var details=new org.springframework.security.web.util.matcher.OrRequestMatcher(new org.springframework.security.web.util.matcher.AntPathRequestMatcher("/chuyen-gia/**"),new org.springframework.security.web.util.matcher.AntPathRequestMatcher("/dich-vu/xem/**"));
        var entryPoints=new LinkedHashMap<org.springframework.security.web.util.matcher.RequestMatcher,org.springframework.security.web.AuthenticationEntryPoint>();entryPoints.put(details,discoveryLogin);
        var entryPoint=new org.springframework.security.web.authentication.DelegatingAuthenticationEntryPoint(entryPoints);entryPoint.setDefaultEntryPoint(new org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint("/dang-nhap"));
        return http.authenticationProvider(accountProvider).authorizeHttpRequests(a->a.requestMatchers("/","/trang/trangchu","/ve-sang-connect","/images/mai-phuoc-sang.jpg","/dich-vu","/dang-nhap","/dang-ky","/dang-ky/**","/xac-thuc-email","/xac-thuc-email/**","/css/**","/js/**","/videos/**","/error").permitAll().anyRequest().authenticated())
          .exceptionHandling(e->e.authenticationEntryPoint(entryPoint))
          .formLogin(f->f.loginPage("/dang-nhap").usernameParameter("email").passwordParameter("password").successHandler(discoveryLogin).failureHandler(loginFailure).permitAll())
          .addFilterAfter(accountSessions,org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
          .logout(l->l.logoutUrl("/dang-xuat").logoutSuccessUrl("/"))
          .headers(h->h.contentSecurityPolicy(c->c.policyDirectives("default-src 'self'; style-src 'self'; img-src 'self' data:; form-action 'self'; frame-ancestors 'none'; object-src 'none'")))
          .build();
    }
}

