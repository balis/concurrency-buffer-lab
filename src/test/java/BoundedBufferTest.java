import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for BoundedBuffer implementation.
 *
 * Tests cover:
 * - Basic operations
 * - Thread safety
 * - Blocking behavior
 * - Multiple producers and consumers
 * - Stress testing
 */
@Timeout(10) // Global timeout: all tests must complete within 10 seconds
class BoundedBufferTest {

    @Test
    @DisplayName("Constructor should reject invalid capacity")
    void testConstructorInvalidCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new BoundedBuffer<>(0));
        assertThrows(IllegalArgumentException.class, () -> new BoundedBuffer<>(-1));
    }

    @Test
    @DisplayName("Constructor should accept valid capacity")
    void testConstructorValidCapacity() {
        assertDoesNotThrow(() -> new BoundedBuffer<>(1));
        assertDoesNotThrow(() -> new BoundedBuffer<>(10));
        assertDoesNotThrow(() -> new BoundedBuffer<>(100));
    }

    @Test
    @DisplayName("New buffer should be empty")
    void testNewBufferIsEmpty() {
        BoundedBuffer<String> buffer = new BoundedBuffer<>(5);
        assertTrue(buffer.isEmpty());
        assertFalse(buffer.isFull());
        assertEquals(0, buffer.size());
        assertEquals(5, buffer.getCapacity());
    }

    @Test
    @DisplayName("Put should reject null items")
    void testPutNullItem() {
        BoundedBuffer<String> buffer = new BoundedBuffer<>(5);
        assertThrows(IllegalArgumentException.class, () -> buffer.put(null));
    }

    @Test
    @DisplayName("Single put and take should work")
    void testSinglePutAndTake() throws InterruptedException {
        BoundedBuffer<String> buffer = new BoundedBuffer<>(5);

        buffer.put("item1");
        assertEquals(1, buffer.size());
        assertFalse(buffer.isEmpty());
        assertFalse(buffer.isFull());

        String item = buffer.take();
        assertEquals("item1", item);
        assertEquals(0, buffer.size());
        assertTrue(buffer.isEmpty());
    }

    @Test
    @DisplayName("Multiple puts and takes should maintain FIFO order")
    void testFIFOOrder() throws InterruptedException {
        BoundedBuffer<Integer> buffer = new BoundedBuffer<>(10);

        for (int i = 0; i < 5; i++) {
            buffer.put(i);
        }

        for (int i = 0; i < 5; i++) {
            assertEquals(i, buffer.take());
        }
    }

    @Test
    @DisplayName("Buffer should enforce capacity limit")
    void testCapacityLimit() throws InterruptedException {
        BoundedBuffer<String> buffer = new BoundedBuffer<>(3);

        buffer.put("item1");
        buffer.put("item2");
        buffer.put("item3");

        assertTrue(buffer.isFull());
        assertEquals(3, buffer.size());
    }

    @Test
    @DisplayName("Buffer should handle wrap-around correctly")
    void testCircularBuffer() throws InterruptedException {
        BoundedBuffer<Integer> buffer = new BoundedBuffer<>(3);

        // Fill the buffer
        buffer.put(1);
        buffer.put(2);
        buffer.put(3);

        // Remove two items
        assertEquals(1, buffer.take());
        assertEquals(2, buffer.take());

        // Add two more items (should wrap around)
        buffer.put(4);
        buffer.put(5);

        // Verify order
        assertEquals(3, buffer.take());
        assertEquals(4, buffer.take());
        assertEquals(5, buffer.take());
        assertTrue(buffer.isEmpty());
    }

    @Test
    @DisplayName("Put should block when buffer is full")
    @Timeout(5)
    void testPutBlocksWhenFull() throws InterruptedException {
        BoundedBuffer<String> buffer = new BoundedBuffer<>(2);

        // Fill the buffer
        buffer.put("item1");
        buffer.put("item2");
        assertTrue(buffer.isFull());

        // Try to put another item in a separate thread
        Thread producer = new Thread(() -> {
            try {
                buffer.put("item3"); // Should block
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        producer.start();
        Thread.sleep(100); // Give producer time to block

        // Producer should be blocked (waiting)
        assertEquals(Thread.State.WAITING, producer.getState());

        // Take an item to unblock producer
        buffer.take();
        producer.join(1000); // Wait for producer to finish

        assertFalse(producer.isAlive());
        assertEquals(2, buffer.size());
    }

    @Test
    @DisplayName("Take should block when buffer is empty")
    @Timeout(5)
    void testTakeBlocksWhenEmpty() throws InterruptedException {
        BoundedBuffer<String> buffer = new BoundedBuffer<>(5);
        assertTrue(buffer.isEmpty());

        // Try to take from empty buffer in a separate thread
        String[] result = new String[1];
        Thread consumer = new Thread(() -> {
            try {
                result[0] = buffer.take(); // Should block
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        consumer.start();
        Thread.sleep(100); // Give consumer time to block

        // Consumer should be blocked (waiting)
        assertEquals(Thread.State.WAITING, consumer.getState());

        // Put an item to unblock consumer
        buffer.put("item1");
        consumer.join(1000); // Wait for consumer to finish

        assertFalse(consumer.isAlive());
        assertEquals("item1", result[0]);
    }

    @Test
    @DisplayName("Single producer and consumer should work correctly")
    void testSingleProducerConsumer() throws InterruptedException {
        BoundedBuffer<Integer> buffer = new BoundedBuffer<>(5);
        int itemCount = 20;
        List<Integer> consumed = Collections.synchronizedList(new ArrayList<>());

        Thread producer = new Thread(() -> {
            try {
                for (int i = 0; i < itemCount; i++) {
                    buffer.put(i);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Thread consumer = new Thread(() -> {
            try {
                for (int i = 0; i < itemCount; i++) {
                    consumed.add(buffer.take());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        producer.start();
        consumer.start();

        producer.join();
        consumer.join();

        assertEquals(itemCount, consumed.size());
        for (int i = 0; i < itemCount; i++) {
            assertEquals(i, consumed.get(i));
        }
        assertTrue(buffer.isEmpty());
    }

    @Test
    @DisplayName("Multiple producers should work correctly")
    void testMultipleProducers() throws InterruptedException {
        BoundedBuffer<Integer> buffer = new BoundedBuffer<>(50);
        int numProducers = 5;
        int itemsPerProducer = 10;
        AtomicInteger producedCount = new AtomicInteger(0);

        Thread[] producers = new Thread[numProducers];
        for (int i = 0; i < numProducers; i++) {
            final int producerId = i;
            producers[i] = new Thread(() -> {
                try {
                    for (int j = 0; j < itemsPerProducer; j++) {
                        buffer.put(producerId * 100 + j);
                        producedCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            producers[i].start();
        }

        for (Thread producer : producers) {
            producer.join();
        }

        assertEquals(numProducers * itemsPerProducer, producedCount.get());
        assertEquals(numProducers * itemsPerProducer, buffer.size());
    }

    @Test
    @DisplayName("Multiple consumers should work correctly")
    void testMultipleConsumers() throws InterruptedException {
        BoundedBuffer<Integer> buffer = new BoundedBuffer<>(10);
        int totalItems = 50;
        List<Integer> consumed = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch producerDone = new CountDownLatch(1);
        CountDownLatch consumersDone = new CountDownLatch(totalItems);

        // Producer runs concurrently with consumers
        Thread producer = new Thread(() -> {
            try {
                for (int i = 0; i < totalItems; i++) {
                    buffer.put(i);
                }
                producerDone.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // Start consumers - each takes items until total is reached
        int numConsumers = 5;
        Thread[] consumers = new Thread[numConsumers];
        for (int i = 0; i < numConsumers; i++) {
            consumers[i] = new Thread(() -> {
                try {
                    while (consumed.size() < totalItems) {
                        Integer item = buffer.take();
                        consumed.add(item);
                        consumersDone.countDown();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            consumers[i].start();
        }

        // Start producer
        producer.start();

        // Wait for all items to be consumed
        consumersDone.await();

        // Give threads time to finish cleanly
        producer.join(1000);
        for (Thread consumer : consumers) {
            consumer.interrupt(); // Wake up any waiting consumers
        }

        assertEquals(totalItems, consumed.size());
    }

    @Test
    @DisplayName("Multiple producers and consumers should work correctly")
    void testMultipleProducersAndConsumers() throws InterruptedException {
        BoundedBuffer<Integer> buffer = new BoundedBuffer<>(10);
        int numProducers = 3;
        int numConsumers = 3;
        int itemsPerProducer = 20;
        int totalItems = numProducers * itemsPerProducer;

        AtomicInteger producedCount = new AtomicInteger(0);
        AtomicInteger consumedCount = new AtomicInteger(0);
        CountDownLatch producersDone = new CountDownLatch(numProducers);
        CountDownLatch consumersDone = new CountDownLatch(totalItems);

        // Create producers
        for (int i = 0; i < numProducers; i++) {
            final int producerId = i;
            new Thread(() -> {
                try {
                    for (int j = 0; j < itemsPerProducer; j++) {
                        buffer.put(producerId * 1000 + j);
                        producedCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    producersDone.countDown();
                }
            }).start();
        }

        // Create consumers
        Thread[] consumerThreads = new Thread[numConsumers];
        for (int i = 0; i < numConsumers; i++) {
            consumerThreads[i] = new Thread(() -> {
                try {
                    while (true) {
                        buffer.take();
                        int count = consumedCount.incrementAndGet();
                        consumersDone.countDown();
                        if (count >= totalItems) {
                            break;
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            consumerThreads[i].start();
        }

        // Wait for all items to be consumed
        consumersDone.await();

        // Interrupt any consumers still waiting
        for (Thread t : consumerThreads) {
            t.interrupt();
        }

        assertEquals(totalItems, producedCount.get());
        assertEquals(totalItems, consumedCount.get());
    }

    @Test
    @DisplayName("Stress test with many threads")
    void testStressWithManyThreads() throws InterruptedException {
        BoundedBuffer<Integer> buffer = new BoundedBuffer<>(20);
        int numProducers = 5;
        int numConsumers = 5;
        int itemsPerProducer = 30;
        int totalItems = numProducers * itemsPerProducer;

        AtomicInteger producedCount = new AtomicInteger(0);
        AtomicInteger consumedCount = new AtomicInteger(0);
        CyclicBarrier startBarrier = new CyclicBarrier(numProducers + numConsumers);
        CountDownLatch producersDone = new CountDownLatch(numProducers);
        CountDownLatch consumersDone = new CountDownLatch(totalItems);

        // Create producers
        for (int i = 0; i < numProducers; i++) {
            final int producerId = i;
            new Thread(() -> {
                try {
                    startBarrier.await(); // All threads start together
                    for (int j = 0; j < itemsPerProducer; j++) {
                        buffer.put(producerId * 10000 + j);
                        producedCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    producersDone.countDown();
                }
            }).start();
        }

        // Create consumers
        Thread[] consumerThreads = new Thread[numConsumers];
        for (int i = 0; i < numConsumers; i++) {
            consumerThreads[i] = new Thread(() -> {
                try {
                    startBarrier.await(); // All threads start together
                    while (true) {
                        buffer.take();
                        int count = consumedCount.incrementAndGet();
                        consumersDone.countDown();
                        if (count >= totalItems) {
                            break;
                        }
                    }
                } catch (Exception e) {
                    // Expected for interrupted threads
                }
            });
            consumerThreads[i].start();
        }

        // Wait for all items to be consumed
        consumersDone.await();

        // Clean up
        for (Thread t : consumerThreads) {
            t.interrupt();
        }

        assertEquals(totalItems, producedCount.get());
        assertEquals(totalItems, consumedCount.get());
    }

    @Test
    @DisplayName("Thread safety: no race conditions in size")
    void testThreadSafetyNoRaceConditions() throws InterruptedException {
        BoundedBuffer<Integer> buffer = new BoundedBuffer<>(50);
        int numThreads = 20;
        int operationsPerThread = 50;
        CountDownLatch latch = new CountDownLatch(numThreads);

        // Half threads produce, half consume
        for (int i = 0; i < numThreads / 2; i++) {
            final int threadId = i;
            new Thread(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        buffer.put(threadId * 1000 + j);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        for (int i = 0; i < numThreads / 2; i++) {
            new Thread(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        buffer.take();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        latch.await();
        assertTrue(buffer.isEmpty());
    }

    @Test
    @DisplayName("Interrupted thread should throw InterruptedException")
    void testInterruptedException() throws InterruptedException {
        BoundedBuffer<String> buffer = new BoundedBuffer<>(1);
        buffer.put("item"); // Fill buffer

        Thread producer = new Thread(() -> {
            try {
                buffer.put("another"); // Should block
                fail("Should have thrown InterruptedException");
            } catch (InterruptedException e) {
                // Expected
                assertTrue(Thread.currentThread().isInterrupted());
            }
        });

        producer.start();
        Thread.sleep(100); // Let producer block
        producer.interrupt(); // Interrupt the blocked thread
        producer.join();
    }
}
