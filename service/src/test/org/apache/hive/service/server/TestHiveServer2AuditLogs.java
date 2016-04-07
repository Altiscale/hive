/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hive.service.server;

import java.io.StringWriter;
import java.util.Collections;

import junit.framework.TestCase;

import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hive.service.cli.SessionHandle;
import org.apache.hive.service.cli.thrift.EmbeddedThriftBinaryCLIService;
import org.apache.hive.service.cli.thrift.ThriftCLIService;
import org.apache.hive.service.cli.thrift.ThriftCLIServiceClient;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.WriterAppender;
import org.junit.Assert;

public class TestHiveServer2AuditLogs extends TestCase {

  private EmbeddedThriftBinaryCLIService service;
  private ThriftCLIServiceClient client;

  public void setUp() throws Exception {
    service = new EmbeddedThriftBinaryCLIService();
    service.init(new HiveConf());
    client = new ThriftCLIServiceClient(service);
  }

  public void tearDown() {
    service.stop();
  }

  public void testHS2AuditLogging () throws Exception {
    String sessionUserName = "user1";

    Logger logger = Logger.getLogger(ThriftCLIService.class.getName() + ".audit");
    Level origLevel = logger.getLevel();
    logger.setLevel(Level.INFO);

    // create an appender to capture the logs in a string
    StringWriter writer = new StringWriter();
    WriterAppender appender = new WriterAppender(new PatternLayout(), writer);

    try {
      logger.addAppender(appender);

      SessionHandle sessionHandle = client.openSession(sessionUserName, "foobar",
        Collections.<String, String>emptyMap());
      client.closeSession(sessionHandle);

      String logStr = writer.toString();
      String expectedString = "OpenSession";
      Assert.assertTrue(logStr + " should contain <" + expectedString,
        logStr.contains("cmd=OpenSession"));
      Assert.assertTrue(logStr + " should contain <" + expectedString,
        logStr.contains("cmd=CloseSession"));

    } finally {
      logger.setLevel(origLevel);
      logger.removeAppender(appender);
    }
  }
}