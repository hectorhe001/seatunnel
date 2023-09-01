package org.apache.seatunnel.format.avro;

import org.apache.avro.Conversions;
import org.apache.avro.LogicalTypes;
import org.apache.avro.Schema;
import org.apache.avro.data.TimeConversions;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumReader;
import org.apache.seatunnel.api.table.type.*;
import org.apache.seatunnel.format.avro.exception.AvroFormatErrorCode;
import org.apache.seatunnel.format.avro.exception.SeaTunnelAvroFormatException;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

public class AvroToRowConverter implements Serializable {

    private static final long serialVersionUID = 8177020083886379563L;

    private DatumReader<GenericRecord> reader = null;

    public AvroToRowConverter() {}

    public DatumReader<GenericRecord> getReader() {
        if (reader == null) {
            reader = createReader();
        }
        return reader;
    }

    private DatumReader<GenericRecord> createReader() {
        GenericDatumReader<GenericRecord> datumReader = new GenericDatumReader<>();
        datumReader.getData().addLogicalTypeConversion(new Conversions.DecimalConversion());
        datumReader.getData().addLogicalTypeConversion(new TimeConversions.DateConversion());
        datumReader
                .getData()
                .addLogicalTypeConversion(new TimeConversions.TimestampMillisConversion());
        return datumReader;
    }

    public SeaTunnelRow converter(GenericRecord record, SeaTunnelRowType rowType) {
        String[] fieldNames = rowType.getFieldNames();

        Object[] values = new Object[fieldNames.length];
        for (int i = 0; i < fieldNames.length; i++) {
            if (record.getSchema().getField(fieldNames[i]) == null) {
                values[i] = null;
                continue;
            }
            values[i] =
                    convertField(
                            rowType.getFieldType(i),
                            record.getSchema().getField(fieldNames[i]),
                            record.get(fieldNames[i]));
        }
        return new SeaTunnelRow(values);
    }

    private Object convertField(SeaTunnelDataType<?> dataType, Schema.Field field, Object val) {
        switch (dataType.getSqlType()) {
            case MAP:
            case STRING:
            case BOOLEAN:
            case SMALLINT:
            case INT:
            case BIGINT:
            case FLOAT:
            case DOUBLE:
            case NULL:
            case BYTES:
                return val;
            case TINYINT:
                Class<?> typeClass = dataType.getTypeClass();
                if (typeClass == Byte.class) {
                    Integer integer = (Integer) val;
                    return integer.byteValue();
                }
                return val;
            case ARRAY:
                BasicType<?> basicType = (BasicType<?>) ((ArrayType<?, ?>) dataType).getElementType();
                List<Object> list = (List<Object>) val;
                return convertArray(list, basicType);
            case DECIMAL:
                LogicalTypes.Decimal decimal =
                        (LogicalTypes.Decimal) field.schema().getLogicalType();
                ByteBuffer buffer = (ByteBuffer) val;
                byte[] bytes = buffer.array();
                return new BigDecimal(new BigInteger(bytes), decimal.getScale());
            case DATE:
                return LocalDate.ofEpochDay((Long) val);
            case TIMESTAMP:
                return LocalDateTime.ofInstant(
                        Instant.ofEpochMilli((Long) val),
                        ZoneOffset.of(ZoneOffset.systemDefault().getId()));
            case ROW:
                SeaTunnelRowType subRow = (SeaTunnelRowType) dataType;
                return converter((GenericRecord) val, subRow);
            default:
                String errorMsg =
                        String.format(
                                "SeaTunnel avro format is not supported for this data type [%s]",
                                dataType.getSqlType());
                throw new SeaTunnelAvroFormatException(
                        AvroFormatErrorCode.UNSUPPORTED_DATA_TYPE, errorMsg);
        }
    }

    protected static Object convertArray(List<Object> val, SeaTunnelDataType<?> dataType) {
        if (val == null) {
            return null;
        }
        int length = val.size();
        switch (dataType.getSqlType()) {
            case STRING:
                String[] strings = new String[length];
                for (int i = 0; i < strings.length; i++) {
                    strings[i] = val.get(i).toString();
                }
                return strings;
            case BOOLEAN:
                Boolean[] booleans = new Boolean[length];
                for (int i = 0; i < booleans.length; i++) {
                    booleans[i] = (Boolean) val.get(i);
                }
                return booleans;
            case BYTES:
                Byte[] bytes = new Byte[length];
                for (int i = 0; i < bytes.length; i++) {
                    bytes[i] = (Byte) val.get(i);
                }
                return bytes;
            case SMALLINT:
                Short[] shorts = new Short[length];
                for (int i = 0; i < shorts.length; i++) {
                    shorts[i] = (Short) val.get(i);
                }
                return shorts;
            case INT:
                Integer[] integers = new Integer[length];
                for (int i = 0; i < integers.length; i++) {
                    integers[i] = (Integer) val.get(i);
                }
                return integers;
            case BIGINT:
                Long[] longs = new Long[length];
                for (int i = 0; i < longs.length; i++) {
                    longs[i] = (Long) val.get(i);
                }
                return longs;
            case FLOAT:
                Float[] floats = new Float[length];
                for (int i = 0; i < floats.length; i++) {
                    floats[i] = (Float) val.get(i);
                }
                return floats;
            case DOUBLE:
                Double[] doubles = new Double[length];
                for (int i = 0; i < doubles.length; i++) {
                    doubles[i] = (Double) val.get(i);
                }
                return doubles;
            default:
                String errorMsg =
                        String.format(
                                "SeaTunnel avro array format is not supported for this data type [%s]",
                                dataType.getSqlType());
                throw new SeaTunnelAvroFormatException(
                        AvroFormatErrorCode.UNSUPPORTED_DATA_TYPE, errorMsg);
        }
    }
}

