package de.uni.sttg.ipvs.as.composite.data.store.core.enforcer;

/**
 * This is a Proxy Class for better naming and structure. All work is done in {@link AbstractExecutionPolicyEnforcer}.
 *
 * @author Roman Bitz
 */
public class ExecutionPolicyEnforcer extends AbstractExecutionPolicyEnforcer {

  public ExecutionPolicyEnforcer(String modelPath, String policyFile) {
    super(modelPath, policyFile, false);
  }

  public ExecutionPolicyEnforcer(String modelPath, String policyFile, boolean enableLog) {
    super(modelPath, policyFile, enableLog);
  }

}
