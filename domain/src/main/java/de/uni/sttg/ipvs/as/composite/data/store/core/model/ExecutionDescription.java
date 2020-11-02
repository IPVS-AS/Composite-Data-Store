package de.uni.sttg.ipvs.as.composite.data.store.core.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author Roman Bitz
 */
@Data
@AllArgsConstructor
public class ExecutionDescription {

  private String dataSourceID;
  private String dataDestinationID;
  private Object payload;

}
