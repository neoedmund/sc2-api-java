A ready to use, jar based, library for sc2-protobuf.
==============
Why? protobuf is hard to compiled by common users.(protobuf is so stupid in software engineering)
This repo do the dirty job.


For users, only need to includes jars in `dist/`



Q:Where are the src/ come from?
A: 
```
protoc-3.4.0-windows-x86_64.exe --java_out=src    google\protobuf\*.proto  (from google/protobuf 3.4)
protoc-3.4.0-windows-x86_64.exe --java_out=src    s2clientprotocol\*.proto (from blizard/sc2-protobuf-java)
```
