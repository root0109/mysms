package com.mysms.core.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

/**
 * Adds the modules' own {@code application.properties} to the monolith after Spring Boot's configuration, so app's
 * file, environment variables, system properties and the command line override them. Only a module's file counts:
 * one whose {@code spring.application.name} has its operation table beside it, never a third-party jar's file.
 */
public final class ModulePropertiesEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    /** Prefix of each module file's property source name, followed by the module's name. */
    public static final String PROPERTY_SOURCE_PREFIX = "mysmsModuleProperties:";

    static final String FILE = "application.properties";
    private static final String NAME = "spring.application.name";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        ClassLoader loader = application.getClassLoader() != null ? application.getClassLoader()
                : ModulePropertiesEnvironmentPostProcessor.class.getClassLoader();
        for (ModuleFile module : moduleFiles(loader)) {
            environment.getPropertySources().addLast(
                    new PropertiesPropertySource(PROPERTY_SOURCE_PREFIX + module.name(), module.properties()));
        }
    }

    /** After Spring Boot's configuration data: the module files only fill what nothing else has set. */
    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    /**
     * The module files the application's own file does not already cover: every {@code application.properties} on
     * the classpath after the first (the one Spring Boot loads) that belongs to a module, in classpath order.
     */
    static List<ModuleFile> moduleFiles(ClassLoader loader) {
        List<URL> files;
        try {
            files = Collections.list(loader.getResources(FILE));
        } catch (IOException e) {
            throw new UncheckedIOException("could not list the application.properties files on the classpath", e);
        }
        List<ModuleFile> modules = new ArrayList<>();
        for (URL file : files.subList(Math.min(1, files.size()), files.size())) {
            Properties properties = load(file);
            String name = properties.getProperty(NAME, "");
            if (name.matches("[a-z]+") && exists(sibling(file, "META-INF/mysms/operations/" + name + ".json"))) {
                modules.add(new ModuleFile(name, properties));
            }
        }
        return modules;
    }

    private static Properties load(URL file) {
        try {
            return PropertiesLoaderUtils.loadProperties(new UrlResource(file));
        } catch (IOException e) {
            throw new UncheckedIOException("could not read " + file, e);
        }
    }

    private static URL sibling(URL file, String path) {
        String location = file.toString();
        try {
            return URI.create(location.substring(0, location.length() - FILE.length()) + path).toURL();
        } catch (MalformedURLException | IllegalArgumentException e) {
            throw new IllegalStateException("not a classpath location: " + file, e);
        }
    }

    private static boolean exists(URL url) {
        try {
            return new UrlResource(url).exists();
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /** One module's {@code application.properties}. */
    record ModuleFile(String name, Properties properties) {
    }
}
