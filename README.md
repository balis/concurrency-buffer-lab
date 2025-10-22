# Concurrent Programming Lab: Bounded Buffer

## Introduction

A **bounded buffer** is a classic synchronization problem in concurrent programming. It's a fixed-size buffer that:
- Allows producers to add items
- Allows consumers to remove items
- Blocks producers when the buffer is full
- Blocks consumers when the buffer is empty

This pattern is fundamental to many real-world applications like message queues, I/O buffering, and thread pools.

## Assignment

You must implement the `BoundedBuffer` class that provides thread-safe operations for multiple producers and consumers.

# Getting Started

1. Clone the repository
2. Implement the methods in [src/main/java/BoundedBuffer.java](src/main/java/BoundedBuffer.java)
3. Test locally: `./mvnw test`
4. Run the demo: `./mvnw exec:java`
5. Commit and push to trigger autograding

## Building and Testing

```bash
# Run tests
./mvnw test

# Run the demo program
./mvnw exec:java

# Clean build
./mvnw clean test
```

