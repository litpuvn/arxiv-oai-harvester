package arxiv.xml;

import com.google.common.collect.Lists;
import arxiv.exception.*;

import arxiv.model.data.ArticleMetadata;
import arxiv.model.data.ArticleVersion;

import org.apache.commons.lang3.StringUtils;
import org.arxiv.oai.arxivraw.ArXivRawType;
import org.openarchives.oai._2.*;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.InputStream;
import java.io.StringReader;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.normalizeSpace;


public class XMLParser {

    private final Unmarshaller unmarshaller;

    private static final RepositoryErrorSeverityComparator repositoryErrorSeverityComparator =
            new RepositoryErrorSeverityComparator();

    /**
     * Constructs a new XML parser by initializing the JAXB unmarshaller and setting up the XML validation.
     *
     * @throws HarvesterError if there are any problems
     */
    public XMLParser() {
        try {
            unmarshaller = JAXBContext.newInstance("org.openarchives.oai._2:org.arxiv.oai.arxivraw")
                    .createUnmarshaller();
        } catch (JAXBException e) {
            throw new HarvesterError("Error creating JAXB unmarshaller", e);
        }

        ClassLoader classLoader = this.getClass().getClassLoader();
        List<Source> schemaSources = Lists.newArrayList();

        schemaSources.add(new StreamSource(classLoader.getResourceAsStream("OAI-PMH.xsd")));
        schemaSources.add(new StreamSource(classLoader.getResourceAsStream("arXivRaw.xsd")));

        try {
            Schema schema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
                    .newSchema(schemaSources.toArray(new Source[schemaSources.size()]));
            unmarshaller.setSchema(schema);
        } catch (SAXException e) {
            throw new HarvesterError("Error creating validation schema", e);
        }
    }

    /**
     * Parse the XML response from the arXiv OAI repository.
     *
     * @throws NullPointerException if xmlData is null
     * @throws ParseException if parsing fails
     * @throws RepositoryError if the repository's response was parseable but invalid
     * @throws BadArgumentException if the repository's response contains a BadArgument error
     * @throws BadResumptionTokenException if the repository's response contains a BadResumptionToken error
     */
    public ParsedXmlResponse parse(String xmlData) {

        OAIPMHtype unmarshalledResponse;
        try {
            StringReader reader = new StringReader(xmlData);
            JAXBElement<OAIPMHtype> jaxbElement = (JAXBElement<OAIPMHtype>) unmarshaller.unmarshal(reader);

            unmarshalledResponse = jaxbElement.getValue();
        } catch (Exception e) {
            throw new ParseException("Error unmarshalling XML response from repository", e);
        }

        ZonedDateTime responseDate = parseResponseDate(unmarshalledResponse.getResponseDate());

        // Parse any errors returned by the repository
        List<OAIPMHerrorType> errors = Lists.newArrayList(unmarshalledResponse.getError());
        if (!errors.isEmpty()) {
            errors.sort(repositoryErrorSeverityComparator);

            // ID_DOES_NOT_EXIST and NO_RECORDS_MATCH are not considered errors, and simply result in an empty result set
            if (errors.get(0).getCode() == OAIPMHerrorcodeType.ID_DOES_NOT_EXIST ||
                    errors.get(0).getCode() == OAIPMHerrorcodeType.NO_RECORDS_MATCH) {
                return ParsedXmlResponse.builder()
                        .responseDate(responseDate)
                        .records(Lists.newArrayList())
                        .build();
            }

            // Produce error report
            StringBuilder errorStringBuilder = new StringBuilder("Received error from repository: \n");
            errors.stream().forEach(error -> errorStringBuilder.append(error.getCode().value()).append(" : ")
                                                                .append(normalizeSpace(error.getValue())).append("\n"));
            String errorString = errorStringBuilder.toString();

            // Throw an exception corresponding to the most severe error
            switch (errors.get(0).getCode()) {
                case BAD_ARGUMENT:
                    throw new BadArgumentException(errorString);
                case BAD_RESUMPTION_TOKEN:
                    throw new BadResumptionTokenException(errorString);
                case BAD_VERB:
                case CANNOT_DISSEMINATE_FORMAT:
                case NO_METADATA_FORMATS:
                case NO_SET_HIERARCHY:
                default:
                    throw new RepositoryError(errorString);
            }
        }


        // Handle the GetRecord response
        if (unmarshalledResponse.getGetRecord() != null) {
            ArticleMetadata record = parseRecord(unmarshalledResponse.getGetRecord().getRecord(), responseDate);

            return ParsedXmlResponse.builder()
                    .responseDate(responseDate)
                    .records(Lists.newArrayList(record))
                    .build();
        }


        // Handle the ListRecords response
        if (unmarshalledResponse.getListRecords() != null) {
            ParsedXmlResponse.ParsedXmlResponseBuilder responseBuilder =  ParsedXmlResponse.builder()
                    .responseDate(responseDate)
                    .records(unmarshalledResponse.getListRecords().getRecord().stream()
                            .map(xmlRecord -> parseRecord(xmlRecord, responseDate))
                            .collect(Collectors.toList()));

            ResumptionTokenType resumptionToken = unmarshalledResponse.getListRecords().getResumptionToken();
            if (resumptionToken != null) {
                responseBuilder.resumptionToken(normalizeSpace(resumptionToken.getValue()))
                        .cursor(resumptionToken.getCursor())
                        .completeListSize(resumptionToken.getCompleteListSize());
            }

            return responseBuilder.build();
        }


        // Handling of other response types is undefined
        throw new RepositoryError("Response from repository was not an error, GetRecord, or ListRecords response");
    }

