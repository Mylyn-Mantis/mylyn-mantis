#!/bin/sh -x

repo_name="Mylyn-Mantis"
repository=/home/robert/workspaces/mylyn-mantis/com.itsolut.mantis-site

java -jar ~/apps/eclipse-3.5/plugins/org.eclipse.equinox.launcher_1.0.200.v20090520.jar \
	-application org.eclipse.equinox.p2.publisher.UpdateSitePublisher \
	-artifactRepository file:$repository \
	-metadataRepository file:$repository \
	-artifactRepositoryName "$repo_name Artifacts" \
	-metadataRepositoryName "$repo_name Metadata" \
	-source . \
	-configs gtk.linux.x86 \
	-publishArtifacts \
	-append 
