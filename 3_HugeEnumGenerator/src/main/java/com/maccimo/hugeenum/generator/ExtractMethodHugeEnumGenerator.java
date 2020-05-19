package com.maccimo.hugeenum.generator;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.Iterator;
import java.util.List;

import static org.objectweb.asm.Opcodes.*;


@SuppressWarnings("SpellCheckingInspection")
public class ExtractMethodHugeEnumGenerator implements IEnumGenerator {

    private static final String CLASS_NAME_JAVA_LANG_ENUM = "java/lang/Enum";

    private static final String MEMBER_NAME_VALUEOF = "valueOf";
    private static final String MEMBER_NAME_CLINIT = "<clinit>";
    private static final String MEMBER_NAME_INIT = "<init>";
    private static final String MEMBER_NAME_CLONE = "clone";
    private static final String MEMBER_NAME_VALUES = "values";
    private static final String MEMBER_NAME_VALUES_ARRAY = "$VALUES";
    private static final String MEMBER_NAME_CREATE_VALUES = "createValues";
    private static final String MEMBER_NAME_VALUE_INDEX = "valueIndex";
    private static final String MEMBER_NAME_NEXT_VALUE = "nextValue";

    private static final String DESCRIPTOR_INT = "I";
    private static final String DESCRIPTOR_NOARG_VOID = "()V";
    private static final String DESCRIPTOR_NOARG_OBJECT = "()Ljava/lang/Object;";
    private static final String DESCRIPTOR_STRING_INT_VOID = "(Ljava/lang/String;I)V";
    private static final String DESCRIPTOR_CLASS_STRING_ENUM = "(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/Enum;";

    private static final int MAX_ITEMS_PER_CREATE_VALUES_PART = 3_854;

    private final String enumClassName;
    private final String binaryEnumClassName;
    private final String binaryEnumArrayClassName;

    private final String descriptorNoargEnum;
    private final String descriptorNoargEnumArray;
    private final String descriptorEnumArrayIntEnumArray;
    private final String descriptorStringEnum;

    private final List<String> elementNames;

