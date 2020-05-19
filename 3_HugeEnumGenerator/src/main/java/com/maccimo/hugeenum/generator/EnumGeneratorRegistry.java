package com.maccimo.hugeenum.generator;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

public class EnumGeneratorRegistry {

    public static final EnumGeneratorRegistry INSTANCE = new EnumGeneratorRegistry();

    private final Map<String, IEnumGeneratorFactory> factories = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    private EnumGeneratorRegistry() {
        addFactory(new ExtractMethodHugeEnumGeneratorFactory());
        addFactory(new ConDyHugeEnumGeneratorFactory());
        addFactory(new UnsafeHugeEnumGeneratorFactory());
    }

    private void addFactory(IEnumGeneratorFactory generatorFactory) {
        factories.put(generatorFactory.getId(), generatorFactory);
    }

    public Collection<IEnumGeneratorFactory> getFactories() {
        return factories.values();
    }

    public IEnumGeneratorFactory getById(String id) {
        return factories.get(id);
    }

}
