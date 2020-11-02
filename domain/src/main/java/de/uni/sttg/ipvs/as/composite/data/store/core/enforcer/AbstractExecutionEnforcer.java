package de.uni.sttg.ipvs.as.composite.data.store.core.enforcer;

import com.googlecode.aviator.AviatorEvaluator;
import com.googlecode.aviator.AviatorEvaluatorInstance;
import com.googlecode.aviator.Expression;
import com.googlecode.aviator.runtime.type.AviatorFunction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.casbin.jcasbin.effect.DefaultEffector;
import org.casbin.jcasbin.effect.Effect;
import org.casbin.jcasbin.effect.Effector;
import org.casbin.jcasbin.main.ManagementEnforcer;
import org.casbin.jcasbin.model.Assertion;
import org.casbin.jcasbin.model.FunctionMap;
import org.casbin.jcasbin.model.Model;
import org.casbin.jcasbin.persist.Adapter;
import org.casbin.jcasbin.persist.Watcher;
import org.casbin.jcasbin.persist.file_adapter.FileAdapter;
import org.casbin.jcasbin.rbac.DefaultRoleManager;
import org.casbin.jcasbin.rbac.RoleManager;
import org.casbin.jcasbin.util.BuiltInFunctions;
import org.casbin.jcasbin.util.Util;

/**
 * Copy of {@link org.casbin.jcasbin.main.CoreEnforcer} with a slight change to the enforce methode. Now it returns a List of matches policies rather
 * than a simple boolean.
 *
 * @author Roman Bitz
 */
public abstract class AbstractExecutionEnforcer extends ManagementEnforcer {

  private String modelPath;
  private Model model;
  private FunctionMap fm;
  private Effector eft;

  private Adapter adapter;
  private Watcher watcher;
  private RoleManager rm;

  private boolean enabled;
  private boolean autoSave;
  private boolean autoBuildRoleLinks;

  private void initialize() {
    rm = new DefaultRoleManager(10);
    eft = new DefaultEffector();
    watcher = null;

    enabled = true;
    autoSave = true;
    autoBuildRoleLinks = true;
  }

  /**
   * AbstractExecutionPolicyEnforcer is the default constructor.
   */
  AbstractExecutionEnforcer() {
    this("", "");
  }

  /**
   * AbstractExecutionPolicyEnforcer initializes an enforcer with a model file and a policy file.
   *
   * @param modelPath the path of the model file.
   * @param policyFile the path of the policy file.
   */
  private AbstractExecutionEnforcer(String modelPath, String policyFile) {
    this(modelPath, new FileAdapter(policyFile));
  }

  /**
   * AbstractExecutionPolicyEnforcer initializes an enforcer with a database adapter.
   *
   * @param modelPath the path of the model file.
   * @param adapter the adapter.
   */
  private AbstractExecutionEnforcer(String modelPath, Adapter adapter) {
    this(newModel(modelPath, ""), adapter);

    this.modelPath = modelPath;
  }

  /**
   * AbstractExecutionPolicyEnforcer initializes an enforcer with a model and a database adapter.
   *
   * @param m the model.
   * @param adapter the adapter.
   */
  private AbstractExecutionEnforcer(Model m, Adapter adapter) {
    this.adapter = adapter;
    watcher = null;

    model = m;
    model.printModel();
    fm = FunctionMap.loadFunctionMap();

    initialize();

    if (this.adapter != null) {
      loadPolicy();
    }
  }

  /**
   * AbstractExecutionPolicyEnforcer initializes an enforcer with a model.
   *
   * @param m the model.
   */
  public AbstractExecutionEnforcer(Model m) {
    this(m, null);
  }

  /**
   * AbstractExecutionPolicyEnforcer initializes an enforcer with a model file.
   *
   * @param modelPath the path of the model file.
   */
  public AbstractExecutionEnforcer(String modelPath) {
    this(modelPath, "");
  }

  /**
   * AbstractExecutionPolicyEnforcer initializes an enforcer with a model file, a policy file and an enable log flag.
   *
   * @param modelPath the path of the model file.
   * @param policyFile the path of the policy file.
   * @param enableLog whether to enable Casbin's log.
   */
  AbstractExecutionEnforcer(String modelPath, String policyFile, boolean enableLog) {
    this(modelPath, new FileAdapter(policyFile));
    enableLog(enableLog);
  }

