package com.maccimo.hugeenum.generator;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.objectweb.asm.Opcodes.*;


@SuppressWarnings("SpellCheckingInspection")
public class UnsafeHugeEnumGenerator implements IEnumGenerator {

    private static final String CLASS_NAME_JAVA_LANG_ENUM = "java/lang/Enum";

    private static final String CLASS_NAME_SUN_MISC_UNSAFE = "sun/misc/Unsafe";
    private static final String CLASS_NAME_JAVA_LANG_CLASS = "java/lang/Class";
    private static final String CLASS_NAME_JAVA_LANG_STRING_BUILDER = "java/lang/StringBuilder";
    private static final String CLASS_NAME_JAVA_UTIL_HASH_MAP = "java/util/HashMap";
    private static final String CLASS_NAME_JAVA_LANG_REFLECT_FIELD = "java/lang/reflect/Field";
    private static final String CLASS_NAME_STRING = "java/lang/String";

    private static final String BINARY_CLASS_NAME_SUN_MISC_UNSAFE = "Lsun/misc/Unsafe;";
    private static final String BINARY_CLASS_NAME_STRING_ARRAY = "[Ljava/lang/String;";
    private static final String BINARY_CLASS_NAME_FIELD_ARRAY = "[Ljava/lang/reflect/Field;";

    private static final String MEMBER_NAME_VALUEOF = "valueOf";
    private static final String MEMBER_NAME_CLINIT = "<clinit>";
    private static final String MEMBER_NAME_INIT = "<init>";
    private static final String MEMBER_NAME_CLONE = "clone";
    private static final String MEMBER_NAME_VALUES = "values";
    private static final String MEMBER_NAME_VALUES_ARRAY = "$VALUES";

    private static final String MEMBER_NAME_THE_UNSAFE = "theUnsafe";
    private static final String MEMBER_NAME_GET_DECLARED_FIELD = "getDeclaredField";
    private static final String MEMBER_NAME_GET_DECLARED_FIELDS = "getDeclaredFields";
    private static final String MEMBER_NAME_SET_ACCESSIBLE = "setAccessible";
    private static final String MEMBER_NAME_GET = "get";
    private static final String MEMBER_NAME_PUT = "put";
    private static final String MEMBER_NAME_APPEND = "append";
    private static final String MEMBER_NAME_TO_STRING = "toString";
    private static final String MEMBER_NAME_SPLIT = "split";
    private static final String MEMBER_NAME_STATIC_FIELD_OFFSET = "staticFieldOffset";
    private static final String MEMBER_NAME_PUT_OBJECT = "putObject";
    private static final String MEMBER_NAME_GET_NAME = "getName";

    private static final String DESCRIPTOR_NOARG_VOID = "()V";
    private static final String DESCRIPTOR_NOARG_OBJECT = "()Ljava/lang/Object;";
    private static final String DESCRIPTOR_STRING_INT_VOID = "(Ljava/lang/String;I)V";
    private static final String DESCRIPTOR_CLASS_STRING_ENUM = "(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/Enum;";

    private static final String DESCRIPTOR_BOOLEAN_VOID = "(Z)V";
    private static final String DESCRIPTOR_OBJECT_OBJECT = "(Ljava/lang/Object;)Ljava/lang/Object;";
    private static final String DESCRIPTOR_STRING_VOID = "(Ljava/lang/String;)V";
    private static final String DESCRIPTOR_STRING_FIELD = "(Ljava/lang/String;)Ljava/lang/reflect/Field;";
    private static final String DESCRIPTOR_STRING_STRING_BUILDER = "(Ljava/lang/String;)Ljava/lang/StringBuilder;";
    private static final String DESCRIPTOR_NOARG_STRING = "()Ljava/lang/String;";
    private static final String DESCRIPTOR_STRING_STRING_ARRAY = "(Ljava/lang/String;)[Ljava/lang/String;";
    private static final String DESCRIPTOR_FIELD_LONG = "(Ljava/lang/reflect/Field;)J";
    private static final String DESCRIPTOR_OBJECT_LONG_OBJECT_VOID = "(Ljava/lang/Object;JLjava/lang/Object;)V";
    private static final String DESCRIPTOR_OBJECT_OBJECT_OBJECT = "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;";
    private static final String DESCRIPTOR_NOARG_FIELD_ARRAY = "()[Ljava/lang/reflect/Field;";
    private static final String DESCRIPTOR_INT_VOID = "(I)V";

