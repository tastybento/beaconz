package com.wasteofplastic.include.it.unimi.dsi.util;

import java.util.Random;

/*		 
 * DSI utilities
 *
 * Copyright (C) 2013-2014 Sebastiano Vigna 
 *
 *  This library is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as published by the Free
 *  Software Foundation; either version 3 of the License, or (at your option)
 *  any later version.
 *
 *  This library is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 *  for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses/>.
 *
 */


/** An unbelievably fast, top-quality 64-bit {@linkplain Random pseudorandom number generator} that
 * combines George Marsaglia's Xorshift generators (described in <a
 * href="http://www.jstatsoft.org/v08/i14/paper/">&ldquo;Xorshift RNGs&rdquo;</a>, <i>Journal of
 * Statistical Software</i>, 8:1&minus;6, 2003) with a multiplication. 
 * 
 * <p><strong>Warning</strong>: the parameters of this generator have changed in version 2.1.7.
 *  
 * <p>More details about <code>xorshift*</code> generators can be found in my paper &ldquo;<a href="http://vigna.di.unimi.it/papers.php#VigEEMXGS">An experimental exploration of Marsaglia's <code>xorshift</code> generators,
 *  scrambled&rdquo;</a>, 2014.
 * 
 * <p>Note that this is
 * <strong>not</strong> a cryptographic-strength pseudorandom number generator, but its quality is
 * preposterously higher than {@link Random}'s, and its period is
 * 2<sup>1024</sup>&nbsp;&minus;&nbsp;1, which is more than enough for any application (it is actually
 * possible to define analogously a generator with period 2<sup>4096</sup>&nbsp;&minus;&nbsp;1,
 * but its interest is eminently academic). 
 * 
 * <p>A speed-comparison table in the documentation of {@link XorShift64StarRandom} shows 
 * that in several cases this generator is faster than <a
 * href="http://docs.oracle.com/javase/7/docs/api/java/util/concurrent/ThreadLocalRandom.html"
 * ><code>ThreadLocalRandom</code></a>, and that it is always faster than the <samp>xorgens</samp> generator described
 * by Richard P. Brent in &ldquo;Some long-period random number generators using shifts and xors&rdquo;, <i>ANZIAM Journal</i> 48 (CTAC2006), C188-C202, 2007.
 * 
 * <p>This is a top-quality generator: for instance, it performs significantly better than <samp>WELL1024a</samp> 
 * or <samp>MT19937</samp> in suites like  
 * <a href="http://www.iro.umontreal.ca/~simardr/testu01/tu01.html">TestU01</a> and
 * <a href="http://www.phy.duke.edu/~rgb/General/dieharder.php">Dieharder</a>. More precisely, over 100 runs of the BigCrush test suite
 * starting from equispaced points of the state space:
 * 
 * <ul>
 * 
 * <li>this generator and its reverse fail 51 tests;
 * 
 * <li><samp>xorgens</samp> and its reverse fail 82 tests;
 * 
 * <li><samp>WELL1024a</samp> and its reverse fail 882 tests (the tests failed at all points are MatrixRank and LinearComp);
 * 
 * <li><samp>MT19937</samp> and its reverse fail 516 tests (the only test failed at all points is LinearComp);
 * 
 * <li>{@link Random} and its reverse fail 13564 tests of all kind.
 * 
 * </ul>
 *
 * <p>This class extends {@link Random}, overriding (as usual) the {@link Random#next(int)} method.
 * Nonetheless, since the generator is inherently 64-bit also {@link Random#nextInt()},
 * {@link Random#nextInt(int)}, {@link Random#nextLong()} and {@link Random#nextDouble()} have been
 * overridden for speed (preserving, of course, {@link Random}'s semantics).
 * 
 * <p>If you do not need an instance of {@link Random}, or if you need a {@link RandomGenerator} to
 * use with <a href="http://commons.apache.org/math/">Commons Math</a>, you might be wanting
 * {@link XorShift1024StarRandomGenerator} instead of this class.
 * 
 * <p>If you want to use less memory at the expense of the period, consider using {@link XorShift64StarRandom} or {@link XorShift64StarRandomGenerator}.
 * 
 * <h3>Notes</h3>
 * 
 * <p>Testing with <a href="http://www.iro.umontreal.ca/~simardr/testu01/tu01.html">TestU01</a> and
 * <a href="http://www.phy.duke.edu/~rgb/General/dieharder.php">Dieharder</a> shows no difference in
 * quality between the higher and the lower bits of this generator. Thus, right shifting is used
 * whenever a subset of bits is necessary. 
 */
