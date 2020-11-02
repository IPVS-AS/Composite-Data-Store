package de.uni.sttg.ipvs.as.composite.data.store.core.persistence;

import de.uni.sttg.ipvs.as.composite.data.store.core.model.DataDestination;
import de.uni.sttg.ipvs.as.composite.data.store.core.model.DataSource;
import java.util.Map;

/**
 * Responsible for writing and deleting Policy data or Datamodel data on a persistence storage.
 *
 * @author Roman Bitz
 */
public interface DatamodelAndPolicyRepository {

  /**
   * Saves the Accesspolicy.
   *
   * @return - if the operation was successful.
   */
  boolean saveAccessPolicy(String conf, String csv);

  /**
   * Saves an Executionpolicy or if not existing creates the given one.
   *
   * @return - if the operation was successful.
   */
  boolean saveExecutionPolicy(String policyname, String conf, String csv);

  /**
   * Deletes the given Executionpolicy
   *
   * @param policyname - name of the Policy
   * @return - if the operation was successful.
   */
  boolean deleteExecutionPolicy(String policyname);

  /**
   * Saves a DatasourceObject or if not existing creates the given one.
   *
   * @param lastKnownID - The ID of the Datasource before this operation (in case of ID change)
   * @return - if the operation was successful.
   */
  boolean saveDatasource(DataSource dataSource, String lastKnownID);

  /**
   * Deletes the given DataSource
   *
   * @param dataSourceID - _id of the Datasource
   * @return - if the operation was successful.
   */
  boolean deleteDataSource(String dataSourceID);

  /**
   * Saves a DatadestinationObject or if not existing creates the given one.
   *
   * @param lastKnownID - The ID of the Datadestination before this operation (in case of ID change)
   * @return - if the operation was successful.
   */
  boolean saveDataDestination(DataDestination dataDestination, String lastKnownID);

  /**
   * Deletes the given DataSource
   *
   * @param dataDestinationID - _id of the Datasource
   * @return - if the operation was successful.
   */
  boolean deleteDataDestination(String dataDestinationID);

  /**
   * Gives a List of DataSource.
   *
   * @return - The Map of DataSources.
   */
  Map<String, DataSource> getDataSources();

  /**
   * Gives a List of DataDestinations.
   *
   * @return - The list of DataDestinations.
   */
  Map<String, DataDestination> getDataDestinations();

  /**
   * Returns the Accesspolicy in a Map.
   *
   * @return - Map with the key beeing the "csv" or "conf".
   */
  Map<String, String> getAccessPolicy();

  /**
   * Returns the Executionpolicy in a Map with other Maps including each a executionpolicy.
   *
   * @return - Map with the key beeing the executionpolicy name.
   */
  Map<String, Map<String, String>> getExecutionPolicy();

}
