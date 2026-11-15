package vn.biz04.service;
import org.springframework.stereotype.Component;
@Component
public class SimulatedPaymentGateway implements PaymentGateway {
    public boolean execute(String operationKey,String demoResult){Checks.require("SUCCESS".equals(demoResult)||"FAIL".equals(demoResult),"Kết quả mô phỏng không hợp lệ.");return "SUCCESS".equals(demoResult);}
}
