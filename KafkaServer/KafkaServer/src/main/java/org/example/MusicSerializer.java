package org.example;
import org.apache.kafka.common.serialization.Serializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Map;

public class MusicSerializer implements Serializer<MusicData> {

    @Override
    public byte[] serialize(String topic, MusicData data) {
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
             ObjectOutputStream objectStream = new ObjectOutputStream(byteStream)) {

            // Serialize and return
            objectStream.writeObject(data);
            return byteStream.toByteArray();

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void close() {}

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {}
}
