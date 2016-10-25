package arxiv;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;
import arxiv.oai.ArxivOAIHarvester;
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
        ArxivOAIHarvester harvester = new ArxivOAIHarvester();
        ArxivDownloader downloader = new ArxivDownloader(harvester);

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

            downloader.download(fromDate, untilDate);
        }
        catch (org.apache.commons.cli.ParseException pe) {
            pe.printStackTrace();
            log.severe("Invalid command line options");
        }
        catch (java.text.ParseException jpe) {
            log.severe("Invalid fromDate or untilDate inputs. Expected format is YYYY-MM-DD");
        }
    }
}
