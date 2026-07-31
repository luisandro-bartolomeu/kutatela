package ao.co.kutatelamama.config;

/**
 * Custom wrapper extending official Hibernate 6 SQLiteDialect for backwards compatibility.
 */
public class SQLiteDialect extends org.hibernate.community.dialect.SQLiteDialect {

    public SQLiteDialect() {
        super();
    }
}
