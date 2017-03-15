Readme.txt
Johnny Bai 1165291
Lakshmi Manasa Velaga 1220557
Ziyi Yang 0733769

This program is a collaborative text-editor with a chatroom for clients editing a particular file. The server accepts a single parameter to specify the first of three sequential ports to be used by the three server components, chatroom, editor, and file server, respectively. The server's main responsibility to facilitate message passing and synchronization between connected clients. The server is also responsible for assigning client IDs to connecting peers. The client is composed of a simple GUI with three components: a login box, the editor text area, and a chat section. For text editing, the client handles the operational transformation logic needed to maintain document consistency. 

Usage:
Running the server:
1. Ensure a directory named "root" is in the same directory where you are running the server.
2. To run the server: java -cp DistributedTools.jar server.MainServer [port]
3. If a port is specified, the server will use three ports (port, port+1, port+2). If a port is not specified, the server will use (9000, 9001, 9002).
Note: the specified port, or 9000 by default, will be used by the client to connect to the server.
Running the client: 
1. To run the client: java -cp DistributedTools.jar main.MainApp
2. Specify server information in the format: HOSTADDRESS:PORT
3. Specify the file location on the server to edit: this path will be relative to the server root (e.g. "path/to/file.txt" will load up the file at "root/path/to/file.txt" on the server)
4. Specify an alias to use for the chatroom. If it is left blank, the alias COLLAB followed by client ID will be used.

Features
1. Saving: CMD+S or File>Save
2. Close: CMD+Q or File>Close
3. Editor supports copy and paste, but not word or line deletions
4. Chatting supports text only.
