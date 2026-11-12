package at.scch.traceon.otel;

import at.sfischer.constraints.data.DataObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RedisPayloadDecoder implements PayloadDecoder {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public DataObject decode(String payload) {
        RespReader reader = new RespReader(Base64.getDecoder().decode(payload));

        List<Object> values = new ArrayList<>();
        while (reader.hasRemaining()) {
            values.add(reader.readValue());
        }

        Object decoded = values.size() == 1 ? values.get(0) : values;
        try {
            String json = MAPPER.writeValueAsString(decoded);
            return DataObject.parseData("{payload:" + json + "}");
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Could not convert RESP payload to JSON", e);
        }
    }

    private static final class RespReader {

        private final byte[] bytes;
        private int position;

        private RespReader(byte[] bytes) {
            this.bytes = bytes;
        }

        private boolean hasRemaining() {
            return position < bytes.length;
        }

        private Object readValue() {
            int type = readByte();
            return switch (type) {
                case '+' -> readLine();
                case '-' -> readLine();
                case ':' -> readLong(readLine());
                case '$' -> readBulkString();
                case '*' -> readArray();
                case '%' -> readMap();
                case '~' -> readArray();
                case '#' -> readBoolean(readLine());
                case ',' -> readDouble(readLine());
                case '_' -> {
                    readLine();
                    yield null;
                }
                case '(' -> readLine();
                case '=' -> readVerbatimString();
                case '>' -> readArray();
                case '!' -> readBlobError();
                default -> throw new IllegalArgumentException("Unsupported RESP type: " + (char) type);
            };
        }

        private String readLine() {
            int start = position;
            while (position < bytes.length) {
                if (bytes[position] == '\r' && position + 1 < bytes.length && bytes[position + 1] == '\n') {
                    String line = new String(bytes, start, position - start, StandardCharsets.UTF_8);
                    position += 2;
                    return line;
                }
                position++;
            }

            throw new IllegalArgumentException("Unterminated RESP line");
        }

        private int readByte() {
            if (!hasRemaining()) {
                throw new IllegalArgumentException("Unexpected end of RESP payload");
            }

            return Byte.toUnsignedInt(bytes[position++]);
        }

        private Object readBulkString() {
            int length = readLength(readLine());
            if (length == -1) {
                return null;
            }

            String value = readString(length);
            requireCrlf();
            return value;
        }

        private String readVerbatimString() {
            int length = readLength(readLine());
            if (length == -1) {
                return null;
            }

            String value = readString(length);
            requireCrlf();
            int separator = value.indexOf(':');
            return separator >= 0 ? value.substring(separator + 1) : value;
        }

        private String readBlobError() {
            int length = readLength(readLine());
            if (length == -1) {
                return null;
            }

            String value = readString(length);
            requireCrlf();
            return value;
        }

        private String readString(int length) {
            if (length < 0 || position + length > bytes.length) {
                throw new IllegalArgumentException("Invalid RESP string length: " + length);
            }

            String value = new String(bytes, position, length, StandardCharsets.UTF_8);
            position += length;
            return value;
        }

        private List<Object> readArray() {
            int length = readLength(readLine());
            if (length == -1) {
                return null;
            }
            if (length < -1) {
                throw new IllegalArgumentException("Invalid RESP array length: " + length);
            }

            List<Object> values = new ArrayList<>(length);
            for (int i = 0; i < length; i++) {
                values.add(readValue());
            }
            return values;
        }

        private Map<String, Object> readMap() {
            int length = readLength(readLine());
            if (length == -1) {
                return null;
            }
            if (length < -1) {
                throw new IllegalArgumentException("Invalid RESP map length: " + length);
            }

            Map<String, Object> values = new LinkedHashMap<>();
            for (int i = 0; i < length; i++) {
                Object key = readValue();
                Object value = readValue();
                values.put(key == null ? "null" : key.toString(), value);
            }
            return values;
        }

        private boolean readBoolean(String value) {
            return switch (value) {
                case "t" -> true;
                case "f" -> false;
                default -> throw new IllegalArgumentException("Invalid RESP boolean: " + value);
            };
        }

        private Number readDouble(String value) {
            return switch (value) {
                case "inf", "+inf" -> Double.POSITIVE_INFINITY;
                case "-inf" -> Double.NEGATIVE_INFINITY;
                case "nan" -> Double.NaN;
                default -> Double.parseDouble(value);
            };
        }

        private int readLength(String value) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid RESP length: " + value, e);
            }
        }

        private long readLong(String value) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid RESP integer: " + value, e);
            }
        }

        private void requireCrlf() {
            if (position + 1 >= bytes.length || bytes[position] != '\r' || bytes[position + 1] != '\n') {
                throw new IllegalArgumentException("Missing RESP CRLF terminator");
            }
            position += 2;
        }
    }
}
