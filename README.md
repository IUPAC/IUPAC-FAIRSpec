![logo](https://iupac.org/wp-content/themes/iupac/dist/images/logo.png)

last updated 2021-09-01

# IUPAC-FAIRSpec

Welcome to the GitHub development and demonstration project for the 
[IUPAC Project 2019-031-1-024 Development of a Standard for FAIR Data Management for Spectroscopic Data](https://iupac.org/projects/project-details/?project_nr=2019-031-1-024). Our current working specification can be found as a [Google Doc](https://docs.google.com/document/d/1WYB3f04dFdVzlvf7aEwdVNwEwLpQ7YBAA00pGbc8Jp0/edit?usp=sharing). A demonstration of IUPAC FAIRSpec finding aids and their application is at https://chemapps.stolaf.edu/iupac/demo/demo.htm, with files at https://chemapps.stolaf.edu/iupac/site/ifs.

This GitHub project provides a reference Java implementation of the [IUPAC FAIRSpec Standard](https://docs.google.com/document/d/1WYB3f04dFdVzlvf7aEwdVNwEwLpQ7YBAA00pGbc8Jp0/edit?usp=sharing) as a Java library as well as a reference Java implementation of an "IUPAC FAIRSpec data and metadata extractor". It is currently under intensely active development. It is *very preliminary* and, though public, is only meant for demonstration purposes. **Please do not implement these preliminary standards** as they are expected to change day by day throughout 2021. 

The principal goal of the project is to define standardized metadata associated with complex collections of spectroscopic data in the area of chemistry -- NMR, IR, Raman, MS, etc. The specification is modular and has been worked out primarily in the area of NMR spectroscopy at this time. 

<img src="https://lh3.googleusercontent.com/oPq4z8xhDHOpvEaudhotW-fl5MxeR5DKe9JUMIlcoAzRcCOyi192vago4BJ8-FrP1qUs3B-tLT-mZgFgKJF_ozw6ZCLTcS6thpix4509qNr0dFteuHdWY4vpWS6uxkTkx5KNXGYI" width="500"/>

It is the IUPAC FAIRSpec Finding Aid that, when [represented as JSON](https://chemapps.stolaf.edu/iupac/site/ifs/acs.joc.0c00770._IFS_findingaid.json) (in this case) or XML (leaving that for others for now), along with the extracted collection forms the basis of what we are calling "FAIR Data Management of Spectroscopic Data." 
 
If you just want to get an idea of what the "data extractor" does and not install anything yourself, see the demo at [St. Olaf College](https://chemapps.stolaf.edu/iupac/demo/demo.htm). It's still rather very crude, but it should give you an idea of what we are about. 

## Reference Implementation

The code here is an Eclipse Java project. If you want to clone it, feel free. Check it out. Run the test. Even suggest changes. Contribute. Since it is quite a preliminary project, don't get too frustrated if it doesn't work for you. It probably means I have forgotten to mention some aspsect of its implmeentation. Please contact Bob Hanson (hansonr@stolaf.edu) if you want some help. We'd like to hear from you.

The reference implementation consists of two main parts -- a Java library of mostly abstract classes that define the basics of the IUPAC FAIRSpec schema, and an imiplemenation of a "data and metadata extractor" that can produce IUPAC FAIRSpec Collections and their associated IUPAC FAIRSpec Finding Aids in JSON format.

The basic demo (src/com/integratedgraphics/ifs/ExtractorTest.java) takes a monolithic ZIP file (30-200MB) provided by authors as supporting information for manuscripts accepted by the Journal of Organic Chemistry and Organic Letters and extracts [Digital Objects](https://www.rd-alliance.org/system/files/DFT%20Core%20Terms-and%20model-v1-6.pdf) from it into a Digital Collection. As it does this, it creates in internal Java data model in the form of a an ISFSpecDataFindingAid. When it is done, it serializes this finding aid and writes it to a file. 

The Java test class is src/main/java/com/integratedgraphics/ifs/ExtractorTest.java. The extractor test reads one or more "extraction scripts" from /extract/ subdirectories and uses those to parse a Figshare zip file that was deposited by the American Chemical Society as part of their [FAIR Data initiative](https://pubs.acs.org/doi/10.1021/acs.orglett.0c00383). 

As it parses the extraction script, it:

<ol>
    <li>opens one or more Figshare ZIP files</li>
    <li>extracts Digital Objects into an "IFS FAIR Data Collection" in the site/ifs directory (not present here because of .gitignore)</li>
    <li>builds an IFSSpecDataFindingAid internal representation of the collection</li>
    <li>when done, generates a JSON serialization of the IFSSpecDataFindingAid object</li>
</ol>
    
Before you run the test, take a look at then test's main() method and adjust the parameters there a bit if you want. They include:

<ul>
    <li>first   the first test to run (0 to 12)</li>
    <li>last    the last test to run (0 to 12)</li>
    <li>targetDir  leave this as "../site/ifs"</li>
    <li>sourceDir  you can indicate a local source dir to use instead of Figshare to save download time. If you do that, you need to save the figshare nnnnnnnn.zip there.</li>
</ul>    
        
There are several other flags that can be set. The demo is not set up for batch command-line operation, and it is not built as a JAR file. It is simply an Eclipse Java project right now.

After you run the test, the /save/ifs directory will be populated, and the /html/demo.htm file should work. Since this HTML file is going to open files on your local machine, be sure to have your browser [set up for local file reading](http://wiki.jmol.org/index.php/Troubleshooting/Local_Files).
    
