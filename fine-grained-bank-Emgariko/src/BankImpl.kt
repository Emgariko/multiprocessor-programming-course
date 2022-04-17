import java.lang.Math.max
import kotlin.math.min

/**
 * Bank implementation.
 *
 * :TODO: This implementation has to be made thread-safe.
 *
 * @author Garipov Emil
 */
class BankImpl(n: Int) : Bank {
    private val accounts: Array<Account> = Array(n) { Account() }

    override val numberOfAccounts: Int
        get() = accounts.size

    /**
     * :TODO: This method has to be made thread-safe.
     */
    override fun getAmount(index: Int): Long {
        accounts[index].lock.lock()
        val res = accounts[index].amount
        accounts[index].lock.unlock()
        return res
    }

    /**
     * :TODO: This method has to be made thread-safe.
     */
    override val totalAmount: Long
        get() {
            for (i in 0 until accounts.size) accounts[i].lock.lock()
            val res = accounts.sumOf { account -> account.amount }
            for (i in accounts.size - 1 downTo 0) accounts[i].lock.unlock()
            return res
        }

    /**
     * :TODO: This method has to be made thread-safe.
     */
    override fun deposit(index: Int, amount: Long): Long {
        require(amount > 0) { "Invalid amount: $amount" }
        val account = accounts[index]
        account.lock.lock()
        val res: Long;
        try {
            check(!(amount > Bank.MAX_AMOUNT || account.amount + amount > Bank.MAX_AMOUNT)) { "Overflow" }
            account.amount += amount
            res = account.amount
        } catch (e : java.lang.IllegalStateException) {
            throw e;
        } finally {
            account.lock.unlock()
        }
        return res
    }

    /**
     * :TODO: This method has to be made thread-safe.
     */
    override fun withdraw(index: Int, amount: Long): Long {
        require(amount > 0) { "Invalid amount: $amount" }
        val account = accounts[index]
        account.lock.lock()
        val res: Long;
        try {
            check(account.amount - amount >= 0) { "Underflow" }
            account.amount -= amount
            res = account.amount
        } catch (e : java.lang.IllegalStateException) {
            throw e;
        } finally {
            account.lock.unlock()
        }
        return res
    }

    /**
     * :TODO: This method has to be made thread-safe.
     */
    override fun transfer(fromIndex: Int, toIndex: Int, amount: Long) {
        require(amount > 0) { "Invalid amount: $amount" }
        require(fromIndex != toIndex) { "fromIndex == toIndex" }
        val firstLock = accounts[fromIndex.coerceAtMost(toIndex)].lock
        val secondLock = accounts[fromIndex.coerceAtLeast(toIndex)].lock
        firstLock.lock()
        secondLock.lock()
        val from = accounts[fromIndex]
        val to = accounts[toIndex]
        try {
            check(amount <= from.amount) { "Underflow" }
            check(!(amount > Bank.MAX_AMOUNT || to.amount + amount > Bank.MAX_AMOUNT)) { "Overflow" }
            to.amount += amount
            from.amount -= amount
        } catch (e : IllegalStateException) {
            throw e
        } finally {
            secondLock.unlock()
            firstLock.unlock()
        }
    }

    /**
     * Private account data structure.
     */
    class Account {
        /**
         * Amount of funds in this account.
         */
        var amount: Long = 0
        var lock: java.util.concurrent.locks.ReentrantLock = java.util.concurrent.locks.ReentrantLock()
    }
}