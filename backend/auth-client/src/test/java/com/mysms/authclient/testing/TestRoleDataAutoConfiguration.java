package com.mysms.authclient.testing;

import com.mysms.authclient.IamAuthzGrpcClient;
import com.mysms.authclient.RolePermissionSource;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Puts {@link TestRoleData} in front of the role data source of every component test (auth-client's test jar is on
 * the component tests' classpath only): over the gRPC client of iam, which a module's tests have no iam for, it
 * answers alone; over iam's own source it adds the test roles to iam's answers.
 */
@AutoConfiguration
public final class TestRoleDataAutoConfiguration {

    private TestRoleDataAutoConfiguration() {
    }

    /** Static, as a post-processor bean must be, so the configuration is not created early. */
    @Bean
    static BeanPostProcessor testRoleData() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String name) {
                if (bean instanceof TestRoleData || !(bean instanceof RolePermissionSource source)) {
                    return bean;
                }
                return new TestRoleData(bean instanceof IamAuthzGrpcClient ? null : source);
            }
        };
    }
}
