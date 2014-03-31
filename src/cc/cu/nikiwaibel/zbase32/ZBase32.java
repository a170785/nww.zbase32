/*
 * ZBase32
 * src/cc/cu/nikiwaibel/zbase32/ZBase32.java
 * Copyright (C) 2014  Niki W. Waibel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package cc.cu.nikiwaibel.zbase32;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * <pre>
 * 0 1 2 3 4 5 6 7 ... 1 byte ->   1/2 values
 * [       ] [    
 * 0 1 2 3 4 5 6 7 ... 2 byte -> 2/3/4 values
 *   ] [       ] [
 * 0 1 2 3 4 5 6 7 ... 3 byte ->   4/5 values
 *       ] [      
 * 0 1 2 3 4 5 6 7 ... 4 byte -> 5/6/7 values
 * ] [       ] [
 * 0 1 2 3 4 5 6 7 ... 5 byte ->   7/8 values
 *     ] [       ]
 * </pre>
 * 
 * @author niki
 * @see <a
 *      href="http://philzimmermann.com/docs/human-oriented-base-32-encoding.txt">http://philzimmermann.com/docs/human-oriented-base-32-encoding.txt</a>
 */
public class ZBase32
{
	protected static final String	ALPHABET_STRING	= "ybndrfg8ejkmcpqxot1uwisza345h769";
	protected static final byte[]	ALPHABET		= new byte[ALPHABET_STRING
															.length()];
	protected static final byte[]	REV_ALPHABET	= new byte[256];
	protected static final int[]	DECODED_JUMPS	= new int[] { 0, 1, 2, 2,
			3, 4, 4, 5								};

	static {
		// Let's generate the byte array alphabet out of the string, before we
		// do anything else.
		for (int i = 0; i < ALPHABET_STRING.length(); i++) {
			ALPHABET[i] = (byte)ALPHABET_STRING.charAt(i);
		}
		for (int i = 0; i < REV_ALPHABET.length; i++) {
			REV_ALPHABET[i] = -1;
		}
		for (int i = 0; i < ALPHABET.length; i++) {
			REV_ALPHABET[ALPHABET[i]] = (byte)i;
		}
	}

	/**
	 * Hidden, as this class is static so far.
	 */
	private ZBase32()
	{
	}

	/**
	 * Returns the size in of the encoded Z-Base-32 ASCII output in bits.
	 * 
	 * @param inBits
	 *            This many bits are used for the input.
	 * @return The number of bits required to store the encoded Z-Base-32 ASCII
	 *         byte values.
	 */
	public static int encodedBits(final int inBits)
	{
		// round upwards to nearest 5
		final int nearest5offset;
		if ((inBits % 5) == 0) {
			// aligned - no modifications required.
			nearest5offset = 0;
		} else {
			// unaligned
			nearest5offset = (int)(5 - (inBits % 5));
		}

		return ((inBits + nearest5offset) / 5) << 3;
	}

	/**
	 * Encode binary data to Z-Base-32 ASCII byte values.
	 * 
	 * @param in
	 *            The binary input data byte array.
	 * @param inBits
	 *            The significant bits to take care of. If greater then
	 *            {@code in.length << 3} it is set to {@code in.length << 3}.
	 * @return The encoded Z-Base-32 byte array.
	 */
	public static byte[] encode(byte[] in, int inBits)
	{
		inBits = growInBitsIfRequired(inBits);
		in = growInIfRequired(in, inBits);

		final byte[] out = new byte[encodedBits(inBits) >> 3];
		int i = 0;
		for (int o = 0; o < out.length; o++) {
			final int x;
			switch (o % 8) {
			case 0:
				x = (in[i] >>> 3);
				break;
			case 1:
				x = (in[i] << 2) | (in[++i] >>> 6 & 0x03);
				break;
			case 2:
				x = in[i] >>> 1;
				break;
			case 3:
				x = (in[i] << 4) | (in[++i] >>> 4 & 0x0f);
				break;
			case 4:
				x = (in[i] << 1) | (in[++i] >>> 7 & 0x01);
				break;
			case 5:
				x = in[i] >>> 2;
				break;
			case 6:
				x = (in[i] << 3) | (in[++i] >>> 5 & 0x07);
				break;
			case 7:
				x = in[i++];
				break;
			default:   // Cannot happen, but makes the compiler happy.
				x = 0;
			}
			out[o] = ALPHABET[x & 0x1f];
		}
		return out;
	} // public static byte[] encode(final byte[] in)

	/**
	 * The number of input bits have to be aligned to a 5 bit boundary.
	 * 
	 * @param inBits
	 *            The number of relevant bits specified.
	 * @return The number of required bits for the conversion.
	 */
	private static int growInBitsIfRequired(final int inBits)
	{
		final int inBitsMod5 = inBits % 5;

		if (inBitsMod5 == 0) {   // aligned
			return inBits;
		} else {   // unaligned
			return inBits + (5 - inBitsMod5);
		}
	}

	/**
	 * <pre>
	 * 0 1 2 3 4 5 6 7 ... 1 byte ->   1/2 values
	 * [       ] [    
	 * 0 1 2 3 4 5 6 7 ... 2 byte -> 2/3/4 values
	 *   ] [       ] [
	 * 0 1 2 3 4 5 6 7 ... 3 byte ->   4/5 values
	 *       ] [      
	 * 0 1 2 3 4 5 6 7 ... 4 byte -> 5/6/7 values
	 * ] [       ] [
	 * 0 1 2 3 4 5 6 7 ... 5 byte ->   7/8 values
	 *     ] [       ]
	 * </pre>
	 */
	private static byte[] growInIfRequired(final byte[] in, final int inBits)
	{
		if ((in.length << 3) < inBits) {
			return Arrays.copyOf(in, (inBits + 7) >> 3);
		} else {
			return in;
		}
	}

