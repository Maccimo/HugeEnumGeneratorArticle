package com.maccimo.hugeenum.generator;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassTooLargeException;
import org.objectweb.asm.MethodTooLargeException;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class HugeEnumGeneratorTest {

    private static final String MEMBER_NAME_FORMAT = "VALUE_%05d";

    private static final IEnumGeneratorFactory GENERATOR_FACTORY_EXTRACT_METHOD = new ExtractMethodHugeEnumGeneratorFactory();

    private static final IEnumGeneratorFactory GENERATOR_FACTORY_CONDY = new ConDyHugeEnumGeneratorFactory();

    private static final IEnumGeneratorFactory GENERATOR_FACTORY_UNSAFE = new UnsafeHugeEnumGeneratorFactory();

    public static final String EXTRACT_METHOD_ENUM_NAME = "ExtractMethodHugeEnum";

    public static final String CONDY_ENUM_NAME = "ConDyHugeEnum";

    public static final String UNSAFE_ENUM_NAME = "UnsafeHugeEnum";

    // Maximum for UnsafeEnumGenerator depend on element names length.
    // Number here is pre-calculated for 11-character element name assuming each character occupy
    // exactly 1 byte in UTF-8 encoding. This is true only for (0 < character code < 128)
    private static final int UNSAFE_ENUM_MAX_ELEMENTS_COUNT = 65_410;

    @Test
    public void testExtractMethodGenerator() throws Throwable {
        doTestEnum(
            GENERATOR_FACTORY_EXTRACT_METHOD,
            EXTRACT_METHOD_ENUM_NAME,
            GENERATOR_FACTORY_EXTRACT_METHOD.getDefaultElementCount()
        );
    }

    @Test
    public void testExtractMethodGeneratorOverflow() {
        assertThrows(
            MethodTooLargeException.class,
            () -> doTestEnum(
                GENERATOR_FACTORY_EXTRACT_METHOD,
                EXTRACT_METHOD_ENUM_NAME,
                GENERATOR_FACTORY_EXTRACT_METHOD.getMaximumElementCount() + 1
            )
        );
    }

    @Test
    public void testConDyGenerator() throws Throwable {
        doTestEnum(
            GENERATOR_FACTORY_CONDY,
            CONDY_ENUM_NAME,
            GENERATOR_FACTORY_CONDY.getDefaultElementCount()
        );
    }

    @Test
    public void testConDyGeneratorOverflow() {
        assertThrows(
            MethodTooLargeException.class,
            () -> doTestEnum(
                GENERATOR_FACTORY_CONDY,
                CONDY_ENUM_NAME,
                GENERATOR_FACTORY_CONDY.getMaximumElementCount() + 1
            )
        );
    }

    @Test
    public void testUnsafeGenerator() throws Throwable {
        doTestEnum(
            GENERATOR_FACTORY_UNSAFE,
            UNSAFE_ENUM_NAME,
            UNSAFE_ENUM_MAX_ELEMENTS_COUNT
        );
    }

    @Test
    public void testUnsafeGeneratorOverflow() {
        assertThrows(
            ClassTooLargeException.class,
            () -> doTestEnum(
                GENERATOR_FACTORY_UNSAFE,
                UNSAFE_ENUM_NAME,
                UNSAFE_ENUM_MAX_ELEMENTS_COUNT + 1
            )
        );
    }

    private void doTestEnum(IEnumGeneratorFactory factory, String enumName, int elementCount) throws Throwable {

        List<String> elementNames = generateElementNames(elementCount);

        IEnumGenerator enumGenerator = factory.create(enumName, elementNames);

        byte[] classBytes = enumGenerator.generate();

        BytesClassLoader classLoader = new BytesClassLoader();

        Class<?> enumClass = classLoader.defineClass(enumName, classBytes);

        assertNotNull(enumClass);

        assertTrue(enumClass.isEnum(), "Generated class is not an enum");

        doTestFields(enumClass, elementNames);

        doTestValues(enumClass, elementNames);
    }

    private static List<String> generateElementNames(int count) {
        return IntStream
            .range(1, count + 1)
            .mapToObj(number -> String.format(MEMBER_NAME_FORMAT, number))
            .collect(Collectors.toList());
    }

    private void doTestFields(Class<?> enumClass, List<String> elementNames) throws Exception {

        Field[] declaredFields = enumClass.getDeclaredFields();

        Map<String, Field> fieldMap = Stream
            .of(declaredFields)
            .collect(Collectors.toMap(Field::getName, Function.identity()));

        for (int i = 0; i < elementNames.size(); i++) {
            int valueIndex = i;
            String elementName = elementNames.get(valueIndex);
            Field declaredField = fieldMap.get(elementName);
            Enum<?> enumItem = (Enum<?>) declaredField.get(null);

            assertEquals(elementName, enumItem.name(), () -> String.format("Element name mismatch at index %d", valueIndex));

            assertEquals(valueIndex, enumItem.ordinal(), () -> String.format("Element ordinal mismatch at index %d", valueIndex));
        }
    }

    private void doTestValues(Class<?> enumClass, List<String> elementNames) throws Exception {

        Method valuesMethod = enumClass.getDeclaredMethod("values");

        Enum<?>[] values = (Enum<?>[])valuesMethod.invoke(null);

        assertEquals(elementNames.size(), values.length, "Invalid values() result length");

        for (int i = 0; i < elementNames.size(); i++) {
            int valueIndex = i;
            Enum<?> value = values[valueIndex];
            String elementName = elementNames.get(valueIndex);

            assertTrue(enumClass.isInstance(value), () -> String.format("values() item at index %d has incompatible type %s", valueIndex, value.getClass().getCanonicalName()));

            assertEquals(elementName, value.name(), () -> String.format("Element name mismatch at index %d", valueIndex));

            assertEquals(valueIndex, value.ordinal(), () -> String.format("Element ordinal mismatch at index %d", valueIndex));
        }
    }

}