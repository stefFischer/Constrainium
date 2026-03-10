package at.sfischer.generator.random;

import at.sfischer.constraints.data.DataObject;
import at.sfischer.constraints.data.DataSchemaEntry;
import at.sfischer.constraints.data.DataValue;
import at.sfischer.constraints.data.SimpleDataSchema;
import at.sfischer.constraints.model.ArrayType;
import at.sfischer.constraints.model.Type;
import at.sfischer.constraints.model.TypeEnum;
import at.sfischer.generator.InputGenerator;
import at.sfischer.generator.random.faker.FakerGenerator;
import net.datafaker.Faker;

import java.lang.reflect.Array;
import java.util.Random;
import java.util.Set;
import java.util.stream.Stream;

public class RandomInputGenerator implements InputGenerator {

    private final Random random;
    private final Faker faker;

    private final RandomNumberGenerationStrategy numberStrategy;

    private RandomNumberGenerationStrategy currentNumberStrategy;

    public RandomInputGenerator(Long seed, RandomNumberGenerationStrategy numberStrategy) {
        this.random = seed == null ? new Random() : new Random(seed);
        this.faker = new Faker(this.random);
        this.numberStrategy = numberStrategy;
    }

    @Override
    public String getIdentifier() {
        return RandomGeneratorProvider.GENERATOR_NAME;
    }

    @Override
    public Stream<DataObject> generate(SimpleDataSchema schema) {
        return Stream.generate(() -> generateInput(schema));
    }

    private DataObject generateInput(SimpleDataSchema schema) {
        if(numberStrategy == null){
            currentNumberStrategy = RandomNumberGenerationStrategy.values()[faker.random().nextInt(RandomNumberGenerationStrategy.values().length)];
        } else {
            currentNumberStrategy = numberStrategy;
        }

        return generateObject(schema);
    }

    private DataObject generateObject(SimpleDataSchema schema) {
        DataObject object = new DataObject();
        Set<String> entryNames = schema.getSchemaEntryNames();
        for (String entryName : entryNames) {
            DataSchemaEntry<SimpleDataSchema> entry = schema.getSchemaEntry(entryName);
            generateValueForSchemaEntry(object, entry);
        }

        return object;
    }

    private void generateValueForSchemaEntry(DataObject object, DataSchemaEntry<SimpleDataSchema> entry){
        if(entry.type instanceof ArrayType arrayType){
            Object array = generateArrayForSchemaEntry(entry, arrayType.elementType());
            putArrayValue(object, entry.name, arrayType, array);
        } else if(entry.type instanceof TypeEnum type){
            switch (type) {
                case INTEGER -> object.putValue(entry.name, generateValueForSchemaEntry(entry, TypeEnum.INTEGER, Integer.class));
                case NUMBER -> object.putValue(entry.name, generateValueForSchemaEntry(entry, TypeEnum.NUMBER, Number.class));
                case BOOLEAN -> object.putValue(entry.name, Boolean.TRUE.equals(generateValueForSchemaEntry(entry, TypeEnum.BOOLEAN, Boolean.class)));
                case STRING -> object.putValue(entry.name, generateValueForSchemaEntry(entry, TypeEnum.STRING, String.class));
                case COMPLEXTYPE -> object.putValue(entry.name, generateValueForSchemaEntry(entry, TypeEnum.COMPLEXTYPE, DataObject.class));
                case ANY -> throw new UnsupportedOperationException("Type ANY is not supported.");
            }
        }
    }

    private <T> T generateValueForSchemaEntry(DataSchemaEntry<SimpleDataSchema> entry, TypeEnum type, Class<T> typeClazz){
        Object value = FakerGenerator.generate(entry, faker);
        if(value != null){
            switch (value) {
                case Integer numberValue when (type == TypeEnum.INTEGER || type == TypeEnum.NUMBER || type == TypeEnum.ANY) -> {
                    return typeClazz.cast(numberValue);
                }
                case Number numberValue when (type == TypeEnum.NUMBER || type == TypeEnum.ANY) -> {
                    return typeClazz.cast(numberValue);
                }
                case String stringValue when (type == TypeEnum.STRING || type == TypeEnum.ANY) -> {
                    return typeClazz.cast(stringValue);
                }
                default -> {
                }
            }
        }

        return switch (type) {
            // TODO We need to distinguish between integer and floating point numbers to generate meaningful inputs.
            case INTEGER -> typeClazz.cast(generateRandomInteger());
            case NUMBER -> typeClazz.cast(generateRandomNumber());
            case BOOLEAN -> typeClazz.cast(generateRandomBoolean());
            case STRING -> typeClazz.cast(generateRandomString());
            case COMPLEXTYPE -> typeClazz.cast(generateObject(entry.dataSchema));
            case ANY -> throw new UnsupportedOperationException("Type ANY is not supported.");
        };
    }

