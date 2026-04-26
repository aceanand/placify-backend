-- Add round_type column if not exists
ALTER TABLE interview_questions
  ADD COLUMN IF NOT EXISTS round_type VARCHAR(50) DEFAULT 'TECHNICAL',
  ADD COLUMN IF NOT EXISTS difficulty VARCHAR(20) DEFAULT 'MEDIUM';

-- Update existing rows
UPDATE interview_questions SET round_type = COALESCE(type, 'TECHNICAL') WHERE round_type IS NULL OR round_type = '';

-- ─── HR ROUND (all roles) ────────────────────────────────────────────────────
INSERT INTO interview_questions (job_role, round_type, difficulty, question, answer) VALUES
('All Roles','HR','EASY','Tell me about yourself.',
 'Structure your answer as: Present (current role/skills), Past (relevant experience), Future (why this role). Keep it under 2 minutes. Focus on professional highlights, not personal history.'),
('All Roles','HR','EASY','Why do you want to join our company?',
 'Research the company beforehand. Mention: 1) Specific product/mission alignment, 2) Growth opportunities, 3) Culture fit. Avoid generic answers like "good salary".'),
('All Roles','HR','EASY','What are your strengths and weaknesses?',
 'Strengths: Pick 2-3 relevant to the role with examples. Weakness: Choose a real one you are actively improving. E.g., "I used to struggle with delegation, so I started using task management tools."'),
('All Roles','HR','MEDIUM','Where do you see yourself in 5 years?',
 'Show ambition aligned with the company. E.g., "I want to grow into a senior/lead role, contribute to larger projects, and mentor junior team members."'),
('All Roles','HR','MEDIUM','Why are you leaving your current job?',
 'Stay positive. Focus on growth: "I am looking for new challenges", "I want to work on larger scale systems", "I want to grow in a different direction." Never badmouth your employer.'),
('All Roles','HR','MEDIUM','What is your expected salary?',
 'Research market rates first. Give a range based on your research. E.g., "Based on my experience and market research, I am looking for X to Y. I am open to discussion based on the overall package."'),
('All Roles','HR','MEDIUM','Describe a conflict with a coworker and how you resolved it.',
 'Use STAR method: Situation, Task, Action, Result. Focus on communication and compromise. Show emotional intelligence.'),
('All Roles','HR','HARD','Tell me about a time you failed.',
 'Pick a real failure. Show: 1) What happened, 2) What you learned, 3) How you applied that learning. Avoid blaming others.'),
('All Roles','HR','EASY','Are you comfortable with remote/hybrid work?',
 'Be honest. If yes, mention your home setup and communication tools you use. If hybrid, express flexibility.'),
('All Roles','HR','EASY','Do you have any questions for us?',
 'Always ask questions! Good ones: "What does success look like in this role in 90 days?", "What are the biggest challenges the team is facing?", "How does the team handle code reviews / deployments?"');

-- ─── BEHAVIORAL ROUND ────────────────────────────────────────────────────────
INSERT INTO interview_questions (job_role, round_type, difficulty, question, answer) VALUES
('All Roles','BEHAVIORAL','MEDIUM','Tell me about a time you handled a tight deadline.',
 'STAR: Describe the project, your prioritization strategy, how you communicated with stakeholders, and the outcome. Mention tools like Jira, time-boxing, or cutting scope.'),
('All Roles','BEHAVIORAL','MEDIUM','Describe a situation where you had to learn something quickly.',
 'Show your learning process: documentation, online resources, asking colleagues. Emphasize the outcome and how fast you became productive.'),
('All Roles','BEHAVIORAL','HARD','Tell me about a time you disagreed with your manager.',
 'Show respectful disagreement: "I shared my concerns with data, listened to their reasoning, and we found a middle ground." Avoid making the manager look bad.'),
('All Roles','BEHAVIORAL','MEDIUM','Give an example of when you showed leadership.',
 'Leadership does not require a title. Examples: leading a project, mentoring a junior, driving a process improvement, or stepping up during a crisis.'),
('All Roles','BEHAVIORAL','EASY','How do you prioritize tasks when everything is urgent?',
 'Mention frameworks: Eisenhower Matrix, MoSCoW, or simply communicating with stakeholders to align on true priorities. Show you do not just react — you think strategically.');

