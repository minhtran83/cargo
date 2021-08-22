/*
 * ========================================================================
 *
 * Copyright 2003-2004 The Apache Software Foundation. Code from this file
 * was originally imported from the Jakarta Cactus project.
 *
 * Codehaus CARGO, copyright 2004-2011 Vincent Massol, 2012-2021 Ali Tokmen.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * ========================================================================
 */
package org.codehaus.cargo.container.internal.http;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

/**
 * HTTP request which posts a form represented as a child of {@link FormContentType}.
 */
public class HttpFormContentTypeRequest extends HttpRequest
{

    /**
     * Form data to be sent by HTTP form request.
     */
    private FormContentType formData;

    /**
     * @param url URL to be called.
     * @param formData Form data to be sent by HTTP form request.
     * @see HttpRequest#HttpRequest(URL)
     */
    public HttpFormContentTypeRequest(URL url, FormContentType formData)
    {
        super(url);
        this.formData = formData;
    }

    /**
     * @param url URL to be called.
     * @param formData Form data to be sent by HTTP form request.
     * @param timeout Request timeout.
     * @see HttpRequest#HttpRequest(URL, long)
     */
    public HttpFormContentTypeRequest(URL url, FormContentType formData, long timeout)
    {
        super(url, timeout);
        this.formData = formData;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeOutputStream(HttpURLConnection connection) throws IOException
    {
        connection.setRequestProperty("Content-Type", formData.getContentType());
        connection.setDoOutput(true);

        if (formData instanceof MultipartFormContentType)
        {
            MultipartFormContentType multipartFormData = (MultipartFormContentType) formData;
            connection.setChunkedStreamingMode(BUFFER_CHUNK_SIZE);

            try (MultipartFormWriter writer =
                new MultipartFormWriter(multipartFormData, connection.getOutputStream()))
            {
                for (Map.Entry<String, String> entry
                    : multipartFormData.getFormContents().entrySet())
                {
                    writer.writeField(entry.getKey(), entry.getValue());
                }

                for (Map.Entry<String, File> entry : multipartFormData.getFormFiles().entrySet())
                {
                    try (InputStream fileInputStream = new FileInputStream(entry.getValue()))
                    {
                        writer.writeFile(entry.getKey(), "application/octet-stream",
                            entry.getValue().getName(), fileInputStream);
                    }
                }
            }
        }
        else if (formData instanceof UrlEncodedFormContentType)
        {
            UrlEncodedFormWriter urlEncodedFormWriter = new UrlEncodedFormWriter();

            for (Map.Entry<String, String> entry : formData.getFormContents().entrySet())
            {
                urlEncodedFormWriter.addField(entry.getKey(), entry.getValue());
            }

            connection.setRequestProperty("Content-Length",
                String.valueOf(urlEncodedFormWriter.getLength()));
            urlEncodedFormWriter.write(connection.getOutputStream());
        }
    }
}
