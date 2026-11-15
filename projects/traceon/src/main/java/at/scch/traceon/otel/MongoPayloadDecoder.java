package at.scch.traceon.otel;


import at.sfischer.constraints.data.DataObject;
import org.bson.RawBsonDocument;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class MongoPayloadDecoder implements PayloadDecoder {

    @Override
    public DataObject decode(String payload) {
        return DataObject.parseData("{payload:" + decodeBson(payload) + "}");
    }

    public static String decodeBson(String base64String) {
        byte[] bytes = Base64.getDecoder().decode(base64String);

        // Case 1: Base64-encoded JSON
        String text = new String(bytes, StandardCharsets.UTF_8).stripLeading();
        if (text.startsWith("{") || text.startsWith("[")) {
            return text;
        }

        if (bytes.length < 5) {
            throw new IllegalArgumentException("Payload is too short to contain BSON; text: " + text);
        }

        ByteBuffer b = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);

        // Case 2: MongoDB wire message
        if (bytes.length >= 16 && b.getInt(0) == bytes.length) {
            int opcode = b.getInt(12);
            switch (opcode) {
                case 1: return decodeReply(bytes);
                case 2004: return decodeQuery(bytes);
                case 2013: return decodeMessage(bytes);

                case 2012:
                    throw new IllegalArgumentException("OP_COMPRESSED detected. Decompress the message using its compressor before parsing.");

                default:
                    // Continue to standalone BSON detection.
            }
        }

        // Case 3: Standalone BSON document
        if (b.getInt(0) == bytes.length && bytes[bytes.length - 1] == 0) {
            return new RawBsonDocument(bytes).toJson();
        }

        throw new IllegalArgumentException("Unknown payload format; length=" + bytes.length + "; text: " + text);
    }

    // OP_REPLY: header + flags + cursor ID + startingFrom
    // + numberReturned + BSON documents
    private static String decodeReply(byte[] bytes) {
        ByteBuffer b = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        b.position(32);
        int count = b.getInt();
        if (count < 0) {
            throw new IllegalArgumentException("Invalid OP_REPLY document count");
        }

        List<String> documents = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            documents.add(readDocument(b, bytes.length));
        }

        requireConsumed(b, bytes.length, "OP_REPLY");
        return asJsonArray(documents);
    }

    // OP_QUERY: header + flags + collection name + skip/return
    // + query BSON + optional projection BSON
    private static String decodeQuery(byte[] bytes) {
        ByteBuffer b = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        b.position(20); // Header plus query flags

        String collection = readCString(b);
        b.getInt(); // numberToSkip
        b.getInt(); // numberToReturn

        List<String> documents = new ArrayList<>();
        documents.add(readDocument(b, bytes.length));
        if (b.hasRemaining()) {
            documents.add(readDocument(b, bytes.length));
        }

        requireConsumed(b, bytes.length, "OP_QUERY");
        return "{\"collection\":\"" + escapeJson(collection)
                + "\",\"documents\":" + asJsonArray(documents) + "}";
    }

    // OP_MSG: header + flags + sections + optional checksum
    private static String decodeMessage(byte[] bytes) {
        ByteBuffer b = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        b.position(16);
        int flags = b.getInt();
        int end = bytes.length - ((flags & 1) != 0 ? 4 : 0);
        if (end < b.position()) {
            throw new IllegalArgumentException("Invalid OP_MSG checksum");
        }

        List<String> documents = new ArrayList<>();
        while (b.position() < end) {
            int kind = Byte.toUnsignedInt(b.get());
            if (kind == 0) {
                // One BSON document
                documents.add(readDocument(b, end));

            } else if (kind == 1) {
                // Document sequence: size, identifier, BSON documents
                int sectionStart = b.position();
                int size = b.getInt();
                int sectionEnd = sectionStart + size;
                if (size < 5 || sectionEnd > end) {
                    throw new IllegalArgumentException("Invalid OP_MSG section size");
                }

                String identifier = readCString(b);
                while (b.position() < sectionEnd) {
                    documents.add(readDocument(b, sectionEnd));
                }

                if (b.position() != sectionEnd) {
                    throw new IllegalArgumentException("Invalid OP_MSG document sequence");
                }

            } else {
                throw new IllegalArgumentException("Unsupported OP_MSG section kind: " + kind);
            }
        }

        if (b.position() != end) {
            throw new IllegalArgumentException("Invalid OP_MSG section boundaries");
        }

        return asJsonArray(documents);
    }

    private static String readDocument(ByteBuffer b, int end) {
        int start = b.position();
        if (end - start < 4) {
            throw new IllegalArgumentException("Incomplete BSON document length");
        }

        int size = b.getInt(start);
        if (size < 5 || size > end - start) {
            throw new IllegalArgumentException("Invalid BSON document size: " + size);
        }

        byte[] document = new byte[size];
        b.get(document);
        return new RawBsonDocument(document).toJson();
    }

    private static String readCString(ByteBuffer b) {
        int start = b.position();
        while (b.hasRemaining() && b.get() != 0) {
            // Find the null terminator.
        }

        if (!b.hasRemaining() && b.get(b.limit() - 1) != 0) {
            throw new IllegalArgumentException("Unterminated MongoDB cstring");
        }

        int end = b.position() - 1;
        return new String(b.array(), start, end - start, StandardCharsets.UTF_8);
    }

    private static String asJsonArray(List<String> documents) {
        return "[" + String.join(",", documents) + "]";
    }

    private static void requireConsumed(ByteBuffer b, int end, String type) {
        if (b.position() != end) {
            throw new IllegalArgumentException(type + ": unexpected trailing bytes");
        }
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

