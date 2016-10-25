package arxiv.oai;

import arxiv.exception.BadArgumentException;
import arxiv.xml.ParsedXmlResponse;
import arxiv.xml.XMLParser;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;

import javax.ws.rs.core.MediaType;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;

public class ArxivOAIHarvester {

    private static final String METADATA_PREFIX_RAW = "arXivRaw";
    private static final String METADATA_PREFIX_OAI_DC = "oai_dc";
    private XMLParser xmlParser;
    private WebResource webResource;
    private static final Logger log = Logger.getLogger(ArxivOAIHarvester.class.getName());

    public ArxivOAIHarvester() {
        this.xmlParser = new XMLParser();
        this.webResource = Client.create().resource( "http://export.arxiv.org" );
    }

    /**
     *
     * @param fromDate
     * @return ParsedXmlResponse
     * @throws UniformInterfaceException
     * @throws BadArgumentException
     */
    public ParsedXmlResponse listRecords(Date fromDate) throws UniformInterfaceException, BadArgumentException {
        return this.listRecords(fromDate, null);
    }

    /**
     * Do list repository records with a certain date range
     *
     * @param fromDate
     * @param toDate
     * @return ParsedXmlResponse
     * @throws UniformInterfaceException
     * @throws BadArgumentException
     */
    public ParsedXmlResponse listRecords(Date fromDate, Date toDate) throws UniformInterfaceException, BadArgumentException {
        if (fromDate == null) {
            throw new BadArgumentException("Expect from Date not null");
        }

        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        WebResource service = this.webResource.path("oai2")
                .queryParam("verb", "ListRecords")
                .queryParam("from", df.format(fromDate))
                .queryParam("metadataPrefix", METADATA_PREFIX_RAW)
        ;

        log.info("Querying repository: " + service.getURI());

        if (toDate != null) {
            if (fromDate.after(toDate)) {
                throw new BadArgumentException("fromDate must be less than or equal to untilDate");
            }

            service.queryParam("until", df.format(toDate));
        }

        return this.xmlParser.parse(this.getXmlDataFromArxiv(service));
    }

    protected String getXmlDataFromArxiv(WebResource service) {
        return service.accept(MediaType.APPLICATION_XML).get(String.class);
    }


}