  @Override
  public void loadPolicy() {
    model.clearPolicy();
    adapter.loadPolicy(model);

    model.printPolicy();
    if (autoBuildRoleLinks) {
      buildRoleLinks();
    }
  }

  /**
   * enforce decides whether a "subject" can access a "object" with the operation "action", input parameters are usually: (sub, obj, act).
   *
   * @param rvals the request needs to be mediated, usually an array of strings, can be class instances if ABAC is used.
   * @return List of Policies matching that DataSource.
   */
  public List<List<String>> getExecutions(Object... rvals) {

    List<List<String>> policies = new ArrayList<>();

    if (!enabled) {
      return null;
    }

    Map<String, AviatorFunction> functions = new HashMap<>();
    for (Map.Entry<String, AviatorFunction> entry : fm.fm.entrySet()) {
      String key = entry.getKey();
      AviatorFunction function = entry.getValue();

      functions.put(key, function);
    }
    if (model.model.containsKey("g")) {
      for (Map.Entry<String, Assertion> entry : model.model.get("g").entrySet()) {
        String key = entry.getKey();
        Assertion ast = entry.getValue();

        RoleManager rm = ast.rm;
        functions.put(key, BuiltInFunctions.generateGFunction(key, rm));
      }
    }
    AviatorEvaluatorInstance eval = AviatorEvaluator.newInstance();
    for (AviatorFunction f : functions.values()) {
      eval.addFunction(f);
    }

    String expString = model.model.get("m").get("m").value;
    Util.logPrint(expString);
    Expression expression = eval.compile(expString);

    Effect policyEffects[];
    float matcherResults[];
    int policyLen;
    if ((policyLen = model.model.get("p").get("p").policy.size()) != 0) {
      policyEffects = new Effect[policyLen];
      matcherResults = new float[policyLen];

      for (int i = 0; i < model.model.get("p").get("p").policy.size(); i++) {
        List<String> pvals = model.model.get("p").get("p").policy.get(i);

        Util.logPrint("Policy Rule: " + pvals);

        Map<String, Object> parameters = new HashMap<>();
        for (int j = 0; j < model.model.get("r").get("r").tokens.length; j++) {
          String token = model.model.get("r").get("r").tokens[j];
          parameters.put(token, rvals[j]);
        }
        for (int j = 0; j < model.model.get("p").get("p").tokens.length; j++) {
          String token = model.model.get("p").get("p").tokens[j];
          parameters.put(token, pvals.get(j));
        }

        Object result = expression.execute(parameters);
        Util.logPrint("Result: " + result + " for " + pvals.toString());

        if (result instanceof Boolean) {
          if (!((boolean) result)) {
            policyEffects[i] = Effect.Indeterminate;
            continue;
          } else {
            policies.add(pvals);
          }
        } else if (result instanceof Float) {
          if ((float) result == 0) {
            policyEffects[i] = Effect.Indeterminate;
            continue;
          } else {
            matcherResults[i] = (float) result;
          }
        } else {
          throw new Error("matcher result should be bool, int or float");
        }
        if (parameters.containsKey("p_eft")) {
          String eft = (String) parameters.get("p_eft");
          if (eft.equals("allow")) {
            policyEffects[i] = Effect.Allow;
          } else if (eft.equals("deny")) {
            policyEffects[i] = Effect.Deny;
          } else {
            policyEffects[i] = Effect.Indeterminate;
          }
        } else {
          policyEffects[i] = Effect.Allow;
        }

        if (model.model.get("e").get("e").value.equals("priority(p_eft) || deny")) {
          break;
        }
      }
    } else {
      policyEffects = new Effect[1];
      matcherResults = new float[1];

      Map<String, Object> parameters = new HashMap<>();
      for (int j = 0; j < model.model.get("r").get("r").tokens.length; j++) {
        String token = model.model.get("r").get("r").tokens[j];
        parameters.put(token, rvals[j]);
      }
      for (int j = 0; j < model.model.get("p").get("p").tokens.length; j++) {
        String token = model.model.get("p").get("p").tokens[j];
        parameters.put(token, "");
      }

      Object result = expression.execute(parameters);
      Util.logPrint("ex Result: " + result);

      if ((boolean) result) {
        policyEffects[0] = Effect.Allow;
      } else {
        policyEffects[0] = Effect.Indeterminate;
      }
    }

    boolean result = eft.mergeEffects(model.model.get("e").get("e").value, policyEffects, matcherResults);

    StringBuilder reqStr = new StringBuilder("Ex Request: ");
    for (int i = 0; i < rvals.length; i++) {
      String rval = rvals[i].toString();

      if (i != rvals.length - 1) {
        reqStr.append(String.format("%s, ", rval));
      } else {
        reqStr.append(String.format("%s", rval));
      }
    }
    reqStr.append(String.format(" ---> %s", result));
    Util.logPrint(reqStr.toString());

    Util.logPrint("ex Policies " + policies.toString());

    return policies;
  }


