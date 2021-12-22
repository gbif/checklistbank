package org.gbif.checklistbank.service.mybatis.postgres;

import org.gbif.utils.file.FileUtils;
import org.gbif.utils.file.ResourcesUtil;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.Properties;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import org.apache.commons.io.FilenameUtils;
import org.postgresql.PGConnection;
import org.postgresql.copy.CopyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility that loads pg_dump generated tables dumps into the database using the native postgres jdbc copy command.
 * Expects for each table a tab separated CSV file with the suffix ".tsv". Use \N for null values.
 *
 * It uses a single transaction so foreign key contrainst are only evaluated at the very end.
 * Therefore be careful with very large datasets.
 *
 * This is a simple alternative to the full dbSetup framework which cannot deal with postgres enumerations with recent postgres jdbc drivers:
 * http://www.postgresql.org/message-id/CADK3HHJNjRxzqdOgo3w8S9ZcZ8TSGORvawBTYt3OUa_OneHz5A@mail.gmail.com
 */
public class DbLoader {
    private static final Logger LOG = LoggerFactory.getLogger(DbLoader.class);

    private static final String PREFIX = "checklistbank.db.dataSource.";
    private static final String FILE_SUFFIX = ".tsv";
    private static final Joiner HEADER_JOINER = Joiner.on(",");

    /**
     *
     * @param con open postgres! connection
     * @param folder the classpath folder to scan for table data files
     * @param truncate if true first truncates the tables
     * @throws Exception
     */
    public static void load(Connection con, String folder, boolean truncate) throws Exception {
        con.setAutoCommit(false);
        LOG.info("Load data from " + folder);
        CopyManager copy = con.unwrap(PGConnection.class).getCopyAPI();
        List<String> tables = listTables(folder);
        if (truncate) {
            truncate(con, tables);
        }
        con.commit();

        for (String table : tables) {
            LOG.debug("Load table " + table);
            InputStreamWithoutHeader in = new InputStreamWithoutHeader(FileUtils.classpathStream(folder + "/" + table + FILE_SUFFIX), '\t', '\n');
            String header = HEADER_JOINER.join(in.header);
            copy.copyIn("COPY " + table + "(" + header + ") FROM STDOUT WITH NULL '\\N'", in);
        }
        con.commit();
    }

    private static List<String> listTables(String folder) throws Exception {
        List<String> tables = Lists.newArrayList();
        for (String res : ResourcesUtil.list(DbLoader.class, folder)) {
            tables.add(FilenameUtils.removeExtension(res));
        }
        return tables;
    }

    public static void truncate(Connection con, String folder) throws Exception {
        LOG.debug("Truncate tables");
        for (String table : listTables(folder)) {
            try (java.sql.Statement st = con.createStatement()) {
                st.execute("TRUNCATE " + table + " CASCADE");
            }
        }
    }

    private static void truncate(Connection con, List<String> tables) throws Exception {
        LOG.debug("Truncate tables");
        for (String table : tables) {
            if (!Strings.isNullOrEmpty(table)) {
                try (java.sql.Statement st = con.createStatement()) {
                    st.execute("TRUNCATE " + table + " CASCADE");
                }
            }
        }
    }

    static class InputStreamWithoutHeader extends InputStream {

        private final char delimiter;
        private final char lineend;
        private final InputStream stream;
        private final List<String> header = Lists.newArrayList();

        public InputStreamWithoutHeader(InputStream stream, char delimiter, char lineEnding) {
            this.delimiter = delimiter;
            this.lineend = lineEnding;
            this.stream = stream;
            readHeader();
        }

        private void readHeader() {
            try {
                int x = stream.read();
                StringBuffer sb = new StringBuffer();
                while (x >= 0) {
                    char c = (char) x;
                    if (c == delimiter) {
                        header.add(sb.toString());
                        sb = new StringBuffer();
                    } else if (c == lineend) {
                        header.add(sb.toString());
                        break;
                    } else {
                        sb.append(c);
                    }
                    x = stream.read();
                }
            } catch (IOException e) {
                Throwables.propagate(e);
            }
        }

        @Override
        public int available() throws IOException {
            return stream.available();
        }

        @Override
        public void close() throws IOException {
            stream.close();
        }

        @Override
        public void mark(int readlimit) {
            stream.mark(readlimit);
        }

        @Override
        public boolean markSupported() {
            return stream.markSupported();
        }

        @Override
        public int read() throws IOException {
            return stream.read();
        }

        @Override
        public int read(byte[] b) throws IOException {
            return stream.read(b);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return stream.read(b, off, len);
        }

        @Override
        public void reset() throws IOException {
            stream.reset();
        }

        @Override
        public long skip(long n) throws IOException {
            return stream.skip(n);
        }
    }

}
