artifacts builderVersion: "1.1", {
  group "com.sap.bds.ats-altiscale", {
    artifact "hive", {
      file "${gendir}/src/hiverpmbuild/hive-artifact/alti-hive-${buildVersion}.rpm"
    }
  }
}