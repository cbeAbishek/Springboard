package dbconnect;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class sqlconnect {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/";
        String user = "root";
        String password = "Abi@2005";

        try {
            Connection con = DriverManager.getConnection(url, user, password);
            System.out.println("Connected to the MySQL database successfully!");
            con.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
