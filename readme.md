#sc2 ai api java

this repo is a demo showing how to use s2client to develop AI bots in Java.
It implements bot vs pc, bot viewing replay. but bot vs bot is not yet.

There are 3 parts here:
* sc2-protobuf-java
	to deal with [s2client-proto] (https://github.com/Blizzard/s2client-proto)
	This is stable, need change until s2client-proto changes.
	
* sc2-link
	to start a game, bot vs PC or play a replay, using Websocket.
* neoebot
	Some bot logic can write here, currently no any smart AI here.
	
If you are going to use java to play sc2 AI, you can make use of this.

There is no dependency hell, maven hell. Just simple jar to be included into your project. 
Jars are pre-built in `dist/`


