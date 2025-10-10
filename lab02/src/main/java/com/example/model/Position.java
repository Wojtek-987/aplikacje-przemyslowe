package com.example.model;

public enum Position {
    PREZES(25_000.0, 1),
    WICEPREZES(18_000.0, 2),
    MANAGER(12_000.0, 3),
    PROGRAMISTA(8_000.0, 4),
    STAZYSTA(3_000.0, 5);

    private final double baseSalary;
    private final int hierarchyLevel;

    Position(double baseSalary, int hierarchyLevel) {
        this.baseSalary = baseSalary;
        this.hierarchyLevel = hierarchyLevel;
    }

    public double getBaseSalary() {
        return baseSalary;
    }

    public int getHierarchyLevel() {
        return hierarchyLevel;
    }
}
