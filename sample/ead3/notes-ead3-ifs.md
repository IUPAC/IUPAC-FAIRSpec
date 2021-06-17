https://www.loc.gov/ead/EAD3taglib/EAD3.htm
and https://www.loc.gov/ead/ead3_undeprecated.dtd


** minimal EAD for IFS **


<!ELEMENT ead (
	control,
	frontmatter,
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

<!ELEMENT frontmatter (
	titlepage?,
		<!ELEMENT titlepage (
			chronlist
			|list
			|table
			|blockquote
			|p
			|author
			|date
			|edition
			|num
			|publisher
			|bibseries
			|sponsor
			|titleproper
			|subtitle
		)+>
	div*
	
)>

** Archival Description **

<!ELEMENT archdesc (
	runner*,
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



