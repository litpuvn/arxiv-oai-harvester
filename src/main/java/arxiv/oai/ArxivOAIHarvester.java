package arxiv.oai;

import arxiv.exception.BadArgumentException;
import arxiv.xml.ParsedXmlResponse;
import arxiv.xml.XMLParser;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import org.apache.commons.codec.net.URLCodec;
import org.apache.http.client.utils.URLEncodedUtils;

import javax.ws.rs.core.MediaType;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
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
       return this.listRecords(fromDate, toDate, null);
    }

    public ParsedXmlResponse listRecords(Date fromDate, Date toDate, String resumptionToken) throws UniformInterfaceException, BadArgumentException {

        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        WebResource service = this.webResource.path("oai2")
                .queryParam("verb", "ListRecords")
        ;

        if (resumptionToken != null && resumptionToken.length() > 0) {
            log.info("Found resumption token: " + resumptionToken);
            service = service.queryParam("resumptionToken", resumptionToken);
        } else {
            if (fromDate == null) {
                throw new BadArgumentException("Expect from Date not null");
            }

            service = service.queryParam("from", df.format(fromDate))
                    .queryParam("metadataPrefix", METADATA_PREFIX_RAW)
            ;

            if (toDate != null) {
                if (fromDate.after(toDate)) {
                    throw new BadArgumentException("fromDate must be less than or equal to untilDate");
                }

                service = service.queryParam("until", df.format(toDate));
            }
        }

        log.info("Querying repository: " + service.getURI());

        return this.xmlParser.parse(this.getXmlDataFromArxiv(service));
    }

    protected String getXmlDataFromArxiv(WebResource service) {
        return service.accept(MediaType.APPLICATION_XML).get(String.class);
    }


}
