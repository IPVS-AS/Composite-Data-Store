package de.uni.sttg.ipvs.as.composite.data.store.adapter.source;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.Delivery;
import de.uni.sttg.ipvs.as.composite.data.store.core.adapter.AbstractDataSourceAdapter;
import de.uni.sttg.ipvs.as.composite.data.store.core.model.DataLocation;
import de.uni.sttg.ipvs.as.composite.data.store.core.model.DataSource;
import java.io.IOException;
import java.util.HashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author Roman Bitz
 */
@Slf4j
@Component("GPS_DATA_RABBITMQ")
public class Source_RabbitMQAdapter extends AbstractDataSourceAdapter implements DeliverCallback {

  DataSource metaDataSource;
  Connection connection;
  Channel channel;
  String queuename;

  ObjectMapper mapper = new ObjectMapper();

  @Override
  public void initializeConnection(DataSource dataSource) {

    DataLocation dataLocation = dataSource.getLocation();
    this.metaDataSource = dataSource;
    String host = dataLocation.getHost();
    String port = dataLocation.getPort();
    String user = dataLocation.getUser();
    String pass = dataLocation.getPass();
    String vhost = dataLocation.getEntrypoint();
    this.queuename = dataLocation.getPath();

    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost(host);
    factory.setPort(Integer.parseInt(port));
    factory.setUsername(user);
    factory.setPassword(pass);
    factory.setVirtualHost(vhost);

    try {
      this.connection = factory.newConnection();
      this.channel = this.connection.createChannel();
      this.channel.queueDeclare(this.queuename, false, false, false, null);

      log.info("Initialized RabbitMQ Connection");

    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  @Override
  public void startReceivingData(DataSource dataSource) throws IOException {

    this.channel.basicConsume(this.queuename, true, this,
        consumerTag -> {
        }
    );

  }

  @Override
  public void handle(String consumerTag, Delivery delivery) throws IOException {

    String message = new String(delivery.getBody(), "UTF-8");
    log.info(" [x] Received '" + message + "'");

    TypeReference<HashMap<String, String>> typeRef
        = new TypeReference<HashMap<String, String>>() {
    };

    HashMap<String, String> json = mapper.readValue(message, typeRef);

    DataSource copy = new DataSource(this.metaDataSource);
    copy.getAttr().put("phonetype", json.get(metaDataSource.getCriteria()));

    this.applyPolicies(copy, json);

  }


}
