package com.example.rmesaage;
import com.example.rmesaage.Chat.Message;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.ArrayList;
public class databaseUtils {
    public boolean auth(String username,String password){
        Connection conn = null;
        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection("jdbc:sqlite:databases/users.db");
            String sql = "SELECT * FROM users" +
                    " WHERE username="+"\""+username+"\""+" AND" +
                    " password="+"\""+password+"\"";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            conn.close();
            return rs.next();
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
        }
        return false;
    }
    public boolean search(String username){
        Connection conn = null;
        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection("jdbc:sqlite:databases/users.db");
            String sql = "SELECT * FROM users" +
                    " WHERE username="+"\""+username+"\"";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            conn.close();
            return rs.next();
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
        }
        return false;
    }
    public boolean register(String username,String password, String type){
        Connection conn = null;
        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection("jdbc:sqlite:databases/users.db");

            // Проверяем, существует ли значение в таблице "users"
            String sql = "SELECT * FROM users WHERE username = ? AND type = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);
            pstmt.setString(2,type);
            ResultSet rs = pstmt.executeQuery();

            if (!rs.next()) {
                // Если значение не существует, вставляем его
                sql = "INSERT INTO users (username, password,type) VALUES (?, ?, ?)";
                pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, username);
                pstmt.setString(2, password);
                pstmt.setString(3,type);
                pstmt.executeUpdate();
                conn.close();
                return true;
            } else {
                conn.close();
                return false;
            }

        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }
        return false;
    }
    private boolean tryToFindDialog(String Name) {
        Connection conn = null;
        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection("jdbc:sqlite:databases/message.db");
            System.out.println(Math.abs(Name.hashCode())+" "+ Name);
            String createTableSQL = "SELECT * FROM d" + Math.abs(Name.hashCode()) + ";";
            Statement stmt = conn.createStatement();
            stmt.execute(createTableSQL);
            conn.close();
            return true;
        }catch (Exception e){
            try {
                String[] name = Name.split("_");
                Name = name[1]+"_"+name[0];
                Class.forName("org.sqlite.JDBC");
                conn = DriverManager.getConnection("jdbc:sqlite:databases/message.db");
                String createTableSQL = "SELECT * FROM d" + Math.abs(Name.hashCode()) + ";";
                Statement stmt = conn.createStatement();
                stmt.execute(createTableSQL);
                conn.close();
                return false;
            }catch (Exception ignored){
            }
        }
        return false;
    }
    public ArrayList<Message> getChats(String username){
        Connection conn;
        ArrayList<Message> res = new ArrayList<>();
        try {
            ArrayList<String> tables = new ArrayList<>();
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection("jdbc:sqlite:databases/message.db");
            String sql = "SELECT * FROM sqlite_master;";
            Statement stmt = conn.createStatement();
            ResultSet rs  = stmt.executeQuery(sql);
            while (rs.next())tables.add(rs.getString("name"));
            for (int i = 0; i < tables.size(); i++) {
                sql = "SELECT * FROM "+ tables.get(i)+";";
                rs = stmt.executeQuery(sql);
                String sendTo = "", author = "",text = "";
                while (rs.next()){
                    author = rs.getString("sender");
                    sendTo = rs.getString("sendTo");
                    text = rs.getString("text");
                    if (author.equals(username)||sendTo.equals(username)) res.add(new Message(0,author,text,null,sendTo,null));
                }
            }
            conn.close();
            return res;
        }catch (Exception e){

        }
        return res;
    }
    public boolean sendMessage(String username,String sendTo, String text){
        Connection conn = null;
        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection("jdbc:sqlite:src/main/resources/message.db");
            // Создаем новую таблицу, если ее еще нет

            String tableName = username + "_" + sendTo;
            String createTableSQL = "";
            if (!tryToFindDialog(tableName)) {
                String[] name = tableName.split("_");
                tableName = name[1] + "_" + name[0];
            }
            createTableSQL = "CREATE TABLE IF NOT EXISTS d" + Math.abs(tableName.hashCode()) +" ("+"\n\tid INTEGER PRIMARY KEY,\n\tsender TEXT,\n\tsendTo TEXT,\n\ttext TEXT\n);";
            Statement stmt = conn.createStatement();
            stmt.execute(createTableSQL);
            // Вставляем новую запись в таблицу
            String insertSQL = "INSERT INTO d" + Math.abs(tableName.hashCode()) + "(sender, sendTo, text) VALUES ('"+username+"', '"+sendTo+"', '"+text+"')";
            stmt.executeUpdate(insertSQL);
            conn.close();
            return true;
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
        }
        return false;
    }
    public ArrayList<Message> getMessages(String username,String sendTo){
        ArrayList<Message> res = new ArrayList<>();
        sendTo = new String(sendTo.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
        System.out.println(sendTo);
        Connection conn = null;
        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection("jdbc:sqlite:src/main/resources/message.db");
            // Выполняем выборку из таблицы "users"
            String tableName = username+"_"+sendTo;
            if (!tryToFindDialog(tableName)) {
                String[] name = tableName.split("_");
                tableName = name[1] + "_" + name[0];
            }
            String sql = "SELECT * FROM d" + Math.abs(tableName.hashCode()) + ";";
            System.out.println(sql);
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            // Выводим результаты выборки в консоль
            while (rs.next()){
                res.add(new Message(rs.getString("sender"),rs.getString("text")));
            }
            conn.close();
            return res;
        } catch (Exception e) {
            return res;
        }
    }


    public ArrayList<User> getUsersDatabase(){
        Connection conn = null;
        ArrayList<User> res = new ArrayList<>();
        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection("jdbc:sqlite:databases/users.db");

            // Проверяем, существует ли значение в таблице "users"
            String sql = "SELECT * FROM users";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()){
                res.add(new User(rs.getString("username"),rs.getString("password"),rs.getString("type")));
            }
            return res;
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }
        return res;
    }
    public boolean registerServer(String serverName, String serverKey, String ip){
        Connection conn = null;
        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection("jdbc:sqlite:databases/servers.db");

            // Проверяем, существует ли значение в таблице "users"
            String sql = "SELECT * FROM servers WHERE ServerName = ? AND ServerKey = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, serverName);
            pstmt.setString(2, serverKey);
            ResultSet rs = pstmt.executeQuery();

            if (!rs.next()) {
                // Если значение не существует, вставляем его
                sql = "INSERT INTO servers (ServerName, ServerKey,ip) VALUES (?, ?, ?)";
                pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, serverName);
                pstmt.setString(2, serverKey);
                pstmt.setString(3,ip);
                pstmt.executeUpdate();
                conn.close();
                return true;
            } else {
                conn.close();
                return false;
            }

        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }
        return false;
    }
    public ArrayList<Server> getServerDatabase(){
        Connection conn = null;
        ArrayList<Server> res = new ArrayList<>();
        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection("jdbc:sqlite:databases/servers.db");

            // Проверяем, существует ли значение в таблице "users"
            String sql = "SELECT * FROM servers";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()){
                res.add(new Server(rs.getString("ServerName")
                        ,rs.getString("ServerKey")
                        ,rs.getString("ip")));
            }
            return res;
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }
        return res;
    }
    public ArrayList<ArrayList<Message>> getSavedMessageDatabase(){
        Connection conn;
        ArrayList<ArrayList<Message>> res = new ArrayList<>();
        try {
            ArrayList<String> tables = new ArrayList<>();
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection("jdbc:sqlite:databases/message.db");
            String sql = "SELECT * FROM sqlite_master;";
            Statement stmt = conn.createStatement();
            ResultSet rs  = stmt.executeQuery(sql);
            while (rs.next())tables.add(rs.getString("name"));
            for (int i = 0; i < tables.size(); i++) {
                sql = "SELECT * FROM "+ tables.get(i)+";";
                rs = stmt.executeQuery(sql);
                String sendTo = "", author = "",text = "";
                ArrayList<Message> chat = new ArrayList<>();
                while (rs.next()){
                    author = rs.getString("sender");
                    sendTo = rs.getString("sendTo");
                    text = rs.getString("text");
                    chat.add(new Message(0,author,text,null,sendTo,null));
                }
                res.add(chat);
            }
            conn.close();
            return res;
        }catch (Exception e){

        }
        return res;
    }
    public boolean syncDatabases(ArrayList database){
        if (database.get(0) instanceof User){
            if (database.get(0)!=null){
                for (int i = 0; i < database.size(); i++) {
                    User user = (User) database.get(i);
                    Connection conn = null;
                    try {
                        Class.forName("org.sqlite.JDBC");
                        conn = DriverManager.getConnection("jdbc:sqlite:databases/users.db");

                        // Проверяем, существует ли значение в таблице "users"
                        String sql = "SELECT * FROM users WHERE username = ? AND type = ?";
                        PreparedStatement pstmt = conn.prepareStatement(sql);
                        pstmt.setString(1, user.getUsername());
                        pstmt.setString(2, user.getType());
                        ResultSet rs = pstmt.executeQuery();

                        if (!rs.next()) {
                            // Если значение не существует, вставляем его
                            sql = "INSERT INTO users (username, password,type) VALUES (?, ?, ?)";
                            pstmt = conn.prepareStatement(sql);
                            pstmt.setString(1, user.getUsername());
                            pstmt.setString(2, user.getPassword());
                            pstmt.setString(3, user.getType());
                            pstmt.executeUpdate();
                        }
                        conn.close();
                    } catch (Exception e) {
                        System.err.println(e.getClass().getName() + ": " + e.getMessage());
                    }
                }
                return true;
            }
        }else if (database.get(0) instanceof Server){
            if (database.get(0)!=null){
                for (int i = 0; i < database.size(); i++) {
                    Connection conn = null;
                    Server server = (Server) database.get(i);
                    try {
                        Class.forName("org.sqlite.JDBC");
                        conn = DriverManager.getConnection("jdbc:sqlite:databases/servers.db");

                        // Проверяем, существует ли значение в таблице "users"
                        String sql = "SELECT * FROM servers WHERE ServerName = ? AND ServerKey = ? AND ip = ?";
                        PreparedStatement pstmt = conn.prepareStatement(sql);
                        pstmt.setString(1, server.ServerName);
                        pstmt.setString(2, server.ServerKey);
                        pstmt.setString(3, server.ip);
                        ResultSet rs = pstmt.executeQuery();

                        if (!rs.next()) {
                            // Если значение не существует, вставляем его
                            sql = "INSERT INTO servers (ServerName, ServerKey, ip) VALUES (?, ?, ?)";
                            pstmt = conn.prepareStatement(sql);
                            pstmt.setString(1, server.getServerName());
                            pstmt.setString(2, server.getServerKey());
                            pstmt.setString(3, server.getIp());
                            pstmt.executeUpdate();
                        }
                        conn.close();
                    } catch (Exception e) {
                        System.err.println(e.getClass().getName() + ": " + e.getMessage());
                    }
                }
                return true;
            }
        }else if (database.get(0) instanceof ArrayList){
            if (((ArrayList) database.get(0)).get(0) instanceof Message){
                if (database.get(0)!=null){
                    for (int i = 0; i < database.size(); i++) {
                        for (int j = 0; j < ((ArrayList)database.get(i)).size(); j++) {
                            Message message = (Message) ((ArrayList)database.get(i)).get(j);
                            Connection conn;
                            try {
                                ArrayList<String> tables = new ArrayList<>();
                                Class.forName("org.sqlite.JDBC");
                                conn = DriverManager.getConnection("jdbc:sqlite:databases/message.db");
                                String sql = "SELECT * FROM sqlite_master;";
                                Statement stmt = conn.createStatement();
                                ResultSet rs  = stmt.executeQuery(sql);
                                while (rs.next())tables.add(rs.getString("name"));
                                for (int k = 0; i < tables.size(); k++) {
                                    sql = "SELECT * FROM "+ tables.get(k)+" WHERE sender = ? AND sendTo = ? AND text = ?;";
                                    PreparedStatement Check = conn.prepareStatement(sql);
                                    Check.setString(1, message.getMessageUser());
                                    Check.setString(2, message.getSendTo());
                                    Check.setString(3, message.getText());
                                    ResultSet resSet = Check.executeQuery();
                                    if (!resSet.next()) {
                                        // Если значение не существует, вставляем его
                                        sql = "INSERT INTO "+tables.get(i)+" (sender, sendTo, text) VALUES (?, ?, ?)";
                                        Check = conn.prepareStatement(sql);
                                        Check.setString(1, message.getMessageUser());
                                        Check.setString(2, message.getSendTo());
                                        Check.setString(3, message.getText());
                                        Check.executeUpdate();
                                    }
                                }
                                conn.close();
                            }catch (Exception e){

                            }
                        }
                    }
                    return true;
                }
            }
        }
        return false;
    }
    static class User{
        String username,password,type;

        public User(String username, String password, String type) {
            this.username = username;
            this.password = password;
            this.type = type;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }
    static class Server{
        private String ServerName, ServerKey, ip;

        public Server(String serverName, String serverKey, String ip) {
            ServerName = serverName;
            ServerKey = serverKey;
            this.ip = ip;
        }

        public String getServerName() {
            return ServerName;
        }

        public void setServerName(String serverName) {
            ServerName = serverName;
        }

        public String getServerKey() {
            return ServerKey;
        }

        public void setServerKey(String serverKey) {
            ServerKey = serverKey;
        }

        public String getIp() {
            return ip;
        }

        public void setIp(String ip) {
            this.ip = ip;
        }
    }
}