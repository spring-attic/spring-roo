package __TOP_LEVEL_PACKAGE__.server;

import java.util.Date;

import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import __TOP_LEVEL_PACKAGE__.server.domain.Employee;
//import __TOP_LEVEL_PACKAGE__.servlet.domain.Report;

@Component
public class DataPopulator implements ApplicationListener<ContextRefreshedEvent>{

        @Override
        public void onApplicationEvent(ContextRefreshedEvent event) {
        }
}