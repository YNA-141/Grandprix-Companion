package com.gpcompanion.core;

import java.io.File;
import java.io.IOException;

/**
 * Contract for any component that can serialise its internal data to a CSV file.
 */
public interface Exportable {

    /**
     * Writes the component's data to the supplied file in CSV format.
     *
     * @param destination the destination file; will be created if absent, overwritten
     *             if present.  Must not be {@code null}.
     * @throws IOException if any I/O error prevents writing.
     */
    void exportToCSV(File destination) throws IOException;
}
