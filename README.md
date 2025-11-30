# CPSC 441 — Programming Assignments
Author: Sultan Alzoghaibi  
UCID: 30178597

This directory contains solutions for **Assignment 1** (TCP) and **Assignment 2** (UDP), including bonus features completed for both.

---

## Assignment 1 — TCP File & Message Server
**Goal:** Implement a TCP server that handles multiple clients, supports message exchange, and performs file transfers.

**Features Implemented**
- Handles multiple concurrent client connections.
- Receives and stores uploaded files via TCP.
- Sends files back to clients on request.
- Clean client/server protocol for commands and responses.
- **Bonus completed:**
    - Extended file transfer support
    - Robust I/O handling & logging

**How to Run**
Run Server + X amount of Clients

---

## Assignment 2 — UDP Multi-Client Chat
**Goal:** Build a UDP-based chat program that supports multiple users and real-time message broadcasting.

**Features Implemented**
- UDP server receives and relays messages to all active clients.
- Clients can join, send messages, and exit gracefully.
- Event-driven message handling without persistent connections.
- **Bonus completed:**
    - Support for chatting with multiple clients concurrently
    - Asynchronous receive/send loop

**How to Run**
Run Server + X amount of Clients
