package org.apache.seatunnel.connectors.seatunnel.gcs.sink;

import org.apache.seatunnel.api.configuration.util.OptionRule;
import org.apache.seatunnel.api.table.factory.Factory;
import org.apache.seatunnel.api.table.factory.TableSinkFactory;

import com.google.auto.service.AutoService;

import static org.apache.seatunnel.connectors.seatunnel.gcs.config.GcsSinkConfig.FORMAT;
import static org.apache.seatunnel.connectors.seatunnel.gcs.config.GcsSinkConfig.PATH;
import static org.apache.seatunnel.connectors.seatunnel.gcs.config.GcsSinkConfig.PROJECT_ID;
import static org.apache.seatunnel.connectors.seatunnel.gcs.config.GcsSinkConfig.SUFFIX;

@AutoService(Factory.class)
public class GcsSinkFactory implements TableSinkFactory {

    @Override
    public String factoryIdentifier() {
        return "GCS";
    }

    @Override
    public OptionRule optionRule() {
        return OptionRule.builder().required(PROJECT_ID, PATH, FORMAT).optional(SUFFIX).build();
    }
}