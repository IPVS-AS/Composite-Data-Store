package de.uni.sttg.ipvs.as.composite.data.store.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.uni.sttg.ipvs.as.composite.data.store.config.CompositeStoreConfiguration;
import de.uni.sttg.ipvs.as.composite.data.store.core.adapter.AbstractDataDestinationAdapter;
import de.uni.sttg.ipvs.as.composite.data.store.core.adapter.AbstractDataSourceAdapter;
import de.uni.sttg.ipvs.as.composite.data.store.core.model.DataDestination;
import de.uni.sttg.ipvs.as.composite.data.store.core.model.DataModels;
import de.uni.sttg.ipvs.as.composite.data.store.core.model.DataSource;
import de.uni.sttg.ipvs.as.composite.data.store.persistence.FilePersistenceService;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.casbin.jcasbin.main.CoreEnforcer;
import org.casbin.jcasbin.main.Enforcer;
import org.casbin.jcasbin.model.Model;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Simple controller for handling UI purposes.
 *
 * @author Roman Bitz
 */
@Slf4j
@RestController
public class UiController {

  @Autowired
  private CompositeStoreConfiguration config;
  @Autowired
  private FilePersistenceService fileService;

  @RequestMapping(value = "/getPolicies", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
  public Map<String, Object> getPolicies() {

    Map<String, Object> policiesResponse = new HashMap<>();
    policiesResponse.put("accesspolicy", fileService.getAccessPolicy());
    policiesResponse.put("execution", fileService.getExecutionPolicy());

    return policiesResponse;
  }

  @RequestMapping(value = "/writePolicies", method = RequestMethod.POST, produces = MediaType.TEXT_PLAIN_VALUE)
  public String writePolicies(String filename, String conf, String csv) {

    if (filename.equals("accesspolicy")) {
      if (!fileService.saveAccessPolicy(conf, csv)) {
        return "IO Error occured!";
      }
    } else {
      if (!fileService.saveExecutionPolicy(filename, conf, csv)) {
        return "IO Error occured!";
      }
    }

    return "true";
  }

  @RequestMapping(value = "/validatePolicy", method = RequestMethod.POST, produces = MediaType.TEXT_PLAIN_VALUE)
  public String validatePolicies(String conf, String csv) {

    try {
      String copy = conf;

      copy = copy.replace("[request_definition]", "");
      copy = copy.replace("[policy_definition]", "");
      copy = copy.replace("[policy_effect]", "");

      if (copy.contains("_")) {
        throw new Throwable("Character ' _ ' is not allowed");
      }

      // create Model
      Model m = CoreEnforcer.newModel(conf);

      // Check Model if a statement is missing
      if (!m.model.containsKey("r") || !m.model.containsKey("p") || !m.model.containsKey("e") || !m.model.containsKey("m")) {
        throw new Throwable("One of the statements is either missing or not correct: request_definition, policy_definition, policy_effect, matchers");
      }

      String effect = m.model.get("e").get("e").value;
      if (!effect.equals("some(where (p_eft == allow))") && !effect.equals("!some(where (p_eft == deny))") && !effect
          .equals("some(where (p_eft == allow)) && !some(where (p_eft == deny))")) {
        throw new Throwable("Policy Effect is not correct!");
      }

      // Read Policy CSV
      BufferedReader br = new BufferedReader(new StringReader(csv));

      String line;
      String[] tokens = new String[0];

      while ((line = br.readLine()) != null) {

        if (line.equals("")) {
          continue;
        }

        if (line.charAt(0) == '#') {
          continue;
        }

        tokens = line.split(", ");

        String key = tokens[0];
        String sec = key.substring(0, 1);
        m.model.get(sec).get(key).policy.add(Arrays.asList(Arrays.copyOfRange(tokens, 1, tokens.length)));

      }

      br.close();

      // Test Enforcer
      Enforcer enf = new Enforcer(m);
      enf.enforce(Arrays.copyOfRange(tokens, 1, tokens.length));
    } catch (Throwable e) {
      //ignore ABAC policies
      if (e.toString().contains("com.googlecode.aviator.exception.ExpressionRuntimeException: Could not find variable")) {
        return "true";
      }
      return e.toString();

    }

    return "true";
  }

  @RequestMapping("/getDatamodel")
  public DataModelReturnObject getDatamodel() {

    DataModels dm = new DataModels();
    dm.setDataDestinations(fileService.getDataDestinations());
    dm.setDataSources(fileService.getDataSources());

    List<String> sourceAdapters = new ArrayList<>();
    List<String> destinationAdapters = new ArrayList<>();

    for (AbstractDataSourceAdapter adapter : config.getSourceAdapters()) {
      sourceAdapters.add(adapter.getClass().getDeclaredAnnotation(Component.class).value());
    }

    for (AbstractDataDestinationAdapter adapter : config.getDestinationAdapters()) {
      destinationAdapters.add(adapter.getClass().getDeclaredAnnotation(Component.class).value());
    }

    return new DataModelReturnObject(dm, sourceAdapters, destinationAdapters);
  }

  @RequestMapping(value = "/writeDatamodel", method = RequestMethod.POST, produces = MediaType.TEXT_PLAIN_VALUE)
  public String writeDatamodel(String datamodel, boolean isDatasource, String originalID) {

    ObjectMapper mapper = new ObjectMapper();

    if (isDatasource) {
      DataSource source;

      try {
        source = mapper.readValue(datamodel, DataSource.class);
      } catch (IOException e) {
        e.printStackTrace();
        return "Syntax of Datamodel seems to be wrong";
      }

      if (source.getId() == null || source.getId().equals("")) {
        return "ID is null or empty";
      }

      if (!fileService.saveDatasource(source, originalID)) {
        return "Syntax of Datamodel seems to be wrong";
      }

      return "true";

    }

    //DataDestinations
    DataDestination destination;
    try {
      destination = mapper.readValue(datamodel, DataDestination.class);
    } catch (IOException e) {
      e.printStackTrace();
      return "Syntax of Datamodel seems to be wrong";
    }

    if (destination.getId() == null || destination.getId().equals("")) {
      return "ID is null or empty";
    }

    if (!fileService.saveDataDestination(destination, originalID)) {
      return "Syntax of Datamodel seems to be wrong";
    }

    return "true";
  }

  @RequestMapping(value = "/deletePolicy", method = RequestMethod.DELETE, produces = MediaType.TEXT_PLAIN_VALUE)
  public String deleteDatamodel(String policyname) {

    if (!fileService.deleteExecutionPolicy(policyname)) {
      return "Deletion Failed";
    }

    return "true";

  }


  @RequestMapping(value = "/deleteDatamodel", method = RequestMethod.DELETE, produces = MediaType.TEXT_PLAIN_VALUE)
  public String deleteDatamodel(String modelname, boolean isDatasource) {

    if (isDatasource) {
      if (!fileService.deleteDataSource(modelname)) {
        return "Deletion Failed";
      }
    } else {
      if (!fileService.deleteDataDestination(modelname)) {
        return "Deletion Failed";
      }

    }

    return "true";

  }


  @Data
  @AllArgsConstructor
  public class DataModelReturnObject {

    public DataModels datamodels;
    public List<String> sourceAdapters;
    public List<String> destinationAdapters;


  }


}
