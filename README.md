# aop-ut-generator

Spring AOP that uses [Java Parser](https://github.com/javaparser/javaparser) library to generate Mockito unit test.

## Usage

- Add the dependency to pom.xml (auto-configuration)

```xml

<dependency>
    <groupId>com.theatomicity</groupId>
    <artifactId>aop.ut.generator</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

- Add targeted packages in configuration

```yml
ut-generator:
  packages:
    - com.theatomicity.scheduler.backend.service
```
