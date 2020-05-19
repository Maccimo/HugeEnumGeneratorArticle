package com.maccimo.hugeenum.generator;

import java.util.List;

public class ConDyHugeEnumGeneratorFactory implements IEnumGeneratorFactory {

    @Override
    public String getId() {
        return "ConDy";
    }

    @Override
    public String getDescription() {
        return "Employ Constant Dynamic (JEP 309) for enum elements initialization";
    }

    @Override
    public int getDefaultElementCount() {
        return getMaximumElementCount();
    }

    @Override
    public int getMaximumElementCount() {
        return 10_963;
    }

    @Override
    public IEnumGenerator create(String name, List<String> elementNames) {
        return new ConDyHugeEnumGenerator(name, elementNames);
    }

}