    /**
     * Parse a single record of article metadata.
     * @throws ParseException if there is a parsing error
     */
    private ArticleMetadata parseRecord(RecordType xmlRecord, ZonedDateTime retrievalDateTime) {
        ArticleMetadata.ArticleMetadataBuilder articleBuilder = ArticleMetadata.builder();
        articleBuilder.retrievalDateTime(retrievalDateTime);

        HeaderType header = xmlRecord.getHeader();
        articleBuilder.identifier(normalizeSpace(header.getIdentifier()))
                .datestamp(parseDatestamp(normalizeSpace(header.getDatestamp())))
                .sets(header.getSetSpec().stream().map(StringUtils::normalizeSpace).collect(Collectors.toSet()))
                .deleted(header.getStatus() != null && header.getStatus() == StatusType.DELETED);

        @SuppressWarnings("unchecked")
        JAXBElement<ArXivRawType> jaxbElement = (JAXBElement<ArXivRawType>) xmlRecord.getMetadata().getAny();

        ArXivRawType metadata = jaxbElement.getValue();
        articleBuilder.id(normalizeSpace(metadata.getId()))
                .submitter(normalizeSpace(metadata.getSubmitter()))
                .versions(metadata.getVersion().stream()
                        .map(versionType -> ArticleVersion.builder()
                                .versionNumber(parseVersionNumber(normalizeSpace(versionType.getVersion())))
                                .submissionTime(parseSubmissionTime(normalizeSpace(versionType.getDate())))
                                .size(normalizeSpace(versionType.getSize()))
                                .sourceType(normalizeSpace(versionType.getSourceType()))
                                .build())
                        .collect(Collectors.toSet()))
                .title(normalizeSpace(metadata.getTitle()))
                .authors(normalizeSpace(metadata.getAuthors()))
                .categories(parseCategories(normalizeSpace(metadata.getCategories())))
                .comments(normalizeSpace(metadata.getComments()))
                .proxy(normalizeSpace(metadata.getProxy()))
                .reportNo(normalizeSpace(metadata.getReportNo()))
                .acmClass(normalizeSpace(metadata.getAcmClass()))
                .mscClass(normalizeSpace(metadata.getMscClass()))
                .journalRef(normalizeSpace(metadata.getJournalRef()))
                .doi(normalizeSpace(metadata.getDoi()))
                .license(normalizeSpace(metadata.getLicense()))
                .articleAbstract(normalizeSpace(metadata.getAbstract()));

        return articleBuilder.build();
    }

    /**
     * Parse the response date.  The result will be in UTC.
     */
    private ZonedDateTime parseResponseDate(XMLGregorianCalendar xmlGregorianCalendar) {
        return xmlGregorianCalendar.toGregorianCalendar().toZonedDateTime().withZoneSameInstant(ZoneOffset.UTC);
    }

    /**
     * Parse the datestamp of a record.
     * @throws ParseException if there is a parsing error
     */
    private LocalDate parseDatestamp(String value) {
        LocalDate datestamp;

        try {
            datestamp = LocalDate.parse(value);
        } catch(DateTimeParseException e) {
            throw new ParseException("Could not parse datestamp '" + value + "' in ISO_LOCAL_DATE format");
        }

        return datestamp;
    }

    /**
     * Parse the version number from the version string.  Per the arXivRaw XML schema, this should be in the form "v1",
     * "v2", etc.
     * @throws ParseException if the label cannot be found or does not fit the specified format
     */
    private Integer parseVersionNumber(String versionString) {
        String errorString = "Could not parse version '" + versionString + "'";

        if (versionString == null || !versionString.startsWith("v")) {
            throw new ParseException(errorString);
        }

        Integer version;
        try {
            version = Integer.valueOf(versionString.substring(1));
        } catch (NumberFormatException e) {
            throw new ParseException(errorString, e);
        }

        return version;
    }

    /**
     * Parse the date of an article version.
     * @throws ParseException if there is a parsing error
     */
    private ZonedDateTime parseSubmissionTime(String value) {
        ZonedDateTime submissionTime;
        try {
            submissionTime = ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME);
        } catch (DateTimeParseException e) {
            throw new ParseException("Could not parse version date '" + value + "' in RFC_1123_DATE_TIME format", e);
        }

        return submissionTime;
    }

    /**
     * Parse the category string of an article.
     * @return List of separate categories, in the same order as they were in the string
     */
    private List<String> parseCategories(String value) {
        return value != null ? Lists.newArrayList(value.split(" ")) : Lists.newArrayList();
    }


}
