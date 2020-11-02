package de.uni.sttg.ipvs.as.composite.data.store.adapter.destination;

import de.uni.sttg.ipvs.as.composite.data.store.core.adapter.AbstractDataDestinationAdapter;
import de.uni.sttg.ipvs.as.composite.data.store.core.model.DataDestination;
import de.uni.sttg.ipvs.as.composite.data.store.core.model.DataLocation;
import de.uni.sttg.ipvs.as.composite.data.store.core.model.ExecutionDescription;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author Roman Bitz
 */
@Slf4j
@Component("MYSQLDB")
public class MySQLAdapter extends AbstractDataDestinationAdapter {

  Connection conn;
  String tablenametemp;
  String tablenamegps;
  String databasename;

  @Override
  public void initializeConnection(DataDestination dataDestination) throws Exception {

    DataLocation dataLocation = dataDestination.getLocation();
    String host = dataLocation.getHost();
    String port = dataLocation.getPort();
    String user = dataLocation.getUser();
    String pass = dataLocation.getPass();
    databasename = dataLocation.getEntrypoint();
    tablenametemp = dataLocation.getPath();
    tablenamegps = dataLocation.getAttr().get("tablegps");

    String connectString = "jdbc:mysql://" + host + ":" + port + "/" + databasename + "?" +
        "user=" + user + "&password=" + pass + "&autoReconnect=true";

    conn = DriverManager.getConnection(connectString);

    String tablequerytemp = "CREATE TABLE IF NOT EXISTS `" + tablenametemp + "` (\n"
        + "`id` varchar(45) NOT NULL,\n"
        + "`device` varchar(20) DEFAULT NULL,\n"
        + "`temp` varchar(20) DEFAULT NULL,\n"
        + "`type` varchar(20) DEFAULT NULL,\n"
        + "PRIMARY KEY (`id`)\n"
        + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;";

    String tablequerygps = "CREATE TABLE IF NOT EXISTS `" + tablenamegps + "` (\n"
        + "`id` varchar(45) NOT NULL,\n"
        + "`owner` varchar(20) DEFAULT NULL,\n"
        + "`gender` varchar(20) DEFAULT NULL,\n"
        + "`phonetype` varchar(20) DEFAULT NULL,\n"
        + "`gpslat` varchar(20) DEFAULT NULL,\n"
        + "`gpslng` varchar(20) DEFAULT NULL,\n"
        + "PRIMARY KEY (`id`)\n"
        + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;";

    try {
      Statement stmt = conn.createStatement();

      log.info("Creating MySQL Table temperatur and gps");

      stmt.execute(tablequerytemp);
      stmt.execute(tablequerygps);

    } catch (SQLException e) {
      e.printStackTrace();
    }

  }

  @Override
  public Map<String, Function<ExecutionDescription, Boolean>> initializeAndReturnFunctionalityMapping(DataDestination dataDestination) {

    Map<String, Function<ExecutionDescription, Boolean>> functionalityMapping = new HashMap<>();

    functionalityMapping.put("INSERT_GPS_DATA", this::executeInsertDataGPS);
    functionalityMapping.put("DELETE_GPS_DATA", this::deleteDataGPS);
    functionalityMapping.put("INSERT_TEMP_DATA", this::executeInsertDataTEMP);
    functionalityMapping.put("DELETE_TEMP_DATA", this::deleteDataTEMP);

    return functionalityMapping;
  }

  private boolean executeInsertDataTEMP(ExecutionDescription executionDescription) {

    HashMap<String, String> json = (HashMap<String, String>) executionDescription.getPayload();

    String id = json.get("id");
    String device = json.get("device");
    String temp = json.get("temp");
    String type = json.get("type");

    Statement stmt;
    try {
      stmt = conn.createStatement();

      log.info("Inserting MySQL Data");

      String query = "INSERT INTO " + tablenametemp + " VALUES ('" + id + "','" + device + "','" + temp + "','" + type + "');";

      stmt.executeUpdate(query,
          Statement.RETURN_GENERATED_KEYS);

    } catch (SQLException e) {
      e.printStackTrace();
      return false;
    }

    return true;
  }

  private Boolean deleteDataGPS(ExecutionDescription executionDescription) {

    Statement stmt;
    try {
      stmt = conn.createStatement();

      log.info("Deleting MySQL Data");

      String deleteQuery = "DELETE FROM `" + databasename + "`.`" + tablenamegps + "`";

      stmt.execute(deleteQuery);

    } catch (SQLException e) {
      e.printStackTrace();
      return false;
    }

    return true;

  }

  private boolean executeInsertDataGPS(ExecutionDescription executionDescription) {

    HashMap<String, String> json = (HashMap<String, String>) executionDescription.getPayload();

    String id = json.get("id");
    String owner = json.get("owner");
    String gender = json.get("gender");
    String phonetype = json.get("phonetype");
    String gpslat = json.get("gpslat");
    String gpslng = json.get("gpslng");

    Statement stmt;
    try {
      stmt = conn.createStatement();

      log.info("Inserting MySQL Data");

      String query =
          "INSERT INTO " + tablenamegps + " VALUES ('" + id + "','" + owner + "','" + gender + "','" + phonetype + "','" + gpslat + "','" + gpslng
              + "');";

      stmt.executeUpdate(query,
          Statement.RETURN_GENERATED_KEYS);

    } catch (SQLException e) {
      e.printStackTrace();
      return false;
    }

    return true;
  }

  private Boolean deleteDataTEMP(ExecutionDescription executionDescription) {

    Statement stmt;
    try {
      stmt = conn.createStatement();

      log.info("Deleting MySQL Data");

      String deleteQuery = "DELETE FROM `" + databasename + "`.`" + tablenametemp + "`";

      stmt.execute(deleteQuery);

    } catch (SQLException e) {
      e.printStackTrace();
      return false;
    }

    return true;

  }


}
