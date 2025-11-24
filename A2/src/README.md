# CPSC 441 – Programming Assignment 2
## UDP Chat Application with Application-Layer Reliability

This project implements a chat system over **UDP**, where all connection establishment, acknowledgements, message reliability, and termination are handled at the **application layer**.

---

## 🔧 Architecture Overview

### Server
- Runs on **UDP port 7070**.
- Maintains a mapping: `ClientID → ListenerPort`.
- Automatically prints the updated active-client list whenever a client joins or leaves.
- Handles:
    - registering clients
    - returning port numbers
    - relaying connection results
    - relaying chat messages
    - tracking active chat sessions
    - removing a client when they leave

### Client
Each client creates **two UDP sockets**:
1. **ServerConnectsocket** — used to register with server & send commands
2. **ServerNewChatListenerSocket** — a dedicated listener for:
    - incoming chat requests
    - chat messages
    - connection accept/reject
    - termination messages

This listener thread always stays alive and processes messages concurrently.

The server also sends:
```
CHAT_PORT:<port>:SEQ:<num>
```
which the client ACKs before using.

---

## 🔁 Reliable Data Transfer (RDT)

Since UDP provides no delivery guarantee, I implemented reliability manually:

### **On send**
- Append `:SEQ:<num>`
- Send packet up to **3 times**
- Wait up to **2 seconds** for an ACK
- Accept ACK only if it matches `SEQ + 1`

### **On receive**
- Parse sequence number
- Send back `ACK:SEQ:<received_seq + 1>`
- Return the original message after stripping trailing SEQ

Duplicate messages (same SEQ) are ignored.

---

## 🔐 Preventing Race Conditions (Scanner Input)

Multiple threads:
- Main thread (user input)
- Chat listener thread

Both need to read from `Scanner`.  
To prevent **race conditions** and double-reading the terminal:

```
private final ReentrantLock inputLock = new ReentrantLock(true);

inputLock.lock();
try {
    input = sc.nextLine();
} finally {
    inputLock.unlock();
}
```

This ensures **only one thread can read input at a time**.

---

## 🔌 Connection Establishment

### For Client A connecting to Client B:

1. A sends:  
   `connect <B>`
2. Server returns B’s listener port
3. A sends:
   ```
   CONNECTION_REQUEST:FROM:<A>
   ```
4. B’s listener thread asks user:
   ```
   Accept from <clientID? (y/n)
   ```
5. B replies with:
    - `CONNECTION_ACCEPT:FROM:<B>`
    - OR  
      `CONNECTION_REJECT:FROM:<B>`

6. Server updates:
   ```
   connectionsMap[A].add(B)
   connectionsMap[B].add(A)
   ```

7. Server notifies A:
   ```
   CONNECTION_WAS_ACCEPTED:client B accepted
   ```

---

## 💬 Chat Messaging

When two clients are connected, messages are formatted as:

```
CHATFROM:<sender_id>:<text>
```

The receiving client displays:

```
[CHAT] Message from client <id>: <text>
```

Every message is:
- sent via reliable sender
- requires ACK
- duplicate messages ignored

---

## 🔚 Connection Termination

Either client can run:

```
terminate <client_id>
```

This sends:

```
TERMINATE:Chat with <id> has been terminated.
```

Both clients remove each other from `connectionsMap`.

---

## 🚪 Leaving the Application

Client sends a “leave” command:

```
leave
```

Server:
- removes the client from `clientMap`
- cleans up connections involving that ID
- sends back:
```
BYE: Client <id> has left the application.
```

Client prints message and stops running.

---

## 📝 Difficulties & How I Solved Them

### **1. Handling scanner input from multiple threads**
- Fixed with a **ReentrantLock**, preventing simultaneous `nextLine()` calls.

### **2. UDP messages arriving on the wrong socket**
- Introduced **dedicated listener socket** for chat events.
- Main thread only listens for server responses.

### **3. Sequence number parsing collisions**
- Standardized message formats.
- Always append SEQ at the **end**.
- ACK logic isolated in one function.

### **4. Keeping track of active chats**
- `connectionsMap` with a `HashSet` for each client.

---

## 🚀 Possible Improvements
- Add message ordering buffer
- Add encryption
- Allow multi-line chat messages
- Persist active client list to disk
- Add colored terminal UI

---

## ✔ Summary
This submission meets all rubric items:

- Client registration ✔
- Server updates active list ✔
- Client-inactive detection ✔
- Connection establishment ✔
- Accept/reject ✔
- Reliable messages with SEQ ✔
- Duplicate suppression ✔
- Termination ✔
- Leave application ✔
- Bonus: multiple active chats ✔

To run the system, start the server first by running `Server.main()` which opens UDP port 7070 and waits for registrations. Then start each client separately by running `Client.main()`. Every client automatically creates two sockets—one for communicating with the server and one dedicated listener for chat requests—and sends its listener port to the server. Once clients appear in the server’s active list, you can use commands such as `connect <id>`, `msg <id> <text>`, `terminate <id>`, `chats`, `list`, and `leave`. All interactions print directly to the console for easy testing. No additional configuration is required.