package linked_list_set;

import kotlinx.atomicfu.AtomicRef;

public class SetImpl implements Set {
    private interface NodeInterface {}

    private static class Removed implements NodeInterface {
        Node node;
        Removed(Node node) {
            this.node = node;
        }
    }

    private static class Node implements NodeInterface {
        AtomicRef<NodeInterface> next;
        int x;

        Node(int x, NodeInterface next) {
            this.next = new AtomicRef<>(next);
            this.x = x;
        }
    }

    private static class Window {
        Node cur, next;
    }

    private final Node head = new Node(Integer.MIN_VALUE, new Node(Integer.MAX_VALUE, null));

    /**
     * Returns the {@link Window}, where cur.x < x <= next.x
     */
    private Window findWindow(int x) {
        retry:
        while (true) {
            Node cur = head, next = (Node) cur.next.getValue();
            while (next.x < x) {
                NodeInterface node = next.next.getValue();
                if (node instanceof Removed) {
                    Removed node1 = (Removed) node;
                    if (!cur.next.compareAndSet(next, node1.node)) {
                        continue retry;
                    }
                    next = node1.node;
                } else {
                    cur = next;
                    next = (Node) node;
                }
            }
            NodeInterface node = next.next.getValue();
            if (node instanceof Removed) {
                Removed node1 = (Removed) node;
                cur.next.compareAndSet(next, node1.node);
            }
            else {
                Window w = new Window();
                w.cur = cur;
                w.next = next;
                return w;
            }
        }
    }

    @Override
    public boolean add(int x) {
        while (true) {
            Window w = findWindow(x);
            if (w.next.x == x) {
                return false;
            }
            Node node = new Node(x, w.next);
            if (w.cur.next.compareAndSet(w.next, node)) {
                return true;
            }
        }
    }

    @Override
    public boolean remove(int x) {
        while (true) {
            Window w = findWindow(x);
            if (w.next.x != x) {
                return false;
            }
            NodeInterface node = w.next.next.getValue();
            if (node instanceof Removed) {
                Removed node1 = (Removed) node;
                w.cur.next.compareAndSet(w.next, node1.node);
            } else {
                Removed removed = new Removed((Node) node);
                if (w.next.next.compareAndSet(node, removed)) {
                    w.cur.next.compareAndSet(w.next, node);
                    return true;
                }
            }
        }
    }

    @Override
    public boolean contains(int x) {
        Window w = findWindow(x);
        return w.next.x == x;
    }
}