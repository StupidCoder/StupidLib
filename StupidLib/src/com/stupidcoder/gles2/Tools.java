/**
 * A collection of useful utility functions for Android
 *
 * @author Dennis Harms
 * @version 1.0
 */

package com.stupidcoder.gles2;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;

import android.content.res.Resources;

public class Tools {
	/**
	 * A DataInput stream implementation for reading little endian values from a
	 * binary InputStream.
	 * 
	 * @author Peter Franza (http://www.peterfranza.com/)
	 */
	public static class LittleEndianDataInputStream extends InputStream
			implements DataInput {
		private DataInputStream d; // to get at high level readFully methods of

		// DataInputStream
		private InputStream in; // to get at the low-level read methods of

		// InputStream
		private byte w[]; // work array for buffering input

		public LittleEndianDataInputStream(InputStream in) {
			this.in = in;
			this.d = new DataInputStream(in);
			w = new byte[8];
		}

		@Override
		public int available() throws IOException {
			return d.available();
		}

		@Override
		public final void close() throws IOException {
			d.close();
		}

		@Override
		public int read() throws IOException {
			return in.read();
		}

		@Override
		public final int read(byte b[], int off, int len) throws IOException {
			return in.read(b, off, len);
		}

		public final boolean readBoolean() throws IOException {
			return d.readBoolean();
		}

		public final byte readByte() throws IOException {
			return d.readByte();
		}

		/**
		 * like DataInputStream.readChar except little endian.
		 */
		public final char readChar() throws IOException {
			d.readFully(w, 0, 2);
			return (char) ((w[1] & 0xff) << 8 | (w[0] & 0xff));
		}

		public final double readDouble() throws IOException {
			return Double.longBitsToDouble(readLong());
		}

		public final float readFloat() throws IOException {
			return Float.intBitsToFloat(readInt());
		}

		public final void readFully(byte b[]) throws IOException {
			d.readFully(b, 0, b.length);
		}

		public final void readFully(byte b[], int off, int len)
				throws IOException {
			d.readFully(b, off, len);
		}

		/**
		 * like DataInputStream.readInt except little endian.
		 */
		public final int readInt() throws IOException {
			d.readFully(w, 0, 4);
			return (w[3]) << 24 | (w[2] & 0xff) << 16 | (w[1] & 0xff) << 8
					| (w[0] & 0xff);
		}

		@Deprecated
		public final String readLine() throws IOException {
			return d.readLine();
		}

		/**
		 * like DataInputStream.readLong except little endian.
		 */
		public final long readLong() throws IOException {
			d.readFully(w, 0, 8);
			return (long) (w[7]) << 56 | (long) (w[6] & 0xff) << 48
					| (long) (w[5] & 0xff) << 40 | (long) (w[4] & 0xff) << 32
					| (long) (w[3] & 0xff) << 24 | (long) (w[2] & 0xff) << 16
					| (long) (w[1] & 0xff) << 8 | (w[0] & 0xff);
		}

		public final short readShort() throws IOException {
			d.readFully(w, 0, 2);
			return (short) ((w[1] & 0xff) << 8 | (w[0] & 0xff));
		}

		public final int readUnsignedByte() throws IOException {
			return d.readUnsignedByte();
		}

		/**
		 * Note, returns int even though it reads a short.
		 */
		public final int readUnsignedShort() throws IOException {
			d.readFully(w, 0, 2);
			return ((w[1] & 0xff) << 8 | (w[0] & 0xff));
		}

		public final String readUTF() throws IOException {
			return d.readUTF();
		}

		public final int skipBytes(int n) throws IOException {
			return d.skipBytes(n);
		}
	}

	/**
	 * Gets the current time in milliseconds.
	 * 
	 * @return The current time in milliseconds.
	 */
	public static long currentTime() {
		return Calendar.getInstance().getTimeInMillis();
	}

	/**
	 * Reads a text file from a raw resource.
	 * 
	 * @param pRes
	 *            The resources instance of the application's package.
	 * @param pId
	 *            The ID of the resource to read.
	 * @return The content of the resource as a string.
	 * @throws IOException
	 *             if a problem occurs while reading the data.
	 */
	public static String getTextFromResource(Resources pRes, int pId)
			throws IOException {
		String text = "";
		DataInputStream input = new DataInputStream(pRes.openRawResource(pId));
		while (true) {
			String line = input.readLine();
			if (line == null)
				break;
			text += line + "\n";
		}
		input.close();
		return text;
	}
}
