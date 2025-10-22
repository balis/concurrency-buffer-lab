/**
 * A thread-safe bounded buffer implementation using wait/notify.
 *
 * STUDENT TODO: Implement all methods marked with TODO.
 *
 * Requirements:
 * - Use synchronized methods or blocks
 * - Use wait() to block when buffer is full (for put) or empty (for take)
 * - Use notify() or notifyAll() to wake up waiting threads
 * - Ensure thread safety for all operations
 * - No busy waiting or Thread.sleep()
 */
public class BoundedBuffer<T> {

    private final Object[] buffer;
    private final int capacity;

    // TODO: Add necessary fields to track buffer state
    // Hints: You'll need to track:
    // - The current number of items in the buffer
    // - The index where the next item will be inserted (put index)
    // - The index where the next item will be removed (take index)

    /**
     * Creates a bounded buffer with the specified capacity.
     *
     * @param capacity the maximum number of items the buffer can hold
     * @throws IllegalArgumentException if capacity <= 0
     */
    public BoundedBuffer(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive");
        }
        this.capacity = capacity;
        this.buffer = new Object[capacity];

        // TODO: Initialize your tracking fields
    }

    /**
     * Inserts an item into the buffer.
     * Blocks if the buffer is full until space becomes available.
     *
     * @param item the item to insert (must not be null)
     * @throws InterruptedException if interrupted while waiting
     * @throws IllegalArgumentException if item is null
     */
    public synchronized void put(T item) throws InterruptedException {
        if (item == null) {
            throw new IllegalArgumentException("Cannot put null item");
        }

        // TODO: Implement put operation
        // Hints:
        // 1. Wait while buffer is full (use while loop, not if!)
        // 2. Add item to buffer at the correct position
        // 3. Update buffer state (count, putIndex)
        // 4. Notify waiting threads that buffer state changed

        throw new UnsupportedOperationException("TODO: Implement put()");
    }

    /**
     * Removes and returns an item from the buffer.
     * Blocks if the buffer is empty until an item becomes available.
     *
     * @return the item removed from the buffer
     * @throws InterruptedException if interrupted while waiting
     */
    public synchronized T take() throws InterruptedException {
        // TODO: Implement take operation
        // Hints:
        // 1. Wait while buffer is empty (use while loop, not if!)
        // 2. Remove item from buffer at the correct position
        // 3. Update buffer state (count, takeIndex)
        // 4. Notify waiting threads that buffer state changed
        // 5. Return the item (you'll need to cast it to T)
        // Note: You may need to add @SuppressWarnings("unchecked") when you implement the cast

        throw new UnsupportedOperationException("TODO: Implement take()");
    }

    /**
     * Returns the number of items currently in the buffer.
     *
     * @return the number of items in the buffer
     */
    public synchronized int size() {
        // TODO: Return the current number of items
        throw new UnsupportedOperationException("TODO: Implement size()");
    }

    /**
     * Returns the maximum capacity of the buffer.
     *
     * @return the buffer capacity
     */
    public int getCapacity() {
        return capacity;
    }

    /**
     * Returns true if the buffer is empty.
     *
     * @return true if buffer contains no items
     */
    public synchronized boolean isEmpty() {
        // TODO: Implement isEmpty check
        throw new UnsupportedOperationException("TODO: Implement isEmpty()");
    }

    /**
     * Returns true if the buffer is full.
     *
     * @return true if buffer is at capacity
     */
    public synchronized boolean isFull() {
        // TODO: Implement isFull check
        throw new UnsupportedOperationException("TODO: Implement isFull()");
    }
}
