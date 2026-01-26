Purpose of codebase: POC personal project for demonstrating
concepts of an architecture for building a obust, high-performance 
financial trading engine in Java. This uses an event-driven, 
single-threaded matching engine with a concurrent queue 
(like a BlockingQueue) for incoming orders. This design avoids 
locking the core matching logic, enhancing both safety and speed.