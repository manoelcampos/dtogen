# Java automatic DTO Generation Library [![Build Status](https://github.com/manoelcampos/dtogen/actions/workflows/build.yml/badge.svg)](https://github.com/manoelcampos/dtogen/actions/workflows/build.yml) [![Maven Central](https://img.shields.io/maven-central/v/io.github.manoelcampos/dtogen.svg?label=Maven%20Central)](https://central.sonatype.com/search?q=dtogen&namespace=io.github.manoelcampos) [![javadoc](https://javadoc.io/badge2/io.github.manoelcampos/dtogen/javadoc.svg)](https://javadoc.io/doc/io.github.manoelcampos/dtogen)

DTOGen is a Java 17+ library that automatically generates [Data Transfer Object](https://en.wikipedia.org/wiki/Data_transfer_object) (DTO) [records](https://openjdk.org/jeps/395) from a given model class using annotations. 
It is a straightfoward library that requires no extra configuration to work: just add the dependency and include the `@DTO` annotation on desired model classes to see the magic of generating DTO records happening. 

The library is validation-aware. It means that if you use [Lombok](http://projectlombok.org), [Hibernate Validator](https://hibernate.org/validator/) Annotations or other ones, they will be copied to the DTO fields. This way, validation will be performed on the DTO fields as well, so that you don't need to duplicate validation rules between the model class and the DTO.

The library relies on Annotation Processing to automatically create Java files containing the DTOs.
Since it generates DTOs as Java records, they are [shallowly immutable](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Record.html).

## 1. Include the library in your project

### 1.1 Maven

If you have a Maven project, add the dependency below to your `<dependencies>` tag in your `pom.xml`:

```xml
<dependency>
    <groupId>io.github.manoelcampos</groupId>
    <artifactId>dtogen</artifactId>
    <!-- Set a specific version or use the latest one -->
    <version>DTOGEN_VERSION_HERE</version>
</dependency>
```

### 1.2 Gradle

Add the following dependency into the build.gradle file of your Gradle project.

```groovy
dependencies {
    //Set a specific version or use the latest one
    implementation 'io.github.manoelcampos:dtogen:DTOGEN_VERSION_HERE'
}
```

## 2. How to use

Now, annotate the model classes you want to generate DTOs with `@DTO`.
If you annotated a `Person` class, a `PersonDTO` record will be generated in the same package.

**WARNING**: Don't try to use the generated DTO before performing a successfull build on the project (for instance, using `mvn clean package`). That is when the DTOs are generated. If you try to use them before that, the IDE may show an error saying the DTO doesn't exist.

Finally, use the DTO record where you wish, bu you may need to import the DTOs you are using. Since DTOs are generated in the same package as the annotated classes, you can import them using the `*` wildcard.

If you import the DTO individually, you may get a compiler error saying the DTO doesn't exist.
Usually, you just need to build the project before trying to use any generated DTO,
then run the project (even if the IDE is showing some erros), that it may work after all.

The annotation processing will be automatically performed when you build your project, generating the DTOs for model classes that are annotation with `@DTO`.

The example below shows a `Person` model class which uses the `@DTO` annotation to generate a `PersonDTO` record. This is the only annotation required to create a DTO. By default, the DTO record will have the same fields from the model class. Other annotation such as `@NotNull` and `@NotBlank` are from Hibernate Validation and will be copied to the DTO fields. This way, you don't need to duplicate validation rules between the model class and the DTO.

```java
@DTO
public class Person {
    private long id;

    @NotNull @NotBlank
    private String name;

    @DecimalMin("0.1") @DecimalMax("200")
    private double weightKg;

    @Min(5) @Max(50)
    private int footSize;

    @DTO.Exclude
    private String password;

    private Country country;

    private Profession profession;
}
```

After that, build your project using, for instance, `mvn clean package`.
That will generate the following `PersonDTO` record in the same package as the `Person` class:

```java
public record PersonDTO ( 
    long id, 
    @NotNull @NotBlank String name, 
    @DecimalMin(value="0.1") @DecimalMax(value="200") double weightKg, 
    @Min(value=5) @Max(value=50) int footSize,
    String password,
    Country country,  
    Profession profession) 
{
    
}
```

### 2.1 Additional Options

You can use `@DTO` sub annotations to configure how the DTO record is created. For instance, the `@DTO.Exclude` will exclude the annotated field from the DTO record.

The `@DTO.MapToId` can be used in fields in which the type is anoter model class,
so that instead of including the entire object as an attribute in the DTO record,
only its id will be included. This way, placing this annotation on a `country` field will generate a `countryId` field on the DTO.

```java
@DTO.Exclude
private String password;

@DTO.MapToId
private Country country;
```

That will generate the following `PersonDTO` record:

```java
public record PersonDTO ( 
    long id, 
    @NotNull @NotBlank String name, 
    @DecimalMin(value="0.1") @DecimalMax(value="200") double weightKg, 
    @Min(value=5) @Max(value=50) int footSize,
    @NotNull long countryId,  
    Profession profession) 
{
    
}
```

For more details, check the [sample project](sample).

## 3. Troubleshooting

If you try to use a DTO such as the `PersonDTO` in the example above, and the IDE shows an error saying it doesn't exist, follow the steps below:

1. ensure your code compiles after you use the `@DTO` annotations and before including any DTO record on your code;
1. enable the "Annotation Processing" in the IDE settings (that is required if you are using IntelliJ IDEA: usually a notification is shown when you open the project that has the library included);
2. make sure you have imported the DTO record in the class where you are using it, as described in the end of Section 2 above (if you try to import the DTO record directly, instead of using the `*` wildcard, the IDE may show an error saying the DTO doesn't exist, but follow the steps below that it usually works);
3. build the project using, for instance, `mvn clean package` in the command line;
4. after that, try to run the project (even if it is showing that the DTO records don't exist, usually after that they are generated and the code works).

## Known Issues

If you try to open this entire repository on some IDEs such as IntelliJ,
the sample project won't generate the DTOs.
You must open just the sample project on the IDE for the annotation processing to work.