    // Maximum Utf8 constant pool entry size in bytes.
    private static final int MAX_STRING_SIZE = 65_535;

    private static final String FIELD_NAME_SEPARATOR = ",";

    private final String enumClassName;
    private final String binaryEnumClassName;
    private final String binaryEnumArrayClassName;

    private final String descriptorNoargEnumArray;
    private final String descriptorStringEnum;

    private final List<String> elementNames;


    public UnsafeHugeEnumGenerator(String enumClassName, List<String> elementNames) {
        this.enumClassName = enumClassName;

        this.binaryEnumClassName = "L" + enumClassName + ";";
        this.binaryEnumArrayClassName = "[" + binaryEnumClassName;

        this.descriptorNoargEnumArray = "()" + binaryEnumArrayClassName;
        this.descriptorStringEnum = "(Ljava/lang/String;)" + binaryEnumClassName;

        this.elementNames = elementNames;
    }

    public byte[] generate() {

        ClassWriter classWriter = new ClassWriter(0);

        classWriter.visit(
            Opcodes.V1_7,
            ACC_PUBLIC | ACC_FINAL | ACC_SUPER | ACC_ENUM,
            enumClassName,
            null,
            CLASS_NAME_JAVA_LANG_ENUM,
            null
        );

        generateCommonMembers(classWriter);
        generateElementMembers(classWriter, elementNames);
        generateValues(classWriter);
        generateValueOf(classWriter);
        generateConstructor(classWriter);
        generateStaticInitializer(classWriter, elementNames);
        generateCreateValuesChain(classWriter, elementNames);

        classWriter.visitEnd();

        return classWriter.toByteArray();
    }

    //<editor-fold desc="Bytecode generation">

    private void generateCommonMembers(ClassWriter classWriter) {
        classWriter
            .visitField(
                ACC_PRIVATE | ACC_STATIC | ACC_FINAL | ACC_SYNTHETIC,
                MEMBER_NAME_VALUES_ARRAY,
                binaryEnumArrayClassName,
                null,
                null
            )
            .visitEnd();
    }

    private void generateElementMembers(ClassWriter classWriter, List<String> elementNames) {
        for (String elementName : elementNames) {
            classWriter
                .visitField(
                    ACC_PUBLIC | ACC_STATIC | ACC_FINAL | ACC_ENUM,
                    elementName,
                    binaryEnumClassName,
                    null,
                    null
                )
                .visitEnd();
        }
    }