  /**
   * getRolesForUser gets the roles that a user has.
   *
   * @param name the user.
   * @return the roles that the user has.
   */
  private List<String> getRolesForUser(String name) {
    try {
      return model.model.get("g").get("g").rm.getRoles(name);
    } catch (Error e) {
      if (!e.getMessage().equals("error: name does not exist")) {
        throw e;
      }
    }
    return null;
  }

  /**
   * getUsersForRole gets the users that has a role.
   *
   * @param name the role.
   * @return the users that has the role.
   */
  public List<String> getUsersForRole(String name) {
    try {
      return model.model.get("g").get("g").rm.getUsers(name);
    } catch (Error e) {
      if (!e.getMessage().equals("error: name does not exist")) {
        throw e;
      }
    }
    return null;
  }

  /**
   * hasRoleForUser determines whether a user has a role.
   *
   * @param name the user.
   * @param role the role.
   * @return whether the user has the role.
   */
  public boolean hasRoleForUser(String name, String role) {
    List<String> roles = getRolesForUser(name);

    boolean hasRole = false;
    for (String r : roles) {
      if (r.equals(role)) {
        hasRole = true;
        break;
      }
    }

    return hasRole;
  }

  /**
   * addRoleForUser adds a role for a user. Returns false if the user already has the role (aka not affected).
   *
   * @param user the user.
   * @param role the role.
   * @return succeeds or not.
   */
  public boolean addRoleForUser(String user, String role) {
    return addGroupingPolicy(user, role);
  }

  /**
   * deleteRoleForUser deletes a role for a user. Returns false if the user does not have the role (aka not affected).
   *
   * @param user the user.
   * @param role the role.
   * @return succeeds or not.
   */
  public boolean deleteRoleForUser(String user, String role) {
    return removeGroupingPolicy(user, role);
  }

  /**
   * deleteRolesForUser deletes all roles for a user. Returns false if the user does not have any roles (aka not affected).
   *
   * @param user the user.
   * @return succeeds or not.
   */
  public boolean deleteRolesForUser(String user) {
    return removeFilteredGroupingPolicy(0, user);
  }

  /**
   * deleteUser deletes a user. Returns false if the user does not exist (aka not affected).
   *
   * @param user the user.
   * @return succeeds or not.
   */
  public boolean deleteUser(String user) {
    return removeFilteredGroupingPolicy(0, user);
  }

  /**
   * deleteRole deletes a role.
   *
   * @param role the role.
   */
  public void deleteRole(String role) {
    removeFilteredGroupingPolicy(1, role);
    removeFilteredPolicy(0, role);
  }

  /**
   * deletePermission deletes a permission. Returns false if the permission does not exist (aka not affected).
   *
   * @param permission the permission, usually be (obj, act). It is actually the rule without the subject.
   * @return succeeds or not.
   */
  private boolean deletePermission(String... permission) {
    return removeFilteredPolicy(1, permission);
  }

  /**
   * deletePermission deletes a permission. Returns false if the permission does not exist (aka not affected).
   *
   * @param permission the permission, usually be (obj, act). It is actually the rule without the subject.
   * @return succeeds or not.
   */
  public boolean deletePermission(List<String> permission) {
    return deletePermission(permission.toArray(new String[0]));
  }

  /**
   * addPermissionForUser adds a permission for a user or role. Returns false if the user or role already has the permission (aka not affected).
   *
   * @param user the user.
   * @param permission the permission, usually be (obj, act). It is actually the rule without the subject.
   * @return succeeds or not.
   */
  private boolean addPermissionForUser(String user, String... permission) {
    List<String> params = new ArrayList<>();

    params.add(user);
    Collections.addAll(params, permission);

    return addPolicy(params);
  }

