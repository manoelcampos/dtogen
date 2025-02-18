package io.github.manoelcampos.dtogen.samples;

import io.github.manoelcampos.dtogen.DTO;

import java.util.List;

/**
 * A simple class that doesn't have any association with other classes/records.
 * @author Manoel Campos
 */
@DTO
@SuppressWarnings({"rawtypes", "unused"})
public class SampleClass {
    private String str;
    private boolean bool;
    private List<String> genericList;
    private List nonGenericList;

    public String getStr() {
        return str;
    }

    public void setStr(String str) {
        this.str = str;
    }

    public boolean isBool() {
        return bool;
    }

    public void setBool(boolean bool) {
        this.bool = bool;
    }

    public List<String> getGenericList() {
        return genericList;
    }

    public void setGenericList(List<String> genericList) {
        this.genericList = genericList;
    }

    public List getNonGenericList() {
        return nonGenericList;
    }

    public void setNonGenericList(List nonGenericList) {
        this.nonGenericList = nonGenericList;
    }
}
