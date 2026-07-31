package ao.co.kutatelamama.config;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.identity.IdentityColumnSupportImpl;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;

public class SQLiteDialect extends Dialect {

    public SQLiteDialect() {
        super(DatabaseVersion.make(3, 0));
    }

    @Override
    protected void registerColumnTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
        super.registerColumnTypes(typeContributions, serviceRegistry);
        JdbcTypeRegistry jdbcTypeRegistry = typeContributions.getTypeConfiguration().getJdbcTypeRegistry();
        jdbcTypeRegistry.addDescriptor(SqlTypes.INTEGER, org.hibernate.type.descriptor.jdbc.IntegerJdbcType.INSTANCE);
        jdbcTypeRegistry.addDescriptor(SqlTypes.BIGINT, org.hibernate.type.descriptor.jdbc.BigIntJdbcType.INSTANCE);
        jdbcTypeRegistry.addDescriptor(SqlTypes.VARCHAR, org.hibernate.type.descriptor.jdbc.VarcharJdbcType.INSTANCE);
        jdbcTypeRegistry.addDescriptor(SqlTypes.DATE, org.hibernate.type.descriptor.jdbc.DateJdbcType.INSTANCE);
        jdbcTypeRegistry.addDescriptor(SqlTypes.TIMESTAMP, org.hibernate.type.descriptor.jdbc.TimestampJdbcType.INSTANCE);
        jdbcTypeRegistry.addDescriptor(SqlTypes.BOOLEAN, org.hibernate.type.descriptor.jdbc.BooleanJdbcType.INSTANCE);
    }

    @Override
    public IdentityColumnSupport getIdentityColumnSupport() {
        return new IdentityColumnSupportImpl() {
            @Override
            public boolean supportsIdentityColumns() {
                return true;
            }

            @Override
            public String getIdentitySelectString(String table, String column, int type) {
                return "select last_insert_rowid()";
            }

            @Override
            public String getIdentityColumnString(int type) {
                return "integer primary key autoincrement";
            }
        };
    }

    @Override
    public boolean supportsLimit() {
        return true;
    }

    @Override
    public String getLimitString(String query, boolean hasOffset) {
        return query + (hasOffset ? " limit ? offset ?" : " limit ?");
    }
}
