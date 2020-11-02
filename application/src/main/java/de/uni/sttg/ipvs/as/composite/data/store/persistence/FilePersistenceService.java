package de.uni.sttg.ipvs.as.composite.data.store.persistence;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import de.uni.sttg.ipvs.as.composite.data.store.core.model.DataDestination;
import de.uni.sttg.ipvs.as.composite.data.store.core.model.DataSource;
import de.uni.sttg.ipvs.as.composite.data.store.core.persistence.DatamodelAndPolicyRepository;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * {@inheritDoc}
 */
@Slf4j
@Service
public class FilePersistenceService implements DatamodelAndPolicyRepository {

  //Paths
  @Value("${composite.store.model.datasources.path}")
  private String pathDataSources;
  @Value("${composite.store.model.datadestinations.path}")
  private String pathDataDestination;
  @Value("${composite.store.policy.basepath}")
  private String pathAccessPolicyDir;
  @Value("${composite.store.policy.execution.dir}")
  private String pathExecutionPolicyDir;

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean saveAccessPolicy(String conf, String csv) {
    return savePolicy(pathAccessPolicyDir + "/accesspolicy", conf, csv);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean saveExecutionPolicy(String policyname, String conf, String csv) {
    return savePolicy(pathExecutionPolicyDir + "/" + policyname, conf, csv);
  }

  private boolean savePolicy(String pathWithName, String conf, String csv) {
    try {
      FileOutputStream fosconf = new FileOutputStream(pathWithName + ".conf");
      fosconf.write(conf.getBytes());
      fosconf.close();
      FileOutputStream foscsv = new FileOutputStream(pathWithName + ".csv");
      foscsv.write(csv.getBytes());
      foscsv.close();
    } catch (IOException e) {
      e.printStackTrace();
      return false;
    }
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean deleteExecutionPolicy(String policyname) {
    File csv = new File(pathExecutionPolicyDir + "/" + policyname + ".csv");
    File conf = new File(pathExecutionPolicyDir + "/" + policyname + ".conf");
    return csv.delete() && conf.delete();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean saveDatasource(DataSource dataSource, String lastKnownID) {

    if (dataSource.getId() == null || dataSource.getId().equals("")) {
      return false;
    }

    Map<String, DataSource> sourceMap = getDataSources();
    sourceMap.put(lastKnownID, dataSource);
    ArrayList sourceList = new ArrayList(sourceMap.values());
    ObjectWriter writer = new ObjectMapper().writer(new DefaultPrettyPrinter());

    try {
      writer.writeValue(new File(this.pathDataSources), sourceList);
    } catch (IOException e) {
      e.printStackTrace();
      return false;
    }
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean deleteDataSource(String dataSourceID) {
    Map<String, DataSource> sourceMap = getDataSources();
    sourceMap.remove(dataSourceID);
    ArrayList sourceList = new ArrayList(sourceMap.values());

    ObjectWriter writer = new ObjectMapper().writer(new DefaultPrettyPrinter());
    try {
      writer.writeValue(new File(this.pathDataSources), sourceList);
    } catch (IOException e) {
      e.printStackTrace();
      return false;
    }

    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean saveDataDestination(DataDestination dataDestination, String lastKnownID) {

    if (dataDestination.getId() == null || dataDestination.getId().equals("")) {
      return false;
    }

    Map<String, DataDestination> destinationMap = getDataDestinations();
    destinationMap.put(lastKnownID, dataDestination);
    ArrayList destinationList = new ArrayList(destinationMap.values());
    ObjectWriter writer = new ObjectMapper().writer(new DefaultPrettyPrinter());

    try {
      writer.writeValue(new File(this.pathDataDestination), destinationList);

    } catch (IOException e) {
      e.printStackTrace();
      return false;
    }
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean deleteDataDestination(String dataDestinationID) {
    Map<String, DataDestination> destinationMap = getDataDestinations();
    destinationMap.remove(dataDestinationID);
    ArrayList destinationList = new ArrayList(destinationMap.values());

    ObjectWriter writer = new ObjectMapper().writer(new DefaultPrettyPrinter());
    try {
      writer.writeValue(new File(this.pathDataDestination), destinationList);
    } catch (IOException e) {
      e.printStackTrace();
      return false;
    }

    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<String, DataSource> getDataSources() {
    ObjectMapper mapper = new ObjectMapper();
    List<DataSource> sourcesList = null;
    try {
      sourcesList = Arrays.asList(mapper.readValue(new File(pathDataSources), DataSource[].class));
    } catch (IOException e) {
      e.printStackTrace();
      return new HashMap<>();
    }
    return sourcesList.stream().collect(Collectors.toMap(DataSource::getId, item -> item));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<String, DataDestination> getDataDestinations() {
    ObjectMapper mapper = new ObjectMapper();
    List<DataDestination> destionationsList = null;
    try {
      destionationsList = Arrays.asList(mapper.readValue(new File(pathDataDestination), DataDestination[].class));
    } catch (IOException e) {
      e.printStackTrace();
      new HashMap<>();
    }
    return destionationsList.stream().collect(Collectors.toMap(DataDestination::getId, item -> item));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<String, String> getAccessPolicy() {

    Map<String, String> access = new HashMap<>();
    try {
      access.put("conf", new String(Files.readAllBytes(Paths.get(this.pathAccessPolicyDir + "/accesspolicy.conf")), "UTF-8"));
      access.put("csv", new String(Files.readAllBytes(Paths.get(this.pathAccessPolicyDir + "/accesspolicy.csv")), "UTF-8"));
    } catch (IOException e) {
      e.printStackTrace();
      return access;
    }
    return access;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<String, Map<String, String>> getExecutionPolicy() {

    Map<String, Map<String, String>> execution = new HashMap<>();

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

    //Find pairs of .csv and .conf files with the same name
    for (File possibleConfFile : listOfFiles) {
      if (possibleConfFile.getName().contains(".conf")) {

        String possibleCsvName = possibleConfFile.getName().replace(".conf", ".csv");
        Optional<File> possibleCSV = listOfFiles.stream().filter(file -> file.getName().equals(possibleCsvName)).findFirst();
        if (possibleCSV.isPresent()) {
          File csvFile = possibleCSV.get();

          Map<String, String> oneExecutionPolicy = new HashMap<>();
          try {
            oneExecutionPolicy.put("conf", new String(Files.readAllBytes(Paths.get(possibleConfFile.getPath())), "UTF-8"));
            oneExecutionPolicy.put("csv", new String(Files.readAllBytes(Paths.get(csvFile.getPath())), "UTF-8"));
          } catch (IOException e) {
            e.printStackTrace();
          }

          execution.put(possibleCsvName.replace(".csv", ""), oneExecutionPolicy);

        } else {
          log.error("No CSV File found for " + possibleConfFile.getName());
        }

      }
    }

    return execution;
  }
}
