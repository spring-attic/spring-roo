package org.springframework.roo.process.manager;

import java.io.InputStream;
import java.io.OutputStream;

import org.springframework.roo.file.undo.UndoManager;

/**
 * Represents a handle to a file (not a directory) that can be legally modified.
 * <p>
 * It is critical that classes using {@link MutableFile} close any streams
 * acquired from the file before further {@link UndoManager} operations. Failure
 * to do so many compromise the ability of {@link UndoManager} to operate
 * correctly.
 * <p>
 * Implementations must guarantee that the file physically exists and is indeed
 * a file. Specifically, {@link MutableFile} implementations should not exist
 * for directories or non-existent files.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public interface MutableFile {

    String getCanonicalPath();

    InputStream getInputStream();

    OutputStream getOutputStream();

    /**
     * Permits presentation of additional information about a change being made
     * via {@link #getOutputStream()}.
     * 
     * @param message the additional information (can be null or empty to clear
     *            any extra information)
     */
    void setDescriptionOfChange(String message);
}
