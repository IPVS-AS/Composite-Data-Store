package de.uni.sttg.ipvs.as.composite.data.store.config;

import de.uni.sttg.ipvs.as.composite.data.store.core.adapter.AbstractDataDestinationAdapter;
import de.uni.sttg.ipvs.as.composite.data.store.core.adapter.AbstractDataSourceAdapter;
import de.uni.sttg.ipvs.as.composite.data.store.core.enforcer.ExecutionEnforcer;
import de.uni.sttg.ipvs.as.composite.data.store.core.executor.PolicyDecisionPoint;
import de.uni.sttg.ipvs.as.composite.data.store.core.model.DataDestination;
import de.uni.sttg.ipvs.as.composite.data.store.core.model.DataModels;
import de.uni.sttg.ipvs.as.composite.data.store.core.model.DataSource;
import de.uni.sttg.ipvs.as.composite.data.store.persistence.FilePersistenceService;
import java.io.File;
import java.nio.file.FileSystemNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.Data;
import org.casbin.jcasbin.main.Enforcer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/**
 * @author Roman Bitz
 */
@Configuration
@Data
public class CompositeStoreConfiguration {

  Logger log = LoggerFactory.getLogger(CompositeStoreConfiguration.class);

  @Autowired
  private List<AbstractDataDestinationAdapter> destinationAdapters;

  @Autowired
  private List<AbstractDataSourceAdapter> sourceAdapters;

  @Autowired
  private FilePersistenceService fileService;

  //General confg
  @Value("${composite.store.logging.policy}")
  private boolean enablePolicyLog;
  //Paths
  @Value("${composite.store.model.datasources.path}")
  private String pathDataSources;
  @Value("${composite.store.model.datadestinations.path}")
  private String pathDataDestination;
  @Value("${composite.store.policy.access.csv.path}")
  private String pathAccessPolicy;
  @Value("${composite.store.policy.access.conf.path}")
  private String pathAccessModel;
  @Value("${composite.store.policy.execution.dir}")
  private String pathExecutionPolicyDir;

  private DataModels dataModels;
  private PolicyDecisionPoint policyDecisionPoint;

  @Bean
  public void initializeCompositeStore() {

    this.createDataModels();
    this.createDestinationAdapters();
    this.createDataController();
    this.createSourceAdapters();
    this.startSourceAdapters();

  }

  private DataModels createDataModels() {

    DataModels dm = new DataModels();

    Map<String, DataSource> dataSources = fileService.getDataSources();
    Map<String, DataDestination> dataDestinations = fileService.getDataDestinations();

    dm.setDataDestinations(dataDestinations);
    dm.setDataSources(dataSources);

    this.dataModels = dm;

    return dm;
  }

  private void startSourceAdapters() {

    for (AbstractDataSourceAdapter adapter : this.sourceAdapters) {

      String id = adapter.getClass().getDeclaredAnnotation(Component.class).value();
      try {
        adapter.startReceivingData(this.dataModels.getDataSources().get(id));
      } catch (Exception e) {
        log.error("Source Adapter( " + adapter.getClass().getName() + " ) could not be started:");
        e.printStackTrace();
      }

    }
  }

  private void createDataController() {

    Enforcer accessEnforcer = new Enforcer(this.pathAccessModel, this.pathAccessPolicy, enablePolicyLog);

    List<ExecutionEnforcer> executionList = this.getExecutionPolicyEnforcerList();

    if (executionList.isEmpty()) {
      log.info("#### No ExecutionPolicies found !!! ####");
    }

    Map<String, AbstractDataDestinationAdapter> map = new HashMap();

    for (AbstractDataDestinationAdapter adapter : this.destinationAdapters) {

      String id = adapter.getClass().getDeclaredAnnotation(Component.class).value();
      map.put(id, adapter);

    }

    this.policyDecisionPoint = new PolicyDecisionPoint(executionList, accessEnforcer, map);

  }

  private void createDestinationAdapters() {

    log.info("### List of DestinationAdapters: ###");

    for (AbstractDataDestinationAdapter adapter : this.destinationAdapters) {
      log.info(adapter.getClass().toGenericString());

      String id = adapter.getClass().getDeclaredAnnotation(Component.class).value();
      DataDestination destination = this.dataModels.getDataDestinations().get(id);

      try {
        adapter.initializeConnection(destination);
      } catch (Exception e) {
        log.error("Destination Adapter( " + adapter.getClass().getName() + " ) could not be initialized.");
        e.printStackTrace();
      }

      //TODO in future check functionalitymapping for public and nullpointer
      try {
        adapter.setFunctionalityMap(adapter.initializeAndReturnFunctionalityMapping(destination));
      } catch (Exception e) {
        log.error("FunctionMapping of Destination Adapter( " + adapter.getClass().getName() + " ) is not correct. ");
        e.printStackTrace();
      }

    }
    log.info("##################################");

  }

  private void createSourceAdapters() {
    log.info("### List of SourceAdapters: ###");

    for (AbstractDataSourceAdapter adapter : this.sourceAdapters) {

      log.info(adapter.getClass().toGenericString());

      String id = adapter.getClass().getDeclaredAnnotation(Component.class).value();
      DataSource source = this.dataModels.getDataSources().get(id);

      try {
        adapter.initializeConnection(source);
      } catch (Exception e) {
        log.error("Source Adapter( " + adapter.getClass().getName() + " ) could not be initialized.");
        e.printStackTrace();
      }
      adapter.setPolicyDecisionPoint(this.policyDecisionPoint);

    }
    log.info("##################################");
  }


  private List<ExecutionEnforcer> getExecutionPolicyEnforcerList() throws FileSystemNotFoundException {

    File dir = new File(pathExecutionPolicyDir);
    if (!dir.isDirectory()) {
      throw new FileSystemNotFoundException("Execution Policy Path is not a directory");
    }

    //Only look at .csv and .conf files
    File[] filteredFilenames = dir.listFiles((dir1, name) -> {
      if (name.contains(".conf") || name.contains(".csv")) {
        return true;
      }
      return false;
    });

    List<File> listOfFiles = Arrays.asList(filteredFilenames);

    List<ExecutionEnforcer> executionEnforcerList = new ArrayList<>();
    //Find pairs of .csv and .conf files with the same name
    for (File possibleConfFile : listOfFiles) {
      if (possibleConfFile.getName().contains(".conf")) {

        String possibleCsvName = possibleConfFile.getName().replace(".conf", ".csv");
        Optional<File> possibleCSV = listOfFiles.stream().filter(file -> file.getName().equals(possibleCsvName)).findFirst();
        if (possibleCSV.isPresent()) {
          File csvFile = possibleCSV.get();
          log.info("Found ExecutionPolicy Pair: " + csvFile.getName() + " and " + possibleConfFile.getName());
          executionEnforcerList.add(new ExecutionEnforcer(possibleConfFile.getPath(), csvFile.getPath(), enablePolicyLog));

        } else {
          log.error("No CSV File found for " + possibleConfFile.getName());
        }

      }
    }

    return executionEnforcerList;
  }


}
