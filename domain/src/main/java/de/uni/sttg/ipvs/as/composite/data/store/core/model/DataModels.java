package de.uni.sttg.ipvs.as.composite.data.store.core.model;

import java.util.Map;
import lombok.Data;

/**
 * Holds the Pojos of the Json Configuration.
 *
 * @author Roman Bitz
 */
@Data
public class DataModels {

  private Map<String, DataSource> dataSources;
  private Map<String, DataDestination> dataDestinations;

}
