# CPSC 441 – Programming Assignment 1 — Report

## (a) Description, operation, and assumptions

**Goal.** Implement a TCP client–server chat with a bonus *file listing* and *file download* feature.

### High-level design
- **Server**
    - Listens on port `30001`.
    - Spawns a **thread per client** (`ServerSideConnection`).
    - Enforces **max 3 clients**. If full, replies `SERVER_FULL` and closes the socket.
    - Tracks:
        - `nameToSsc: Map<String, ServerSideConnection>` → enforces **unique names**.
        - `clientSessionLogs: Map<String, ClientSession>` → **start/end timestamps** per client.
        - `filesSet: Set<String>` populated from `src/A1/ServerFiles/` at startup.
    - **Protocol (chat)**
        1) Server → `"plz input ur name:"`
        2) Client → `<name>`
        3) Server → `"VALID NAME"` **or** `"name already in use or invalid, try again"` (repeat until valid)
        4) Chat loop:
            - Any text → Server echoes `"ACK: <text>"`
            - `"list"` → Server sends list of repo files and stays in a mini-menu until `"leave"`/`"exit"`
            - `<fileName>` (must exist) → **file transfer** (see below)
            - `"exit"` → Server replies `"exiting server"` and disconnects

- **Client**
    - Connects to `localhost:30001`.
    - If first message is `SERVER_FULL`, prints and exits.
    - Otherwise sends a name until it receives `"VALID NAME"`.
    - CLI loop:
        - Reads a line, sends it.
        - If server replies `"FILE_RECEIVE_MODE"`, switches to file receive.
        - If server replies `"exiting server"`, quits.
        - Otherwise prints the message.

### File transfer (bonus)
- **Framing** (to avoid deadlocks/ambiguity):
    - Server sends control: `"FILE_RECEIVE_MODE"`
    - Server sends: `fileName` (UTF)
    - Server sends: `fileSize` (`long`)
    - Server streams **raw bytes**
- **Client receive:**
    - Reads `fileName`, `fileSize`
    - Saves bytes to: `src/A1/ClientDownloads/<fileName>` with a **32 KB buffer**, stopping exactly after `fileSize` bytes.

### Assumptions / clarifications
- **Paths & working directory.** The program may run from `out/production/...`; to keep grading simple we:
    - Load server files from `src/A1/ServerFiles/`
    - Save client downloads to `src/A1/ClientDownloads/`
- **No persistence.** Session cache is **in-memory** only (per spec).
- **TCP sockets only.** No HTTP; all messages are UTF strings over `DataOutputStream`.

---

## (b) Difficulties and how they were handled

- **Read/write ordering & deadlocks.**
    - Issue: Both sides could block on `readUTF()`.
    - Fix: Introduced strict request/response and a control marker `"FILE_RECEIVE_MODE"` before streaming bytes.

- **Null reads / abrupt disconnects.**
    - Issue: `readUTF()` can fail/return nothing if peer closes.
    - Fix: Guarded for `null` and broke loops so `finally { close(); }` runs.

- **Reconnect/name reuse.**
    - Issue: Name stayed in `nameToSsc` after exit → user couldn’t reuse the name.
    - Fix: Moved all cleanup (remove name + decrement count) into `close()` and ensured it’s called from `finally`.

- **Path confusion (repo not found).**
    - Issue: Relative paths differed under IDE runtime.
    - Fix: Explicitly used `src/A1/ServerFiles` for loading and logged absolute paths while initializing.

- **Slow transfer perception.**
    - Fix: Used **32 KB buffer**, wrote exactly `fileSize` bytes, and printed completion paths.

---

## (c) Possible Improvements (if more time)

- **HTTP pre-check before TCP connection**  
  Add a lightweight HTTP-based request step before establishing the full TCP socket, allowing clients to quickly learn whether the server is at capacity before attempting a full connection.  
  This would improve user feedback and reduce wasted socket handshakes.

- **Hierarchical file navigation (tree format)**  
  Replace the flat file listing with a navigable directory structure, enabling commands like `ls`, `cd`, and `get`.  
  This would let clients browse subdirectories efficiently, especially in larger repositories (50+ files), reducing the time needed to locate specific files.

- **Graphical User Interface (GUI)**  
  Implement a simple GUI version of the client (e.g., using JavaFX or Swing) to make the chat and file retrieval experience more user-friendly compared to the command-line interface.

- **File transfer progress tracking**  
  Display a live progress percentage or progress bar while downloading files, improving transparency and usability for larger files.

---

## Notes for TAs
- All networking uses **TCP sockets**.
- Session logs (start/end) are maintained in-memory per client.
- File transfer uses **size-prefixed framing** (name → size → bytes) to avoid blocking and to support arbitrary file types.