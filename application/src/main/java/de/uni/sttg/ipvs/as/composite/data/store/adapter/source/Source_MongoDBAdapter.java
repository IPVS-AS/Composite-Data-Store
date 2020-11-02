package de.uni.sttg.ipvs.as.composite.data.store.adapter.source;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import de.uni.sttg.ipvs.as.composite.data.store.core.adapter.AbstractDataSourceAdapter;
import de.uni.sttg.ipvs.as.composite.data.store.core.model.DataLocation;
import de.uni.sttg.ipvs.as.composite.data.store.core.model.DataSource;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author Roman Bitz
 */
@Slf4j
@Component("SOURCE_MONGODB")
public class Source_MongoDBAdapter extends AbstractDataSourceAdapter {

  DataSource metaDataSource;
  private DB mydatabase;
  private DBCollection collectiongps;
  private DBCollection collectiontemp;

  @Override
  public void initializeConnection(DataSource dataSource) {

    metaDataSource = dataSource;
    DataLocation dataLocation = dataSource.getLocation();
    String host = dataLocation.getHost();
    String port = dataLocation.getPort();
    String user = dataLocation.getUser();
    String pass = dataLocation.getPass();
    String entrypoint = dataLocation.getEntrypoint();
    String database = dataLocation.getPath();
    String collectiongpsname = dataLocation.getAttr().get("collectiongps");
    String collectiontempname = dataLocation.getAttr().get("collectiontemp");

    try {

      MongoCredential credential = MongoCredential.createCredential(user, entrypoint, pass.toCharArray());
      MongoClient mongoClient = new MongoClient(new ServerAddress(host, Integer.parseInt(port)), Arrays.asList(credential));
      this.mydatabase = mongoClient.getDB(database);
      this.collectiongps = this.mydatabase.getCollection(collectiongpsname);
      this.collectiontemp = this.mydatabase.getCollection(collectiontempname);

    } catch (UnknownHostException e) {
      e.printStackTrace();
    }

  }

  @Override
  public void startReceivingData(DataSource dataSource) {

    CompletableFuture.runAsync(() ->
        handle());

  }

  public void handle() {

    while (true) {
      long gpslength = this.collectiongps.getCount();
      long templength = this.collectiontemp.getCount();

      DataSource copy = new DataSource(metaDataSource);

      copy.getAttr().put("gpssize", String.valueOf(gpslength));
      copy.getAttr().put("templength", String.valueOf(templength));

      //log.info("Mongo gps " + gpslength);
      //log.info("Mongo temp " + templength);

      this.applyPolicies(metaDataSource, "Payload");

      try {
        TimeUnit.SECONDS.sleep(1);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

  }


}
