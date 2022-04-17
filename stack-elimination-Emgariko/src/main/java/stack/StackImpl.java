package stack;

import java.util.Random;
import kotlinx.atomicfu.AtomicRef;

public class StackImpl implements Stack {
    private final static int elimArraySize = 10;
    private final static int spinWait = 10;
    private final static int pushAttempts = 7;
    private final static int popAttempts = 7;
    private final static Random rand = new Random();
    private final AtomicRef<Integer>[] elimArray;

    public StackImpl() {
        elimArray = new AtomicRef[elimArraySize];
        for (int i = 0; i < elimArraySize; i++) {
            elimArray[i] = new AtomicRef<>(null);
        }
    }

    private static class Node {
        final AtomicRef<Node> next;
        final int x;

        Node(int x, Node next) {
            this.next = new AtomicRef<>(next);
            this.x = x;
        }
    }

    // head pointer
    private AtomicRef<Node> head = new AtomicRef<>(null);

    private static int getRandomIndex() {
        return rand.nextInt(elimArraySize);
    }

    @Override
    public void push(int x) {
        int ind = getRandomIndex();

        int pushInd = -1;
        Integer value = x;
        for (int i = 0; i < pushAttempts; i++) {
            int pos = (ind + i) % elimArraySize;
            if (elimArray[pos].compareAndSet(null, value)) {
                pushInd = pos;
                break;
            }
        }
        if (pushInd != -1) {
            for (int i = 0; i < spinWait; i++) {
                if (elimArray[pushInd].getValue() == null) { // can be changed(sum reducing)
                    return;
                }
            }
        }

        if (pushInd == -1 || elimArray[pushInd].compareAndSet(value, null)) {
            simple_push(x);
        }
    }

    private void simple_push(int x) {
        while (true) {
            Node curHead = head.getValue();
            Node newHead = new Node(x, curHead);
            if (head.compareAndSet(curHead, newHead)) {
                return;
            }
        }
    }

    @Override
    public int pop() {
        int ind = getRandomIndex();
        for (int i = 0; i < popAttempts; i++) {
            int pos = (ind + i) % elimArraySize;
            Integer curValue = elimArray[pos].getValue();
            if (curValue != null && elimArray[pos].compareAndSet(curValue, null)) {
                return curValue;
            }
        }
        return simple_pop();
    }

    private int simple_pop() {
        while (true) {
            Node curHead = head.getValue();
            if (curHead == null) {
                return Integer.MIN_VALUE;
            }
            if (head.compareAndSet(curHead, curHead.next.getValue())) {
                return curHead.x;
            }
        }
    }
}
