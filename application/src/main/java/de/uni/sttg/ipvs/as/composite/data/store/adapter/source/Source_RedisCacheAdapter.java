package de.uni.sttg.ipvs.as.composite.data.store.adapter.source;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.uni.sttg.ipvs.as.composite.data.store.core.adapter.AbstractDataSourceAdapter;
import de.uni.sttg.ipvs.as.composite.data.store.core.model.DataLocation;
import de.uni.sttg.ipvs.as.composite.data.store.core.model.DataSource;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;

/**
 * @author Roman Bitz
 */
@Slf4j
@Component("TEMP_DATA_REDIS")
public class Source_RedisCacheAdapter extends AbstractDataSourceAdapter {

  DataSource metaDataSource;
  String keyname;
  Jedis jedis;
  ObjectMapper mapper = new ObjectMapper();


  @Override
  public void initializeConnection(DataSource dataSource) {

    DataLocation dataLocation = dataSource.getLocation();
    this.metaDataSource = dataSource;
    String host = dataLocation.getHost();
    String port = dataLocation.getPort();
    String pass = dataLocation.getPass();
    this.keyname = dataLocation.getPath();

    this.jedis = new Jedis(host, Integer.parseInt(port));

    jedis.auth(pass);

  }

  @Override
  public void startReceivingData(DataSource dataSource) {

    CompletableFuture.runAsync(() ->
        handle());

  }


  public void handle() {

    String lastvalue = "";

    while (true) {

      try {
        String jsonstring = jedis.get(keyname);

        //log.info("Redis Data pull");

        TypeReference<HashMap<String, String>> typeRef
            = new TypeReference<HashMap<String, String>>() {
        };

        HashMap<String, String> json = mapper.readValue(jsonstring, typeRef);

        String id = json.get("id");

        if (lastvalue.equals(id)) {
          //log.info("Redis no new data");
          TimeUnit.SECONDS.sleep(1);
          continue;
        }
        lastvalue = id;

        log.info(" [R] " + json);

        DataSource copy = new DataSource(this.metaDataSource);
        copy.getAttr().put("temperatur", json.get(metaDataSource.getCriteria()));

        this.applyPolicies(copy, json);

        TimeUnit.SECONDS.sleep(1);
      } catch (InterruptedException e) {
        log.warn("Sleep Timeout in Redis Adapter");
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }


  }


}
