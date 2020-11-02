package de.uni.sttg.ipvs.as.composite.data.store.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.Data;


/**
 * @author Roman Bitz
 */
@Data
@JsonPropertyOrder({
    "_id",
    "location",
    "criteria",
    "attr"
})
public class DataSource {

  @NotNull
  @JsonProperty("_id")
  public String id;
  @JsonProperty("location")
  public DataLocation location;
  @JsonProperty("criteria")
  public String criteria;
  @JsonProperty("attr")
  public Map<String, Object> attr;

  public DataSource(DataSource dataSource) {

    this.id = dataSource.getId();
    this.location = dataSource.getLocation();
    this.criteria = dataSource.getCriteria();
    this.attr = dataSource.getAttr();

  }

  // For Jackson
  public DataSource() {

  }
}
