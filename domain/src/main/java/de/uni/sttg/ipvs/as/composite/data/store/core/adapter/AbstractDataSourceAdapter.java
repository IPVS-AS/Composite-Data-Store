package de.uni.sttg.ipvs.as.composite.data.store.core.adapter;

import de.uni.sttg.ipvs.as.composite.data.store.core.adapter.interfaces.IDataSourceAdapter;
import de.uni.sttg.ipvs.as.composite.data.store.core.executor.PolicyDecisionPoint;
import de.uni.sttg.ipvs.as.composite.data.store.core.model.DataSource;
import lombok.Data;


/**
 * @author Roman Bitz
 */
@Data
public abstract class AbstractDataSourceAdapter implements IDataSourceAdapter {

  PolicyDecisionPoint policyDecisionPoint;

  /**
   * Applies the configured Policies on the data
   *
   * @param dataobj - Represents the json file
   * @param payload - Represents the actual data
   */
  public void applyPolicies(DataSource dataobj, Object payload) {
    try {
      this.policyDecisionPoint.enforceAndExecutePolicies(dataobj, payload);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
