package org.apache.hive.minikdc;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.conf.HiveConf.ConfVars;
import org.apache.hive.jdbc.TestSSL;
import org.apache.hive.jdbc.miniHS2.MiniHS2;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(Parameterized.class)
public class TestJdbWithCustomAuthHttpWithKerberos {
  private static final Logger LOG = LoggerFactory.getLogger(TestSSL.class);

  private static final String KEY_STORE_NAME = "keystore.jks";
  protected static final String TRUST_STORE_NAME = "truststore.jks";
  protected static final String KEY_STORE_PASSWORD = "HiveJdbc";
  private static final String JAVA_TRUST_STORE_PROP = "javax.net.ssl.trustStore";
  private static final String JAVA_TRUST_STORE_PASS_PROP = "javax.net.ssl.trustStorePassword";
  private MiniHiveKdc miniHiveKdc;
  protected static MiniHS2 miniHS2 = null;
  private static Connection hs2Conn = null;
  protected static HiveConf hiveConf = new HiveConf();
  private Map<String, String> confOverlay;
  protected String dataFileDir = hiveConf.get("test.data.files");
  @Parameter
  public String transportMode =  null;

  @Parameters(name = "{index}: tranportMode={0}")
  public static Collection<Object[]> transportModes() {
    return Arrays.asList(new Object[][] {
        { MiniHS2.HS2_ALL_MODE },{ MiniHS2.HS2_HTTP_MODE}
    });
  }

  protected String getJdbcUrl(boolean isSsl) {
    if (isSsl) {
      // JDBC connection with ID/PASSWD over SSL with Kerberos (Custom class)
      String url = "jdbc:hive2://" + miniHS2.getHost() + ":" + miniHS2.getHttpPort()
          + "/default;transportMode=http;httpPath=cliservice;http.header.x-http-authtype=Basic"
          + ";ssl=true;sslTrustStore=" + dataFileDir + File.separator + TRUST_STORE_NAME
          + ";trustStorePassword=" + KEY_STORE_PASSWORD;
      return url;
    } else {
      return "jdbc:hive2://" + miniHS2.getHost() + ":" + miniHS2.getHttpPort()
          + "/default;transportMode=http;httpPath=cliservice;http.header.x-http-authtype=Basic";
    }
  }

  @BeforeClass
  public static void beforeTest() throws Exception {
    Class.forName(MiniHS2.getJdbcDriverName());
  }

  @Before
  public void setUp() throws Exception {
    DriverManager.setLoginTimeout(0);
    hiveConf = new HiveConf();
    hiveConf.setVar(ConfVars.HIVE_SERVER2_TRANSPORT_MODE, transportMode);
  }

  @After
  public void tearDown() throws Exception {
    if (hs2Conn != null) {
      hs2Conn.close();
      hs2Conn = null;
    }
    if (miniHS2 != null && miniHS2.isStarted()) {
      miniHS2.stop();
      miniHS2.cleanup();
    }
    System.clearProperty(JAVA_TRUST_STORE_PROP);
    System.clearProperty(JAVA_TRUST_STORE_PASS_PROP);
  }

  @Test
  public void testKerberosAuthentication() throws Exception {
    setCustomAuthWithKrbOverlay(false);
    startMiniHS2();

    // JDBC connection to HiveServer2 with Kerberos
    hs2Conn = DriverManager.getConnection(miniHS2.getHttpJdbcURL());
  }

  @Test
  public void testCustomAuthenticationOverPlainWithKerberos() throws Exception {
    setCustomAuthWithKrbOverlay(false);
    startMiniHS2();

    // JDBC connection with ID/PASSWD without SSL with Kerberos
    String url = getJdbcUrl(false);

    // wrong ID/PASSWD
    try {
      hs2Conn = DriverManager.getConnection(url, "wronguser", "pwd");
    } catch (Exception e) {
      assertNotNull(e.getMessage());
      assertTrue(e.getMessage(), e.getMessage().contains("HTTP Response code: 401"));
    }

    // success ID/PASSWD
    hs2Conn = DriverManager.getConnection(url, "hiveuser", "hive");
  }

  @Test
  public void testCustomAuthenticationOverSslWithKerberos() throws Exception {
    setCustomAuthWithKrbOverlay(true);
    startMiniHS2();

    // JDBC connection with ID/PASSWD with SSL with Kerberos
    String url = getJdbcUrl(true);

    // wrong ID/PASSWD
    try {
      hs2Conn = DriverManager.getConnection(url, "wronguser", "pwd");
    } catch (Exception e) {
      assertNotNull(e.getMessage());
      assertTrue(e.getMessage(), e.getMessage().contains("HTTP Response code: 401"));
    }

    // success ID/PASSWD
    hs2Conn = DriverManager.getConnection(url, "hiveuser", "hive");
  }

  private void setCustomAuthWithKrbOverlay(boolean isSslUsed) {
    confOverlay = new HashMap<String, String>();
    confOverlay.put(ConfVars.HIVE_SERVER2_KERBEROS_CUSTOM_AUTH_USED.varname, "true");
    confOverlay.put(ConfVars.HIVE_SERVER2_KERBEROS_CUSTOM_AUTH_CLASS.varname,
        "org.apache.hive.minikdc.TestJdbWithCustomAuthWithKerberos$SimpleCustomAuthWithKerberosProviderImpl");
    if (isSslUsed) {
      // Custom class over SSL with Kerberos
      confOverlay.put(ConfVars.HIVE_SERVER2_KERBEROS_CUSTOM_AUTH_SSL_USED.varname, "true");
      confOverlay.put(ConfVars.HIVE_SERVER2_KERBEROS_CUSTOM_AUTH_SSL_KEYSTORE_PATH.varname,
          dataFileDir + File.separator + KEY_STORE_NAME);
      confOverlay.put(ConfVars.HIVE_SERVER2_KERBEROS_CUSTOM_AUTH_SSL_KEYSTORE_PASSWORD.varname,
          KEY_STORE_PASSWORD);
      confOverlay.put(ConfVars.HIVE_SERVER2_USE_SSL.varname, "true");
      confOverlay.put(ConfVars.HIVE_SERVER2_SSL_KEYSTORE_PATH.varname,
          dataFileDir + File.separator + KEY_STORE_NAME);
      confOverlay.put(ConfVars.HIVE_SERVER2_SSL_KEYSTORE_PASSWORD.varname,
          KEY_STORE_PASSWORD);
    } else {
      confOverlay.put(ConfVars.HIVE_SERVER2_KERBEROS_CUSTOM_AUTH_SSL_USED.varname, "false");
    }
  }

  private void startMiniHS2() {
    try {
      miniHiveKdc = MiniHiveKdc.getMiniHiveKdc(hiveConf);
      miniHS2 = MiniHiveKdc.getMiniHS2WithKerb(miniHiveKdc, hiveConf);
      miniHS2.start(confOverlay);
    } catch (Exception e) {
      e.printStackTrace();
      assertNotNull(e.getMessage());
    }
  }

}
