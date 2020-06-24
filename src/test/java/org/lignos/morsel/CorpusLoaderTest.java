/*
 * Copyright 2009-2019 Constantine Lignos
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
package org.lignos.morsel;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import junit.framework.TestCase;
import org.lignos.morsel.lexicon.Lexicon;
import org.lignos.morsel.lexicon.Word;

/** Test basic wordlist loading capabilities. */
public class CorpusLoaderTest extends TestCase {
  private static final Charset CHARSET = Charset.forName("ISO8859_1");
  private static final Path wordListPath = Paths.get("data/test/test_wordlist.txt");
  private static final Path overflowListPath = Paths.get("data/test/test_overflowlist.txt");

  /** Test that entries are added to the lexicon when processing a wordlist. */
  public void testloadWordlist() throws IOException {
    Lexicon lex = CorpusLoader.loadWordlist(wordListPath, CHARSET, false);
    assertNotNull(lex.getWord("a"));
    assertEquals(500, lex.getWord("a").getCount());
    assertNotNull(lex.getWord("rat"));
    assertEquals(200, lex.getWord("rat").getCount());
    assertEquals(43295, lex.getTokenCount());
  }

  /** Test parsing a single line into a Word object. */
  public void testparseWordlistEntry() {
    assertEquals(new Word("at", 400, true, false), CorpusLoader.parseWordlistEntry("400 at"));
  }

  /** Test that counts bigger than MAX_INT do not cause overflow. */
  public void testloadWordlistOverflow() throws IOException {
    Lexicon lex = CorpusLoader.loadWordlist(overflowListPath, CHARSET, false);
    assertEquals(2147483648L, lex.getWord("biggerthanmaxint").getCount());
    assertEquals(2147526943L, lex.getTokenCount());
  }
}
