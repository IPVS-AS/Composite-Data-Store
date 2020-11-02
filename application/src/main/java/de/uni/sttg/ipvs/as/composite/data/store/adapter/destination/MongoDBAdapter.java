package de.uni.sttg.ipvs.as.composite.data.store.adapter.destination;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import de.uni.sttg.ipvs.as.composite.data.store.core.adapter.AbstractDataDestinationAdapter;
import de.uni.sttg.ipvs.as.composite.data.store.core.model.DataDestination;
import de.uni.sttg.ipvs.as.composite.data.store.core.model.DataLocation;
import de.uni.sttg.ipvs.as.composite.data.store.core.model.ExecutionDescription;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author Roman Bitz
 */
@Slf4j
@Component("MONGODB")
public class MongoDBAdapter extends AbstractDataDestinationAdapter {

  private DB mydatabase;
  private DBCollection collectiongps;
  private DBCollection collectiontemp;

  @Override
  public void initializeConnection(DataDestination dataDestination) {

    DataLocation dataLocation = dataDestination.getLocation();
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
  public Map<String, Function<ExecutionDescription, Boolean>> initializeAndReturnFunctionalityMapping(DataDestination dataDestination) {

    Map<String, Function<ExecutionDescription, Boolean>> functionalityMapping = new HashMap<>();

    functionalityMapping.put("INSERT_GPS_DATA", this::executeSyncDataGPS);
    functionalityMapping.put("DELETE_GPS_DATA", this::executeDeleteDataGPS);
    functionalityMapping.put("INSERT_TEMP_DATA", this::executeSyncDataTemp);
    functionalityMapping.put("DELETE_TEMP_DATA", this::executeDeleteDataTemp);

    return functionalityMapping;

  }

  public boolean executeSyncDataGPS(ExecutionDescription executionDescription) {

    log.info("Sync MongoDB");

    BasicDBObject doc = new BasicDBObject((Map) executionDescription.getPayload());

    this.collectiongps.insert(doc);

    return true;

  }

  public boolean executeDeleteDataGPS(ExecutionDescription executionDescription) {

    log.info("Deleting MongoDB Collection");
    this.collectiongps.drop();

    return true;

  }

  public boolean executeSyncDataTemp(ExecutionDescription executionDescription) {

    log.info("Sync MongoDB");

    BasicDBObject doc = new BasicDBObject((Map) executionDescription.getPayload());

    this.collectiontemp.insert(doc);

    return true;

  }

  public boolean executeDeleteDataTemp(ExecutionDescription executionDescription) {

    log.info("Deleting MongoDB Collection");
    this.collectiontemp.drop();

    return true;

  }

}
