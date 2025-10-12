package org.example.config;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
public class QuartzConfig {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private ApplicationContext applicationContext;

    @Bean
    public SchedulerFactoryBean schedulerFactoryBean() {
        SchedulerFactoryBean factory = new SchedulerFactoryBean();
        factory.setDataSource(dataSource);
        factory.setQuartzProperties(quartzProperties());
        factory.setJobFactory(new AutowiringSpringBeanJobFactory());
        factory.setWaitForJobsToCompleteOnShutdown(true);
        factory.setStartupDelay(30); // Delay startup to ensure application is fully loaded
        return factory;
    }

    @Bean
    @Primary
    public Scheduler scheduler(SchedulerFactoryBean schedulerFactoryBean) throws SchedulerException {
        Scheduler scheduler = schedulerFactoryBean.getScheduler();
        scheduler.getContext().put("applicationContext", applicationContext);
        return scheduler;
    }

    private Properties quartzProperties() {
        Properties properties = new Properties();

        // Scheduler properties
        properties.setProperty("org.quartz.scheduler.instanceName", "SpringBootQuartzScheduler");
        properties.setProperty("org.quartz.scheduler.instanceId", "AUTO");

        // ThreadPool properties
        properties.setProperty("org.quartz.threadPool.class", "org.quartz.simpl.SimpleThreadPool");
        properties.setProperty("org.quartz.threadPool.threadCount", "10");
        properties.setProperty("org.quartz.threadPool.threadPriority", "5");

        // JobStore properties - Use in-memory store for simplicity
        properties.setProperty("org.quartz.jobStore.class", "org.quartz.simpl.RAMJobStore");

        return properties;
    }
}
