package arxiv.model.response;

import arxiv.model.request.ArxivRequest;

import java.time.ZonedDateTime;

/**
 * Implementations of this interface represent a response from the arXiv OAI repository.
 */
public interface ArxivResponseInterface {

    /**
     * Get the response datetime
     */
    ZonedDateTime getResponseDate();

    /**
     * Get the original request to the harvester
     */
    ArxivRequest getRequest();

}
