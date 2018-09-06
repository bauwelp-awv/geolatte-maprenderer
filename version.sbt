val snapshotSuffix = "SNAPSHOT"

val base_version = "1.0.3"

version in ThisBuild := base_version + "-" + sys.props.getOrElse("bamboo_buildNumber", snapshotSuffix)

isSnapshot := version.value.endsWith(snapshotSuffix)
