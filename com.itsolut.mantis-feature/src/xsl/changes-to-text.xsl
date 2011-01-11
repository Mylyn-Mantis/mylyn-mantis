<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:c="http://maven.apache.org/changes/1.0.0" exclude-result-prefixes="c">
	<xsl:template match="/">
		<xsl:for-each select="c:document/c:body/c:release">
Version <xsl:value-of select="@version"/> released

Mylyn-Mantis Connector - <xsl:value-of select="@version"/>
==============================

<xsl:for-each select="c:action">- <xsl:value-of select="@issue"/>: <xsl:value-of select="."/> ( <xsl:value-of select="@dev"/> )
</xsl:for-each>
		</xsl:for-each>
	</xsl:template>
</xsl:stylesheet>