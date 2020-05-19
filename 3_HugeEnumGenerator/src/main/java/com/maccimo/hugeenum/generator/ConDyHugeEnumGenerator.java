package com.maccimo.hugeenum.generator;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.List;

import static org.objectweb.asm.Opcodes.*;

@SuppressWarnings("SpellCheckingInspection")
public class ConDyHugeEnumGenerator implements IEnumGenerator {

    private static final String CLASS_NAME_JAVA_LANG_ENUM = "java/lang/Enum";
    private static final String CLASS_NAME_JAVA_LANG_INVOKE_METHOD_HANDLES = "java/lang/invoke/MethodHandles";
    private static final String CLASS_NAME_JAVA_LANG_INVOKE_METHOD_HANDLES_LOOKUP = "java/lang/invoke/MethodHandles$Lookup";
    public static final String CLASS_NAME_LOOKUP = "Lookup";

    private static final String MEMBER_NAME_VALUEOF = "valueOf";
    private static final String MEMBER_NAME_CLINIT = "<clinit>";
    private static final String MEMBER_NAME_INIT = "<init>";
    private static final String MEMBER_NAME_CLONE = "clone";
    private static final String MEMBER_NAME_VALUES = "values";
    private static final String MEMBER_NAME_VALUES_ARRAY = "$VALUES";
    private static final String MEMBER_NAME_CREATE_VALUES = "createValues";

    private static final String DESCRIPTOR_NOARG_VOID = "()V";
    private static final String DESCRIPTOR_NOARG_OBJECT = "()Ljava/lang/Object;";
    private static final String DESCRIPTOR_STRING_INT_VOID = "(Ljava/lang/String;I)V";
    private static final String DESCRIPTOR_CLASS_STRING_ENUM = "(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/Enum;";
    private static final String DESCRIPTOR_METHODHANDLES_LOOKUP_STRING_CLASS_INT_VOID = "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Class;I)V";

    private final String enumClassName;
    private final String binaryEnumClassName;
    private final String binaryEnumArrayClassName;

    private final String descriptorNoargEnumArray;
    private final String descriptorStringEnum;
    private final String descriptorMethodhandlesLookupStringClassEnumArrayEnumArray;

    private final List<String> elementNames;


    public ConDyHugeEnumGenerator(String enumClassName, List<String> elementNames) {
        this.enumClassName = enumClassName;

        this.binaryEnumClassName = "L" + enumClassName + ";";
        this.binaryEnumArrayClassName = "[" + binaryEnumClassName;

        this.descriptorNoargEnumArray = "()" + binaryEnumArrayClassName;
        this.descriptorStringEnum = "(Ljava/lang/String;)" + binaryEnumClassName;
        this.descriptorMethodhandlesLookupStringClassEnumArrayEnumArray =
            "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Class;" + binaryEnumArrayClassName + ")" + binaryEnumArrayClassName;

        this.elementNames = elementNames;
    }

    public byte[] generate() {

        ClassWriter classWriter = new ClassWriter(
            getBootstrapClassReader(
                Opcodes.V11,
                ACC_PUBLIC | ACC_FINAL | ACC_SUPER | ACC_ENUM,
                enumClassName,
                elementNames
            ),
            0
        );

        classWriter.visit(
            Opcodes.V11,
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
        generateCreateValues(classWriter);
        generateInnerClassAttribute(classWriter);

        classWriter.visitEnd();

        return classWriter.toByteArray();
    }

    //<editor-fold desc="Bytecode generation">

    @SuppressWarnings("SameParameterValue")
    private ClassReader getBootstrapClassReader(int version, int access, String enumClassName, List<String> elementNames) {
        byte[] bootstrapClassBytes = new ConDyBootstrapClassGenerator(
            version,
            access,
            enumClassName,
            elementNames
        )
        .generate();

        if (bootstrapClassBytes == null) {
            return null;
        } else {
            return new ClassReader(bootstrapClassBytes);
        }
    }

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
            DESCRIPTOR_METHODHANDLES_LOOKUP_STRING_CLASS_INT_VOID,
            null,
            null
        );

        methodVisitor.visitCode();
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitVarInsn(ALOAD, 2);
        methodVisitor.visitVarInsn(ILOAD, 4);
        methodVisitor.visitMethodInsn(INVOKESPECIAL, CLASS_NAME_JAVA_LANG_ENUM, MEMBER_NAME_INIT, DESCRIPTOR_STRING_INT_VOID, false);
        methodVisitor.visitInsn(RETURN);
        methodVisitor.visitMaxs(3, 5);
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

        ConstantDynamic[] elementConDys = new ConstantDynamic[elementNames.size()];
        for (int i = 0; i < elementNames.size(); i++) {
            String elementName = elementNames.get(i);
            elementConDys[i] = new ConstantDynamic(
                elementName,
                binaryEnumClassName,
                new Handle(
                    H_NEWINVOKESPECIAL,
                    enumClassName,
                    MEMBER_NAME_INIT,
                    DESCRIPTOR_METHODHANDLES_LOOKUP_STRING_CLASS_INT_VOID,
                    false
                ),
                i
            );
        }

        for (ConstantDynamic constantDynamic : elementConDys) {
            methodVisitor.visitLdcInsn(constantDynamic);

            methodVisitor.visitFieldInsn(
                PUTSTATIC,
                enumClassName,
                constantDynamic.getName(),
                binaryEnumClassName
            );
        }

        //noinspection RedundantCast
        methodVisitor.visitLdcInsn(
            new ConstantDynamic(
                MEMBER_NAME_CREATE_VALUES,
                binaryEnumArrayClassName,
                new Handle(
                    H_INVOKESTATIC,
                    enumClassName,
                    MEMBER_NAME_CREATE_VALUES,
                    descriptorMethodhandlesLookupStringClassEnumArrayEnumArray,
                    false
                ),
                (Object[]) elementConDys
            )
        );

        methodVisitor.visitFieldInsn(PUTSTATIC, enumClassName, MEMBER_NAME_VALUES_ARRAY, binaryEnumArrayClassName);

        methodVisitor.visitInsn(RETURN);
        methodVisitor.visitMaxs(1, 0);
        methodVisitor.visitEnd();
    }

    private void generateCreateValues(ClassWriter classWriter) {
        MethodVisitor methodVisitor = classWriter.visitMethod(
            ACC_PRIVATE | ACC_STATIC | ACC_VARARGS | ACC_SYNTHETIC,
            MEMBER_NAME_CREATE_VALUES,
            descriptorMethodhandlesLookupStringClassEnumArrayEnumArray,
            null,
            null
        );

        methodVisitor.visitCode();

        methodVisitor.visitVarInsn(ALOAD, 3);
        methodVisitor.visitInsn(ARETURN);

        methodVisitor.visitMaxs(1, 4);
        methodVisitor.visitEnd();
    }

    private void generateInnerClassAttribute(ClassWriter classWriter) {
        classWriter.visitInnerClass(
            CLASS_NAME_JAVA_LANG_INVOKE_METHOD_HANDLES_LOOKUP,
            CLASS_NAME_JAVA_LANG_INVOKE_METHOD_HANDLES,
            CLASS_NAME_LOOKUP,
            ACC_PUBLIC | ACC_FINAL | ACC_STATIC
        );
    }

    //</editor-fold>

}
