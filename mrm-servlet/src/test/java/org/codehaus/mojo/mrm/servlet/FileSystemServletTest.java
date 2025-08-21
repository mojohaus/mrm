package org.codehaus.mojo.mrm.servlet;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.input.NullInputStream;
import org.codehaus.mojo.mrm.api.maven.ArtifactStore;
import org.codehaus.mojo.mrm.impl.maven.ArtifactStoreFileSystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileSystemServletTest {

    @Mock
    private ArtifactStore store;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private ServletConfig servletConfig;

    @Mock
    private ServletContext servletContext;

    @Mock
    private ServletOutputStream servletOutputStream;

    private FileSystemServlet servlet;

    @BeforeEach
    void setup() throws Exception {
        when(servletConfig.getServletContext()).thenReturn(servletContext);

        when(request.getPathInfo()).thenReturn("/commons/commons/1.0/commons-1.0.pom");
        when(response.getOutputStream()).thenReturn(servletOutputStream);

        when(store.get(any())).thenReturn(new NullInputStream());

        servlet = new FileSystemServlet(new ArtifactStoreFileSystem(store));
        servlet.init(servletConfig);
    }

    @Test
    void xChecksumHeaderShouldBeAdded() throws Exception {
        when(store.getSha1Checksum(any())).thenReturn("1234567890abcdef1234567890abcdef12345678");

        servlet.doGet(request, response);

        verify(response).addHeader("x-checksum-sha1", "1234567890abcdef1234567890abcdef12345678");
    }

    @Test
    void xChecksumHeaderShouldBeNotAdded() throws Exception {
        when(store.getSha1Checksum(any())).thenReturn(null);

        servlet.doGet(request, response);

        verify(response, never()).addHeader(eq("x-checksum-sha1"), any());
    }
}
