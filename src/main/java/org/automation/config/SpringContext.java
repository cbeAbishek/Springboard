package org.automation.config;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Spring context holder for accessing Spring beans from non-Spring managed classes
 * like TestNG listeners
 */
@Component
public class SpringContext implements ApplicationContextAware {

    private static ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        SpringContext.context = applicationContext;
    }

    /**
     * Get Spring bean by class type
     */
    public static <T> T getBean(Class<T> beanClass) {
        if (context == null) {
            throw new IllegalStateException("Spring ApplicationContext not initialized");
        }
        return context.getBean(beanClass);
    }

    /**
     * Check if Spring context is available
     */
    public static boolean isContextAvailable() {
        return context != null;
    }

    /**
     * Get the application context
     */
    public static ApplicationContext getContext() {
        return context;
    }
}