  /**
   * addPermissionForUser adds a permission for a user or role. Returns false if the user or role already has the permission (aka not affected).
   *
   * @param user the user.
   * @param permission the permission, usually be (obj, act). It is actually the rule without the subject.
   * @return succeeds or not.
   */
  public boolean addPermissionForUser(String user, List<String> permission) {
    return addPermissionForUser(user, permission.toArray(new String[0]));
  }

  /**
   * deletePermissionForUser deletes a permission for a user or role. Returns false if the user or role does not have the permission (aka not
   * affected).
   *
   * @param user the user.
   * @param permission the permission, usually be (obj, act). It is actually the rule without the subject.
   * @return succeeds or not.
   */
  private boolean deletePermissionForUser(String user, String... permission) {
    List<String> params = new ArrayList<>();

    params.add(user);
    Collections.addAll(params, permission);

    return removePolicy(params);
  }

  /**
   * deletePermissionForUser deletes a permission for a user or role. Returns false if the user or role does not have the permission (aka not
   * affected).
   *
   * @param user the user.
   * @param permission the permission, usually be (obj, act). It is actually the rule without the subject.
   * @return succeeds or not.
   */
  public boolean deletePermissionForUser(String user, List<String> permission) {
    return deletePermissionForUser(user, permission.toArray(new String[0]));
  }

  /**
   * deletePermissionsForUser deletes permissions for a user or role. Returns false if the user or role does not have any permissions (aka not
   * affected).
   *
   * @param user the user.
   * @return succeeds or not.
   */
  public boolean deletePermissionsForUser(String user) {
    return removeFilteredPolicy(0, user);
  }

  /**
   * getPermissionsForUser gets permissions for a user or role.
   *
   * @param user the user.
   * @return the permissions, a permission is usually like (obj, act). It is actually the rule without the subject.
   */
  public List<List<String>> getPermissionsForUser(String user) {
    return getFilteredPolicy(0, user);
  }

  /**
   * hasPermissionForUser determines whether a user has a permission.
   *
   * @param user the user.
   * @param permission the permission, usually be (obj, act). It is actually the rule without the subject.
   * @return whether the user has the permission.
   */
  private boolean hasPermissionForUser(String user, String... permission) {
    List<String> params = new ArrayList<>();

    params.add(user);
    Collections.addAll(params, permission);

    return hasPolicy(params);
  }

  /**
   * hasPermissionForUser determines whether a user has a permission.
   *
   * @param user the user.
   * @param permission the permission, usually be (obj, act). It is actually the rule without the subject.
   * @return whether the user has the permission.
   */
  public boolean hasPermissionForUser(String user, List<String> permission) {
    return hasPermissionForUser(user, permission.toArray(new String[0]));
  }

  /**
   * getRolesForUserInDomain gets the roles that a user has inside a domain.
   *
   * @param name the user.
   * @param domain the domain.
   * @return the roles that the user has in the domain.
   */
  public List<String> getRolesForUserInDomain(String name, String domain) {
    try {
      return model.model.get("g").get("g").rm.getRoles(name, domain);
    } catch (Error e) {
      if (!e.getMessage().equals("error: name does not exist")) {
        throw e;
      }
    }
    return null;
  }

  /**
   * getPermissionsForUserInDomain gets permissions for a user or role inside a domain.
   *
   * @param user the user.
   * @param domain the domain.
   * @return the permissions, a permission is usually like (obj, act). It is actually the rule without the subject.
   */
  public List<List<String>> getPermissionsForUserInDomain(String user, String domain) {
    return getFilteredPolicy(0, user, domain);
  }

  /**
   * addRoleForUserInDomain adds a role for a user inside a domain. Returns false if the user already has the role (aka not affected).
   *
   * @param user the user.
   * @param role the role.
   * @param domain the domain.
   * @return succeeds or not.
   */
  public boolean addRoleForUserInDomain(String user, String role, String domain) {
    return addGroupingPolicy(user, role, domain);
  }


  /**
   * newModel creates a model.
   *
   * @return an empty model.
   */
  public static Model newModel() {
    Model m = new Model();

    return m;
  }

