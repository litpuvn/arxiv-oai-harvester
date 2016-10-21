package arxiv;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;

import arxiv.exception.BadArgumentException;
import arxiv.oai.ArxivOAIHarvester;
import arxiv.xml.ParsedXmlResponse;
import org.apache.commons.cli.*;

public class Cli {
    private String[] args = null;
    private Options options = new Options();

    private static final Logger log = Logger.getLogger(Cli.class.getName());

    public Cli(String[] args) {
        this.args = args;

        options.addOption("f", "fromDate", true, "From date (YYYY-MM-DD)");
        options.addOption("u", "untilDate", true, "Until date (YYYY-MM-DD");
    }

    public void parse() {
        CommandLineParser parser = new BasicParser();
        CommandLine cmd;
        Date fromDate;
        Date untilDate = null;

        try {
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            Date today = new Date();

            cmd = parser.parse(options, args);

            fromDate = df.parse(cmd.getOptionValue("f", df.format(today)));
            if (cmd.hasOption("u")) {
                untilDate = df.parse(cmd.getOptionValue("u", df.format(today)));
            }

            log.info("Harvesting...");

            ArxivOAIHarvester harvester = new ArxivOAIHarvester();
            ParsedXmlResponse response = harvester.listRecords(fromDate, untilDate);

            log.info("Found: " + response.getRecords().size() + " records");
        }
        catch (org.apache.commons.cli.ParseException pe) {
            pe.printStackTrace();
            log.warning("Invalid command line options");
        }
        catch (java.text.ParseException jpe) {
            log.warning("Invalid fromDate or untilDate inputs");
        }
        catch (BadArgumentException be) {
            log.severe(be.getMessage());
        }
        catch (Exception e) {
            log.severe("Some errors occur. The error was:");
            e.printStackTrace();
        }
    }

}
