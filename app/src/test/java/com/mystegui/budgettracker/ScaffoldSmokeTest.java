package com.mystegui.budgettracker;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/** T9 done-when: the Spring half of the stack boots headlessly, without any web server. */
@SpringBootTest(properties = { "spring.main.web-application-type=none" })
class ScaffoldSmokeTest {

    private final ApplicationContext context;

    ScaffoldSmokeTest(ApplicationContext context) {
        this.context = context;
    }

    @Test
    void springBootsWithNoWebStack() {
        assertThat(context.getBeanProvider(BudgetTrackerApp.class).getIfAvailable()).isNotNull();
    }

    @Test
    void launchBeanIsResolvableFromTheContext() {
        assertThat(context.getBean(UiLauncher.class)).isNotNull();
    }
}
