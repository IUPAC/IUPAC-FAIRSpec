<?xml version="1.0" encoding="ISO-8859-1" ?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output method="html" version="1.0" encoding="UTF-8"
		indent="yes" />

	<xsl:param name="quote">
		"
	</xsl:param>

	<xsl:template match="/">
		<xsl:element name="html">
			<xsl:element name="head">
				<xsl:element name="title">
					<xsl:value-of
						select="//eadheader/filedesc/titlestmt/titleproper" />
					Finding Aids : IUPAC FAIRSpec prototype only
				</xsl:element>


				<xsl:element name="meta">
					<xsl:attribute name="name">dc.title</xsl:attribute>
					<xsl:attribute name="content">
                        <xsl:value-of
						select="ead/eadheader/filedesc/titlestmt/titleproper" />
                        <xsl:text>  </xsl:text>
                        <xsl:value-of
						select="ead/eadheader/filedesc/titlestmt/subtitle" />
                    </xsl:attribute>
				</xsl:element>

				<xsl:element name="meta">
					<xsl:attribute name="name">dc.author</xsl:attribute>
					<xsl:attribute name="content">
                        <xsl:value-of
						select="ead/archdesc/did/origination" />
                    </xsl:attribute>
				</xsl:element>

				<xsl:for-each
					select="ead//controlaccess/persname | ead//controlaccess/famname  | ead//controlaccess/corpname | ead//controlaccess/title">
					<xsl:choose>
						<xsl:when test="@encodinganalog='600'">
							<xsl:element name="meta">
								<xsl:attribute name="name">dc.subject</xsl:attribute>
								<xsl:attribute name="content">
                                    <xsl:value-of
									select="." />
                                </xsl:attribute>
							</xsl:element>
						</xsl:when>
						<xsl:when test="@encodinganalog='610'">
							<xsl:element name="meta">
								<xsl:attribute name="name">dc.subject</xsl:attribute>
								<xsl:attribute name="content">
                                    <xsl:value-of
									select="." />
                                </xsl:attribute>
							</xsl:element>
						</xsl:when>
						<xsl:when test="@encodinganalog='611'">
							<xsl:element name="meta">
								<xsl:attribute name="name">dc.subject</xsl:attribute>
								<xsl:attribute name="content">
                                    <xsl:value-of
									select="." />
                                </xsl:attribute>
							</xsl:element>
						</xsl:when>

						<xsl:when test="@encodinganalog='630'">
							<xsl:element name="meta">
								<xsl:attribute name="name">dc.subject</xsl:attribute>
								<xsl:attribute name="content">
                                    <xsl:value-of
									select="." />
                                </xsl:attribute>
							</xsl:element>
						</xsl:when>

						<xsl:when test="@encodinganalog='700'">
							<xsl:element name="meta">
								<xsl:attribute name="name">dc.contributor</xsl:attribute>
								<xsl:attribute name="content">
                                    <xsl:value-of
									select="." />
                                </xsl:attribute>
							</xsl:element>
						</xsl:when>

						<xsl:when test="@encodinganalog='710'">
							<xsl:element name="meta">
								<xsl:attribute name="name">dc.contributor</xsl:attribute>
								<xsl:attribute name="content">
                                    <xsl:value-of
									select="." />
                                </xsl:attribute>
							</xsl:element>
						</xsl:when>

						<xsl:when test="@encodinganalog='711'">
							<xsl:element name="meta">
								<xsl:attribute name="name">dc.contributor</xsl:attribute>
								<xsl:attribute name="content">
                                    <xsl:value-of
									select="." />
                                </xsl:attribute>
							</xsl:element>
						</xsl:when>

						<xsl:when test="@encodinganalog='730'">
							<xsl:element name="meta">
								<xsl:attribute name="name">dc.title</xsl:attribute>
								<xsl:attribute name="content">
                                    <xsl:value-of
									select="." />
                                </xsl:attribute>
							</xsl:element>
						</xsl:when>

						<xsl:when test="@encodinganalog='740'">
							<xsl:element name="meta">
								<xsl:attribute name="name">dc.title</xsl:attribute>
								<xsl:attribute name="content">
                                    <xsl:value-of
									select="." />
                                </xsl:attribute>
							</xsl:element>
						</xsl:when>
					</xsl:choose>
				</xsl:for-each>

				<xsl:for-each select="ead//controlaccess/subject">
					<xsl:element name="meta">
						<xsl:attribute name="name">dc.subject</xsl:attribute>
						<xsl:attribute name="content">
                            <xsl:value-of select="." />
                        </xsl:attribute>
					</xsl:element>
				</xsl:for-each>

				<xsl:element name="meta">
					<xsl:attribute name="name">dc.title</xsl:attribute>
					<xsl:attribute name="content">
                        <xsl:value-of
						select="ead/archdesc/did/unittitle" />
                    </xsl:attribute>
				</xsl:element>

				<xsl:element name="meta">
					<xsl:attribute name="name">dc.type</xsl:attribute>
					<xsl:attribute name="content">text</xsl:attribute>
				</xsl:element>

				<xsl:element name="meta">
					<xsl:attribute name="name">dc.format</xsl:attribute>
					<xsl:attribute name="content">manuscripts</xsl:attribute>
				</xsl:element>

				<xsl:element name="meta">
					<xsl:attribute name="name">dc.format</xsl:attribute>
					<xsl:attribute name="content">finding aids</xsl:attribute>
				</xsl:element>

				<xsl:for-each select="ead//controlaccess/geogname">
					<xsl:element name="meta">
						<xsl:attribute name="name">dc.coverage</xsl:attribute>
						<xsl:attribute name="content">
                            <xsl:value-of select="." />
                        </xsl:attribute>
					</xsl:element>
				</xsl:for-each>
				<style type="text/css" media="screen" title="Default">
					@import
					url("assets/mnhs_global_text.css");
					@import
					url("assets/mnhs_library.css");
					@import
					url("assets/mnhs_findingaids.css");
				</style>

				<!--SOLR Search Stylesheet -->
				<link
					href="assets/solr-search-standard.css"
					type="text/css" media="screen" />

				<!-- EAD Print Stylesheet -->
				<link href="assets/eadprint.css" rel="stylesheet" media="print"
					type="text/css" />

				<!-- ========== MHS JAVASCRIPT ========== -->

				<script type="text/javascript"
					src="assets/jquery-1.5.min.js"></script>
				<script type="text/javascript"
					src="assets/jquery.hoverIntent.min.js"></script>
				<script type="text/javascript"
					src="assets/jquery.client.min.js"></script>
				<script type="text/javascript"
					src="assets/jquery.topzindex.min.js"></script>
				<!--<script type="text/javascript" src="http://www.mnhs.org/web_assets/js/jquery.mhs_dropnav.js"></script>
				<script
					src="http://legacy.mnhs.org/sites/all/includes/ismobile.php"
					type="text/javascript"></script>
					 -->
				<!-- ========== END MHS JAVASCRIPT ========== -->


				<!--#include virtual="/include/mhs-header-scripts.inc" -->


				<!--<link rel="stylesheet" href="http://www.mnhs.org/web_assets/stylesheets/dropnav/drop-navigation.css" 
					type="text/css" media="screen" /> -->
				
				<!--  BH	
					
				<link rel="stylesheet"
					href="http://www.mnhs.org/web_assets/stylesheets/mega/css/reset.css"
					type="text/css" media="screen" />   -->
				<!-- Reset -->
				<!--  BH <link rel="stylesheet"  href="http://www.mnhs.org/web_assets/stylesheets/mega/menu.css"
					type="text/css" media="screen" />   -->
					
				<!-- Menu -->
				<!--[if IE 6]> <link rel="stylesheet" href="http://www.mnhs.org/web_assets/stylesheets/mega/ie/ie6.css" 
					type="text/css" media="screen" /> <![endif] -->
				<!--[if IE]> <![endif] -->

				<style>
					#nav {
					width:100%!important;}
					#nav ol li {font-size:10px;}
					#nav
					ol li a {
					padding:6px 4px 8px 5px;}
				</style>


			</xsl:element>

			<!-- Inserts logo and title at the top of the display. -->

			<body id="findingaids">
			
				<script>
				var dispClosed = [];
				
				</script>
				<a name="top" />
				<a name="a0" />
				<div align="center">
					<div id="border">
						<!-- PAGE CONTENT -->
						<a name="content" />

						<div id="contentWrapper">
							<div id="content" align="left">

								<xsl:apply-templates />

							</div>
						</div> <!-- endcontentWrapper -->
						<!-- END PAGE CONTENT -->

					</div> <!-- end border -->
				</div> <!-- end center -->

				<!-- BEGIN COLLAPSIBLE FUNCTION -->
				<script language="JavaScript" type="text/javascript">
					//by Adrienne MacKay and		Stephanie Adamson
					//Carolina Digital Libary and Archives (CDLA)
					//define an array identifying all expandible/collapsible div elements by ID
					var divs = document.getElementsByTagName('div');
					var
					ids = new Array();
					var n = 0;
					for (var i = 0; i &lt; divs.length;
					i++) {
					//conditional statement limits divs in array to those with class 'showhide'
					//change 'showhide' to whatever class identifies expandable/collapsible divs.
					if (divs[i].className == 'showhide') {
					ids[n] = divs[i].id;
					n++;
					}
					}

					function expandAll() {
					//loop through the array and expand each element by ID
					for (var i = 0; i &lt;
					ids.length; i++) {
					var ex_obj =
					document.getElementById(ids[i]).style;
					ex_obj.display =
					ex_obj.display == "none"? "block": "block";
					//swap corresponding			arrow image to reflect change
					var arrow =
					document.getElementById("x" +(ids[i]));
					arrow.src =
					"assets/DownArrowBlue.png";
					}
					}

					function collapseAll() {
					//loop through	the array and collapse each element by ID
					for (var i = 0; i &lt;
					ids.length; i++) {
					var col_obj =
					document.getElementById(ids[i]).style;
					col_obj.display =
					col_obj.display == "block"? "none": "none";
					//swap corresponding	arrow image to reflect change
					var arrow =
					document.getElementById("x" +(ids[i]));
					arrow.src =
					"assets/RightArrowBlue.png";
					}
					}

					//Adrienne's original show/hide script for individual div toggling
					//by Adrienne MacKay, adapted 10/2008 by Stephanie Adamson to include arrow image swapping
					function changeDisplay(obj_name) {
					var my_obj =
					document.getElementById(obj_name);
					var arrow =
					document.getElementById("x" + obj_name);
					if (my_obj.style.display ==
					"none") {
					my_obj.style.display = "block";
					arrow.src =
					"assets/DownArrowBlue.png";
					} else {
					my_obj.style.display = "none";
					arrow.src = "assets/RightArrowBlue.png";
					}
					};</script>
				<!-- END COLLAPSIBLE FUNCTION -->
			</body>
			
		<script> 
		for (var i in dispClosed) {
		changeDisplay(dispClosed[i]);
		}
		</script>
		</xsl:element>
	</xsl:template>


	<!-- =============== BEGIN EAD TRANSFORMATION =============== -->
	<!-- Revised: January 24, 2019 - Changed link to Search Library & Archives 
		catalog. Added switch test in <descgrp> to add Availability note when no 
		access restrictions are present. -->
	<!-- Revised: Monica Manny Ralston, December 26, 2018 - Added boilerplate 
		language to <controlaccess><p> Section 7 with link to library catalog and 
		commented out the transformation of other paragraphs in that same section -->
	<!-- Revised: Monica Manny Ralston, June 15, 2015 - Revised search urls 
		to new MNHS search urls -->
	<!-- Revised: Monica Manny Ralston, June 17, 2013 - Revised GRN seach box 
		form -->
	<!-- Revised: Monica Manny Ralston, March 8, 2013 - Added physdesc output 
		to c02@level=subseries -->
	<!-- Revised: Monica Manny Ralston, March 7, 2012 - Changed valign="top" 
		to "bottom" for all component unittitles so that long physlocs would not 
		add white space between the unititle and following elements -->
	<!-- Revised: Monica Manny Ralston, October 28, 2011: Added class="eadsubheading" 
		to <seriesstmt><p> for holding type print purposes -->
	<!-- Revised: Monica Manny Ralston, September 13, 2011: Added class="hiddenprint" 
		to mhs_signature.gif due unresolved printing problem in Win7 where Logo prints 
		out on a separate paage -->
	<!-- Revised: Monica Manny Ralston, April 14-22, 2011: Added chronologies 
		to scope note, added link to New Finding Aids page, and corrected rendering 
		of superscripts -->
	<!-- Revised: Monica Manny Ralston, February 18, 2011: Changed color in 
		container template from #3a6894 to #666666 -->
	<!-- Revised: Monica Manny Ralston, February 8, 2011: Added <phystech> to 
		<descgrp> -->
	<!-- Revised: Monica Manny Ralston, June 14, 2010: Moved sections 5 (Related 
		Material), 5a (Separated Material), and 7 (Catalog Headings) to follow section 
		9 (Detailed Description); Added call to chronlist template for c01-complex 
		bioghist; Added call to EADtable template for c01-complex scopecontent; Added 
		<accruals> <appraisal> and <phystech> to components; Moved <materialsspec> 
		into component-did template. -->
	<!-- Revised: Monica Manny Ralston, April 9, 2010: Added <seriesstmt> -->
	<!-- Revised: Monica Manny Ralston, August 18, 2009: Added <otherfindaid> 
		to components -->
	<!-- Revised: Monica Manny Ralston, July 28, 2009: Added <originalsloc> 
		to components; revised <daoloc> for links without thumbnails -->
	<!-- Revised: Monica Manny Ralston, June 24, 2009: head for notes in c01 
		and c02 -->
	<!-- Revised: Monica Manny Ralston, March 1, 2009: Styles for class="eadtable" 
		and class-"dsctable"; <dao> -->
	<!-- Revised: Monica Manny Ralston, February 24, 2009: <extptr>, collapsible 
		divisions, and style (colors, font-sizes) -->
	<!-- Revised: January 12, 2009 -->
	<!-- Revised: Monica Manny Ralston, Minnesota Historical Society, St. Paul, 
		October 2008: Flat, complex and simple table styles -->
	<!-- Drafted: Joyce Chapman, University of North Carolina, Chapel Hill, 
		July-August 15, 2008 -->
	<!-- Version 1: Michael Fox, Minnesota Historical Society, 2000 -->


	<xsl:template match="ead">
		<xsl:apply-templates />
	</xsl:template>

	<!-- ========== EAD header ========== -->

	<xsl:template match="eadheader">
		<xsl:apply-templates />
	</xsl:template>
	<xsl:template match="eadid" />
	<xsl:template match="filedesc">
		<xsl:apply-templates />
	</xsl:template>
	<xsl:template match="notestmt" />
	<xsl:template match="publicationstmt" />
	<xsl:template match="sponsor" />
	<!-- xsl:template match="notestmt"/> -->
	<xsl:template match="profiledesc" />
	<xsl:template match="revisiondesc" />


	<xsl:template match="titlestmt">

		<h1>
			<center>
				<xsl:value-of
					select="translate(titleproper,'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ')" />
				<xsl:text>&#x20;</xsl:text>
			</center>
		</h1>
		<p class="eadsubheading">
			<xsl:value-of select="subtitle" />
		</p>

	</xsl:template>

	<xsl:template match="seriesstmt">
		<p class="eadsubheading" style="color:#404040;">
			<xsl:value-of select="p" />
		</p>
	</xsl:template>


	<!-- ========== TEST for RESTRICTION STATEMENT ========== -->
	<xsl:template match="archdesc">
		<xsl:if test="descgrp/accessrestrict | descgrp/userestrict">
			<center>
				<p class="restriction">
					Part or all of this collection is restricted.
					<br />
					For details, please see the
					<a href="#a8">restrictions</a>
					.
				</p>
			</center>
			<br />
		</xsl:if>
		<p />


		<!-- ========== ORDER OF SECTIONS ========== -->

		<!-- Used to order display of inventory sections and their anchored sidebar 
			navigation buttons. Changed 2008: <relatedmaterial> and <controlaccess> were 
			moved to display below the <descgrp> -->
		<!-- anchor 1 <did> -->
		<!-- anchor 2 <bioghist> -->
		<!-- anchor 3 <scopecontent> -->
		<!-- anchor 4 <arrangement> -->
		<!-- anchor 6 <otherfindaids> -->
		<!-- anchor 8 <descgrp> -->
		<!-- anchor 9 <dsc> -->
		<!-- anchor 5 <relatedmaterial> -->
		<!-- anchor 5a <separatedmaterial> -->
		<!-- anchor 7 <controlaccess> -->


		<xsl:apply-templates select="did" />
		<xsl:apply-templates select="bioghist" />
		<xsl:apply-templates select="scopecontent" />
		<xsl:apply-templates select="arrangement" />
		<xsl:apply-templates select="otherfindaid" />
		<xsl:apply-templates select="descgrp" />
		<xsl:apply-templates select="dsc" />
		<xsl:apply-templates select="relatedmaterial" />
		<xsl:apply-templates select="separatedmaterial" />
		<xsl:apply-templates select="controlaccess" />
	</xsl:template>

	<!-- ========== RENDER FONT ========== -->

	<xsl:template match="*[@altrender='subhead']">
		<strong style="color:#3a6894">
			<xsl:value-of
				select="translate(.,'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ')" />
		</strong>
	</xsl:template>

	<xsl:template match="*[@render='bold']">
		<strong style="color:#404040;">
			<xsl:value-of select="." />
		</strong>
	</xsl:template>

	<xsl:template match="*[@render='italic']">
		<em>
			<xsl:value-of select="." />
		</em>
	</xsl:template>

	<xsl:template match="*[@render='underline']">
		<u>
			<xsl:value-of select="." />
		</u>
	</xsl:template>

	<xsl:template match="*[@render='sub']">
		<sub>
			<xsl:value-of select="." />
		</sub>
	</xsl:template>

	<xsl:template match="*[@render='super']">
		<sup>
			<xsl:value-of select="." />
		</sup>
	</xsl:template>

	<xsl:template match="*[@render='quoted']">
		<xsl:text>"</xsl:text>
		<xsl:value-of select="." />
		<xsl:text>"</xsl:text>
	</xsl:template>

	<xsl:template match="*[@render='boldquoted']">
		<strong style="color:#404040;">
			<xsl:text>"</xsl:text>
			<xsl:value-of select="." />
			<xsl:text>"</xsl:text>
		</strong>
	</xsl:template>

	<xsl:template match="*[@render='boldunderline']">
		<strong style="color:#404040;">
			<u>
				<xsl:value-of select="." />
			</u>
		</strong>
	</xsl:template>

	<xsl:template match="*[@render='bolditalic']">
		<strong style="color:#404040;">
			<em>
				<xsl:value-of select="." />
			</em>
		</strong>
	</xsl:template>

	<xsl:template match="*[@render='boldsmcaps']">
		<font style="font-variant: small-caps">
			<strong style="color:#404040;">
				<xsl:value-of select="." />
			</strong>
		</font>
	</xsl:template>

	<xsl:template match="//title[@render='smcaps']">
		<small>
			<xsl:value-of select="." />
		</small>
	</xsl:template>

	<xsl:template match="title">
		<em>
			<xsl:value-of select="." />
		</em>
	</xsl:template>


	<!-- ========== INTERNAL LINKS TO INVENTORY SECTIONS <ref> ========== -->

	<xsl:template match="ead/archdesc//ref">
		<xsl:variable name="linktarget">
			<xsl:value-of select="@target" />
		</xsl:variable>
		<a href="#{$linktarget}">
			<xsl:value-of select="." />
		</a>
	</xsl:template>

	<xsl:template match="*[@id]">
		<a name="{@id}" />
		<xsl:value-of select="." />
	</xsl:template>


	<!-- ========== EXTERNAL LINKS <extref> ========== -->

	<xsl:template match="*/extref">
		<xsl:choose>
			<xsl:when test="@show='new'">
				<a target="_blank" href="{@href}">
					<xsl:apply-templates />
				</a>
			</xsl:when>
			<xsl:otherwise>
				<a href="{@href}">
					<xsl:apply-templates />
				</a>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<!-- ========== EMBEDDED IMAGES <extptr> ========== -->

	<xsl:template match="*/extptr">
		<img class="extptr" src="{@href}" align="{@altrender}" border="0"
			alt="{@title}">
			<xsl:apply-templates />
		</img>
	</xsl:template>

	<!-- ===== Digital Archival Objects <daodesc> ===== -->

	<!-- Monica, Feb. 28, 2009: Added for Digital Archival Objects: -->

	<xsl:template match="*/daogrp">

		<xsl:element name="a">
			<xsl:attribute name="class">DigitalArchivalObject</xsl:attribute>
			<xsl:attribute name="target">_blank</xsl:attribute>
			<xsl:attribute name="href">
                <xsl:for-each
				select="daoloc[@role='reference']">
                    <xsl:apply-templates
				select="@href"></xsl:apply-templates>
                </xsl:for-each>
            </xsl:attribute>

			<xsl:for-each select="daoloc[@role='thumbnail']">
				<xsl:element name="img">
					<xsl:attribute name="src">
                        <xsl:apply-templates
						select="@href"></xsl:apply-templates>
                    </xsl:attribute>
					<xsl:attribute name="alt">
                        <xsl:apply-templates
						select="@title"></xsl:apply-templates>
                    </xsl:attribute>
					<xsl:attribute name="align">
                        <xsl:apply-templates
						select="@altrender"></xsl:apply-templates>
                    </xsl:attribute>
					<xsl:attribute name="border">0</xsl:attribute>
					<xsl:attribute name="hspace">10</xsl:attribute>
				</xsl:element>
			</xsl:for-each>
			<xsl:apply-templates select="daodesc"></xsl:apply-templates>
		</xsl:element>

		<br clear="all" /> <!-- Clear all to force breaks between embedded image and following table 
			row -->

	</xsl:template>


	<!-- ========== GENERIC TABLES ========== -->

	<xsl:template name="EADtable">
		<xsl:for-each select="table/tgroup">
			<div id="EADtable">
				<table>
					<xsl:for-each select="thead/row/entry[@colname]">
						<tr>
							<td valign="top">
								<xsl:choose>
									<xsl:when test="*//emph[@render='bold']">
										<strong style="color:#3a6894">
											<xsl:value-of
												select="translate(entry[@colname='tablehead'],'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ')" />
										</strong>
									</xsl:when>
									<xsl:otherwise>
										<xsl:value-of
											select="translate(entry[@colname='tablehead'],'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ')" />
									</xsl:otherwise>
								</xsl:choose>
							</td>
						</tr>
					</xsl:for-each>
					<xsl:for-each select="tbody">


						<xsl:for-each select="row">
							<tr>
								<td valign="top">
									<xsl:choose>
										<xsl:when test="*//emph[@render='bold']">
											<strong style="color:#404040">
												<xsl:value-of select="entry[@colname='1']" />
											</strong>
										</xsl:when>
										<xsl:otherwise>
											<xsl:value-of select="entry[@colname='1']" />
										</xsl:otherwise>
									</xsl:choose>
								</td>
								<td valign="top">
									<xsl:choose>
										<xsl:when test="*//emph[@render='bold']">
											<strong style="color:#404040">
												<xsl:value-of select="entry[@colname='2']" />
											</strong>
										</xsl:when>
										<xsl:otherwise>
											<xsl:value-of select="entry[@colname='2']" />
										</xsl:otherwise>
									</xsl:choose>
								</td>
								<td valign="top">
									<xsl:choose>
										<xsl:when test="*//emph[@render='bold']">
											<strong style="color:#404040">
												<xsl:value-of select="entry[@colname='3']" />
											</strong>
										</xsl:when>
										<xsl:otherwise>
											<xsl:value-of select="entry[@colname='3']" />
										</xsl:otherwise>
									</xsl:choose>
								</td>
								<td valign="top">
									<xsl:choose>
										<xsl:when test="*//emph[@render='bold']">
											<strong style="color:#404040">
												<xsl:value-of select="entry[@colname='4']" />
											</strong>
										</xsl:when>
										<xsl:otherwise>
											<xsl:value-of select="entry[@colname='4']" />
										</xsl:otherwise>
									</xsl:choose>
								</td>
							</tr>
						</xsl:for-each>

						<p />
					</xsl:for-each>
				</table>
			</div>
		</xsl:for-each>
	</xsl:template>

	<xsl:template name="table1">
		<xsl:for-each select="table/tgroup">
			<table border="0" width="98%">
				<xsl:for-each select="thead">
					<xsl:for-each select="row">
						<tr>
							<xsl:for-each select="entry">
								<td valign="top">
									<strong style="color:#404040;">
										<xsl:apply-templates select="." />
									</strong>
								</td>
							</xsl:for-each>
						</tr>
					</xsl:for-each>
				</xsl:for-each>
				<xsl:for-each select="tbody">
					<xsl:for-each select="row">
						<tr>
							<xsl:for-each select="entry">
								<td valign="top">
									<xsl:apply-templates select="." />
								</td>
							</xsl:for-each>
						</tr>
					</xsl:for-each>
				</xsl:for-each>
				<xsl:apply-templates />
			</table>
		</xsl:for-each>
	</xsl:template>


	<!-- ===== SECTION 1: OVERVIEW <did> ===== -->

	<xsl:template match="archdesc/did">

