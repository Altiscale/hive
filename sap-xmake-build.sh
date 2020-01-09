 #!/bin/bash -l

# find this script and establish base directory
SCRIPT_DIR="$( dirname "${BASH_SOURCE[0]}" )"
cd "$SCRIPT_DIR" &> /dev/null
MY_DIR="$(pwd)"
echo "[INFO] Executing in ${MY_DIR}"

# PATH does not contain ant in this login shell
export M2_HOME=/opt/mvn3
export JAVA_HOME=/opt/sapjvm_7
export FORREST_HOME=/opt/apache-forrest
export PATH=$M2_HOME/bin:$JAVA_HOME/bin:/opt/apache-ant/bin:$PATH
export MAVEN_OPTS="-Xmx3000m"


#------------------------------------------------------------------------------
#
#  ***** compile and package hive *****
#
#------------------------------------------------------------------------------

#HIVE_VERSION="${HIVE_VERSION:-${XMAKE_PROJECT_VERSION}}"
HIVE_VERSION="2.3.3"
export ARTIFACT_VERSION="$HIVE_VERSION"

env
MVN_SKIPTESTS_BOOL=${SKIPTESTS_BOOL:-false}
if [ "${MVN_SKIPTESTS_BOOL}" != "true" ] ; then
    MVN_SKIPTESTS_BOOL=false
fi

MVN_IGNORE_TESTFAILURES_BOOL=${IGNORE_TESTFAILURES_BOOL:-false}
if [ "${MVN_IGNORE_TESTFAILURES_BOOL}" != "true" ] ; then
    MVN_IGNORE_TESTFAILURES_BOOL=false
fi

# Build hiv
mvn clean install -Pdist \
-Dmaven.test.skip=true -DskipTests=true \
-Dmaven.test.failure.ignore=${MVN_IGNORE_TESTFAILURES_BOOL} -DtestFailureIgnore=${MVN_IGNORE_TESTFAILURES_BOOL} \
-DcreateChecksum=true -Dmaven.javadoc.skip=true -Dmaven-javadoc-plugin=false \
${JUSTBUILD_EXTRA_OPTS}


#------------------------------------------------------------------------------
#
#  ***** setup the environment generating RPM via fpm *****
#
#------------------------------------------------------------------------------

ALTISCALE_RELEASE="${ALTISCALE_RELEASE:-0.1.0}"
DATE_STRING=`date +%Y%m%d%H%M%S`
GIT_REPO="https://github.com/Altiscale/hive"

INSTALL_DIR="$MY_DIR/hiverpmbuild"
mkdir --mode=0755 -p ${INSTALL_DIR}

# deal with the hive artifacts to create a tarball ARTIFACT_VERSION is supplied by the ruby wrapper
env
ALTISCALE_RELEASE=${ALTISCALE_RELEASE:-0.1.0}
#HIVE_VERSION=${ARTIFACT_VERSION:-0.11.0}
RPM_DESCRIPTION="Apache Hive ${HIVE_VERSION}\n\n${DESCRIPTION}"
RPM_DIR="${RPM_DIR:-"${INSTALL_DIR}/hive-artifact/"}"
mkdir --mode=0755 -p ${RPM_DIR}

#convert each tarball into an RPM
DEST_ROOT=${INSTALL_DIR}/opt
mkdir --mode=0755 -p ${DEST_ROOT}
cd ${DEST_ROOT}
tar -xvzpf ${MY_DIR}/packaging/target/apache-hive-${HIVE_VERSION}-bin.tar.gz
tar -xvzpf ${MY_DIR}/packaging/target/apache-hive-${HIVE_VERSION}-src.tar.gz
mv apache-hive-${HIVE_VERSION}-bin hive-${HIVE_VERSION}
mv apache-hive-${HIVE_VERSION}-src hive-${HIVE_VERSION}/src

mkdir --mode=0755 -p ${INSTALL_DIR}/etc
mv ${INSTALL_DIR}/opt/hive-${ARTIFACT_VERSION}/conf ${INSTALL_DIR}/etc/hive-${ARTIFACT_VERSION}
cd ${INSTALL_DIR}/opt/hive-${ARTIFACT_VERSION}
cp ${INSTALL_DIR}/etc/hive-${ARTIFACT_VERSION} conf

# Add init.d scripts and sysconfig
mkdir --mode=0755 -p ${INSTALL_DIR}/etc/rc.d/init.d
cp ${MY_DIR}/etc/init.d/* ${INSTALL_DIR}/etc/rc.d/init.d
mkdir --mode=0755 -p ${INSTALL_DIR}/etc/sysconfig
cp ${MY_DIR}/etc/sysconfig/* ${INSTALL_DIR}/etc/sysconfig

# convert all the etc files to config files
cd ${INSTALL_DIR}
export CONFIG_FILES=""
find etc -type f -print | awk '{print "/" $1}' > /tmp/$$.files
for i in `cat /tmp/$$.files`; do CONFIG_FILES="--config-files $i $CONFIG_FILES "; done
export CONFIG_FILES 
rm -f /tmp/$$.files

cd ${RPM_DIR}

export RPM_NAME="alti-${PACKAGES}-${HIVE_VERSION}"
echo "TESTE:"
fpm

fpm --verbose \
--maintainer support@altiscale.com \
--vendor Altiscale \
--provides ${RPM_NAME} \
--description "$(printf "${RPM_DESCRIPTION}")" \
--url ${GITREPO} \
--license "Apache License v2" \
-s dir \
-t rpm \
-n ${RPM_NAME} \
-v ${ALTISCALE_RELEASE} \
--iteration ${DATE_STRING} \
${CONFIG_FILES} \
--rpm-user root \
--rpm-group root \
-C ${INSTALL_DIR} \
opt etc

mv "${RPM_DIR}${RPM_NAME}-${ALTISCALE_RELEASE}-${DATE_STRING}.x86_64.rpm" "${RPM_DIR}alti-hive-${XMAKE_PROJECT_VERSION}.rpm"

exit 0