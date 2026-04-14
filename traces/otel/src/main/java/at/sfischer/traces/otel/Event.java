package at.sfischer.traces.otel;

public class Event {

    private final String name;

    private final long epochNanos;

    // attributes
    private final Attributes attributes;

    public Event(String name, long epochNanos) {
        this.name = name;
        this.epochNanos = epochNanos;

        this.attributes = new Attributes();
    }

    public String getName() {
        return name;
    }

    public long getEpochNanos() {
        return epochNanos;
    }

    public void putAttributes(Attributes attributes){
        this.attributes.putAll(attributes);
    }

    public <T> T getAttribute(String key){
        return this.attributes.get(key);
    }

    public <T> T removeAttribute(String key){
        return this.attributes.remove(key);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(epochNanos + ": (");
        sb.append(name);
        sb.append("): ");
        boolean first = true;
        for (Attribute<?> attribute : this.attributes.getAttributes()) {
            if(!first){
                sb.append("; ");
            }
            sb.append(attribute);
            first = false;
        }

        return sb.toString();
    }
}