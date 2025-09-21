package org.example.scheduler;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class SchedulerInitializer {

    private static final Logger log = LoggerFactory.getLogger(SchedulerInitializer.class);

    @Autowired
    private Scheduler quartzScheduler;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private TestScheduler testScheduler;

    @EventListener(ContextRefreshedEvent.class)
    public void initializeScheduler() {
        try {
            // Add ApplicationContext to scheduler context for Spring bean access
            quartzScheduler.getContext().put("applicationContext", applicationContext);

            if (!quartzScheduler.isStarted()) {
                quartzScheduler.start();
                log.info("Quartz scheduler started successfully");
            }
            // (Re)register active schedules
            testScheduler.scheduleAllActiveOnStartup();
        } catch (SchedulerException e) {
            log.error("Failed to initialize Quartz scheduler", e);
            throw new RuntimeException("Scheduler initialization failed", e);
        }
    }
}
