package arxiv.model.response;

import arxiv.model.request.ArxivRequest;
import com.google.common.collect.ImmutableList;
import arxiv.exception.BadResumptionTokenException;
import arxiv.model.data.ArticleMetadata;
import arxiv.model.request.ListRecordsRequest;
import arxiv.model.request.ResumeListRecordsRequest;
import lombok.Builder;
import lombok.Value;

import java.math.BigInteger;
import java.net.URISyntaxException;
import java.time.ZonedDateTime;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Value
@Builder
public class ListRecordsResponse implements ArxivResponse {

    /**
     * Response datetime.
     */
    private ZonedDateTime responseDate;

    /**
     * The original request sent to the arXiv OAI repository.
     */
    private ListRecordsRequest request;

    /**
     * Immutable list of records returned by the repository.  It will be empty if no records were found.
     */
    private ImmutableList<ArticleMetadata> records;

    /**
     * Resumption token, if there are more pages left in the response.  If there are no more pages left, this will be
     * null.
     */
    private String resumptionToken;

    /**
     * Position information, if there are more pages left in the response.  If there are no more pages left, this will
     * be null.
     */
    private BigInteger cursor;
    private BigInteger completeListSize;

    /**
     * @return whether or not there are more pages left in the response
     */
    public boolean hasResumption() {
        return !isBlank(resumptionToken);
    }

    /**
     * Create a {@link ListRecordsRequest} that resumes this request if there are more pages left in the response.
     * If there are no more pages left, this will return {@link ListRecordsRequest#NONE}.
     */
    public ListRecordsRequest resumption() {
        if (isBlank(resumptionToken)) {
            return ListRecordsRequest.NONE;
        }

        try {
            return new ResumeListRecordsRequest(resumptionToken, request);
        } catch (URISyntaxException e) {
            throw new BadResumptionTokenException(e);
        }
    }


    @Override
    public ZonedDateTime getResponseDate() {
        return null;
    }

    @Override
    public ArxivRequest getRequest() {
        return null;
    }
}
