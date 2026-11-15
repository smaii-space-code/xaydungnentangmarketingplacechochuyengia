package vn.biz04.config;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.context.ConfigurableApplicationContext;
import vn.biz04.service.*;
import java.io.Console;
import java.util.Arrays;

@Component @org.springframework.core.annotation.Order(100) @ConditionalOnProperty(name="biz.takeover.cli",havingValue="true")
public class TakeoverCommand implements ApplicationRunner {
    private final AccountTakeoverService takeover;private final ConfigurableApplicationContext context;
    public TakeoverCommand(AccountTakeoverService takeover,ConfigurableApplicationContext context){this.takeover=takeover;this.context=context;}
    private String input(ApplicationArguments arguments,Console console,String option,String prompt){var values=arguments.getOptionValues(option);return values==null?console.readLine(prompt):values.size()==1?values.get(0):null;}
    @Override public void run(ApplicationArguments arguments){
        Console console=System.console();if(console==null){System.err.println("Mo PowerShell truc tiep tren may va chay tiep-quan-tai-khoan.ps1; khong chuyen huong dau ra vao log.");context.close();return;}
        char[] password=null,again=null;
        try{
            Long id=Long.valueOf(input(arguments,console,"takeover-id","ID tai khoan can tiep quan: "));
            String name=input(arguments,console,"takeover-name","Ten hien thi chinh thuc: "),email=input(arguments,console,"takeover-email","Email rieng ban thuc su kiem soat: ");
            console.printf("Tiep quan ID %d - %s - %s%n",id,name,email);
            var sent=takeover.start(id,name,email);console.printf("%s%n",sent.message());if(!sent.success())return;
            password=console.readPassword("Tu dat mat khau moi (toi thieu 8 ky tu, khong hien ky tu): ");again=console.readPassword("Nhap lai mat khau: ");
            for(int n=0;n<5;n++){char[] code=console.readPassword("Ma OTP vua nhan qua email (khong chia se qua chat): ");try{var result=takeover.complete(id,sent.token(),new String(code),new String(password),new String(again));console.printf("%s%n",result.message());if(result.success())break;}finally{Arrays.fill(code,'\0');}}
        }catch(BusinessException|IllegalArgumentException e){console.printf("%s%n",e.getMessage());}
        finally{if(password!=null)Arrays.fill(password,'\0');if(again!=null)Arrays.fill(again,'\0');context.close();}
    }
}