    public ExtractMethodHugeEnumGenerator(String enumClassName, List<String> elementNames) {
        this.enumClassName = enumClassName;

        this.binaryEnumClassName = "L" + enumClassName + ";";
        this.binaryEnumArrayClassName = "[" + binaryEnumClassName;

        this.descriptorNoargEnum = "()" + binaryEnumClassName;
        this.descriptorNoargEnumArray = "()" + binaryEnumArrayClassName;
        this.descriptorEnumArrayIntEnumArray = "(" + binaryEnumArrayClassName + DESCRIPTOR_INT + ")" + binaryEnumArrayClassName;
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
        generateNextValue(classWriter);

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

        classWriter
            .visitField(
                ACC_PRIVATE | ACC_STATIC | ACC_SYNTHETIC,
                MEMBER_NAME_VALUE_INDEX,
                DESCRIPTOR_INT,
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
        methodVisitor.visitMethodInsn(INVOKESTATIC, enumClassName, MEMBER_NAME_CREATE_VALUES, descriptorNoargEnumArray, false);
        methodVisitor.visitFieldInsn(PUTSTATIC, enumClassName, MEMBER_NAME_VALUES_ARRAY, binaryEnumArrayClassName);

        methodVisitor.visitInsn(ICONST_0);
        methodVisitor.visitFieldInsn(PUTSTATIC, enumClassName, MEMBER_NAME_VALUE_INDEX, DESCRIPTOR_INT);

        for (String elementName : elementNames) {
            methodVisitor.visitMethodInsn(
                INVOKESTATIC,
                enumClassName,
                MEMBER_NAME_NEXT_VALUE,
                descriptorNoargEnum,
                false
            );
            methodVisitor.visitFieldInsn(
                PUTSTATIC,
                enumClassName,
                elementName,
                binaryEnumClassName
            );
        }

        methodVisitor.visitInsn(RETURN);
        methodVisitor.visitMaxs(1, 0);
        methodVisitor.visitEnd();
    }

    private void generateCreateValuesChain(ClassWriter classWriter, List<String> elementNames) {

        int elementsLeft = elementNames.size();
        Iterator<String> namesIterator = elementNames.iterator();

        int partNumber = 0;

        boolean isFirst = true;
        boolean isLast;

        String nextChainPartName = MEMBER_NAME_CREATE_VALUES;
        String chainPartDescriptor = descriptorNoargEnumArray;

        while (elementsLeft > 0) {
            isLast = (elementsLeft <= MAX_ITEMS_PER_CREATE_VALUES_PART);
            int elementsInPart = isLast ? elementsLeft : MAX_ITEMS_PER_CREATE_VALUES_PART;

            MethodVisitor methodVisitor = classWriter.visitMethod(
                ACC_PRIVATE | ACC_STATIC | ACC_SYNTHETIC,
                nextChainPartName,
                chainPartDescriptor,
                null,
                null
            );

            methodVisitor.visitCode();

            if (isFirst) {
                methodVisitor.visitLdcInsn(elementNames.size());
                methodVisitor.visitTypeInsn(ANEWARRAY, enumClassName);
                methodVisitor.visitVarInsn(ASTORE, 0);
                methodVisitor.visitInsn(ICONST_0);
                methodVisitor.visitVarInsn(ISTORE, 1);
            }

            for (int i = 0; i < elementsInPart; i++) {
                methodVisitor.visitVarInsn(ALOAD, 0);
                methodVisitor.visitVarInsn(ILOAD, 1);
                methodVisitor.visitTypeInsn(NEW, enumClassName);
                methodVisitor.visitInsn(DUP);
                methodVisitor.visitLdcInsn(namesIterator.next());
                methodVisitor.visitVarInsn(ILOAD, 1);
                methodVisitor.visitMethodInsn(
                    INVOKESPECIAL,
                    enumClassName,
                    MEMBER_NAME_INIT,
                    DESCRIPTOR_STRING_INT_VOID,
                    false
                );
                methodVisitor.visitInsn(AASTORE);
                methodVisitor.visitIincInsn(1, 1);
            }

            chainPartDescriptor = descriptorEnumArrayIntEnumArray;
            nextChainPartName = MEMBER_NAME_CREATE_VALUES + partNumber;
            partNumber++;

            //noinspection IfStatementWithIdenticalBranches
            if (isLast) {
                methodVisitor.visitVarInsn(ALOAD, 0);
                methodVisitor.visitInsn(ARETURN);
            } else {
                methodVisitor.visitVarInsn(ALOAD, 0);
                methodVisitor.visitVarInsn(ILOAD, 1);
                methodVisitor.visitMethodInsn(
                    INVOKESTATIC,
                    enumClassName,
                    nextChainPartName,
                    chainPartDescriptor,
                    false
                );
                methodVisitor.visitInsn(ARETURN);
            }

            methodVisitor.visitMaxs(6, 2);
            methodVisitor.visitEnd();

            elementsLeft -= elementsInPart;
            isFirst = false;
        }
    }

    private void generateNextValue(ClassWriter classWriter) {
        MethodVisitor methodVisitor = classWriter.visitMethod(
            ACC_PRIVATE | ACC_STATIC | ACC_SYNTHETIC,
            MEMBER_NAME_NEXT_VALUE,
            descriptorNoargEnum,
            null,
            null
        );

        methodVisitor.visitCode();
        methodVisitor.visitFieldInsn(
            GETSTATIC,
            enumClassName,
            MEMBER_NAME_VALUES_ARRAY,
            binaryEnumArrayClassName
        );
        methodVisitor.visitFieldInsn(
            GETSTATIC,
            enumClassName,
            MEMBER_NAME_VALUE_INDEX,
            DESCRIPTOR_INT
        );
        methodVisitor.visitInsn(DUP);
        methodVisitor.visitInsn(ICONST_1);
        methodVisitor.visitInsn(IADD);
        methodVisitor.visitFieldInsn(
            PUTSTATIC,
            enumClassName,
            MEMBER_NAME_VALUE_INDEX,
            DESCRIPTOR_INT
        );
        methodVisitor.visitInsn(AALOAD);
        methodVisitor.visitInsn(ARETURN);
        methodVisitor.visitMaxs(4, 0);
        methodVisitor.visitEnd();
    }

    //</editor-fold>

}