<script> dispClosed.push('collapsible_overview')</script>
				
		<a name="a1" />
		<h2>
			<!-- BEGIN collapsible_overview Function -->
			<xsl:element name="a">
				<xsl:attribute name="href">
                    <xsl:text>javascript:changeDisplay('collapsible_overview');</xsl:text>
                </xsl:attribute>
				<xsl:attribute name="style">
                    <xsl:text>cursor:pointer; color:#404040; text-decoration:none;</xsl:text>
                </xsl:attribute>
				<img id="xcollapsible_overview" class="hiddenprint"
					src="assets/DownArrowBlue.png" border="0" height="10px"
					align="bottom" alt="Expand/Collapse" hspace="0" vspace="0" />
				<xsl:value-of select="head" />
			</xsl:element>
			<!-- END collapsible_overview Function -->

		</h2>
		<!-- BEGIN collapsible_overview Division -->
		<div id="collapsible_overview" class="showhide"
			style="display:block;">
			<table border="0" width="96%">
				<tr>
					<td width="5%">
					</td>
					<td width="15%">
					</td>
					<td width="76%">
					</td>
				</tr>

				<xsl:if test="origination">
					<tr>
						<td valign="top" />
						<td valign="top">
							<strong style="color:#404040;">Creator:</strong>
						</td>
						<td>
							<xsl:value-of select="origination" />
						</td>
					</tr>
				</xsl:if>
				<xsl:if test="unittitle">
					<tr>
						<td valign="top" />
						<td valign="top">
							<strong style="color:#404040;">Title:</strong>
						</td>
						<td>
							<xsl:apply-templates select="unittitle" />
						</td>
					</tr>
				</xsl:if>
				<xsl:if test="unitdate">
					<tr>
						<td valign="top" />
						<td valign="top">
							<strong style="color:#404040;">Dates:</strong>
						</td>
						<td>
							<!-- Joyce 2008: Changed to provide for bulk dates 245$g by adding 
								a <xsl:for-each> with dates -->
							<xsl:for-each select="unitdate">
								<xsl:value-of select="." />
								<xsl:if test="position()!=last()">
									<xsl:text> </xsl:text>
								</xsl:if>
							</xsl:for-each>
						</td>
					</tr>
				</xsl:if>

				<xsl:if test="langmaterial">
					<tr>
						<td valign="top" />
						<td valign="top">
							<strong style="color:#404040;">Language:</strong>
						</td>
						<td>
							<xsl:value-of select="langmaterial" />
						</td>
					</tr>
				</xsl:if>

				<xsl:if test="abstract">
					<tr>
						<td valign="top" />
						<td valign="top">
							<strong style="color:#404040;">Abstract:</strong>
						</td>
						<td>
							<xsl:apply-templates select="abstract" />
						</td>
					</tr>
				</xsl:if>
				<xsl:if test="physdesc">
					<tr>
						<td valign="top" />
						<td valign="top">
							<strong style="color:#404040;">Quantity:</strong>
						</td>
						<td>
							<xsl:value-of select="physdesc" />
						</td>
					</tr>
				</xsl:if>
				<xsl:if test="physloc">
					<tr>
						<td valign="top" />
						<td valign="top">
							<strong style="color:#404040;">Location:</strong>
						</td>
						<td>
							<xsl:apply-templates select="physloc" />
						</td>
					</tr>
				</xsl:if>
			</table>
			<br />
		</div>
		<!-- END collapsible_overview Division -->
	</xsl:template>


	<!-- ========== SECTION 2: BIOGRAPHICAL/HISTORICAL NOTE <bioghist> ========== -->

	<xsl:template match="archdesc/bioghist">
	
	<script> dispClosed.push('collapsiblebioghist')</script>
	
		<a name="a2" />
		<h2>
			<!-- BEGIN collapsiblebioghist Function -->
			<xsl:element name="a">
				<xsl:attribute name="href">
                    <xsl:text>javascript:changeDisplay('collapsiblebioghist');</xsl:text>
                </xsl:attribute>
				<xsl:attribute name="style">
                    <xsl:text>cursor:pointer; color:#404040; text-decoration:none;</xsl:text>
                </xsl:attribute>
				<img id="xcollapsiblebioghist" class="hiddenprint"
					src="assets/DownArrowBlue.png" border="0" height="10px"
					alt="Expand/Collapse" align="bottom" hspace="0" vspace="0" />
				<xsl:value-of select="head" />
			</xsl:element>
			<!-- END collapsiblebioghist Function -->
		</h2>
		<!-- BEGIN collapsiblebioghist Division -->
		<div id="collapsiblebioghist" class="showhide"
			style="display:block;">
			<xsl:for-each select="daogrp">
				<xsl:apply-templates select="."></xsl:apply-templates>
			</xsl:for-each>
			<xsl:for-each select="p">
				<p>
					<xsl:apply-templates select="." />
				</p>
			</xsl:for-each>
			<xsl:call-template name="chronlist" />
			<xsl:call-template name="EADtable" />

			<xsl:for-each select="bioghist">
				<h3>
					<!-- BEGIN collapsiblebioghistsubsection Function -->
					<xsl:element name="a">
						<xsl:attribute name="href">
                            <xsl:text>javascript:changeDisplay('collapsiblebioghistsubsection</xsl:text><xsl:number
							format="1" count="bioghist" />'); </xsl:attribute>
						<xsl:attribute name="style">
                            <xsl:text>cursor:pointer; color:#3a6894; text-decoration:none;</xsl:text>
                        </xsl:attribute>
						<img class="hiddenprint" src="assets/DownArrowBlue.png"
							border="0" height="9px" alt="Collapse/Expand" align="bottom"
							hspace="0" vspace="0">
							<xsl:attribute name="id">xcollapsiblebioghistsubsection<xsl:number
								format="1" count="bioghist" />
                            </xsl:attribute>
						</img>
						<xsl:value-of select="head" />
					</xsl:element>
					<!-- END collapsiblebioghistsubsection Function -->

				</h3>
				<!-- BEGIN collapsiblebioghistsubsection Division -->
				<div class="showhide" style="display:block;">
					<xsl:attribute name="id">collapsiblebioghistsubsection<xsl:number
						format="1" count="bioghist" />
                        </xsl:attribute>
					<xsl:for-each select="p">
						<p>
							<xsl:apply-templates select="." />
						</p>
					</xsl:for-each>
					<xsl:call-template name="chronlist" />
					<xsl:call-template name="EADtable" />
				</div>
				<!-- END collapsiblebioghistsubsection Division -->
			</xsl:for-each>

			<br clear="all" />
			<p>
				<a href="#a0" class="hiddenprint">Return to top</a>
			</p>
		</div>
		<!-- END collapsiblebioghist Division -->
	</xsl:template>


	<!-- ===== BEGIN Chronology Table ===== -->
	<xsl:template name="chronlist">
		<div id="chronlist">
			<xsl:for-each select="chronlist">
				<h3>
					<xsl:value-of select="head" />
				</h3>
				<table>
					<tr>
						<td valign="top" width="25%">
						</td>
						<td valign="top" width="auto">
						</td>
					</tr>
					<tr>
						<td valign="top" width="25%">
							<strong style="color:#404040;">
								<xsl:value-of select="listhead/head01" />
							</strong>
						</td>
						<td valign="top">
							<strong style="color:#404040;">
								<xsl:value-of select="listhead/head02" />
							</strong>
						</td>
					</tr>
					<xsl:for-each select="chronitem">
						<xsl:choose>
							<xsl:when test="eventgrp">
								<tr>
									<td valign="top" width="25%">
										<xsl:apply-templates select="date" />
									</td>
									<td valign="top">
										<xsl:apply-templates
											select="eventgrp/event[position()=1]" />
									</td>
								</tr>
								<xsl:for-each
									select="eventgrp/event[position()!=1]">
									<tr>
										<td />
										<td />
										<td valign="top">
											<xsl:apply-templates />
										</td>
									</tr>
								</xsl:for-each>
							</xsl:when>
							<xsl:otherwise>
								<tr>
									<td valign="top" width="25%">
										<xsl:apply-templates select="date" />
									</td>
									<td valign="top">
										<xsl:for-each select="event">
											<xsl:apply-templates select="." />
										</xsl:for-each>
									</td>
								</tr>
							</xsl:otherwise>
						</xsl:choose>
					</xsl:for-each>
				</table>
			</xsl:for-each>
		</div>
	</xsl:template>
	<!-- ===== END Chronologies ===== -->


	<!-- ========== SECTION 3: SCOPE AND CONTENTS <scopecontent> ========== -->
	<xsl:template match="archdesc/scopecontent">
	
	<script> dispClosed.push('collapsiblescopecontent')</script>
	
		<a name="a3" />
		<h2>
			<!-- BEGIN collapsiblescopecontent Function -->
			<xsl:element name="a">
				<xsl:attribute name="href">
                    <xsl:text>javascript:changeDisplay('collapsiblescopecontent');</xsl:text>
                </xsl:attribute>
				<xsl:attribute name="style">
                    <xsl:text>cursor:pointer; color:#404040; text-decoration:none;</xsl:text>
                </xsl:attribute>
				<img id="xcollapsiblescopecontent" class="hiddenprint"
					src="assets/DownArrowBlue.png" border="0" height="10x"
					align="bottom" alt="Expand/Collapse" hspace="0" vspace="0" />
				<xsl:value-of select="head" />
			</xsl:element>
			<!-- END collapsiblescopecontent Function -->
		</h2>

		<!-- BEGIN collapsiblescopecontent Division -->
		<div id="collapsiblescopecontent" class="showhide"
			style="display:block;">
			<xsl:for-each select="p">
				<p>
					<xsl:apply-templates select="." />
				</p>
			</xsl:for-each>
			<xsl:call-template name="chronlist" />
			<xsl:if test="table">
				<xsl:call-template name="EADtable" />
			</xsl:if>
			<xsl:for-each select="scopecontent">
				<xsl:for-each select="head">
					<h3>
						<!-- BEGIN collapsiblescopecontentsubsection Function -->
						<xsl:element name="a">
							<xsl:attribute name="href">
                            <xsl:text>javascript:changeDisplay('collapsiblescopecontentsubsection</xsl:text><xsl:number
								format="1" count="scopecontent" />'); </xsl:attribute>
							<xsl:attribute name="style">
                            <xsl:text>cursor:pointer; color:#3a6894; text-decoration:none;</xsl:text>
                        </xsl:attribute>
							<img class="hiddenprint" src="assets/DownArrowBlue.png"
								border="0" height="9px" alt="Collapse/Expand" align="bottom"
								hspace="0" vspace="0">
								<xsl:attribute name="id">xcollapsiblescopecontentsubsection<xsl:number
									format="1" count="scopecontent" />
                            </xsl:attribute>
							</img>
							<xsl:apply-templates select="." />
						</xsl:element>
						<!-- END collapsiblescopecontentsubsection Function -->
					</h3>
				</xsl:for-each>
				<!-- BEGIN collapsiblescopecontentsubsection Division -->
				<div class="showhide" style="display:block;">
					<xsl:attribute name="id">collapsiblescopecontentsubsection<xsl:number
						format="1" count="scopecontent" />
                    </xsl:attribute>
					<xsl:for-each select="p">
						<p>
							<xsl:apply-templates select="." />
						</p>
					</xsl:for-each>
					<xsl:call-template name="chronlist" />
					<xsl:call-template name="EADtable" />
				</div>
				<!-- END collapsiblescopecontentsubsection Division -->
			</xsl:for-each>

			<br clear="all" />
			<p>
				<a href="#a0" class="hiddenprint">Return to top</a>
			</p>
		</div>
		<!-- END collapsiblescopecontent Division -->
	</xsl:template>


	<!-- ========== SECTION 4: ARRANGEMENT <arrangement> ========== -->
	<xsl:template match="archdesc/arrangement">
		<a name="a4" />
		<h2>
			<!-- BEGIN collapsiblearrangement Function -->
			<xsl:element name="a">
				<xsl:attribute name="href">
                    <xsl:text>javascript:changeDisplay('collapsiblearrangement');</xsl:text>
                </xsl:attribute>
				<xsl:attribute name="style">
                    <xsl:text>cursor:pointer; color:#404040; text-decoration:none</xsl:text>
                </xsl:attribute>
				<img id="xcollapsiblearrangement" class="hiddenprint"
					src="assets/DownArrowBlue.png" border="0" height="10px"
					align="bottom" alt="Expand/Collapse" hspace="0" vspace="0" />
				<xsl:value-of select="head" />
			</xsl:element>
			<!-- END collapsiblearrangement Function -->
		</h2>
		<!-- BEGIN collapsiblearrangement Division -->
		<div id="collapsiblearrangement" class="showhide"
			style="display:block;">
			<xsl:for-each select="p">
				<p>
					<xsl:apply-templates select="." />
				</p>
			</xsl:for-each>
			<table border="0" width="85%">
				<tr>
					<td valign="top" width="10%">
					</td>
					<td valign="top" width="5%">
					</td>
					<td valign="top" width="70%">
					</td>
				</tr>

				<!-- Condition test: If c02 (@level="subseries"), link on item, if c02 
					not(@level="subseries"), link on list/head. -->
				<xsl:for-each select="list">
					<xsl:choose>
						<xsl:when test="not(head)">
							<xsl:for-each select="item">
								<tr>
									<td valign="top" />
									<td valign="top" />
									<td colspan="1">

										<!-- BEGIN COMMENT OUT <a> <xsl:attribute name="href">#series<xsl:number 
											level="any" from="arrangement" format="1" count="list/item"/> </xsl:attribute> 
											END COMMENT OUT -->

										<xsl:apply-templates select="." />

										<!-- BEGIN COMMENT OUT </a> END COMMENT OUT -->

									</td>
								</tr>
							</xsl:for-each>
						</xsl:when>

						<xsl:otherwise>
							<xsl:for-each select="head">
								<tr>
									<td valign="top" />
									<td colspan="2" valign="top">


										<!-- BEGIN COMMENT OUT <a> <xsl:attribute name="href">#series<xsl:number 
											level="any" from="arrangement" format="1" count="list/head"/> </xsl:attribute> 
											END COMMENT OUT -->

										<xsl:apply-templates select="." />

										<!-- BEGIN COMMENT OUT </a> END COMMENT OUT -->

									</td>
								</tr>
							</xsl:for-each>
							<xsl:for-each select="item">
								<tr>
									<td valign="top" />
									<td valign="top" />
									<td colspan="1">

										<!-- BEGIN COMMENT OUT <a> <xsl:attribute name="href">#subseries<xsl:number 
											level="any" from="arrangement" format="1" count="item"/> </xsl:attribute> 
											END COMMENT OUT -->

										<xsl:apply-templates select="." />

										<!-- BEGIN COMMENT OUT </a> END COMMENT OUT -->

									</td>
								</tr>
							</xsl:for-each>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:for-each>
				<!-- close list -->
				<!-- ===== END LIST ===== -->



			</table>
			<p />
			<br clear="all" />
			<p>
				<a href="#a0" class="hiddenprint">Return to top</a>
			</p>
		</div>
		<!-- END collapsiblearrangement Division -->
	</xsl:template>


	<!-- ========== SECTION 6: OTHER FINDING AIDS <otherfindaid> ========== -->

	<xsl:template match="archdesc/otherfindaid">
	
	<script> dispClosed.push('collapsibleotherfindaid')</script>
	
		<a name="a6" />
		<h2>
			<!-- BEGIN collapsibleotherfindaid Function -->
			<xsl:element name="a">
				<xsl:attribute name="href">
                    <xsl:text>javascript:changeDisplay('collapsibleotherfindaid');</xsl:text>
                </xsl:attribute>
				<xsl:attribute name="style">
                    <xsl:text>cursor:pointer; color:#404040; text-decoration:none;</xsl:text>
                </xsl:attribute>
				<img id="xcollapsibleotherfindaid" class="hiddenprint"
					src="assets/DownArrowBlue.png" border="0" height="10px"
					align="bottom" alt="Expand/Collapse" hspace="0" vspace="0" />
				<xsl:value-of select="head" />
			</xsl:element>
		</h2>
		<!-- BEGIN collapsibleotherfindaid Division -->
		<div id="collapsibleotherfindaid" class="showhide"
			style="display:block;">
			<xsl:for-each select="p">
				<p>
					<xsl:apply-templates />
				</p>
			</xsl:for-each>
			<p />
			<p>
				<a href="#a0" class="hiddenprint">Return to top</a>
			</p>
		</div>
		<!-- END collapsibleotherfindaid Division -->
	</xsl:template>

	<!-- ========== SECTION 8: ADMINISTRATIVE INFORMATION <descgrp> ========== -->

	<xsl:template match="archdesc/descgrp">
	
	<script> dispClosed.push('collapsibledescgrp')</script>
	
		<a name="a8" />
		<h2>
			<!-- BEGIN collapsibledescgrp Function -->
			<xsl:element name="a">
				<xsl:attribute name="href">
                    <xsl:text>javascript:changeDisplay('collapsibledescgrp');</xsl:text>
                </xsl:attribute>
				<xsl:attribute name="style">
                    <xsl:text>cursor:pointer; color:#404040; text-decoration:none;</xsl:text>
                </xsl:attribute>
				<img id="xcollapsibledescgrp" class="hiddenprint"
					src="assets/DownArrowBlue.png" border="0" height="10px"
					align="bottom" alt="Expand/Collapse" hspace="0" vspace="0" />
				<xsl:value-of select="head" />
			</xsl:element>
			<!-- END collapsibledescgrp Function -->
		</h2>
		<!-- BEGIN collapsibledescgrp Divison -->
		<div id="collapsibledescgrp" class="showhide"
			style="display:block;">
			<xsl:choose>
				<xsl:when test="accessrestrict">
				</xsl:when>
				<xsl:otherwise>
					<p style="color:#404040; margin-left:15px;">
						<strong>Availability:</strong><!-- <strong>Access Information:</strong> -->
					</p>
					<p style="margin-left:30px;">The collection is open for research use.</p>
				</xsl:otherwise>
			</xsl:choose>
			<xsl:for-each
				select="accessrestrict | userestrict | phystech | bibliography | prefercite | odd | altformavail | originalsloc | custodhist | acqinfo | processinfo | appraisal">
				<xsl:if test="head">
					<p style="color:#404040; margin-left:15px;">
						<strong>
							<xsl:value-of select="head" />
						</strong>
					</p>
					<xsl:for-each select="p">
						<p style="margin-left:30px;">
							<xsl:apply-templates select="." />
						</p>
					</xsl:for-each>
				</xsl:if>
			</xsl:for-each>
			<br clear="all" />
			<p>
				<a href="#a0" class="hiddenprint">Return to top</a>
			</p>
		</div>
		<!-- END collapsibledescgrp Division -->
	</xsl:template>

	<!-- ========== SECTION 9: DETAILED DESCRIPTION <dsc> ========== -->
	<!-- Joyce/Monica 2008: Based on Michael Fox's EAD Cookbook dsc4.xsl with 
		modification to fit MHS use and style -->

	<!-- ===== Section 9.A. <head>, <note>, and <p> ===== -->

	<xsl:template match="archdesc/dsc">
		<xsl:apply-templates />
		<!-- <p> <a href="#a0" class="hiddenprint">Return to top</a> </p> -->
	</xsl:template>

	<xsl:template match="dsc/head">
		<a name="a9" />
		<h2>
			<xsl:apply-templates />
		</h2>
	</xsl:template>


	<xsl:template match="dsc/p | dsc/note/p">
		<p>
			<xsl:apply-templates />
		</p>
	</xsl:template>


	<!-- ===== Section 9.B. TEMPLATE: CONTAINER ===== -->
	<!-- Revised March 3, 2009 1. Test for physloc. Output type attribute when 
		present, otherwise output "Location." 2. Test for container. Output type 
		attribute when present, otherwise output "Box." -->

	<xsl:template name="container">
		<xsl:if test="physloc">
			<tr>
				<td valign="top" colspan="12" style="background-color:black">
					<xsl:apply-templates select="container" />
				</td>
			</tr>
			<tr>
				<td colspan="10" style="">
					<strong style="color:#666666;">
						<xsl:choose>
							<xsl:when test="physloc/@type">
								<xsl:value-of
									select="concat(translate(substring(physloc/@type, 1,1),'abcdefghijklmnopqrstuvwxyz',
                                'ABCDEFGHIJKLMNOPQRSTUVWXYZ'),substring(physloc/@type,2,string-length(physloc/@type)))" />
							</xsl:when>
							<xsl:otherwise>
								<!-- <text>Location</text> -->
							</xsl:otherwise>
						</xsl:choose>
					</strong>
				</td>
			</tr>
			<tr>
				<td colspan="10">
					<strong style="color:#666666;">
						<xsl:choose>
							<xsl:when test="container/@type">
								<xsl:value-of
									select="concat(translate(substring(container/@type, 1,1),'abcdefghijklmnopqrstuvwxyz',
                                    'ABCDEFGHIJKLMNOPQRSTUVWXYZ'),substring(container/@type,2,string-length(container/@type)))" />
							</xsl:when>
							<xsl:when test="not(container)">
								<xsl:text />
							</xsl:when>
							<xsl:otherwise>
								<xsl:text> Box </xsl:text>
							</xsl:otherwise>
						</xsl:choose>
					</strong>
				</td>
			</tr>
		</xsl:if>
	</xsl:template>


	<!-- ===== Section 9.C. TEMPLATE: COMPONENT-DID ===== -->

	<!-- This template is used generically to format <unitid>, <origination>, 
		<unittitle>, <unitdate>, and <physdesc> at all component levels. Anhors for 
		each did//ref[@id] are included. A space is added between each <did> element. -->

	<xsl:template name="component-did">
		<xsl:if test="unitid">
			<xsl:apply-templates select="unitid" />
			<xsl:text>&#x20;</xsl:text>
		</xsl:if>

		<xsl:if test="origination">
			<xsl:apply-templates select="origination" />
			<xsl:text>&#x20;</xsl:text>
		</xsl:if>

		<xsl:apply-templates select="unittitle" />
		<xsl:text>&#x20;</xsl:text>


		<xsl:if test="unittitle/geogname">
			<xsl:apply-templates select="geogname" />
			<xsl:text>&#x20;</xsl:text>
		</xsl:if>

		<xsl:if test="unittitle/imprint/geogname">
			<xsl:apply-templates select="imprint/geogname" />
			<xsl:text>&#x20;</xsl:text>
		</xsl:if>

		<xsl:if test="unittitle/imprint/publisher">
			<xsl:apply-templates select="imprint/publisher" />
			<xsl:text>&#x20;</xsl:text>
		</xsl:if>

		<xsl:if test="unittitle/imprint/date">
			<xsl:apply-templates select="imprint/date" />
			<xsl:text>&#x20;</xsl:text>
		</xsl:if>

		<!-- Process unitdate when it is not a child of unittitle -->
		<xsl:for-each select="unitdate">
			<xsl:apply-templates />
			<xsl:text>&#x20;</xsl:text>
		</xsl:for-each>

		<xsl:if test="physdesc">
			<xsl:apply-templates select="physdesc" />
			<xsl:text>&#x20;</xsl:text>
		</xsl:if>

		<xsl:if test="materialspec">
			<xsl:apply-templates select="materialspec" />
			<xsl:text>&#x20;</xsl:text>
		</xsl:if>


	</xsl:template>



	<!-- IUPAC FAIRSpec additions BH -->


	<xsl:template name="fs-dao">

		<xsl:choose>
			<xsl:when test="@role='compound'">
				<xsl:call-template name="fs-dao-compound" />
			</xsl:when>
			<xsl:when test="starts-with(@role,'nmr:')">
				<xsl:call-template name="fs-dao-nmr" />
			</xsl:when>
			<xsl:otherwise>
				<td colspan="20">
					<xsl:apply-templates />
				</td>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<xsl:template name="fs-dao-compound">
		<xsl:for-each select="daodesc/note">
			<xsl:choose>
				<xsl:when test="@type='chem-name'">
				<xsl:variable name="chemName" select="p"></xsl:variable>
					<tr>
						<td colspan="20">
							<xsl:element name="span">
								<xsl:attribute name="style">
                    						<xsl:text>font-size:12pt;font-weight:bold;text-decoration:none;color:blue</xsl:text>
                						</xsl:attribute>
								<xsl:value-of select='p' />
							</xsl:element>
						</td>
					</tr>

					<tr>
						<td colspan="19" valign="top">
							<img>
								<xsl:attribute name="src">https://cactus.nci.nih.gov/chemical/structure/<xsl:value-of
									select='p' />/image</xsl:attribute>
							</img>

						</td>
					</tr>
				</xsl:when>
				<xsl:otherwise>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:for-each>
		<tr>
			<td colspan="20">
				<xsl:element name="a">
					<xsl:attribute name="target">
			<xsl:text>_blank</xsl:text>              	
								</xsl:attribute>
					<xsl:attribute name="href">
											<xsl:value-of select="@href" />
                						</xsl:attribute>
					<xsl:attribute name="style">
                    						<xsl:text>font-weight:bold;text-decoration:none;color:blue</xsl:text>
                						</xsl:attribute>
					<xsl:text>repository entry</xsl:text>
				</xsl:element>
					<span style="margin-left:3em"></span>
				<xsl:for-each select="daodesc/note">
					<xsl:choose>
						<xsl:when test="@type='chem-name'">
							<xsl:element name="a">
								<xsl:attribute name="target">
                    						<xsl:text>_blank</xsl:text>
                						</xsl:attribute>
								<xsl:attribute name="href">https://chemapps.stolaf.edu/jmol/jmol.php?model=<xsl:value-of select='p' />
   										</xsl:attribute>
								<xsl:attribute name="style">
                    						<xsl:text>font-weight:bold;text-decoration:none;color:blue</xsl:text>
                						</xsl:attribute>
								<xsl:text>3D Model</xsl:text>
							</xsl:element>
						</xsl:when>
						<xsl:when test="@type='chem-inchi'">
							<xsl:element name="a">
								<xsl:attribute name="href">
										javascript:alert('<xsl:value-of select='p'></xsl:value-of>')
                						</xsl:attribute>
								<xsl:attribute name="style">
                    						<xsl:text>font-weight:bold;text-decoration:none;color:blue</xsl:text>
                						</xsl:attribute>
								<xsl:text>InChI</xsl:text>
							</xsl:element>
						</xsl:when>
						<xsl:when test="@type='chem-inchikey'">
							<xsl:element name="a">
								<xsl:attribute name="href">
										javascript:alert('<xsl:value-of select='p'></xsl:value-of>')
                						</xsl:attribute>
								<xsl:attribute name="style">
                    						<xsl:text>font-weight:bold;text-decoration:none;color:blue</xsl:text>
                						</xsl:attribute>
								<xsl:text>InChIKey</xsl:text>
							</xsl:element>
						</xsl:when>
						<xsl:otherwise>
							<xsl:apply-templates />
						</xsl:otherwise>
					</xsl:choose>
					<span style="margin-left:3em"></span>
				</xsl:for-each>
			</td>
		</tr>

	</xsl:template>

	<xsl:template name="fs-dao-nmr">
		<td width="40"></td><td>
			<xsl:element name="a">
				<xsl:attribute name="href">
					<xsl:value-of select="@href" />
                </xsl:attribute>
				<xsl:attribute name="style">
                    <xsl:text>text-decoration:none;color:blue</xsl:text>
                </xsl:attribute>
				<xsl:text>download</xsl:text>
			</xsl:element>
		</td>
		<xsl:if test="daodesc">
			<xsl:call-template name="fs-daodesc-nmr" />
		</xsl:if>
		</xsl:template>

	<xsl:template name="fs-daodesc-nmr">
		<xsl:for-each select="daodesc/note">
			<td>
				<xsl:choose>
					<xsl:when test="@type='type'">
						<xsl:value-of select="p" />
					</xsl:when>
					<xsl:when test="@type='filename'">
						<xsl:value-of select="p" />
					</xsl:when>
					<xsl:when test="@type='size'">
						<xsl:value-of select="p" />
					</xsl:when>
					<xsl:when test="@type='mime-type'">
						<xsl:value-of select="p" />
					</xsl:when>
					<xsl:otherwise>
						<xsl:apply-templates />
					</xsl:otherwise>
				</xsl:choose>
			</td>
		</xsl:for-each>
	</xsl:template>



	<!-- ===== Section 9.D. CONDITIONS FOR TABLE DISPLAY STYLE ===== -->

	<!-- ===== Section 9.D.1. FLAT COLLECTIONS ===== -->

	<!-- <c01> "not[@level='series] and not(@level='subseries')" Legacy: <dsc 
		@id="fruin"> -->

	<!--Style used for c01 components that are file level descriptions with 
		associated <physloc> and <container> data. The instructions here process 
		<c0n> elements with calls to the templates named c0n-flat. -->



	<xsl:template match="c01">


		<!-- Flat collections are styled with a 12 column table. -->
		<div id="dsctable">
			<table border="0" width="98%" align="center">
				<tr>
					<td width="4%">
					</td>
					<td width="4%">
					</td>
					<td width="4%">
					</td>
					<td width="4%">
					</td>
					<td width="4%">
					</td>
					<td width="4%">
					</td>
					<td width="4%">
					</td>
					<td width="4%">
					</td>
					<td width="4%">
					</td>
					<td width="4%">
					</td>
					<td width="4%">
					</td>
					<td width="4%">
					</td>
				</tr>

				<xsl:choose>
					<xsl:when
						test="not(@level='series') or not(@level='subseries')">
						<!-- ==== Series ANCHOR from Section 4 <arrangement> ===== -->

						<!-- ===== ANCHOR from <arrangement/list/item> (c01-flat) ===== -->

						<!-- BEGIN COMMENT OUT <a> <xsl:attribute name="name">series<xsl:number 
							level="any" from="dsc" format="1" count="c01"/> </xsl:attribute> </a> END 
							COMMENT OUT -->

						<xsl:call-template name="c01-flat" />
					</xsl:when>
				</xsl:choose>

				<xsl:for-each select="c02">
					<xsl:choose>
						<xsl:when
							test="not(@level='series') or not(@level='subseries')">
							<xsl:call-template name="c02-flat" />
						</xsl:when>
						<xsl:otherwise>
						</xsl:otherwise>
					</xsl:choose>
					<xsl:for-each select="c03">
						<xsl:call-template name="c03-flat" />

						<xsl:for-each select="c04">
							<xsl:call-template name="c04-flat" />

							<xsl:for-each select="c05">
								<xsl:call-template name="c05-flat" />

								<xsl:for-each select="c06">
									<xsl:call-template name="c06-flat" />

									<xsl:for-each select="c07">
										<xsl:call-template name="c07-flat" />

										<xsl:for-each select="c08">
											<xsl:call-template name="c08-flat" />

											<xsl:for-each select="c09">
												<xsl:call-template name="c09-flat" />

												<xsl:for-each select="c10">
													<xsl:call-template name="c10-flat" />

												</xsl:for-each>
												<!--Close C10 -->
											</xsl:for-each>
											<!--Close c09 -->
										</xsl:for-each>
										<!--Close c08 -->
									</xsl:for-each>
									<!--Close c07 -->
								</xsl:for-each>
								<!--Close c06 -->
							</xsl:for-each>
							<!--Close c05 -->
						</xsl:for-each>
						<!--Close c04 -->
					</xsl:for-each>
					<!--Close c03 -->
				</xsl:for-each>
				<!--Close c02 -->
			</table>
		</div>
	</xsl:template>


	<!-- ===== SECTION 9.D.2. COMPLEX, SIMPLE, and MIXED COLLECTIONS ===== -->

	<!-- Complex Condition: c01[@level='series'] and c02[@level='subseries'] 
		Legacy: dsc[@id='muller'] -->

	<!-- Simple Condition: c01[@level='series'] Legacy: dsc[@id='feith'] -->

	<!-- Mixed Condition: varying combinations of (<c01 @level="series"> and 
		<c02 @level="subseries"] Legacy: None -->


	<xsl:template match="c01[@level='series']">
		<!-- ==== Series ANCHOR from Section 4 <arrangement> ===== -->

		<!--BEGIN COMMENT OUT <a> <xsl:attribute name="name">series<xsl:number 
			level="any" from="dsc" format="1" count="c01"/> </xsl:attribute> </a> END 
			COMMMENT OUT -->

		<xsl:call-template name="c01-complex" />

		<!-- BEGIN collapsiblec01 Division -->

		<div class="showhide" style="display:block;">
			<xsl:attribute name="id">collapsiblec01<xsl:number
				format="1" count="c01[@level='series']" />
            </xsl:attribute>
			<xsl:for-each
				select="abstract | langmaterial | accessrestrict |userestrict">
				<p>
					<xsl:apply-templates />
				</p>
			</xsl:for-each>

			<xsl:for-each select="bioghist">
				<xsl:for-each select="head">
					<p style="font-size:11px;">
						<strong>
							<xsl:apply-templates select="." />
						</strong>
					</p>
				</xsl:for-each>
				<xsl:for-each select="p">
					<p>
						<xsl:apply-templates select="." />
					</p>
				</xsl:for-each>
				<xsl:if test="chronlist">
					<xsl:call-template name="chronlist" />
				</xsl:if>
				<br clear="all" />
			</xsl:for-each>

			<xsl:for-each select="scopecontent">
				<xsl:for-each select="head">
					<p style="font-size:11px;">
						<strong>
							<xsl:apply-templates select="." />
						</strong>
					</p>
				</xsl:for-each>
				<xsl:for-each select="p">
					<p>
						<xsl:apply-templates select="." />
					</p>
				</xsl:for-each>


				<xsl:if test="table">
					<xsl:call-template name="EADtable" />
				</xsl:if>
				<br clear="all" />
			</xsl:for-each>


			<xsl:for-each
				select="arrangement  | processinfo | 
                acqinfo | accruals | appraisal | custodhist | phystech | controlaccess/controlaccess | altformavail | bibliography |
                relatedmaterial | separatedmaterial | originalsloc | odd | note | otherfindaid | descgrp/*">
				<xsl:for-each select="head">
					<p style="font-size:11px;">
						<strong>
							<xsl:apply-templates />
						</strong>
					</p>
				</xsl:for-each>
				<xsl:for-each select="*[not(self::head)]">
					<p>
						<xsl:apply-templates />
					</p>
				</xsl:for-each>
				<br clear="all" />
			</xsl:for-each>

			<xsl:for-each select="daogrp">
				<p>
					<xsl:apply-templates select="." />
				</p>
			</xsl:for-each>


			<!-- Complex, simple, and mixed collections display in a 14-column HTML 
				table. -->


			<xsl:for-each select="c02">
				<xsl:choose>
					<xsl:when test="@level='subseries'">

						<!-- ===== Subseries ANCHOR from <arrangement/list/item> ===== -->

						<!-- BEGIN COMMENT OUT <a> <xsl:attribute name="name">subseries<xsl:number 
							level="any" from="dsc" format="1" count="c02"/> </xsl:attribute> </a> END 
							COMMENT OUT -->

						<xsl:call-template name="c02-complex" />
						<!-- BEGIN collapsiblesubseries division -->
						<div class="showhide" style="display:block;">
							<xsl:attribute name="id">collapsiblesubseries<xsl:number
								level="any" from="dsc" format="1"
								count="c01/c02[@level='subseries']" />
                                </xsl:attribute>
							<div id="dsctable">
								<table border="0" width="98%" align="center">
									<tr>
										<td width="16%">
										</td>
										<td width="10%">
										</td>
										<td width="4%">
										</td>
										<td width="4%">
										</td>
										<td width="4%">
										</td>
										<td width="4%">
										</td>
										<td width="4%">
										</td>
										<td width="4%">
										</td>
										<td width="4%">
										</td>
										<td width="4%">
										</td>
										<td width="4%">
										</td>
										<td width="4%">
										</td>
										<td width="4%">
										</td>
										<td width="4%">
										</td>
									</tr>
									<xsl:for-each select="abstract | langmaterial">
										<tr>
											<td colspan="14" valign="top">
												<xsl:apply-templates />
											</td>
										</tr>
									</xsl:for-each>
									<xsl:for-each
										select="scopecontent | bioghist | arrangement | userestrict | accessrestrict | processinfo |
                                    acqinfo | accruals | appraisal | custodhist | phystech | controlaccess/controlaccess | altformavail | bibliography |  
                                    relatedmaterial | separatedmaterial | originalsloc | odd | note | otherfindaid | descgrp/*">
										<xsl:for-each select="head">
											<tr>
												<td valign="top" colspan="14">
													<strong>
														<xsl:apply-templates />
													</strong>
												</td>
											</tr>
										</xsl:for-each>
										<xsl:for-each select="*[not(self::head)]">
											<tr>
												<td valign="top" colspan="14">
													<xsl:apply-templates />
												</td>
											</tr>
										</xsl:for-each>
									</xsl:for-each>
									<xsl:for-each select="daogrp">
										<tr>

											<td colspan="13" valign="top">
												<xsl:apply-templates select="." />
											</td>
										</tr>
									</xsl:for-each>

									<xsl:for-each select="c03">
										<xsl:call-template name="c03-complex" />

										<xsl:for-each select="c04">
											<xsl:call-template name="c04-complex" />

											<xsl:for-each select="c05">
												<xsl:call-template name="c05-complex" />

												<xsl:for-each select="c06">
													<xsl:call-template name="c06-complex" />

													<xsl:for-each select="c07">
														<xsl:call-template name="c07-complex" />

														<xsl:for-each select="c08">
															<xsl:call-template name="c08-complex" />

															<xsl:for-each select="c09">
																<xsl:call-template name="c09-complex" />

																<xsl:for-each select="c10">
																	<xsl:call-template name="c10-complex" />

																	<xsl:for-each select="c11">
																		<xsl:call-template
																			name="c11-complex" />

																		<xsl:for-each select="c12">
																			<xsl:call-template
																				name="c12-complex" />
																			<!-- close c12 -->
																		</xsl:for-each>
																		<!-- close c11 -->
																	</xsl:for-each>
																	<!-- close 10 -->
																</xsl:for-each>
																<!-- close c09 -->
															</xsl:for-each>
															<!-- close c08 -->
														</xsl:for-each>
														<!-- close c07 -->
													</xsl:for-each>
													<!-- close c06 -->
												</xsl:for-each>
												<!-- close c05 -->
											</xsl:for-each>
											<!-- close c04 -->
										</xsl:for-each>
										<!-- close c03 -->
									</xsl:for-each>
									<!-- close c02 -->
								</table>
							</div>
							<!-- BEGIN COMMENT OUT <p> <a href="#a4" class="hiddenprint"><small>Return 
								to Arrangement</small></a> </p> END COMMENT OUT -->

							<!-- End collapsiblesubseries division -->
						</div>
					</xsl:when>
					<xsl:otherwise>
						<div id="dsctable">
							<table border="0" width="98%" align="center">
								<tr>
									<td width="16%">
									</td>
									<td width="10%">
									</td>
									<td width="4%">
									</td>
									<td width="4%">
									</td>
									<td width="4%">
									</td>
									<td width="4%">
									</td>
									<td width="4%">
									</td>
									<td width="4%">
									</td>
									<td width="4%">
									</td>
									<td width="4%">
									</td>
									<td width="4%">
									</td>
									<td width="4%">
									</td>
									<td width="4%">
									</td>
									<td width="4%">
									</td>
								</tr>
								<xsl:call-template name="c02-simple" />

								<xsl:for-each select="c03">
									<xsl:call-template name="c03-simple" />

									<xsl:for-each select="c04">
										<xsl:call-template name="c04-simple" />

										<xsl:for-each select="c05">
											<xsl:call-template name="c05-simple" />

											<xsl:for-each select="c06">
												<xsl:call-template name="c06-simple" />

												<xsl:for-each select="c07">
													<xsl:call-template name="c07-simple" />

													<xsl:for-each select="c08">
														<xsl:call-template name="c08-simple" />

														<xsl:for-each select="c09">
															<xsl:call-template name="c09-simple" />

															<xsl:for-each select="c10">
																<xsl:call-template name="c10-simple" />

																<xsl:for-each select="c11">
																	<xsl:call-template name="c11-simple" />
																	<!-- close c11 -->
																</xsl:for-each>
																<!-- close c10 -->
															</xsl:for-each>
															<!-- close c09 -->
														</xsl:for-each>
														<!-- close c08 -->
													</xsl:for-each>
													<!-- close c07 -->
												</xsl:for-each>
												<!-- close c06 -->
											</xsl:for-each>
											<!-- close c05 -->
										</xsl:for-each>
										<!-- close c04 -->
									</xsl:for-each>
									<!-- close c03 -->
								</xsl:for-each>
							</table>
						</div>
					</xsl:otherwise>
				</xsl:choose>
				<!-- close c02 -->
			</xsl:for-each>
			<br clear="all" />
			<p>
				<a href="#a0" class="hiddenprint">Return to top</a>
			</p>
			<hr class="hiddenprint" size="1px" style="color:#ece8e8;" />
		</div>
		<!-- END collapsiblec01 Division -->

	</xsl:template>


	<!-- ===== Section 9.E. TEMPLATE DEFINITIONS: FLAT, COMPLEX, SIMPLE ========== -->

	<!-- This section contains a separate named template for each table display 
		style. The contents of each is identical except for the spacing that is inserted 
		to create the proper column display in HTML for each component in each style. -->


	<!-- ===== 9.E.1. TEMPLATE: FLAT COLLECTIONS (c01-flat - c10-flat) ===== -->

	<!-- c01[not(@level='series') or not(@level='subseries')] Legacy: <dsc @id="fruin"> -->

	<!-- This template is used to process c01-c10 small or flat collections 
		that include c01s with associated container data. -->

	<!-- ===== C01-FLAT ===== -->
	<xsl:template name="c01-flat">


		<tr>
			<td colspan="20">
				<table cellpadding="10" cellspacing="10" style="margin:3px">

					<xsl:for-each select="dao">
						<tr>
							<td valign="top" colspan="20" style="background-color:orange">
								<xsl:apply-templates select="container" />
							</td>
						</tr>
						<tr>
							<xsl:call-template name="fs-dao" />
						</tr>
					</xsl:for-each>

				</table>
			</td>
		</tr>


		<xsl:for-each select="did">
			<xsl:call-template name="container" />
			<tr>
				<td valign="top" colspan="20">
					<xsl:apply-templates select="container" />
				</td>
			</tr>
			<tr>
				<td valign="top" colspan="10">
					<xsl:apply-templates select="extref" />
				</td>
			</tr>
			<tr>
				<td colspan="10" valign="bottom">
					<xsl:call-template name="component-did" />
				</td>
			</tr>
			<xsl:for-each select="abstract | langmaterial">
				<tr>
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td colspan="9" valign="top">
						<xsl:apply-templates />
					</td>
				</tr>
			</xsl:for-each>
		</xsl:for-each>
		<!-- Close <did> -->



		<xsl:for-each
			select="scopecontent | bioghist | arrangement | userestrict | accessrestrict | processinfo |
			acqinfo | accruals | appraisal | custodhist | phystech | controlaccess/controlaccess | altformavail | bibliography |  
			relatedmaterial| separatedmaterial | originalsloc | odd | note | otherfindaid | descgrp/*">
			<xsl:for-each select="head">
				<tr>
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td colspan="9" valign="top">
						<strong>
							<xsl:apply-templates />
						</strong>
					</td>
				</tr>
			</xsl:for-each>
			<xsl:for-each select="*[not(self::head)]">
				<tr>
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td colspan="9" valign="top">
						<xsl:apply-templates />
					</td>
				</tr>
			</xsl:for-each>
		</xsl:for-each>
		<xsl:for-each select="daogrp">
			<tr>
				<td valign="top" />
				<td valign="top" />
				<td valign="top" />
				<td colspan="9" valign="top">
					<xsl:apply-templates select="." />
				</td>
			</tr>
		</xsl:for-each>
	</xsl:template>
	<!-- ===== CO2-FLAT ===== -->
	<xsl:template name="c02-flat">


		<xsl:for-each select="did">

			<xsl:call-template name="container" />
			<tr>
				<td valign="top" colspan="10">
					<xsl:apply-templates select="physloc" />
				</td>
			</tr>
			<tr>
				<td />
				<td valign="top" colspan="12" style="background-color:blue">
					<xsl:apply-templates select="container" />
				</td>
			</tr>
			<tr>
				<td valign="top" />
				<td colspan="9" valign="bottom">
					<xsl:call-template name="component-did" />
				</td>
			</tr>

			<xsl:for-each select="abstract | langmaterial">
				<tr>
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td colspan="8" valign="top">
						<xsl:apply-templates />
					</td>
				</tr>
			</xsl:for-each>
		</xsl:for-each>


		<tr>
			<td colspan="20">
				<table cellpadding="10" cellspacing="10" style="margin:3px">

					<xsl:for-each select="dao">
						<tr>
							<xsl:call-template name="fs-dao" />
						</tr>
					</xsl:for-each>

				</table>
			</td>
		</tr>


		<xsl:for-each
			select="scopecontent | bioghist | arrangement | userestrict | accessrestrict | processinfo |
            acqinfo | accruals | appraisal | custodhist | phystech | controlaccess/controlaccess | altformavail | bibliography |  
            relatedmaterial| separatedmaterial | originalsloc | odd | note | otherfindaid | descgrp/*">
			<xsl:for-each select="head">
				<tr>
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td colspan="8" valign="top">
						<strong>
							<xsl:apply-templates />
						</strong>
					</td>
				</tr>
			</xsl:for-each>
			<xsl:for-each select="*[not(self::head)]">
				<tr>
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td colspan="8" valign="top">
						<xsl:apply-templates />
					</td>
				</tr>
			</xsl:for-each>
		</xsl:for-each>



		<xsl:for-each select="daogrp">
			<tr>
				<td valign="top" />
				<td valign="top" />
				<td valign="top" />
				<td valign="top" />
				<td colspan="8" valign="top">
					<xsl:apply-templates select="." />
				</td>
			</tr>
		</xsl:for-each>

	</xsl:template>

	<!-- ===== CO3-FLAT ===== -->
	<xsl:template name="c03-flat">
		<xsl:for-each select="did">
			<xsl:call-template name="container" />
			<tr>
				<td valign="top">
					<xsl:apply-templates select="physloc" />
				</td>
				<td valign="top">
					<xsl:apply-templates select="container" />
				</td>
				<td valign="top" />
				<td valign="top" />
				<td colspan="8" valign="bottom">
					<xsl:call-template name="component-did" />
				</td>
			</tr>
			<xsl:for-each select="abstract | langmaterial">
				<tr>
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td colspan="7" valign="top">
						<xsl:apply-templates />
					</td>
				</tr>
			</xsl:for-each>
		</xsl:for-each>
		<!-- Close <did> -->
		<xsl:for-each
			select="scopecontent | bioghist | arrangement | userestrict | accessrestrict | processinfo |
            acqinfo | accruals | appraisal | custodhist | phystech | controlaccess/controlaccess | altformavail | bibliography |  
            relatedmaterial| separatedmaterial | originalsloc | odd | note | otherfindaid | descgrp/*">
			<xsl:for-each select="head">
				<tr>
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td colspan="7" valign="top">
						<strong>
							<xsl:apply-templates />
						</strong>
					</td>
				</tr>
			</xsl:for-each>
			<xsl:for-each select="*[not(self::head)]">
				<tr>
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td colspan="7" valign="top">
						<xsl:apply-templates />
					</td>
				</tr>
			</xsl:for-each>
		</xsl:for-each>
		<xsl:for-each select="daogrp">
			<tr>
				<td valign="top" />
				<td valign="top" />
				<td valign="top" />
				<td valign="top" />
				<td valign="top" />
				<td colspan="7" valign="top">
					<xsl:apply-templates select="." />
				</td>
			</tr>
		</xsl:for-each>
	</xsl:template>


	<!-- ===== CO4-FLAT ===== -->
	<xsl:template name="c04-flat">
		<xsl:for-each select="did">
			<xsl:call-template name="container" />
			<tr>
				<td valign="top">
					<xsl:apply-templates select="physloc" />
				</td>
				<td valign="top">
					<xsl:apply-templates select="container" />
				</td>
				<td valign="top" />
				<td valign="top" />
				<td valign="top" />
				<td colspan="7" valign="bottom">
					<xsl:call-template name="component-did" />
				</td>
			</tr>
			<xsl:for-each select="abstract | langmaterial">
				<tr>
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td colspan="6" valign="top">
						<xsl:apply-templates />
					</td>
				</tr>
			</xsl:for-each>
		</xsl:for-each>
		<!-- Close <did> -->
		<xsl:for-each
			select="scopecontent | bioghist | arrangement | userestrict | accessrestrict | processinfo |
            acqinfo | accruals | appraisal | custodhist | phystech | controlaccess/controlaccess | altformavail | bibliography |  
            relatedmaterial| separatedmaterial | originalsloc | odd | note | otherfindaid | descgrp/*">
			<xsl:for-each select="head">
				<tr>
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td colspan="6" valign="top">
						<strong>
							<xsl:apply-templates />
						</strong>
					</td>
				</tr>
			</xsl:for-each>
			<xsl:for-each select="*[not(self::head)]">
				<tr>
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td colspan="6" valign="top">
						<xsl:apply-templates />
					</td>
				</tr>
			</xsl:for-each>
		</xsl:for-each>
		<xsl:for-each select="daogrp">
			<tr>
				<td valign="top" />
				<td valign="top" />
				<td valign="top" />
				<td valign="top" />
				<td valign="top" />
				<td valign="top" />
				<td colspan="6" valign="top">
					<xsl:apply-templates select="." />
				</td>
			</tr>
		</xsl:for-each>
	</xsl:template>

	<!-- ===== CO5-FLAT ===== -->
	<xsl:template name="c05-flat">
		<xsl:for-each select="did">
			<xsl:call-template name="container" />
			<tr>
				<td valign="top">
					<xsl:apply-templates select="physloc" />
				</td>
				<td valign="top">
					<xsl:apply-templates select="container" />
				</td>
				<td valign="top" />
				<td valign="top" />
				<td valign="top" />
				<td valign="top" />
				<td colspan="6" valign="bottom">
					<xsl:call-template name="component-did" />
				</td>
			</tr>
			<xsl:for-each select="abstract | langmaterial">
				<tr>
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td colspan="5" valign="top">
						<xsl:apply-templates />
					</td>
				</tr>
			</xsl:for-each>
		</xsl:for-each>
		<!-- Close <did> -->
		<xsl:for-each
			select="scopecontent | bioghist | arrangement | userestrict | accessrestrict | processinfo |
            acqinfo | accruals | appraisal | custodhist | phystech | controlaccess/controlaccess | altformavail | bibliography |  
            relatedmaterial| separatedmaterial | originalsloc | odd | note | otherfindaid | descgrp/*">
			<xsl:for-each select="head">
				<tr>
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td colspan="5" valign="top">
						<strong>
							<xsl:apply-templates />
						</strong>
					</td>
				</tr>
			</xsl:for-each>
			<xsl:for-each select="*[not(self::head)]">
				<tr>
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td colspan="5" valign="top">
						<xsl:apply-templates />
					</td>
				</tr>
			</xsl:for-each>
		</xsl:for-each>
		<xsl:for-each select="daogrp">
			<tr>
				<td valign="top" />
				<td valign="top" />
				<td valign="top" />
				<td valign="top" />
				<td valign="top" />
				<td valign="top" />
				<td valign="top" />
				<td colspan="5" valign="top">
					<xsl:apply-templates select="." />
				</td>
			</tr>
		</xsl:for-each>
	</xsl:template>

	<!-- ===== CO6-FLAT ===== -->
	<xsl:template name="c06-flat">
		<xsl:for-each select="did">
			<xsl:call-template name="container" />
			<tr>
				<td valign="top">
					<xsl:apply-templates select="physloc" />
				</td>
				<td valign="top">
					<xsl:apply-templates select="container" />
				</td>
				<td valign="top" />
				<td valign="top" />
				<td valign="top" />
				<td valign="top" />
				<td valign="top" />
				<td colspan="5" valign="bottom">
					<xsl:call-template name="component-did" />
				</td>
			</tr>
		</xsl:for-each>
		<!-- Close <did> -->
		<xsl:for-each select="abstract | langmaterial">
			<tr>
				<td valign="top" />
				<td valign="top" />
				<td valign="top" />
				<td valign="top" />
				<td valign="top" />
				<td valign="top" />
				<td valign="top" />
				<td valign="top" />
				<td colspan="4" valign="top">
					<xsl:apply-templates />
				</td>
			</tr>
		</xsl:for-each>
		<xsl:for-each
			select="scopecontent | bioghist | arrangement | userestrict | accessrestrict | processinfo |
            acqinfo | accruals | appraisal | custodhist | phystech | controlaccess/controlaccess | altformavail | bibliography |  
            relatedmaterial| separatedmaterial | originalsloc | odd | note | otherfindaid | descgrp/*">
			<xsl:for-each select="head">
				<tr>
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td colspan="4" valign="top">
						<strong>
							<xsl:apply-templates />
						</strong>
					</td>
				</tr>
			</xsl:for-each>
			<xsl:for-each select="*[not(self::head)]">
				<tr>
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td colspan="4" valign="top">
						<xsl:apply-templates />
					</td>
				</tr>
			</xsl:for-each>
		</xsl:for-each>
	</xsl:template>

	<!-- ===== CO7-FLAT ===== -->
	<xsl:template name="c07-flat">
		<xsl:for-each select="did">
			<xsl:call-template name="container" />
			<tr>
				<td valign="top">
					<xsl:apply-templates select="physloc" />
				</td>
				<td valign="top">
					<xsl:apply-templates select="container" />
				</td>
				<td valign="top" />
				<td valign="top" />
				<td valign="top" />
				<td valign="top" />
				<td valign="top" />
				<td valign="top" />
				<td colspan="4" valign="bottom">
					<xsl:call-template name="component-did" />
				</td>
			</tr>
			<xsl:for-each select="abstract | langmaterial">
				<tr>
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td colspan="3" valign="top">
						<xsl:apply-templates />
					</td>
				</tr>
			</xsl:for-each>
		</xsl:for-each>
		<!-- Close <did> -->
		<xsl:for-each
			select="scopecontent | bioghist | arrangement | userestrict | accessrestrict | processinfo |
            acqinfo | accruals | appraisal | custodhist | phystech | controlaccess/controlaccess | altformavail | bibliography |  
            relatedmaterial| separatedmaterial | originalsloc | odd | note | otherfindaid | descgrp/*">
			<xsl:for-each select="head">
				<tr>
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td colspan="3" valign="top">
						<strong>
							<xsl:apply-templates />
						</strong>
					</td>
				</tr>
			</xsl:for-each>
			<xsl:for-each select="*[not(self::head)]">
				<tr>
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td colspan="3" valign="top">
						<xsl:apply-templates />
					</td>
				</tr>
			</xsl:for-each>
		</xsl:for-each>
	</xsl:template>


	<!-- ===== CO8-FLAT ===== -->
	<xsl:template name="c08-flat">
		<xsl:for-each select="did">
			<xsl:call-template name="container" />
			<tr>
				<td valign="top">
					<xsl:apply-templates select="physloc" />
				</td>
				<td valign="top">
					<xsl:apply-templates select="container" />
				</td>
				<td valign="top" />
				<td valign="top" />
				<td valign="top" />
				<td valign="top" />
				<td valign="top" />
				<td valign="top" />
				<td valign="top" />
				<td colspan="3" valign="bottom">
					<xsl:call-template name="component-did" />
				</td>
			</tr>
			<xsl:for-each select="abstract | langmaterial">
				<tr>
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td colspan="2" valign="top">
						<xsl:apply-templates />
					</td>
				</tr>
			</xsl:for-each>
		</xsl:for-each>
		<!-- Close <did> -->
		<xsl:for-each
			select="scopecontent | bioghist | arrangement | userestrict | accessrestrict | processinfo |
            acqinfo | accruals | appraisal | custodhist | phystech | controlaccess/controlaccess | altformavail | bibliography |  
            relatedmaterial| separatedmaterial | originalsloc | odd | note | otherfindaid | descgrp/*">
			<xsl:for-each select="head">
				<tr>
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td colspan="2" valign="top">
						<strong>
							<xsl:apply-templates />
						</strong>
					</td>
				</tr>
			</xsl:for-each>
			<xsl:for-each select="*[not(self::head)]">
				<tr>
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td colspan="2" valign="top">
						<xsl:apply-templates />
					</td>
				</tr>
			</xsl:for-each>
		</xsl:for-each>
	</xsl:template>


	<!-- ===== C09-FLAT ===== -->
	<xsl:template name="c09-flat">
		<xsl:for-each select="did">
			<xsl:call-template name="container" />
			<tr>
				<td valign="top">
					<xsl:apply-templates select="physloc" />
				</td>
				<td valign="top">
					<xsl:apply-templates select="container" />
				</td>
				<td valign="top" />
				<td valign="top" />
				<td valign="top" />
				<td valign="top" />
				<td valign="top" />
				<td valign="top" />
				<td valign="top" />
				<td valign="top" />
				<td valign="bottom" colspan="2">
					<xsl:call-template name="component-did" />
				</td>
			</tr>
			<xsl:for-each select="abstract | langmaterial">
				<tr>
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td colspan="1" valign="top">
						<xsl:apply-templates />
					</td>
				</tr>
			</xsl:for-each>
		</xsl:for-each>
		<!-- Close <did> -->
		<xsl:for-each
			select="scopecontent | bioghist | arrangement | userestrict | accessrestrict | processinfo |
            acqinfo | accruals | appraisal | custodhist | phystech | controlaccess/controlaccess | altformavail | bibliography |  
            relatedmaterial| separatedmaterial | originalsloc | odd | note | otherfindaid | descgrp/*">
			<xsl:for-each select="head">
				<tr>
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td colspan="1" valign="top">
						<strong>
							<xsl:apply-templates />
						</strong>
					</td>
				</tr>
			</xsl:for-each>
			<xsl:for-each select="*[not(self::head)]">
				<tr>
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td valign="top" />
					<td colspan="1" valign="top">
						<xsl:apply-templates />
					</td>
				</tr>
			</xsl:for-each>
		</xsl:for-each>
	</xsl:template>


	<!-- ===== C10-FLAT ===== -->
	<xsl:template name="c10-flat">
		<xsl:for-each select="did">
			<xsl:call-template name="container" />
			<tr>
				<td valign="top">
					<xsl:apply-templates select="physloc" />
				</td>
				<td valign="top">
					<xsl:apply-templates select="container" />
				</td>
				<td valign="top" />
				<td valign="top" />
				<td valign="top" />
				<td valign="top" />
				<td valign="top" />
				<td valign="top" />
				<td valign="top" />
				<td valign="top" />
				<td valign="top" />
				<td colspan="1" valign="bottom">
					<xsl:call-template name="component-did" />
				</td>
			</tr>
		</xsl:for-each>
		<!-- Close <did> -->
	</xsl:template>


	<!-- ===== 9.E.2. TEMPLATE: COMPLEX COMPONENTS (c01-complex - c12-complex) 
		===== -->

	<!-- <c01[@level='series'] and c02[@level='subseeries'] Legacy: dsc[@id='muller'> -->

	<!-- This template is used to process c01-c12 complex components that include 
		both series at the c01 level and subseries at the c02 level and that do not 
		have associated container data. -->

	<xsl:template name="c01-complex">
		<xsl:for-each select="did">
			<h3
				style="font-size:75%; background-color:#ffffff; color:#404040;">
				<!-- <span style="font-size:1em;"> -->
				<strong>
					<!-- BEGIN collapsiblec01 Function -->
					<xsl:element name="a">
						<xsl:attribute name="href">
                                <xsl:text>javascript:changeDisplay('collapsiblec01</xsl:text><xsl:number
							from="dsc" format="1" count="c01" />'); </xsl:attribute>
						<xsl:attribute name="style">
                                <xsl:text>cursor:pointer; color: #585858; text-decoration:none;</xsl:text>
                            </xsl:attribute>
						<img class="hiddenprint" src="assets/DownArrowBlue.png"
							border="0" height="9px" align="bottom" alt="Expand/Collapse"
							hspace="0" vspace="0">
							<xsl:attribute name="id">xcollapsiblec01<xsl:number
								from="dsc" format="1" count="c01" />
                                </xsl:attribute>
						</img>&#x20;

                        <!-- END collapsiblec01 Function -->
                        <xsl:if test="unitid">
                            <xsl:value-of
                                select="translate(unitid,'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ')"/>
                            <xsl:text>&#x20;</xsl:text>
                        </xsl:if>
                        <xsl:if test="origination">
                            <xsl:value-of
                                select="translate(origination,'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ')"/>
                            <xsl:text>&#x20;</xsl:text>
                        </xsl:if>
                        <xsl:value-of
                            select="translate(unittitle,'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ')"/>
                        <xsl:text>&#x20;</xsl:text>
                        <xsl:if test="unittitle/geogname">
                            <xsl:value-of
                                select="translate(geogname,'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ')"/>
                            <xsl:text>&#x20;</xsl:text>
                        </xsl:if>
                        <xsl:if test="unittitle/imprint">
                            <xsl:value-of
                                select="translate(imprint,'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ')"/>
                            <xsl:text>&#x20;</xsl:text>
                        </xsl:if>
                        <xsl:if test="unitdate">
                            <xsl:value-of
                                select="translate(unitdate,'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ')"/>
                            <xsl:text>&#x20;</xsl:text>
                        </xsl:if>
                            <xsl:apply-templates select="physdesc"/>
                        </xsl:element>
                    </strong>
                <!-- </span> -->
            </h3>
        </xsl:for-each>

    </xsl:template>


    <!-- ===== C02-COMPLEX ===== -->

    <xsl:template name="c02-complex">
        <xsl:for-each select="did">
            <tr>
                <td valign="top" colspan="14">
                    <h4>
                        <!-- BEGIN collapsiblesubseries Function -->
                        <xsl:element name="a">
                            <xsl:attribute name="href">
                                <xsl:text>javascript:changeDisplay('collapsiblesubseries</xsl:text><xsl:number level="any" from="dsc" format="1" count="c01/c02[@level='subseries']"/>'); </xsl:attribute>
                            <xsl:attribute name="style">
                                <xsl:text>cursor:pointer; color:#3a6894; text-decoration:none;</xsl:text>
                            </xsl:attribute>
                            <img class="hiddenprint" src="assets/DownArrowBlue.png" border="0"
                                height="9px" align="bottom" alt="Expand/Collapse" hspace="0" vspace="0">
                                <xsl:attribute name="id">xcollapsiblesubseries<xsl:number level="any" from="dsc" format="1" count="c01/c02[@level='subseries']"/>
                                </xsl:attribute>
                            </img>&#x20;
                        <!-- END collapsiblesubseries Function -->
                        <xsl:if test="unitid">
                            <xsl:value-of
                                select="unitid"/>
                            <xsl:text>&#x20;</xsl:text>
                        </xsl:if>
                        <xsl:if test="origination">
                            <xsl:value-of
                                select="origination"/>
                            <xsl:text>&#x20;</xsl:text>
                            
                        </xsl:if>
                            <!--<xsl:value-of select="unittitle"/>  -->
                         
                        <xsl:value-of
                            select="unittitle"/>
                        <xsl:text>&#x20;</xsl:text>
                        <xsl:if test="unittitle/geogname">
                            <xsl:value-of
                                select="geogname"/>
                            <xsl:text>&#x20;</xsl:text>
                        </xsl:if>
                        <xsl:if test="unittitle/imprint">
                            <xsl:value-of
                                select="imprint"/>
                            <xsl:text>&#x20;</xsl:text>
                        </xsl:if>
                        <xsl:if test="unitdate">
                            <xsl:value-of
                                select="unitdate"/>
                            <xsl:text>&#x20;</xsl:text>
                        </xsl:if>
                            <xsl:if test="physdesc">
                                <xsl:value-of
                                    select="physdesc"/>
                                <xsl:text>&#x20;</xsl:text>
                            </xsl:if>
                        </xsl:element>
                    </h4>                    
                </td>
            </tr>
        </xsl:for-each> 
    </xsl:template>


    <!-- ===== C03-COMPLEX ===== -->

    <xsl:template name="c03-complex">
        <xsl:for-each select="did">
            <xsl:call-template name="container"/>
            <tr>
                <td valign="top">
                    <xsl:apply-templates select="physloc"/>
                </td>
                <td valign="top">
                    <xsl:apply-templates select="container"/>
                </td>
                <td valign="bottom" colspan="12">
                    <xsl:call-template name="component-did"/>
                </td>
            </tr>
            <xsl:for-each select="abstract | langmaterial">
                <tr>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td colspan="11" valign="top">
                        <xsl:apply-templates/>
                    </td>
                </tr>
            </xsl:for-each>
        </xsl:for-each>
        <xsl:for-each
            select="scopecontent | bioghist | arrangement | userestrict | accessrestrict | processinfo |
            acqinfo | accruals | appraisal | custodhist | phystech | controlaccess/controlaccess | altformavail | bibliography |  
            relatedmaterial| separatedmaterial | originalsloc | odd | note | otherfindaid | descgrp/*">
            <xsl:for-each select="head">
                <tr>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top" colspan="11">
                        <strong>
                            <xsl:apply-templates/>
                        </strong>
                    </td>
                </tr>
            </xsl:for-each>
            <xsl:for-each select="*[not(self::head)]">
                <tr>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top" colspan="11">
                        <xsl:apply-templates/>
                    </td>
                </tr>
            </xsl:for-each>
        </xsl:for-each>        
        <xsl:for-each select="daogrp">
            <tr>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td colspan="11" valign="top">
                    <xsl:apply-templates select="."/>
                </td>
            </tr>
        </xsl:for-each>
    </xsl:template>

    <!-- ===== C04-COMPLEX ===== -->
    <xsl:template name="c04-complex">
        <xsl:for-each select="did">
            <xsl:call-template name="container"/>
            <tr>
                <td valign="top">
                    <xsl:apply-templates select="physloc"/>
                </td>
                <td valign="top">
                    <xsl:apply-templates select="container"/>
                </td>
                <td valign="top"/>
                <td valign="bottom" colspan="11">
                    <xsl:call-template name="component-did"/>
                </td>
            </tr>
            <xsl:for-each select="abstract | langmaterial">
                <tr>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td colspan="10" valign="top">
                        <xsl:apply-templates/>
                    </td>
                </tr>
            </xsl:for-each>
        </xsl:for-each>
        <xsl:for-each
            select="scopecontent | bioghist | arrangement | userestrict | accessrestrict | processinfo |
            acqinfo | accruals | appraisal | custodhist | phystech | controlaccess/controlaccess | altformavail | bibliography |  
            relatedmaterial| separatedmaterial | originalsloc | odd | note | otherfindaid | descgrp/*">
            <xsl:for-each select="head">
                <tr>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td colspan="10">
                        <strong>
                            <xsl:apply-templates/>
                        </strong>
                    </td>
                </tr>
            </xsl:for-each>
            <xsl:for-each select="*[not(self::head)]">
                <tr>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td colspan="10">
                        <xsl:apply-templates/>
                    </td>
                </tr>
            </xsl:for-each>
        </xsl:for-each>        
        <xsl:for-each select="daogrp">
            <tr>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td colspan="10" valign="top">
                    <xsl:apply-templates select="."/>
                </td>
            </tr>
        </xsl:for-each>
    </xsl:template>

    <!-- ===== C05-COMPLEX ===== -->
    <xsl:template name="c05-complex">
        <xsl:for-each select="did">
            <xsl:call-template name="container"/>
            <tr>
                <td valign="top">
                    <xsl:apply-templates select="physloc"/>
                </td>
                <td valign="top">
                    <xsl:apply-templates select="container"/>
                </td>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="bottom" colspan="10">
                    <xsl:call-template name="component-did"/>
                </td>
            </tr>
            <xsl:for-each select="abstract | langmaterial">
                <tr>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td colspan="9" valign="top">
                        <xsl:apply-templates/>
                    </td>
                </tr>
            </xsl:for-each>
        </xsl:for-each>
        <xsl:for-each
            select="scopecontent | bioghist | arrangement | userestrict | accessrestrict | processinfo |
            acqinfo | accruals | appraisal | custodhist | phystech | controlaccess/controlaccess | altformavail | bibliography |  
            relatedmaterial| separatedmaterial | originalsloc | odd | note | otherfindaid | descgrp/*">
            <xsl:for-each select="head">
                <tr>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td colspan="9">
                        <strong>
                            <xsl:apply-templates/>
                        </strong>
                    </td>
                </tr>
            </xsl:for-each>
            <xsl:for-each select="*[not(self::head)]">
                <tr>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td colspan="9">
                        <xsl:apply-templates/>
                    </td>
                </tr>
            </xsl:for-each>
        </xsl:for-each>        
        <xsl:for-each select="daogrp">
            <tr>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td colspan="9" valign="top">
                    <xsl:apply-templates select="."/>
                </td>
            </tr>
        </xsl:for-each>
    </xsl:template>

    <!-- ===== C06-COMPLEX ===== -->
    <xsl:template name="c06-complex">
        <xsl:for-each select="did">
            <xsl:call-template name="container"/>
            <tr>
                <td valign="top">
                    <xsl:apply-templates select="physloc"/>
                </td>
                <td valign="top">
                    <xsl:apply-templates select="container"/>
                </td>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="bottom" colspan="9">
                    <xsl:call-template name="component-did"/>
                </td>
            </tr>
            <xsl:for-each select="abstract | langmaterial">
                <tr>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td colspan="8" valign="top">
                        <xsl:apply-templates/>
                    </td>
                </tr>
            </xsl:for-each>
        </xsl:for-each>
        <xsl:for-each
            select="scopecontent | bioghist | arrangement | userestrict | accessrestrict | processinfo |
            acqinfo | accruals | appraisal | custodhist | phystech | controlaccess/controlaccess | altformavail | bibliography |  
            relatedmaterial| separatedmaterial | originalsloc | odd | note | otherfindaid | descgrp/*">
            <xsl:for-each select="head">
                <tr>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td colspan="8">
                        <strong>
                            <xsl:apply-templates/>
                        </strong>
                    </td>
                </tr>
            </xsl:for-each>
            <xsl:for-each select="*[not(self::head)]">
                <tr>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td colspan="8">
                        <xsl:apply-templates/>
                    </td>
                </tr>
            </xsl:for-each>
        </xsl:for-each>        
        <xsl:for-each select="daogrp">
            <tr>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td colspan="8" valign="top">
                    <xsl:apply-templates select="."/>
                </td>
            </tr>
        </xsl:for-each>
    </xsl:template>

    <!-- ===== C07-COMPLEX ===== -->
    <xsl:template name="c07-complex">
        <xsl:for-each select="did">
            <xsl:call-template name="container"/>
            <tr>
                <td valign="top">
                    <xsl:apply-templates select="physloc"/>
                </td>
                <td valign="top">
                    <xsl:apply-templates select="container"/>
                </td>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="bottom" colspan="8">
                    <xsl:call-template name="component-did"/>
                </td>
            </tr>
        </xsl:for-each>
        <xsl:for-each select="abstract | langmaterial">
            <tr>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td colspan="7" valign="top">
                    <xsl:apply-templates/>
                </td>
            </tr>
        </xsl:for-each>
        <xsl:for-each
            select="scopecontent | bioghist | arrangement | userestrict | accessrestrict | processinfo |
            acqinfo | accruals | appraisal | custodhist | phystech | controlaccess/controlaccess | altformavail | bibliography |  
            relatedmaterial| separatedmaterial | originalsloc | odd | note | otherfindaid | descgrp/*">
            <xsl:for-each select="head">
                <tr>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td colspan="7">
                        <strong>
                            <xsl:apply-templates/>
                        </strong>
                    </td>
                </tr>
            </xsl:for-each>
            <xsl:for-each select="*[not(self::head)]">
                <tr>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td colspan="7">
                        <xsl:apply-templates/>
                    </td>
                </tr>
            </xsl:for-each>
        </xsl:for-each>
    </xsl:template>

    <!-- ===== C08-COMPLEX ===== -->
    <xsl:template name="c08-complex">
        <xsl:for-each select="did">
            <xsl:call-template name="container"/>
            <tr>
                <td valign="top">
                    <xsl:apply-templates select="physloc"/>
                </td>
                <td valign="top">
                    <xsl:apply-templates select="container"/>
                </td>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="bottom" colspan="7">
                    <xsl:call-template name="component-did"/>
                </td>
            </tr>
            <xsl:for-each select="abstract | langmaterial">
                <tr>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td colspan="6" valign="top">
                        <xsl:apply-templates/>
                    </td>
                </tr>
            </xsl:for-each>
        </xsl:for-each>
        <xsl:for-each
            select="scopecontent | bioghist | arrangement | userestrict | accessrestrict | processinfo |
            acqinfo | accruals | appraisal | custodhist | phystech | controlaccess/controlaccess | altformavail | bibliography |  
            relatedmaterial| separatedmaterial | originalsloc | odd | note | otherfindaid | descgrp/*">
            <xsl:for-each select="head">
                <tr>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td colspan="6">
                        <strong>
                            <xsl:apply-templates/>
                        </strong>
                    </td>
                </tr>
            </xsl:for-each>
            <xsl:for-each select="*[not(self::head)]">
                <tr>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td colspan="6">
                        <xsl:apply-templates/>
                    </td>
                </tr>
            </xsl:for-each>
        </xsl:for-each>
    </xsl:template>

    <!-- ===== C09-COMPLEX ===== -->
    <xsl:template name="c09-complex">
        <xsl:for-each select="did">
            <xsl:call-template name="container"/>
            <tr>
                <td valign="top">
                    <xsl:apply-templates select="physloc"/>
                </td>
                <td valign="top">
                    <xsl:apply-templates select="container"/>
                </td>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="bottom" colspan="6">
                    <xsl:call-template name="component-did"/>
                </td>
            </tr>
            <xsl:for-each select="abstract | langmaterial">
                <tr>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td colspan="5" valign="top">
                        <xsl:apply-templates/>
                    </td>
                </tr>
            </xsl:for-each>
        </xsl:for-each>
        <xsl:for-each
            select="scopecontent | bioghist | arrangement | userestrict | accessrestrict | processinfo |
            acqinfo | accruals | appraisal | custodhist | phystech | controlaccess/controlaccess | altformavail | bibliography |  
            relatedmaterial| separatedmaterial | originalsloc | odd | note | otherfindaid | descgrp/*">
            <xsl:for-each select="head">
                <tr>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td colspan="5" valign="top">
                        <strong>
                            <xsl:apply-templates/>
                        </strong>
                    </td>
                </tr>
            </xsl:for-each>
            <xsl:for-each select="*[not(self::head)]">
                <tr>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td colspan="5" valign="top">
                        <xsl:apply-templates/>
                    </td>
                </tr>
            </xsl:for-each>
        </xsl:for-each>
    </xsl:template>

    <!-- ===== C10-COMPLEX ===== -->
    <xsl:template name="c10-complex">
        <xsl:for-each select="did">
            <xsl:call-template name="container"/>
            <tr>
                <td valign="top">
                    <xsl:apply-templates select="physloc"/>
                </td>
                <td valign="top">
                    <xsl:apply-templates select="container"/>
                </td>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="bottom" colspan="5">
                    <xsl:call-template name="component-did"/>
                </td>
            </tr>
        </xsl:for-each>
        <xsl:for-each
            select="scopecontent | bioghist | arrangement | userestrict | accessrestrict | processinfo |
            acqinfo | accruals | appraisal | custodhist | phystech | controlaccess/controlaccess | altformavail | bibliography |  
            relatedmaterial| separatedmaterial | originalsloc | odd | note | otherfindaid | descgrp/*">
            <xsl:for-each select="head">
                <tr>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td colspan="4" valign="top">
                        <strong>
                            <xsl:apply-templates/>
                        </strong>
                    </td>
                </tr>
            </xsl:for-each>
            <xsl:for-each select="*[not(self::head)]">
                <tr>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td colspan="4" valign="top">
                        <xsl:apply-templates/>
                    </td>
                </tr>
            </xsl:for-each>
        </xsl:for-each>
    </xsl:template>

    <!-- ===== C11-COMPLEX ===== -->
    <xsl:template name="c11-complex">
        <xsl:for-each select="did">
            <xsl:call-template name="container"/>
            <tr>
                <td valign="top">
                    <xsl:apply-templates select="physloc"/>
                </td>
                <td valign="top">
                    <xsl:apply-templates select="container"/>
                </td>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="bottom" colspan="4">
                    <xsl:call-template name="component-did"/>
                </td>
            </tr>
        </xsl:for-each>
        <xsl:for-each
            select="scopecontent | bioghist | arrangement | userestrict | accessrestrict | processinfo |
            acqinfo | accruals | appraisal | custodhist | phystech | controlaccess/controlaccess | altformavail | bibliography |  
            relatedmaterial| separatedmaterial | originalsloc | odd | note | otherfindaid | descgrp/*">
            <xsl:for-each select="head">
                <tr>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td colspan="3" valign="top">
                        <strong>
                            <xsl:apply-templates/>
                        </strong>
                    </td>
                </tr>
            </xsl:for-each>
            <xsl:for-each select="*[not(self::head)]">
                <tr>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td colspan="3" valign="top">
                        <xsl:apply-templates/>
                    </td>
                </tr>
            </xsl:for-each>
        </xsl:for-each>
    </xsl:template>

    <!-- ===== C12-COMPLEX ===== -->
    <xsl:template name="c12-complex">
        <xsl:for-each select="did">
            <xsl:call-template name="container"/>
            <tr>
                <td valign="top">
                    <xsl:apply-templates select="physloc"/>
                </td>
                <td valign="top">
                    <xsl:apply-templates select="container"/>
                </td>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="bottom" colspan="3">
                    <xsl:call-template name="component-did"/>
                </td>
            </tr>
        </xsl:for-each>
        <xsl:for-each
            select="scopecontent | bioghist | arrangement | userestrict | accessrestrict | processinfo |
            acqinfo | accruals | appraisal | custodhist | phystech | controlaccess/controlaccess | altformavail | bibliography |  
            relatedmaterial| separatedmaterial | originalsloc | odd | note | otherfindaid | descgrp/*">
            <xsl:for-each select="head">
                <tr>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td colspan="2" valign="top">
                        <strong>
                            <xsl:apply-templates/>
                        </strong>
                    </td>
                </tr>
            </xsl:for-each>
            <xsl:for-each select="*[not(self::head)]">
                <tr>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td colspan="2" valign="top">
                        <xsl:apply-templates/>
                    </td>
                </tr>
            </xsl:for-each>
        </xsl:for-each>
    </xsl:template>



    <!-- ===== 9.E.3. TEMPLATE:  SIMPLE COMPONENTS (c01-simple - c11-simple) ===== -->

    <!-- c01[@level='series'] 
        Legacy: dsc[@id='feith']  -->

    <!-- This template is used to process c01-c11 components
         that include only series only at the c01 level
         and that do not have associated container data.  -->

    <!-- ===== C02-SIMPLE ===== -->
    <xsl:template name="c02-simple">
        <xsl:for-each select="did">
            <xsl:call-template name="container"/>
            <tr>
                <td valign="top">
                    <xsl:apply-templates select="physloc"/>
                </td>
                <td valign="top">
                    <xsl:apply-templates select="container"/>
                </td>
                <td valign="bottom" colspan="12">
                    <xsl:call-template name="component-did"/>
                </td>
            </tr>
            <xsl:for-each select="abstract | langmaterial">
                <tr>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td colspan="11" valign="top">
                        <xsl:apply-templates/>
                    </td>
                </tr>
            </xsl:for-each>
        </xsl:for-each>
        <!-- Close <did> -->
        <xsl:for-each
            select="scopecontent | bioghist | arrangement | userestrict | accessrestrict | processinfo |
            acqinfo | accruals | appraisal | custodhist | phystech | controlaccess/controlaccess | altformavail | bibliography |  
            relatedmaterial| separatedmaterial | originalsloc | odd | note | otherfindaid | descgrp/*">
            <xsl:for-each select="head">
                <tr>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td colspan="11" valign="top">
                        <strong>
                            <xsl:apply-templates/>
                        </strong>
                    </td>
                </tr>
            </xsl:for-each>
            <xsl:for-each select="*[not(self::head)]">
                <tr>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td colspan="11" valign="top">
                        <xsl:apply-templates/>
                     </td>
                </tr>
            </xsl:for-each>
        </xsl:for-each>        
        <xsl:for-each select="daogrp">
            <tr>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td colspan="11" valign="top">
                    <xsl:apply-templates select="."/>
                </td>
            </tr>
        </xsl:for-each>
    </xsl:template>

    <!-- ===== C03-SIMPLE ===== -->
    <xsl:template name="c03-simple">
        <xsl:for-each select="did">
            <xsl:call-template name="container"/>
            <tr>
                <td valign="top">
                    <xsl:apply-templates select="physloc"/>
                </td>
                <td valign="top">
                    <xsl:apply-templates select="container"/>
                </td>
                <td valign="top"/>
                <td valign="bottom" colspan="11">
                    <xsl:call-template name="component-did"/>
                </td>
            </tr>
            <xsl:for-each select="abstract | langmaterial">
                <tr>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td colspan="10" valign="top">
                        <xsl:apply-templates/>
                    </td>
                </tr>
            </xsl:for-each>
        </xsl:for-each>
        <!-- Close <did> -->
        <xsl:for-each
            select="scopecontent | bioghist | arrangement | userestrict | accessrestrict | processinfo |
            acqinfo | accruals | appraisal | custodhist | phystech | controlaccess/controlaccess | altformavail | bibliography |  
            relatedmaterial| separatedmaterial | originalsloc | odd | note | otherfindaid | descgrp/*">
            <xsl:for-each select="head">
                <tr>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td colspan="10" valign="top">
                        <strong>
                            <xsl:apply-templates/>
                        </strong>
                    </td>
                </tr>
            </xsl:for-each>
            <xsl:for-each select="*[not(self::head)]">
                <tr>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td colspan="10" valign="top">
                        <xsl:apply-templates/>
                    </td>
                </tr>
            </xsl:for-each>
        </xsl:for-each>        
        <xsl:for-each select="daogrp">
            <tr>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td colspan="10" valign="top">
                    <xsl:apply-templates select="."/>
                </td>
            </tr>
        </xsl:for-each>
    </xsl:template>


    <!-- ===== C04-SIMPLE ===== -->
    <xsl:template name="c04-simple">
        <xsl:for-each select="did">
            <xsl:call-template name="container"/>
            <tr>
                <td valign="top">
                    <xsl:apply-templates select="physloc"/>
                </td>
                <td valign="top">
                    <xsl:apply-templates select="container"/>
                </td>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="bottom" colspan="10">
                    <xsl:call-template name="component-did"/>
                </td>
            </tr>
            <xsl:for-each select="abstract | langmaterial">
                <tr>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td colspan="9" valign="top">
                        <xsl:apply-templates/>
                    </td>
                </tr>
            </xsl:for-each>
        </xsl:for-each>
        <!-- Close <did> -->
        <xsl:for-each
            select="scopecontent | bioghist | arrangement | userestrict | accessrestrict | processinfo |
            acqinfo | accruals | appraisal | custodhist | phystech | controlaccess/controlaccess | altformavail | bibliography |  
            relatedmaterial| separatedmaterial | originalsloc | odd | note | otherfindaid | descgrp/*">
            <xsl:for-each select="head">
                <tr>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td colspan="9" valign="top">
                        <strong>
                            <xsl:apply-templates/>
                        </strong>
                    </td>
                </tr>
            </xsl:for-each>
            <xsl:for-each select="*[not(self::head)]">
                <tr>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td colspan="9" valign="top">
                        <xsl:apply-templates/>
                    </td>
                </tr>
            </xsl:for-each>
        </xsl:for-each>        
        <xsl:for-each select="daogrp">
            <tr>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td colspan="9" valign="top">
                    <xsl:apply-templates select="."/>
                </td>
            </tr>
        </xsl:for-each>
    </xsl:template>


    <!-- ===== C05-SIMPLE ===== -->
    <xsl:template name="c05-simple">
        <xsl:for-each select="did">
            <xsl:call-template name="container"/>
            <tr>
                <td valign="top">
                    <xsl:apply-templates select="physloc"/>
                </td>
                <td valign="top">
                    <xsl:apply-templates select="container"/>
                </td>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="bottom" colspan="9">
                    <xsl:call-template name="component-did"/>
                </td>
            </tr>
            <xsl:for-each select="abstract | langmaterial">
                <tr>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td colspan="8" valign="top">
                        <xsl:apply-templates/>
                    </td>
                </tr>
            </xsl:for-each>
        </xsl:for-each>
        <!-- Close <did> -->
        <xsl:for-each
            select="scopecontent | bioghist | arrangement | userestrict | accessrestrict | processinfo |
            acqinfo | accruals | appraisal | custodhist | phystech | controlaccess/controlaccess | altformavail | bibliography |  
            relatedmaterial| separatedmaterial | originalsloc | odd | note | otherfindaid | descgrp/*">
            <xsl:for-each select="head">
                <tr>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td colspan="8" valign="top">
                        <strong>
                            <xsl:apply-templates/>
                        </strong>
                    </td>
                </tr>
            </xsl:for-each>
            <xsl:for-each select="*[not(self::head)]">
                <tr>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td colspan="8" valign="top">
                        <xsl:apply-templates/>
                    </td>
                </tr>
            </xsl:for-each>
        </xsl:for-each>        
        <xsl:for-each select="daogrp">
            <tr>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td colspan="8" valign="top">
                    <xsl:apply-templates select="."/>
                </td>
            </tr>
        </xsl:for-each>
    </xsl:template>

    <!-- ===== C06-SIMPLE ===== -->
    <xsl:template name="c06-simple">
        <xsl:for-each select="did">
            <xsl:call-template name="container"/>
            <tr>
                <td valign="top">
                    <xsl:apply-templates select="physloc"/>
                </td>
                <td valign="top">
                    <xsl:apply-templates select="container"/>
                </td>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="bottom" colspan="8">
                    <xsl:call-template name="component-did"/>
                </td>
            </tr>
            <xsl:for-each select="abstract | langmaterial">
                <tr>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td colspan="7" valign="top">
                        <xsl:apply-templates/>
                    </td>
                </tr>
            </xsl:for-each>
        </xsl:for-each>
        <!-- Close <did> -->
        <xsl:for-each
            select="scopecontent | bioghist | arrangement | userestrict | accessrestrict | processinfo |
            acqinfo | accruals | appraisal | custodhist | phystech | controlaccess/controlaccess | altformavail | bibliography |  
            relatedmaterial| separatedmaterial | originalsloc | odd | note | otherfindaid | descgrp/*">
            <xsl:for-each select="head">
                <tr>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td colspan="7" valign="top">
                        <strong>
                            <xsl:apply-templates/>
                        </strong>
                    </td>
                </tr>
            </xsl:for-each>
            <xsl:for-each select="*[not(self::head)]">
                <tr>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td colspan="7" valign="top">
                        <xsl:apply-templates/>
                    </td>
                </tr>
            </xsl:for-each>
        </xsl:for-each>
    </xsl:template>

    <!-- ===== C07-SIMPLE ===== -->
    <xsl:template name="c07-simple">
        <xsl:for-each select="did">
            <xsl:call-template name="container"/>
            <tr>
                <td valign="top">
                    <xsl:apply-templates select="physloc"/>
                </td>
                <td valign="top">
                    <xsl:apply-templates select="container"/>
                </td>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="bottom" colspan="7">
                    <xsl:call-template name="component-did"/>
                </td>
            </tr>
            <xsl:for-each select="abstract | langmaterial">
                <tr>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td colspan="6" valign="top">
                        <xsl:apply-templates/>
                    </td>
                </tr>
            </xsl:for-each>
        </xsl:for-each>
        <!-- Close <did> -->
        <xsl:for-each
            select="scopecontent | bioghist | arrangement | userestrict | accessrestrict | processinfo |
            acqinfo | accruals | appraisal | custodhist | phystech | controlaccess/controlaccess | altformavail | bibliography |  
            relatedmaterial| separatedmaterial | originalsloc | odd | note | otherfindaid | descgrp/*">
            <xsl:for-each select="head">
                <tr>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td colspan="6" valign="top">
                        <strong>
                            <xsl:apply-templates/>
                        </strong>
                    </td>
                </tr>
            </xsl:for-each>
            <xsl:for-each select="*[not(self::head)]">
                <tr>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td colspan="6" valign="top">
                        <xsl:apply-templates/>
                    </td>
                </tr>
            </xsl:for-each>
        </xsl:for-each>
    </xsl:template>

    <!-- ===== C08-SIMPLE ===== -->
    <xsl:template name="c08-simple">
        <xsl:for-each select="did">
            <xsl:call-template name="container"/>
            <tr>
                <td valign="top">
                    <xsl:apply-templates select="physloc"/>
                </td>
                <td valign="top">
                    <xsl:apply-templates select="container"/>
                </td>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="bottom" colspan="6">
                    <xsl:call-template name="component-did"/>
                </td>
            </tr>
            <xsl:for-each select="abstract | langmaterial">
                <tr>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td colspan="5" valign="top">
                        <xsl:apply-templates/>
                    </td>
                </tr>
            </xsl:for-each>
        </xsl:for-each>
        <!-- Close <did> -->
        <xsl:for-each
            select="scopecontent | bioghist | arrangement | userestrict | accessrestrict | processinfo |
            acqinfo | accruals | appraisal | custodhist | phystech | controlaccess/controlaccess | altformavail | bibliography |  
            relatedmaterial| separatedmaterial | originalsloc | odd | note | otherfindaid | descgrp/*">
            <xsl:for-each select="head">
                <tr>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td colspan="5" valign="top">
                        <strong>
                            <xsl:apply-templates/>
                        </strong>
                    </td>
                </tr>
            </xsl:for-each>
            <xsl:for-each select="*[not(self::head)]">
                <tr>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td colspan="5" valign="top">
                        <xsl:apply-templates/>
                    </td>
                </tr>
            </xsl:for-each>
        </xsl:for-each>
    </xsl:template>

    <!-- ===== C09-SIMPLE ===== -->
    <xsl:template name="c09-simple">
        <xsl:for-each select="did">
            <xsl:call-template name="container"/>
            <tr>
                <td valign="top">
                    <xsl:apply-templates select="physloc"/>
                </td>
                <td valign="top">
                    <xsl:apply-templates select="container"/>
                </td>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="bottom" colspan="5">
                    <xsl:call-template name="component-did"/>
                </td>
            </tr>
        </xsl:for-each>
        <!-- Close <did> -->
        <xsl:for-each select="abstract | langmaterial">
            <tr>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td colspan="4" valign="top">
                    <xsl:apply-templates/>
                </td>
            </tr>
        </xsl:for-each>
        <xsl:for-each
            select="scopecontent | bioghist | arrangement | userestrict | accessrestrict | processinfo |
            acqinfo | accruals | appraisal | custodhist | phystech | controlaccess/controlaccess | altformavail | bibliography |  
            relatedmaterial| separatedmaterial | originalsloc | odd | note | otherfindaid | descgrp/*">
            <xsl:for-each select="head">
                <tr>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td colspan="4" valign="top">
                        <strong>
                            <xsl:apply-templates/>
                        </strong>
                    </td>
                </tr>
            </xsl:for-each>
            <xsl:for-each select="*[not(self::head)]">
                <tr>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td colspan="4" valign="top">
                        <xsl:apply-templates/>
                    </td>
                </tr>
            </xsl:for-each>
        </xsl:for-each>
    </xsl:template>

    <!-- ===== C10-SIMPLE ===== -->
    <xsl:template name="c10-simple">
        <xsl:for-each select="did">
            <xsl:call-template name="container"/>
            <tr>
                <td valign="top">
                    <xsl:apply-templates select="physloc"/>
                </td>
                <td valign="top">
                    <xsl:apply-templates select="container"/>
                </td>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="bottom" colspan="4">
                    <xsl:call-template name="component-did"/>
                </td>
            </tr>
            <xsl:for-each select="abstract | langmaterial">
                <tr>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td colspan="3" valign="top">
                        <xsl:apply-templates/>
                    </td>
                </tr>
            </xsl:for-each>
        </xsl:for-each>
        <!-- Close <did> -->
        <xsl:for-each
            select="scopecontent | bioghist | arrangement | userestrict | accessrestrict | processinfo |
            acqinfo | accruals | appraisal | custodhist | phystech | controlaccess/controlaccess | altformavail | bibliography |  
            relatedmaterial| separatedmaterial | originalsloc | odd | note | otherfindaid | descgrp/*">
            <xsl:for-each select="head">
                <tr>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td colspan="3" valign="top">
                        <strong>
                            <xsl:apply-templates/>
                        </strong>
                    </td>
                </tr>
            </xsl:for-each>
            <xsl:for-each select="*[not(self::head)]">
                <tr>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td colspan="3" valign="top">
                        <xsl:apply-templates/>
                    </td>
                </tr>
            </xsl:for-each>
        </xsl:for-each>
    </xsl:template>

    <!-- ===== C11-SIMPLE ===== -->
    <xsl:template name="c11-simple">
        <xsl:for-each select="did">
            <xsl:call-template name="container"/>
            <tr>
                <td valign="top">
                    <xsl:apply-templates select="physloc"/>
                </td>
                <td valign="top">
                    <xsl:apply-templates select="container"/>
                </td>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="top"/>
                <td valign="bottom" colspan="3">
                    <xsl:call-template name="component-did"/>
                </td>
            </tr>
            <xsl:for-each select="abstract | langmaterial">
                <tr>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td valign="top"/>
                    <td colspan="2" valign="top">
                        <xsl:apply-templates/>
                    </td>
                </tr>
            </xsl:for-each>
        </xsl:for-each>
        <!-- Close <did> -->
    </xsl:template>


    <!-- ========== SECTION 5: RELATED MATERIAL <relatedmaterial> ========== -->
    <xsl:template match="archdesc/relatedmaterial">
        <a name="a5"/>
        <h2>
            <!-- BEGIN collapsiblerelatedmaterial Function -->
            <xsl:element name="a">
                <xsl:attribute name="href">
                    <xsl:text>javascript:changeDisplay('collapsiblerelatedmaterial');</xsl:text>
                </xsl:attribute>
                <xsl:attribute name="style">
                    <xsl:text>cursor:pointer; color:#404040; text-decoration:none;</xsl:text>
                </xsl:attribute>
                <img id="xcollapsiblerelatedmaterial" class="hiddenprint"
                    src="assets/DownArrowBlue.png" border="0" height="10px" align="bottom" alt="Expand/Collapse" hspace="0" vspace="0"/>
                <xsl:value-of select="head"/>
            </xsl:element>
            <!-- END  collapsiblerelatedmaterialFunction -->            
        </h2>
        <!-- BEGIN collapsiblerelatedmaterial Division -->
        <div id="collapsiblerelatedmaterial" class="showhide" style="display:block;">
            <xsl:for-each select="p">
                <p>
                    <xsl:apply-templates/>
                </p>
            </xsl:for-each>
            <p/>
            <p>
                <a href="#a0" class="hiddenprint">Return to top</a>
            </p>
        </div>
        <!-- END collapsiblerelatedmaterial Division-->
    </xsl:template>
    
    <!-- ========== SECTION 5a: SEPARATED MATERIAL <separatedmaterial> ========== -->
    <xsl:template match="archdesc/separatedmaterial">
        <a name="a5a"/>
        <h2>
            <!-- BEGIN collapsibleseparatedmaterial Function -->
            <xsl:element name="a">
                <xsl:attribute name="href">
                    <xsl:text>javascript:changeDisplay('collapsibleseparatedmaterial');</xsl:text>
                </xsl:attribute>
                <xsl:attribute name="style">
                    <xsl:text>cursor:pointer; color:#404040; text-decoration:none</xsl:text>
                </xsl:attribute>
                <img id="xcollapsibleseparatedmaterial" class="hiddenprint"
                    src="assets/DownArrowBlue.png" border="0" height="10px" align="bottom" alt="Expand/Collapse" hspace="0" vspace="0"/>
                <xsl:value-of select="head"/>
            </xsl:element>
            <!-- END collapsibleseparatedmaterial Function  -->
        </h2>
        <!-- BEGIN collapsibleseparatedmaterial Division -->
        <div id="collapsibleseparatedmaterial" class="showhide" style="display:block;">
            <xsl:for-each select="p">
                <p>
                    <xsl:apply-templates/>
                </p>
            </xsl:for-each>
            <p/>
            <p>
                <a href="#a0" class="hiddenprint">Return to top</a>
            </p>
        </div>
        <!-- END collapsibleseparatedmaterial Division -->
    </xsl:template>
    
    
    <!-- ========== SECTION 7: CATALOG HEADINGS <controlaccess> ========== -->
    <xsl:template match="archdesc/controlaccess">
        <a name="a7"/>
        <h2>
            <!-- BEGIN collapsiblecontrolaccess Function -->
            <xsl:element name="a">
                <xsl:attribute name="href">
                    <xsl:text>javascript:changeDisplay('collapsiblecontrolaccess');</xsl:text>
                </xsl:attribute>
                <xsl:attribute name="style">
                    <xsl:text>cursor:pointer; color:#404040; text-decoration:none;</xsl:text>
                </xsl:attribute>
                <img id="xcollapsiblecontrolaccess" class="hiddenprint"
                    src="assets/DownArrowBlue.png" border="0" height="10px"  align="bottom" alt="Expand/Collapse" hspace="0" vspace="0"/>
                <xsl:value-of select="head"/>
            </xsl:element>
        </h2>
        <!-- BEGIN collapsiblecontrolaccess Division -->
        <div id="collapsiblecontrolaccess" class="showhide" style="display:block;">

            <!-- <xsl:for-each select="p">
                <p>
                    <xsl:apply-templates/>
                </p>
            </xsl:for-each> -->
            <xsl:for-each select="controlaccess">
                <dl>
                    <xsl:for-each select="head">
                        <dt style="margin-left:30px; margin-bottom:4px; font-size:11px;"><strong style="color:#404040;">
                            <xsl:apply-templates/>
                        </strong></dt> 
                    </xsl:for-each>
                    <xsl:for-each
                        select="subject |corpname | persname | famname | genreform | title | geogname | occupation | function">
                        <dd style="margin-left:60px;">
                            <xsl:apply-templates/>
                        </dd>
                    </xsl:for-each>
                </dl>
            </xsl:for-each>
            <br clear="all"/>
            <p>
                <a href="#a0" class="hiddenprint">Return to top</a>
            </p>
        </div>
        <!-- END collapsiblecontrolaccess Division -->
    </xsl:template>

    <!-- ========== END EAD TRANSFORMATION ========== -->
    

</xsl:stylesheet>