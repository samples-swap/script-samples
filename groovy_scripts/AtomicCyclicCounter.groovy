import java.util.concurrent.atomic.AtomicInteger

/**
 * A cyclic counter that is backed by an {@code AtomicInteger}.
 *
 * This counter will return integers starting at {@code start} up to {@code max}, then starting over (cycling).
 */
class AtomicCyclicCounter {
    int max
    int start = 1
    private AtomicInteger counter = new AtomicInteger()

    AtomicCyclicCounter() {
        this([:])
    }

    AtomicCyclicCounter(Map<String, Object> properties) {
        properties.each { String k, Object v ->
            this."set${k.capitalize()}"(v)
        }
        resetCounter()
    }

    private void resetCounter() {
        counter.set(start - 1) // minus one has us begin at start value
    }

    void setStart(int start) {
        this.start = start
        resetCounter()
    }

    /**
     * Return the next value, or start over at the {@code start} value if {@code max} has been reached.
     * @return the next value
     */
    int getAndIncrement() {
        if (start > max) {
            throw new IllegalArgumentException("start must be <= max")
        }
        int curVal, newVal
        for (;;) {
            curVal = counter.get()
            newVal = (curVal + 1) % (max + 1) // max + 1 makes it inclusive
            if (newVal < start) {
                newVal = start
            }

            // compare when setting in case another thread beat us to newVal
            if (counter.compareAndSet(curVal, newVal)) {
                return newVal
            }
        }
    }
}