package com.mysms.demoseed;

import java.time.LocalDate;
import tools.jackson.databind.json.JsonMapper;

/** The command line: {@code mvn -pl demo-seed exec:java -Dmysms.demo.baseUrl=… -Dmysms.demo.action=seed|remove}. */
public final class DemoSeedApplication {

    private DemoSeedApplication() {
    }

    public static void main(String[] args) {
        DemoSettings settings = DemoSettings.from(System.getProperties(), System.getenv(), LocalDate.now());
        new DemoSeed(settings, JsonMapper.builder().build()).run();
    }
}
