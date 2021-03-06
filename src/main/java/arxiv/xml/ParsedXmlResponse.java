package arxiv.xml;

import arxiv.model.data.ArticleMetadata;
import lombok.Builder;
import lombok.Value;

import java.math.BigInteger;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * The information parsed from a successful XML response from arXiv's OAI repository.
 */
@Value
@Builder
public class ParsedXmlResponse {

    private ZonedDateTime responseDate;

    /**
     * List of records returned by the repository.  It may be empty.
     */
    private List<ArticleMetadata> records;

    /**
     * Resumption token information.
     */
    private String resumptionToken;
    private BigInteger cursor;
    private BigInteger completeListSize;

}
