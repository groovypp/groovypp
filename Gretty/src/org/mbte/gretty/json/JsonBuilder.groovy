/*
 * Copyright 2009-2010 MBTE Sweden AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@Typed package org.mbte.gretty.json

import org.codehaus.jackson.JsonGenerator
import org.codehaus.jackson.map.MappingJsonFactory

class JsonBuilder2 {
    private final MappingJsonFactory factory = []
    private final JsonGenerator gen

    JsonBuilder2(Writer out) {
        gen = factory.createJsonGenerator(out)
        gen.useDefaultPrettyPrinter()
    }

    abstract static class Definition {
        protected JsonGenerator gen

        abstract void define ()

        final void invokeUnresolvedMethod(String name, Object obj) {
            if(obj == null) {
                gen.writeNullField name
                return 
            }

            switch(obj) {
                case Closure:
                    gen.writeObjectFieldStart(name)
                    obj.call()
                    gen.writeEndObject()
                break

                case Definition:
                    gen.writeObjectFieldStart(name)
                    obj.gen = gen
                    obj.define()
                    obj.gen = null
                    gen.writeEndObject()
                break

                case String:
                    gen.writeStringField(name, obj)
                break

                case Number:
                    gen.writeNumberField(name, obj)
                break

                case Map:
                    gen.writeObjectFieldStart(name)
                    for(e in obj.entrySet()) {
                        invokeUnresolvedMethod(e.key.toString(), e.value)
                    }
                    gen.writeEndObject()
                break

                case Iterable:
                    gen.writeArrayFieldStart(name)
                    iterate(obj)
                    gen.writeEndArray()
                break

                case Object []:
                    invokeUnresolvedMethod(name, obj.iterator())
                break

                default:
                    gen.writeObjectField(name, obj)
                break
            }
        }

        private void iterate(Iterable obj) {
            for (e in obj) {
                if(e == null) {
                    gen.writeNull()
                    continue
                }

                switch (e) {
                    case Closure:
                        gen.writeStartObject()
                        e.call()
                        gen.writeEndObject()
                        break

                    case Definition:
                        e.gen = gen
                        gen.writeStartObject()
                        e.define()
                        gen.writeEndObject()
                        e.gen = null
                        break

                    case Map:
                        gen.writeStartObject()
                        for (ee in e.entrySet()) {
                            invokeUnresolvedMethod(ee.key.toString(), ee.value)
                        }
                        gen.writeEndObject()
                        break

                    case String:
                        gen.writeString(e)
                        break

                    case Number:
                        gen.writeNumber(e)
                        break

                    case Iterable:
                        gen.writeStartArray()
                        iterate(e)
                        gen.writeEndArray()
                        return

                    default:
                        gen.writeObject(e)
                }
            }
        }
    }

    void invokeUnresolvedMethod(String name, Definition obj) {
        gen.writeStartObject()
        gen.writeObjectFieldStart name
        obj.gen = gen
        obj.define()
        obj.gen = null
        gen.writeEndObject()
        gen.writeEndObject()
        gen.close ()
    }
}

JsonBuilder2.Definition externalData = {
    z 239
    h 45
    time( new Date())
}

JsonBuilder2 builder = [new PrintWriter(System.out)]
builder.person {
    firstName 'Guillaume'
    lastName  'Laforge'
    address (
        city: 'Paris',
        country: 'France',
        zip: 12345,
    )

    additionalData(["xxxxxx", [x: 12, y:14], externalData, ['a', 'b', 'c']])
//        married = true
//        conferences ['JavaOne', 'Gr8conf']
}
