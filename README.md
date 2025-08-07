Noark Extraction Validator
----

The Noark Extraction Validator provides a means for validating Noark (5.x) extraction packages according to the requirements specified by the [standard](http://arkivverket.no/arkivverket/Offentleg-forvalting/Noark/Noark-5). The tool is a Java console application that:
* parses a given extraction package;
* stores its contents in a temporary in-memory (or file) database;
* validates metadata and documents, and collects information - validation rules are listed in the [Validation](#validationRules) section;
* generates a report that summarizes the results.


# Prerequisites
**Required**
* JDK 17

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
java -jar noark-extraction-validator-0.4.0.jar
```
OR
```
java -jar noark-extraction-validator-0.4.0.jar --help
```

**Validating different extraction package versions**

Currently supported versions are *Noark 5.3*, *Noark 5.4* and *Noark 5.5*.

Noark 5.3:
```
java -jar noark-extraction-validator-0.4.0.jar noark53 ...
```

Noark 5.4:
```
java -jar noark-extraction-validator-0.4.0.jar noark54 ...
```

Noark 5.5:
```
java -jar noark-extraction-validator-0.4.0.jar noark55 ...
```

To validate against the desired version, substitute the `<noark-version>` parameter in the following examples with one of the supported versions.

**Logging**

If you would like to enable logging in the console or in a specific file, you will need to provide a reference to a log4j2 configuration:
```
java -Dlog4j.configurationFile='/path/to/log4j2.xml' -jar noark-extraction-validator-0.4.0.jar <noark-version> ...
```
A default configuration exists in a directory called `config/`.

**Validating a Noark 5.x extraction package**

The shortest command to validate a package is:
```
java -jar noark-extraction-validator-0.4.0.jar <noark-version> -extraction /path/to/uttrekk/directory
```
More complex scenarios would include modifying the output report type and/or directory:
```
java -jar noark-extraction-validator-0.4.0.jar <noark-version> -extraction /path/to/uttrekk/directory -output-dir /path/to/report/output/directory -output-type excel_xls
```
You can request a report to be generated in multiple formats:
```
java -jar noark-extraction-validator-0.4.0.jar <noark-version> -extraction /path/to/uttrekk/directory -output-dir /path/to/report/output/directory -output-type excel_xls -output-type xml
```
You can specify custom Noark schemas to be used in the validation process (in addition to the Noark ones and the extraction package ones). In such cases the reports would also include information about the compliance of the extraction package to these schemas:
```
java -jar noark-extraction-validator-0.4.0.jar <noark-version> -extraction /path/to/uttrekk/directory -custom-schema-location /path/to/custom/schemas/directory
```
The specified `-custom-schema-location` directory may contain any custom Noark 5 schemas and a UTF-8-encoded description.txt file. The contents of this file will be copied to the Execution Information sections of the generated reports for completeness.


You can also change the persistence settings:
```
java -jar noark-extraction-validator-0.4.0.jar <noark-version> -extraction /path/to/uttrekk/directory -output-dir /path/to/report/output/directory -db-name myDBName -storage hsqldb_server -server-location http://my.hsqldb.server.com
```
If you would like to run several instances of the validator at the same time, you will have to use the *hsqldb_server* or *hsqldb_file* persistence settings and specify a different DB name / DB file location for each instance:
* For *hsqldb_file* change the `-db-dir-location` argument
* For *hsqldb_server* change the `-db-name` argument

If you would like to persist the database created by the validator then it's best to use *hsqldb_server*:
  * In this case, you need to create a server instance of HSQLDB 1.8.0. Refer to [HSQLDB's page](http://hsqldb.org/) for more information and to [HSQLDB 1.8.0](https://sourceforge.net/projects/hsqldb/files/) for the binaries. Running the server would include:
    ```
    java -cp hsqldb.jar org.hsqldb.Server -database.0 file:mydb -dbname.0 xdb
    ```
    where **xdb** is the name of the default database.
    
In case of huge extractions you can also specify the storage type to be *hsqldb_file* like so:
```
java -jar noark-extraction-validator-0.4.0.jar <noark-version> -extraction /path/to/uttrekk/directory -output-dir /path/to/report/output/directory -storage hsqldb_file
```
Keep in mind that this will make the validation run slower (2 times or more), but could be the only option when having to deal with extremely large extractions and still generate an EXCEL_XLS report.
Note that by default the database storage directory is called *"noark-extraction-validator-db"* and is situated in the OS temporary dir, but can be specified via the *-db-dir-location* flag:
```
java -jar noark-extraction-validator-0.4.0.jar <noark-version> -extraction /path/to/uttrekk/directory -output-dir /path/to/report/output/directory -storage hsqldb_file -db-dir-location /path/to/desired/db-storage/direcotry
```

Making the validation process less strict is also possible. The default behavior of the validator is to stop execution if an XML file does not comply with the corresponding Noark XSD schema. To continue execution instead:
```
java -jar noark-extraction-validator-0.4.0.jar <noark-version> -extraction /path/to/uttrekk/directory -ignore-non-compliant-xml
```
This, however, is a bad practice that we would *advise strongly against* - the reason being that the result of the validation in such cases may be unreliable because crucial metadata elements are missing.

**Validating a Noark 5.x extraction package - Windows**

Similar to the above but note that Windows uses a different path separator:

```
"/path/to/jdk17/bin/java" -jar noark-extraction-validator-0.4.0.jar <noark-version> -extraction /path/to/uttrekk -output-dir /path/to/output/reports/dir
```

Also note that the above command specifies the path to Java explicitly to make sure that we are using a JDK, not a JRE (if one is installed). There are other ways to achieve the same thing.

**Running as a library**

The application can also be run as a library. To do so, you will need to prepare a command, create a validator, and run it:
```
# Initialize report configuration
ReportConfiguration reportConfiguration = new ReportConfiguration();
reportConfiguration.setOutputDir(new File("/path/to/output/directory/"));
reportConfiguration.setOutputTypes(Arrays.asList(ReportType.EXCEL_XLSX, ReportType.XML));

# Create a Noark 5.3 command
Noark53Command command = new Noark53Command();

# To create a Noark 5.4 command, use alternatively:
# Noark54Command command = new Noark54Command();

# Similar for Noark 5.5:
# Noark55Command command = new Noark55Command();

command.setExtractionDirectory(new File("/path/to/extraction"));
command.setReportConfiguration(reportConfiguration);

# Create a validator
Validator validator = ValidationFactory.createValidator(ValidatorType.byName(command.getName()), command);

validator.run();
```

# Samples

The [Noark Extraction Validator Samples repository](https://github.com/documaster/noark-extraction-validator-samples) contains a number of sample extraction packages and their corresponding validation reports produced by this tool. Note that the samples are of varying quality in order to demonstrate the capabilities of the validator.

<a name="validationRules"></a>
# Noark 5.x Validation

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
      loependeJournal.xml (optional)
      loependeJournal.xsd (optional)
      offentligJournal.xml (optional)
      offentligJournal.xsd (optional)
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
  * arkivstruktur.xsd, endringslogg.xsd, offentligJournal.xsd, loependeJournal.xsd, addml.xsd, metadatakatalog.xsd, where offentligJournal.xsd and loependeJournal.xsd are optional
  * arkivstruktur.xml, endringslogg.xml, offentligJournal.xml, loependeJournal.xml, arkivuttrekk.xml, where offentligJournal.xml and loependeJournal.xml are optional
* The checksums of all XSD schemas in the package match the corresponding checksums of the schemas distributed with the Noark 5 standard
* All XML files in the package match the corresponding XSD schemas distributed with the Noark 5 standard

## Content validation rules
There are two distinct categories of validation rules - *checks* and *tests*. Both of them have an ID, title, and description, and belong to a group. *Checks* produce information about a specific aspect of an extraction package whereas *tests* perform validations and may produce warnings and errors. Generally, you will see *checks* printed before *tests* in validation reports.

Below are all validation rules ordered by their execution priority (every test is annotated based on the kind of result it produces - info/warning/error):

**package:**

|Type|Results in|ID|Title|Description|
|---|---|---|---|---|
|Test|Info,Error|P1|Validity of addml.xsd (existence, well-formedness, compliance with schemas|Tests whether the XSD schema 1) exists, 2) is valid XML, and 3) its checksum matches the checksum of its Noark counterpart.|
|Test|Info,Warning,Error|P2|Validity of arkivuttrekk.xml (existence, well-formedness, compliance with schemas)|Produces warnings when arkivuttrekk.xml does not comply with the package addml.xsd. Produces errors when arkivuttrekk.xml does not comply with the Noark addml.xsd.|
|Test|Info,Error|P3|Validity of offentligJournal.xsd (existence, well-formedness, comparison against Noark counterpart)|Tests whether the XSD schema 1) exists, 2) is valid XML, and 3) its checksum matches the checksum of its Noark counterpart.|
|Test|Info,Warning,Error|P4|Validity of offentligJournal.xml (existence, well-formedness, compliance with schemas)|Produces warnings when offentligJournal.xml does not comply with the package offentligJournal.xsd. Produces errors when offentligJournal.xml does not comply with the Noark offentligJournal.xsd.|
|Test|Info,Error|P5|Validity of endringslogg.xsd (existence, well-formedness, comparison against Noark counterpart)|Tests whether the XSD schema 1) exists, 2) is valid XML, and 3) its checksum matches the checksum of its Noark counterpart.|
|Test|Info,Warning,Error|P6|Validity of endringslogg.xml (existence, well-formedness, compliance with schemas)|Produces warnings when endringslogg.xml does not comply with the package endringslogg.xsd. Produces errors when endringslogg.xml does not comply with the Noark endringslogg.xsd.|
|Test|Info,Error|P7|Validity of metadatakatalog.xsd (existence, well-formedness, comparison against Noark counterpart)|Tests whether the XSD schema 1) exists, 2) is valid XML, and 3) its checksum matches the checksum of its Noark counterpart.|
|Test|Info,Error|P8|Validity of arkivstruktur.xsd (existence, well-formedness, comparison against Noark counterpart)|Tests whether the XSD schema 1) exists, 2) is valid XML, and 3) its checksum matches the checksum of its Noark counterpart.|
|Test|Info,Warning,Error|P9|Validity of arkivstruktur.xml (existence, well-formedness, compliance with schemas)|Produces warnings when arkivstruktur.xml does not comply with the package arkivstruktur.xsd. Produces errors when arkivstruktur.xml does not comply with the Noark arkivstruktur.xsd.|
|Test|Info,Error|P10|Validity of loependeJournal.xsd (existence, well-formedness, comparison against Noark counterpart)|Tests whether the XSD schema 1) exists, 2) is valid XML, and 3) its checksum matches the checksum of its Noark counterpart.|
|Test|Info,Warning,Error|P11|Validity of loependeJournal.xml (existence, well-formedness, compliance with schemas)|Produces warnings when loependeJournal.xml does not comply with the package loependeJournal.xsd. Produces errors when loependeJournal.xml does not comply with the Noark loependeJournal.xsd.|

**arkivstruktur:**

|Type|Results in|ID|Title|Description|
|---|---|---|---|---|
|Check|Info|ASC1|Fonds|Number of fonds|
|Check|Info|ASC2|Classification systems|Number of classification systems grouped by series|
|Check|Info|ASC3|Classes|Number of classes grouped by classification system|
|Check|Info|ASC4|Classes without subclasses, files, or records|Number of classes without children of any type, grouped by series|
|Check|Info|ASC5|Files with classes|Number of files grouped by class and series|
|Check|Info|ASC6|Files without subfiles or records|Number of files without children of any type, grouped by series|
|Check|Info|ASC7|Registry entries without a main document (document description)|Number of registry entries without a main document, grouped by series|
|Check|Info|ASC8|Records with classes|Number of records with classes, grouped by class and series|
|Check|Info|ASC9|Records without document descriptions|Number of records without document descriptions, grouped by series|
|Check|Info|ASC10|Document descriptions|Number of document descriptions grouped by series|
|Check|Info|ASC11|Document descriptions without document objects|Number of document descriptions without document objects, grouped by series|
|Check|Info|ASC12|Document objects|Number of document objects grouped by series and parent (document description or record)|
|Check|Info|ASC13|Case parties|Number of case parties grouped by series|
|Check|Info|ASC14|Notes|Number of notes associated with files, records, or document descriptions, grouped by series|
|Check|Info|ASC15|Cross references|Number of cross references associated with classes, files, or records, grouped by series|
|Check|Info|ASC16|Precedents|Number of precedents associated with files or records, grouped by series|
|Check|Info|ASC17|Sign-offs|Number of sign-offs grouped by series|
|Check|Info|ASC18|Document flows|Number of document flows grouped by series|
|Check|Info|ASC19|Gradings|Number of gradings associated with series, classes, files, records, or document descriptions|
|Check|Info|ASC20|Conversions|Number of conversions grouped by series|
|Check|Info|ASC21|Deletions (apart from disposals)|Number of deletions (apart from disposals)|
|Test|Info,Error|AST1|Extraction-wide systemID uniqueness|Tests whether the same systemID has been used for more than one object in the archive.|
|Test|Info,Error|AST2|Series|Provides information about the number of series in the archive, and tests whether a fonds without any series exists.|
|Test|Info,Warning|AST3|Fonds period containment|Tests whether the created and finalized dates of all fonds are within the archival period specified in arkivuttrekk.xml.|
|Test|Info,Warning|AST4|Series period containment|Tests whether the created and finalized dates of all series are within the archival period specified in arkivuttrekk.xml.|
|Test|Info,Warning|AST5|Series status|Tests whether non-finalized series exist in the archive. A series is considered non-finalized if a finalized date or a finalizing party is not specified, or if the series status is not 'Avsluttet periode'.|
|Test|Info,Error|AST6|File count|Provides information about the number of files per series and tests whether the corresponding number specified in arkivuttrekk.xml is correct.|
|Test|Info,Warning|AST7|File period containment|Tests whether the created and finalized dates of all files are within the archival period specified in arkivuttrekk.xml.|
|Test|Info,Error|AST8|Files in leaf classes|Provides information about the number of files per series and class, and tests whether any file belongs to a class which has other classes as children (i.e. the file has a class sibling).|
|Test|Info,Warning|AST9|File status|Tests whether non-finalized files exist in the archive. A file is considered non-finalized if a finalized date or a finalizing party is not specified, or if the file status is not 'Avsluttet'.|
|Test|Info,Error|AST10|Records|Provides information about the number of records per series and tests whether the corresponding number specified in arkivuttrekk.xml is correct.|
|Test|Info,Warning|AST11|Record period containment|Tests whether the created and finalized dates of all records are within the archival period specified in arkivuttrekk.xml.|
|Test|Info,Error|AST12|Records in leaf classes|Provides information about the number of records per series and class, and tests whether any record belongs to a class which has other classes as children (i.e. the record has a class sibling).|
|Test|Info,Warning|AST13|Record status|Tests whether non-finalized records exist in the archive. A record is considered non-finalized if a finalized date or a finalizing party is not specified, or if the record status is not 'Arkivert' or 'Utgår'.|
|Test|Info,Warning|AST14|Document description status|Tests whether non-finalized document descriptions exist in the archive. A document description is considered non-finalized if a finalized date or a finalizing party is not specified, or if the document description status is not 'Dokumentet er ferdigstilt'.|
|Test|Info,Warning|AST15|Document description period containment|Tests whether the created and finalized dates of all document descriptions are within the archival period specified in arkivuttrekk.xml.|
|Test|Info,Error|AST16|Document object checksums|Tests whether the document object checksums specified in arkivstruktur.xml match the ones that the validator calculated using the SHA256 algorithm.|
|Test|Info,Error|AST17|Document object file types|Tests whether all document objects are valid PDF/A-1B documents.|
|Test|Info,Error|AST18|Correspondence parties|Provides information about the number of correspondence parties grouped by series and tests whether any registry entries without correspondence parties exist.|
|Test|Info,Warning|AST19|Screenings|Provides information about the number of screened series, classes, files, records, and document descriptions, and tests whether the corresponding value in arkivuttrekk.xml (inneholderSkjermetInformasjon) is correct.|
|Test|Info,Warning|AST20|Disposal decisions|Provides information about the number of disposal decisions related to series, classes, files, records, and document descriptions, and tests whether the corresponding value in arkivuttrekk.xml (inneholderDokumenterSomSkalKasseres) is correct.|
|Test|Info,Warning|AST21|Disposals|Provides information about the number of disposals of series and document descriptions, and tests whether the corresponding value in arkivuttrekk.xml (omfatterDokumenterSomErKassert) is correct.|
|Test|Info,Warning|AST22|Personal name fields|Checks whether all name fields contain seemingly valid personal names. The regular expressions used for the purpose are "\^[\p{L}\s-'.]*$" and "\^[\p{L}\s-'.]+$" depending on whether the value can be blank or not. The validated fields are: saksansvarlig (M306), kontaktperson (M412), korrespondansepartNavn (M400), sakspartNavn (M302), moeteDeltakerNavn (M372), arkivertAv (M605), avskrevetAv (M618), tilknyttetAv (M621), merknadRegistrertAv (M612), kassertAv (M631), slettetAv (M614), gradertAv (M625), presedensGodkjentAv (M629), verifisertAv (M623), nedgradertAv (M627), opprettetAv (M601), avsluttetAv (M603).|

**loependejournal:**

|Type|Results in|ID|Title|Description|
|---|---|---|---|---|
|Check|Info|LJC1|Screened registry entries|Number of screened registry entries found in loependeJournal.xml|
|Test|Info,Warning|LJT1|Registry entries|Provides information about the number of registry entries found in loependeJournal.xml and tests whether it matches the number of registry entries found in arkivstruktur.xml.|
|Test|Info,Warning|LJT2|Period containment of registry entries|Tests whether the created and finalized dates of all registry entries in loependeJournal.xml are within the archival period specified in arkivuttrekk.xml, and whether the same registry entries (same systemIDs) can be found in arkivstruktur.xml.|

**offentligjournal:**

|Type|Results in|ID|Title|Description|
|---|---|---|---|---|
|Test|Info,Warning|OJT1|Registry entries|Provides information about the number of registry entries found in offentligJournal.xml and tests whether it matches the number of registry entries found in arkivstruktur.xml.|
|Test|Info,Warning|OJT2|Period containment of registry entries|Tests whether the created and finalized dates of all registry entries in offentligJournal.xml are within the archival period specified in arkivuttrekk.xml, and whether the same registry entries (same systemIDs) can be found in arkivstruktur.xml.|

**arkivuttrekk:**

|Type|Results in|ID|Title|Description|
|---|---|---|---|---|
|Test|Info,Error|AUT1|Validity of the arkivstruktur.xml checksum specified in arkivuttrekk.xml|Tests whether the checksum of arkivstruktur.xml matches the one specified in arkivuttrekk.xml.|
|Test|Info,Error|AUT2|Validity of the arkivstruktur.xsd checksum specified in arkivuttrekk.xml|Tests whether the checksum of arkivstruktur.xsd matches the one specified in arkivuttrekk.xml.|
|Test|Info,Error|AUT3|Validity of the loependeJournal.xml checksum specified in arkivuttrekk.xml|Tests whether the checksum of loependeJournal.xml matches the one specified in arkivuttrekk.xml.|
|Test|Info,Error|AUT4|Validity of the loependeJournal.xsd checksum specified in arkivuttrekk.xml|Tests whether the checksum of loependeJournal.xsd matches the one specified in arkivuttrekk.xml.|
|Test|Info,Error|AUT5|Validity of the offentligJournal.xml checksum specified in arkivuttrekk.xml|Tests whether the checksum of offentligJournal.xml matches the one specified in arkivuttrekk.xml.|
|Test|Info,Error|AUT6|Validity of the offentligJournal.xsd checksum specified in arkivuttrekk.xml|Tests whether the checksum of offentligJournal.xsd matches the one specified in arkivuttrekk.xml.|
|Test|Info,Error|AUT7|Validity of the endringslogg.xml checksum specified in arkivuttrekk.xml|Tests whether the checksum of endringslogg.xml matches the one specified in arkivuttrekk.xml.|
|Test|Info,Error|AUT8|Validity of the endringslogg.xsd checksum specified in arkivuttrekk.xml|Tests whether the checksum of endringslogg.xsd matches the one specified in arkivuttrekk.xml.|

**endringslogg:**

|Type|Results in|ID|Title|Description|
|---|---|---|---|---|
|Check|Info|ELC1|Change log entries|Provides information about the number of change log entries in endringslogg.xml.|
|Test|Info,Warning|ELT1|Change log references|Tests whether all objects referenced in the change log can be found by their systemID in arkivstruktur.xml.|

## Reports
The tool can produce reports in different formats (see the examples above for more information on how to request multiple reports). Currently supported formats are Excel (pre-2007 binary format), Excel (post-2007 OOXML format), and XML. All reports include:
* information about the execution
  * version of the tool
  * list of key parameters specified for the execution
* summary of the results
* details for each executed validation rule (check or test), such as:
  * title
  * description
  * info
  * list of warnings
  * list of errors

Each list of warnings and errors aims to provide as detailed information as possible with regard to the exact location where an issue was encountered. Where applicable, this includes information such as system ID, file ID, case year, case number, record ID, record year, record number, line, or column.

### Excel report
The purpose of the Excel report is to provide an intuitive human-readable representation of the validation results. It is color-coded and includes multiple links between generated sheets in order to facilitate the navigation between them.

### XML report
The XML report is not intended to be human-readable but to be used for long-term preservation of the validation results. The corresponding XSD schema is bundled with the report.
