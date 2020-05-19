package com.maccimo.hugeenum.generator;

import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

@SuppressWarnings("SpellCheckingInspection")
public class ConDyBootstrapClassGenerator {

    public static final int CLASS_FILE_SIGNATURE = 0xCAFEBABE;

    public static final int MAX_CONSTANT_POOL_SIZE = 65_535;

    private static final int CONSTANT_Utf8 = 1;
    private static final int CONSTANT_Integer = 3;
    private static final int CONSTANT_Class = 7;
    private static final int CONSTANT_Methodref = 10;
    private static final int CONSTANT_NameAndType = 12;
    private static final int CONSTANT_MethodHandle = 15;
    private static final int CONSTANT_Dynamic = 17;

    private static final int REF_newInvokeSpecial = 8;

    private static final String JAVA_LANG_ENUM = "java/lang/Enum";
    private static final String ATTRIBUTE_NAME_BOOTSTRAP_METHODS = "BootstrapMethods";
    private static final String BOOTSTRAP_METHOD_NAME = "<init>";
    private static final String BOOTSTRAP_METHOD_DESCRIPTOR = "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Class;I)V";

    private final int version;
    private final int access;
    private final String enumClassName;
    private final List<String> elementNames;
    private final ByteArrayOutputStream outputStream;
    private final DataOutput dataOutput;

    /**
     *
     * @param version Class file format version
     * @param access Class access flags
     * @param enumClassName Class name. May contain package name
     * @param elementNames List of enumeration element names
     */
    public ConDyBootstrapClassGenerator(int version, int access, String enumClassName, List<String> elementNames) {
        this.version = version;
        this.access = access;
        this.enumClassName = enumClassName;
        this.elementNames = elementNames;
        this.outputStream = new ByteArrayOutputStream();
        this.dataOutput = new DataOutputStream(outputStream);
    }

    /**
     *
     * Build bootstrap class and return them as a byte array.
     * If any error occurs during generation {@code null} will be returned instead.
     *
     * @return Bootstrap class bytes array or {@code null} if any error occurs
     */
    public byte[] generate() {
        // Calculate constant indices.
        int elementCount = elementNames.size();

        int baseConDy = 1;
        int baseNameAndType = baseConDy + elementCount;
        int baseUtf8 = baseNameAndType + elementCount;
        int baseInteger = baseUtf8 + elementCount;
        int indexThisClass = baseInteger + elementCount;
        int indexThisClassUtf8 = indexThisClass + 1;
        int indexSuperClass = indexThisClassUtf8 + 1;
        int indexSuperClassUtf8 = indexSuperClass + 1;
        int indexBootstrapMethodsUtf8 = indexSuperClassUtf8 + 1;
        int indexConDyDescriptorUtf8 = indexBootstrapMethodsUtf8 + 1;
        int indexBootstrapMethodHandle = indexConDyDescriptorUtf8 + 1;
        int indexBootstrapMethodRef = indexBootstrapMethodHandle + 1;
        int indexBootstrapMethodNameAndType = indexBootstrapMethodRef + 1;
        int indexBootstrapMethodName = indexBootstrapMethodNameAndType + 1;
        int indexBootstrapMethodDescriptor = indexBootstrapMethodName + 1;

        int constantPoolSize = indexBootstrapMethodDescriptor + 1;

        if (constantPoolSize > MAX_CONSTANT_POOL_SIZE) {
            // On constant pool overflow return null instead of bootstrap class bytes. Let's caller deal with it!
            return null;
        }

        String binaryEnumClassName = "L" + enumClassName + ";";

        try {
            // Class file header
            u4(CLASS_FILE_SIGNATURE);
            u4(version);

            // Constant pool
            u2(constantPoolSize);

            // N * CONSTANT_Dynamic
            for (int i = 0; i < elementCount; i++) {
                u1u2u2(CONSTANT_Dynamic, i, baseNameAndType + i);
            }

            // N * CONSTANT_NameAndType
            for (int i = 0; i < elementCount; i++) {
                u1u2u2(CONSTANT_NameAndType, baseUtf8 + i, indexConDyDescriptorUtf8);
            }

            // N * CONSTANT_Utf8
            //noinspection ForLoopReplaceableByForEach
            for (int i = 0; i < elementCount; i++) {
                u1(CONSTANT_Utf8);
                utf8(elementNames.get(i));
            }

            // N * CONSTANT_Integer
            for (int i = 0; i < elementCount; i++) {
                u1(CONSTANT_Integer);
                u4(i);
            }

            // ThisClass
            u1(CONSTANT_Class);
            u2(indexThisClassUtf8);

            // ThisClassUtf8
            u1(CONSTANT_Utf8);
            utf8(enumClassName);

            // SuperClass
            u1(CONSTANT_Class);
            u2(indexSuperClassUtf8);

            // SuperClassUtf8
            u1(CONSTANT_Utf8);
            utf8(JAVA_LANG_ENUM);

            // BootstrapMethodsUtf8
            u1(CONSTANT_Utf8);
            utf8(ATTRIBUTE_NAME_BOOTSTRAP_METHODS);

            // ConDyDescriptorUtf8
            u1(CONSTANT_Utf8);
            utf8(binaryEnumClassName);

            // BootstrapMethodHandle
            u1(CONSTANT_MethodHandle);
            u1(REF_newInvokeSpecial);
            u2(indexBootstrapMethodRef);

            // BootstrapMethodRef
            u1u2u2(CONSTANT_Methodref, indexThisClass, indexBootstrapMethodNameAndType);

            // BootstrapMethodNameAndType
            u1u2u2(CONSTANT_NameAndType, indexBootstrapMethodName, indexBootstrapMethodDescriptor);

            // BootstrapMethodName
            u1(CONSTANT_Utf8);
            utf8(BOOTSTRAP_METHOD_NAME);

            // BootstrapMethodDescriptor
            u1(CONSTANT_Utf8);
            utf8(BOOTSTRAP_METHOD_DESCRIPTOR);

            u2(access);
            u2(indexThisClass);
            u2(indexSuperClass);

            // Interfaces count
            u2(0);
            // Fields count
            u2(0);
            // Methods count
            u2(0);
            // Attributes count
            u2(1);

            // BootstrapMethods attribute
            u2(indexBootstrapMethodsUtf8);
            // BootstrapMethods attribute size
            u4(2 /* num_bootstrap_methods */ + 6 * elementCount);
            // Bootstrap method count
            u2(elementCount);

            for (int i = 0; i < elementCount; i++) {
                // bootstrap_method_ref
                u2(indexBootstrapMethodHandle);
                // num_bootstrap_arguments
                u2(1);
                // bootstrap_arguments[1]
                u2(baseInteger + i);
            }
        } catch (IOException e) {
            return null;
        }

        return outputStream.toByteArray();
    }

    private void u1(int value) throws IOException {
        dataOutput.writeByte(value);
    }

    private void u2(int value) throws IOException {
        dataOutput.writeShort(value);
    }

    private void u4(int value) throws IOException {
        dataOutput.writeInt(value);
    }

    private void utf8(String value) throws IOException {
        dataOutput.writeUTF(value);
    }

    private void u1u2u2(int tag, int first, int second) throws IOException {
        u1(tag);
        u2(first);
        u2(second);
    }

}
