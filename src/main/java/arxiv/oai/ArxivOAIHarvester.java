package arxiv.oai;

import arxiv.xml.ParsedXmlResponse;
import arxiv.xml.XMLParser;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;

import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

public class ArxivOAIHarvester {

    private static final String METADATA_PREFIX_RAW = "arXivRaw";
    private static final String METADATA_PREFIX_OAI_DC = "oai_dc";
    private XMLParser xmlParser;
    private Client client;
    private WebResource webResource;

    public ArxivOAIHarvester() {
        this.xmlParser = new XMLParser();
        this.client = Client.create();
        this.webResource = this.client.resource( "http://export.arxiv.org" );
    }

    /**
     *
     * @param fromDate
     * @return number of records
     */
    public ParsedXmlResponse listRecords(Date fromDate) {
        return this.listRecords(fromDate, null);
    }

    /**
     *
     * @param fromDate
     * @param toDate
     * @return
     */
    public ParsedXmlResponse listRecords(Date fromDate, Date toDate) {
        try {
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            WebResource service = this.webResource.path("oai2")
                    .queryParam("verb", "ListRecords")
                    .queryParam("from", df.format(fromDate))
                    .queryParam("metadataPrefix", METADATA_PREFIX_RAW)
            ;

            if (toDate != null) {
                service.queryParam("until", df.format(toDate));
            }

            return this.xmlParser.parse(this.getXmlDataFromArxiv(service));
        }
        catch (UniformInterfaceException ue) {
            System.err.println("Error getting Data from Arxiv. Please try again latter. The exception was");
            ue.printStackTrace();
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

    protected String getXmlDataFromArxiv(WebResource service) {
        System.out.println(service.getURI().toString());

        return service.accept(MediaType.APPLICATION_XML).get(String.class);
    }


}
