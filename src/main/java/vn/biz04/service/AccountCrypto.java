package vn.biz04.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

@Component
public class AccountCrypto {
    private final String otpKey,cccdKey;
    private final SecureRandom random=new SecureRandom();
    public AccountCrypto(@Value("${biz.otp.hmac-key:}")String otpKey,@Value("${biz.cccd.key:}")String cccdKey){this.otpKey=otpKey;this.cccdKey=cccdKey;}
    private byte[] key(String value,String name){
        try {byte[] bytes=Base64.getDecoder().decode(value);if(bytes.length!=32)throw new IllegalArgumentException();return bytes;}
        catch(IllegalArgumentException ex){throw new BusinessException("Thiếu cấu hình hoặc khóa không hợp lệ: "+name+" (Base64, 32 byte).");}
    }
    public String token(){byte[] b=new byte[32];random.nextBytes(b);return Base64.getUrlEncoder().withoutPadding().encodeToString(b);}
    public String code(){return String.format(Locale.ROOT,"%06d",random.nextInt(1_000_000));}
    public String digest(String text){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));}catch(GeneralSecurityException ex){throw new IllegalStateException(ex);}}
    public String otp(String binding,String code){
        try {Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(key(otpKey,"BIZ04_OTP_HMAC_KEY"),"HmacSHA256"));return HexFormat.of().formatHex(mac.doFinal((binding+":"+code).getBytes(StandardCharsets.UTF_8)));}
        catch(GeneralSecurityException ex){throw new IllegalStateException("Không khởi tạo được bảo vệ OTP.");}
    }
    public boolean matches(String binding,String code,String expected){return MessageDigest.isEqual(otp(binding,code).getBytes(StandardCharsets.US_ASCII),expected.getBytes(StandardCharsets.US_ASCII));}
    public String encryptCccd(Long user,String value){
        byte[] k=key(cccdKey,"BIZ04_CCCD_KEY"),nonce=new byte[12];random.nextBytes(nonce);
        try {Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,new SecretKeySpec(k,"AES"),new GCMParameterSpec(128,nonce));c.updateAAD(("SangConnect:CCCD:v1:"+user).getBytes(StandardCharsets.UTF_8));return "v1:"+Base64.getEncoder().encodeToString(nonce)+":"+Base64.getEncoder().encodeToString(c.doFinal(value.getBytes(StandardCharsets.US_ASCII)));}
        catch(GeneralSecurityException ex){throw new BusinessException("Chưa mã hóa được CCCD. Hồ sơ chưa được lưu.");}
    }
}
