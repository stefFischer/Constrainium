package at.sfischer.traces.otel;

public class Link {

    private final String spanId;

    // attributes
    private final Attributes attributes;

    public Link(String spanId) {
        this.spanId = spanId;

        this.attributes = new Attributes();
    }

    public String getSpanId() {
        return spanId;
    }

    public void putAttributes(Attributes attributes){
        this.attributes.putAll(attributes);
    }
}