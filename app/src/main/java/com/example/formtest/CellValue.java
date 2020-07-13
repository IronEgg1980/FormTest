package com.example.formtest;

public class CellValue implements FormViewByTextView.ICellValue {
    private String value;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public CellValue(String s){
        this.value = s;
    }

    @Override
    public String getText() {
        return this.value;
    }
}
