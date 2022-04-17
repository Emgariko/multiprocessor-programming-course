package dijkstra

import java.util.concurrent.Phaser
import kotlin.concurrent.thread

private val NODE_DISTANCE_COMPARATOR = Comparator<Node> { o1, o2 -> Integer.compare(o1!!.distance, o2!!.distance) }

fun shortestPathParallel(start: Node) {
    val workers = Runtime.getRuntime().availableProcessors()
    start.distance = 0
    val q = MultiQueue(workers, NODE_DISTANCE_COMPARATOR)
    q.add(start)
    val onFinish = Phaser(workers + 1)
    q.activeNodes.lazySet(1)

    repeat(workers) {
        thread {
            while (true) {
                if (q.activeNodes.value == 0) {
                    break
                }
                val cur = q.extractMin() ?: continue
                for (e in cur.outgoingEdges) {
                    val dist = cur.distance + e.weight
                    while (true) {
                        val toDist = e.to.distance
                        if (toDist > dist) {
                            if (e.to.casDistance(toDist, dist)) {
                                q.activeNodes.incrementAndGet()
                                q.add(e.to)
                                break
                            }
                        } else {
                            break
                        }
                    }
                }
                q.activeNodes.decrementAndGet()
            }
            onFinish.arrive()
        }
    }
    onFinish.arriveAndAwaitAdvance()
}