  /**
   * newModel creates a model.
   *
   * @param text the model text.
   * @return the model.
   */
  public static Model newModel(String text) {
    Model m = new Model();

    m.loadModelFromText(text);

    return m;
  }

  /**
   * newModel creates a model.
   *
   * @param modelPath the path of the model file.
   * @param unused unused parameter, just for differentiating with newModel(String text).
   * @return the model.
   */
  public static Model newModel(String modelPath, String unused) {
    Model m = new Model();

    if (!modelPath.equals("")) {
      m.loadModel(modelPath);
    }

    return m;
  }


  /**
   * loadModel reloads the model from the model CONF file. Because the policy is attached to a model, so the policy is invalidated and needs to be
   * reloaded by calling LoadPolicy().
   */
  public void loadModel() {
    model = newModel();
    model.loadModel(modelPath);
    model.printModel();
    fm = FunctionMap.loadFunctionMap();
  }

  /**
   * getModel gets the current model.
   *
   * @return the model of the enforcer.
   */
  public Model getModel() {
    return model;
  }

  /**
   * setModel sets the current model.
   *
   * @param model the model.
   */
  public void setModel(Model model) {
    this.model = model;
    fm = FunctionMap.loadFunctionMap();
  }

  /**
   * getAdapter gets the current adapter.
   *
   * @return the adapter of the enforcer.
   */
  public Adapter getAdapter() {
    return adapter;
  }

  /**
   * setAdapter sets the current adapter.
   *
   * @param adapter the adapter.
   */
  public void setAdapter(Adapter adapter) {
    this.adapter = adapter;
  }

  /**
   * setWatcher sets the current watcher.
   *
   * @param watcher the watcher.
   */
  public void setWatcher(Watcher watcher) {
    this.watcher = watcher;
    watcher.setUpdateCallback(this::loadPolicy);
  }

  /**
   * SetRoleManager sets the current role manager.
   *
   * @param rm the role manager.
   */
  public void setRoleManager(RoleManager rm) {
    this.rm = rm;
  }

  /**
   * setEffector sets the current effector.
   *
   * @param eft the effector.
   */
  public void setEffector(Effector eft) {
    this.eft = eft;
  }

  /**
   * clearPolicy clears all policy.
   */
  public void clearPolicy() {
    model.clearPolicy();
  }


  /**
   * loadFilteredPolicy reloads a filtered policy from file/database.
   *
   * @param filter the filter used to specify which type of policy should be loaded.
   */
  public void loadFilteredPolicy(Object filter) {
  }

  /**
   * isFiltered returns true if the loaded policy has been filtered.
   *
   * @return if the loaded policy has been filtered.
   */
  public boolean isFiltered() {
    return false;
  }

  /**
   * savePolicy saves the current policy (usually after changed with Casbin API) back to file/database.
   */
  public void savePolicy() {
    if (isFiltered()) {
      throw new Error("cannot save a filtered policy");
    }

    adapter.savePolicy(model);
    if (watcher != null) {
      watcher.update();
    }
  }

  /**
   * enableEnforce changes the enforcing state of Casbin, when Casbin is disabled, all access will be allowed by the enforce() function.
   *
   * @param enable whether to enable the enforcer.
   */
  public void enableEnforce(boolean enable) {
    enabled = enable;
  }

  /**
   * enableLog changes whether to print Casbin log to the standard output.
   *
   * @param enable whether to enable Casbin's log.
   */
  public void enableLog(boolean enable) {
    Util.enableLog = enable;
  }

  /**
   * enableAutoSave controls whether to save a policy rule automatically to the adapter when it is added or removed.
   *
   * @param autoSave whether to enable the AutoSave feature.
   */
  public void enableAutoSave(boolean autoSave) {
    this.autoSave = autoSave;
  }

  /**
   * enableAutoBuildRoleLinks controls whether to save a policy rule automatically to the adapter when it is added or removed.
   *
   * @param autoBuildRoleLinks whether to automatically build the role links.
   */
  public void enableAutoBuildRoleLinks(boolean autoBuildRoleLinks) {
    this.autoBuildRoleLinks = autoBuildRoleLinks;
  }

  /**
   * buildRoleLinks manually rebuild the role inheritance relations.
   */
  public void buildRoleLinks() {
    rm.clear();
    model.buildRoleLinks(rm);
  }


}
