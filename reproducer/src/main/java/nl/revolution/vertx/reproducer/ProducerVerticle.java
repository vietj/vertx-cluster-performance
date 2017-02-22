package nl.revolution.vertx.reproducer;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.eventbus.Message;

import java.util.stream.IntStream;

public class ProducerVerticle extends AbstractVerticle {

    private static final int NUM_MESSAGES = 500_000;

    private long replyCount = 0;
    private long start;

    @Override
    public void start() throws Exception {
        System.out.println("Starting " + this.getClass().getSimpleName());
        start = System.currentTimeMillis();
        System.out.println("Starting to send messages.");
        sendMessage(0, 1000);
    }

    private void sendMessage(int sent, int batchSize) {
        int count = 0;
        while (true) {
            if (sent < NUM_MESSAGES) {
                if (count < batchSize) {
                    vertx.eventBus().send(
                        ConsumerVerticle.ADDRESS,
                        "dummy message",
                        this::onReply);
                    count++;
                    sent++;
                    if (sent % 20000 == 0) {
                        System.out.println(sent + " eventbus messages sent in " + (System.currentTimeMillis() - start) + " ms.");
                    }
                } else {
                    int val = sent;
                    vertx.runOnContext(v -> sendMessage(val, batchSize));
                    break;
                }
            } else {
                System.out.println(NUM_MESSAGES + " eventbus messages sent in " + (System.currentTimeMillis() - start) + " ms.");
                break;
            }
        }
    }

    private void onReply(AsyncResult<Message<Object>> reply) {
        if (reply.result() != null && "OK".equals(reply.result().body())) {
            replyCount++;

            // log progress for the impatient ;-)
            if (replyCount % 10000 == 0) {
                System.out.println("replyCount: " + replyCount + " after " + (System.currentTimeMillis() - start) + " ms");
            }

            // stop when the number of received replies is equal to the number of sent messages
            if (replyCount == NUM_MESSAGES) {
                System.out.println("all replies received in " + (System.currentTimeMillis() - start) + " ms");
                System.exit(0);
            }
        }
    }
}