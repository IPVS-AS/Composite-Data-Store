package de.uni.sttg.ipvs.as.composite.data.store.adapter.source;

import de.uni.sttg.ipvs.as.composite.data.store.core.adapter.AbstractDataSourceAdapter;
import de.uni.sttg.ipvs.as.composite.data.store.core.model.DataLocation;
import de.uni.sttg.ipvs.as.composite.data.store.core.model.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author Roman Bitz
 */
@Slf4j
@Component("SOURCE_MYSQLDB")
public class Source_MySQLAdapter extends AbstractDataSourceAdapter {

  DataSource metaDataSource;
  Connection conn;
  String tablenametemp;
  String tablenamegps;
  String databasename;

  @Override
  public void initializeConnection(DataSource dataSource) throws Exception {

    metaDataSource = dataSource;
    DataLocation dataLocation = dataSource.getLocation();
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

  }

  private void handle() {

    while (true) {

      try {
        executeQueries();
        TimeUnit.SECONDS.sleep(1);

      } catch (InterruptedException e) {
        e.printStackTrace();
      }

    }

  }

  private void executeQueries() {

    String queryCountGps = "SELECT COUNT(*) FROM " + tablenamegps + ";";
    String queryCountTemp = "SELECT COUNT(*) FROM " + tablenametemp + ";";

    try {
      Statement stmtgps = conn.createStatement();
      Statement stmttemp = conn.createStatement();

      ResultSet resultgps = stmtgps.executeQuery(queryCountGps);
      ResultSet resulttemp = stmttemp.executeQuery(queryCountTemp);

      int gpssize = 0;
      int tempsize = 0;

      while (resultgps.next()) {
        gpssize = resultgps.getInt(1);
      }

      while (resulttemp.next()) {
        tempsize = resulttemp.getInt(1);
      }

      DataSource copy = new DataSource(metaDataSource);

      copy.getAttr().put("gpssize", String.valueOf(gpssize));
      copy.getAttr().put("templength", String.valueOf(tempsize));

      //log.info("MySQL gps " + gpssize);
      //log.info("MySQL temp " + tempsize);

      this.applyPolicies(metaDataSource, "Payload");


    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void startReceivingData(DataSource dataSource) {
    CompletableFuture.runAsync(() ->
        handle());
  }


}
