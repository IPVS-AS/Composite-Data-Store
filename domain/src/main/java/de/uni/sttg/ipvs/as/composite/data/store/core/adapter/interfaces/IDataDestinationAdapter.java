package de.uni.sttg.ipvs.as.composite.data.store.core.adapter.interfaces;

import de.uni.sttg.ipvs.as.composite.data.store.core.model.DataDestination;
import de.uni.sttg.ipvs.as.composite.data.store.core.model.ExecutionDescription;
import java.util.Map;
import java.util.function.Function;

/**
 * @author Roman Bitz
 */

public interface IDataDestinationAdapter {

  /**
   * This method will be called on startup. You should use the two arguments to create a Connection to a DataDestination. For example you can create a
   * Connection to a MongoDB or MYSQL Database. All the values, like user password, host, port etc. are in the given arguments, which represent the
   * JSON configuration files.
   *
   * @param dataDestination - representation of one entity in the datadestinations.json file.
   */
  void initializeConnection(DataDestination dataDestination) throws Exception;

  /**
   * This method will be called on startup. You should return the functionalitymap. this is a map containing a key, that represents a functionality
   * key in the datadestinations.json file. The Value should be a method of your adapter class, written by you. The mapping tells the framework, which
   * exact method to call is a functionality of a DataDestination is called.
   *
   * @param dataDestination - representation of the datadestinations.json file.
   * @return - The FunctionalityMapping.
   */
  Map<String, Function<ExecutionDescription, Boolean>> initializeAndReturnFunctionalityMapping(DataDestination dataDestination) throws Exception;

}