    private void generateValues(ClassWriter classWriter) {
        MethodVisitor methodVisitor = classWriter.visitMethod(
            ACC_PUBLIC | ACC_STATIC,
            MEMBER_NAME_VALUES,
            descriptorNoargEnumArray,
            null,
            null
        );

        methodVisitor.visitCode();
        methodVisitor.visitFieldInsn(GETSTATIC, enumClassName, MEMBER_NAME_VALUES_ARRAY, binaryEnumArrayClassName);
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, binaryEnumArrayClassName, MEMBER_NAME_CLONE, DESCRIPTOR_NOARG_OBJECT, false);
        methodVisitor.visitTypeInsn(CHECKCAST, binaryEnumArrayClassName);
        methodVisitor.visitInsn(ARETURN);
        methodVisitor.visitMaxs(1, 0);
        methodVisitor.visitEnd();
    }

    private void generateValueOf(ClassWriter classWriter) {
        MethodVisitor methodVisitor = classWriter.visitMethod(
            ACC_PUBLIC | ACC_STATIC,
            MEMBER_NAME_VALUEOF,
            descriptorStringEnum,
            null,
            null
        );

        methodVisitor.visitCode();
        methodVisitor.visitLdcInsn(Type.getType(binaryEnumClassName));
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitMethodInsn(INVOKESTATIC, CLASS_NAME_JAVA_LANG_ENUM, MEMBER_NAME_VALUEOF, DESCRIPTOR_CLASS_STRING_ENUM, false);
        methodVisitor.visitTypeInsn(CHECKCAST, enumClassName);
        methodVisitor.visitInsn(ARETURN);
        methodVisitor.visitMaxs(2, 1);
        methodVisitor.visitEnd();
    }

    private void generateConstructor(ClassWriter classWriter) {
        MethodVisitor methodVisitor = classWriter.visitMethod(
            ACC_PRIVATE,
            MEMBER_NAME_INIT,
            DESCRIPTOR_STRING_INT_VOID,
            null,
            null
        );

        methodVisitor.visitCode();
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitVarInsn(ALOAD, 1);
        methodVisitor.visitVarInsn(ILOAD, 2);
        methodVisitor.visitMethodInsn(INVOKESPECIAL, CLASS_NAME_JAVA_LANG_ENUM, MEMBER_NAME_INIT, DESCRIPTOR_STRING_INT_VOID, false);
        methodVisitor.visitInsn(RETURN);
        methodVisitor.visitMaxs(3, 3);
        methodVisitor.visitEnd();
    }

    private void generateStaticInitializer(ClassWriter classWriter, List<String> elementNames) {
        MethodVisitor methodVisitor = classWriter.visitMethod(
            ACC_STATIC,
            MEMBER_NAME_CLINIT,
            DESCRIPTOR_NOARG_VOID,
            null,
            null
        );

        methodVisitor.visitCode();

        if (elementNames.size() == 0) {
            methodVisitor.visitInsn(ICONST_0);
            methodVisitor.visitTypeInsn(ANEWARRAY, enumClassName);
            methodVisitor.visitFieldInsn(PUTSTATIC, enumClassName, MEMBER_NAME_VALUES_ARRAY, binaryEnumArrayClassName);
            methodVisitor.visitInsn(RETURN);
            methodVisitor.visitMaxs(1, 0);
        } else {
            methodVisitor.visitLdcInsn(Type.getType(BINARY_CLASS_NAME_SUN_MISC_UNSAFE));
            methodVisitor.visitLdcInsn(MEMBER_NAME_THE_UNSAFE);
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, CLASS_NAME_JAVA_LANG_CLASS, MEMBER_NAME_GET_DECLARED_FIELD, DESCRIPTOR_STRING_FIELD, false);
            methodVisitor.visitVarInsn(ASTORE, 0);

            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitInsn(ICONST_1);
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, CLASS_NAME_JAVA_LANG_REFLECT_FIELD, MEMBER_NAME_SET_ACCESSIBLE, DESCRIPTOR_BOOLEAN_VOID, false);
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitInsn(ACONST_NULL);
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, CLASS_NAME_JAVA_LANG_REFLECT_FIELD, MEMBER_NAME_GET, DESCRIPTOR_OBJECT_OBJECT, false);
            methodVisitor.visitTypeInsn(CHECKCAST, CLASS_NAME_SUN_MISC_UNSAFE);
            methodVisitor.visitVarInsn(ASTORE, 1);

            List<String> packedNames = packElementNames(elementNames);

            if (packedNames.size() == 1) {
                methodVisitor.visitLdcInsn(packedNames.get(0));
            } else {
                // Length of all enum element names joined into string may exceed maximum length of one string in constant pool
                generateConcatStrings(methodVisitor, packedNames);
            }

            methodVisitor.visitLdcInsn(FIELD_NAME_SEPARATOR);
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, CLASS_NAME_STRING, MEMBER_NAME_SPLIT, DESCRIPTOR_STRING_STRING_ARRAY, false);
            methodVisitor.visitInsn(DUP);
            methodVisitor.visitVarInsn(ASTORE, 2);

            methodVisitor.visitInsn(ARRAYLENGTH);
            methodVisitor.visitTypeInsn(ANEWARRAY, enumClassName);
            methodVisitor.visitVarInsn(ASTORE, 3);

            methodVisitor.visitLdcInsn(Type.getType(binaryEnumClassName));
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, CLASS_NAME_JAVA_LANG_CLASS, MEMBER_NAME_GET_DECLARED_FIELDS, DESCRIPTOR_NOARG_FIELD_ARRAY, false);
            methodVisitor.visitVarInsn(ASTORE, 4);

            methodVisitor.visitTypeInsn(NEW, CLASS_NAME_JAVA_UTIL_HASH_MAP);
            methodVisitor.visitInsn(DUP);
            methodVisitor.visitVarInsn(ALOAD, 4);
            methodVisitor.visitInsn(ARRAYLENGTH);
            methodVisitor.visitMethodInsn(INVOKESPECIAL, CLASS_NAME_JAVA_UTIL_HASH_MAP, MEMBER_NAME_INIT, DESCRIPTOR_INT_VOID, false);
            methodVisitor.visitVarInsn(ASTORE, 5);

            methodVisitor.visitVarInsn(ALOAD, 4);
            methodVisitor.visitVarInsn(ASTORE, 6);

            methodVisitor.visitVarInsn(ALOAD, 6);
            methodVisitor.visitInsn(ARRAYLENGTH);
            methodVisitor.visitVarInsn(ISTORE, 7);

            methodVisitor.visitInsn(ICONST_0);
            methodVisitor.visitVarInsn(ISTORE, 8);

            Label labelCacheInitLoopBegin = new Label();
            methodVisitor.visitLabel(labelCacheInitLoopBegin);
            methodVisitor.visitFrame(Opcodes.F_FULL, 9, new Object[] {CLASS_NAME_JAVA_LANG_REFLECT_FIELD, CLASS_NAME_SUN_MISC_UNSAFE, BINARY_CLASS_NAME_STRING_ARRAY, binaryEnumArrayClassName, BINARY_CLASS_NAME_FIELD_ARRAY, CLASS_NAME_JAVA_UTIL_HASH_MAP, BINARY_CLASS_NAME_FIELD_ARRAY, Opcodes.INTEGER, Opcodes.INTEGER}, 0, new Object[] {});
            methodVisitor.visitVarInsn(ILOAD, 8);
            methodVisitor.visitVarInsn(ILOAD, 7);

            Label labelCacheInitLoopEnd = new Label();
            methodVisitor.visitJumpInsn(IF_ICMPGE, labelCacheInitLoopEnd);
            methodVisitor.visitVarInsn(ALOAD, 6);
            methodVisitor.visitVarInsn(ILOAD, 8);
            methodVisitor.visitInsn(AALOAD);
            methodVisitor.visitVarInsn(ASTORE, 9);

            methodVisitor.visitVarInsn(ALOAD, 5);
            methodVisitor.visitVarInsn(ALOAD, 9);
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, CLASS_NAME_JAVA_LANG_REFLECT_FIELD, MEMBER_NAME_GET_NAME, DESCRIPTOR_NOARG_STRING, false);
            methodVisitor.visitVarInsn(ALOAD, 9);
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, CLASS_NAME_JAVA_UTIL_HASH_MAP, MEMBER_NAME_PUT, DESCRIPTOR_OBJECT_OBJECT_OBJECT, false);
            methodVisitor.visitInsn(POP);
            methodVisitor.visitIincInsn(8, 1);
            methodVisitor.visitJumpInsn(GOTO, labelCacheInitLoopBegin);

            methodVisitor.visitLabel(labelCacheInitLoopEnd);
            methodVisitor.visitFrame(Opcodes.F_CHOP,3, null, 0, null);
            methodVisitor.visitInsn(ICONST_0);
            methodVisitor.visitVarInsn(ISTORE, 6);

            Label labelElementInitLoopBegin = new Label();
            methodVisitor.visitLabel(labelElementInitLoopBegin);
            methodVisitor.visitFrame(Opcodes.F_APPEND,1, new Object[] {Opcodes.INTEGER}, 0, null);
            methodVisitor.visitVarInsn(ILOAD, 6);
            methodVisitor.visitVarInsn(ALOAD, 2);
            methodVisitor.visitInsn(ARRAYLENGTH);

            Label labelElementInitLoopEnd = new Label();
            methodVisitor.visitJumpInsn(IF_ICMPGE, labelElementInitLoopEnd);
            methodVisitor.visitVarInsn(ALOAD, 2);
            methodVisitor.visitVarInsn(ILOAD, 6);
            methodVisitor.visitInsn(AALOAD);
            methodVisitor.visitVarInsn(ASTORE, 7);

            methodVisitor.visitVarInsn(ALOAD, 5);
            methodVisitor.visitVarInsn(ALOAD, 7);
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, CLASS_NAME_JAVA_UTIL_HASH_MAP, MEMBER_NAME_GET, DESCRIPTOR_OBJECT_OBJECT, false);
            methodVisitor.visitTypeInsn(CHECKCAST, CLASS_NAME_JAVA_LANG_REFLECT_FIELD);
            methodVisitor.visitVarInsn(ASTORE, 8);

            methodVisitor.visitVarInsn(ALOAD, 1);
            methodVisitor.visitVarInsn(ALOAD, 8);
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, CLASS_NAME_SUN_MISC_UNSAFE, MEMBER_NAME_STATIC_FIELD_OFFSET, DESCRIPTOR_FIELD_LONG, false);
            methodVisitor.visitVarInsn(LSTORE, 9);

            methodVisitor.visitVarInsn(ALOAD, 1);
            methodVisitor.visitLdcInsn(Type.getType(binaryEnumClassName));
            methodVisitor.visitVarInsn(LLOAD, 9);
            methodVisitor.visitVarInsn(ALOAD, 3);
            methodVisitor.visitVarInsn(ILOAD, 6);
            methodVisitor.visitTypeInsn(NEW, enumClassName);
            methodVisitor.visitInsn(DUP);
            methodVisitor.visitVarInsn(ALOAD, 7);
            methodVisitor.visitVarInsn(ILOAD, 6);
            methodVisitor.visitMethodInsn(INVOKESPECIAL, enumClassName, MEMBER_NAME_INIT, DESCRIPTOR_STRING_INT_VOID, false);
            methodVisitor.visitInsn(DUP_X2);
            methodVisitor.visitInsn(AASTORE);

            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, CLASS_NAME_SUN_MISC_UNSAFE, MEMBER_NAME_PUT_OBJECT, DESCRIPTOR_OBJECT_LONG_OBJECT_VOID, false);
            methodVisitor.visitIincInsn(6, 1);
            methodVisitor.visitJumpInsn(GOTO, labelElementInitLoopBegin);

            methodVisitor.visitLabel(labelElementInitLoopEnd);
            methodVisitor.visitFrame(Opcodes.F_CHOP,1, null, 0, null);

            methodVisitor.visitVarInsn(ALOAD, 3);
            methodVisitor.visitFieldInsn(PUTSTATIC, enumClassName, MEMBER_NAME_VALUES_ARRAY, binaryEnumArrayClassName);

            methodVisitor.visitInsn(RETURN);
            methodVisitor.visitMaxs(10, 11);
        }

        methodVisitor.visitEnd();
    }

    private void generateConcatStrings(MethodVisitor methodVisitor, List<String> packedNames) {
        methodVisitor.visitTypeInsn(NEW, CLASS_NAME_JAVA_LANG_STRING_BUILDER);
        methodVisitor.visitInsn(DUP);
        methodVisitor.visitLdcInsn(packedNames.get(0));
        methodVisitor.visitMethodInsn(INVOKESPECIAL, CLASS_NAME_JAVA_LANG_STRING_BUILDER, MEMBER_NAME_INIT, DESCRIPTOR_STRING_VOID, false);

        for (int i = 1; i < packedNames.size(); i++) {
            methodVisitor.visitLdcInsn(packedNames.get(i));
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, CLASS_NAME_JAVA_LANG_STRING_BUILDER, MEMBER_NAME_APPEND, DESCRIPTOR_STRING_STRING_BUILDER, false);
        }

        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, CLASS_NAME_JAVA_LANG_STRING_BUILDER, MEMBER_NAME_TO_STRING, DESCRIPTOR_NOARG_STRING, false);
    }

    private List<String> packElementNames(List<String> elementNames) {

        List<String> result = new ArrayList<>();

        List<StringIntPair> codepointInfoList = String.join(FIELD_NAME_SEPARATOR, elementNames)
            .codePoints()
            .mapToObj(Character::toString)
            .map(item -> new StringIntPair(item, item.getBytes(StandardCharsets.UTF_8).length))
            .collect(Collectors.toList());

        int currentSize = 0;
        StringBuilder builder = new StringBuilder();

        for (StringIntPair codepointInfo : codepointInfoList) {
            int appendSize = codepointInfo.getInteger();

            if (currentSize + appendSize > MAX_STRING_SIZE) {
                result.add(builder.toString());
                builder = new StringBuilder();
                currentSize = appendSize;
            } else {
                currentSize += appendSize;
            }

            builder.append(codepointInfo.getString());
        }

        if (builder.length() != 0) {
            result.add(builder.toString());
        }

        return result;
    }

    private void generateCreateValuesChain(ClassWriter classWriter, List<String> elementNames) {

        // Empty

    }

    private static class StringIntPair {

        private final String string;
        private final int integer;

        public StringIntPair(String string, int integer) {
            this.string = string;
            this.integer = integer;
        }


        public String getString() {
            return string;
        }

        public int getInteger() {
            return integer;
        }

    }

    //</editor-fold>

}
