# Real-Time Log File Monitoring and Analysis System (Java NIO, HashMap, TreeMap)

Course-end project, CBIT Dept. of IT, A.Y. 2026-27 - Anvesh Mamidi (160125737098), ShivaKumar Sanganamoni (160125737125).

## Build
    javac -d out src/*.java        (JDK 17 or later; tested with OpenJDK 21)

## Run
    java -cp out Main generate app.log 1000000          # synthetic log, fixed seed
    java -cp out Main analyze app.log fast               # io | files | all | fast | channel
    java -cp out Main bench data 250000,500000,1000000,2000000
    java -cp out Main buffers data/app_1000000.log
    java -Xmx64m -cp out Main analyze data/app_2000000.log all   # OutOfMemoryError demo
    java -cp out Main monitor-demo demo                  # WatchService real-time monitor

Timings in results/ were measured on one machine; re-run to get your own numbers.
