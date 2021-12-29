/*
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
package org.gbif.nub.lookup.similarity;

// Copyright (c) 2011, Commonwealth of Australia
// Copied and adapted from ALA:
// https://ala-nsl.googlecode.com/svn/taxamatch/trunk/src/au/org/biodiversity/services/taxamatch/impl/ModifiedDamerauLevenshtein.java

import java.util.Arrays;

public class ModifiedDamerauLevenshtein implements StringSimilarity {
	private final int pBlockLimit;

	// this variable holds a shared array, to reduce heap use
	// we use a little synchronisation to manage the space, but
	// synchronisation is reasonably quick these days.

	private static volatile int[] a_matrix = new int[64 * 64];
	private static final Object mutex = new Object();

  /**
   * A default MDL with a block limit of just 2 edits.
   */
  public ModifiedDamerauLevenshtein() {
    this.pBlockLimit = 2;
  }

  /**
   * @param limit the maximum allowed distance
   */
  public ModifiedDamerauLevenshtein(int limit) {
		this.pBlockLimit = limit;
	}

  @Override
  public double getSimilarity(String x1, String x2) {
    return DistanceUtils.convertEditDistanceToSimilarity(getEditDistance(x1, x2), x1, x2);
  }

  public final int getEditDistance(final String s1, final String s2) {
		if (s1.equals(s2)) {
			return 0;
    } else if (s1.isEmpty() || s2.isEmpty()) {
			return Math.max(s1.length(), s2.length());
    } else if (s1.length() == 1 && s2.length() == 1) {
      return 1;
    }

		final char[] t1;
		final char[] t2;

		{
			StringBuilder sb1 = new StringBuilder(s1);
			StringBuilder sb2 = new StringBuilder(s2);

			// these hold the index of the last character.
			int l1 = sb1.length()-1;
			int l2 = sb2.length()-1;

			while (l1>=0 && l2>=0 && sb1.charAt(0) == sb2.charAt(0)) {
				sb1.deleteCharAt(0);
				sb2.deleteCharAt(0);
				l1--;
				l2--;
			}

			while (l1>=0 && l2>=0 && sb1.charAt(l1) == sb2.charAt(l2)) {
				sb1.deleteCharAt(l1--);
				sb2.deleteCharAt(l2--);
			}

			l1++;
			l2++;
			if (l1 == 0 || l2 == 0)
				return Math.max(l1, l2);
			else if (l1 == 1 && l2 == 1) return 1;

			t1 = sb1.toString().toCharArray();
			t2 = sb2.toString().toCharArray();
		}

		final int temp1Len = t1.length;
		final int temp2Len = t2.length;

		// using a 1-dimensional array with bit fiddling to get to the elements
		// saves about 12-15% off the running time.
		// so, we replace matrix[a][b] with matrix[((a)<<6)|(b)]
		// this limits us to 64-character words, which should be plenty. I hope.

		final int[] matrix;
		synchronized (mutex) {
			if (a_matrix == null) {
				matrix = new int[64 * 64];
			}
			else {
				matrix = a_matrix;
				a_matrix = null;
				Arrays.fill(matrix, 0);
			}
		}

		for (int i = 0; i <= temp1Len; i++) {
			matrix[i << 6] = i;
		}

		for (int i = 0; i <= temp2Len; i++) {
			matrix[i] = i;
		}

		for (int i = 1; i <= temp1Len; i++) {
			matrix[i << 6] = i;
			for (int j = 1; j <= temp2Len; j++) {
				int cost;
				if (t1[i - 1] == t2[j - 1]) {
					cost = 0;
				}
				else {
					cost = 1;
				}

				int temp_block_length = Math.max(//
						Math.min(temp1Len / 2, //
								Math.min(temp2Len / 2, //
										Math.max(pBlockLimit, 1))), //
						1);

				while (temp_block_length >= 1) {

					final int sub1 = i - ((temp_block_length * 2) - 1);
					final int sub2 = j - (temp_block_length - 1);
					final int sub3 = i - (temp_block_length - 1);
					final int sub4 = j - ((temp_block_length * 2) - 1);

					if (i >= (temp_block_length * 2) && j >= (temp_block_length * 2)
							&& substreq(t1, sub1, t2, sub2, temp_block_length)
							&& substreq(t1, sub3, t2, sub4, temp_block_length)) {

						final int ins = matrix[((i) << 6) | (j - 1)] + 1;
						final int del = matrix[((i - 1) << 6) | j] + 1;
						final int tran = matrix[((i - (temp_block_length * 2)) << 6) | (j - (temp_block_length * 2))]
								+ cost + (temp_block_length - 1);
						matrix[(i << 6) | j] = minimum(ins, del, tran);

						temp_block_length = 0;

					}
					else if (temp_block_length == 1) {

						final int del = matrix[((i - 1) << 6) | j] + 1;
						final int ins = matrix[(i << 6) | (j - 1)] + 1;
						final int sub = matrix[((i - 1) << 6) | (j - 1)] + cost;

						matrix[(i << 6) | j] = minimum(ins, del, sub);
					}
					temp_block_length--;
				}

			}
		}

		int ret = matrix[(temp1Len << 6) | temp2Len];

		a_matrix = matrix; // does not need to be synchronized, it is atomic

		return ret;

	}

	// The compiler inlines these, I think.

	private final static int minimum(int d, int i, int s) {
		return d < i ? (d < s ? d : s) : (i < s ? i : s);
	}

	private final static boolean substreq(char[] src1, int start1, char[] src2, int start2, int length) {
		int at1 = start1 >= 0 ? start1 - 1 : src1.length + start1;
		int at2 = start2 >= 0 ? start2 - 1 : src2.length + start2;
		while (length-- > 0) {
			if (src1[at1++] != src2[at2++]) return false;
		}
		return true;
	}
}
