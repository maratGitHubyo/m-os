package com.mos.seed;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
@ConditionalOnProperty(prefix = "mos.demo.seed", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class DemoDataSeederRunner implements ApplicationRunner {

    private final DemoDataSeeder demoDataSeeder;

    @Override
    public void run(ApplicationArguments args) {
        demoDataSeeder.seedDemoData();
    }
}
