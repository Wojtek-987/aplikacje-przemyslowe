package com.example.dto;

/**
 * DTO representing aggregated statistics for a company.
 */
public class CompanyStatisticsDTO {

    private String companyName;
    private long employeeCount;
    private double averageSalary;
    private double highestSalary;
    private String topEarnerName;

    public CompanyStatisticsDTO() {
    }

    public CompanyStatisticsDTO(String companyName, long employeeCount,
                                double averageSalary, double highestSalary, String topEarnerName) {
        this.companyName = companyName;
        this.employeeCount = employeeCount;
        this.averageSalary = averageSalary;
        this.highestSalary = highestSalary;
        this.topEarnerName = topEarnerName;
    }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public long getEmployeeCount() { return employeeCount; }
    public void setEmployeeCount(long employeeCount) { this.employeeCount = employeeCount; }

    public double getAverageSalary() { return averageSalary; }
    public void setAverageSalary(double averageSalary) { this.averageSalary = averageSalary; }

    public double getHighestSalary() { return highestSalary; }
    public void setHighestSalary(double highestSalary) { this.highestSalary = highestSalary; }

    public String getTopEarnerName() { return topEarnerName; }
    public void setTopEarnerName(String topEarnerName) { this.topEarnerName = topEarnerName; }

    @Override
    public String toString() {
        return "CompanyStatisticsDTO{" +
                "companyName='" + companyName + '\'' +
                ", employeeCount=" + employeeCount +
                ", averageSalary=" + averageSalary +
                ", highestSalary=" + highestSalary +
                ", topEarnerName='" + topEarnerName + '\'' +
                '}';
    }
}
