eclipse_bin=~/apps/eclipse-3.5/eclipse
repo_name="Mylyn-Mantis connector"

$eclipse_bin -application org.eclipse.equinox.p2.metadata.generator.EclipseGenerator -updateSite . -site site.xml -metadataRepository . -metadataRepositoryName $repo_name -artifactRepository . -artifactRepositoryName $repo_name -compress -append -reusePack200files -noDefaultIUs -vmargs -Xmx256m
