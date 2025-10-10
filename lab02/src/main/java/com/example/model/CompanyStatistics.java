package com.example.model;

public class CompanyStatistics {
    private final long employeeCount;
    private final double averageSalary;
    private final String topEarnerFullName;

    public CompanyStatistics(long employeeCount, double averageSalary, String topEarnerFullName) {
        this.employeeCount = employeeCount;
        this.averageSalary = averageSalary;
        this.topEarnerFullName = topEarnerFullName;
    }

    public long getEmployeeCount() { return employeeCount; }
    public double getAverageSalary() { return averageSalary; }
    public String getTopEarnerFullName() { return topEarnerFullName; }

    @Override
    public String toString() {
        return "CompanyStatistics{" +
                "employeeCount=" + employeeCount +
                ", averageSalary=" + String.format("%.2f", averageSalary) +
                ", topEarnerFullName='" + topEarnerFullName + '\'' +
                '}';
    }
}
