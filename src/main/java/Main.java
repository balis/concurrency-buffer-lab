import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Driver program demonstrating the BoundedBuffer with multiple producers and consumers.
 *
 * DO NOT MODIFY THIS FILE - It is provided as a complete demo and reference.
 *
 * This program creates:
 * - Multiple producer threads that add items to the buffer
 * - Multiple consumer threads that remove items from the buffer
 * - Demonstrates proper synchronization and thread coordination
 */
public class Main {

    private static final int BUFFER_CAPACITY = 5;
    private static final int NUM_PRODUCERS = 3;
    private static final int NUM_CONSUMERS = 2;
    private static final int ITEMS_PER_PRODUCER = 10;

    // Shared counters for tracking progress
    private static final AtomicInteger producedCount = new AtomicInteger(0);
    private static final AtomicInteger consumedCount = new AtomicInteger(0);

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Bounded Buffer Demo ===");
        System.out.println("Buffer Capacity: " + BUFFER_CAPACITY);
        System.out.println("Producers: " + NUM_PRODUCERS);
        System.out.println("Consumers: " + NUM_CONSUMERS);
        System.out.println("Items per Producer: " + ITEMS_PER_PRODUCER);
        System.out.println("Total Items: " + (NUM_PRODUCERS * ITEMS_PER_PRODUCER));
        System.out.println();

        BoundedBuffer<String> buffer = new BoundedBuffer<>(BUFFER_CAPACITY);

        // Create producer threads
        Thread[] producers = new Thread[NUM_PRODUCERS];
        for (int i = 0; i < NUM_PRODUCERS; i++) {
            final int producerId = i + 1;
            producers[i] = new Thread(new Producer(buffer, producerId, ITEMS_PER_PRODUCER), "Producer-" + producerId);
            producers[i].start();
        }

        // Create consumer threads
        Thread[] consumers = new Thread[NUM_CONSUMERS];
        for (int i = 0; i < NUM_CONSUMERS; i++) {
            final int consumerId = i + 1;
            consumers[i] = new Thread(new Consumer(buffer, consumerId, NUM_PRODUCERS * ITEMS_PER_PRODUCER), "Consumer-" + consumerId);
            consumers[i].start();
        }

        // Wait for all producers to finish
        for (Thread producer : producers) {
            producer.join();
        }
        System.out.println("\n>>> All producers finished <<<");

        // Wait for all consumers to finish
        for (Thread consumer : consumers) {
            consumer.join();
        }
        System.out.println(">>> All consumers finished <<<\n");

        // Print summary
        System.out.println("=== Summary ===");
        System.out.println("Total Produced: " + producedCount.get());
        System.out.println("Total Consumed: " + consumedCount.get());
        System.out.println("Buffer Size: " + buffer.size());
        System.out.println("Buffer Empty: " + buffer.isEmpty());

        if (producedCount.get() == consumedCount.get() && buffer.isEmpty()) {
            System.out.println("\n✅ SUCCESS: All items produced and consumed correctly!");
        } else {
            System.out.println("\n❌ ERROR: Item count mismatch or buffer not empty!");
        }
    }

    /**
     * Producer thread that adds items to the buffer.
     */
    static class Producer implements Runnable {
        private final BoundedBuffer<String> buffer;
        private final int id;
        private final int itemCount;
        private final Random random = new Random();

        public Producer(BoundedBuffer<String> buffer, int id, int itemCount) {
            this.buffer = buffer;
            this.id = id;
            this.itemCount = itemCount;
        }

        @Override
        public void run() {
            try {
                for (int i = 1; i <= itemCount; i++) {
                    String item = "P" + id + "-Item" + i;

                    // Simulate some work before producing
                    Thread.sleep(random.nextInt(50));

                    buffer.put(item);
                    producedCount.incrementAndGet();

                    System.out.println("[Producer-" + id + "] Produced: " + item +
                            " (Buffer size: " + buffer.size() + "/" + buffer.getCapacity() + ")");
                }
                System.out.println("[Producer-" + id + "] Finished producing " + itemCount + " items");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("[Producer-" + id + "] Interrupted");
            }
        }
    }

    /**
     * Consumer thread that removes items from the buffer.
     */
    static class Consumer implements Runnable {
        private final BoundedBuffer<String> buffer;
        private final int id;
        private final int totalItems;
        private final Random random = new Random();

        public Consumer(BoundedBuffer<String> buffer, int id, int totalItems) {
            this.buffer = buffer;
            this.id = id;
            this.totalItems = totalItems;
        }

        @Override
        public void run() {
            try {
                while (consumedCount.get() < totalItems) {
                    String item = buffer.take();
                    int count = consumedCount.incrementAndGet();

                    System.out.println("  [Consumer-" + id + "] Consumed: " + item +
                            " (Buffer size: " + buffer.size() + "/" + buffer.getCapacity() + ")");

                    // Simulate some work after consuming
                    Thread.sleep(random.nextInt(100));

                    // Stop if we've consumed all items
                    if (count >= totalItems) {
                        break;
                    }
                }
                System.out.println("  [Consumer-" + id + "] Finished");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("  [Consumer-" + id + "] Interrupted");
            }
        }
    }
}
