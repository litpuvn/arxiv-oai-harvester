package arxiv;

import java.util.logging.Logger;

public class Arxiv {
    private static final Logger log = Logger.getLogger(Cli.class.getName());

    public static void main(String[] args) {
        log.info("Starting harvester...");
        new Cli(args).parse();
    }
}
