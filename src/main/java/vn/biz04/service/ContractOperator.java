package vn.biz04.service;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
@Component
public class ContractOperator {
    private final String name,phone,representative;
    public ContractOperator(@Value("${biz.contract.operator-name:}")String name,@Value("${biz.contract.operator-phone:}")String phone,@Value("${biz.contract.operator-representative:}")String representative){this.name=name;this.phone=phone;this.representative=representative;}
    public record Info(String platform,String operator,String phone,String representative,String role){}
    public Info snapshot(){return new Info("Sang Connect",Checks.text(name,2,255,"Tên đơn vị/người vận hành"),Checks.text(phone,8,30,"Liên hệ đơn vị vận hành"),representative==null?"":representative.trim(),"Hỗ trợ nền tảng, ghi nhận giao dịch, ký quỹ mô phỏng, thu phí và xử lý tranh chấp theo chính sách hệ thống. Không cung cấp dịch vụ hoặc trả tiền thay hai bên.");}
}
