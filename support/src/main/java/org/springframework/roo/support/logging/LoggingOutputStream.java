package org.springframework.roo.support.logging;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;

/**
 * Wraps an {@link OutputStream} and automatically passes each line to the
 * {@link Logger} when {@link OutputStream#flush()} or
 * {@link OutputStream#close()} is called.
 * 
 * @author Ben Alex
 * @since 1.1
 */
public class LoggingOutputStream extends OutputStream {

    protected static final Logger LOGGER = HandlerUtils
            .getLogger(LoggingOutputStream.class);

    private ByteArrayOutputStream baos = new ByteArrayOutputStream();
    private int count;
    private final Level level;
    private String sourceClassName = LoggingOutputStream.class.getName();

    /**
     * Constructor
     * 
     * @param level the level at which to log (required)
     */
    public LoggingOutputStream(final Level level) {
        Validate.notNull(level, "A logging level is required");
        this.level = level;
    }

    @Override
    public void close() throws IOException {
        flush();
    }

    @Override
    public void flush() throws IOException {
        if (count > 0) {
            final String msg = new String(baos.toByteArray());
            final LogRecord record = new LogRecord(level, msg);
            record.setSourceClassName(sourceClassName);
            try {
                LOGGER.log(record);
            }
            finally {
                count = 0;
                IOUtils.closeQuietly(baos);
                baos = new ByteArrayOutputStream();
            }
        }
    }

    public String getSourceClassName() {
        return sourceClassName;
    }

    public void setSourceClassName(final String sourceClassName) {
        this.sourceClassName = sourceClassName;
    }

    @Override
    public void write(final int b) throws IOException {
        baos.write(b);
        count++;
    }
}