    private Object generateArrayForSchemaEntry(DataSchemaEntry<SimpleDataSchema> entry, Type elementType){
        if (elementType instanceof TypeEnum t) {
            int len = faker.random().nextInt(10);
            Object array = Array.newInstance(getClassForType(t), len);
            for (int i = 0; i < len; i++) {
                Object element = generateValueForSchemaEntry(entry, t, getClassForType(t));
                Array.set(array, i, element);
            }

            return array;

        } else if (elementType instanceof ArrayType(Type nestedType)){
            int len = faker.random().nextInt(10);
            Object array = Array.newInstance(DataValue.class, len);
            for (int i = 0; i < len; i++) {
                Object element = generateArrayForSchemaEntry(entry, nestedType);
                DataObject valueDao = new DataObject();
                putArrayValue(valueDao, entry.name, (ArrayType)elementType, element);
                Array.set(array, i, valueDao.getDataValue(entry.name));
            }

            return array;
        }

        return null;
    }

    private static Class<?> getClassForType(Type type) {
        if (type instanceof TypeEnum t) {
            return switch (t) {
                case INTEGER -> Integer.class;
                case NUMBER -> Number.class;
                case BOOLEAN -> Boolean.class;
                case STRING -> String.class;
                case COMPLEXTYPE -> DataObject.class;
                default -> Object.class;
            };
        }

        throw new IllegalArgumentException("Unsupported type: " + type);
    }

    private void putArrayValue(DataObject object, String name, ArrayType arrayType, Object array) {
        if(arrayType.elementType() instanceof ArrayType){
            object.putValue(name, (DataValue<?>[]) array, arrayType.elementType());
            return;
        }

        if(arrayType.elementType() instanceof TypeEnum type){
            switch(type){
                case BOOLEAN -> object.putValue(name, (Boolean[]) array);
                case INTEGER -> object.putValue(name, (Integer[]) array);
                case NUMBER -> object.putValue(name, (Number[]) array);
                case STRING -> object.putValue(name, (String[]) array);
                case COMPLEXTYPE -> object.putValue(name, (DataObject[]) array);
                default -> throw new IllegalArgumentException("Unsupported type");
            }
        }
    }

    private Integer generateRandomInteger(){
        if(this.currentNumberStrategy == null){
            return this.random.nextInt();
        }

        int[] boundaries = {
                0,
                1,
                -1,
                Integer.MAX_VALUE,
                Integer.MIN_VALUE
        };

        return switch (this.currentNumberStrategy){
            case SMALL -> random.nextInt(100);
            case MEDIUM -> random.nextInt(10_000);
            case FULL -> random.nextInt();
            case BOUNDARIES -> boundaries[random.nextInt(boundaries.length)];
        };
    }

    private Number generateRandomNumber(){
        if(this.currentNumberStrategy == null){
            return this.random.nextLong() * this.random.nextDouble();
        }

        double[] boundaries = {
                0.0,
                1.0,
                -1.0,
                Double.MAX_VALUE,
                Double.MIN_VALUE
        };

        return switch (this.currentNumberStrategy){
            case SMALL -> random.nextDouble() * 100;
            case MEDIUM -> random.nextDouble() * 10_000;
            case FULL -> random.nextDouble() * Long.MAX_VALUE;
            case BOUNDARIES -> boundaries[random.nextInt(boundaries.length)];
        };
    }

    private Boolean generateRandomBoolean(){
        return this.random.nextBoolean();
    }

    private String generateRandomString() {
        int choice = faker.random().nextInt(2);
        return switch (choice) {
            case 0 -> faker.lorem().characters(faker.random().nextInt(3, 25));
            default -> generateRandomCharSeq();
        };
    }

    private String generateRandomCharSeq() {
        int length = faker.number().numberBetween(3, 20);
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = faker.random().nextInt(chars.length());
            sb.append(chars.charAt(index));
        }

        return sb.toString();
    }
}
