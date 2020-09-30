package cz.to2.noe.load.thread;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by pavelnovotny on 26.09.20.
 */
public class Barrier {
    private int maxParties;
    private volatile int parties;
    private static Logger logger = LoggerFactory.getLogger(Barrier.class);

    public Barrier(int maxParties) {
        this.maxParties = maxParties;
    }

    public void await() {
        try {
            awaitInternal();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void awaitInternal() throws InterruptedException {
        synchronized (this) {
            if (++parties > maxParties) {
                logger.error("Already notified.");
                return;
            }
            if (parties == maxParties) {
                logger.debug("notifyAll()");
                this.notifyAll();
                return;
            }
            logger.debug("await()");
            this.wait();
        }
    }
}
