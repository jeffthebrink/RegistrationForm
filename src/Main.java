import jodd.json.JsonParser;
import jodd.json.JsonSerializer;
import org.h2.tools.Server;
import spark.Spark;
import java.sql.*;
import java.util.ArrayList;

public class Main {

    public static void createTable(Connection conn) throws SQLException {
        Statement stmt = conn.createStatement();
        stmt.execute("CREATE TABLE IF NOT EXISTS users (id IDENTITY, username VARCHAR, address VARCHAR, email VARCHAR )");
    }

    public static void insertUser(Connection conn, String username, String address, String email) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO users VALUES (NULL, ?, ?, ? );");
        stmt.setString(1, username);
        stmt.setString(2, address);
        stmt.setString(3, email);
        stmt.execute();
    }

    public static ArrayList<User> selectUsers(Connection conn) throws SQLException {
        ArrayList<User> users = new ArrayList<>();
        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users");
        ResultSet results = stmt.executeQuery();
        while (results.next()) {
            Integer id = results.getInt("id");
            String username = results.getString("username");
            String address = results.getString("address");
            String email = results.getString("email");
            users.add(new User(id, username, address, email));
        }
        return users;
    }

    public static void updateUser(
            Connection conn, Integer id, String username, String address, String email) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(
                "UPDATE users SET username = ?, address = ?, email = ? WHERE ID = ?"
        );
        stmt.setString(1, username);
        stmt.setString(2, address);
        stmt.setString(3, email);
        stmt.setInt(4, id);
        stmt.execute();
    }

    public static void deleteUser(Connection conn, Integer id) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("DELETE FROM users WHERE ID  = ?");
        stmt.setInt(1, id);
        stmt.execute();
    }


    public static void main(String[] args) throws SQLException {
        Server.createWebServer().start();
        Connection conn = DriverManager.getConnection("jdbc:h2:./main");
        createTable(conn);

        Spark.externalStaticFileLocation("public");
        Spark.init();

        Spark.get("/user", (request, response) -> {
            ArrayList<User> users =  selectUsers(conn);
            JsonSerializer s = new JsonSerializer();
            return s.serialize(users);
        });

        Spark.post("/user", (request, response) -> {
            String json = request.body();
            JsonParser p = new JsonParser();
            User user = p.parse(json, User.class);
            insertUser(conn, user.getUsername(), user.getAddress(), user.getEmail());
            return "";
        });

        Spark.put("/user", (request, response) -> {
            String json = request.body();
            JsonParser p = new JsonParser();
            User user = p.parse(json, User.class); // parser requires the ".class"
            updateUser(conn, user.getId(),user.getUsername(), user.getAddress(), user.getEmail());
            return "";
        });

        Spark.delete("/user/:id", (request, response) -> {
            Integer id = Integer.parseInt(request.params(":id"));
            deleteUser(conn, id);
            return "";
        });
    }
}
