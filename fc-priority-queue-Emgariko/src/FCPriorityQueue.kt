import kotlinx.atomicfu.AtomicInt
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.locks.ReentrantLock
import java.util.*

class FCPriorityQueue<E : Comparable<E>> {
    object Params {
        const val tasksSize = 32
    }

    class Task<E>(var data: E? = null) {
        val op: AtomicInt = atomic(0)
    }

    private val lock = ReentrantLock()
    private val q = PriorityQueue<E>()
    private val tasks = Array<Task<E>>(Params.tasksSize) {Task(null)}
    private val NOTHING = 0
    private val POLL = 1
    private val ADD = 2
    private val PEEK = 3
    private val DONE = 4
    private val WAITDATA = 5

    /**
     * Retrieves the element with the highest priority
     * and returns it as the result of this function;
     * returns `null` if the queue is empty.
     */
    fun poll(): E? {
        return doTask(POLL, null)
    }

    /**
     * Returns the element with the highest priority
     * or `null` if the queue is empty.
     */
    fun peek(): E? {
        return doTask(PEEK, null)
    }

    /**
     * Adds the specified element to the queue.
     */
    fun add(element: E) {
        doTask(ADD, element)
    }

    fun combine(curTaskPos: Int?): E? {
        var res: E? = null
        for (i in 0 until Params.tasksSize) {
            if (tasks[i].op.value == ADD) {
                q.add(tasks[i].data)
                tasks[i].data = null
                tasks[i].op.compareAndSet(ADD, DONE)
            } else if (tasks[i].op.value == POLL) {
                tasks[i].data = q.poll()
                if (i == curTaskPos) {
                    res = tasks[i].data
                }
                tasks[i].op.compareAndSet(POLL, DONE)
            } else if (tasks[i].op.value == PEEK) {
                tasks[i].data = q.peek()
                if (i == curTaskPos) {
                    res = tasks[i].data
                }
                tasks[i].op.compareAndSet(PEEK, DONE)
            } else if (tasks[i].op.value == WAITDATA) {
                continue
            }
        }
        return res
    }

    fun doTask(task: Int, element: E?): E? {
        if (lock.tryLock()) {
            var res: E? = null
            if (task == ADD) {
                res =  null
                q.add(element)
            } else if (task == POLL) {
                res = q.poll()
            } else if (task == PEEK) {
                res = q.peek()
            }
            combine(null)
            lock.unlock()
            return res
        } else {
            var pos = -1
            while (pos == -1) {
                for (i in 0 until Params.tasksSize) {
                    if (task == ADD) {
                        if (tasks[i].op.compareAndSet(NOTHING, WAITDATA)) {
                            tasks[i].data = element
                            tasks[i].op.value = task
                            pos = i
                            break
                        } else {
                            continue
                        }
                    } else if (tasks[i].op.compareAndSet(NOTHING, task)) {
                        if (task == POLL || task == PEEK) {
                            // do nothing
                        }
                        pos = i
                        break
                    }
                }
            }
            var res: E? = null
            while (true) {
                if (tasks[pos].op.value == DONE) {
                    if (task == ADD) {
                        // do nothing
                    } else if (task == POLL || task == PEEK) {
                        res = tasks[pos].data
                        tasks[pos].data = null
                    }
                    tasks[pos].op.compareAndSet(DONE, NOTHING)
                    break
                }
                if (lock.tryLock()) {
                    res = combine(pos)
                    lock.unlock()
                }
            }
            return res
        }
    }
}