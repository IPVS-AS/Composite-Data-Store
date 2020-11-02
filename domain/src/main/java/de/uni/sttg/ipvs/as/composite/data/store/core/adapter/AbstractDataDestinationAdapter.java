package de.uni.sttg.ipvs.as.composite.data.store.core.adapter;

import de.uni.sttg.ipvs.as.composite.data.store.core.adapter.interfaces.IDataDestinationAdapter;
import de.uni.sttg.ipvs.as.composite.data.store.core.model.ExecutionDescription;
import java.util.Map;
import java.util.function.Function;
import lombok.Data;

/**
 * @author Roman Bitz
 */
@Data
public abstract class AbstractDataDestinationAdapter implements IDataDestinationAdapter {

  protected Map<String, Function<ExecutionDescription, Boolean>> functionalityMap;

}
