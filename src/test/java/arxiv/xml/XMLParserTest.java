package arxiv.xml;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

import static org.junit.Assert.*;


public class XMLParserTest {

    private XMLParser parser = new XMLParser();


    @Test
    public void parseGetRecord() throws Exception {
        String getRecordContent = readFile("src/main/resources/GetRecord.xml");
        ParsedXmlResponse response = parser.parse(getRecordContent);

        assertEquals(response.getRecords().size(), 1);
    }

    @Test
    public void parseListRecords() throws Exception {
        String getRecordContent = readFile("src/main/resources/ListRecords.xml");
        ParsedXmlResponse response = parser.parse(getRecordContent);

        assertEquals(response.getRecords().size(), 387);
    }

    private String readFile(String pathname) throws IOException {

        File file = new File(pathname);
        StringBuilder fileContents = new StringBuilder((int)file.length());
        Scanner scanner = new Scanner(file);
        String lineSeparator = System.getProperty("line.separator");

        try {
            while(scanner.hasNextLine()) {
                fileContents.append(scanner.nextLine() + lineSeparator);
            }
            return fileContents.toString();
        } finally {
            scanner.close();
        }
    }
}