# Java autopmatic DTO Generation Library

DTOGen is a Java 17+ library that automatically generates DTO [records](https://openjdk.org/jeps/395) from a given model class using `@DTO` annotations.
The library relies on Annotation Processing to automatically create Java files containing the DTOs.
Since it generates DTOs as Java records, they are immutable.

## 1. Include the library in your project

If you have a Maven project, add the dependency below to your `<dependencies>` tag in your `pom.xml`:

```xml
        <dependency>
            <groupId>io.github.manoelcampos</groupId>
            <artifactId>dtogen</artifactId>
            <version>DTOGEN_VERSION_HERE</version>
        </dependency>
```

That is the only configuration required. The annotation processing will be automatically performed when you build your project, generating the DTOs for model classes that are annotation with `@DTO`.

## 2. How to use

The example below shows a `Person` model class which uses the `@DTO` annotation to generate a `DTO` record. This is the only annotation required to create a DTO. It will create a `PersonDTO` record, where all instance fields are included in the DTO.

```java
@DTO 
public class Person {
    private long id;
    private String name;
    private double weightKg;
    private int footSize;
    private String password;
    private Country country;
    private Profession profession;
}
```

Then build your project using, for instance `mvn clean package`.
The DTO record will be automatically generated in the same package of the model class.

### 2.1 Additional Options

You can use `@DTO` sub annotations to configure how the DTO record is created. For instance, the `@DTO.Exclude` will exclude the field from the DTO record.

The `@DTO.MapToId` can also be used in fields whose the type is anoter model class,
so that instead of including the entire object as an attribute in the DTO record,
only its id will be included. This way, including this annotation on a `country` field will generate a `countryId` field on the DTO.

```java
    @DTO.Exclude
    private String password;

    @DTO.MapToId
    private Country country;
```

For more details, check the [sample](samples) projects.

## 3. Details

The good part about the library is that if you use Lombok, Hibernate Validation Annotations or other ones, they will be copied to the DTO fields. This way, validation will be performed on the DTO fields as well.

## 4. Troubleshooting

If DTO records are not being generated, and you are using IntelliJ IDEA, try to:

1. enable the "Annotation Processing" in the IDE settings (usually a notificatio is shown when you open the project that has the library included);
2. build the project using, for instance, `mvn clean package` in the command line;
3. just after that, try to run the project (even if it is showing that the DTO records don't exist).