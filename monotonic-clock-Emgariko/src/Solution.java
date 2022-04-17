import org.jetbrains.annotations.NotNull;

/**
 * В теле класса решения разрешено использовать только финальные переменные типа RegularInt.
 * Нельзя volatile, нельзя другие типы, нельзя блокировки, нельзя лазить в глобальные переменные.
 *
 * @author Garipov Emil
 */
public class Solution implements MonotonicClock {
    private final RegularInt l1 = new RegularInt(0);
    private final RegularInt m1 = new RegularInt(0);
    private final RegularInt l2 = new RegularInt(0);
    private final RegularInt m2 = new RegularInt(0);
    private final RegularInt r = new RegularInt(0);

    @Override
    public void write(@NotNull Time time) {
        int d1 = time.getD1();
        int d2 = time.getD2();
        int d3 = time.getD3();
        l2.setValue(d1);
        m2.setValue(d2);
        r.setValue(d3);
        m1.setValue(d2);
        l1.setValue(d1);
    }

    @NotNull
    @Override
    public Time read() {
        int v1 = l1.getValue();
        int v2 = m1.getValue();
        int v3 = r.getValue();
        int v4 = m2.getValue();
        int v5 = l2.getValue();
        if (v1 == v5) {
            if (v2 == v4) {
                return new Time(v1, v2, v3);
            } else {
                return new Time(v5, v4, 0);
            }
        } else {
            return new Time(v5, 0, 0);
        }
    }
}
