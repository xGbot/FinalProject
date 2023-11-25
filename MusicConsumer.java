import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.io.File;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

public class MusicConsumer {
    public static void main(String[] args) {
        // Kafka consumer configuration
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "music-consumer-group");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, MusicDeserializer.class.getName());

        // Create Kafka consumer
        try (Consumer<String, MusicData> consumer = new KafkaConsumer<>(properties)) {
            // Subscribe to the "song-streams" topic
            consumer.subscribe(Collections.singletonList("song-streams"));

            // Poll for messages
            while (true) {
                ConsumerRecords<String, MusicData> records = consumer.poll(Duration.ofMillis(100));

                // Process the received records
                records.forEach(record -> {
                    MusicData musicData = record.value();
                    // Retrieve the file path and stream the music
                    streamMusic(musicData.getAudioDataFilePath());
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void streamMusic(String filePath) {
        try {
            // byte[] audioData = Files.readAllBytes(Path.of(filePath));
            // Implement your logic to stream or play the music using the audioData

            Media hit = new Media(new file(filePath).toURI().toString());
            MediaPlayer mediaPlayer = new MediaPlayer(hit);
            mediaPlayer.play();

            System.out.println("Streaming music from file: " + filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
