package org.example;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class MusicConsumer {

    private static final String KAFKA_TOPIC = "song-streams";
    private static final String GROUP_ID = "song-consumers-group";


    public static void main(String[] args) throws IOException {
        // Set up Kafka consumer
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, MusicDeserializer.class.getName());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, GROUP_ID);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        List<MusicData> musicList = Collections.synchronizedList(new ArrayList<>());

        // Start Kafka consumer thread
        new Thread(() -> {
            try (Consumer<String, MusicData> consumer = new KafkaConsumer<>(props)) {
                consumer.subscribe(Collections.singletonList(KAFKA_TOPIC));

                while (true) {
                    ConsumerRecords<String, MusicData> records = consumer.poll(Duration.ofMillis(100));
                    records.forEach(record -> {
                        MusicData musicData = record.value();
                        musicList.add(musicData);
                    });
                }
            }
        }).start();

        // Set up HTTP server
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/request-song", new RequestSongHandler(musicList));
        server.setExecutor(null);
        server.start();

    }

    private static class RequestSongHandler implements HttpHandler {

        private final List<MusicData> musicList;

        public RequestSongHandler(List<MusicData> musicList) {
            this.musicList = musicList;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {

            if ("GET".equals(exchange.getRequestMethod())) {
                String req = exchange.getRequestURI().getQuery(); // Extract song ID from query parameter

                String[] parts = req.split("=");
                String songId = parts.length == 2 ? parts[1] : req;

                System.out.println(musicList.size());

                while(true) {
                    // Iterate through the records to find the matching song
                    for (MusicData music : musicList) {

                        // Assuming songId matches the songName in this example
                        if (music.getTitle().equals(songId)) {


                            // Stream the MP3 file to the client
                            streamMp3File(exchange, music.getMusicPath());
                            return; // Exit the loop once the song is found
                        }
                    }

                    // If the song is not found, respond with an appropriate message
                    String notFoundResponse = "Song not found for ID: " + songId;
                    System.out.println(notFoundResponse);

                    exchange.sendResponseHeaders(404, notFoundResponse.length());
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(notFoundResponse.getBytes());
                    }

                }
            }
        }

        private void streamMp3File(HttpExchange exchange, String filePath) throws IOException {
            // Set response headers for streaming
            exchange.getResponseHeaders().set("Content-Type", "audio/mp3");
            exchange.sendResponseHeaders(200, 0);

            try (OutputStream os = exchange.getResponseBody();
                 FileInputStream fis = new FileInputStream(filePath)) {

                byte[] buffer = new byte[4096];
                int bytesRead;

                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            }
        }

    }
}
