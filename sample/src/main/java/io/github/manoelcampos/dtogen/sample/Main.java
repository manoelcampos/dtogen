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
 * Sample class to demonstrate the utilization of the generated DTOs.
 * @author Manoel Campos
 */
public class Main {
    public static void main(String[] args) {
        final var language = new LanguageClass(1, "Brazilian Portuguese", "PT-BR");
        final var country = new CountryClass(1, "Brazil", "South America", 8_510_417_771L, language);
        final var profession = new ProfessionRecord(1, "Software Engineer", "IT");
        final var religion = new ReligionClass("Christianity", "Middle East");
        final var mother = new PersonRecord(1, "Mother", 60, 35, "Brazil", country, profession, religion);
        final var father = new PersonRecord(2, "Father", 75, 40, "Brazil", country, profession, religion);
        final var person = new PersonRecord(3, "Manoel Campos", 80, 42, "Brazil", country, profession, religion, mother, father);
        System.out.println(person);
        System.out.println();

        /* To make the DTO generation work on your IDE, you must open the sample project directly,
         * instead o opening the entire repository. */
        final PersonRecordDTO dto = new PersonRecordDTO(0,  "Manoel Campos", 80, 42, country.getId(), profession.id(), religion, mother, father.id());
        System.out.println(dto);
    }
}
