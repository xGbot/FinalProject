import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.Line.Info;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Properties;

public class MusicProducer {
    public static void main(String[] args) {
        // Kafka producer configuration
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, MusicSerializer.class.getName()); // Replace with your actual serializer

        // Create Kafka producer
        try (Producer<String, MusicData> producer = new KafkaProducer<>(properties)) {
            // Produce music stream to the topic
            String topic = "song-streams";

            // Folder containing the MP3 files
            String folderPath = "/Songs";

            // Assuming songs are stored in a folder
            File songsFolder = new File("/Songs");
            File[] songFiles = songsFolder.listFiles();

            if (songFiles != null) {
                for (File songFile : songFiles) {
                    String songName = songFile.getName().replaceFirst("[.][^.]+$", "");
                    MusicData musicData = new MusicData(songName, "Artist Name", songFile.getAbsolutePath());

                    ProducerRecord<String, MusicData> record = new ProducerRecord<>(topic, musicData);
                    producer.send(record);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
