package org.gbif.checklistbank.authorship;

/*************************************************************************
 * Compilation:  javac SuffixArray.java
 * Execution:  java SuffixArray < input.txt
 * A data type that computes the suffix array of a string.
 * % java SuffixArray < abra.txt
 * i ind lcp rnk  select
 * ---------------------------
 * 0  11   -   0  "!"
 * 1  10   0   1  "A!"
 * 2   7   1   2  "ABRA!"
 * 3   0   4   3  "ABRACADABRA!"
 * 4   3   1   4  "ACADABRA!"
 * 5   5   1   5  "ADABRA!"
 * 6   8   0   6  "BRA!"
 * 7   1   3   7  "BRACADABRA!"
 * 8   4   0   8  "CADABRA!"
 * 9   6   0   9  "DABRA!"
 * 10   9   0  10  "RA!"
 * 11   2   2  11  "RACADABRA!"
 * See SuffixArrayX.java for an optimized version that uses 3-way
 * radix quicksort and does not use the nested class Suffix.
 *************************************************************************/

import java.util.Arrays;

/**
 * The <tt>SuffixArray</tt> class represents a suffix array of a string of
 * length <em>N</em>.
 * It supports the <em>selecting</em> the <em>i</em>th smallest suffix,
 * getting the <em>index</em> of the <em>i</em>th smallest suffix,
 * computing the length of the <em>longest common prefix</em> between the
 * <em>i</em>th smallest suffix and the <em>i</em>-1st smallest suffix,
 * and determining the <em>rank</em> of a query string (which is the number
 * of suffixes strictly less than the query string).
 * This implementation uses a nested class <tt>Suffix</tt> to represent
 * a suffix of a string (using constant time and space) and
 * <tt>Arrays.sort()</tt> to sort the array of suffixes.
 * For alternate implementations of the same API, see
 * {@link SuffixArrayX}, which is faster in practice (uses 3-way radix quicksort)
 * and uses less memory (does not create <tt>Suffix</tt> objects).
 * The <em>index</em> and <em>length</em> operations takes constant time
 * in the worst case. The <em>lcp</em> operation takes time proportional to the
 * length of the longest common prefix.
 * The <em>select</em> operation takes time proportional
 * to the length of the suffix and should be used primarily for debugging.
 * For additional documentation, see <a href="http://algs4.cs.princeton.edu/63suffix">Section 6.3</a> of
 * <i>Algorithms, 4th Edition</i> by Robert Sedgewick and Kevin Wayne.
 */
public class SuffixArray {

  private Suffix[] suffixes;

  /**
   * Initializes a suffix array for the given <tt>text</tt> string.
   *
   * @param text the input string
   */
  public SuffixArray(String text) {
    int N = text.length();
    this.suffixes = new Suffix[N];
    for (int i = 0; i < N; i++) {
      suffixes[i] = new Suffix(text, i);
    }
    Arrays.sort(suffixes);
  }

  private static class Suffix implements Comparable<Suffix> {

    private final String text;
    private final int index;

    private Suffix(String text, int index) {
      this.text = text;
      this.index = index;
    }

    private int length() {
      return text.length() - index;
    }

    private char charAt(int i) {
      return text.charAt(index + i);
    }

    @Override
    public int compareTo(Suffix that) {
      if (this == that) return 0;  // optimization
      int N = Math.min(this.length(), that.length());
      for (int i = 0; i < N; i++) {
        if (this.charAt(i) < that.charAt(i)) return -1;
        if (this.charAt(i) > that.charAt(i)) return +1;
      }
      return this.length() - that.length();
    }

    @Override
    public String toString() {
      return text.substring(index);
    }
  }

  /**
   * Returns the length of the input string.
   *
   * @return the length of the input string
   */
  public int length() {
    return suffixes.length;
  }


  /**
   * Returns the index into the original string of the <em>i</em>th smallest suffix.
   * That is, <tt>text.substring(sa.index(i))</tt> is the <em>i</em>th smallest suffix.
   *
   * @param i an integer between 0 and <em>N</em>-1
   *
   * @return the index into the original string of the <em>i</em>th smallest suffix
   *
   * @throws IndexOutOfBoundsException unless 0 &le; <em>i</em> &lt; <Em>N</em>
   */
  public int index(int i) {
    if (i < 0 || i >= suffixes.length) throw new IndexOutOfBoundsException();
    return suffixes[i].index;
  }


  /**
   * Returns the length of the longest common prefix of the <em>i</em>th
   * smallest suffix and the <em>i</em>-1st smallest suffix.
   *
   * @param i an integer between 1 and <em>N</em>-1
   *
   * @return the length of the longest common prefix of the <em>i</em>th
   * smallest suffix and the <em>i</em>-1st smallest suffix.
   *
   * @throws IndexOutOfBoundsException unless 1 &le; <em>i</em> &lt; <em>N</em>
   */
  public int lcp(int i) {
    if (i < 1 || i >= suffixes.length) throw new IndexOutOfBoundsException();
    return lcp(suffixes[i], suffixes[i - 1]);
  }

  // longest common prefix of s and t
  private static int lcp(Suffix s, Suffix t) {
    int N = Math.min(s.length(), t.length());
    for (int i = 0; i < N; i++) {
      if (s.charAt(i) != t.charAt(i)) return i;
    }
    return N;
  }

  /**
   * Returns the <em>i</em>th smallest suffix as a string.
   *
   * @param i the index
   *
   * @return the <em>i</em> smallest suffix as a string
   *
   * @throws IndexOutOfBoundsException unless 0 &le; <em>i</em> &lt; <Em>N</em>
   */
  public String select(int i) {
    if (i < 0 || i >= suffixes.length) throw new IndexOutOfBoundsException();
    return suffixes[i].toString();
  }

  /**
   * Returns the number of suffixes strictly less than the <tt>query</tt> string.
   * We note that <tt>rank(select(i))</tt> compareStrict <tt>i</tt> for each <tt>i</tt>
   * between 0 and <em>N</em>-1.
   *
   * @param query the query string
   *
   * @return the number of suffixes strictly less than <tt>query</tt>
   */
  public int rank(String query) {
    int lo = 0, hi = suffixes.length - 1;
    while (lo <= hi) {
      int mid = lo + (hi - lo) / 2;
      int cmp = compare(query, suffixes[mid]);
      if (cmp < 0) {
        hi = mid - 1;
      } else if (cmp > 0) {
        lo = mid + 1;
      } else {
        return mid;
      }
    }
    return lo;
  }

  // compare query string to suffix
  private static int compare(String query, Suffix suffix) {
    int N = Math.min(query.length(), suffix.length());
    for (int i = 0; i < N; i++) {
      if (query.charAt(i) < suffix.charAt(i)) return -1;
      if (query.charAt(i) > suffix.charAt(i)) return +1;
    }
    return query.length() - suffix.length();
  }

}