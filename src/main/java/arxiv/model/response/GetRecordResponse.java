package arxiv.model.response;

import arxiv.model.data.ArticleMetadata;
import arxiv.model.request.ArxivRequest;
import arxiv.model.request.GetRecordRequest;
import lombok.Builder;
import lombok.Value;

import java.time.ZonedDateTime;

@Value
@Builder
public class GetRecordResponse implements ArxivResponseInterface {

    /**
     * Response datetime.
     */
    private ZonedDateTime responseDate;

    /**
     * The original request sent to the arXiv OAI repository.
     */
    private GetRecordRequest request;

    /**
     * Record returned by the repository.  It will be null if no record was found.
     */
    private ArticleMetadata record;

    @Override
    public ZonedDateTime getResponseDate() {
        return null;
    }

    @Override
    public ArxivRequest getRequest() {
        return null;
    }
}
