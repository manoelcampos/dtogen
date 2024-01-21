# Java autopmatic DTO Generation Library

DTOGen is a Java 17+ library that automatically generates DTO [records](https://openjdk.org/jeps/395) from a given model class using `@DTO` annotations.
The library relies on Annotation Processing to automatically create Java files containing the DTOs.
Since it generates DTOs as Java records, they are immutable.

## How to use

Below is a `Person` model class that uses the `@DTO` annotation to generate a `DTO` record.
This is the only annotation required to create a DTO.

```java
@DTO 
public class Person implements Serializable {
    private static final long serialVersionUID = 1L;

    private long id;
    private String name;
    private double weightKg;
    private int footSize;
    private String password;
    private Country country;
    private Profession profession;
}
```

That will create a `PersonDTO` record, where all instance fields are included in the DTO.
However, you can use `@DTO` sub annotations to configure how the DTO record is created.
For instance, the `@DTO.Ignore` will exclude the field from the DTO record.

The `@DTO.MapIdOnly` can also be used in fields whose the type is anoter model class,
so that instead of including the entire object as an attribute in the DTO record,
only its id will be included. This way, including this annotation on a `country` field will generate a `countryId` field on the DTO.

```java
    @DTO.Ignore
    private String password;

    @DTO.MapIdOnly
    private Country country;
```

For more details, check the [sample](samples) projects.
