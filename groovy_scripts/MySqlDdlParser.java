/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
 * 
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.mysql;

import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import io.debezium.annotation.NotThreadSafe;
import io.debezium.relational.Column;
import io.debezium.relational.ColumnEditor;
import io.debezium.relational.TableEditor;
import io.debezium.relational.TableId;
import io.debezium.relational.ddl.DataType;
import io.debezium.relational.ddl.DataTypeParser;
import io.debezium.relational.ddl.DdlParser;
import io.debezium.relational.ddl.DdlTokenizer;
import io.debezium.text.ParsingException;
import io.debezium.text.TokenStream;
import io.debezium.text.TokenStream.Marker;

/**
 * A parser for DDL statements.
 * <p>
 * See the <a href="http://dev.mysql.com/doc/refman/5.7/en/sql-syntax-data-definition.html">MySQL SQL Syntax documentation</a> for
 * the grammar supported by this parser.
 * 
 * @author Randall Hauch
 */
@NotThreadSafe
public class MySqlDdlParser extends DdlParser {

    /**
     * Create a new DDL parser for SQL-2003.
     */
    public MySqlDdlParser() {
        super(";");
    }

    @Override
    protected void initializeDataTypes(DataTypeParser dataTypes) {
        dataTypes.register(Types.BIT, "BIT[(L)]");
        dataTypes.register(Types.INTEGER, "TINYINT[(L)] [UNSIGNED] [ZEROFILL]");
        dataTypes.register(Types.INTEGER, "SMALLINT[(L)] [UNSIGNED] [ZEROFILL]");
        dataTypes.register(Types.INTEGER, "MEDIUMINT[(L)] [UNSIGNED] [ZEROFILL]");
        dataTypes.register(Types.INTEGER, "INT[(L)] [UNSIGNED] [ZEROFILL]");
        dataTypes.register(Types.INTEGER, "INTEGER[(L)] [UNSIGNED] [ZEROFILL]");
        dataTypes.register(Types.BIGINT, "BIGINT[(L)] [UNSIGNED] [ZEROFILL]");
        dataTypes.register(Types.REAL, "REAL[(M[,D])] [UNSIGNED] [ZEROFILL]");
        dataTypes.register(Types.DOUBLE, "DOUBLE[(M[,D])] [UNSIGNED] [ZEROFILL]");
        dataTypes.register(Types.FLOAT, "FLOAT[(M[,D])] [UNSIGNED] [ZEROFILL]");
        dataTypes.register(Types.DECIMAL, "DECIMAL[(M[,D])] [UNSIGNED] [ZEROFILL]");
        dataTypes.register(Types.NUMERIC, "NUMERIC[(M[,D])] [UNSIGNED] [ZEROFILL]");
        dataTypes.register(Types.DATE, "DATE");
        dataTypes.register(Types.TIME, "TIME[(L)]");
        dataTypes.register(Types.TIMESTAMP, "TIMESTAMP[(L)]");
        dataTypes.register(Types.TIMESTAMP, "DATETIME[(L)]");
        dataTypes.register(Types.DATE, "YEAR[(2|4)]");
        dataTypes.register(Types.BLOB, "CHAR[(L)] BINARY [CHARACTER SET charset_name] [COLLATE collation_name]");
        dataTypes.register(Types.BLOB, "VARCHAR(L) BINARY [CHARACTER SET charset_name] [COLLATE collation_name]");
        dataTypes.register(Types.VARCHAR, "CHAR[(L)] [CHARACTER SET charset_name] [COLLATE collation_name]");
        dataTypes.register(Types.VARCHAR, "VARCHAR(L) [CHARACTER SET charset_name] [COLLATE collation_name]");
        dataTypes.register(Types.CHAR, "BINARY[(L)]");
        dataTypes.register(Types.VARBINARY, "VARBINARY(L)");
        dataTypes.register(Types.BLOB, "TINYBLOB");
        dataTypes.register(Types.BLOB, "BLOB");
        dataTypes.register(Types.BLOB, "MEDIUMBLOB");
        dataTypes.register(Types.BLOB, "LONGBLOB");
        dataTypes.register(Types.BLOB, "TINYTEXT BINARY [CHARACTER SET charset_name] [COLLATE collation_name]");
        dataTypes.register(Types.BLOB, "TEXT BINARY [CHARACTER SET charset_name] [COLLATE collation_name]");
        dataTypes.register(Types.BLOB, "MEDIUMTEXT BINARY [CHARACTER SET charset_name] [COLLATE collation_name]");
        dataTypes.register(Types.BLOB, "LONGTEXT BINARY [CHARACTER SET charset_name] [COLLATE collation_name]");
        dataTypes.register(Types.VARCHAR, "TINYTEXT [CHARACTER SET charset_name] [COLLATE collation_name]");
        dataTypes.register(Types.VARCHAR, "TEXT [CHARACTER SET charset_name] [COLLATE collation_name]");
        dataTypes.register(Types.VARCHAR, "MEDIUMTEXT [CHARACTER SET charset_name] [COLLATE collation_name]");
        dataTypes.register(Types.VARCHAR, "LONGTEXT [CHARACTER SET charset_name] [COLLATE collation_name]");
        dataTypes.register(Types.CHAR, "ENUM(...) [CHARACTER SET charset_name] [COLLATE collation_name]");
        dataTypes.register(Types.CHAR, "SET(...) [CHARACTER SET charset_name] [COLLATE collation_name]");
        dataTypes.register(Types.OTHER, "JSON");
    }

