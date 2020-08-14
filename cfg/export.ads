artifacts builderVersion: "1.1", {
  group "com.sap.bds.ats-altiscale", {
    artifact "hive", {
      file "${gendir}/src/hiverpmbuild/hive-artifact/alti-hive-${buildVersion}.rpm"
    }
  }
  group "com.sap.bds.ats-altiscale.hive", {
    artifact "hive-ant", {
      file "${gendir}/src/hiverpmbuild/hive-artifact/hive-ant-${buildVersion}.jar"
    }
    artifact "hive-common", {
      file "${gendir}/src/hiverpmbuild/hive-artifact/hive-common-${buildVersion}.jar"
    }
    artifact "hive-contrib", {
      file "${gendir}/src/hiverpmbuild/hive-artifact/hive-contrib-${buildVersion}.jar"
    }
    artifact "hive-exec", {
      file "${gendir}/src/hiverpmbuild/hive-artifact/hive-exec-${buildVersion}.jar"
    }
    artifact "hive-metastore", {
      file "${gendir}/src/hiverpmbuild/hive-artifact/hive-metastore-${buildVersion}.jar"
    }
    artifact "hive-serde", {
      file "${gendir}/src/hiverpmbuild/hive-artifact/hive-serde-${buildVersion}.jar"
    }
    artifact "hive-shims", {
      file "${gendir}/src/hiverpmbuild/hive-artifact/hive-shims-${buildVersion}.jar"
    }

  }
  group "com.sap.bds.ats-altiscale.hive.shims", {
    artifact "hive-shims-common", {
      file "${gendir}/src/hiverpmbuild/hive-artifact/hive-shims-common-${buildVersion}.jar"
    }
    artifact "hive-shims-scheduler", {
      file "${gendir}/src/hiverpmbuild/hive-artifact/hive-shims-scheduler-${buildVersion}.jar"
    }
    artifact "hive-shims-0.20S", {
      file "${gendir}/src/hiverpmbuild/hive-artifact/hive-shims-0.20S-${buildVersion}.jar"
    }
    artifact "hive-shims-0.23", {
      file "${gendir}/src/hiverpmbuild/hive-artifact/hive-shims-0.23-${buildVersion}.jar"
    }

  }
}
