package com.maccimo.hugeenum.generator;

public class BytesClassLoader extends ClassLoader {

    public Class<?> defineClass(String name, byte[] bytes) {
        return super.defineClass(name, bytes, 0, bytes.length);
    }

}
