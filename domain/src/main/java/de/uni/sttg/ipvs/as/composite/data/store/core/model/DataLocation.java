package de.uni.sttg.ipvs.as.composite.data.store.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.Map;
import lombok.Data;

/**
 *
 */
@Data
@JsonPropertyOrder({
    "host",
    "port",
    "user",
    "pass",
    "entrypoint",
    "attr"
})
public class DataLocation {

  @JsonProperty("host")
  public String host;
  @JsonProperty("port")
  public String port;
  @JsonProperty("user")
  public String user;
  @JsonProperty("pass")
  public String pass;
  @JsonProperty("entrypoint")
  public String entrypoint;
  @JsonProperty("path")
  public String path;
  @JsonProperty("attr")
  public Map<String, String> attr;

}
