package org.ugoptimizer.database.importers;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.ugoptimizer.database.importers.CsvRecordParser.CsvRecord;

class CsvRecordParserTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void parsesQuotedCommasEscapedQuotesEmptyFieldsAndTrailingEmptyField()
            throws Exception {
        Path csv = temporaryDirectory.resolve("quoted.csv");
        Files.writeString(
                csv,
                "id,place,description,optional,trailing\n"
                        + "1,\"Hall, Annex\",\"He said \"\"ready\"\"\",,\n",
                StandardCharsets.UTF_8);

        try (CsvRecordParser parser = new CsvRecordParser(csv)) {
            CsvRecord header = parser.nextRecord();
            CsvRecord data = parser.nextRecord();

            assertEquals(1, header.physicalRowNumber());
            assertArrayEquals(
                    new String[]{"id", "place", "description", "optional", "trailing"},
                    header.values());
            assertEquals(2, data.physicalRowNumber());
            assertArrayEquals(
                    new String[]{"1", "Hall, Annex", "He said \"ready\"", "", ""},
                    data.values());
            assertNull(parser.nextRecord());
        }
    }

    @Test
    void rejectsUnterminatedQuotedFieldWithPhysicalRowNumber() throws Exception {
        assertMalformedRow(
                "1,\"unterminated",
                "Unterminated quoted field");
    }

    @Test
    void rejectsNonCommaCharacterAfterClosingQuoteWithPhysicalRowNumber()
            throws Exception {
        assertMalformedRow(
                "1,\"closed\"x",
                "Unexpected character after a closing quote");
    }

    @Test
    void rejectsQuoteBeginningInsideUnquotedFieldWithPhysicalRowNumber()
            throws Exception {
        assertMalformedRow(
                "1,unquoted\"value",
                "A quoted field must begin with a double quote");
    }

    private void assertMalformedRow(String malformedRow, String expectedReason)
            throws Exception {
        Path csv = temporaryDirectory.resolve("malformed.csv");
        Files.writeString(
                csv,
                "id,value\n" + malformedRow + "\n",
                StandardCharsets.UTF_8);

        try (CsvRecordParser parser = new CsvRecordParser(csv)) {
            assertEquals(1, parser.nextRecord().physicalRowNumber());
            IOException exception = assertThrows(IOException.class, parser::nextRecord);
            assertTrue(exception.getMessage().contains("row 2"));
            assertTrue(exception.getMessage().contains(expectedReason));
        }
    }
}
