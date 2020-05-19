package com.maccimo.hugeenum.generator;

import java.util.List;

public class UnsafeHugeEnumGeneratorFactory implements IEnumGeneratorFactory {

    @Override
    public String getId() {
        return "Unsafe";
    }

    @Override
    public String getDescription() {
        return "Employ sun.misc.Unsafe for enum elements initialization";
    }

    @Override
    public int getDefaultElementCount() {
        // Maximum for UnsafeEnumGenerator depend on element names length.
        // Number here is pre-calculated for 11-character element name assuming each character occupy
        // exactly 1 byte in UTF-8 encoding. This is true only for (0 < character code < 128)
        return 65_410;
    }

    @Override
    public int getMaximumElementCount() {
        // Maximum element count for this generator is a tricky thing and may vary depending on element names length.
        // So we return here number exceeding real maximum.
        return 65_535;
    }

    @Override
    public IEnumGenerator create(String name, List<String> elementNames) {
        return new UnsafeHugeEnumGenerator(name, elementNames);
    }

}
