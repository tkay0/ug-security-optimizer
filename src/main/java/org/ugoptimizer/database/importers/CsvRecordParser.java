package org.ugoptimizer.database.importers;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Reads one-record-per-line CSV files while supporting quoted commas, escaped
 * double quotes, empty fields, and trailing empty fields.
 *
 * <p>The canonical project datasets contain no embedded line breaks inside
 * quoted fields, so keeping physical lines aligned with CSV records gives clear
 * and useful error row numbers without introducing a full CSV framework.</p>
 */
final class CsvRecordParser implements AutoCloseable {

    private static final int INITIAL_FIELD_CAPACITY = 8;

    record CsvRecord(int physicalRowNumber, String[] values) {
    }

    private final Path source;
    private final BufferedReader reader;
    private int physicalRowNumber;

    CsvRecordParser(Path source) throws IOException {
        this.source = source;
        this.reader = Files.newBufferedReader(source, StandardCharsets.UTF_8);
    }

    CsvRecord nextRecord() throws IOException {
        String line = reader.readLine();
        if (line == null) {
            return null;
        }
        physicalRowNumber++;
        return new CsvRecord(physicalRowNumber, parseLine(line));
    }

    private String[] parseLine(String line) throws IOException {
        String[] fields = new String[INITIAL_FIELD_CAPACITY];
        int fieldCount = 0;
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        boolean quoteClosed = false;

        for (int index = 0; index < line.length(); index++) {
            char current = line.charAt(index);

            if (inQuotes) {
                if (current == '"') {
                    if (index + 1 < line.length() && line.charAt(index + 1) == '"') {
                        field.append('"');
                        index++;
                    } else {
                        inQuotes = false;
                        quoteClosed = true;
                    }
                } else {
                    field.append(current);
                }
                continue;
            }

            if (quoteClosed) {
                if (current != ',') {
                    throw error("Unexpected character after a closing quote");
                }
                fields = ensureFieldCapacity(fields, fieldCount + 1);
                fields[fieldCount++] = field.toString();
                field.setLength(0);
                quoteClosed = false;
                continue;
            }

            if (current == ',') {
                fields = ensureFieldCapacity(fields, fieldCount + 1);
                fields[fieldCount++] = field.toString();
                field.setLength(0);
            } else if (current == '"') {
                if (field.length() != 0) {
                    throw error("A quoted field must begin with a double quote");
                }
                inQuotes = true;
            } else {
                field.append(current);
            }
        }

        if (inQuotes) {
            throw error("Unterminated quoted field");
        }
        fields = ensureFieldCapacity(fields, fieldCount + 1);
        fields[fieldCount++] = field.toString();

        String[] result = new String[fieldCount];
        System.arraycopy(fields, 0, result, 0, fieldCount);
        return result;
    }

    private String[] ensureFieldCapacity(String[] fields, int requiredCapacity)
            throws IOException {
        if (requiredCapacity <= fields.length) {
            return fields;
        }
        if (fields.length > Integer.MAX_VALUE / 2) {
            throw error("CSV field count exceeds the supported array capacity");
        }

        int newCapacity = fields.length * 2;
        if (newCapacity < requiredCapacity) {
            newCapacity = requiredCapacity;
        }
        String[] expanded = new String[newCapacity];
        System.arraycopy(fields, 0, expanded, 0, fields.length);
        return expanded;
    }

    private IOException error(String reason) {
        return new IOException(
                source + " row " + physicalRowNumber + ": " + reason);
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }
}
