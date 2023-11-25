import org.apache.kafka.common.serialization.Deserializer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.util.Map;

public class MusicDeserializer implements Deserializer<MusicData> {

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        // No additional configuration needed
    }

    @Override
    public MusicData deserialize(String topic, byte[] data) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
             ObjectInput in = new ObjectInputStream(bis)) {
            
            // Deserialize and return the MusicData object
            return (MusicData) in.readObject();
            
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("Error deserializing MusicData", e);
        }
    }

    @Override
    public void close() {
        // No resources to close
    }
}
