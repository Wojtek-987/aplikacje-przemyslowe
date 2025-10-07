package com.techcorp;

import com.techcorp.model.Employee;
import com.techcorp.model.Position;
import com.techcorp.service.EmployeeDirectory;

public class Main {
    public static void main(String[] args) {
        EmployeeDirectory directory = new EmployeeDirectory();

        directory.addEmployee(new Employee("Alice Kowalska", "alice@techcorp.com", "TechCorp", Position.PROGRAMISTA, 8500));
        directory.addEmployee(new Employee("Bob Nowak", "bob@techcorp.com", "TechCorp", Position.MANAGER, 12500));
        directory.addEmployee(new Employee("Carla Wisniewska", "carla@techcorp.com", "TechCorp", Position.PREZES, 26000));
        directory.addEmployee(new Employee("David Zielinski", "david@other.com", "OtherCorp", Position.STAZYSTA, 3200));

        System.out.println("=== All employees ===");
        directory.getAllEmployees().forEach(System.out::println);

        System.out.println("\n=== Employees at TechCorp ===");
        directory.findByCompany("TechCorp").forEach(System.out::println);

        System.out.println("\n=== Employees sorted by surname ===");
        directory.getEmployeesSortedBySurname().forEach(System.out::println);

        System.out.println("\n=== Grouped by position ===");
        directory.groupByPosition().forEach((pos, list) ->
                System.out.println(pos + " -> " + list));

        System.out.println("\n=== Count by position ===");
        directory.countByPosition().forEach((pos, count) ->
                System.out.println(pos + " -> " + count));

        System.out.println("\n=== Average salary ===");
        directory.averageSalary().ifPresent(avg ->
                System.out.printf("Average salary: %.2f%n", avg));

        System.out.println("\n=== Highest paid employee ===");
        directory.highestPaid().ifPresent(emp ->
                System.out.println("Highest paid: " + emp));
    }
}
