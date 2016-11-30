Noark Extraction Validator
----

The Noark Extraction Validator provides a means for validating Noark (5.3) extraction packages according to the requirements specified by the [standard](http://arkivverket.no/arkivverket/Offentleg-forvalting/Noark/Noark-5). The tool is a Java console application that:
* parses a given extraction package;
* stores its contents in a temporary in-memory (or file) database;
* validates metadata and documents, and collects information - validation rules are listed in the [Validation](#validationRules) section;
* generates a report that summarizes the results.


# Prerequisites
**Required**
* JDK 8

**Optional**
* Maven
  * Required to build the tool
* Internet connection
  * Required to download dependencies (unless they are already available in the local Maven repository) 


# Build

```
mvn clean package
```


# Examples

**Help**

```
java -jar noark-extraction-validator-0.1.0.jar
```
OR
```
java -jar noark-extraction-validator-0.1.0.jar --help
```

**Validating a Noark 5.3 extraction package**

The shortest command to validate a package is:
```
java -jar noark-extraction-validator-0.1.0.jar noark53 -extraction /path/to/uttrekk/directory
```
More complex scenarios would include modifying the output report type and/or directory:
```
java -jar noark-extraction-validator-0.1.0.jar noark53 -extraction /path/to/uttrekk/directory -output-dir /path/to/report/output/directory -output-type excel_xls
```
You can also change the persistence settings:
```
java -jar noark-extraction-validator-0.1.0.jar noark53 -extraction /path/to/uttrekk/directory -output-dir /path/to/report/output/directory -db-name myDBName -storage hsqldb_server -server-location http://my.hsqldb.server.com
```
Do not modify the persistence settings unless:
* You would like to run several instances of the validator at the same time
  * In this case, you need to modify the target database name to avoid conflicts (-db-name).
* You would like to persist the database created by the validator
  * In this case, you need to create a server instance of HSQLDB 1.8.0. Refer to [HSQLDB's page](http://hsqldb.org/) for more information and to [HSQLDB 1.8.0](https://sourceforge.net/projects/hsqldb/files/) for the binaries. Running the server would include:
    ```
    java -cp hsqldb.jar org.hsqldb.Server -database.0 file:mydb -dbname.0 xdb
    ```
    where **xdb** is the name of the default database.

Making the validation process less strict is also possible. The default behavior of the validator is to stop execution if an XML file does not comply with the corresponding Noark XSD schema. To continue execution instead:
```
java -jar noark-extraction-validator-0.1.0.jar noark53 -extraction /path/to/uttrekk/directory -ignore-non-compliant-xml
```
This, however, is a bad practice that we would strongly advise against - the reason being that the result of the validation in such cases may be unreliable because crucial metadata elements are missing.

**Validating a Noark 5.3 extraction package - Windows**

Similar to the above but note that Windows uses a different path separator:

```
"c:\Program Files\Java\jdk1.8.0_111\bin\java" -jar noark-extraction-validator-0.1.0.jar noark53 -extraction c:\path\to\uttrekk -output-dir c:\path\to\output\reports\dir
```

Also note that the above command specifies the path to Java explicitly to make sure that we are using a JDK, not a JRE (if one is installed). There are other ways to achieve the same thing.


<a name="validationRules"></a>
# Noark 5.3 Validation

## Extraction package structure

```
ROOT
   /uttrekk
      /dokumenter
      addml.xsd
      arkivstruktur.xml
      arkivstruktur.xsd
      arkivuttrekk.xml
      business-specific-metadata.xml (vendor-dependent; may not exist)
      endringslogg.xml
      endringslogg.xsd
      loependeJournal.xml
      loependeJournal.xsd
      offentligJournal.xml
      offentligJournal.xsd
   info.xml (not well-defined currently)
```

## Translations of common terms

| Norsk  | English | Notes |
|---|---|---|
|arkiv|fonds||
|arkivdel|series||
|arkivskaper|fonds creator||
|klassifikasjonssystem|classification system||
|klasse|class||
|mappe|file|has the meaning of folder|
|saksmappe|case file||
|registrering|record|can be used to denote all kinds of records|
|basisregistrering|basic record||
|journalpost|registry entry||
|dokumentbeskrivelse|document description||
|dokumentobjekt|document object|a document version|
|sakspart|case party||
|merknad|note||
|kryssreferanse|cross reference||
|presedens|precedent||
|korrespondansepart|correspondence party||
|avskrivning|sign-off||
|dokumentflyt|document flow||
|skjerming|screening||
|gradering|grading||
|kassasjonsvedtak|disposal decision||
|kassasjon|disposal||
|konvertering|conversion||
|sletting|deletion||

## Package structure validation rules
The package structure is validated according to the following rules:
* Required XML and XSD files exist in the package and are well-formed:
  * arkivstruktur.xsd, endringslogg.xsd, offentligJournal.xsd, loependeJournal.xsd, addml.xsd, metadatakatalog.xsd
  * arkivstruktur.xml, endringslogg.xml, offentligJournal.xml, loependeJournal.xml, arkivuttrekk.xml
* The checksums of all XSD schemas in the package match the corresponding checksums of the schemas distributed with the Noark 5 standard
* All XML files in the package match the corresponding XSD schemas distributed with the Noark 5 standard

## Content validation rules

**common:**
* [Test] Validity of the arkivstruktur.xml checksum specified in arkivuttrekk.xml
* [Test] Validity of the arkivstruktur.xsd checksum specified in arkivuttrekk.xml
* [Test] Validity of the loependeJournal.xml checksum specified in arkivuttrekk.xml
* [Test] Validity of the loependeJournal.xsd checksum specified in arkivuttrekk.xml
* [Test] Validity of the offentligJournal.xml checksum specified in arkivuttrekk.xml
* [Test] Validity of the offentligJournal.xsd checksum specified in arkivuttrekk.xml
* [Test] Validity of the endringslogg.xml checksum specified in arkivuttrekk.xml
* [Test] Validity of the endringslogg.xsd checksum specified in arkivuttrekk.xml

**arkivstruktur:**
* [Test] Extraction-wide system ID uniqueness
* [Info] Fonds count
* [Info] Number of series (at least 1 per leaf fonds entity)
* [Test] Fonds created/finalized dates are within the extraction period start and end dates
* [Test] Series created/finalized dates are within the extraction period start and end dates
* [Test] Series status validity
* [Info] Number of classification systems
* [Info] Number of classes
* [Info] Classes without sub-classes, files, or records
* [Test] File count specified in arkivuttrekk against the actual file count in arkivstruktur
* [Test] File created/finalized dates are within the extraction period start and end dates
* [Test] Files are only linked to classes that do not have sub-classes
* [Info] Number of files per class
* [Info] Number of Files without sub-files and records
* [Test] File status validity
* [Test] Record count specified in arkivuttrekk against the actual Record count in arkivstruktur
* [Info] Records without a main document (document description)
* [Test] Record created/finalized dates are within the extraction period start and end dates
* [Test] Records are only linked to classes that do not have sub-classes
* [Info] Number of records per class
* [Info] Number of records without documents (document descriptions)
* [Test] Record status validity
* [Info] Number of document descriptions
* [Info] Number of document descriptions without document objects
* [Test] Document description status validity
* [Test] Document Description created/finalized dates are within the extraction period start and end dates
* [Info] Number of document objects
* [Test] Document object checksums specified in arkivstruktur against the document object checksums calculated by the validator
* [Test] Document object file types validity (PDF/A-1b required)
* [Info] Number of case parties
* [Info] Number of notes
* [Info] Number of cross references
* [Info] Number of precedents
* [Info] Number of correspondence parties
* [Info] Number of sign-offs
* [Info] Number of document flows
* [Test] Cross-reference the screening value (boolean) specified in arkivuttrekk against the screened objects encountered by the tool
* [Info] Number of gradings
* [Test] Cross-reference the disposal decision value (boolean) specified in arkivuttrekk against the disposal decision objects encountered by the tool
* [Test] Cross-reference the disposal value (boolean) specified in arkivuttrekk against the disposal objects encountered by the tool
* [Info] Number of conversions
* [Info] Number of deletions (note that this is different from disposal)

**loependejournal:**
* [Test] Cross-reference the record count specified in arkivuttrekk against the actual record count in loependejournal
* [Test] Record created/finalized dates are within the extraction period start and end dates
* [Info] Number of screened records

**offentligjournal:**
* [Test] Cross-reference the record count specified in arkivuttrekk with the actual record count in offentligjournal
* [Test] Record created/finalized dates are within the extraction period start and end dates

**endringslogg:**
* [Info] Number of changes in endringslogg
* [Test] Number of system IDs found in endringlogg, but not in arkivstruktur