	/**
	 * Returns the size of the decoded byte buffer required.
	 * 
	 * @param encodedBuffer
	 *            The buffer to decode aka the encoded buffer.
	 * @return The number of bytes required to store the decoded byte values of
	 *         the encodedBuffer.
	 */
	public static int decodedSize(final byte[] encodedBuffer)
	{
		return encodedBuffer.length / 8 * 5
				+ DECODED_JUMPS[encodedBuffer.length % 8];
	}

	/**
	 * TODO
	 * 
	 * @param in
	 *            TODO
	 * @return TODO
	 */
	public static byte[] decode(final byte[] in)
	{
		final byte[] out = new byte[decodedSize(in)];
		int o = 0;
		for (int i = 0; i < in.length; i++) {
// System.out.printf("0x%02x -> 0x%08x\n", in[i], REV_ALPHABET[in[i]]);
			switch (i % 8) {
			case 0:
				out[o] |= (byte)(REV_ALPHABET[in[i]] << 3);
				break;
			case 1:
				out[o++] |= (byte)(REV_ALPHABET[in[i]] >>> 2 & 0x07);
				out[o] |= (byte)(REV_ALPHABET[in[i]] << 6);
				break;
			case 2:
				out[o] |= (byte)(REV_ALPHABET[in[i]] << 1 & 0x3f);
				break;
			case 3:
				out[o++] |= (byte)(REV_ALPHABET[in[i]] >>> 4 & 0x01);
				out[o] |= (byte)(REV_ALPHABET[in[i]] << 4);
				break;
			case 4:
				out[o++] |= (byte)(REV_ALPHABET[in[i]] >>> 1 & 0x0f);
				out[o] |= (byte)(REV_ALPHABET[in[i]] << 7);
				break;
			case 5:
				out[o] |= (byte)(REV_ALPHABET[in[i]] << 2 & 0x7c);
				break;
			case 6:
				out[o++] |= (byte)(REV_ALPHABET[in[i]] >>> 3 & 0x03);
				out[o] |= (byte)(REV_ALPHABET[in[i]] << 5);
				break;
			case 7:
				out[o++] |= (byte)(REV_ALPHABET[in[i]] & 0x1f);
				break;
			}
		}
		return out;
	} // public static byte[] decode(final byte[] in)

	/**
	 * Minimal example implementation of the {@link #encode(byte[], int)} and
	 * {@link #decode(byte[])} methods.
	 * 
	 * @param args
	 *            The arguments provided on the commandline.
	 */
	public static void main(final String[] args)
	{
		final BufferedInputStream bufferedInputStream;
		final BufferedOutputStream bufferedOutputStream;
		final boolean streaming;

		if (args.length < 1 || args.length > 2) {
			showUsage(true);
			System.exit(1);
		}

		if ("help".equalsIgnoreCase(args[0])) {
			showUsage(false);
			System.exit(0);
		}

		if (args.length == 2) {
			streaming = false;
			bufferedInputStream = null;
			bufferedOutputStream = null;
		} else {
			streaming = true;
			bufferedInputStream = new BufferedInputStream(System.in);
			bufferedOutputStream = new BufferedOutputStream(System.out);
		}

		if ("encode".equalsIgnoreCase(args[0])) {
			if (streaming) {
				final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
				try {
					int b;
					while ((b = bufferedInputStream.read()) != -1) {
						byteArrayOutputStream.write(b);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				final byte[] a = byteArrayOutputStream.toByteArray();
				try {
					bufferedOutputStream.write(encode(a, a.length << 3));
					bufferedOutputStream.flush();
					bufferedOutputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				System.out.println(new String(encode(args[1].getBytes(),
						args[1].getBytes().length << 3)));
			}
		} else if ("decode".equalsIgnoreCase(args[0])) {
			if (streaming) {
				final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
				try {
					int b;
					while ((b = bufferedInputStream.read()) != -1) {
						byteArrayOutputStream.write(b);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				final byte[] a = byteArrayOutputStream.toByteArray();
				try {
					bufferedOutputStream.write(decode(a));
					bufferedOutputStream.flush();
					bufferedOutputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				System.out.println(new String(decode(args[1].getBytes())));
			}
		} else {
			showUsage(true);
			System.exit(1);
		}
	} // public static void main(final String[] args)

	/**
	 * Guess what.
	 * 
	 * @param error
	 *            If true, print to stderr. stdout otherwise.
	 */
	private static void showUsage(boolean error)
	{
		final String msg = "Usage:\tjava -jar zbase32.jar [encode|decode] <text>\n"
				+ "\tjava -jar zbase32.jar [encode|decode] < [file]\n"
				+ "\techo -ne '\\x00\\xff' | java -jar zbase32.jar [encode|decode]\n"
				+ "\tjava -jar zbase32.jar help\n"
				+ "\n"
				+ "Example: java -jar zbase32.jar encode foo\n"
				+ "Result: c3zs6";
		if (error) {
			System.err.println(msg);
		} else {
			System.out.println(msg);
		}
	}
} // public class ZBase32