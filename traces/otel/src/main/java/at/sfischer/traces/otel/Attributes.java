package at.sfischer.traces.otel;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Attributes {

    private final Map<String, Attribute<?>> attributes;

    public Attributes() {
        this.attributes = new HashMap<>();
    }

    private void put(String key, Object value){
        attributes.put(key, new Attribute<>(key, value));
    }

    public void put(String key, String value){
        put(key, (Object)value);
    }

    public void put(String key, long value){
        put(key, (Object)value);
    }

    public void put(String key, double value){
        put(key, (Object)value);
    }

    public void put(String key, char value){
        put(key, (Object)value);
    }

    public void put(String key, boolean value){
        put(key, (Object)value);
    }

    public void put(String key, String[] array){
        put(key, (Object)array);
    }

    public void put(String key, long[] array){
        put(key, (Object)array);
    }

    public void put(String key, double[] array){
        put(key, (Object)array);
    }

    public void put(String key, char[] array){
        put(key, (Object)array);
    }

    public void put(String key, boolean[] array){
        put(key, (Object)array);
    }

    public void putAll(Attributes attributes) {
        for (Map.Entry<String, Attribute<?>> entry : attributes.attributes.entrySet()) {
            put(entry.getKey(), entry.getValue().getValue());
        }
    }

    public <T> T get(String key){
        try {
            @SuppressWarnings("unchecked")
            Attribute<T> attribute = (Attribute<T>) attributes.get(key);
            if(attribute == null){
                return null;
            }

            return attribute.getValue();
        } catch (ClassCastException e) {
            return null;
        }
    }

    public <T> T remove(String key){
        try {
            @SuppressWarnings("unchecked")
            Attribute<T> attribute = (Attribute<T>) attributes.remove(key);
            if(attribute == null){
                return null;
            }

            return attribute.getValue();
        } catch (ClassCastException e) {
            return null;
        }
    }

    public Collection<Attribute<?>> getAttributes(){
        return this.attributes.values();
    }

    public Attributes getSubSet(String... attributeNames){
        Attributes subSet = new Attributes();
        for (String attributeName : attributeNames) {
            if(this.attributes.containsKey(attributeName)){
                subSet.put(attributeName, this.attributes.get(attributeName).getValue());
            }
        }

        return subSet;
    }
}
