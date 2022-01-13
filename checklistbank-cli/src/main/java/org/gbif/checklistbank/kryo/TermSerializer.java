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
package org.gbif.checklistbank.kryo;

import org.gbif.dwc.terms.Term;
import org.gbif.dwc.terms.TermFactory;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

public class TermSerializer extends Serializer<Term> {
    private final TermFactory TF = TermFactory.instance();

    public TermSerializer() {
        // dont accept null values
        super(false);
    }

    @Override
    public void write(Kryo kryo, Output output, Term term) {
        output.writeString(term.qualifiedName());
    }

    @Override
    public Term read(Kryo kryo, Input input, Class<Term> aClass) {
        return TF.findTerm(input.readString());
    }
}