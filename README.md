**\#\#\#** **UPDATE 2022:** This project is no longer being maintained **\#\#\#**

# Command Line Secure* Messenger
**\*NOTE**: This software does NOT yet (and may never) provide secure messaging, let alone true end-to-end encryption. This software is the product of a misunderstood school assignment. It is designed to authenticate other hosts andencrypt messages using the RSA encryption algorithm. Not only does this software not provide that functionality in its current state, RSA is not suitable for message encryption. This software sends messages in plaintext, meaning anyone can see and read your messages if they want to. Do not use this software for any purpose other than personal curiosity.

## About CLSM
This is a work-in-progress. It's an absolute mess in its current state. That being said, this is currently a partially functional command-line based peer-to-peer chat program.  The .jar file should be up to date, so if you'd like to test it out, you can run it with the command `java -jar CLSM.jar`. It's worth noting that the `!help` command does not actually do anything at the current time. The following list outlines the commands currently available:
* `!connect`: connect to a remote host
* `!message`: send a message to a connected machine
* `!disconnect` or `!disco`: terminate an active chat session
* `!generate`: generate a new RSA public/private key pair
* `!quit`: terminate CLSM

## Using CLSM for the first time
CLSM needs an RSA key pair in order to run. When CLSM starts up, it will automatically look for an RSA key pair inits default directory, `jar/rsa_keys`. It will look for two files in this directory, `key_rsa` and `key_rsa.pub`. These files will contain the local machine's private and public rsa keys, respectively. (These keys are entirely independent of any actual RSA keys stored elsewhere on the computer.) If CLSM hasn't been run on your computer yet, it obviously won't be able to load these key files, as they don't exist yet.  CLSM will then ask you where it can find the RSA key files. Again, since it's the first run, the key files don't exist yet.  Standard key files, such as those generated with the `ssh-keygen` command for example, are not supported at this time. You can simply press `enter` at each of the three file prompts.  CLSM will then notify you that it couldn't find key files at the specified location (the aforementioned default, since you skipped through those prompts) and will ask if you'd like to generate new keys. Type `yes`, and CLSM will generate a new RSA key pair. It will then ask if you'd like to save them. Type `yes` again and then hit `enter` for each prompt to save the keys to their default directories (this is reccommended). CLSM will then tell you whether it was able to save the key files and will begin a standard session.

## Standard use
If CLSM has been run before, it can be assumed that you used it to generate and store key files on your machine.  If youchose to store these key files in the default directory, CLSM will find them on startup and ask if you'd like to load them. If you stored your files elsewhere, you'll have to tell CLSM where they're stored.  After the keys have been loaded, you should be presented with a CLSM prompt. At this point, CLSM is waiting for one of two things to happen: 1. A remote client requests a connection -- this causes CLSM to enter ["server" mode](#server-mode), described below 2. you type a command at the `clsm>` prompt. This command can be one of: `!connect`, `!generate`, and `!quit`. Issuing the `!connect` command will transition CLSM into ["client" mode](#client-mode), described below.

### Server mode
In this case, the background server detected that someone else wanted to start a messaging session with you. CLSM will ask you if you'd like to accept the connection.  Accepting the connection takes CLSM into ["server" mode](#server-mode). It will then ask if you'd like to authenticate the client, the other machine requesting a connection. Should you accept, CLSM will then attempt to authenticate the remote host and will notify you if authentication was successful (***NOTE*** Authentication is not currently supported and attempting to authenticate will crash CLSM).  Once the authentication process is complete, CLSM will transition to ["messaging" mode](#messaging-mode).

### Client mode
When you type `!connect`, CLSM will ask you for an IP address and a port number.  Currently, the listening port for CLSM is `3001`. After entering an IP and a port, CLSM will attempt to connect to the specified host. At this point, your instance of CLSM is running in ["client" mode](#client-mode). The remote host to which you are attempting to connect must also be running CLSM, have their keys loaded, and be waiting at the main CLSM prompt in ["server" mode](#server-mode), described in 1 above. If the host requests authentication, your instance of CLSM will attempt to comply and will notify you if authentication was successful (***NOTE*** Authentication is not currently supported and attempting to authenticate will crash CLSM).  Once the authentication process is complete, CLSM will transition to ["messaging" mode](#messaging-mode).

### Messaging mode
Here, you can use the `!message` command to send a message to the connected host.  Messages are multi-line and support any character combination *except* "!done". `!done` is a keyword reserved to tell CLSM you'd like to send a message. Do note that while messages can contain multiple lines, you cannot edit a line once you have submitted it to the prompt. Unfortunately, it was suggested that I write this software in Java and I made the mistake of taking that suggestion. Once a message is complete, as stated earlier, use `!done` to send it. When you are finished messaging with the other host, use the `!disconnect` command to terminate the connection and return to the main prompt.

### Generate mode
This is essentially the same process as the one mentioned above in [Using CLSM for the first time](#using-clsm-for-the-first-time). CLSM will ask if you're sure you want to generate new keys, then generate new keys and ask if you'd like to save them and, if so, where.

## Current known issues
This section could go on for quite some time, but essentially...
* don't attempt to authenticate incoming client connections yet, as this will cause CLSM to crash.
* the client and server are currently not able to detect if the other disconnects. This means CLSM will NOT notify you if the host you're messaging with disconnects. If the other host disconnects, and you attempt to send a message after it's disconnected, CLSM will likely either spit out an exception stack trace or simply hang.  If you see the former, you're probably okay, as CLSM should continue running, and you can just issue the `!disconnect` command to return to the main prompt. Potentially more to come. I'd like to continue work on this project, but I will not guarantee that it will happen.
