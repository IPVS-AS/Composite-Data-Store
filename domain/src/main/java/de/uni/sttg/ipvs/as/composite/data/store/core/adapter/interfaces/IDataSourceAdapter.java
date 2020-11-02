package de.uni.sttg.ipvs.as.composite.data.store.core.adapter.interfaces;

import de.uni.sttg.ipvs.as.composite.data.store.core.model.DataSource;

/**
 * @author Roman Bitz
 */

public interface IDataSourceAdapter {

  /**
   * This method will be called on startup. You should use the two arguments to create a Connection to a DataSource. For example you can create a
   * Connection to a MongoDB or MYSQL Database. All the values, like user password, host, port etc. are in the given arguments, which represent the
   * JSON configuration files.
   *
   * @param dataSource - representation of one entity in the datasource.json file.
   */
  void initializeConnection(DataSource dataSource) throws Exception;

  /**
   * This method will be called on startup. This will signal the DataSource Adapter to start receiving Data.
   *
   * @param dataSource - representation of one entity in the datasource.json file.
   */
  void startReceivingData(DataSource dataSource) throws Exception;

}