-- ─── TECHNICAL — SOFTWARE ENGINEER ──────────────────────────────────────────
INSERT INTO interview_questions (job_role, round_type, difficulty, question, answer) VALUES
('Software Engineer','TECHNICAL','EASY','What is the difference between stack and heap memory?',
 'Stack: stores local variables, function calls, LIFO order, automatically managed, limited size. Heap: dynamic memory allocation, manually managed (or GC), larger but slower. Stack overflow occurs when stack is exhausted.'),
('Software Engineer','TECHNICAL','MEDIUM','Explain SOLID principles.',
 'S: Single Responsibility — one class, one reason to change. O: Open/Closed — open for extension, closed for modification. L: Liskov Substitution — subclasses must be substitutable. I: Interface Segregation — small specific interfaces. D: Dependency Inversion — depend on abstractions.'),
('Software Engineer','TECHNICAL','MEDIUM','What is the difference between REST and GraphQL?',
 'REST: multiple endpoints, fixed response shape, over/under-fetching issues. GraphQL: single endpoint, client specifies exact data needed, strongly typed schema, better for complex/nested data. REST is simpler; GraphQL is more flexible.'),
('Software Engineer','TECHNICAL','HARD','Explain CAP theorem.',
 'In a distributed system, you can only guarantee 2 of 3: Consistency (all nodes see same data), Availability (every request gets a response), Partition Tolerance (system works despite network splits). Since partitions are unavoidable, you choose CP (e.g., HBase) or AP (e.g., Cassandra).'),
('Software Engineer','TECHNICAL','MEDIUM','What is a deadlock? How do you prevent it?',
 'Deadlock: two or more threads wait for each other indefinitely. Prevention: 1) Lock ordering (always acquire locks in same order), 2) Timeout on lock acquisition, 3) Avoid nested locks, 4) Use lock-free data structures.'),
('Software Engineer','TECHNICAL','EASY','What is Big O notation?',
 'Describes algorithm time/space complexity as input grows. Common: O(1) constant, O(log n) binary search, O(n) linear, O(n log n) merge sort, O(n²) bubble sort. Always analyze worst case unless specified.'),
('Software Engineer','TECHNICAL','MEDIUM','Explain the difference between process and thread.',
 'Process: independent program with its own memory space. Thread: lightweight unit within a process, shares memory. Threads are faster to create/switch but require synchronization. Use threads for I/O-bound, processes for CPU-bound tasks.'),
('Software Engineer','TECHNICAL','HARD','What is eventual consistency?',
 'In distributed systems, after an update, all replicas will eventually converge to the same value — but not immediately. Used in AP systems (Cassandra, DynamoDB). Contrast with strong consistency where all reads reflect the latest write.');

-- ─── SYSTEM DESIGN ROUND ─────────────────────────────────────────────────────
INSERT INTO interview_questions (job_role, round_type, difficulty, question, answer) VALUES
('Software Engineer','SYSTEM_DESIGN','HARD','Design a URL shortener like bit.ly.',
 'Components: 1) API service (POST /shorten → returns short URL), 2) Key generation service (base62 encoding of auto-increment ID), 3) Redirect service (GET /{key} → 301/302 redirect), 4) Database (key-value store like Redis for cache, SQL for persistence), 5) Analytics. Scale: CDN for redirects, read replicas, rate limiting.'),
('Software Engineer','SYSTEM_DESIGN','HARD','Design a notification system.',
 '1) API to accept notification requests, 2) Message queue (Kafka/RabbitMQ) for async processing, 3) Worker services per channel (email, SMS, push), 4) Template service, 5) Retry logic with exponential backoff, 6) User preference service (opt-out), 7) Delivery tracking DB.'),
('Software Engineer','SYSTEM_DESIGN','HARD','How would you design a rate limiter?',
 'Algorithms: Token Bucket (smooth bursts), Leaky Bucket (constant rate), Fixed Window Counter (simple), Sliding Window Log (accurate). Implementation: Redis with atomic operations (INCR + EXPIRE). Store per user/IP. Return 429 when limit exceeded. Distribute across nodes with Redis cluster.'),
('Software Engineer','SYSTEM_DESIGN','HARD','Design a chat application like WhatsApp.',
 '1) WebSocket connections for real-time messaging, 2) Message queue for offline delivery, 3) Message DB (Cassandra — write-heavy, time-series), 4) Presence service (Redis), 5) Media storage (S3 + CDN), 6) End-to-end encryption, 7) Push notifications for offline users.'),
('Backend Developer','SYSTEM_DESIGN','HARD','Design a job queue system.',
 '1) Producer API to enqueue jobs, 2) Message broker (Redis/Kafka/RabbitMQ), 3) Worker pool with configurable concurrency, 4) Job status tracking (DB), 5) Retry with exponential backoff, 6) Dead letter queue for failed jobs, 7) Monitoring dashboard. Consider idempotency for retries.');

