package arxiv.oai;

import arxiv.xml.XMLParser;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;

import javax.ws.rs.core.MediaType;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ArxivOAIHarvester {

    private XMLParser xmlParser;
    private Client client;
    private WebResource webResource;
    //private final static int BULK_SIZE = Integer.parseInt(Config.getProperties("org.mrdlib.importer.arxiv", ArxivImport.class).getProperty("bulkSize", "10"));

    public ArxivOAIHarvester() {
        this.xmlParser = new XMLParser();
        this.client = Client.create();
        this.webResource = this.client.resource( "http://export.arxiv.org" );
    }

    public void listRecords(Date releaseDate) {
        try {
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            WebResource service = this.webResource.path("oai2")
                    .queryParam("verb", "ListRecords")
                    .queryParam("from", df.format(releaseDate));

            System.out.println(service.getURI().toString());
            String xmlData = service.accept(MediaType.APPLICATION_XML).get(String.class);
            System.out.println(xmlData);

            this.xmlParser.parse(xmlData);


        }
        catch (UniformInterfaceException ue) {
            System.err.println("Error getting Data from Arxiv. Please try again latter. The exception was");
            ue.printStackTrace();
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
    }
}
