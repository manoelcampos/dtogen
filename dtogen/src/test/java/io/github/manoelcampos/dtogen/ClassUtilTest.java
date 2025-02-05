package io.github.manoelcampos.dtogen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClassUtilTest {
    @Test
    void getPackageNameString() {
        assertEquals("io.github.manoelcampos.dtogen", ClassUtil.getPackageName("io.github.manoelcampos.dtogen.ClassUtil"));
        assertEquals("", ClassUtil.getPackageName("DTOProcessor"));
    }

    @Test
    void getUpCaseFieldName() {
        assertEquals("Name", ClassUtil.getUpCaseFieldName("name"));
        assertEquals("Name", ClassUtil.getUpCaseFieldName("Name"));
        assertEquals("NAME", ClassUtil.getUpCaseFieldName("nAME"));
        assertEquals("NamE", ClassUtil.getUpCaseFieldName("namE"));
    }

    @Test
    void getSimpleClassName() {
        assertEquals("ClassUtil", ClassUtil.getSimpleClassName("io.github.manoelcampos.dtogen.ClassUtil"));
        assertEquals("DTOProcessor", ClassUtil.getSimpleClassName("DTOProcessor"));
    }
}
