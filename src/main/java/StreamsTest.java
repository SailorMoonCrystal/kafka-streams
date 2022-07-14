import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.*;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

public class StreamsTest {

    public static void main(String[] args) {
        final Serde<String> stringSerde = Serdes.String();
        final Serde<Long> longSerde = Serdes.Long();

        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.INFO);
        final StreamsBuilder builder = new StreamsBuilder();

        KStream<String, String> views = builder.stream(
                "wordcount-input",
                Consumed.with(stringSerde, stringSerde)
        );

        KTable<String, Long> totalViews = views
                .mapValues(v -> Long.parseLong(v))
                .groupByKey(Grouped.with(stringSerde, longSerde))
                .reduce(Long::sum);

        totalViews.toStream().to("wordcount-output", Produced.with(stringSerde, longSerde));

        final Properties props = new Properties();
        props.putIfAbsent(StreamsConfig.APPLICATION_ID_CONFIG, "streams-totalviews");
        props.putIfAbsent(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");

        final KafkaStreams streams = new KafkaStreams(builder.build(), props);

        final CountDownLatch latch = new CountDownLatch(1);

        try {
            streams.start();
            latch.await();
        } catch (final Throwable e) {
            System.exit(1);
        }

        Runtime.getRuntime().addShutdownHook(new Thread("streams-totalviews") {
            @Override
            public void run() {
                streams.close();
                latch.countDown();
            }
        });

        System.exit(0);
    }





}
