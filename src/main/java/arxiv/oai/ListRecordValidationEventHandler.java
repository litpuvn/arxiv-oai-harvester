package arxiv.oai;

import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;

public class ListRecordValidationEventHandler implements ValidationEventHandler {

    public boolean handleEvent(ValidationEvent validationEvent) {

        System.out.println("Event on xml validation: " + validationEvent.getMessage());

        return true;
    }
}
