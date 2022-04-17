/**
 * @author Garipov Emil
 */
public class Solution implements AtomicCounter {
    // объявите здесь нужные вам поля
    private final Node root = new Node(0);
    final ThreadLocal<Node> last = ThreadLocal.withInitial(() -> root);

    public int getAndAdd(int x) {
        int old;
        Node node;
        do {
            old = last.get().val;
            int res = old + x;
            node = new Node(res);
            last.set(last.get().next.decide(node));
        } while (last.get() != node);
        return old;
    }

    // вам наверняка потребуется дополнительный класс
    private static class Node {
        final int val;
        final Consensus<Node> next = new Consensus<>();

        public Node(int val) {
            this.val = val;
        }
    }
}
