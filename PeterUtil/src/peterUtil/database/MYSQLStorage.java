package peterUtil.database;

import peterUtil.database.queryBuilder.QueryBuilderFactory;
import peterUtil.database.queryBuilder.QueryBuilderInterface;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.sql.*;
import java.util.HashMap;
import java.util.UUID;

public class MYSQLStorage implements StorageInterface{
    private JavaPlugin plugin;
    private String tableName;
    private Connection connection;
    private Statement statement;

    public MYSQLStorage(JavaPlugin plugin, String tableName, String databaseName, String userName, String password, String createTableQuery){
        this.plugin = plugin;
        this.tableName = tableName;

        try{
            Class.forName("com.mysql.jdbc.Driver");
            connection = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/" + databaseName + "?useSSL=false", userName, password);

            statement = connection.createStatement();
            statement.executeUpdate(createTableQuery);
        } catch(Exception e){
            System.out.println(e.getMessage());
        }
    }

    public Connection getConnection(){
        return this.connection;
    }

    @Override
    public void store(UUID uniqueId, ConfigStructure configStructure) throws IOException {
        HashMap<String, Object> data = configStructure.getData();
        data.put("id", uniqueId.toString());

        String[] keys = data.keySet().toArray(new String[0]);
        Object[] values = new Object[keys.length];
        for(int i=0; i<keys.length; i++){
            values[i] = data.get(keys[i]);
        }

        QueryBuilderInterface insertQueryBuilder = QueryBuilderFactory.getInsertQueryBuilder();
        insertQueryBuilder = insertQueryBuilder.from(tableName)
                .column(keys)
                .where(new String[] {"id"});
        String query = insertQueryBuilder.getQuery();
        Bukkit.getConsoleSender().sendMessage(query);
        try {
            PreparedStatement statement = connection.prepareStatement(query);
            for(int i=0; i<keys.length; i++){
                statement.setObject(i+1, values[i]);
            }
            statement.execute();
        } catch (SQLException e) {
            QueryBuilderInterface updateQueryBuilder = QueryBuilderFactory.getUpdateQueryBuilder();
            updateQueryBuilder = updateQueryBuilder.from(tableName).set(keys);
            query = updateQueryBuilder.getQuery();
            Bukkit.getConsoleSender().sendMessage(query);
            PreparedStatement statement;
            try {
                statement = connection.prepareStatement(query);
                for(int i=0; i<keys.length; i++){
                    statement.setObject(i+1, values[i]);
                }
                statement.executeUpdate();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    @Override
    public HashMap<String, Object> get(UUID uniqueId, String[] keys) {
        QueryBuilderInterface selectQueryBuilder = QueryBuilderFactory.getSelectQueryBuilder();
        selectQueryBuilder = selectQueryBuilder.from(tableName).where(new String[] {"id"});

        String query = selectQueryBuilder.getQuery();
        Bukkit.getConsoleSender().sendMessage(query);
        try {
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setObject(1, uniqueId.toString());

            ResultSet resultSet = statement.executeQuery();
            if(resultSet.next()){
                HashMap<String, Object> result = new HashMap<>();
                for(int i=0; i<keys.length; i++){
                    result.put(keys[i], resultSet.getObject(i+2));
                }
                return result;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void store(ConfigStructure configStructure) throws IOException { }
}
