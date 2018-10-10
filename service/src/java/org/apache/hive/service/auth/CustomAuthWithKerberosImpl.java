package org.apache.hive.service.auth;

import java.lang.reflect.InvocationTargetException;

import javax.security.sasl.AuthenticationException;

import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.thrift.KrbCustomAuthenticationProvider;
import org.apache.hadoop.util.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomAuthWithKerberosImpl implements PasswdAuthenticationProvider {
  private final KrbCustomAuthenticationProvider customProvider;
  public static final Logger LOG = LoggerFactory.getLogger(CustomAuthWithKerberosImpl.class.getName());

  @SuppressWarnings("unchecked")
  CustomAuthWithKerberosImpl(HiveConf conf) {
    Class<? extends KrbCustomAuthenticationProvider> customHandlerClass =
        (Class<? extends KrbCustomAuthenticationProvider>) conf.getClass(
            HiveConf.ConfVars.HIVE_SERVER2_KERBEROS_CUSTOM_AUTH_CLASS.varname,
            KrbCustomAuthenticationProvider.class);
    KrbCustomAuthenticationProvider customProvider = null;
    try {
      customProvider = customHandlerClass.getConstructor(HiveConf.class).newInstance(conf);
    } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
      try {
        customProvider = ReflectionUtils.newInstance(customHandlerClass, conf);
      } catch (Exception error) {
        LOG.error("Error fetching instance of custom kerberos auth instance", error);
      }
    }
    this.customProvider = customProvider;
  }

  @Override
  public void Authenticate(String user, String password) throws AuthenticationException {
    customProvider.authenticate(user, password);
  }
}
