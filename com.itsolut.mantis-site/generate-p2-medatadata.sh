#!/bin/sh -x
eclipse_bin=~/apps/eclipse-3.5/eclipse
repo_location=.
repo_name="Mylyn-Mantis connector"

$eclipse_bin \
	-nosplash \
	-application org.eclipse.equinox.p2.metadata.generator.EclipseGenerator \
	-updateSite $repo_location \
	-site $repo_location/site.xml \
	-metadataRepository $repo_location -metadataRepositoryName "$repo_name Update Site"\
	-artifactRepository $repo_location -artifactRepositoryName "$repo_name Artifacts"\
	-compress \
	-reusePack200files \
	-noDefaultIUs \
	-vmargs -Xmx256m 
