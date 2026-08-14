# 🛡️ UG Campus Security & Emergency Response Optimizer

An enterprise-grade Java 17 & SQLite academic simulation designed to optimize incident reporting, dispatch priorities, and emergency response routing across the University of Ghana, Legon campus.

---

## 📌 Project Overview
This system serves as the backend operational brain for UG Campus Security. It processes fictional safety incidents (medical cases, theft, crowd control, road blockages), calculates real-time incident priorities, assigns available security teams, determines optimal routes across campus, and tracks incident lifecycles with full audit logging.

Every data structure and algorithm core is built **from scratch** without relying on standard Java Collections (`ArrayList`, `HashMap`, `PriorityQueue`, etc.).

---

## 🏗️ System Architecture & Workflow

`SQLite Database` ➔ `DAO Layer` ➔ `Custom Data Structures` ➔ `Algorithm Engines` ➔ `Service Layer` ➔ `Console UI`

1. **Data Ingestion:** Reads locations, campus roads, available security resources, and incident logs from SQLite/CSV files.
2. **Prioritization & Queueing:** Incident severity is calculated; critical life-safety incidents enter a priority heap and deque, while routine issues use FIFO queues.
3. **Indexing & Search:** Custom BSTs, Red-Black Trees, B-Trees, and Hash Tables maintain fast indices over active incidents.
4. **Route Optimization:** Dijkstra and Graph algorithms determine the fastest non-blocked response route across campus nodes.
5. **Audit Trail & Undo:** Stacks maintain incident status history and allow undo operations on incorrect dispatches.

---

## 👥 Team Structure & Allocation

* **Overall Project Lead:** Isaac Morrison Quaye
* **Frontend Lead:** Selorm Sem
* **GitHub & Integration Lead:** Eastwood Tweneboah Osei
* **Algorithm Quality Coordinator:** Messiah Asiedu

### Module Division
* **Team 1 (Foundation & Routing):** Dynamic Arrays, Linked Lists, Graph (Matrix/List), Disjoint Set | BFS, DFS, Dijkstra, Prim, Kruskal.
* **Team 2 (Requests & Priority Indexing):** Binary Heap, Priority Queue, B-Tree, Hash Table, Custom Set/Map | Linear/Binary Search, Selection/Insertion Sort, Greedy Assignment.
* **Team 3 (Workflow, Trees & Optimization):** BST, Red-Black Tree, Stack, FIFO Queue, Circular Queue, Deque | Merge Sort, Quick Sort, Dynamic Programming, Brute-Force.

---

## 🛠️ Tech Stack & Requirements
* **Language:** Java 17 (Pure Java, no built-in collections for core logic)
* **Database:** SQLite (JDBC Driver)
* **Testing Framework:** JUnit 5
* **Build System:** Standard Java Compiler (`javac`) / IDE (VS Code / IntelliJ)

---

## 🚀 Getting Started

### 1. Clone the Repository
```bash
git clone [https://github.com/YOUR_ORGANIZATION/ug-security-optimizer.git](https://github.com/YOUR_ORGANIZATION/ug-security-optimizer.git)
cd ug-security-optimizer
