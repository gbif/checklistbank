package org.gbif.checklistbank.cli.common;

import org.gbif.checklistbank.service.mybatis.guice.InternalChecklistBankServiceMyBatisModule;
import org.gbif.common.messaging.DefaultMessagePublisher;
import org.gbif.common.messaging.MessageListener;
import org.gbif.common.messaging.api.Message;
import org.gbif.common.messaging.api.MessageCallback;
import org.gbif.common.messaging.api.MessagePublisher;
import org.gbif.common.messaging.config.MessagingConfiguration;

import java.io.IOException;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Injector;
import com.yammer.metrics.MetricRegistry;
import com.yammer.metrics.jvm.FileDescriptorRatioGauge;
import com.yammer.metrics.jvm.MemoryUsageGaugeSet;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class RabbitBaseService<T extends Message> extends AbstractIdleService implements MessageCallback<T> {
    private static final Logger LOG = LoggerFactory.getLogger(RabbitBaseService.class);

    private final MessagingConfiguration mCfg;
    private final GangliaConfiguration gCfg;
    private final int poolSize;
    private final String queue;
    protected final MetricRegistry registry;
    protected HikariDataSource hds;
    protected MessagePublisher publisher;
    protected MessageListener listener;

    public RabbitBaseService(String queue, int poolSize, MessagingConfiguration mCfg, GangliaConfiguration gCfg) {
        this.mCfg = mCfg;
        this.gCfg = gCfg;
        this.poolSize = poolSize;
        this.queue = queue;
        registry = new MetricRegistry(queue);
        registry.registerAll(new MemoryUsageGaugeSet());
        registry.register(Metrics.OPEN_FILES, new FileDescriptorRatioGauge());
    }

    protected String regName(String name) {
        return registry.getName() + "." + name;
    }

    /**
     * Gets the clb DataSource instance from the existing guice injector.
     * Make sure the injector has the ChecklistBankServiceMyBatisModule bound.
     */
    protected void initDbPool(Injector inj) {
        hds = (HikariDataSource) inj.getInstance(InternalChecklistBankServiceMyBatisModule.DATASOURCE_KEY);
    }

    @Override
    protected void startUp() throws Exception {
        gCfg.start(registry);

        publisher = new DefaultMessagePublisher(mCfg.getConnectionParameters());

        // dataset messages are slow, long running processes. Only prefetch one message
        listener = new MessageListener(mCfg.getConnectionParameters(), 1);
        listener.listen(queue, poolSize, this);
    }

    @Override
    protected void shutDown() throws Exception {
        if (hds != null) {
            hds.close();
        }
        if (listener != null) {
            listener.close();
        }
        if (publisher != null) {
            publisher.close();
        }
    }

    protected void send(Message msg) throws IOException {
        try {
            LOG.info("Sending {}", msg.getClass().getSimpleName());
            publisher.send(msg);
        } catch (IOException e) {
            LOG.error("Could not send {}", msg.getClass().getSimpleName(), e);
            throw e;
        }
    }

}
