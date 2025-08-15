/*
 * Copyright 2011 Stephen Connolly
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.codehaus.mojo.mrm.servlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.mojo.mrm.api.DefaultDirectoryEntry;
import org.codehaus.mojo.mrm.api.DirectoryEntry;
import org.codehaus.mojo.mrm.api.Entry;
import org.codehaus.mojo.mrm.api.FileEntry;
import org.codehaus.mojo.mrm.api.FileSystem;

/**
 * Servlet that serves a {@link FileSystem}.
 *
 * @since 1.0
 */
public class FileSystemServlet extends HttpServlet {

    /**
     * The file system that we are serving.
     *
     * @since 1.0
     */
    private final FileSystem fileSystem;

    /**
     * Constructor that takes a specific file system instance.
     *
     * @param fileSystem the file systen to serve.
     * @since 1.0
     */
    public FileSystemServlet(FileSystem fileSystem) {
        this.fileSystem = fileSystem;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        if (path == null) {
            path = req.getServletPath();
        }

        Entry entry = fileSystem.get(path);
        if (entry instanceof FileEntry) {
            FileEntry fileEntry = (FileEntry) entry;
            long size = fileEntry.getSize();
            if (size >= 0 && size < Integer.MAX_VALUE) {
                resp.setContentLength((int) size);
            }
            resp.setContentType(getServletContext().getMimeType(fileEntry.getName()));

            LocalDateTime lastModifiedDate = LocalDateTime.ofEpochSecond(
                    fileEntry.getLastModified() / 1000, (int) (fileEntry.getLastModified() % 1000), ZoneOffset.UTC);
            String formattedLastModifiedDate =
                    lastModifiedDate.atZone(ZoneId.of("UTC")).format(DateTimeFormatter.RFC_1123_DATE_TIME);
            resp.addHeader("Last-Modified", formattedLastModifiedDate);

            try (InputStream source = fileEntry.getInputStream()) {
                IOUtils.copy(source, resp.getOutputStream());
            }
            return;
        }

        resp.sendError(HttpURLConnection.HTTP_NOT_FOUND);
    }

    /**
     * {@inheritDoc}
     */
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getPathInfo();
        if (path == null) {
            path = req.getServletPath();
        }
        if (path.endsWith("/")) {
            resp.sendError(HttpURLConnection.HTTP_BAD_METHOD);
            return;
        }
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        String[] parts = path.split("/");
        if (parts.length == 0) {
            resp.sendError(HttpURLConnection.HTTP_BAD_METHOD);
            return;
        }
        String name = parts[parts.length - 1];
        if (StringUtils.isEmpty(name)) {
            resp.sendError(HttpURLConnection.HTTP_BAD_METHOD);
            return;
        }
        DirectoryEntry parent = fileSystem.getRoot();
        for (int i = 0; i < parts.length - 1; i++) {
            parent = new DefaultDirectoryEntry(fileSystem, parent, parts[i]);
        }

        FileEntry put = fileSystem.put(parent, name, req.getInputStream());
        if (put != null) {
            resp.setStatus(HttpURLConnection.HTTP_OK);
            return;
        }

        resp.sendError(HttpURLConnection.HTTP_BAD_METHOD);
    }
}
