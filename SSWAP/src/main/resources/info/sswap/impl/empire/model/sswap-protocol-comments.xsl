<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet version="1.0" 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:sswap="http://sswapmeet.sswap.info/sswap/">
    
    <xsl:template match="sswap:name">
        <xsl:comment> 
        Every resource must have a name. Use something short and
        informative that can be displayed to users.  
        </xsl:comment>    
    	<xsl:copy>
      		<xsl:apply-templates select="@*|node()"/>
   		</xsl:copy>
    </xsl:template>
    
    <xsl:template match="sswap:oneLineDescription">
    	<xsl:comment>
    	Enter a one line description about this resource. Client viewers,
        such as the search engine at http://sswap.info may use this one
        line description to give users a quick description about the
        resource. For more detailed information, direct users to the
        sswap:aboutURI URL.  
        </xsl:comment>         
    	<xsl:copy>
      		<xsl:apply-templates select="@*|node()"/>
   		</xsl:copy>
    </xsl:template>
    
    <xsl:template match="sswap:aboutURI">
    	<xsl:comment> 
    	The sswap:aboutURI allows you to link this resource to the
        web for the benefit of users seeking more detailed information
        about than just a sswap:name and sswap:oneLineDescription.

        SSWAP does not stipulate how the sswap:aboutURI is to be used, but it
        is suggested that it point to an informative human-readable web page.  
        </xsl:comment>         
    	<xsl:copy>
      		<xsl:apply-templates select="@*|node()"/>
   		</xsl:copy>
    </xsl:template>
    
    <xsl:template match="sswap:metadata">
    	<xsl:comment>
    	The sswap:metadata URI allows you to associate arbitrary content
    	(e.g., plain text words) with this resource. While you have complete
    	control over what is at the URI, resources do not have control on how,
    	or even if, others use the content.
    	
    	Common use is a plain text file of words associated with the resource
    	to enhance non-semantic search and discovery.
        </xsl:comment>         
    	<xsl:copy>
      		<xsl:apply-templates select="@*|node()"/>
   		</xsl:copy>
    </xsl:template>
    
    <xsl:template match="sswap:providedBy">
    	<xsl:comment>
    	Use the sswap:providedBy predicate to identify the resource provider
    	which claims ownership of this resource.

        If a resource's URL does not parse to a sibling or sub-directory
        of its provider's URL, then the provider's sswap:providesResource
        predicate must reciprocate the assertion back to this resource.

        Each resource must have exactly one provider. Providers are always
        of type sswap:Provider and must have their own Provider Description Graph
        (PDG) dereferenceable from their URL.
        </xsl:comment>         
    	<xsl:copy>
      		<xsl:apply-templates select="@*|node()"/>
   		</xsl:copy>
    </xsl:template>
    
    <xsl:template match="sswap:inputURI">
    	<xsl:comment> 
    	Resources may require special user interfaces to gather input such as 
    	using web pages to solicit input from users. Programs that are preparing to 
    	invoke a resource may direct users to the URI pointed to by this property for 
    	the appropriate user interface.
    	
    	The sswap:inputURI is always supplemental: arbitrary web actors may always
    	invoke a SSWAP resource directly, by-passing the sswap:inputURI, by POSTing
    	a Resource Invocation Graph (RIG) directly to the resource.
    	</xsl:comment>         
    	<xsl:copy>
      		<xsl:apply-templates select="@*|node()"/>
   		</xsl:copy>
    </xsl:template>
    
    <xsl:template match="sswap:outputURI">
    	<xsl:comment>
		Resources may require special user interfaces to display output such as
		using web pages to display output to users. Programs that are handling
		a resource's response may direct users to the URI pointed to by the
		sswap:outputURI to handle the result graph.

		The sswap:outputURI is not used when pipelining.
		
		A Resource Description Graph (RDG) may declare the resource's default
		sswap:outputURI; Resource Invocation Graphs (RIGs) may supply a different
		value. The default action in cases of neither a default nor invocation
		value is to simply return the Resource Response Graph (RRG) to the caller.
    	</xsl:comment>         
    	<xsl:copy>
      		<xsl:apply-templates select="@*|node()"/>
   		</xsl:copy>
    </xsl:template>
    
    <xsl:template match="sswap:operatesOn">
    	<xsl:comment>
    	Here starts the protocol graph.

        In this template, the sequence of predicates (properties: sswap:operatesOn,
        sswap:hasMapping, and sswap:mapsTo) join nodes of types sswap:Resource,
        sswap:Graph, sswap:Subject, and sswap:Object, respectively.  It is the
        conceptual analogy of the fundamental RDF model of subject -> predicate
        -> object, but here abstracted to express that "some resource" has
        "some mapping" of a subject to an object.

        To establish an actual mapping, add predicates and restrictions to
        the sswap:Subject and sswap:Object nodes. These nodes anchor arbitrary
        OWL sub-graphs with domain-specific ontologies. The sswap:Graph node is
        for alternative mappings.

        See the protocol at http://sswap.info/protocol
        </xsl:comment>         
    	<xsl:copy>
      		<xsl:apply-templates select="@*|node()"/>
   		</xsl:copy>
    </xsl:template>
    
	<xsl:template match="@*|node()">
    	<xsl:copy>
      		<xsl:apply-templates select="@*|node()"/>
   		</xsl:copy>
	</xsl:template>
</xsl:stylesheet>
