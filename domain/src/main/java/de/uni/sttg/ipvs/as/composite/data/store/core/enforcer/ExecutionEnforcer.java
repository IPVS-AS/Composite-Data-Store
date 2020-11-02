package de.uni.sttg.ipvs.as.composite.data.store.core.enforcer;

/**
 * This is a Proxy Class for better naming and structure. All work is done in {@link AbstractExecutionEnforcer}.
 *
 * @author Roman Bitz
 */
public class ExecutionEnforcer extends AbstractExecutionEnforcer {

  public ExecutionEnforcer(String modelPath, String policyFile) {
    super(modelPath, policyFile, false);
  }

  public ExecutionEnforcer(String modelPath, String policyFile, boolean enableLog) {
    super(modelPath, policyFile, enableLog);
  }

}
