package at.sfischer.traces.otel;

public class Attribute<T> {

    private final String key;

    private final T value;

    public Attribute(String key, T value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public T getValue() {
        return value;
    }

    @Override
    public String toString() {
        return key + ": " + valueString();
    }

    private String valueString(){
        if(value.getClass().isArray()){
            Object[] a = (Object[])value;
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Object o : a) {
                if(!first){
                    sb.append(",");
                }
                sb.append(o);
                first = false;
            }
            sb.append("]");
            return sb.toString();
        }

        return value.toString();
    }
}
