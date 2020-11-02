package de.uni.sttg.ipvs.as.composite.data.store.core.executor;

import de.uni.sttg.ipvs.as.composite.data.store.core.adapter.AbstractDataDestinationAdapter;
import de.uni.sttg.ipvs.as.composite.data.store.core.enforcer.AbstractExecutionEnforcer;
import de.uni.sttg.ipvs.as.composite.data.store.core.model.DataSource;
import de.uni.sttg.ipvs.as.composite.data.store.core.model.ExecutionDescription;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.casbin.jcasbin.main.Enforcer;

/**
 * This class applies the different polices on the given data
 *
 * @author Roman Bitz
 */
public class PolicyDecisionPoint {

  private List<? extends AbstractExecutionEnforcer> executionEnforcerList;
  private Enforcer accessEnforcer;
  private Map<String, AbstractDataDestinationAdapter> destinationMap;

  /**
   * Constructor
   */
  public PolicyDecisionPoint(List<? extends AbstractExecutionEnforcer> executionEnforcerList, Enforcer accessEnforcer,
      Map<String, AbstractDataDestinationAdapter> destinationMap) {

    this.executionEnforcerList = executionEnforcerList;
    this.accessEnforcer = accessEnforcer;
    this.destinationMap = destinationMap;

  }

  /**
   * Applies ExecutionPolicies and AccessPolicies on the given data.
   *
   * @param dataobj - The data on which the policies needs to be applied.
   */
  public void enforceAndExecutePolicies(DataSource dataobj, Object payload) throws Exception {

    List<List<String>> policies = new ArrayList<>();
    executionEnforcerList.forEach(enforcer -> policies.addAll(enforcer.getExecutions(dataobj)));

    for (List<String> policy : policies) {
      String action = policy.get(1);
      String target = policy.get(2);
      //Access Policy
      boolean result = this.accessEnforcer.enforce(dataobj.getId(), action, target);
      if (result) {
        this.processExecutionPolicy(dataobj.getId(), action, target, payload);
      }
    }

  }

  /**
   * Executes the ExecutionPolicy with the adapter function.
   */
  private void processExecutionPolicy(String data_id, String action, String target, Object payload) throws Exception {

    AbstractDataDestinationAdapter adapter = this.destinationMap.get(target);
    Function<ExecutionDescription, Boolean> function = adapter.getFunctionalityMap().get(action);

    if (function != null) {
      boolean result = function.apply(new ExecutionDescription(data_id, target, payload));
      if (!result) {
        throw new Exception("Execution of functionality " + action + " in adapter failed!");
      }
      return;
    }
    throw new Exception("AdapterFunction for " + action + " in DataDestination " + target + " is null");

  }

}
