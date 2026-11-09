#!/usr/bin/env python3
import pathlib
ROOT = str(pathlib.Path(__file__).resolve().parent.parent.parent / 'backend')
"""Writes the generated-code module POMs (contracts, sdk): one OpenAPI Generator execution per module contract."""
MODULES = ["academics","admission","assessment","attendance","careers","comms","facility","finance","frontoffice","hr",
           "iam","lms","master","org","payroll","people","platform","sis","timetable","transport","library"]
MODULES = sorted(MODULES)
assert len(MODULES) == 21

BUNDLE = """      <!-- 1. the generator jar (it also carries Jackson for the bundler) -->
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-dependency-plugin</artifactId>
        <executions>
          <execution>
            <id>openapi-generator-cli</id>
            <phase>initialize</phase>
            <goals>
              <goal>copy</goal>
            </goals>
            <configuration>
              <artifactItems>
                <artifactItem>
                  <groupId>org.openapitools</groupId>
                  <artifactId>openapi-generator-cli</artifactId>
                  <version>${openapi-generator.version}</version>
                  <destFileName>openapi-generator-cli.jar</destFileName>
                </artifactItem>
              </artifactItems>
              <outputDirectory>${project.build.directory}/tools</outputDirectory>
            </configuration>
          </execution>
        </executions>
      </plugin>
      <!-- 2. bundle each module contract (api/tools/OpenApiBundler.java, shared with the TypeScript SDK) -->
      <plugin>
        <groupId>org.codehaus.mojo</groupId>
        <artifactId>exec-maven-plugin</artifactId>
        <executions>
          <execution>
            <id>bundle-contracts</id>
            <phase>generate-sources</phase>
            <goals>
              <goal>exec</goal>
            </goals>
            <configuration>
              <executable>${java.home}/bin/java</executable>
              <arguments>
                <argument>-cp</argument>
                <argument>${project.build.directory}/tools/openapi-generator-cli.jar</argument>
                <argument>${mysms.api.dir}/tools/OpenApiBundler.java</argument>
                <argument>${mysms.api.dir}/openapi</argument>
                <argument>${project.build.directory}/openapi</argument>
%PERMISSIONS%              </arguments>
            </configuration>
          </execution>
        </executions>
      </plugin>
"""

def executions(kind):
    out = []
    for m in MODULES:
        if kind == "contracts":
            extra = """                <interfaceOnly>true</interfaceOnly>
                <skipDefaultInterface>true</skipDefaultInterface>
                <useBeanValidation>true</useBeanValidation>
                <performBeanValidation>false</performBeanValidation>
                <useResponseEntity>true</useResponseEntity>"""
            lib = "spring-boot"
        else:
            extra = """                <useBeanValidation>false</useBeanValidation>"""
            lib = "spring-http-interface"
        out.append(f"""          <execution>
            <id>{m}</id>
            <goals>
              <goal>generate</goal>
            </goals>
            <configuration>
              <inputSpec>${{project.build.directory}}/openapi/{m}.json</inputSpec>
              <output>${{project.build.directory}}/generated-sources/openapi/{m}</output>
              <apiPackage>com.mysms.{kind}.{m}.api</apiPackage>
              <modelPackage>com.mysms.{kind}.{m}.model</modelPackage>
              <library>{lib}</library>
              <configOptions>
{extra}
              </configOptions>
            </configuration>
          </execution>""")
    return "\n".join(out)

def common_generator_config(kind):
    return f"""      <!-- 3. one generator run per module contract -->
      <plugin>
        <groupId>org.openapitools</groupId>
        <artifactId>openapi-generator-maven-plugin</artifactId>
        <configuration>
          <generatorName>spring</generatorName>
          <generateApiTests>false</generateApiTests>
          <generateModelTests>false</generateModelTests>
          <generateApiDocumentation>false</generateApiDocumentation>
          <generateModelDocumentation>false</generateModelDocumentation>
          <generateSupportingFiles>false</generateSupportingFiles>
          <addCompileSourceRoot>true</addCompileSourceRoot>
          <skipValidateSpec>false</skipValidateSpec>
          <!-- operationId "master.CreateCountry" becomes method createCountry -->
          <removeOperationIdPrefix>true</removeOperationIdPrefix>
          <additionalProperties>
            <additionalProperty>removeOperationIdPrefixDelimiter=.</additionalProperty>
            <additionalProperty>removeOperationIdPrefixCount=1</additionalProperty>
          </additionalProperties>
          <!-- decimals travel as strings (api/README "Data types"): keep them strings, so the contract's pattern applies -->
          <typeMappings>
            <typeMapping>decimal=String</typeMapping>
          </typeMappings>
          <configOptions>
            <useSpringBoot4>true</useSpringBoot4>
            <useJackson3>true</useJackson3>
            <useTags>true</useTags>
            <openApiNullable>false</openApiNullable>
            <documentationProvider>none</documentationProvider>
            <annotationLibrary>none</annotationLibrary>
            <hideGenerationTimestamp>true</hideGenerationTimestamp>
            <sourceFolder>src/main/java</sourceFolder>
            <dateLibrary>java8</dateLibrary>
            <generateJsonIncludeAnnotations>true</generateJsonIncludeAnnotations>
          </configOptions>
        </configuration>
        <executions>
{executions(kind)}
        </executions>
      </plugin>
"""

HEADER = """<?xml version="1.0" encoding="UTF-8"?>
<!-- GENERATED by tools/backend/gen_backend_poms.py — edit the generator, not this file. -->
"""

