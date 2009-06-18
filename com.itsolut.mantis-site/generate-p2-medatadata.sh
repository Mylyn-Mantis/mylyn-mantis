#!/bin/sh -x

repo_name="Mylyn Mantis Connector"
repository=/home/robert/workspaces/mylyn-mantis/com.itsolut.mantis-site
target_dir=repo

java -jar ~/apps/eclipse/plugins/org.eclipse.equinox.launcher_1.0.200.v20090520.jar \
	-application org.eclipse.equinox.p2.publisher.UpdateSitePublisher \
	-artifactRepository file:$repository/$target_dir \
	-metadataRepository file:$repository/$target_dir \
	-artifactRepositoryName "$repo_name Artifacts" \
	-metadataRepositoryName "$repo_name Update Site" \
	-source . \
	-configs gtk.linux.x86 \
	-publishArtifacts \
	-append \
	-compress

cp -f site.xml $target_dir/

cp -f $target_dir/{artifacts,content}.jar .
