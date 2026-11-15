package vn.biz04.service;
import java.math.*;
public final class Checks {
    private Checks() {}
    public static void require(boolean value,String message) { if(!value) throw new BusinessException(message); }
    public static String text(String value,int min,int max,String label) {
        String s=value==null?"":value.trim(); require(s.length()>=min&&s.length()<=max,label+" cần từ "+min+" đến "+max+" ký tự."); return s;
    }
    public static BigDecimal money(BigDecimal value,boolean zero) {
        require(value!=null&&value.scale()<=0&&value.compareTo(BigDecimal.valueOf(1_000_000_000_000L))<=0&&(zero?value.signum()>=0:value.signum()>0),"Số tiền phải là số nguyên hợp lệ, tối đa 1.000 tỷ đồng."); return value.setScale(0);
    }
}
