public class TestFew {

    public static void main(String... args) {
        for(String arg : args) {
            System.out.print(arg + " : ");

            try {
                UnsafeHugeEnum value = UnsafeHugeEnum.valueOf(arg);

                doSwitch(value);
            } catch(Throwable e) {
                e.printStackTrace();
            }
        }
    }

    private static void doSwitch(UnsafeHugeEnum value) {
        switch(value) {
            case VALUE_00001:
                System.out.println("First");
                break;
            case VALUE_31415:
                System.out.println("(int) (10_000 * Math.PI)");
                break;
            case VALUE_65410:
                System.out.println("Last");
                break;
            default:
                System.out.println("Unexpected value: " + value);
                break;
        }    
    }

}
