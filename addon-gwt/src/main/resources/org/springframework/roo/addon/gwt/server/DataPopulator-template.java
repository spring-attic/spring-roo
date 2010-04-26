package __TOP_LEVEL_PACKAGE__.server;

import java.util.Date;

import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import __TOP_LEVEL_PACKAGE__.server.domain.Employee;
import __TOP_LEVEL_PACKAGE__.server.domain.Report;

@Component
public class DataPopulator implements ApplicationListener<ContextRefreshedEvent>{

        @Override
        @Transactional
        public void onApplicationEvent(ContextRefreshedEvent event) {
        	if (event.getApplicationContext().getParent() == null) {
        	    Employee abc = new Employee();
        	    abc.setUserName("abc");
        	    abc.setDisplayName("Able B. Charlie");
        	    abc.persist();
        	    abc.setSupervisor(abc);

        	    Employee def = new Employee();
        	    def.setUserName("def");
        	    def.setDisplayName("Delta E. Foxtrot");
        	    def.setSupervisor(abc);
        	    def.persist();

        	    Employee ghi = new Employee();
        	    ghi.setUserName("ghi");
        	    ghi.setDisplayName("George H. Indigo");
        	    ghi.setSupervisor(abc);
        	    ghi.persist();

        	    Report abc1 = new Report();
        	    abc1.setCreated(new Date());
        	    abc1.setPurpose("Spending lots of money");
        	    abc1.persist();

        	    Report abc2 = new Report();
        	    abc2.setCreated(new Date());
        	    abc2.setPurpose("Team building diamond cutting offsite");
        	    abc2.persist();

        	    Report abc3 = new Report();
        	    abc3.setCreated(new Date());
        	    abc3.setPurpose("Visit to Istanbul");
        	    abc3.persist();

        	    Report def1 = new Report();
        	    def1.setCreated(new Date());
        	    def1.setPurpose("Money laundering");
        	    def1.persist();

        	    Report def2 = new Report();
        	    def2.setCreated(new Date());
        	    def2.setPurpose("Donut day");
        	    def2.persist();

        	    Report ghi1 = new Report();
        	    ghi1.setCreated(new Date());
        	    ghi1.setPurpose("ISDN modem for telecommuting");
        	    ghi1.persist();

        	    Report ghi2 = new Report();
        	    ghi2.setCreated(new Date());
        	    ghi2.setPurpose("Sushi offsite");
        	    ghi2.persist();

        	    Report ghi3 = new Report();
        	    ghi3.setCreated(new Date());
        	    ghi3.setPurpose("Baseball card research");
        	    ghi3.persist();

        	    Report ghi4 = new Report();
        	    ghi4.setCreated(new Date());
        	    ghi4.setPurpose("Potato chip cooking offsite");
        	    ghi4.persist();
        	}
        }
}