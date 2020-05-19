package com.maccimo.hugeenum.generator;

import java.util.List;

public interface IEnumGeneratorFactory {

    public String getId();

    public String getDescription();

    public int getDefaultElementCount();

    public int getMaximumElementCount();

    public IEnumGenerator create(String name, List<String> elementNames);

}
