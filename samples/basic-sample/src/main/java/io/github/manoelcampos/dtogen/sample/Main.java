package io.github.manoelcampos.dtogen.sample;

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

        final PersonDTO dto = new PersonDTO( 1, "Manoel Campos", 80, 42, country.getId(), profession);
        System.out.println(dto);
    }
}
