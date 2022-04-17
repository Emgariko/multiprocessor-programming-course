import kotlinx.atomicfu.*

class DynamicArrayImpl<E> : DynamicArray<E> {
    private val core = atomic(Core<E>(INITIAL_CAPACITY))
    val sz = atomic(0)

    override fun get(index: Int): E {
        if (index >= sz.value) {
            throw IllegalArgumentException("Index out of bounds")
        }
        val coreVal = core.value
        val node = coreVal.array[index].value
        return node!!.value
    }

    override fun put(index: Int, element: E) {
        while (true) {
            val sz = sz.value
            if (index >= sz) {
                throw IllegalArgumentException("Index out of bounds")
            }
            val coreVal = core.value
            val node = coreVal.array[index]
            val nodeVal = node.value
            val curNxt = coreVal.next.value
            val nodeStateIsStandard = (nodeVal?.state == State.STANDARD)
            val nodeIsFrozen = (nodeVal?.state == State.FROZEN)
            val newNode = Node(element, State.STANDARD)
            if ((nodeStateIsStandard && coreVal.array[index].compareAndSet(nodeVal, newNode))) {
                break
            } else {
                if (nodeIsFrozen) {
                    curNxt!!.array[index].compareAndSet(null, Node(element, State.STANDARD))
                    coreVal.array[index].compareAndSet(nodeVal, Node(nodeVal!!.value, State.MOVED))
                } else {
                    continue
                }
            }
        }
    }

    override fun pushBack(element: E) {
        while (true) {
            val curSz = sz.value
            val curCore = core.value
            val curCapacity = curCore.capacity
            val curNxt = curCore.next.value
            if (curSz < curCapacity) {
                val newNode = Node(element, State.STANDARD)
                if (curCore.array[curSz].compareAndSet(null, newNode)) {
                    sz.compareAndSet(curSz, curSz + 1)
                    break
                } else {
                    sz.compareAndSet(curSz, curSz + 1)
                }
            } else {
                if (curNxt == null) {
                    val nwCore = Core<E>(2 * curCapacity)
                    curCore.next.compareAndSet(null, nwCore)
                    continue
                }
                for (i in 0 until curCapacity) {
                    while (true) {
                        val curNode = curCore.array[i].value
                        val curNodeFrozenState = Node(curNode!!.value, State.FROZEN)
                        if (curNode.state == State.STANDARD) {
                            curCore.array[i].compareAndSet(curNode, curNodeFrozenState)
                        } else if (curNode.state == State.FROZEN) {
                            curNxt.array[i].compareAndSet(null, Node(curNode.value, State.STANDARD))
                            curCore.array[i].compareAndSet(curNodeFrozenState, Node(curNode.value, State.MOVED))
                            break
                        } else {
                            break
                        }
                    }
                }
                core.compareAndSet(curCore, curNxt)
            }
        }
    }

    override val size: Int get() {
        val sz = sz.value
        return sz
    }
}

private class Core<E>(
    val capacity: Int
) {
    val array = atomicArrayOfNulls<Node<E>>(capacity)
    val next = atomic<Core<E>?>(null)
}

private class Node<E>(
    val value: E,
    val state: State = State.STANDARD
) {
}

private enum class State {
    STANDARD, MOVED, FROZEN
}

private const val INITIAL_CAPACITY = 1 // DO NOT CHANGE ME