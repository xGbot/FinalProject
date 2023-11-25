package org.example;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Properties;

public class MusicProducer {
    public static void main(String[] args) {

        // Kafka producer configuration
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, MusicSerializer.class.getName());
        // Create Kafka producer
        try (Producer<String, MusicData> producer = new KafkaProducer<>(properties)) {
            // Produce music stream to the topic
            String topic = "song-streams";

            // Assuming songs are stored in a folder
            File songsFolder = new File("Songs");
            File[] songFiles = songsFolder.listFiles();

            if (songFiles != null) {

                for (File songFile : songFiles) {
                    String songName = songFile.getName().replaceFirst("[.][^.]+$", "");
                    MusicData musicData = new MusicData(songName, "Artist Name", songFile.getPath());

                    ProducerRecord<String, MusicData> record = new ProducerRecord<>(topic, musicData);
                    producer.send(record);
                    System.out.println(record);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
