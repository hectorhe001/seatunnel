package org.apache.seatunnel.connectors.seatunnel.bigtable.sink;

import org.apache.seatunnel.shade.com.typesafe.config.Config;

import org.apache.seatunnel.api.common.PrepareFailException;
import org.apache.seatunnel.api.common.SeaTunnelAPIErrorCode;
import org.apache.seatunnel.api.sink.SeaTunnelSink;
import org.apache.seatunnel.api.sink.SinkWriter;
import org.apache.seatunnel.api.table.type.BasicType;
import org.apache.seatunnel.api.table.type.SeaTunnelDataType;
import org.apache.seatunnel.api.table.type.SeaTunnelRow;
import org.apache.seatunnel.api.table.type.SeaTunnelRowType;
import org.apache.seatunnel.api.table.type.SqlType;
import org.apache.seatunnel.common.config.CheckConfigUtil;
import org.apache.seatunnel.common.config.CheckResult;
import org.apache.seatunnel.common.constants.PluginType;
import org.apache.seatunnel.connectors.seatunnel.bigtable.config.BigtableParameters;
import org.apache.seatunnel.connectors.seatunnel.bigtable.exception.BigtableConnectorException;
import org.apache.seatunnel.connectors.seatunnel.common.sink.AbstractSimpleSink;
import org.apache.seatunnel.connectors.seatunnel.common.sink.AbstractSinkWriter;

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableSet;

import java.io.IOException;
import java.util.Set;

import static org.apache.seatunnel.api.table.type.SqlType.BIGINT;
import static org.apache.seatunnel.api.table.type.SqlType.BOOLEAN;
import static org.apache.seatunnel.api.table.type.SqlType.BYTES;
import static org.apache.seatunnel.api.table.type.SqlType.DOUBLE;
import static org.apache.seatunnel.api.table.type.SqlType.FLOAT;
import static org.apache.seatunnel.api.table.type.SqlType.INT;
import static org.apache.seatunnel.api.table.type.SqlType.SMALLINT;
import static org.apache.seatunnel.api.table.type.SqlType.STRING;
import static org.apache.seatunnel.api.table.type.SqlType.TINYINT;
import static org.apache.seatunnel.connectors.seatunnel.bigtable.config.BigtableConfig.COLUMN_MAPPINGS;
import static org.apache.seatunnel.connectors.seatunnel.bigtable.config.BigtableConfig.CONFIG_CENTER_ENVIRONMENT;
import static org.apache.seatunnel.connectors.seatunnel.bigtable.config.BigtableConfig.CONFIG_CENTER_PROJECT;
import static org.apache.seatunnel.connectors.seatunnel.bigtable.config.BigtableConfig.CONFIG_CENTER_TOKEN;
import static org.apache.seatunnel.connectors.seatunnel.bigtable.config.BigtableConfig.CONFIG_CENTER_URL;
import static org.apache.seatunnel.connectors.seatunnel.bigtable.config.BigtableConfig.INSTANCE_ID;
import static org.apache.seatunnel.connectors.seatunnel.bigtable.config.BigtableConfig.KEY_ALIAS;
import static org.apache.seatunnel.connectors.seatunnel.bigtable.config.BigtableConfig.PROJECT_ID;
import static org.apache.seatunnel.connectors.seatunnel.bigtable.config.BigtableConfig.TABLE_ID;

/**
 * @author: gf.xu
 * @email: gf.xu@aftership.com
 * @date: 2023/8/17 14:42
 */
@AutoService(SeaTunnelSink.class)
public class BigtableSink extends AbstractSimpleSink<SeaTunnelRow, Void> {

    private static final Set<SqlType> SUPPORTED_FIELD_TYPES =
            ImmutableSet.of(BOOLEAN, TINYINT, SMALLINT, INT, BIGINT, FLOAT, DOUBLE, STRING, BYTES);

    private SeaTunnelRowType seaTunnelRowType;

    private BigtableParameters bigtableParameters;

    private int rowkeyColumnIndex;

    @Override
    public String getPluginName() {
        return BigtableTableSinkFactory.IDENTIFIER;
    }

    @Override
    public void prepare(Config pluginConfig) throws PrepareFailException {
        // must have
        CheckResult result =
                CheckConfigUtil.checkAllExists(
                        pluginConfig,
                        PROJECT_ID.key(),
                        INSTANCE_ID.key(),
                        TABLE_ID.key(),
                        KEY_ALIAS.key(),
                        COLUMN_MAPPINGS.key(),
                        CONFIG_CENTER_TOKEN.key(),
                        CONFIG_CENTER_URL.key(),
                        CONFIG_CENTER_ENVIRONMENT.key(),
                        CONFIG_CENTER_PROJECT.key());
        if (!result.isSuccess()) {
            throw new BigtableConnectorException(
                    SeaTunnelAPIErrorCode.CONFIG_VALIDATION_FAILED,
                    String.format(
                            "PluginName: %s, PluginType: %s, Message: %s",
                            getPluginName(), PluginType.SINK, result.getMsg()));
        }
        this.bigtableParameters = BigtableParameters.buildWithConfig(pluginConfig);
    }

    @Override
    public void setTypeInfo(SeaTunnelRowType seaTunnelRowType) {
        this.seaTunnelRowType = seaTunnelRowType;
        this.rowkeyColumnIndex = seaTunnelRowType.indexOf(bigtableParameters.getKeyAlias());

        for (int i = 0; i < seaTunnelRowType.getTotalFields(); i++) {
            String fieldName = seaTunnelRowType.getFieldName(i);
            SeaTunnelDataType<?> dataType = seaTunnelRowType.getFieldType(i);
            if (!(dataType instanceof BasicType)
                    || !SUPPORTED_FIELD_TYPES.contains(dataType.getSqlType())) {
                throw new BigtableConnectorException(
                        SeaTunnelAPIErrorCode.CONFIG_VALIDATION_FAILED,
                        String.format(
                                "PluginName: %s, PluginType: %s, Message: %s",
                                getPluginName(),
                                PluginType.SINK,
                                "Unsupported field type: "
                                        + dataType.getSqlType()
                                        + " for field: "
                                        + fieldName));
            }
        }
    }

    @Override
    public SeaTunnelDataType<SeaTunnelRow> getConsumedType() {
        return seaTunnelRowType;
    }

    @Override
    public AbstractSinkWriter<SeaTunnelRow, Void> createWriter(SinkWriter.Context context)
            throws IOException {
        return new BigtableSinkWriter(seaTunnelRowType, bigtableParameters, rowkeyColumnIndex);
    }
}