-- ─── MACHINE CODING ROUND ────────────────────────────────────────────────────
INSERT INTO interview_questions (job_role, round_type, difficulty, question, answer) VALUES
('Software Engineer','MACHINE_CODING','HARD','Implement an LRU Cache.',
 'Use HashMap + Doubly Linked List. HashMap for O(1) lookup, DLL for O(1) insertion/deletion. On get: move node to front. On put: add to front, evict tail if capacity exceeded. Java: use LinkedHashMap with accessOrder=true as shortcut.'),
('Software Engineer','MACHINE_CODING','HARD','Design a parking lot system.',
 'Classes: ParkingLot, ParkingFloor, ParkingSpot (Compact/Large/Handicapped), Vehicle (Car/Truck/Bike), Ticket, Payment. Use Strategy pattern for pricing. Observer pattern for spot availability. Singleton for ParkingLot.'),
('Software Engineer','MACHINE_CODING','MEDIUM','Implement a rate limiter class.',
 'Token Bucket: maintain tokens count and last refill time. On each request: refill tokens based on elapsed time, check if tokens > 0, decrement and allow, else reject. Thread-safe with synchronized or AtomicLong.'),
('Frontend Developer','MACHINE_CODING','MEDIUM','Build an infinite scroll component.',
 'Use IntersectionObserver to detect when sentinel element enters viewport. On intersection: fetch next page, append to list, update sentinel position. Handle loading state, error state, and end-of-list. Debounce rapid triggers.'),
('Backend Developer','MACHINE_CODING','HARD','Implement a thread-safe singleton.',
 'Double-checked locking: check null, synchronize, check null again, create instance. Or use enum singleton (thread-safe by JVM). Or use static inner class (lazy initialization, thread-safe without synchronization overhead).');

-- ─── FRONTEND DEVELOPER ──────────────────────────────────────────────────────
INSERT INTO interview_questions (job_role, round_type, difficulty, question, answer) VALUES
('Frontend Developer','TECHNICAL','EASY','What is the difference between == and === in JavaScript?',
 '== does type coercion (1 == "1" is true). === checks value AND type (1 === "1" is false). Always use === to avoid unexpected behavior.'),
('Frontend Developer','TECHNICAL','MEDIUM','Explain the JavaScript event loop.',
 'JS is single-threaded. Call stack executes synchronous code. Web APIs handle async (setTimeout, fetch). Callback queue holds completed async callbacks. Event loop moves callbacks to call stack when it is empty. Microtask queue (Promises) has higher priority than macrotask queue (setTimeout).'),
('Frontend Developer','TECHNICAL','MEDIUM','What is React reconciliation?',
 'React compares virtual DOM trees (diffing algorithm) to find minimal changes. Uses keys to identify list items. Fiber architecture allows incremental rendering. Avoid changing component types in same position — causes full remount.'),
('Frontend Developer','TECHNICAL','HARD','Explain React hooks rules and why they exist.',
 'Rules: 1) Only call at top level (not in loops/conditions), 2) Only call from React functions. Why: React relies on call order to associate state with hooks. Breaking order corrupts state mapping. ESLint plugin enforces these rules.'),
('Frontend Developer','TECHNICAL','MEDIUM','What is CSS specificity?',
 'Order: inline styles (1000) > IDs (100) > classes/attributes/pseudo-classes (10) > elements/pseudo-elements (1). !important overrides all. When equal specificity, last rule wins. Use BEM methodology to avoid specificity wars.'),
('Frontend Developer','TECHNICAL','MEDIUM','What is the difference between useMemo and useCallback?',
 'useMemo: memoizes a computed value. useCallback: memoizes a function reference. Both take deps array. Use useMemo for expensive calculations, useCallback to prevent child re-renders when passing callbacks as props. Do not overuse — profiling first.');

-- ─── BACKEND DEVELOPER ───────────────────────────────────────────────────────
INSERT INTO interview_questions (job_role, round_type, difficulty, question, answer) VALUES
('Backend Developer','TECHNICAL','MEDIUM','What is the N+1 query problem?',
 'When fetching a list of N items and then making N additional queries for related data. Fix: use JOIN queries, eager loading (JPA fetch = EAGER), or batch loading. In GraphQL, use DataLoader for batching.'),
