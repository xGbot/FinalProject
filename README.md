# Distributed Systems - Final Project
Distributed Music Player 

**HOW TO RUN**
1. Intall [Apache Kafka](https://kafka.apache.org/downloads)
2. CD into the Kafka folder and start the zookeeper using: <br>
```.\bin\windows\zookeeper-server-start.bat .\config\zookeeper.properties```

4. Open another command prompt and start the Kafka Server using: <br>
```.\bin\windows\kafka-server-start.bat .\config\server.properties```

5. Start the MusicConsumer in the KafkaServer folder
6. Start the MusicProducer in the KafkaServer folder
7. Launch the android application
