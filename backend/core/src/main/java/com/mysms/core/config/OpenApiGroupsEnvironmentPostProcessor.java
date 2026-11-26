package com.mysms.core.config;

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * One springdoc group per area the process serves ({@code mysms.auth.modules}): {@code /v3/api-docs/<area>} documents
 * the area's {@code /v1/<area>/**} paths, the source of the clients' SDK namespace of that area. Declared through
 * springdoc's {@code springdoc.group-configs} so its auto-configuration sees the groups when it decides to serve them;
 * added last, so an operator's own group configuration wins.
 */
public final class OpenApiGroupsEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    /** Name of the property source holding the groups. */
    public static final String PROPERTY_SOURCE = "mysmsOpenApiGroups";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> groups = new HashMap<>();
        for (int i = 0; environment.containsProperty("mysms.auth.modules[" + i + "]"); i++) {
            String area = environment.getProperty("mysms.auth.modules[" + i + "]");
            groups.put("springdoc.group-configs[" + i + "].group", area);
            groups.put("springdoc.group-configs[" + i + "].paths-to-match", "/v1/" + area + "/**");
        }
        if (!groups.isEmpty()) {
            environment.getPropertySources().addLast(new MapPropertySource(PROPERTY_SOURCE, groups));
        }
    }

    /** After the module files ({@link ModulePropertiesEnvironmentPostProcessor}), which may name the areas. */
    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