public class XorShift {
	
	private static long[] randomnessReserve=new long[]{
		0x8b654c54c5a25ee5L,
		0x75d986ee758ad676L,
		0xb2cad20309e80705L,
		0x3336695cdb7920a5L,
		0x3264ce96e505c279L,
		0xad8e0cebcc75264eL,
		0x8017de927c35d7bfL,
		0x733cde84a995cd7dL,
		0xa2297f411a7856b4L,
		0xbb9bc3b924a62341L,
		0x01e84a6b90b1a56dL,
		0x04a710dd1977377dL,
		0xe669ae68f500148bL,
		0x31ed0656c119508eL,
		0x3bc874713bba82ddL,
		0x8a1cf61baf444412L
	};
	
	private static final long serialVersionUID = 1L;

	/** 2<sup>-53</sup>. */
	private static final double NORM_53 = 1. / ( 1L << 53 );
	/** 2<sup>-24</sup>. */
	private static final double NORM_24 = 1. / ( 1L << 24 );

	/** The internal state of the algorithm. */
	private long[] s;
	private int p;

	/** Creates a new generator using a given seed.
	 * 
	 * @param seed a nonzero seed for the generator (if zero, the generator will be seeded with -1).
	 */
	public XorShift( final long[] seed ) {
		setState(seed);
	}

	protected int next( int bits ) {
		return (int)( nextLong() >>> 64 - bits );
	}
	
	public long nextLong() {
		long s0 = s[ p ];
		long s1 = s[ p = ( p + 1 ) & 15 ];
		s1 ^= s1 << 31;
		return 1181783497276652981L * ( s[ p ] = s1 ^ s0 ^ ( s1 >>> 11 ) ^ ( s0 >>> 30 ) );
	}

	public int nextInt() {
		return (int)nextLong();
	}
	
	public int nextInt( final int n ) {
		return (int)nextLong( n );
	}
	
	/** Returns a pseudorandom uniformly distributed {@code long} value
     * between 0 (inclusive) and the specified value (exclusive), drawn from
     * this random number generator's sequence. The algorithm used to generate
     * the value guarantees that the result is uniform, provided that the
     * sequence of 64-bit values produced by this generator is. 
     * 
     * @param n the positive bound on the random number to be returned.
     * @return the next pseudorandom {@code long} value between {@code 0} (inclusive) and {@code n} (exclusive).
     */
	public long nextLong( final long n ) {
        if ( n <= 0 ) throw new IllegalArgumentException();
		// No special provision for n power of two: all our bits are good.
		for(;;) {
			final long bits = nextLong() >>> 1;
			final long value = bits % n;
			if ( bits - value + ( n - 1 ) >= 0 ) return value;
		}
	}
	
	 public double nextDouble() {
		return ( nextLong() >>> 11 ) * NORM_53;
	}
	
	public float nextFloat() {
		return (float)( ( nextLong() >>> 40 ) * NORM_24 );
	}

	public boolean nextBoolean() {
		return ( nextLong() & 1 ) != 0;
	}
	
	public void nextBytes( final byte[] bytes ) {
		int i = bytes.length, n = 0;
		while( i != 0 ) {
			n = Math.min( i, 8 );
			for ( long bits = nextLong(); n-- != 0; bits >>= 8 ) bytes[ --i ] = (byte)bits;
		}
	}

	/** Sets the state of this generator.
	 * 
	 * <p>The internal state of the generator will be reset, and the state array filled with the provided array.
	 * 
	 * @param state an array of 16 longs; at least one must be nonzero.
	 */
	public void setState( final long[] state ) {
		int i=0;
		for(; i<state.length && i<s.length; i++) {
			s[i]=state[i] ^ randomnessReserve[i];
		}
		for(; i<s.length; i++)
			s[i]=randomnessReserve[i];
		//warm up the generator - this might be excessive
		for(i=0; i<s.length * 8; i++) {
			nextLong();
		}
		p = 0;
	}
}