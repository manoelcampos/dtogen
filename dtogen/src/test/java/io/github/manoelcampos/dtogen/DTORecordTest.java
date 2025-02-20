package io.github.manoelcampos.dtogen;

import io.github.manoelcampos.dtogen.samples.Class1;
import io.github.manoelcampos.dtogen.samples.Class1DTO;
import io.github.manoelcampos.dtogen.samples.Class2;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DTORecordTest {
    private final DTORecord<Class1> dto = new Class1DTO(0, null);

    @Test
    void createNewObject() {
        // If there is an ID, the supplier is called and an object is created
        assertNotNull(dto.newObject(1, Class2::new));
        assertNotNull(dto.newObject(1L, Class2::new));
        assertNotNull(dto.newObject(1.0, Class2::new));
        assertNotNull(dto.newObject("1", Class2::new));
        assertNotNull(dto.newObject(BigDecimal.ONE, Class2::new));
        assertNotNull(dto.newObject(BigInteger.ONE, Class2::new));
        assertNotNull(dto.newObject(UUID.randomUUID(), Class2::new));
    }

    @Test
    void dontCreateNewObject() {
        // If there is no ID, the method must not create a new object
        assertNull(dto.newObject(null, Class2::new));
        assertNull(dto.newObject(0, Class2::new));
        assertNull(dto.newObject(-1, Class2::new));
        assertNull(dto.newObject("", Class2::new));
        assertNull(dto.newObject(BigDecimal.ZERO, Class2::new));
    }

    @Test
    void hasId() {
        assertTrue(dto.hasId(1));
        assertTrue(dto.hasId(1L));
        assertTrue(dto.hasId("1"));
        assertTrue(dto.hasId(1.0));
        assertTrue(dto.hasId(BigDecimal.ONE));
        assertTrue(dto.hasId(BigInteger.ONE));
        assertTrue(dto.hasId(UUID.randomUUID()));
    }

    @Test
    void hasNoId() {
        assertFalse(dto.hasId(null));
        assertFalse(dto.hasId(0));
        assertFalse(dto.hasId(-1));
        assertFalse(dto.hasId(""));
        assertFalse(dto.hasId(BigDecimal.ZERO));
        assertFalse(dto.hasId(BigInteger.ZERO));
    }

}
