package dijkstra

import kotlinx.atomicfu.atomic
import java.util.*
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.locks.ReentrantLock

class MultiQueue(workers: Int, cmp: Comparator<Node>) {
    val activeNodes = atomic(0)
    private val cnt = workers * 2
    private val locks = ArrayList<ReentrantLock>(cnt)
    private val queues = ArrayList<PriorityQueue<Node>>(cnt)

    init {
        for (i in 1..cnt) {
            queues.add(PriorityQueue(cmp));
            locks.add(ReentrantLock())
        }
    }

    fun add(x: Node) {
        while (true) {
            val id = ThreadLocalRandom.current().nextInt(cnt)
            if (locks[id].tryLock()) {
                queues[id].offer(x)
                locks[id].unlock()
                break
            }
        }
    }

    fun extractMin(): Node? {
        while (true) {
            val id1 = ThreadLocalRandom.current().nextInt(cnt)
            val id2 = ThreadLocalRandom.current().nextInt(cnt)
            if (id1 == id2) { continue }
            if (locks[id1].tryLock()) {
                if (locks[id2].tryLock()) {
                    var res = queues[id1].peek()
                    val y = queues[id2].peek()
                    res = if (res == null) {
                        queues[id2].poll()
                    } else {
                        if (y == null) {
                            queues[id1].poll()
                        } else {
                            if (queues[id1].comparator().compare(res, y) < 0) {
                                queues[id1].poll()
                            } else {
                                queues[id2].poll()
                            }
                        }
                    }
                    locks[id1].unlock()
                    locks[id2].unlock()
                    return res
                }
                locks[id1].unlock()
            }
        }
    }
}