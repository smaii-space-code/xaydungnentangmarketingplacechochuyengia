package vn.biz04.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.mail.javamail.*;
import org.eclipse.angus.mail.smtp.SMTPAddressFailedException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import org.slf4j.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
public class SmtpOtpMailGateway implements OtpMailGateway {
    private static final Logger LOG=LoggerFactory.getLogger(SmtpOtpMailGateway.class);
    private final JavaMailSenderImpl sender=new JavaMailSenderImpl(); private final String from;
    public SmtpOtpMailGateway(@Value("${biz.smtp.host:}")String host,@Value("${biz.smtp.port:587}")int port,@Value("${biz.smtp.username:}")String username,@Value("${biz.smtp.password:}")String password,@Value("${biz.smtp.from:}")String from){
        this.from=from;sender.setHost(host);sender.setPort(port);sender.setUsername(username);sender.setPassword(password);sender.setDefaultEncoding("UTF-8");
        var p=sender.getJavaMailProperties();p.setProperty("mail.smtp.auth","true");p.setProperty("mail.smtp.starttls.enable","true");p.setProperty("mail.smtp.starttls.required","true");p.setProperty("mail.smtp.ssl.checkserveridentity","true");
        p.setProperty("mail.smtp.connectiontimeout","10000");p.setProperty("mail.smtp.timeout","10000");p.setProperty("mail.smtp.writetimeout","10000");p.setProperty("mail.debug","false");
    }
    public Delivery send(String recipient,String code){
        if(sender.getHost()==null||sender.getHost().isBlank()||sender.getUsername()==null||sender.getUsername().isBlank()||sender.getPassword()==null||sender.getPassword().isBlank()||from.isBlank()){
            LOG.warn("Chưa cấu hình đủ BIZ04_SMTP_HOST/USERNAME/PASSWORD/FROM; không gửi OTP.");return Delivery.FAILED;
        }
        try{
            var message=sender.createMimeMessage();var helper=new MimeMessageHelper(message,StandardCharsets.UTF_8.name());
            helper.setFrom(new InternetAddress(from,"Sang Connect","UTF-8"));helper.setTo(recipient);helper.setSubject("Mã xác thực email Sang Connect");
            helper.setText("Mã xác thực của bạn là: "+code+"\nMã có hiệu lực 5 phút. Không chia sẻ mã với người khác.\nMã này chỉ xác thực email, không xác thực điện thoại hoặc danh tính.",false);
            sender.send(message);return Delivery.ACCEPTED;
        }catch(Exception ex){
            // Không log exception/raw SMTP: có thể chứa email, nội dung thư và bí mật.
            Delivery result=classify(ex);LOG.warn("Gửi OTP không thành công; loại kết quả: {}",result);return result;
        }
    }
    public static Delivery classify(Throwable error){
        Set<Throwable> seen=Collections.newSetFromMap(new IdentityHashMap<>());Deque<Throwable> queue=new ArrayDeque<>();queue.add(error);
        while(!queue.isEmpty()){
            Throwable t=queue.remove();if(!seen.add(t))continue;
            if(t instanceof SMTPAddressFailedException s&&s.getReturnCode()==550&&s.getCommand()!=null&&s.getCommand().toUpperCase(Locale.ROOT).startsWith("RCPT TO")&&s.getMessage()!=null&&s.getMessage().matches("(?s)^550[ -]5\\.1\\.1(?:[ \\r\\n].*)?$"))return Delivery.RECIPIENT_MISSING;
            if(t.getCause()!=null)queue.add(t.getCause());
            if(t instanceof MessagingException m&&m.getNextException()!=null)queue.add(m.getNextException());
            if(t instanceof org.springframework.mail.MailSendException m)queue.addAll(Arrays.asList(m.getMessageExceptions()));
        }
        return Delivery.FAILED;
    }
}