contracts = HEADER + """<!--
  contracts: GENERATED, never hand-edited (JAVA-CODING-RULES §2). From /api on every build:
    - per module, the Spring server interfaces + request/response DTOs that controllers implement
      (com.mysms.contracts.<module>.api / .model), by OpenAPI Generator (generator "spring");
    - the gRPC stubs and protobuf messages of every module (com.mysms.<module>.v1), by protoc + protoc-gen-grpc-java;
    - per module, the operation table (method, path, x-permission, security) the auth filter enforces at the edge
      (META-INF/mysms/operations/<module>.json).
-->
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <parent>
    <groupId>com.mysms</groupId>
    <artifactId>mysms</artifactId>
    <version>0.1.0-SNAPSHOT</version>
  </parent>
  <artifactId>contracts</artifactId>
  <name>MySMS contracts (generated)</name>

  <properties>
    <!-- generated code: no style, bug-pattern or coverage gates (§13); nothing hand-written to test -->
    <checkstyle.skip>true</checkstyle.skip>
    <spotbugs.skip>true</spotbugs.skip>
    <jacoco.skip>true</jacoco.skip>
  </properties>

  <dependencies>
    <dependency>
      <groupId>org.springframework</groupId>
      <artifactId>spring-web</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework</groupId>
      <artifactId>spring-context</artifactId>
    </dependency>
    <dependency>
      <groupId>jakarta.validation</groupId>
      <artifactId>jakarta.validation-api</artifactId>
    </dependency>
    <dependency>
      <groupId>jakarta.annotation</groupId>
      <artifactId>jakarta.annotation-api</artifactId>
    </dependency>
    <dependency>
      <groupId>tools.jackson.core</groupId>
      <artifactId>jackson-databind</artifactId>
    </dependency>
    <dependency>
      <groupId>com.google.protobuf</groupId>
      <artifactId>protobuf-java</artifactId>
    </dependency>
    <dependency>
      <groupId>io.grpc</groupId>
      <artifactId>grpc-protobuf</artifactId>
    </dependency>
    <dependency>
      <groupId>io.grpc</groupId>
      <artifactId>grpc-stub</artifactId>
    </dependency>
  </dependencies>

  <build>
    <resources>
      <resource>
        <directory>${project.build.directory}/generated-resources/mysms</directory>
      </resource>
    </resources>
    <plugins>
""" + BUNDLE.replace("%PERMISSIONS%", "                <argument>${project.build.directory}/generated-resources/mysms/META-INF/mysms/operations</argument>\n") + common_generator_config("contracts") + """      <!-- 4. protobuf messages + gRPC stubs of every module -->
      <plugin>
        <groupId>io.github.ascopes</groupId>
        <artifactId>protobuf-maven-plugin</artifactId>
        <configuration>
          <protoc>${protobuf-java.version}</protoc>
          <sourceDirectories>
            <sourceDirectory>${mysms.api.dir}/proto</sourceDirectory>
          </sourceDirectories>
          <plugins>
            <plugin kind="binary-maven">
              <groupId>io.grpc</groupId>
              <artifactId>protoc-gen-grpc-java</artifactId>
              <version>${grpc-java.version}</version>
            </plugin>
          </plugins>
        </configuration>
        <executions>
          <execution>
            <!-- same id as the Boot parent's managed execution, so protoc runs once -->
            <id>generate</id>
            <goals>
              <goal>generate</goal>
            </goals>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
</project>
"""

sdk = HEADER + """<!--
  sdk: GENERATED, never hand-edited (JAVA-CODING-RULES §2). The Java clients modules use to call each other: per module,
  Spring HTTP service interfaces + DTOs (com.mysms.sdk.<module>.api / .model), generated by OpenAPI Generator
  (generator "spring", library "spring-http-interface") from the same bundled contracts as `contracts` and the
  TypeScript SDK. Instances are created by core's SdkClientFactory (trace + credential propagation).
-->
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <parent>
    <groupId>com.mysms</groupId>
    <artifactId>mysms</artifactId>
    <version>0.1.0-SNAPSHOT</version>
  </parent>
  <artifactId>sdk</artifactId>
  <name>MySMS SDK (generated)</name>

  <properties>
    <checkstyle.skip>true</checkstyle.skip>
    <spotbugs.skip>true</spotbugs.skip>
    <jacoco.skip>true</jacoco.skip>
  </properties>

  <dependencies>
    <dependency>
      <groupId>org.springframework</groupId>
      <artifactId>spring-web</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework</groupId>
      <artifactId>spring-context</artifactId>
    </dependency>
    <dependency>
      <groupId>jakarta.validation</groupId>
      <artifactId>jakarta.validation-api</artifactId>
    </dependency>
    <dependency>
      <groupId>jakarta.annotation</groupId>
      <artifactId>jakarta.annotation-api</artifactId>
    </dependency>
    <dependency>
      <groupId>tools.jackson.core</groupId>
      <artifactId>jackson-databind</artifactId>
    </dependency>
  </dependencies>

  <build>
    <plugins>
""" + BUNDLE.replace("%PERMISSIONS%", "") + common_generator_config("sdk") + """    </plugins>
  </build>
</project>
"""
open(ROOT + '/contracts/pom.xml', 'w').write(contracts)
open(ROOT + '/sdk/pom.xml', 'w').write(sdk)
print('ok')