    @Override
    protected void initializeKeywords(TokenSet keywords) {
    }

    @Override
    protected void initializeStatementStarts(TokenSet statementStartTokens) {
        statementStartTokens.add("CREATE", "ALTER", "DROP", "INSERT", "SET", "GRANT", "REVOKE");
    }

    @Override
    protected void parseNextStatement(Marker marker) {
        if (tokens.matches(DdlTokenizer.COMMENT)) {
            parseComment(marker);
        } else if (tokens.matches("CREATE")) {
            parseCreate(marker);
        } else if (tokens.matches("ALTER")) {
            parseAlter(marker);
        } else if (tokens.matches("DROP")) {
            parseDrop(marker);
        } else {
            parseUnknownStatement(marker);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void parseCreate(Marker marker) {
        tokens.consume("CREATE");
        if (tokens.matches("TABLE") || tokens.matches("TEMPORARY", "TABLE")) {
            parseCreateTable(marker);
        } else if (tokens.matches("VIEW")) {
            parseCreateView(marker);
        } else if (tokens.matchesAnyOf("DATABASE", "SCHEMA")) {
            parseCreateUnknown(marker);
        } else if (tokens.matchesAnyOf("EVENT")) {
            parseCreateUnknown(marker);
        } else if (tokens.matchesAnyOf("FUNCTION", "PROCEDURE")) {
            parseCreateUnknown(marker);
        } else if (tokens.matchesAnyOf("UNIQUE", "FULLTEXT", "SPATIAL", "INDEX")) {
            parseCreateUnknown(marker);
        } else if (tokens.matchesAnyOf("SERVER")) {
            parseCreateUnknown(marker);
        } else if (tokens.matchesAnyOf("TABLESPACE")) {
            parseCreateUnknown(marker);
        } else if (tokens.matchesAnyOf("TRIGGER")) {
            parseCreateUnknown(marker);
        } else {
            // It could be several possible things (including more elaborate forms of those matches tried above),
            sequentially(this::parseCreateView,
                         this::parseCreateUnknown);
        }
    }

    protected void parseCreateTable(Marker start) {
        tokens.canConsume("TEMPORARY");
        tokens.consume("TABLE");
        boolean onlyIfNotExists = tokens.canConsume("IF", "NOT", "EXISTS");
        TableId tableId = parseQualifiedTableName(start);
        if (onlyIfNotExists && databaseTables.forTable(tableId) != null) {
            // The table does exist, so we should do nothing ...
            consumeRemainingStatement(start);
            debugParsed(start);
            return;
        }
        TableEditor table = databaseTables.editOrCreateTable(tableId);

        // create_definition ...
        if (tokens.matches('(')) parseCreateDefinitionList(start, table);
        // table_options ...
        parseTableOptions(start, table);
        // partition_options ...
        if (tokens.matches("PARTITION")) {
            parsePartitionOptions(start, table);
        }
        // select_statement
        if (tokens.canConsume("AS") || tokens.canConsume("IGNORE", "AS") || tokens.canConsume("REPLACE", "AS")) {
            parseAsSelectStatement(start, table);
        }

        // Update the table definition ...
        databaseTables.overwriteTable(table.create());
        debugParsed(start);
    }

    protected void parseTableOptions(Marker start, TableEditor table) {
        while (parseTableOption(start, table)) {
        }
    }

    protected boolean parseTableOption(Marker start, TableEditor table) {
        if (tokens.canConsume("AUTO_INCREMENT")) {
            // Sets the auto-incremented value for the next incremented value ...
            tokens.consume('=');
            tokens.consume();
            return true;
        } else if (tokens.canConsumeAnyOf("CHECKSUM", "ENGINE", "AVG_ROW_LENGTH", "MAX_ROWS", "MIN_ROWS", "ROW_FORMAT",
                                          "DELAY_KEY_WRITE", "INSERT_METHOD", "KEY_BLOCK_SIZE", "PACK_KEYS",
                                          "STATS_AUTO_RECALC", "STATS_PERSISTENT", "STATS_SAMPLE_PAGES")) {
            // One option token followed by '=' by a single value
            tokens.consume('=');
            tokens.consume();
            return true;
        } else if (tokens.canConsume("DEFAULT", "CHARACTER", "SET") || tokens.canConsume("CHARACTER", "SET")) {
            tokens.consume('=');
            tokens.consume();
            return true;
        } else if (tokens.canConsume("DEFAULT", "COLLATE") || tokens.canConsume("COLLATE")) {
            tokens.consume('=');
            tokens.consume();
            return true;
        } else if (tokens.canConsumeAnyOf("COMMENT", "COMPRESSION", "CONNECTION", "ENCRYPTION", "PASSWORD")) {
            tokens.consume('=');
            consumeQuotedString();
            return true;
        } else if (tokens.canConsume("DATA", "DIRECTORY") || tokens.canConsume("INDEX", "DIRECTORY")) {
            tokens.consume('=');
            consumeQuotedString();
            return true;
        } else if (tokens.canConsume("TABLESPACE")) {
            tokens.consume();
            return true;
        } else if (tokens.canConsume("UNION")) {
            tokens.consume('=');
            tokens.consume();
            while (tokens.canConsume(',')) {
                tokens.consume();
            }
            return true;
        }
        return false;
    }

    protected void parsePartitionOptions(Marker start, TableEditor table) {
        tokens.consume("PARTITION", "BY");
        if (tokens.canConsume("LINEAR", "HASH") || tokens.canConsume("HASH")) {
            consumeExpression(start);
        } else if (tokens.canConsume("LINEAR", "KEY") || tokens.canConsume("KEY")) {
            if (tokens.canConsume("ALGORITHM")) {
                tokens.consume("=");
                tokens.consumeAnyOf("1", "2");
            }
            parseColumnNameList(start);
        } else if (tokens.canConsumeAnyOf("RANGE", "LIST")) {
            if (tokens.canConsume("COLUMNS")) {
                parseColumnNameList(start);
            } else {
                consumeExpression(start);
            }
        }

        if (tokens.canConsume("PARTITIONS")) {
            tokens.consume();
        }
        if (tokens.canConsume("SUBPARTITION", "BY")) {
            if (tokens.canConsume("LINEAR", "HASH") || tokens.canConsume("HASH")) {
                consumeExpression(start);
            } else if (tokens.canConsume("LINEAR", "KEY") || tokens.canConsume("KEY")) {
                if (tokens.canConsume("ALGORITHM")) {
                    tokens.consume("=");
                    tokens.consumeAnyOf("1", "2");
                }
                parseColumnNameList(start);
            }
            if (tokens.canConsume("SUBPARTITIONS")) {
                tokens.consume();
            }
        }
        if (tokens.canConsume('(')) {
            do {
                parsePartitionDefinition(start, table);
            } while (tokens.canConsume(','));
            tokens.consume(')');
        }
    }

    protected void parsePartitionDefinition(Marker start, TableEditor table) {
        tokens.consume("PARTITION");
        tokens.consume(); // name
        if (tokens.canConsume("VALUES")) {
            if (tokens.canConsume("LESS", "THAN")) {
                if (!tokens.canConsume("MAXVALUE")) {
                    consumeExpression(start);
                }
            } else {
                tokens.consume("IN");
                consumeValueList(start);
            }
        } else if (tokens.canConsume("STORAGE", "ENGINE") || tokens.canConsume("ENGINE")) {
            tokens.consume('=');
            tokens.consume();
        } else if (tokens.canConsumeAnyOf("COMMENT")) {
            tokens.consume('=');
            consumeQuotedString();
        } else if (tokens.canConsumeAnyOf("DATA", "INDEX") && tokens.canConsume("DIRECTORY")) {
            tokens.consume('=');
            consumeQuotedString();
        } else if (tokens.canConsumeAnyOf("MAX_ROWS", "MIN_ROWS", "TABLESPACE")) {
            tokens.consume('=');
            tokens.consume();
        } else if (tokens.canConsume('(')) {
            do {
                parseSubpartitionDefinition(start, table);
            } while (tokens.canConsume(','));
            tokens.consume(')');
        }
    }

    protected void parseSubpartitionDefinition(Marker start, TableEditor table) {
        tokens.consume("SUBPARTITION");
        tokens.consume(); // name
        if (tokens.canConsume("STORAGE", "ENGINE") || tokens.canConsume("ENGINE")) {
            tokens.consume('=');
            tokens.consume();
        } else if (tokens.canConsumeAnyOf("COMMENT")) {
            tokens.consume('=');
            consumeQuotedString();
        } else if (tokens.canConsumeAnyOf("DATA", "INDEX") && tokens.canConsume("DIRECTORY")) {
            tokens.consume('=');
            consumeQuotedString();
        } else if (tokens.canConsumeAnyOf("MAX_ROWS", "MIN_ROWS", "TABLESPACE")) {
            tokens.consume('=');
            tokens.consume();
        }
    }

    protected void parseAsSelectStatement(Marker start, TableEditor table) {
        tokens.consume("SELECT");
        consumeRemainingStatement(start);
    }

    protected void parseCreateDefinitionList(Marker start, TableEditor table) {
        tokens.consume('(');
        parseCreateDefinition(start, table);
        while (tokens.canConsume(',')) {
            parseCreateDefinition(start, table);
        }
        tokens.consume(')');
    }

    protected void parseCreateDefinition(Marker start, TableEditor table) {
        // Try to parse the constraints first ...
        if (tokens.canConsume("CHECK")) {
            consumeExpression(start);
        } else if (tokens.canConsume("CONSTRAINT", TokenStream.ANY_VALUE, "PRIMARY", "KEY") || tokens.canConsume("PRIMARY", "KEY")) {
            if (tokens.canConsume("USING")) {
                parseIndexType(start);
            }
            table.setPrimaryKeyNames(parseIndexColumnNames(start));
            parseIndexOptions(start);
        } else if (tokens.canConsume("CONSTRAINT", TokenStream.ANY_VALUE, "UNIQUE") || tokens.canConsume("UNIQUE")) {
            tokens.consumeAnyOf("KEY", "INDEX");
            if (!tokens.matches('(')) {
                if (!tokens.matches("USING")) {
                    tokens.consume(); // name of unique index ...
                }
                if (tokens.matches("USING")) {
                    parseIndexType(start);
                }
            }
            List<String> uniqueKeyColumnNames = parseIndexColumnNames(start);
            if (table.primaryKeyColumnNames().isEmpty()) {
                table.setPrimaryKeyNames(uniqueKeyColumnNames); // this may eventually get overwritten by a real PK
            }
            parseIndexOptions(start);
        } else if (tokens.canConsume("CONSTRAINT", TokenStream.ANY_VALUE, "FOREIGN", "KEY") || tokens.canConsume("FOREIGN", "KEY")) {
            if (!tokens.matches('(')) {
                tokens.consume(); // name of foreign key
            }
            parseIndexColumnNames(start);
            if (tokens.matches("REFERENCES")) {
                parseReferenceDefinition(start);
            }
        } else if (tokens.canConsume("INDEX", "KEY")) {
            if (!tokens.matches('(')) {
                if (!tokens.matches("USING")) {
                    tokens.consume(); // name of unique index ...
                }
                if (tokens.matches("USING")) {
                    parseIndexType(start);
                }
            }
            parseIndexColumnNames(start);
            parseIndexOptions(start);
        } else if (tokens.canConsume("FULLTEXT", "SPATIAL")) {
            tokens.canConsumeAnyOf("INDEX", "KEY");
            if (!tokens.matches('(')) {
                tokens.consume(); // name of unique index ...
            }
            parseIndexColumnNames(start);
            parseIndexOptions(start);
        } else {
            tokens.canConsume("COLUMN"); // optional in ALTER TABLE but never CREATE TABLE

            // Obtain the column editor ...
            String columnName = tokens.consume();
            Column existingColumn = table.columnWithName(columnName);
            ColumnEditor column = existingColumn != null ? existingColumn.edit() : Column.editor().name(columnName);
            AtomicBoolean isPrimaryKey = new AtomicBoolean(false);

            parseColumnDefinition(start, columnName, tokens, table, column, isPrimaryKey);

            // Update the table ...
            Column newColumnDefn = column.create();
            table.addColumns(newColumnDefn);
            if (isPrimaryKey.get()) {
                table.setPrimaryKeyNames(newColumnDefn.name());
            }
        }
    }

    protected void parseColumnDefinition(Marker start, String columnName, TokenStream tokens, TableEditor table, ColumnEditor column,
                                         AtomicBoolean isPrimaryKey) {
        // Parse the data type, which must be at this location ...
        List<ParsingException> errors = new ArrayList<>();
        Marker dataTypeStart = tokens.mark();
        DataType dataType = dataTypeParser.parse(tokens, errors::addAll);
        if (dataType == null) {
            String dataTypeName = parseDomainName(start);
            if (dataTypeName != null) dataType = DataType.userDefinedType(dataTypeName);
        }
        if (dataType == null) {
            // No data type was found
            parsingFailed(dataTypeStart.position(), errors, "Unable to read the data type");
            return;
        }
        column.jdbcType(dataType.jdbcType());
        column.typeName(dataType.name());
        if (dataType.length() > -1) column.length((int) dataType.length());
        if (dataType.scale() > -1) column.scale(dataType.scale());

        if (tokens.canConsume("AS") || tokens.canConsume("GENERATED", "ALWAYS", "AS")) {
            consumeExpression(start);
            tokens.canConsumeAnyOf("VIRTUAL", "STORED");
            if (tokens.canConsume("UNIQUE")) {
                tokens.canConsume("KEY");
            }
            if (tokens.canConsume("COMMENT")) {
                consumeQuotedString();
            }
            tokens.canConsume("NOT", "NULL");
            tokens.canConsume("NULL");
            tokens.canConsume("PRIMARY", "KEY");
            tokens.canConsume("KEY");
        } else {
            while (tokens.matchesAnyOf("NOT", "NULL", "DEFAULT", "AUTO_INCREMENT", "UNIQUE", "PRIMARY", "KEY", "COMMENT",
                                       "REFERENCES", "COLUMN_FORMAT", "ON")) {
                // Nullability ...
                if (tokens.canConsume("NOT", "NULL")) {
                    column.optional(false);
                } else if (tokens.canConsume("NULL")) {
                    column.optional(true);
                }
                // Default value ...
                if (tokens.canConsume("DEFAULT")) {
                    parseDefaultClause(start);
                }
                if (tokens.canConsume("ON")) {
                    if (tokens.canConsumeAnyOf("UPDATE", "DELETE")) {
                        tokens.consume(); // e.g., "ON UPATE CURRENT_TIMESTAMP"
                    }
                    column.autoIncremented(true);
                }
                // Other options ...
                if (tokens.canConsume("AUTO_INCREMENT")) {
                    column.autoIncremented(true);
                    column.generated(true);
                }
                if (tokens.canConsume("UNIQUE", "KEY") || tokens.canConsume("UNIQUE")) {
                    table.setPrimaryKeyNames(column.name());
                }
                if (tokens.canConsume("PRIMARY", "KEY") || tokens.canConsume("KEY")) {
                    table.setPrimaryKeyNames(column.name());
                }
                if (tokens.canConsume("COMMENT")) {
                    consumeQuotedString();
                }
                if (tokens.canConsume("COLUMN_FORMAT")) {
                    tokens.consumeAnyOf("FIXED", "DYNAMIC", "DEFAULT");
                }
                if (tokens.matches("REFERENCES")) {
                    parseReferenceDefinition(start);
                }
            }
        }
    }

    protected String parseDomainName(Marker start) {
        return parseSchemaQualifiedName(start);
    }

    protected List<String> parseIndexColumnNames(Marker start) {
        List<String> names = new ArrayList<>();
        tokens.consume('(');
        parseIndexColumnName(names::add);
        while (tokens.canConsume(',')) {
            parseIndexColumnName(names::add);
        }
        tokens.consume(')');
        return names;
    }

    private void parseIndexColumnName(Consumer<String> name) {
        name.accept(tokens.consume());
        if (tokens.canConsume('(')) {
            tokens.consume(); // length
            tokens.consume(')');
        }
        tokens.canConsumeAnyOf("ASC", "DESC");
    }

    protected void parseIndexType(Marker start) {
        tokens.consume("USING");
        tokens.consumeAnyOf("BTREE", "HASH");
    }

    protected void parseIndexOptions(Marker start) {
        while (true) {
            if (tokens.matches("USING")) {
                parseIndexType(start);
            } else if (tokens.canConsume("COMMENT")) {
                consumeQuotedString();
            } else if (tokens.canConsume("KEY_BLOCK_SIZE")) {
                tokens.consume("=");
                tokens.consume();
            } else if (tokens.canConsume("WITH", "PARSER")) {
                tokens.consume();
            } else {
                break;
            }
        }
    }

    protected void parseReferenceDefinition(Marker start) {
        tokens.consume("REFERENCES");
        parseSchemaQualifiedName(start); // table name
        parseColumnNameList(start);
        if (tokens.canConsume("MATCH")) {
            tokens.consumeAnyOf("FULL", "PARTIAL", "SIMPLE");
            if (tokens.canConsume("ON")) {
                tokens.consumeAnyOf("DELETE", "UPDATE");
                parseReferenceOption(start);
            }
        }
    }

    protected void parseReferenceOption(Marker start) {
        if (tokens.canConsume("RESTRICT")) {
        } else if (tokens.canConsume("CASCADE")) {
        } else if (tokens.canConsume("SET", "NULL")) {
        } else {
            tokens.canConsume("NO", "ACTION");
        }
    }

    protected void parseCreateView(Marker start) {
        tokens.canConsume("OR", "REPLACE");
        if (tokens.canConsume("ALGORITHM")) {
            tokens.consume('=');
            tokens.consumeAnyOf("UNDEFINED", "MERGE", "TEMPTABLE");
        }
        if (tokens.canConsume("DEFINER")) {
            tokens.consume('=');
            tokens.consume(); // user or CURRENT_USER
        }
        if (tokens.canConsume("SQL", "SECURITY")) {
            tokens.consumeAnyOf("DEFINER", "INVOKER");
        }
        tokens.consume("VIEW");
        TableId tableId = parseQualifiedTableName(start);
        TableEditor table = databaseTables.editOrCreateTable(tableId);
        if (tokens.matches('(')) {
            List<String> columnNames = parseColumnNameList(start);
            // We know nothing other than the names ...
            columnNames.forEach(name -> {
                table.addColumn(Column.editor().name(name).create());
            });
        }
        tokens.canConsume("AS");
        // We don't care about the rest ...
        consumeRemainingStatement(start);

        // Update the table definition ...
        databaseTables.overwriteTable(table.create());

        debugParsed(start);
    }

    protected void parseCreateUnknown(Marker start) {
        consumeRemainingStatement(start);
    }

    @Override
    protected void parseAlter(Marker marker) {
        tokens.consume("ALTER");
        if (tokens.matches("TABLE") || tokens.matches("IGNORE", "TABLE")) {
            parseAlterTable(marker);
            debugParsed(marker);
        } else {
            parseAlterUnknown(marker);
        }
    }

    protected void parseAlterTable(Marker start) {
        tokens.canConsume("IGNORE");
        tokens.consume("TABLE");
        TableId tableId = parseQualifiedTableName(start);
        TableEditor table = databaseTables.editOrCreateTable(tableId);
        AtomicReference<TableId> newTableName = new AtomicReference<>(null);
        if (!tokens.matches(terminator()) && !tokens.matches("PARTITION")) {
            parseAlterSpecificationList(start, table, newTableName::set);
        }
        if (tokens.matches("PARTITION")) {
            parsePartitionOptions(start, table);
        }
        databaseTables.overwriteTable(table.create());
        if (newTableName.get() != null) {
            // the table was renamed ...
            databaseTables.renameTable(tableId, newTableName.get());
        }
    }

    protected void parseAlterSpecificationList(Marker start, TableEditor table, Consumer<TableId> newTableName) {
        parseAlterSpecification(start, table, newTableName);
        while (tokens.canConsume(',')) {
            parseAlterSpecification(start, table, newTableName);
        }
        tokens.consume(')');
    }

    protected void parseAlterSpecification(Marker start, TableEditor table, Consumer<TableId> newTableName) {
        parseTableOptions(start, table);
        if (tokens.canConsume("ADD")) {
            if (tokens.matches("COLUMN", "(") || tokens.matches('(')) {
                tokens.canConsume("COLUMN");
                parseCreateDefinitionList(start, table);
            } else if (tokens.canConsume("PARTITION", "(")) {
                parsePartitionDefinition(start, table);
                tokens.consume(')');
            } else {
                parseCreateDefinition(start, table);
            }
        } else if (tokens.canConsume("DROP")) {
            if (tokens.canConsume("PRIMARY", "KEY")) {
                table.setPrimaryKeyNames();
            } else if (tokens.canConsume("FOREIGN", "KEY")) {
                tokens.consume(); // foreign key symbol
            } else if (tokens.canConsumeAnyOf("INDEX", "KEY")) {
                tokens.consume(); // index name
            } else if (tokens.canConsume("PARTITION")) {
                parsePartitionNames(start);
            } else {
                tokens.canConsume("COLUMN");
                String columnName = tokens.consume();
                table.removeColumn(columnName);
            }
        } else if (tokens.canConsume("ALTER")) {
            tokens.canConsume("COLUMN");
            tokens.consume(); // column name
            if (!tokens.canConsume("DROP", "DEFAULT")) {
                tokens.consume("SET", "DEFAULT");
                parseDefaultClause(start);
            }
        } else if (tokens.canConsume("CHANGE")) {
            tokens.canConsume("COLUMN");
            String oldName = tokens.consume();
            String newName = tokens.consume();
            table.renameColumn(oldName, newName);
            parseCreateDefinition(start, table);
            if (tokens.canConsume("FIRST")) {
                table.reorderColumn(newName, null);
            } else if (tokens.canConsume("AFTER")) {
                table.reorderColumn(newName, tokens.consume());
            }
        } else if (tokens.canConsume("MODIFY")) {
            tokens.canConsume("COLUMN");
            String columnName = tokens.consume();
            parseCreateDefinition(start, table);
            if (tokens.canConsume("FIRST")) {
                table.reorderColumn(columnName, null);
            } else if (tokens.canConsume("AFTER")) {
                table.reorderColumn(columnName, tokens.consume());
            }
        } else if (tokens.canConsumeAnyOf("ALGORITHM", "LOCK")) {
            tokens.consume('=').consume();
        } else if (tokens.canConsume("DISABLE", "KEYS") || tokens.canConsume("ENABLE", "KEYS")) {
        } else if (tokens.canConsume("RENAME", "INDEX") || tokens.canConsume("RENAME", "KEY")) {
            tokens.consume(); // old
            tokens.consume("TO");
            tokens.consume(); // new
        } else if (tokens.canConsume("RENAME")) {
            tokens.canConsumeAnyOf("AS", "TO");
            TableId newTableId = parseQualifiedTableName(start);
            newTableName.accept(newTableId);
        } else if (tokens.canConsume("ORDER", "BY")) {
            consumeCommaSeparatedValueList(start); // this should not affect the order of the columns in the table
        } else if (tokens.canConsume("CONVERT", "TO", "CHARACTER", "SET")) {
            tokens.consume(); // charset name
            if (tokens.canConsume("COLLATE")) {
                tokens.consume(); // collation name
            }
        } else if (tokens.canConsume("CHARACTER", "SET") || tokens.canConsume("DEFAULT", "CHARACTER", "SET")) {
            tokens.consume('=');
            tokens.consume(); // charset name
            if (tokens.canConsume("COLLATE")) {
                tokens.consume('=');
                tokens.consume(); // collation name
            }
        } else if (tokens.canConsume("DISCARD", "TABLESPACE") || tokens.canConsume("IMPORT", "TABLESPACE")) {
            // nothing
        } else if (tokens.canConsume("FORCE")) {
            // nothing
        } else if (tokens.canConsume("WITH", "VALIDATION") || tokens.canConsume("WITHOUT", "VALIDATION")) {
            // nothing
        } else if (tokens.canConsume("DISCARD", "PARTITION") || tokens.canConsume("IMPORT", "PARTITION")) {
            if (!tokens.canConsume("ALL")) {
                tokens.consume(); // partition name
            }
            tokens.consume("TABLESPACE");
        } else if (tokens.canConsume("COALLESCE", "PARTITION")) {
            tokens.consume(); // number
        } else if (tokens.canConsume("REORGANIZE", "PARTITION")) {
            parsePartitionNames(start);
            tokens.consume("INTO", "(");
            parsePartitionDefinition(start, table);
            tokens.consume(')');
        } else if (tokens.canConsume("EXCHANGE", "PARTITION")) {
            tokens.consume(); // partition name
            tokens.consume("WITH", "TABLE");
            parseSchemaQualifiedName(start); // table name
            if (tokens.canConsumeAnyOf("WITH", "WITHOUT")) {
                tokens.consume("VALIDATION");
            }
        } else if (tokens.matches(TokenStream.ANY_VALUE, "PARTITION")) {
            tokens.consumeAnyOf("TRUNCATE", "CHECK", "ANALYZE", "OPTIMIZE", "REBUILD", "REPAIR");
            tokens.consume("PARTITION");
            if (!tokens.canConsume("ALL")) {
                parsePartitionNames(start);
            }
        } else if (tokens.canConsume("REMOVE", "PARTITIONING")) {
            // nothing
        } else if (tokens.canConsume("UPGRADE", "PARTITIONING")) {
            // nothing
        }
    }

    protected void parseAlterUnknown(Marker start) {
        consumeRemainingStatement(start);
        debugSkipped(start);
    }

    @Override
    protected void parseDrop(Marker marker) {
        tokens.consume("DROP");
        if (tokens.matches("TABLE") || tokens.matches("TEMPORARY", "TABLE")) {
            parseDropTable(marker);
        } else if (tokens.matches("VIEW")) {
            parseDropView(marker);
        } else {
            parseDropUnknown(marker);
        }
    }

    protected void parseDropTable(Marker start) {
        tokens.canConsume("TEMPORARY");
        tokens.consume("TABLE");
        tokens.canConsume("IF", "EXISTS");
        databaseTables.removeTable(parseQualifiedTableName(start));
        while (tokens.canConsume('.')) {
            databaseTables.removeTable(parseQualifiedTableName(start));
        }
        tokens.canConsumeAnyOf("RESTRICT", "CASCADE");
        debugParsed(start);
    }

    protected void parseDropView(Marker start) {
        tokens.consume("VIEW");
        tokens.canConsume("IF", "EXISTS");
        databaseTables.removeTable(parseQualifiedTableName(start));
        while (tokens.canConsume('.')) {
            databaseTables.removeTable(parseQualifiedTableName(start));
        }
        tokens.canConsumeAnyOf("RESTRICT", "CASCADE");
        debugParsed(start);
    }

    protected void parseDropUnknown(Marker start) {
        consumeRemainingStatement(start);
        debugSkipped(start);
    }

    protected List<String> parseColumnNameList(Marker start) {
        List<String> names = new ArrayList<>();
        tokens.consume('(');
        names.add(tokens.consume());
        while (tokens.canConsume(',')) {
            names.add(tokens.consume());
        }
        tokens.consume(')');
        return names;
    }

    protected void parsePartitionNames(Marker start) {
        consumeCommaSeparatedValueList(start);
    }

    protected void consumeCommaSeparatedValueList(Marker start) {
        tokens.consume();
        while (tokens.canConsume(',')) {
            tokens.consume();
        }
    }

    protected void consumeValueList(Marker start) {
        tokens.consume('(');
        consumeCommaSeparatedValueList(start);
        tokens.consume(')');
    }

    /**
     * Consume an expression surrounded by parentheses.
     * 
     * @param start the start of the statement
     */
    protected void consumeExpression(Marker start) {
        tokens.consume("(");
        tokens.consumeThrough(')', '(');
    }

    /**
     * Try calling the supplied functions in sequence, stopping as soon as one of them succeeds.
     * 
     * @param functions the functions
     */
    @SuppressWarnings("unchecked")
    protected void sequentially(Consumer<Marker>... functions) {
        if (functions == null || functions.length == 0) return;
        Collection<ParsingException> errors = new ArrayList<>();
        Marker marker = tokens.mark();
        for (Consumer<Marker> function : functions) {
            try {
                function.accept(marker);
                return;
            } catch (ParsingException e) {
                errors.add(e);
                tokens.rewind(marker);
            }
        }
        parsingFailed(marker.position(), errors, "Unable to parse statement");
    }

    protected void parseDefaultClause(Marker start) {
        tokens.consume("DEFAULT");
        if (tokens.canConsume("CURRENT_TIMESTAMP")) {
            tokens.canConsume("ON", "UPDATE", "CURRENT_TIMESTAMP");
        } else if (tokens.canConsume("NULL")) {
            // do nothing ...
        } else {
            parseLiteral(start);
            // do nothing ...
        }
    }
}
