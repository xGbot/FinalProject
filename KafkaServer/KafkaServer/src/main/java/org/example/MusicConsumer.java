package org.example;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Collections;
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

        try (KafkaConsumer<String, MusicData> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(Collections.singletonList(KAFKA_TOPIC));

            System.out.println("normal cons " + consumer);

            // Set up HTTP server
            HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
            server.createContext("/request-song", new RequestSongHandler(consumer));
            server.setExecutor(null);
            server.start();
        }
    }


    private static class RequestSongHandler implements HttpHandler {

        private final KafkaConsumer<String, MusicData> kafkaConsumer;

        public RequestSongHandler(KafkaConsumer<String, MusicData> kafkaConsumer) {
            this.kafkaConsumer = kafkaConsumer;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {

            if ("GET".equals(exchange.getRequestMethod())) {
                String req = exchange.getRequestURI().getQuery(); // Extract song ID from query parameter

                String[] parts = req.split("=");
                String songId = parts.length == 2 ? parts[1] : req;
                // Consume the requested song from Kafka
                while(true) {
                    System.out.println("BEFORE REC" + kafkaConsumer);
                    ConsumerRecords<String, MusicData> records = kafkaConsumer.poll(Duration.ofMillis(100));

                    System.out.println("RECORD " + records);

                    // Iterate through the records to find the matching song
                    for (ConsumerRecord<String, MusicData> record : records) {
                        MusicData musicData = record.value();

                        // Assuming songId matches the songName in this example
                        if (musicData.getTitle().equals(songId)) {
                            // Stream the MP3 file to the client
                            System.out.println("STREAMING?");

                            streamMp3File(exchange, musicData.getMusicPath());
                            return; // Exit the loop once the song is found
                        }
                    }

                    // If the song is not found, respond with an appropriate message
                    String notFoundResponse = "Song not found for ID: " + songId;
                    exchange.sendResponseHeaders(404, notFoundResponse.length());
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(notFoundResponse.getBytes());
                    }
                }
            }
        }

        private void streamMp3File(HttpExchange exchange, String filePath) throws IOException {
            // Set response headers for streaming
            exchange.getResponseHeaders().set("Content-Type", "audio/mpeg");
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

        private void sendData(HttpExchange exchange, MusicData data) throws IOException{
            // Set response headers for streaming
            exchange.getResponseHeaders().set("Content-Type", "audio/mpeg");
            exchange.sendResponseHeaders(200, 0);

            try (OutputStream os = exchange.getResponseBody()) {

                os.write(data.getMusicPath().getBytes());
                os.flush();
                os.write(data.getTitle().getBytes());
                os.flush();
            }
        }

    }
}
