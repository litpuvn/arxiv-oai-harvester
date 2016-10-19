package arxiv;

import arxiv.oai.ArxivOAIHarvester;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class Arxiv {

    public static void main(String[] args) {
        ArxivOAIHarvester harvester = new ArxivOAIHarvester();
        Date releaseDate = getReleaseDate(args);

        harvester.listRecords(releaseDate);
    }

    private static Date getReleaseDate(String[] args) {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        Date today = new Date();
        Date releaseDate = new Date();

        if (args != null && args.length > 0) {
            try {
                releaseDate = df.parse(args[0]);
                if (releaseDate.after(today) || releaseDate.equals(today)) {
                    releaseDate = yesterday();
                }
            }
            catch (ParseException pe) {
                releaseDate = yesterday();
            }
        }

        return releaseDate;
    }

    private static Date yesterday() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);

        return cal.getTime();
    }
}
