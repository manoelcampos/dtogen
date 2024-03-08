package io.github.manoelcampos.dtogen.sample;

/*
 * DTOs are generated in the same package of the @DTO annotated classes (model classes).
 * Here we are using the * wildcard to import all classes in the model package
 * (which are annotated with @DTO), including the generated DTOs.
 * That makes it easier to use the DTOs, since is less likely to get a compiler error
 * saying that a given DTO you tried to use was not found.
 * Check the README.md at the project's root dir for more details.
*/

import io.github.manoelcampos.dtogen.sample.model.*;

/**
 * @author Manoel Campos
 */
public class Main {
    public static void main(String[] args) {
        final var country = new Country(1, "Brazil", "South America", 8_510_417_771L);
        final var profession = new Profession(1, "Software Engineer", "IT");
        final var person = new Person(1, "Manoel Campos", 80, 42, "Brazil", country, profession);
        System.out.println(person);
        System.out.println();

        /* To make the DTO generation work on your IDE, you must open the sample project directly,
         * instead o openning the entire repository. */
        final PersonDTO dto = new PersonDTO( 1, "Manoel Campos", 80, 42, country.getId(), profession);
        System.out.println(dto);
    }
}
