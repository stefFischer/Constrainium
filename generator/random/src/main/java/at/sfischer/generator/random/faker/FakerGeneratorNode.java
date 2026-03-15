package at.sfischer.generator.random.faker;

import at.sfischer.constraints.data.DataSchemaEntry;
import net.datafaker.Faker;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;

import java.util.LinkedHashSet;
import java.util.Set;

public class FakerGeneratorNode implements IFakerGeneratorNode {

    private final LinkedHashSet<String> identifiers;
    private final LinkedHashSet<IFakerGeneratorNode> nodes;

    protected FakerGeneratorNode() {
        this(new LinkedHashSet<>(), new LinkedHashSet<>());
    }

    protected FakerGeneratorNode(String... identifiers) {
        this(new LinkedHashSet<>(Set.of(identifiers)), new LinkedHashSet<>());
    }

    protected FakerGeneratorNode(LinkedHashSet<String> identifiers, LinkedHashSet<IFakerGeneratorNode> nodes) {
        this.identifiers = identifiers;
        this.nodes = nodes;
    }

    @Override
    public Set<String> identifiers() {
        return identifiers;
    }

    private static String normalize(String name) {
        return name
                .replaceAll("[^a-zA-Z]", "")
                .toLowerCase();
    }

    private static double similarity(String a, String b) {
        JaroWinklerSimilarity sim = new JaroWinklerSimilarity();
        return sim.apply(a, b);
    }

    private IFakerGeneratorNode findBestMatchingNode(DataSchemaEntry<?> entry) {
        String fullName = normalize(entry.getQualifiedName());
        String simpleName = normalize(entry.name);

        IFakerGeneratorNode bestNode = null;
        double bestScore = 0.0;
        for (IFakerGeneratorNode node : nodes) {
            for (String identifier : node.identifiers()) {
                double score = similarity(fullName, normalize(identifier));
                if (score > bestScore) {
                    bestScore = score;
                    bestNode = node;
                }

                score = similarity(simpleName, normalize(identifier));
                if (score > bestScore) {
                    bestScore = score;
                    bestNode = node;
                }
            }
        }

        // minimum threshold to avoid bad matches
        double MIN_SCORE = 0.75;
        if (bestScore >= MIN_SCORE) {
            return bestNode;
        }

        return null;
    }

    @Override
    public Object generate(DataSchemaEntry<?> entry, Faker faker) {
        IFakerGeneratorNode node = findBestMatchingNode(entry);
        if (node == null) {
            return null;
        }

        DataSchemaEntry<?> parent = entry.getParentSchemaEntry() != null
                        ? entry.getParentSchemaEntry()
                        : entry;

        Object value = node.generate(parent, faker);
        if (value != null) {
            return value;
        }

        return node.generate(entry, faker);

//        for (IFakerGeneratorNode node : nodes) {
//            for (String identifier : node.identifiers()) {
//                if(identifier.contains(name)){
//                    DataSchemaEntry<?> parent;
//                    if(entry.getParentSchemaEntry() != null){
//                        parent = entry.getParentSchemaEntry();
//                    } else {
//                        parent = entry;
//                    }
//
//                    return node.generate(parent, faker);
//                }
//            }
//        }
//
//        return null;
    }

    public FakerGeneratorNode nodes(IFakerGeneratorNode... nodes){
        this.nodes.addAll(Set.of(nodes));
        return this;
    }
}
