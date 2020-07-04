package com.example.formtest;

import androidx.annotation.NonNull;

public class Duty {
    private String name;
    private boolean isOffDay;
    private double offDayValue;
    private boolean isAdd;
    private double perAddValue;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isOffDay() {
        return isOffDay;
    }

    public void setOffDay(boolean offDay) {
        isOffDay = offDay;
    }

    public double getOffDayValue() {
        return offDayValue;
    }

    public void setOffDayValue(double offDayValue) {
        this.offDayValue = offDayValue;
    }

    public boolean isAdd() {
        return isAdd;
    }

    public void setAdd(boolean add) {
        isAdd = add;
    }

    public double getPerAddValue() {
        return perAddValue;
    }

    public void setPerAddValue(double perAddValue) {
        this.perAddValue = perAddValue;
    }

    @NonNull
    @Override
    public String toString() {
        return name;
    }
}
