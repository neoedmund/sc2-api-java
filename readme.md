# sc2 ai API java

this repo is a demo showing how to use s2client to develop AI bots in Java.
It implements bot vs pc, bot viewing replay. but bot vs bot is not yet.

There are 2 parts here:
* `sc2-protobuf-java`
    is a java [s2client-proto] (https://github.com/Blizzard/s2client-proto)
    This is almost stable, need change until s2client-proto changes.

* `neoebot`
    It starts SC2 game instance with bot API enabled, listening on a TCP port.
    Bot use WebSocket and sc2 protobuf to send requests to the game and get responses. 
    with Bot class, you can write AI in plain multithread manner, and not worry about sync problems in underlying IO. 
    MyZergBot demonstrates usage. It now used as a 'tool-assisted human play'. 
    Play zerg, it makes drones and queens, auto queen spawn larva(which should, but not an auto-cast skill).
    I beat VeryHard inner AI of SC2 easily with help of this tool, see the replay `neoebot/20190305224805.SC2Replay`
    
If you are going to use java to play sc2 AI, you can make use of this.

There is no dependency hell, maven hell. Just simple jar to be included in your project.


