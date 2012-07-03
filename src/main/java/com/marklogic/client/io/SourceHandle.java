/*
 * Copyright 2012 MarkLogic Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.marklogic.client.io;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.marklogic.client.Format;
import com.marklogic.client.MarkLogicIOException;
import com.marklogic.client.io.marker.BufferableHandle;
import com.marklogic.client.io.marker.StructureReadHandle;
import com.marklogic.client.io.marker.StructureWriteHandle;
import com.marklogic.client.io.marker.XMLReadHandle;
import com.marklogic.client.io.marker.XMLWriteHandle;

/**
 * A Source Handle represents XML content as a transform source for reading
 * or transforms a source into a result for writing.
 */
public class SourceHandle
	extends BaseHandle<InputStream, OutputStreamSender>
	implements OutputStreamSender, BufferableHandle,
	    XMLReadHandle, XMLWriteHandle, 
	    StructureReadHandle, StructureWriteHandle
{
	static final private Logger logger = LoggerFactory.getLogger(SourceHandle.class);

	private Transformer transformer;
	private Source      content;

	public SourceHandle() {
		super();
		super.setFormat(Format.XML);
	}

	public Transformer getTransformer() {
		return transformer;
	}
	public void setTransformer(Transformer transformer) {
		this.transformer = transformer;
	}
	public SourceHandle withTransformer(Transformer transformer) {
		setTransformer(transformer);
		return this;
	}

	public Source get() {
		return content;
	}
	public void set(Source content) {
		this.content = content;
	}
	public SourceHandle with(Source content) {
		set(content);
		return this;
	}
	public void transform(Result result) {
		if (logger.isInfoEnabled())
			logger.info("Transforming source into result");
		try {
			if (content == null) {
				throw new IllegalStateException("No source to transform");
			}

			Transformer transformer = null;
			if (this.transformer != null) {
				transformer = getTransformer();
			} else {
				if (logger.isWarnEnabled())
					logger.warn("No transformer, so using identity transform");
				transformer = TransformerFactory.newInstance().newTransformer();
			}

			transformer.transform(content, result);
		} catch (TransformerException e) {
			logger.error("Failed to transform source into result",e);
			throw new MarkLogicIOException(e);
		}
	}

	public void setFormat(Format format) {
		if (format != Format.XML)
			throw new IllegalArgumentException("SourceHandle supports the XML format only");
	}
	public SourceHandle withMimetype(String mimetype) {
		setMimetype(mimetype);
		return this;
	}

	@Override
	public void fromBuffer(byte[] buffer) {
		if (buffer == null || buffer.length == 0)
			content = null;
		else
			receiveContent(new ByteArrayInputStream(buffer));
	}
	@Override
	public byte[] toBuffer() {
		try {
			if (content == null)
				return null;

			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			write(buffer);

			byte[] b = buffer.toByteArray();
			fromBuffer(b);

			return b;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected Class<InputStream> receiveAs() {
		return InputStream.class;
	}
	@Override
	protected void receiveContent(InputStream content) {
		if (content == null) {
			this.content = null;
			return;
		}

		this.content = new StreamSource(content);
	}
	@Override
	protected OutputStreamSender sendContent() {
		if (content == null) {
			throw new IllegalStateException("No source to transform to result for writing");
		}

		return this;
	}
	public void write(OutputStream out) throws IOException {
		transform(new StreamResult(new BufferedWriter(new OutputStreamWriter(out, "UTF-8"))));
	}
}
