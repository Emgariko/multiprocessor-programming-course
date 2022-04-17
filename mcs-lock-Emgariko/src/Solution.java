import java.util.concurrent.atomic.*;

public class Solution implements Lock<Solution.Node> {
    private final Environment env;

    final AtomicReference<Node> tail = new AtomicReference<>(null);

    public Solution(Environment env) {
        this.env = env;
    }

    @Override
    public Node lock() {
        Node my = new Node(); // сделали узел
        my.locked.set(true);
        Node pred = tail.getAndSet(my);
        if (pred != null) {
            pred.next.set(my);
            while (my.locked.get()) { env.park(); }
        }
        return my; // вернули узел
    }

    @Override
    public void unlock(Node my) {
        if (my.next.get() == null) {
            if (tail.compareAndSet(my, null)) {
                return;
            } else {
                while (my.next.get() == null) { }
            }
        }
        my.next.get().locked.set(false);
        env.unpark(my.next.get().thread);
    }

    static class Node {
        final Thread thread = Thread.currentThread(); // запоминаем поток, которые создал узел
        final AtomicReference<Boolean> locked = new AtomicReference<>(false);
        final AtomicReference<Node> next = new AtomicReference<>(null);
    }
}
