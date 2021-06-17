https://www.loc.gov/ead/EAD3taglib/EAD3.htm
and https://www.loc.gov/ead/ead3_undeprecated.dtd


**simple elements**

<!ELEMENT descriptivenote (p)+>


(#PCDATA|abbr|emph|expan|foreign|lb|ptr|ref)*

used for: 

titleproper subtitle author sponsor edition publisher citation container didnote materialspec physloc unitdate unitid label head01 head02 head03 addressline head datesingle fromdate todate date emph num quote extent 


<!ELEMENT physloc (#PCDATA|abbr|emph|expan|foreign|lb|ptr|ref)*>


<!ELEMENT div (
	head?,
		<!ELEMENT head (
			#PCDATA
			|abbr
			|emph
			|expan
			|foreign
			|lb
			|ptr
			|ref
		)*>
	(
		chronlist
		|list
		|table
		|blockquote
		|p
	)*,
	div*
)>

** Encoded Archival Description **

<!ELEMENT ead (
	control,
	archdesc
)>


<!ELEMENT control (
	recordid,
	otherrecordid*,
	representation*,
	filedesc, 
	maintenancestatus,
	publicationstatus?,
    maintenanceagency,
    languagedeclaration*,
    conventiondeclaration*,
    rightsdeclaration*,
    localtypedeclaration*,
    localcontrol*,
    maintenancehistory,
    sources?
 )>

** Archival Description **

<!ELEMENT archdesc (
	did,
	(
		accessrestrict
		|accruals
		|acqinfo
		|altformavail
		|appraisal
		|arrangement
		|bibliography
		|bioghist
		|controlaccess
        |custodhist
        |fileplan
        |index
        |legalstatus
        |odd
        |originalsloc
       	|otherfindaid
       	|phystech
       	|prefercite
        |processinfo
        |relatedmaterial
        |relations
        |scopecontent
        |separatedmaterial
        |userestrict
        |dsc
    )*
)>
	
** Descriptive Identification **

<!ELEMENT did (    
	head?,
	(
		abstract
		|container
		|dao
		|daoset
		|didnote
		|langmaterial
        |materialspec
        |origination
        |physdescset
        |physdesc
        |physdescstructured
        |physloc
        |repository
        |unitdate
        |unitdatestructured
        |unitid
        |unittitle
    )
 +)>

** Digital Archival Object **

<!ELEMENT dao (descriptivenote)?> 
 ATTR: daotype (borndigital|derived|unknown|otherdaotype) #REQUIRED


<!ELEMENT physdescstructured (
	quantity,
	unittype,
	(
		physfacet
		|dimensions
	)*,
    descriptivenote?
 )>


** Description of Subordinate Components **

<!ELEMENT dsc (
	head?,(
		chronlist
		|list
		|table
		|blockquote
		|p
	)*,
	thead?,
	(c+|c01+)?
)>

** general container **

<!ELEMENT c (
	(
		head?,
		did,(
			accessrestrict
			|accruals
			|acqinfo
            |altformavail
            |appraisal
            |arrangement
            |bibliography
            |bioghist
            |controlaccess
            |custodhist
            |descgrp
            |fileplan
            |index
            |legalstatus
            |odd
            |originalsloc
            |otherfindaid
            |phystech
            |prefercite
            |processinfo
            |relatedmaterial
            |relations
            |scopecontent
            |separatedmaterial
            |userestrict
         )*
	),
	(thead?,c+)*
)>

ATTR:  level (class|collection|file|fonds|item|otherlevel|recordgrp|series
         |subfonds|subgrp|subseries) #IMPLIED
         
usage:
Use @base to specify a base URI other than the URI of the EAD instance for the purpose of resolving any relative URIs contained within <c>.


** top-level container **

<!ELEMENT c01 ( ... (thread?,c02+)* )>

same as c, but ends with zero or more C02 nested

** bibliography **

<!ELEMENT bibliography (head?,
                        (chronlist|list|table|blockquote|p|bibliography|archref|bibref)+)>

** relatedmaterial **

<!ELEMENT relatedmaterial (head?,
                           (chronlist|list|table|blockquote|p|relatedmaterial|archref
                            |bibref)+)>

** bibref **

<!ELEMENT bibref (
	#PCDATA
	|abbr
	|emph
	|expan
	|foreign
	|lb
	|ptr
	|ref
	|persname
	|corpname
	|famname
	|geogname
	|name
	|occupation
    |subject
    |genreform
    |function
    |title
    |date
    |footnote
    |num
    |quote
 )*>

<!ELEMENT ref (
	#PCDATA
	|abbr
	|expan
	|emph
	|foreign
	|lb
	|ptr
	|quote
	|num
	|footnote
    |date
    |persname
    |corpname
    |famname
    |geogname
    |name
    |occupation
    |subject
    |genreform
    |function
    |title
 )*>



@lastdatetimeverified
Last Date and Time Verified [toc]
Summary:
Last date or last date and time the linked resource was verified. Verification may include link resolution as well as verification of the version of the linked object. Available in <citation>, <relation>, <source>, and <term>.
Data Type:
Constrained to the following patterns: YYYY-MM-DD, YYYY-MM, YYYY, or YYYY-MM-DDThh:mm:ss with optional timezone offset from UTC in the form of +|- hh:mm, or "Z" to indicate the dateTime is UTC. No timezone implies the dateTime is UTC.


@level
Level [toc]
Summary:
The hierarchical level of the materials being described by the element. This attribute is available in <archdesc>, where the highest level of material represented in the finding aid must be declared (e.g., collection, fonds, record group), and in <c> and <c01>-<c12>, where it may be used to declare the level of description represented by each component (e.g., subgroup, series, file). If none of the values in the semi-closed list are appropriate, the value "otherlevel" may be chosen and some other value specified in otherlevel.

@linkrole 
Link Role [toc]
Summary: A URI that characterizes the nature of the remote resource to which a linking element refers.
Data Type: anyURI
Example: <representation href="http://drs.library.yale.edu:8083/fedora/get/beinecke:jonesss/PDF" linkrole="application/pdf">PDF version of finding aid</representation>




** Relations and Relation **


<relations>
Relations [toc]
Summary:
An element that groups one or more <relation> elements, which identify external entities and characterize the nature of their relationships to the materials being described.
See also:
<controlaccess>, which binds together elements containing access headings from controlled vocabularies related to the described materials.



<relation>
Relation [toc]
Summary:
A child element of <relations> for describing a relationship between the materials described in the EAD instance and a related entity.
Attribute usage:
Use @relationtype to specify the kind of relationship being encoded.



Use @otherrelationtype to specify the alternate type of relationship, when @relationtype is set to "otherrelationtype"



Use @arcrole to supply a URI that describes the nature of the relationship between the materials being described and the related entity.



** Controlled Access Headings **


<controlaccess>
Controlled Access Headings [toc]
Summary:
An element that binds together elements containing access headings for the described materials.
See also:
<relations> contains one or more <relation> elements that identify an external entity or concept, and describe the nature of the relationship of the described materials to that entity or concept.



May contain:
blockquote, chronlist, controlaccess, corpname, famname, function, genreform, geogname, head, list, name, occupation, p, persname, subject, table, title
May occur within:
archdesc, c, c01, c02, c03, c04, c05, c06, c07, c08, c09, c10, c11, c12, controlaccess
Description and Usage:
Use <controlaccess> to bundle in a single group access points — names, topics, places, functions, occupations, titles, and genre terms — that represent the contexts and contents of the materials described. Although <controlaccess> is often used within <archdesc> to provide significant access terms for the entirety of the materials described, it may be used at the component level to provide terms specific to a component if so desired.
<controlaccess> helps to enable authority-controlled searching across finding aids, particularly when its children contain terms drawn from nationally or internationally controlled vocabularies such as the Library of Congress Subject Headings (LCSH) or the UK Archival Thesaurus (UKAT) for topics, the Virtual International Authority File (VIAF) for names, or GeoNames for places.



** @encodinganalog **


@encodinganalog
Encoding Analog [toc]
Summary:
A field or element in another descriptive encoding system to which an EAD element or attribute is comparable. Mapping elements from one system to another enables creation of a single user interface that can index comparable information across multiple schemas. The mapping designations may also enable a repository to harvest selected data from a finding aid, for example, to build a basic catalog record, or OAI-PMH compliant Dublin Core record. The relatedencoding attribute may be used in <ead>, <control>, or <archdesc> to identify the encoding system from which fields are specified in encodinganalog. If relatedencoding is not used, then include the system designation in encodinganalog.
Data Type:
token
Examples:
<origination>
<corpname encodinganalog="MARC21 110">
<part>Waters Studio</part>

</corpname>

</origination>



<archdesc relatedencoding="MARC21">
<origination>
<persname encodinganalog="100$a$q$d$e" source="lcnaf">
<part>Waters, E. C. (Elizabeth Cat), 1870-1944, photographer</part>

</persname>







** Convention Declaration **


<conventiondeclaration>
Convention Declaration [toc]
Summary:
An optional child element of <control>, used to bind together <citation> with optional <abbr> and <descriptivenote> elements that identify rules or conventions applied in compiling the description.
See also:
Use <localtypedeclaration> to identify local values used in @localtype attributes.



May contain:
abbr, citation, descriptivenote
May occur within:
control
Description and Usage:
A statement about any rules or conventions used in constructing the description. Examples include content standards, controlled vocabularies, or thesauri.
You may use <conventiondeclaration> to:
identify any rules used to formulate the content of controlled access terms and referenced in @rules.



identify any controlled vocabularies used to populate controlled access terms and referenced in @source.



identify any related encoding schemes referenced in @relatedencoding.



specify standards used to formulate data elements or provide codes.

<conventiondeclaration> should always be included when @langencoding, @scriptencoding, @dateencoding, @countryencoding, or @repositoryencoding are set to the "other" value.



Each additional rule or set of rules, controlled vocabulary, or standard should be contained in a separate <conventiondeclaration>.
It may not be necessary to include <conventiondeclaration> in such cases where the above scenarios are addressed in local or consortial documentation.
<abbr> may be used to identify the standard or controlled vocabulary in a coded structure. The content of <abbr> should be the same value given to rules, source, or relatedencoding when referencing a given convention. Any notes relating to how these rules or conventions have been used may be given within <descriptivenote>.
The prescribed order of all child elements (both required and optional) is:
<abbr>



<citation>



<descriptivenote>



References:
ISAD(G) 3.7.2
MODS <descriptionStandard>
Attributes:
altrender
Optional
audience
Optional (values limited to: external, internal)
encodinganalog
Optional
id
Optional
lang
Optional
localtype
Optional
script
Optional
Availability:
Optional, repeatable
Examples:
<control> [. . .]
<conventiondeclaration>
<abbr>ISAD(G)</abbr>

<citation>ISAD(G): General International Standard Archival Description, second edition, Ottawa 2000</citation>

</conventiondeclaration>

<conventiondeclaration>
<abbr>NCARules</abbr>

<citation>National Council on Archives, Rules for the Construction of Personal, Place and Corporate Names, 1997</citation>

</conventiondeclaration>

<conventiondeclaration>
<citation>ISO 8601 - Data elements and interchange formats - Information interchange - Representation of dates and times, 2nd ed., Geneva: International Standards Organization, 2000</citation>

</conventiondeclaration>

[. . .] </control>



<control> [. . .]
<conventiondeclaration>
<abbr>DACS</abbr>

<citation href="http://www2.archivists.org/standards/DACS" lastdatetimeverified="2015-07-02T16:30:21-5:00" linktitle="DACS in HTML on SAA website" actuate="onload" show="new">Describing Archives: a Content Standard</citation>

<descriptivenote>
<p>DACS was used as the primary description standard.</p>

</descriptivenote>

</conventiondeclaration>

[. . .] </control>



</origination>

</archdesc>


