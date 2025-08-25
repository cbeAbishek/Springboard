package org.example;

import java.sql.*;
import java.util.Scanner;

public class dataset {

    private static final String URL = "jdbc:mysql://localhost:3306/infosys";
    private static final String USER = "root";
    private static final String PASSWORD = "Abi@2005";
    private static final String TABLE_NAME = "users";

    private static Connection connection = null;

    public static void main(String[] args) {
        // Load the MySQL JDBC driver
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL JDBC Driver not found. Please check your pom.xml file.");
            e.printStackTrace();
            return;
        }

        Scanner scanner = new Scanner(System.in);

        // Main application loop
        while (true) {
            System.out.println("\n--- Console CRUD Application ---");
            System.out.println("1. Create Table");
            System.out.println("2. Insert Data");
            System.out.println("3. View All Data");
            System.out.println("4. View Data by Name");
            System.out.println("5. Update Data");
            System.out.println("6. Delete Data");
            System.out.println("7. Exit");
            System.out.print("Enter your choice: ");

            try {
                int choice = scanner.nextInt();
                scanner.nextLine(); // Consume newline

                switch (choice) {
                    case 1:
                        createTable();
                        break;
                    case 2:
                        insertData(scanner);
                        break;
                    case 3:
                        viewAllData();
                        break;
                    case 4:
                        viewDataByName(scanner);
                        break;
                    case 5:
                        updateData(scanner);
                        break;
                    case 6:
                        deleteData(scanner);
                        break;
                    case 7:
                        System.out.println("Exiting application. Goodbye!");
                        closeConnection();
                        return;
                    default:
                        System.out.println("Invalid choice. Please try again.");
                }
            } catch (Exception e) {
                System.err.println("Invalid input. Please enter a number.");
                scanner.nextLine(); // Clear the invalid input from the scanner
            }
        }
    }

    // Establishes a connection to the MySQL database
    private static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            System.out.println("Attempting to connect to the database...");
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Database connection established successfully!");
        }
        return connection;
    }

    // Closes the database connection
    private static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                System.out.println("Database connection closed.");
            } catch (SQLException e) {
                System.err.println("Error closing the database connection: " + e.getMessage());
            }
        }
    }

    // Creates the users table with an auto-incrementing 'sno'
    private static void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " ("
                + "sno INT PRIMARY KEY AUTO_INCREMENT,"
                + "name VARCHAR(255) NOT NULL,"
                + "number VARCHAR(20),"
                + "age INT,"
                + "gender VARCHAR(50)"
                + ")";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("Table '" + TABLE_NAME + "' was created successfully (or already exists).");
        } catch (SQLException e) {
            System.err.println("Error creating table: " + e.getMessage());
        }
    }

    // Inserts new data into the users table
    private static void insertData(Scanner scanner) {
        try (Connection conn = getConnection()) {
            System.out.print("Enter Name: ");
            String name = scanner.nextLine();
            System.out.print("Enter Phone Number: ");
            String number = scanner.nextLine();
            System.out.print("Enter Your Age: ");
            int age = scanner.nextInt();
            scanner.nextLine();
            System.out.print("Enter Gender (M or F): ");
            String gender = scanner.nextLine();

            String sql = "INSERT INTO " + TABLE_NAME + " (name, number, age, gender) VALUES (?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, name);
                pstmt.setString(2, number);
                pstmt.setInt(3, age);
                pstmt.setString(4, gender);
                int affectedRows = pstmt.executeUpdate();
                if (affectedRows > 0) {
                    System.out.println("Data inserted successfully!");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error inserting data: " + e.getMessage());
        }
    }

    // Retrieves and prints all data from the users table
    private static void viewAllData() {
        String sql = "SELECT sno, name, number, age, gender FROM " + TABLE_NAME;
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            System.out.println("\n--- All Data in '" + TABLE_NAME + "' ---");
            while (rs.next()) {
                System.out.printf("S.No: %d, Name: %s, Number: %s, Age: %d, Gender: %s\n",
                        rs.getInt("sno"),
                        rs.getString("name"),
                        rs.getString("number"),
                        rs.getInt("age"),
                        rs.getString("gender"));
            }
        } catch (SQLException e) {
            System.err.println("Error viewing all data: " + e.getMessage());
        }
    }

    // Retrieves and prints data for a specific user by name
    private static void viewDataByName(Scanner scanner) {
        System.out.print("Enter the name to search for: ");
        String name = scanner.nextLine();
        String sql = "SELECT sno, name, number, age, gender FROM " + TABLE_NAME + " WHERE name = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (!rs.isBeforeFirst()) {
                    System.out.println("No data found for the name: " + name);
                    return;
                }
                System.out.println("\n--- Data for '" + name + "' ---");
                while (rs.next()) {
                    System.out.printf("S.No: %d, Name: %s, Number: %s, Age: %d, Gender: %s\n",
                            rs.getInt("sno"),
                            rs.getString("name"),
                            rs.getString("number"),
                            rs.getInt("age"),
                            rs.getString("gender"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error viewing data by name: " + e.getMessage());
        }
    }

    // Updates a record based on its sno (auto-incrementing ID)
    private static void updateData(Scanner scanner) {
        try (Connection conn = getConnection()) {
            System.out.print("Enter the S.No of the record to update: ");
            int sno = scanner.nextInt();
            scanner.nextLine();

            System.out.print("Enter new Name: ");
            String name = scanner.nextLine();
            System.out.print("Enter new Number: ");
            String number = scanner.nextLine();
            System.out.print("Enter new Age: ");
            int age = scanner.nextInt();
            scanner.nextLine();
            System.out.print("Enter new Gender: ");
            String gender = scanner.nextLine();

            String sql = "UPDATE " + TABLE_NAME + " SET name = ?, number = ?, age = ?, gender = ? WHERE sno = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, name);
                pstmt.setString(2, number);
                pstmt.setInt(3, age);
                pstmt.setString(4, gender);
                pstmt.setInt(5, sno);

                int affectedRows = pstmt.executeUpdate();
                if (affectedRows > 0) {
                    System.out.println("Data updated successfully for S.No: " + sno);
                } else {
                    System.out.println("No record found with S.No: " + sno);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error updating data: " + e.getMessage());
        }
    }

    // Deletes a record based on its sno (auto-incrementing ID)
    private static void deleteData(Scanner scanner) {
        try (Connection conn = getConnection()) {
            System.out.print("Enter the S.No of the record to delete: ");
            int sno = scanner.nextInt();
            scanner.nextLine();

            String sql = "DELETE FROM " + TABLE_NAME + " WHERE sno = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, sno);
                int affectedRows = pstmt.executeUpdate();
                if (affectedRows > 0) {
                    System.out.println("Data deleted successfully for S.No: " + sno);
                } else {
                    System.out.println("No record found with S.No: " + sno);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error deleting data: " + e.getMessage());
        }
    }
}
