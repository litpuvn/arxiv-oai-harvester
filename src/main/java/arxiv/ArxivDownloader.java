package arxiv;

import arxiv.exception.BadArgumentException;
import arxiv.oai.ArxivOAIHarvester;
import arxiv.xml.ParsedXmlResponse;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.http.HttpStatus;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class ArxivDownloader {

    private ArxivOAIHarvester harvester;
    private static final Logger log = Logger.getLogger(ArxivDownloader.class.getName());
    private static final String HTTP_HEADER_RETRY_AFTER = "Retry-After";
    private static final long DEFAULT_DELAY_INTERVAL_IN_MILLIS = 20000;

    public ArxivDownloader(ArxivOAIHarvester harvester) {

        this.harvester = harvester;
    }

    public void download(Date fromDate, Date untilDate) {
        String resumptionToken = null;
        int totalRecords = 0;
        try {

            log.info("Harvesting...");

            ArxivOAIHarvester harvester = new ArxivOAIHarvester();

            do {
                try {
                    ParsedXmlResponse response = harvester.listRecords(fromDate, untilDate, resumptionToken);
                    resumptionToken = response.getResumptionToken();
                    totalRecords += response.getRecords().size();

                    log.info("Found: " + response.getRecords().size() + " records");
                    log.info("Total list size: " + response.getCompleteListSize().toString());
                    log.info("resumption token is: " + resumptionToken);

                    if (resumptionToken != null && resumptionToken.length() > 0) {
                        log.info("Please wait a while. System is waiting for next repository harvesting...");
                        TimeUnit.MILLISECONDS.sleep(DEFAULT_DELAY_INTERVAL_IN_MILLIS);
                    }

                }
                catch (UniformInterfaceException ue) {

                    switch (ue.getResponse().getStatus()) {
                        case HttpStatus.SC_SERVICE_UNAVAILABLE:
                            // delay for retry-interval
                            TimeUnit.MILLISECONDS.sleep(this.determineRetryAfterInterval(ue.getResponse()));
                            break;
                        case HttpStatus.SC_MOVED_TEMPORARILY:
                            log.warning("The repository is moved temporarily. The tool is about to end since it does not support this behavior");
                            return; // do not support service temporary moved feature
                        default:
                            log.severe("Server error. The error was:");
                            ue.printStackTrace();
                            return;
                    }
                }
            }
            while(resumptionToken != null && resumptionToken.length() > 0);

            log.info("Total Parsed Records: " + totalRecords);

        }
        catch (BadArgumentException be) {
            log.severe(be.getMessage());
        }
        catch (Exception e) {
            log.severe("Some errors occur. The error was:");
            e.printStackTrace();
        }
    }

    private long determineRetryAfterInterval(ClientResponse clientResponse) {
        String retryAfter = clientResponse.getHeaders().getFirst(
                HTTP_HEADER_RETRY_AFTER);
        long retryInterval = DEFAULT_DELAY_INTERVAL_IN_MILLIS;
        if (retryAfter != null) {
            try {
                retryInterval = Long.parseLong(retryAfter)*1000 + 50; // add 50ms addition to make sure repository response
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

        return retryInterval;
    }
}
