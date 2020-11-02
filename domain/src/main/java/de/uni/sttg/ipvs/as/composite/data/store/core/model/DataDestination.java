package de.uni.sttg.ipvs.as.composite.data.store.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;
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
    "functionalities"
})
public class DataDestination {

  @NotNull
  @JsonProperty("_id")
  public String id;
  @JsonProperty("location")
  public DataLocation location;
  @JsonProperty("functionalities")
  public List<String> functionalities = null;
  @JsonProperty("attr")
  public Map<String, String> attr;


}
