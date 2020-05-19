package com.maccimo.hugeenum.generator;

import java.util.List;

public class ExtractMethodHugeEnumGeneratorFactory implements IEnumGeneratorFactory {

    @Override
    public String getId() {
        return "ExtractMethod";
    }

    @Override
    public String getDescription() {
        return "Extract enum elements initialization code to separate method";
    }

    @Override
    public int getDefaultElementCount() {
        return getMaximumElementCount();
    }

    @Override
    public int getMaximumElementCount() {
        return 10_920;
    }

    @Override
    public IEnumGenerator create(String name, List<String> elementNames) {
        return new ExtractMethodHugeEnumGenerator(name, elementNames);
    }

}