('Backend Developer','TECHNICAL','MEDIUM','Explain database indexing.',
 'Index: data structure (B-tree, Hash) that speeds up reads at cost of write performance and storage. Use on: WHERE columns, JOIN columns, ORDER BY columns. Avoid on: low-cardinality columns, frequently updated columns. Composite index: column order matters — leftmost prefix rule.'),
('Backend Developer','TECHNICAL','HARD','What is database sharding?',
 'Horizontal partitioning of data across multiple databases. Shard key determines which shard stores a record. Types: Range-based, Hash-based, Directory-based. Challenges: cross-shard queries, rebalancing, transactions. Use when single DB cannot handle load.'),
('Backend Developer','TECHNICAL','MEDIUM','Explain JWT authentication.',
 'JWT: Header.Payload.Signature. Header: algorithm. Payload: claims (user ID, roles, expiry). Signature: HMAC of header+payload with secret. Stateless — server does not store sessions. Verify signature on each request. Store in httpOnly cookie (not localStorage) to prevent XSS.'),
('Backend Developer','TECHNICAL','EASY','What is the difference between SQL and NoSQL?',
 'SQL: structured schema, ACID transactions, relations, vertical scaling. Good for complex queries. NoSQL: flexible schema, horizontal scaling, eventual consistency. Types: Document (MongoDB), Key-Value (Redis), Column (Cassandra), Graph (Neo4j). Choose based on data structure and access patterns.');

-- ─── DATA ANALYST ────────────────────────────────────────────────────────────
INSERT INTO interview_questions (job_role, round_type, difficulty, question, answer) VALUES
('Data Analyst','TECHNICAL','EASY','What is the difference between INNER JOIN and LEFT JOIN?',
 'INNER JOIN: returns only matching rows from both tables. LEFT JOIN: returns all rows from left table + matching rows from right (NULL for non-matches). Use LEFT JOIN when you want to keep all records from the primary table.'),
('Data Analyst','TECHNICAL','MEDIUM','Explain the difference between OLTP and OLAP.',
 'OLTP: Online Transaction Processing — many small, fast read/write operations (e.g., banking). Normalized schema. OLAP: Online Analytical Processing — complex queries on large datasets (e.g., reporting). Denormalized, star/snowflake schema. Data warehouses use OLAP.'),
('Data Analyst','TECHNICAL','MEDIUM','What is a window function in SQL?',
 'Performs calculation across a set of rows related to current row without collapsing them. Examples: ROW_NUMBER(), RANK(), LAG(), LEAD(), SUM() OVER(PARTITION BY ... ORDER BY ...). Unlike GROUP BY, window functions do not reduce row count.'),
('Data Analyst','TECHNICAL','HARD','How do you handle missing data?',
 'Options: 1) Remove rows (if few and random), 2) Mean/median/mode imputation, 3) Forward/backward fill (time series), 4) Model-based imputation (KNN, regression), 5) Flag as separate category. Choice depends on missingness pattern (MCAR, MAR, MNAR) and downstream impact.'),
('Data Analyst','TECHNICAL','MEDIUM','What is the difference between correlation and causation?',
 'Correlation: statistical relationship between two variables. Causation: one variable directly causes change in another. Correlation does not imply causation. To establish causation: randomized controlled trials, A/B tests, or causal inference methods (instrumental variables, diff-in-diff).');

-- ─── FULL STACK DEVELOPER ────────────────────────────────────────────────────
INSERT INTO interview_questions (job_role, round_type, difficulty, question, answer) VALUES
('Full Stack Developer','TECHNICAL','MEDIUM','What is CORS and how do you handle it?',
 'Cross-Origin Resource Sharing: browser security mechanism blocking requests to different origins. Server must include Access-Control-Allow-Origin header. In Spring Boot: @CrossOrigin or global CorsConfig. In Express: cors() middleware. Preflight OPTIONS request checks permissions before actual request.'),
('Full Stack Developer','TECHNICAL','MEDIUM','Explain the difference between authentication and authorization.',
 'Authentication: verifying identity (who are you?). Authorization: verifying permissions (what can you do?). Auth flow: login → JWT token → send token with requests → server validates token (authn) → checks user roles (authz).'),
('Full Stack Developer','TECHNICAL','HARD','How do you optimize a slow web application?',
 'Frontend: code splitting, lazy loading, image optimization, CDN, caching, reduce bundle size. Backend: DB query optimization, caching (Redis), connection pooling, async processing. Network: HTTP/2, compression (gzip/brotli), minimize round trips. Measure first with profiling tools.');
